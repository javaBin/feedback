package no.javazone.feedback

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import no.javazone.feedback.database.FeedbackDatabaseConfig
import no.javazone.feedback.database.setupDatabase

data class AuthConfig(
    val username: String,
    val password: String,
)

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
    ),
    authConfig: AuthConfig = AuthConfig(
        username = environment.config.property("auth.username").getString(),
        password = environment.config.property("auth.password").getString(),
    ),
) {
    setupDatabase(databaseConfig)

    install(ContentNegotiation) {
        json()
    }
    install(CallLogging)
    install(Authentication) {
        basic("admin") {
            realm = "JavaZone Feedback Admin"
            validate { credentials ->
                if (credentials.name == authConfig.username && credentials.password == authConfig.password) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }

    setupRouting()
}
