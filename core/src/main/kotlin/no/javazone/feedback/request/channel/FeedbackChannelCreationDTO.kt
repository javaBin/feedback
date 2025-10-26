package no.javazone.feedback.request.channel

import kotlinx.serialization.Serializable
import no.javazone.feedback.domain.FeedbackChannelCreationInput

@Serializable
data class FeedbackChannelCreationDTO(
    val title: String,
    val speakers: List<String>,
    val channelPrefix: String?
) {
    fun toDomain(): FeedbackChannelCreationInput {
        return FeedbackChannelCreationInput(
            title = title,
            speakers = speakers,
            channelTag = channelPrefix ?: "feedback"
        )
    }
}

