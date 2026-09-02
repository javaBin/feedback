package no.javazone.feedback.domain.adapters

import no.javazone.feedback.domain.FakeFeedbackRepository
import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackChannel
import no.javazone.feedback.domain.FeedbackChannelCreationInput
import no.javazone.feedback.domain.FeedbackChannelRatingCategory
import no.javazone.feedback.domain.FeedbackRating
import no.javazone.feedback.domain.errors.ChannelClosedError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

@Disabled
class FeedbackAdapterTest {

    private val defaultInput = FeedbackChannelCreationInput(
        title = "Test Channel",
        speakers = listOf("Test Speaker"),
        ratings = listOf(
            FeedbackChannelRatingCategory(id = 1, name = "Content")
        )
    )

    @Test
    fun `should create a feedback channel with a repository-assigned external id`() {
        val adapter = FeedbackAdapter(
            repository = FakeFeedbackRepository(sequenceOf("AAAA").iterator()),
        )

        val channel = adapter.createFeedbackChannel(defaultInput)

        assertEquals("AAAA", channel.externalId)
    }

    @Test
    fun `should return channel with populated external id`() {
        val adapter = FeedbackAdapter(
            repository = FakeFeedbackRepository(),
        )

        val channel = adapter.createFeedbackChannel(defaultInput)

        assertNotNull(channel.externalId)
        assert(channel.externalId.isNotEmpty())
    }

    private fun openChannelInRepo(
        repo: FakeFeedbackRepository,
        opensAt: Instant?,
        closesAt: Instant?,
        isOpen: Boolean = true,
    ) {
        repo.insertExisting(
            FeedbackChannel(
                title = "T",
                speakers = listOf("S"),
                externalId = "SCHD",
                ratingCategories = listOf(FeedbackChannelRatingCategory(id = 1, name = "R")),
                isOpen = isOpen,
                opensAt = opensAt,
                closesAt = closesAt,
            )
        )
    }

    private fun sampleFeedback() =
        Feedback(comment = null, ratings = listOf(FeedbackRating(id = 0, name = "R", typeId = 1, value = 5)))

    @Test
    fun `submitFeedback throws before opensAt`() {
        val repo = FakeFeedbackRepository()
        val opens = Instant.parse("2026-09-01T09:00:00Z")
        openChannelInRepo(repo, opensAt = opens, closesAt = null)
        val adapter = FeedbackAdapter(repository = repo)
        assertThrows<ChannelClosedError> {
            adapter.submitFeedback("SCHD", sampleFeedback(), Instant.parse("2026-09-01T08:59:59Z"))
        }
    }

    @Test
    fun `submitFeedback succeeds inside window`() {
        val repo = FakeFeedbackRepository()
        openChannelInRepo(
            repo,
            opensAt = Instant.parse("2026-09-01T09:00:00Z"),
            closesAt = Instant.parse("2026-09-01T10:00:00Z"),
        )
        val adapter = FeedbackAdapter(repository = repo)
        val result = adapter.submitFeedback(
            "SCHD",
            sampleFeedback(),
            Instant.parse("2026-09-01T09:30:00Z"),
        )
        assertEquals("SCHD", result.channel.externalId)
    }

    @Test
    fun `submitFeedback throws at or after closesAt`() {
        val repo = FakeFeedbackRepository()
        val closes = Instant.parse("2026-09-01T10:00:00Z")
        openChannelInRepo(repo, opensAt = null, closesAt = closes)
        val adapter = FeedbackAdapter(repository = repo)
        assertThrows<ChannelClosedError> {
            adapter.submitFeedback("SCHD", sampleFeedback(), closes)
        }
    }

    @Test
    fun `submitFeedback throws when master switch is off even in window`() {
        val repo = FakeFeedbackRepository()
        openChannelInRepo(
            repo,
            opensAt = Instant.parse("2026-09-01T09:00:00Z"),
            closesAt = Instant.parse("2026-09-01T10:00:00Z"),
            isOpen = false,
        )
        val adapter = FeedbackAdapter(repository = repo)
        assertThrows<ChannelClosedError> {
            adapter.submitFeedback("SCHD", sampleFeedback(), Instant.parse("2026-09-01T09:30:00Z"))
        }
    }
}
