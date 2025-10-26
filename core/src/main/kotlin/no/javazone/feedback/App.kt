package no.javazone.feedback

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import no.javazone.feedback.database.FeedbackDatabaseConfig
import no.javazone.feedback.database.setupDatabase
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

fun Application.module(
    databaseConfig: FeedbackDatabaseConfig = FeedbackDatabaseConfig(
        host = "localhost",
        port = 5432,
        databaseName = "feedback",
        username = "feedback",
        password = "feedback",
    )
) {
    setupDatabase(databaseConfig)

    install(ContentNegotiation) {
        json()
    }
    install(CallLogging)

    setupRouting()
}
