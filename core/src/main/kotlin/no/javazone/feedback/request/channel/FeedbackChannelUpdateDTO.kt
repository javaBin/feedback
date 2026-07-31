package no.javazone.feedback.request.channel

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackChannelUpdateDTO(
    val isOpen: Boolean? = null
)
