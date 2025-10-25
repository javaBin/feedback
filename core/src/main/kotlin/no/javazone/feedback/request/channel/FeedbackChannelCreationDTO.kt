package no.javazone.feedback.request.channel

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackChannelCreationDTO(
    val title: String,
    val speakers: List<String>,
    val channelPrefix: String?
)

