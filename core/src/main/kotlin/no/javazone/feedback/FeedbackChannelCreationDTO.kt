package no.javazone.feedback

import java.util.UUID

data class FeedbackChannelCreationDTO(
    val title: String,
    val speakers: List<String>,
    val externalId: UUID
)

data class FeedbackChannelDTO(
    val id: Long,
    val title: String,
    val speakers: List<String>,
    val externalId: UUID
)

