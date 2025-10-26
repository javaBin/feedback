package no.javazone.feedback.domain

class Feedback(
    val id: Long = 0,
    val comment: String?,
    val ratings: List<FeedbackRating>
)