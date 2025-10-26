package no.javazone.feedback.request.channel

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackDTO(
    val id: Long,
    val channel: FeedbackChannelDTO,
    val detailedComment: String?,
    val ratings: List<FeedbackRatingDTO>
)