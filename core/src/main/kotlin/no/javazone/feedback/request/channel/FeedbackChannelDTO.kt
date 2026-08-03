package no.javazone.feedback.request.channel

import kotlinx.serialization.Serializable
import no.javazone.feedback.domain.FeedbackChannel

@Serializable
data class FeedbackChannelDTO(
    val title: String,
    val speakers: List<String>,
    val channelId: String,
    val ratingCategories: List<FeedbackChannelRatingCategoryDTO>,
    val isOpen: Boolean,
    val opensAt: String? = null,
    val closesAt: String? = null,
)

fun FeedbackChannel.toDTO(): FeedbackChannelDTO {
    return FeedbackChannelDTO(
        title = title,
        speakers = speakers,
        channelId = externalId,
        ratingCategories = ratingCategories.map {
            FeedbackChannelRatingCategoryDTO(
                title = it.name,
                id = it.id,
            )
        },
        isOpen = isOpen,
        opensAt = opensAt?.toString(),
        closesAt = closesAt?.toString(),
    )
}
