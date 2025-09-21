package bot

import com.google.gson.Gson
import bot.dtos.AssetsResponseDto
import bot.dtos.QuoteResponseDto
import bot.dtos.StatusDto
import bot.dtos.SwapPayloadDto
import bot.dtos.TransactionResponseDto
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.io.File
import java.math.BigDecimal

val dotenv = dotenv()
val gson = Gson()

const val API_BASE = "https://dex-backend-prod1.defi.gala.com"

val PRIVATE_KEY = dotenv["PRIVATE_KEY"].trim()
val USER = dotenv["USER_ADDRESS"].trim()

const val slippagePercentage = 0.5

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
}

@Serializable
data class Config(
    val tokenIn: String,
    val tokenOut: String,
    val intervalsLeft: Int,
    val totalIntervals: Int
)

fun getConfig(): Config {
    val file = File("config.json")
    val config = Json.decodeFromString<Config>(file.readText())
    val updatedConfig = if(config.intervalsLeft <= 1) {
        config.copy(
            intervalsLeft = config.totalIntervals,
            tokenIn = config.tokenOut,
            tokenOut = config.tokenIn)
    } else {
        config.copy(intervalsLeft = config.intervalsLeft - 1)
    }
    file.writeText(Json.encodeToString(updatedConfig))
    return config
}

fun main() = runBlocking {
    val config = getConfig()

    println("User: $USER - interval: ${config.intervalsLeft}/${config.totalIntervals}")

    val assets = getUserAssets(USER)
    if (assets == null) return@runBlocking

    var amountIn: Double? = null

    assets.data.forEach { asset ->
        if(asset.collection == config.tokenIn.toTokenClass().collection) {
            amountIn = (asset.quantity.toDouble() * 0.9) / config.intervalsLeft.toDouble()
        }
        println("${asset.collection} ${asset.quantity}")
    }

    if(amountIn == null) {
        println("❗${config.tokenOut} not found in user assets")
        return@runBlocking
    }

    val tokenIn = config.tokenIn
    val tokenOut = config.tokenOut

    val quoteResult = getBestQuote(
        tokenIn = tokenIn,
        tokenOut = tokenOut,
        amountIn = amountIn
    )
    if (quoteResult == null) return@runBlocking

    println("Best quote: ${quoteResult.data.amountIn} ${tokenIn.toTokenClass().collection} -> ${quoteResult.data.amountOut} ${tokenOut.toTokenClass().collection}, tier: ${quoteResult.data.fee}, slippage: $slippagePercentage%")

    var minOut =
        quoteResult.data.amountOut.toBigDecimal() * (1 - slippagePercentage / 100f).toBigDecimal()

    if (minOut > BigDecimal.ZERO) {
        minOut = minOut * (-1).toBigDecimal()
    }

    val swapPayload = getSwapPayload(
        tokenIn = tokenIn,
        tokenOut = tokenOut,
        fee = quoteResult.data.fee,
        sqrtPriceLimit = quoteResult.data.newSqrtPrice,
        amountIn = quoteResult.data.amountIn,
        amountOutMinimum = minOut.toPlainString()
    )

    if (swapPayload == null) return@runBlocking
    println("✅Swap payload")

    val signature = signPayload(swapPayload.data)

    val response = executeTransaction(payload = swapPayload.data, signature = signature)
    if (response == null) return@runBlocking
    println("✅Execute transaction")

    var statusResponse: StatusDto?
    do {
        delay(300L)
        statusResponse = getTransactionStatus(response.data.data)
        val emoji = when (statusResponse?.data?.status) {
            "PENDING" -> "⌛"
            "PROCESSED" -> "✅"
            else -> ""
        }
        print("\r${emoji}Status: ${statusResponse?.data?.status}")
    } while (statusResponse?.data?.status == "PENDING")

    println()
}

suspend fun getUserAssets(user: String): AssetsResponseDto? {
    return try {
        client.post("https://api-galaswap.gala.com/galachain/api/asset/token-contract/FetchBalances") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("owner", user)
            })
        }.body()
    } catch (e: Exception) {
        println("❗Error fetching user assets: ${e.message}")
        null
    }
}

