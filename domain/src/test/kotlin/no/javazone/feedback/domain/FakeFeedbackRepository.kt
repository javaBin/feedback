package no.javazone.feedback.domain

import no.javazone.feedback.domain.persistence.FeedbackRepository

internal class FakeFeedbackRepository(
    private val externalIdSequence: Iterator<String> = generateSequence(0) { it + 1 }
        .map { "T%03d".format(it) }
        .iterator()
) : FeedbackRepository {
    private val channels = mutableMapOf<String, FeedbackChannel>()
    private var nextId = 1L

    override fun intializeChannel(input: FeedbackChannelCreationInput): FeedbackChannel {
        val channel = FeedbackChannel(
            id = nextId++,
            title = input.title,
            speakers = input.speakers,
            externalId = externalIdSequence.next(),
            ratingCategories = input.ratings,
            opensAt = input.opensAt,
            closesAt = input.closesAt,
        )
        channels[channel.externalId] = channel
        return channel
    }

    fun insertExisting(channel: FeedbackChannel) {
        channels[channel.externalId] = channel
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
            isOpen = channel.isOpen,
            opensAt = channel.opensAt,
            closesAt = channel.closesAt,
        )
        channels[channel.externalId] = updated
        return updated
    }

    override fun findFeedbacksByChannelId(channelId: Long): List<Feedback> = emptyList()

    override fun findFeedbackById(feedbackId: Long): Feedback? = null

    override fun updateFeedback(feedbackId: Long, comment: String?, ratings: Map<Long, Int>): Feedback? = null

    override fun deleteFeedback(feedbackId: Long): Boolean = false
}
