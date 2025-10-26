package no.javazone.feedback.domain

data class FeedbackChannelCreationInput(
    val title: String,
    val speakers: List<String>,
    val channelTag: String
)