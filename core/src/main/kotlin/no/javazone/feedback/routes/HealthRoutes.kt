package no.javazone.feedback.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.javazone.feedback.database.isDatabaseHealthy

fun Route.healthRoutes() {
    get("/health") {
        if (isDatabaseHealthy()) {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        } else {
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                mapOf("status" to "unhealthy", "reason" to "database connection failed"),
            )
        }
    }
}
