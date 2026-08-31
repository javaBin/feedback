package no.javazone.feedback.request.channel

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackChannelUpdateDTO(
    val title: String? = null,
    val speakers: List<String>? = null,
    val isOpen: Boolean? = null,
    val opensAt: String? = null,
    val closesAt: String? = null,
)
