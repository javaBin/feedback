package no.javazone.feedback.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.javazone.feedback.domain.adapters.FeedbackAdapter
import no.javazone.feedback.pages.feedbackPage
import no.javazone.feedback.pages.thankYouFragment
import java.time.Clock

fun Route.sessionRoutes(feedbackAdapter: FeedbackAdapter, clock: Clock) {
    route("session") {
        get("/") {
            call.respondRedirect("/")
        }

        get {
            call.respondRedirect("/")
        }

        authenticate("admin", optional = true) {
            get("/{channelId}") {
                val channelId = call.parameters["channelId"]
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                if (channelId.length != 4) {
                    return@get call.respond(HttpStatusCode.BadRequest)
                }
                val channel = feedbackAdapter.findChannel(channelId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, "Channel not found")
                val isAdmin = call.principal<UserIdPrincipal>() != null
                call.respondHtml {
                    feedbackPage(channel = channel, now = clock.instant(), isAdmin = isAdmin)
                }
            }
        }

        get("/{channelId}/thank-you") {
            call.respondHtml { thankYouFragment() }
        }
    }
}
