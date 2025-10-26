package no.javazone.feedback

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.javazone.feedback.database.repository.FeedbackRepositoryDb
import no.javazone.feedback.database.setupDatabase
import no.javazone.feedback.domain.generators.ExternalIdGeneratorDefault
import no.javazone.feedback.domain.adapters.FeedbackAdapter
import no.javazone.feedback.request.channel.FeedbackChannelCreationDTO
import no.javazone.feedback.request.channel.toDTO
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val bootstrappingLogger = LoggerFactory.getLogger(object {}.javaClass)

fun main() {
    val server = embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        bootstrappingLogger.info("Receiving shutdown signal, stopping server...")
        server.stop(shutdownGracePeriod = 10, 60, timeUnit = TimeUnit.SECONDS)
        bootstrappingLogger.info("Application shut down, goodbye!")
    })

    server.start(wait = true)
}

fun Application.module() {
    setupDatabase()

    install(ContentNegotiation) {
        json()
    }
    install(CallLogging)

    routing {
        route("/v1/feedback") {
            val feedbackAdapter = FeedbackAdapter(
                repository = FeedbackRepositoryDb,
                externalIdGenerator = ExternalIdGeneratorDefault
            )
            route("channel") {
                post {
                    val input = call.receive<FeedbackChannelCreationDTO>()

                    val channel = feedbackAdapter.createFeedbackChannel(
                        input = input.toDomain()
                    )
                    call.respond(channel.toDTO())
                }
            }
            post {
                call.respond("Not implemented yet")
            }
        }
        get {
            call.respond("Hello, World!")
        }
    }
}