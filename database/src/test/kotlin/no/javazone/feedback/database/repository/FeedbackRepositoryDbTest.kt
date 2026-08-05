package no.javazone.feedback.database.repository

import no.javazone.feedback.database.TestDatabase
import no.javazone.feedback.database.setupDatabase
import no.javazone.feedback.domain.FeedbackChannelCreationInput
import no.javazone.feedback.domain.FeedbackChannelRatingCategory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeedbackRepositoryDbTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestDatabase.start()
            setupDatabase(TestDatabase.config())
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            TestDatabase.stop()
        }
    }

    @BeforeEach
    fun cleanDatabase() {
        TestDatabase.cleanDatabase()
    }

    @Test
    fun `intializeChannel generates a unique 4-char uppercase alphanumeric external id`() {
        val input = FeedbackChannelCreationInput(
            title = "Kotlin Workshop",
            speakers = listOf("Alice"),
            ratings = listOf(FeedbackChannelRatingCategory(name = "Content"))
        )

        val channel = FeedbackRepositoryDb.intializeChannel(input)

        assertTrue(
            channel.externalId.matches(Regex("^[A-Z0-9]{4}$")),
            "External id '${channel.externalId}' does not match [A-Z0-9]{4}"
        )
    }

    @Test
    fun `intializeChannel generates distinct external ids across inserts`() {
        val input = FeedbackChannelCreationInput(
            title = "T",
            speakers = listOf("S"),
            ratings = listOf(FeedbackChannelRatingCategory(name = "R"))
        )

        val a = FeedbackRepositoryDb.intializeChannel(input)
        val b = FeedbackRepositoryDb.intializeChannel(input)

        assertNotEquals(a.externalId, b.externalId)
    }
}
