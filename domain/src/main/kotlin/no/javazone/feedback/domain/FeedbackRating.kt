package no.javazone.feedback.domain

class FeedbackRating(
    val id: Long = 0,
    val name: String,
    val typeId: Long = 0,
    val value: Int,
) {
    companion object {
        fun new(name: String, value: Int): FeedbackRating {
            return FeedbackRating(
                name = name,
                value = value
            )
        }
    }
}
