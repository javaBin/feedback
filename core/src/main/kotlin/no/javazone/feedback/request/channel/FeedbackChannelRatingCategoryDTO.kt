package no.javazone.feedback.request.channel

import kotlinx.serialization.Serializable
import no.javazone.feedback.domain.FeedbackChannelRatingCategory

@Serializable
data class FeedbackChannelRatingCategoryDTO(
    val id: Long? = null,
    val title: String,
) {
    fun toDomain(): FeedbackChannelRatingCategory = FeedbackChannelRatingCategory(
        name = title
    )
}
