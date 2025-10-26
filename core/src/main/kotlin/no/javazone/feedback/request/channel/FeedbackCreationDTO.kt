package no.javazone.feedback.request.channel

import kotlinx.serialization.Serializable
import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackRating

@Serializable
data class FeedbackCreationDTO(
    val ratings: List<FeedbackRatingCreationDTO>,
    val detailedComment: String?
) {
    fun toDomain() = Feedback(comment = detailedComment, ratings = ratings.map { it.toDomain() })
}

@Serializable
data class FeedbackRatingCreationDTO(
    val id: Long,
    val score: Int,
) {
    fun toDomain() = FeedbackRating(id = id, name = "", value = score)
}