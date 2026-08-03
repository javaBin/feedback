package no.javazone.feedback.domain

import java.time.Instant

class FeedbackChannel(
    val id: Long = 0,
    val title: String,
    val speakers: List<String>,
    val externalId: String,
    val ratingCategories: List<FeedbackChannelRatingCategory>,
    val isOpen: Boolean = false,
    val opensAt: Instant? = null,
    val closesAt: Instant? = null,
) {
    init {
        require(speakers.all { it.isNotEmpty() }) { "All speakers must not be empty." }
        require(title.isNotEmpty()) { "Title must not be empty." }
        require(opensAt == null || closesAt == null || closesAt.isAfter(opensAt)) {
            "closesAt must be after opensAt."
        }
    }

    fun isEffectivelyOpen(now: Instant): Boolean =
        isOpen &&
            (opensAt == null || !now.isBefore(opensAt)) &&
            (closesAt == null || now.isBefore(closesAt))
}
