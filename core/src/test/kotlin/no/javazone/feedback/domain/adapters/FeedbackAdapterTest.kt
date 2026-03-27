package no.javazone.feedback.domain.adapters

import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackChannel
import no.javazone.feedback.domain.FeedbackChannelCreationInput
import no.javazone.feedback.domain.FeedbackChannelRatingCategory
import no.javazone.feedback.domain.generators.ExternalIdGenerator
import no.javazone.feedback.domain.persistence.FeedbackRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FeedbackAdapterTest {
    @Test
    fun `should create a feedback channel`() {
        val feedbackAdapter = FeedbackAdapter(
            repository = FakeFeedbackRepository(),
            externalIdGenerator = StaticIdGenerator()
        )

        val channel = feedbackAdapter.createFeedbackChannel(
            FeedbackChannelCreationInput(
                title = "Test Channel",
                speakers = listOf("Test Speaker"),
                ratings = listOf(
                    FeedbackChannelRatingCategory(
                        id = 1,
                        name = "test"
                    )
                )
            )
        )

        assertEquals("TEST", channel.externalId)
    }

}

private class FakeFeedbackRepository : FeedbackRepository {
    val channels = mutableMapOf<String, FeedbackChannel>()

    override fun intializeChannel(channel: FeedbackChannel): FeedbackChannel {
        channels[channel.externalId] = channel
        return channel
    }

    override fun submitFeedback(
        feedback: Feedback,
        feedbackChannel: FeedbackChannel
    ): Feedback {
        return Feedback(
            id = 1,
            comment = feedback.comment,
            ratings = feedback.ratings
        )
    }

    override fun findByChannelId(channelId: String): FeedbackChannel? {
        return channels[channelId]
    }

    override fun findAllChannels(): List<FeedbackChannel> {
        return channels.values.toList()
    }
}

private class StaticIdGenerator : ExternalIdGenerator {
    override fun generate(): String {
        return "TEST"
    }
}