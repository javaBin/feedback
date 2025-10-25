package no.javazone.feedback

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.jetbrains.exposed.sql.Database
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
            route("channel") {
                post {
                    call.respond("Not implemented yet")
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

private fun setupDatabase() {
    val connectionPoolConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://localhost:5432/feedback"
        username = "feedback"
        password = "feedback"
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
    }
    val dataSource = HikariDataSource(connectionPoolConfig)
    Database.connect(dataSource)

    // Run Liquibase migrations
    dataSource.connection.use { connection ->
        val database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(JdbcConnection(connection))
        val liquibase = Liquibase(
            "db/changelog/db.changelog-master.yaml",
            ClassLoaderResourceAccessor(),
            database
        )
        liquibase.update("")
        bootstrappingLogger.info("Database migrations completed successfully")
    }
}