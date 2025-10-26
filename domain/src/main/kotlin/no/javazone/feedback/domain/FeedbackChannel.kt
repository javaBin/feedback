package no.javazone.feedback.domain

class FeedbackChannel(
    val id: Long = 0,
    val title: String,
    val speakers: List<String>,
    val externalId: String,
    val ratings: List<FeedbackChannelRatingCategory>
) {
    init {
        require(speakers.all { it.isNotEmpty() }) { "All speakers must not be empty." }
        require(title.isNotEmpty()) { "Title must not be empty." }
        require(ratings.isNotEmpty()) { "Ratings must not be empty." }
    }
}