package no.javazone.feedback

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import no.javazone.feedback.database.TestDatabase
import no.javazone.feedback.request.channel.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class FeedbackEndpointsTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestDatabase.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            TestDatabase.stop()
        }
    }

    @Test
    fun `test create feedback channel successfully`() = testApplication {
        application {
            module(TestDatabase.config())
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channelCreationDto = FeedbackChannelCreationDTO(
            title = "Introduction to Kotlin",
            speakers = listOf("John Doe", "Jane Smith"),
            channelPrefix = "kotlin",
            ratingCategories = listOf(
                FeedbackChannelRatingCategoryDTO(id = null, title = "Content Quality"),
                FeedbackChannelRatingCategoryDTO(id = null, title = "Presentation")
            )
        )

        val response = client.post("/v1/feedback/channel") {
            contentType(ContentType.Application.Json)
            setBody(channelCreationDto)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.decodeFromString<FeedbackChannelDTO>(response.bodyAsText())
        assertEquals("Introduction to Kotlin", responseBody.title)
        assertEquals(listOf("John Doe", "Jane Smith"), responseBody.speakers)
        assertTrue(responseBody.channelId != null)
        assertEquals(2, responseBody.ratingCategories.size)
    }

    @Test
    fun `test submit feedback successfully`() = testApplication {
        application {
            module(TestDatabase.config())
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        // First, create a channel
        val channelCreationDto = FeedbackChannelCreationDTO(
            title = "Advanced Java",
            speakers = listOf("Alice Brown"),
            channelPrefix = "java",
            ratingCategories = listOf(
                FeedbackChannelRatingCategoryDTO(id = null, title = "Content"),
                FeedbackChannelRatingCategoryDTO(id = null, title = "Delivery")
            )
        )

        val channel = client.post("/v1/feedback/channel") {
            contentType(ContentType.Application.Json)
            setBody(channelCreationDto)
        }.body<FeedbackChannelDTO>()

        val channelId = channel.channelId

        // Now submit feedback
        val feedbackCreationDto = FeedbackCreationDTO(
            ratings = listOf(
                FeedbackRatingCreationDTO(id = channel.ratingCategories[0].id!!, score = 5),
                FeedbackRatingCreationDTO(id = channel.ratingCategories[1].id!!, score = 4)
            ),
            detailedComment = "Great session!"
        )

        val submitFeedbackResponse = client.post("/v1/feedback/channel/$channelId/submit-feedback") {
            contentType(ContentType.Application.Json)
            setBody(feedbackCreationDto)
        }

        assertEquals(HttpStatusCode.OK, submitFeedbackResponse.status)

        val feedback = Json.decodeFromString<FeedbackDTO>(submitFeedbackResponse.bodyAsText())
        assertTrue(feedback.id > 0)
        assertEquals("Great session!", feedback.detailedComment)
        assertEquals(2, feedback.ratings.size)
        assertEquals(5, feedback.ratings[0].score)
        assertEquals(4, feedback.ratings[1].score)
    }

    @Test
    fun `test submit feedback with missing channel id returns not found`() = testApplication {
        application {
            module(TestDatabase.config())
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val feedbackCreationDto = FeedbackCreationDTO(
            ratings = listOf(
                FeedbackRatingCreationDTO(id = 1, score = 5)
            ),
            detailedComment = "Test feedback"
        )

        val response = client.post("/v1/feedback/channel/INVALID/submit-feedback") {
            contentType(ContentType.Application.Json)
            setBody(feedbackCreationDto)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `test submit feedback without comment`() = testApplication {
        application {
            module(TestDatabase.config())
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        // First, create a channel
        val channelCreationDto = FeedbackChannelCreationDTO(
            title = "Spring Boot Basics",
            speakers = listOf("Bob Wilson"),
            channelPrefix = "spring",
            ratingCategories = listOf(
                FeedbackChannelRatingCategoryDTO(id = null, title = "Technical Depth")
            )
        )

        val createChannelResponse = client.post("/v1/feedback/channel") {
            contentType(ContentType.Application.Json)
            setBody(channelCreationDto)
        }

        val channel = createChannelResponse.body<FeedbackChannelDTO>()
        val channelId = channel.channelId

        // Submit feedback without comment
        val feedbackCreationDto = FeedbackCreationDTO(
            ratings = listOf(
                FeedbackRatingCreationDTO(id = channel.ratingCategories[0].id!!, score = 3)
            ),
            detailedComment = null
        )

        val submitFeedbackResponse = client.post("/v1/feedback/channel/$channelId/submit-feedback") {
            contentType(ContentType.Application.Json)
            setBody(feedbackCreationDto)
        }

        assertEquals(HttpStatusCode.OK, submitFeedbackResponse.status)

        val feedback = Json.decodeFromString<FeedbackDTO>(submitFeedbackResponse.bodyAsText())
        assertTrue(feedback.detailedComment == null)
        assertEquals(1, feedback.ratings.size)
    }

    @Test
    fun `test create multiple channels and verify unique external ids`() = testApplication {
        application {
            module(TestDatabase.config())
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val externalIds = mutableSetOf<String>()

        repeat(5) { index ->
            val channelCreationDto = FeedbackChannelCreationDTO(
                title = "Session $index",
                speakers = listOf("Speaker $index"),
                channelPrefix = "test",
                ratingCategories = listOf(
                    FeedbackChannelRatingCategoryDTO(id = null, title = "Rating")
                )
            )

            val response = client.post("/v1/feedback/channel") {
                contentType(ContentType.Application.Json)
                setBody(channelCreationDto)
            }

            assertEquals(HttpStatusCode.OK, response.status)

            val channel = response.body<FeedbackChannelDTO>()
            externalIds.add(channel.channelId)
        }

        // All external IDs should be unique
        assertEquals(5, externalIds.size)
    }
}
