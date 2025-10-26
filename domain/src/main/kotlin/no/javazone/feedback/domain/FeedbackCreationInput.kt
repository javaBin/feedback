package no.javazone.feedback.domain

data class FeedbackChannelCreationInput(
    val title: String,
    val speakers: List<String>,
    val channelTag: String,
    val ratings: List<FeedbackChannelRatingCategory>
) {
    init {
        require(ratings.isNotEmpty()) {
            "At least one rating must be provided"
        }
    }
}