package no.javazone.feedback.domain

import java.time.Instant

data class FeedbackChannelCreationInput(
    val title: String,
    val speakers: List<String>,
    val ratings: List<FeedbackChannelRatingCategory>,
    val opensAt: Instant? = null,
    val closesAt: Instant? = null,
    val remoteId: String? = null,
) {
    init {
        require(ratings.isNotEmpty()) {
            "At least one rating must be provided"
        }
        require(opensAt == null || closesAt == null || closesAt.isAfter(opensAt)) {
            "closesAt must be after opensAt."
        }
    }
}
