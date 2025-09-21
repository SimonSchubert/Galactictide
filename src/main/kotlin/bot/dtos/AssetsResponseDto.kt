package bot.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssetsResponseDto(
    @SerialName("Data")
    val data: List<Asset>
) {
    @Serializable
    data class Asset(
        val collection: String,
        val category: String,
        val type: String,
        val additionalKey: String,
        val quantity: String
    )
}