package bot.dtos

import kotlinx.serialization.Serializable

@Serializable
data class StatusDto(
    val status: Int,
    val data: Data
) {
    @Serializable
    data class Data(
        val status: String
    )
}