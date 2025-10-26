package no.javazone.feedback.request.channel

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackCreationDTO(
    val ratings: List<FeedbackRatingDTO>,
    val detailedComment: String?
)

@Serializable
data class FeedbackRatingDTO(
    val id: Long,
    val rating: Int,
)