package no.javazone.feedback.domain

import no.javazone.feedback.request.channel.FeedbackChannelDTO

class FeedbackChannel(
    val id: Long = 0,
    val title: String,
    val speakers: List<String>,
    val externalId: String
) {

    fun toDTO(): FeedbackChannelDTO {
        return FeedbackChannelDTO(
            id = id,
            title = title,
            speakers = speakers,
            externalId = externalId
        )
    }
}