suspend fun getTransactionStatus(id: String): StatusDto? {
    return try {
        client.get("$API_BASE/v1/trade/transaction-status") {
            parameter("id", id)
        }.body()
    } catch (e: Exception) {
        println("❗Error during transaction status check: ${e.message}")
        null
    }
}

suspend fun executeTransaction(
    payload: SwapPayloadDto.Data,
    signature: String
): TransactionResponseDto? {
    return try {
        client.post("$API_BASE/v1/trade/bundle") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("payload", Json.encodeToJsonElement(payload))
                    put("type", "swap")
                    put("signature", signature)
                    put("user", USER)
                }
            )
        }.body()
    } catch (e: Exception) {
        println("❗Error during executeTransaction: ${e.message}")
        null
    }
}

suspend fun getSwapPayload(
    tokenIn: String,
    tokenOut: String,
    fee: Int,
    sqrtPriceLimit: String,
    amountIn: String,
    amountOutMinimum: String
): SwapPayloadDto? {
    return try {
        client.post("$API_BASE/v1/trade/swap") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("tokenIn", tokenIn.toJsonObject())
                    put("tokenOut", tokenOut.toJsonObject())
                    put("fee", fee)
                    put("sqrtPriceLimit", sqrtPriceLimit)
                    put("amountIn", amountIn)
                    put("amountInMaximum", amountIn)
                    put("amountOutMinimum", amountOutMinimum)
                }
            )
        }.body()
    } catch (e: Exception) {
        println("❗Error during quoteExactInput: ${e.message}")
        null
    }
}

fun signPayload(data: SwapPayloadDto.Data): String {
    val objectToSign = mapOf(
        "amount" to data.amount,
        "amountInMaximum" to data.amountInMaximum,
        "amountOutMinimum" to data.amountOutMinimum,
        "fee" to data.fee,
        "sqrtPriceLimit" to data.sqrtPriceLimit,
        "token0" to mapOf(
            "additionalKey" to data.token0.additionalKey,
            "category" to data.token0.category,
            "collection" to data.token0.collection,
            "type" to data.token0.type,
        ),
        "token1" to mapOf(
            "additionalKey" to data.token1.additionalKey,
            "category" to data.token1.category,
            "collection" to data.token1.collection,
            "type" to data.token1.type,
        ),
        "uniqueKey" to data.uniqueKey,
        "zeroForOne" to data.zeroForOne,
    )

    val payloadHash = Hash.sha3(gson.toJson(objectToSign).toByteArray())

    val credentials = Credentials.create(PRIVATE_KEY.replace("0x", ""))

    val signatureData = Sign.signMessage(payloadHash, credentials.ecKeyPair, false)

    val signature = Numeric.toHexString(signatureData.r) +
            Numeric.toHexString(signatureData.s).substring(2) +
            Numeric.toHexString(signatureData.v).substring(2)

    return signature
}

suspend fun getQuote(
    tokenIn: String,
    tokenOut: String,
    amountIn: Double,
    fee: Int
): QuoteResponseDto? {
    return try {
        client.get("$API_BASE/v1/trade/quote?tokenIn=$tokenIn&tokenOut=$tokenOut&amountIn=$amountIn&fee=$fee") {
            contentType(ContentType.Application.Json)
        }.body()
    } catch (e: Exception) {
        println("Error during quoteExactInput for fee $fee: ${e.message}")
        null
    }
}

suspend fun getBestQuote(tokenIn: String, tokenOut: String, amountIn: Double): QuoteResponseDto? {
    val feeTiers = listOf(500, 3000, 10000)
    for (fee in feeTiers) {
        try {
            val quote = getQuote(tokenIn, tokenOut, amountIn, fee)
            if (quote != null && quote.data.amountOut.toDouble() > 0.0) {
                return quote
            }
        } catch (e: Exception) {
            println("Failed quote for fee $fee: ${e.message}")
        }
    }
    println("No pools found with liquidity on any fee tier.")
    return null
}