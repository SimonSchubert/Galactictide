package bot.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SwapPayloadDto(
    val status: Int,
    val message: String,
    val error: Boolean,
    val data: Data
) {
    @Serializable
    data class Data(
        val token0: TokenInfo,
        val token1: TokenInfo,
        val fee: Int,
        val amount: String,
        val zeroForOne: Boolean,
        val sqrtPriceLimit: String,
        val amountInMaximum: String,
        val amountOutMinimum: String,
        val uniqueKey: String
    ) {
        @Serializable
        data class TokenInfo(
            val collection: String,
            val category: String,
            val type: String,
            val additionalKey: String
        )
    }
}