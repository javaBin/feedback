package no.javazone.feedback.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.javazone.feedback.domain.adapters.FeedbackAdapter
import no.javazone.feedback.pages.landingPage

fun Route.landingRoutes(feedbackAdapter: FeedbackAdapter) {
    authenticate("admin") {
        get("/") {
            val channels = feedbackAdapter.findAllChannels()
            call.respondHtml { landingPage(channels) }
        }
    }
}
