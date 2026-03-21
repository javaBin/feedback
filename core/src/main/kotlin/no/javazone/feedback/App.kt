package no.javazone.feedback

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import no.javazone.feedback.database.FeedbackDatabaseConfig
import no.javazone.feedback.database.setupDatabase

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module(
    databaseConfig: FeedbackDatabaseConfig = FeedbackDatabaseConfig(
        host = environment.config.property("database.host").getString(),
        port = environment.config.property("database.port").getString().toInt(),
        databaseName = environment.config.property("database.name").getString(),
        username = environment.config.property("database.username").getString(),
        password = environment.config.property("database.password").getString(),
    )
) {
    setupDatabase(databaseConfig)

    install(ContentNegotiation) {
        json()
    }
    install(CallLogging)

    setupRouting()
}
