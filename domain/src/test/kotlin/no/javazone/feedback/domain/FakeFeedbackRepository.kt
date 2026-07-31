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

    override fun updateChannel(channel: FeedbackChannel): FeedbackChannel? {
        val existing = channels[channel.externalId] ?: return null
        val updated = FeedbackChannel(
            id = existing.id,
            title = channel.title,
            speakers = channel.speakers,
            externalId = existing.externalId,
            ratingCategories = existing.ratingCategories,
            isOpen = channel.isOpen
        )
        channels[channel.externalId] = updated
        return updated
    }
}