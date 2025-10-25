package no.javazone.feedback

data class FeedbackChannelCreationDTO(
    val title: String,
    val speakers: List<String>,
    val channelPrefix: String?
)

data class FeedbackChannelDTO(
    val id: Long,
    val title: String,
    val speakers: List<String>,
    val externalId: String?
)

class FeedbackChannel(
    val id: Long,
    val title: String,
    val speakers: List<String>,
    channelPrefix: String?
) {
    val externalId: String = channelPrefix?.let { "$it-id" } ?: id.toString()

    fun toDTO(): FeedbackChannelDTO {
        return FeedbackChannelDTO(
            id = id,
            title = title,
            speakers = speakers,
            externalId = externalId
        )
    }
}

class Feedback(val ratings: FeedbackRatings, val detailedComment: String?)

class FeedbackRatings(val enjoyment: Int, val usefulness: Int)

