package no.javazone.feedback.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.javazone.feedback.domain.adapters.FeedbackAdapter
import no.javazone.feedback.pages.codeEntryPage

private const val CODE_LENGTH = 4

fun Route.landingRoutes(feedbackAdapter: FeedbackAdapter) {
    get("/") {
        call.respondHtml { codeEntryPage() }
    }

    post("/") {
        val code = call.receiveParameters()["code"]?.trim()?.uppercase().orEmpty()

        when {
            code.length != CODE_LENGTH -> call.respondHtml(HttpStatusCode.BadRequest) {
                codeEntryPage(code = code, error = "Koden må være $CODE_LENGTH tegn")
            }

            feedbackAdapter.findChannel(code) == null -> call.respondHtml(HttpStatusCode.NotFound) {
                codeEntryPage(code = code, error = "Fant ingen sesjon med koden \"$code\"")
            }

            else -> call.respondRedirect("/session/$code")
        }
    }
}
