package no.javazone.feedback.request.channel

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackRatingDTO(
    val id: Long,
    val category: FeedbackChannelRatingCategoryDTO,
    val score: Int
)