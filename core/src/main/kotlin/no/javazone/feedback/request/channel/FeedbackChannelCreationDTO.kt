package no.javazone.feedback.request.channel

import kotlinx.serialization.Serializable
import no.javazone.feedback.domain.FeedbackChannelCreationInput
import java.time.Instant

@Serializable
data class FeedbackChannelCreationDTO(
    val title: String,
    val speakers: List<String>,
    val ratingCategories: List<FeedbackChannelRatingCategoryDTO>,
    val opensAt: String? = null,
    val closesAt: String? = null,
    val remoteId: String? = null,
) {
    fun toDomain(): FeedbackChannelCreationInput {
        return FeedbackChannelCreationInput(
            title = title,
            speakers = speakers,
            ratings = ratingCategories.map { it.toDomain() },
            opensAt = opensAt?.let { Instant.parse(it) },
            closesAt = closesAt?.let { Instant.parse(it) },
            remoteId = remoteId,
        )
    }
}
