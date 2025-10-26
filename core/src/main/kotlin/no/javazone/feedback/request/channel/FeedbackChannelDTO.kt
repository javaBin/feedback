package no.javazone.feedback.request.channel

import kotlinx.serialization.Serializable
import no.javazone.feedback.domain.FeedbackChannel

@Serializable
data class FeedbackChannelDTO(
    val id: Long,
    val title: String,
    val speakers: List<String>,
    val externalId: String?
)

fun FeedbackChannel.toDTO(): FeedbackChannelDTO {
    return FeedbackChannelDTO(
        id = id,
        title = title,
        speakers = speakers,
        externalId = externalId
    )
}