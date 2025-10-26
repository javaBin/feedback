package no.javazone.feedback.database

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy

object TestDatabase {
    val container: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:18-alpine")
        .withDatabaseName("feedback-testcontainer-db")
        .withUsername("feedback")
        .withPassword("feedback")
        .waitingFor(HostPortWaitStrategy())

    fun start() {
        container.start()
    }

    fun stop() {
        container.stop()
    }

    fun config(): FeedbackDatabaseConfig {
        return FeedbackDatabaseConfig(
            host = container.host,
            port = container.firstMappedPort,
            databaseName = container.databaseName,
            username = container.username,
            password = container.password
        )
    }
}