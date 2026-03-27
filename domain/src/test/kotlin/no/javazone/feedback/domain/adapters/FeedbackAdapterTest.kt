package no.javazone.feedback.domain.adapters

import no.javazone.feedback.domain.FakeFeedbackRepository
import no.javazone.feedback.domain.FeedbackChannelCreationInput
import no.javazone.feedback.domain.FeedbackChannelRatingCategory
import no.javazone.feedback.domain.SequentialIdGenerator
import no.javazone.feedback.domain.errors.ExternalIdGenerationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FeedbackAdapterTest {

    private val defaultInput = FeedbackChannelCreationInput(
        title = "Test Channel",
        speakers = listOf("Test Speaker"),
        ratings = listOf(
            FeedbackChannelRatingCategory(id = 1, name = "Content")
        )
    )

    @Test
    fun `should create a feedback channel`() {
        val adapter = FeedbackAdapter(
            repository = FakeFeedbackRepository(),
            externalIdGenerator = SequentialIdGenerator("AAAA")
        )

        val channel = adapter.createFeedbackChannel(defaultInput)

        assertEquals("AAAA", channel.externalId)
    }

    @Test
    fun `should succeed on first attempt when no collision`() {
        val adapter = FeedbackAdapter(
            repository = FakeFeedbackRepository(),
            externalIdGenerator = SequentialIdGenerator("ABCD")
        )

        val channel = adapter.createFeedbackChannel(defaultInput)

        assertEquals("ABCD", channel.externalId)
    }

    @Test
    fun `should retry and succeed when external id collides`() {
        val repo = FakeFeedbackRepository(existingExternalIds = mutableSetOf("DUPE"))
        val adapter = FeedbackAdapter(
            repository = repo,
            externalIdGenerator = SequentialIdGenerator("DUPE", "DUPE", "GOOD")
        )

        val channel = adapter.createFeedbackChannel(defaultInput)

        assertEquals("GOOD", channel.externalId)
    }

    @Test
    fun `should throw ExternalIdGenerationException when all retries are exhausted`() {
        val repo = FakeFeedbackRepository(existingExternalIds = mutableSetOf("DUPE"))
        val adapter = FeedbackAdapter(
            repository = repo,
            externalIdGenerator = SequentialIdGenerator("DUPE", "DUPE", "DUPE")
        )

        val exception = assertThrows<ExternalIdGenerationException> {
            adapter.createFeedbackChannel(defaultInput)
        }

        assertEquals(
            "Failed to generate a unique external id after multiple attempts.",
            exception.message
        )
    }
}

