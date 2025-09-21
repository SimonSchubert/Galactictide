package bot.dtos

import kotlinx.serialization.Serializable

@Serializable
data class TransactionResponseDto(val data: TransactionData) {
    @Serializable
    data class TransactionData(val data: String)
}