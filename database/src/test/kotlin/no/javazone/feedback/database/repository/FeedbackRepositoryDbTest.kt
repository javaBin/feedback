package no.javazone.feedback.database.repository

import no.javazone.feedback.database.TestDatabase
import no.javazone.feedback.database.setupDatabase
import no.javazone.feedback.domain.FeedbackChannel
import no.javazone.feedback.domain.FeedbackChannelRatingCategory
import no.javazone.feedback.domain.errors.ExternalIdAlreadyExistsError
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

    @Test
    fun `should throw ExternalIdAlreadyExistsError when inserting channel with duplicate external id`() {
        val channel = FeedbackChannel(
            title = "Kotlin Workshop",
            speakers = listOf("Alice"),
            externalId = "DUPE",
            ratingCategories = listOf(
                FeedbackChannelRatingCategory(name = "Content")
            )
        )

        FeedbackRepositoryDb.intializeChannel(channel)

        val duplicateChannel = FeedbackChannel(
            title = "Another Workshop",
            speakers = listOf("Bob"),
            externalId = "DUPE",
            ratingCategories = listOf(
                FeedbackChannelRatingCategory(name = "Delivery")
            )
        )

        val exception = assertThrows<ExternalIdAlreadyExistsError> {
            FeedbackRepositoryDb.intializeChannel(duplicateChannel)
        }

        assertEquals("Channel with external id DUPE already exists.", exception.message)
        assertNotNull(exception.cause)
        assertInstanceOf(ExposedSQLException::class.java, exception.cause)
    }
}
