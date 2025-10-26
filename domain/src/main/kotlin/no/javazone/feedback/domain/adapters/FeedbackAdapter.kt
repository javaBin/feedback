package no.javazone.feedback.domain.adapters

import no.javazone.feedback.domain.FeedbackChannel
import no.javazone.feedback.domain.FeedbackChannelCreationInput
import no.javazone.feedback.domain.generators.ExternalIdGenerator
import no.javazone.feedback.domain.persistence.FeedbackRepository

class FeedbackAdapter(
    private val repository: FeedbackRepository,
    private val externalIdGenerator: ExternalIdGenerator
) {
    fun createFeedbackChannel(input: FeedbackChannelCreationInput): FeedbackChannel {
        val channel = FeedbackChannel(
            title = input.title,
            speakers = input.speakers,
            externalId = "${input.channelTag}-${externalIdGenerator.generate()}"
        )
        return repository.intializeChannel(channel)
    }
}