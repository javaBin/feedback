package no.javazone.feedback.routes

import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.auth.authenticate
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.javazone.feedback.domain.adapters.FeedbackAdapter
import no.javazone.feedback.domain.errors.ChannelNotFoundError
import no.javazone.feedback.domain.errors.FeedbackNotFoundError
import no.javazone.feedback.pages.adminChannelPage

fun Route.adminRoutes(feedbackAdapter: FeedbackAdapter) {
    authenticate("admin") {
        route("/admin/channel/{channelId}") {
            get {
                val channelId = call.parameters["channelId"]
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                val channel = feedbackAdapter.findChannel(channelId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, "Channel not found")
                val feedbacks = feedbackAdapter.findFeedbacksForChannel(channelId)
                call.respondHtml { adminChannelPage(channel, feedbacks) }
            }

            post("feedback/{feedbackId}/update") {
                val channelId = call.parameters["channelId"]
                    ?: return@post call.respond(HttpStatusCode.NotFound)
                val feedbackId = call.parameters["feedbackId"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid feedback id")

                val params: Parameters = call.receiveParameters()
                val comment = params["detailedComment"]?.takeUnless { it.isBlank() }
                val ratings: Map<Long, Int> = params.entries()
                    .filter { it.key.startsWith("rating-") }
                    .mapNotNull { entry ->
                        val ratingId = entry.key.removePrefix("rating-").toLongOrNull() ?: return@mapNotNull null
                        val value = entry.value.firstOrNull()?.toIntOrNull() ?: return@mapNotNull null
                        ratingId to value
                    }
                    .toMap()

                try {
                    feedbackAdapter.updateFeedback(feedbackId, comment, ratings)
                } catch (e: FeedbackNotFoundError) {
                    return@post call.respond(HttpStatusCode.NotFound, e.message)
                }

                call.respondRedirect("/admin/channel/$channelId")
            }

            post("feedback/{feedbackId}/delete") {
                val channelId = call.parameters["channelId"]
                    ?: return@post call.respond(HttpStatusCode.NotFound)
                val feedbackId = call.parameters["feedbackId"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid feedback id")

                try {
                    feedbackAdapter.deleteFeedback(feedbackId)
                } catch (e: FeedbackNotFoundError) {
                    return@post call.respond(HttpStatusCode.NotFound, e.message)
                }

                call.respondRedirect("/admin/channel/$channelId")
            }
        }
    }
}
