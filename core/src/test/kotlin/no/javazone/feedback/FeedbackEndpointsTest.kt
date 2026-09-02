package no.javazone.feedback

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

@Disabled
class FeedbackEndpointsTest {
    companion object {
        private const val ADMIN_USER = "test-admin"
        private const val ADMIN_PASS = "test-password"
        private val testAuthConfig = AuthConfig(ADMIN_USER, ADMIN_PASS)

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

    private fun HttpRequestBuilder.adminAuth(
        user: String = ADMIN_USER,
        password: String = ADMIN_PASS,
    ) {
        val token = Base64.getEncoder().encodeToString("$user:$password".toByteArray())
        header(HttpHeaders.Authorization, "Basic $token")
    }

    @BeforeEach
    fun cleanDatabase() {
        TestDatabase.cleanDatabase()
    }

    @Test
    fun `test create feedback channel successfully`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
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
            adminAuth()
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
    fun `newly created channel is closed by default`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = client.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Closed by default",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        assertEquals(false, channel.isOpen)
    }

    @Test
    fun `create feedback channel with remoteId returns remoteId in response`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val remoteId = "550e8400-e29b-41d4-a716-446655440000"

        val channel = client.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Talk with remote id",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating")),
                    remoteId = remoteId,
                )
            )
        }.body<FeedbackChannelDTO>()

        assertEquals(remoteId, channel.remoteId)
    }

    @Test
    fun `create feedback channel without remoteId leaves it null`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = client.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Talk without remote id",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        assertEquals(null, channel.remoteId)
    }

    @Test
    fun `submitting feedback to closed channel returns forbidden`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = client.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Closed",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val response = client.post("/v1/feedback/channel/${channel.channelId}/submit-feedback") {
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackCreationDTO(
                    ratings = listOf(FeedbackRatingCreationDTO(id = channel.ratingCategories[0].id!!, score = 5)),
                    detailedComment = null
                )
            )
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `patch channel opens the channel and allows submissions`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = client.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Toggle me",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val patched = client.patch("/v1/feedback/channel/${channel.channelId}") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(isOpen = true))
        }

        assertEquals(HttpStatusCode.OK, patched.status)
        assertEquals(true, patched.body<FeedbackChannelDTO>().isOpen)

        val submit = client.post("/v1/feedback/channel/${channel.channelId}/submit-feedback") {
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackCreationDTO(
                    ratings = listOf(FeedbackRatingCreationDTO(id = channel.ratingCategories[0].id!!, score = 5)),
                    detailedComment = null
                )
            )
        }
        assertEquals(HttpStatusCode.OK, submit.status)
    }

    @Test
    fun `patch channel updates title and speakers`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = client.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Original title",
                    speakers = listOf("Original Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val patched = client.patch("/v1/feedback/channel/${channel.channelId}") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelUpdateDTO(
                    title = "Updated title",
                    speakers = listOf("New Speaker", "Another Speaker"),
                )
            )
        }

        assertEquals(HttpStatusCode.OK, patched.status)
        val body = patched.body<FeedbackChannelDTO>()
        assertEquals("Updated title", body.title)
        assertEquals(listOf("New Speaker", "Another Speaker"), body.speakers)
    }

    @Test
    fun `patch channel with empty body leaves title and speakers unchanged`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = client.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Keep me",
                    speakers = listOf("Keep Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val patched = client.patch("/v1/feedback/channel/${channel.channelId}") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(isOpen = true))
        }

        assertEquals(HttpStatusCode.OK, patched.status)
        val body = patched.body<FeedbackChannelDTO>()
        assertEquals("Keep me", body.title)
        assertEquals(listOf("Keep Speaker"), body.speakers)
    }

    @Test
    fun `patch channel with blank title returns bad request`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = client.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Valid title",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val patched = client.patch("/v1/feedback/channel/${channel.channelId}") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(title = ""))
        }

        assertEquals(HttpStatusCode.BadRequest, patched.status)
    }

    @Test
    fun `patch channel with blank speaker returns bad request`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = client.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Valid title",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val patched = client.patch("/v1/feedback/channel/${channel.channelId}") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(speakers = listOf("")))
        }

        assertEquals(HttpStatusCode.BadRequest, patched.status)
    }

    @Test
    fun `patch channel with empty speakers list is allowed`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = client.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Valid title",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val patched = client.patch("/v1/feedback/channel/${channel.channelId}") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(speakers = emptyList()))
        }

        assertEquals(HttpStatusCode.OK, patched.status)
        val body = patched.body<FeedbackChannelDTO>()
        assertEquals(emptyList<String>(), body.speakers)
    }

    @Test
    fun `patch channel returns not found for unknown channel`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.patch("/v1/feedback/channel/ZZZZ") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(isOpen = true))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `create channel without auth returns unauthorized`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.post("/v1/feedback/channel") {
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Unauthorized",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating")),
                )
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `create channel with wrong credentials returns unauthorized`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.post("/v1/feedback/channel") {
            adminAuth(user = "wrong", password = "wrong")
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Unauthorized",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating")),
                )
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `patch channel without auth returns unauthorized`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.patch("/v1/feedback/channel/ABCD") {
            contentType(ContentType.Application.Json)
            setBody("""{"isOpen": true}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `qrcode endpoint does not require auth`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = client.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "QR",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating")),
                )
            )
        }.body<FeedbackChannelDTO>()

        val response = client.get("/v1/feedback/channel/${channel.channelId}/qrcode")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test submit feedback successfully`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
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
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(channelCreationDto)
        }.body<FeedbackChannelDTO>()

        val channelId = channel.channelId

        client.patch("/v1/feedback/channel/$channelId") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(isOpen = true))
        }

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
            module(TestDatabase.config(), testAuthConfig)
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
            module(TestDatabase.config(), testAuthConfig)
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
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(channelCreationDto)
        }

        val channel = createChannelResponse.body<FeedbackChannelDTO>()
        val channelId = channel.channelId

        client.patch("/v1/feedback/channel/$channelId") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(isOpen = true))
        }

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
            module(TestDatabase.config(), testAuthConfig)
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
                adminAuth()
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
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = jsonClient.post("/v1/feedback/channel") {
            adminAuth()
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

        jsonClient.patch("/v1/feedback/channel/${channel.channelId}") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(isOpen = true))
        }

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
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.get("/session/ZZZZ")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `test session endpoint returns bad request for channel id longer than four characters`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.get("/session/ABCDE")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `test session endpoint returns bad request for channel id shorter than four characters`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.get("/session/ABC")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `feedback page shows closed notice when channel is not open`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = jsonClient.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Not yet open",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val response = client.get("/session/${channel.channelId}")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Feedback is not open yet"))
        assertTrue(body.contains("Please check again closer to when the session starts"))
        assertTrue(!body.contains("feedback-form"))
        assertTrue(!body.contains("submit-btn"))
        assertTrue(!body.contains("/static/js/feedback.js"))
    }

    @Test
    fun `feedback page shows form after channel is opened`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = jsonClient.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Opened",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        jsonClient.patch("/v1/feedback/channel/${channel.channelId}") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(isOpen = true))
        }

        val body = client.get("/session/${channel.channelId}").bodyAsText()

        assertTrue(body.contains("feedback-form"))
        assertTrue(body.contains("submit-btn"))
        assertTrue(!body.contains("Feedback is not open yet"))
    }

    @Test
    fun `test thank you page returns HTML fragment`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
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
            module(TestDatabase.config(), testAuthConfig)
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
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val channel = jsonClient.post("/v1/feedback/channel") {
            adminAuth()
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

        jsonClient.patch("/v1/feedback/channel/${channel.channelId}") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(isOpen = true))
        }

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

    @Test
    fun `qrcode endpoint returns not found for unknown channel`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.get("/v1/feedback/channel/ZZZZ/qrcode")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `patch channel with wrong credentials returns unauthorized`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.patch("/v1/feedback/channel/ABCD") {
            adminAuth(user = "wrong", password = "wrong")
            contentType(ContentType.Application.Json)
            setBody("""{"isOpen": true}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `patch channel with empty body leaves isOpen unchanged`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) { json() }
        }

        val channel = jsonClient.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Untouched",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        // Open the channel first
        jsonClient.patch("/v1/feedback/channel/${channel.channelId}") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(isOpen = true))
        }

        // Send an empty update body
        val patched = jsonClient.patch("/v1/feedback/channel/${channel.channelId}") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(isOpen = null))
        }
        assertEquals(HttpStatusCode.OK, patched.status)
        assertEquals(true, patched.body<FeedbackChannelDTO>().isOpen)
    }

    @Test
    fun `admin dashboard returns HTML and lists created channels`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) { json() }
        }

        val channel = jsonClient.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Landing showcase",
                    speakers = listOf("Alice"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val response = client.get("/admin") { adminAuth() }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Text.Html.withCharset(Charsets.UTF_8), response.contentType())

        val body = response.bodyAsText()
        assertTrue(body.contains("Admin dashboard"))
        assertTrue(body.contains("Landing showcase"))
        assertTrue(body.contains("Alice"))
        assertTrue(body.contains("/admin/channel/${channel.channelId}"))
        assertTrue(!body.contains("href=\"/session/${channel.channelId}\""))
    }

    @Test
    fun `admin dashboard shows empty state when no channels exist`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.get("/admin") { adminAuth() }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("No feedback channels have been created yet."))
    }

    @Test
    fun `admin dashboard without auth returns unauthorized`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.get("/admin")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `landing page shows code form without auth and reveals no channels`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) { json() }
        }
        jsonClient.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Secret session",
                    speakers = listOf("Alice"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val response = client.get("/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Text.Html.withCharset(Charsets.UTF_8), response.contentType())

        val body = response.bodyAsText()
        assertTrue(body.contains("name=\"code\""))
        assertTrue(!body.contains("Secret session"))
        assertTrue(!body.contains("Admin dashboard"))
    }

    @Test
    fun `posting a valid code redirects to the session page`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) { json() }
        }
        val channel = jsonClient.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Code lookup",
                    speakers = listOf("Alice"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val redirectClient = createClient { followRedirects = false }

        val response = redirectClient.submitForm(
            url = "/",
            formParameters = parameters { append("code", channel.channelId) },
        )

        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals("/session/${channel.channelId}", response.headers[HttpHeaders.Location])
    }

    @Test
    fun `posting a lowercase code is normalized to uppercase`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) { json() }
        }
        val channel = jsonClient.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Lowercase lookup",
                    speakers = listOf("Alice"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val redirectClient = createClient { followRedirects = false }

        val response = redirectClient.submitForm(
            url = "/",
            formParameters = parameters { append("code", " ${channel.channelId.lowercase()} ") },
        )

        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals("/session/${channel.channelId}", response.headers[HttpHeaders.Location])
    }

    @Test
    fun `posting an unknown code returns not found with an error message`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.submitForm(
            url = "/",
            formParameters = parameters { append("code", "ZZZZ") },
        )

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Fant ingen sesjon med koden"))
    }

    @Test
    fun `posting a code of wrong length returns bad request`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val tooShort = client.submitForm(
            url = "/",
            formParameters = parameters { append("code", "AB") },
        )
        assertEquals(HttpStatusCode.BadRequest, tooShort.status)
        assertTrue(tooShort.bodyAsText().contains("Koden må være 4 tegn"))

        val missing = client.submitForm(url = "/", formParameters = parameters { })
        assertEquals(HttpStatusCode.BadRequest, missing.status)
    }

    @Test
    fun `session page without auth does not show edit-in-admin link`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) { json() }
        }

        val channel = jsonClient.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Anon view",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val response = client.get("/session/${channel.channelId}")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(!body.contains("Edit in admin"))
        assertTrue(!body.contains("admin-edit-link"))
    }

    @Test
    fun `session page with admin auth shows edit-in-admin link`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) { json() }
        }

        val channel = jsonClient.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Admin view",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val response = client.get("/session/${channel.channelId}") { adminAuth() }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Edit in admin"))
        assertTrue(body.contains("/admin/channel/${channel.channelId}"))
    }

    @Test
    fun `session page with invalid credentials returns unauthorized`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) { json() }
        }

        val channel = jsonClient.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = "Bad creds",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Rating"))
                )
            )
        }.body<FeedbackChannelDTO>()

        val response = client.get("/session/${channel.channelId}") {
            adminAuth(user = "wrong", password = "wrong")
        }

        // With optional auth, providing wrong credentials is still a hard fail.
        // Anonymous access (no header) still works — covered by the other test.
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
