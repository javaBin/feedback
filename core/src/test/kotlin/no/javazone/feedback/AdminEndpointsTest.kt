package no.javazone.feedback

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import no.javazone.feedback.database.TestDatabase
import no.javazone.feedback.request.channel.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class AdminEndpointsTest {
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

    private suspend fun createOpenChannelWithFeedback(
        jsonClient: io.ktor.client.HttpClient,
        title: String = "Session",
        comment: String? = "Great talk",
        score: Int = 5,
    ): Triple<String, Long, Long> {
        val channel = jsonClient.post("/v1/feedback/channel") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackChannelCreationDTO(
                    title = title,
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Content")),
                )
            )
        }.body<FeedbackChannelDTO>()

        jsonClient.patch("/v1/feedback/channel/${channel.channelId}") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(isOpen = true))
        }

        val feedback = jsonClient.post("/v1/feedback/channel/${channel.channelId}/submit-feedback") {
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackCreationDTO(
                    ratings = listOf(FeedbackRatingCreationDTO(id = channel.ratingCategories[0].id!!, score = score)),
                    detailedComment = comment,
                )
            )
        }.body<FeedbackDTO>()

        return Triple(channel.channelId, feedback.id, feedback.ratings[0].id)
    }

    @Test
    fun `admin page without auth returns unauthorized`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.get("/admin/channel/ABCD")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `admin page for unknown channel returns not found`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.get("/admin/channel/ZZZZ") {
            adminAuth()
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `admin page renders submitted feedback`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) { json() }
        }

        val (channelId, feedbackId, _) = createOpenChannelWithFeedback(
            jsonClient,
            comment = "Very insightful",
            score = 4,
        )

        val response = client.get("/admin/channel/$channelId") {
            adminAuth()
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Very insightful"), "body should contain the comment")
        assertTrue(
            body.contains("/admin/channel/$channelId/feedback/$feedbackId/update"),
            "body should contain the update form action",
        )
        assertTrue(
            body.contains("/admin/channel/$channelId/feedback/$feedbackId/delete"),
            "body should contain the delete form action",
        )
    }

    @Test
    fun `updating feedback changes comment and score`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) { json() }
        }

        val (channelId, feedbackId, ratingId) = createOpenChannelWithFeedback(
            jsonClient,
            comment = "Original comment",
            score = 5,
        )

        val updateResponse = client.post("/admin/channel/$channelId/feedback/$feedbackId/update") {
            adminAuth()
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                listOf(
                    "detailedComment" to "Edited comment",
                    "rating-$ratingId" to "2",
                ).formUrlEncode()
            )
        }
        assertEquals(HttpStatusCode.Found, updateResponse.status)

        val pageResponse = client.get("/admin/channel/$channelId") {
            adminAuth()
        }
        assertEquals(HttpStatusCode.OK, pageResponse.status)
        val body = pageResponse.bodyAsText()
        assertTrue(body.contains("Edited comment"), "should show updated comment")
        assertFalse(body.contains("Original comment"), "old comment should be gone")
    }

    @Test
    fun `deleting feedback removes it`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) { json() }
        }

        val (channelId, feedbackId, _) = createOpenChannelWithFeedback(
            jsonClient,
            comment = "Please remove me",
        )

        val deleteResponse = client.post("/admin/channel/$channelId/feedback/$feedbackId/delete") {
            adminAuth()
        }
        assertEquals(HttpStatusCode.Found, deleteResponse.status)

        val pageResponse = client.get("/admin/channel/$channelId") {
            adminAuth()
        }
        assertEquals(HttpStatusCode.OK, pageResponse.status)
        val body = pageResponse.bodyAsText()
        assertFalse(body.contains("Please remove me"), "deleted feedback should not appear")
        assertTrue(body.contains("No feedback has been submitted yet."))
    }

    @Test
    fun `update without auth returns unauthorized`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.post("/admin/channel/ABCD/feedback/1/update") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("detailedComment=hi")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `delete without auth returns unauthorized`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.post("/admin/channel/ABCD/feedback/1/delete")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `updating unknown feedback returns not found`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.post("/admin/channel/ABCD/feedback/999999/update") {
            adminAuth()
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("detailedComment=hi")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `deleting unknown feedback returns not found`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.post("/admin/channel/ABCD/feedback/999999/delete") {
            adminAuth()
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `update with wrong credentials returns unauthorized`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val response = client.post("/admin/channel/ABCD/feedback/1/update") {
            adminAuth(user = "wrong", password = "wrong")
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("detailedComment=hi")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `updating one feedback does not affect siblings`() = testApplication {
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
                    title = "Multi feedback",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Content")),
                )
            )
        }.body<FeedbackChannelDTO>()

        jsonClient.patch("/v1/feedback/channel/${channel.channelId}") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(isOpen = true))
        }

        val first = jsonClient.post("/v1/feedback/channel/${channel.channelId}/submit-feedback") {
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackCreationDTO(
                    ratings = listOf(FeedbackRatingCreationDTO(id = channel.ratingCategories[0].id!!, score = 5)),
                    detailedComment = "Keep me untouched",
                )
            )
        }.body<FeedbackDTO>()

        val second = jsonClient.post("/v1/feedback/channel/${channel.channelId}/submit-feedback") {
            contentType(ContentType.Application.Json)
            setBody(
                FeedbackCreationDTO(
                    ratings = listOf(FeedbackRatingCreationDTO(id = channel.ratingCategories[0].id!!, score = 4)),
                    detailedComment = "Edit me",
                )
            )
        }.body<FeedbackDTO>()

        val updateResponse = client.post("/admin/channel/${channel.channelId}/feedback/${second.id}/update") {
            adminAuth()
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                listOf(
                    "detailedComment" to "Edited second",
                    "rating-${second.ratings[0].id}" to "1",
                ).formUrlEncode()
            )
        }
        assertEquals(HttpStatusCode.Found, updateResponse.status)

        val body = client.get("/admin/channel/${channel.channelId}") {
            adminAuth()
        }.bodyAsText()

        assertTrue(body.contains("Keep me untouched"), "first feedback should still be present")
        assertTrue(body.contains("Edited second"), "second feedback should show new comment")
        assertFalse(body.contains("Edit me"), "second feedback should no longer show old comment")
        assertTrue(
            body.contains("/admin/channel/${channel.channelId}/feedback/${first.id}/update"),
            "first feedback should still be rendered",
        )
    }

    @Test
    fun `admin page renders multiple feedbacks`() = testApplication {
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
                    title = "Many feedbacks",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Content")),
                )
            )
        }.body<FeedbackChannelDTO>()

        jsonClient.patch("/v1/feedback/channel/${channel.channelId}") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbackChannelUpdateDTO(isOpen = true))
        }

        val comments = listOf("First comment", "Second comment", "Third comment")
        comments.forEach { comment ->
            jsonClient.post("/v1/feedback/channel/${channel.channelId}/submit-feedback") {
                contentType(ContentType.Application.Json)
                setBody(
                    FeedbackCreationDTO(
                        ratings = listOf(FeedbackRatingCreationDTO(id = channel.ratingCategories[0].id!!, score = 3)),
                        detailedComment = comment,
                    )
                )
            }
        }

        val body = client.get("/admin/channel/${channel.channelId}") {
            adminAuth()
        }.bodyAsText()

        comments.forEach { comment ->
            assertTrue(body.contains(comment), "body should contain \"$comment\"")
        }
        assertTrue(body.contains("3 feedbacks"), "header should indicate 3 feedbacks")
    }

    @Test
    fun `feedback with null comment renders and can be updated`() = testApplication {
        application {
            module(TestDatabase.config(), testAuthConfig)
        }

        val jsonClient = createClient {
            install(ContentNegotiation) { json() }
        }

        val (channelId, feedbackId, ratingId) = createOpenChannelWithFeedback(
            jsonClient,
            comment = null,
            score = 3,
        )

        val initialBody = client.get("/admin/channel/$channelId") {
            adminAuth()
        }.bodyAsText()
        assertTrue(
            initialBody.contains("/admin/channel/$channelId/feedback/$feedbackId/update"),
            "feedback with null comment should still render",
        )

        val updateResponse = client.post("/admin/channel/$channelId/feedback/$feedbackId/update") {
            adminAuth()
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                listOf(
                    "detailedComment" to "Now with comment",
                    "rating-$ratingId" to "5",
                ).formUrlEncode()
            )
        }
        assertEquals(HttpStatusCode.Found, updateResponse.status)

        val updatedBody = client.get("/admin/channel/$channelId") {
            adminAuth()
        }.bodyAsText()
        assertTrue(updatedBody.contains("Now with comment"), "updated comment should be visible")
    }

    @Test
    fun `admin page for channel without feedback shows empty state`() = testApplication {
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
                    title = "Empty",
                    speakers = listOf("Speaker"),
                    ratingCategories = listOf(FeedbackChannelRatingCategoryDTO(id = null, title = "Content")),
                )
            )
        }.body<FeedbackChannelDTO>()

        val response = client.get("/admin/channel/${channel.channelId}") {
            adminAuth()
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("No feedback has been submitted yet."))
        assertTrue(body.contains("0 feedbacks"))
    }
}
