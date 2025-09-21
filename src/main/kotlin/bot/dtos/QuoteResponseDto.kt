package bot.dtos

import kotlinx.serialization.Serializable

@Serializable
data class QuoteResponseDto(
    val status: Int,
    val message: String,
    val error: Boolean,
    val data: Data
) {
    @Serializable
    data class Data(
        val currentSqrtPrice: String,
        val newSqrtPrice: String,
        val fee: Int,
        val amountIn: String,
        val amountOut: String
    )
}