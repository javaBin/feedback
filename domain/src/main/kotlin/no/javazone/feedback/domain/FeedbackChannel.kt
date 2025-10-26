package no.javazone.feedback.domain

class FeedbackChannel(
    val id: Long = 0,
    val title: String,
    val speakers: List<String>,
    val externalId: String
)