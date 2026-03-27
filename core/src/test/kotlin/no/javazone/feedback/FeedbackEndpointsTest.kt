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

    @Test
    fun `test feedback page returns HTML for valid channel`() = testApplication {
        application {
            module(TestDatabase.config())
        }

        val jsonClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = jsonClient.post("/v1/feedback/channel") {
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Kotlin Coroutines",
                    speakers = listOf("Alice", "Bob"),
                    ratingCategories = listOf(
                        FeedbackChannelRatingCategoryDTO(id = null, title = "Content"),
                        FeedbackChannelRatingCategoryDTO(id = null, title = "Delivery")
                    )
                )
            )
        }.body<FeedbackChannelDTO>()

        val response = client.get("/session/${channel.channelId}")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Text.Html.withCharset(Charsets.UTF_8), response.contentType())

        val body = response.bodyAsText()
        assertTrue(body.contains("Kotlin Coroutines"))
        assertTrue(body.contains("Alice, Bob"))
        assertTrue(body.contains("Content"))
        assertTrue(body.contains("Delivery"))
        assertTrue(body.contains("feedback-form"))
        assertTrue(body.contains("data-channel-id=\"${channel.channelId}\""))
    }

    @Test
    fun `test feedback page returns 404 for non-existent channel`() = testApplication {
        application {
            module(TestDatabase.config())
        }

        val response = client.get("/non-existent-channel")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `test thank you page returns HTML fragment`() = testApplication {
        application {
            module(TestDatabase.config())
        }

        val response = client.get("/session/any-channel/thank-you")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Text.Html.withCharset(Charsets.UTF_8), response.contentType())

        val body = response.bodyAsText()
        assertTrue(body.contains("Thank you!"))
        assertTrue(body.contains("Your feedback has been submitted."))
        assertTrue(body.contains("thank-you"))
    }

    @Test
    fun `test health endpoint returns ok when database is healthy`() = testApplication {
        application {
            module(TestDatabase.config())
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
        assertEquals("ok", body["status"])
    }

    @Test
    fun `test feedback page contains rating inputs for each category`() = testApplication {
        application {
            module(TestDatabase.config())
        }

        val jsonClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = jsonClient.post("/v1/feedback/channel") {
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Microservices Patterns",
                    speakers = listOf("Charlie"),
                    ratingCategories = listOf(
                        FeedbackChannelRatingCategoryDTO(id = null, title = "Depth"),
                        FeedbackChannelRatingCategoryDTO(id = null, title = "Clarity"),
                        FeedbackChannelRatingCategoryDTO(id = null, title = "Pace")
                    )
                )
            )
        }.body<FeedbackChannelDTO>()

        val body = client.get("/session/${channel.channelId}").bodyAsText()

        // Each rating category should have a fieldset with 5 radio inputs
        for (category in channel.ratingCategories) {
            assertTrue(body.contains("rating-${category.id}"), "Missing rating group for ${category.title}")
            for (score in 1..5) {
                assertTrue(
                    body.contains("rating-${category.id}-$score"),
                    "Missing radio button $score for ${category.title}"
                )
            }
        }

        // Should contain the comment textarea
        assertTrue(body.contains("detailed-comment"))
        assertTrue(body.contains("submit-btn"))
    }
}
