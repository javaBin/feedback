package no.javazone.feedback.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class FeedbackChannelTest {
    private val ratings = listOf(FeedbackChannelRatingCategory(id = 1, name = "Content"))

    private fun channel(
        isOpen: Boolean = true,
        opensAt: Instant? = null,
        closesAt: Instant? = null,
    ) = FeedbackChannel(
        title = "T",
        speakers = listOf("S"),
        externalId = "ABCD",
        ratingCategories = ratings,
        isOpen = isOpen,
        opensAt = opensAt,
        closesAt = closesAt,
    )

    @Test
    fun `open with no schedule is effectively open`() {
        assertTrue(channel().isEffectivelyOpen(Instant.parse("2026-09-01T10:00:00Z")))
    }

    @Test
    fun `master switch off is never effectively open`() {
        val c = channel(isOpen = false, opensAt = Instant.parse("2026-09-01T09:00:00Z"))
        assertFalse(c.isEffectivelyOpen(Instant.parse("2026-09-01T10:00:00Z")))
    }

    @Test
    fun `before opensAt is not effectively open`() {
        val c = channel(opensAt = Instant.parse("2026-09-01T09:00:00Z"))
        assertFalse(c.isEffectivelyOpen(Instant.parse("2026-09-01T08:59:59Z")))
    }

    @Test
    fun `at exactly opensAt is effectively open`() {
        val opens = Instant.parse("2026-09-01T09:00:00Z")
        assertTrue(channel(opensAt = opens).isEffectivelyOpen(opens))
    }

    @Test
    fun `between opensAt and closesAt is effectively open`() {
        val c = channel(
            opensAt = Instant.parse("2026-09-01T09:00:00Z"),
            closesAt = Instant.parse("2026-09-01T10:00:00Z"),
        )
        assertTrue(c.isEffectivelyOpen(Instant.parse("2026-09-01T09:30:00Z")))
    }

    @Test
    fun `at exactly closesAt is not effectively open`() {
        val closes = Instant.parse("2026-09-01T10:00:00Z")
        val c = channel(closesAt = closes)
        assertFalse(c.isEffectivelyOpen(closes))
    }

    @Test
    fun `after closesAt is not effectively open`() {
        val c = channel(closesAt = Instant.parse("2026-09-01T10:00:00Z"))
        assertFalse(c.isEffectivelyOpen(Instant.parse("2026-09-01T10:00:01Z")))
    }

    @Test
    fun `closesAt not after opensAt throws`() {
        val t = Instant.parse("2026-09-01T09:00:00Z")
        assertThrows<IllegalArgumentException> { channel(opensAt = t, closesAt = t) }
        assertThrows<IllegalArgumentException> {
            channel(opensAt = t, closesAt = t.minusSeconds(1))
        }
    }

    @Test
    fun `only closesAt set behaves correctly`() {
        val c = channel(closesAt = Instant.parse("2026-09-01T10:00:00Z"))
        assertTrue(c.isEffectivelyOpen(Instant.parse("2026-09-01T09:00:00Z")))
        assertFalse(c.isEffectivelyOpen(Instant.parse("2026-09-01T11:00:00Z")))
        // sanity
        assertEquals(true, c.isOpen)
    }
}
