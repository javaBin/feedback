package no.javazone.feedback.domain

import no.javazone.feedback.domain.errors.ExternalIdAlreadyExistsError
import no.javazone.feedback.domain.persistence.FeedbackRepository

internal class FakeFeedbackRepository(
    private val existingExternalIds: MutableSet<String> = mutableSetOf()
) : FeedbackRepository {
    private val channels = mutableMapOf<String, FeedbackChannel>()

    override fun intializeChannel(channel: FeedbackChannel): FeedbackChannel {
        if (channel.externalId in existingExternalIds) {
            throw ExternalIdAlreadyExistsError(channel.externalId)
        }
        existingExternalIds.add(channel.externalId)
        channels[channel.externalId] = channel
        return channel
    }

    override fun submitFeedback(feedback: Feedback, feedbackChannel: FeedbackChannel): Feedback {
        return Feedback(id = 1, comment = feedback.comment, ratings = feedback.ratings)
    }

    override fun findByChannelId(channelId: String): FeedbackChannel? {
        return channels[channelId]
    }

    override fun findAllChannels(): List<FeedbackChannel> {
        return channels.values.toList()
    }
}