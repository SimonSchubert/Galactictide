package bot

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class TokenClass(
    val collection: String,
    val category: String,
    val type: String,
    val additionalKey: String
)

fun String.toTokenClass(): TokenClass {
    val parts = this.split("$")
    return TokenClass(
        collection = parts[0],
        category = parts[1],
        type = parts[2],
        additionalKey = parts[3]
    )
}

fun String.toJsonObject(): JsonObject {
    val parts = this.split("$")
    return buildJsonObject {
        put("collection", parts[0])
        put("category", parts[1])
        put("type", parts[2])
        put("additionalKey", parts[3])
    }
}