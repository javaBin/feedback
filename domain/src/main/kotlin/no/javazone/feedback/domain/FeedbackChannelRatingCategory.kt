package no.javazone.feedback.domain

class FeedbackChannelRatingCategory(
    val id: Long = 0,
    val name: String,
) {
    init {
        require(name.isNotEmpty()) {
            "Rating category name must not be empty"
        }
    }
}