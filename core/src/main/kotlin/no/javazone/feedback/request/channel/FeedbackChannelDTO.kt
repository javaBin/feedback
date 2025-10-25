package no.javazone.feedback.request.channel

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackChannelDTO(
    val id: Long,
    val title: String,
    val speakers: List<String>,
    val externalId: String?
)