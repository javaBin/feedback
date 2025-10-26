package no.javazone.feedback.adapter

import no.javazone.feedback.domain.ExternalIdGenerator
import no.javazone.feedback.domain.FeedbackChannel
import no.javazone.feedback.domain.FeedbackRepository
import no.javazone.feedback.request.channel.FeedbackChannelCreationDTO

class FeedbackAdapterDefault(
    private val repository: FeedbackRepository,
    private val externalIdGenerator: ExternalIdGenerator
) : FeedbackAdapter {
    override fun createFeedbackChannel(input: FeedbackChannelCreationDTO): FeedbackChannel {
        val channel = FeedbackChannel(
            title = input.title,
            speakers = input.speakers,
            externalId = "${input.channelPrefix}-${externalIdGenerator.generate()}"
        )
        return repository.intializeChannel(channel)
    }
}

