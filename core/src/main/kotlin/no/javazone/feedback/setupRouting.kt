package no.javazone.feedback

import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing
import no.javazone.feedback.database.repository.FeedbackRepositoryDb
import no.javazone.feedback.domain.adapters.FeedbackAdapter
import no.javazone.feedback.qrcode.QRCodeGenerator
import no.javazone.feedback.routes.adminRoutes
import no.javazone.feedback.routes.feedbackChannelRoutes
import no.javazone.feedback.routes.healthRoutes
import no.javazone.feedback.routes.landingRoutes
import no.javazone.feedback.routes.sessionRoutes
import java.time.Clock

fun Application.setupRouting(clock: Clock = Clock.systemUTC()) {
    val feedbackAdapter = FeedbackAdapter(
        repository = FeedbackRepositoryDb,
    )
    val qrCodeGenerator = QRCodeGenerator()

    routing {
        staticResources("/static", "static")
        staticResources("/", "static")

        landingRoutes(feedbackAdapter)
        healthRoutes()
        sessionRoutes(feedbackAdapter, clock)
        feedbackChannelRoutes(feedbackAdapter, qrCodeGenerator, clock)
        adminRoutes(feedbackAdapter, clock)
    }
}
