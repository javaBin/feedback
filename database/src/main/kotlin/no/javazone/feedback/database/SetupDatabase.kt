package no.javazone.feedback.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.jetbrains.exposed.sql.Database
import kotlin.String
import kotlin.use

fun setupDatabase(
    databaseConfig: FeedbackDatabaseConfig
) {
    val connectionPoolConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://${databaseConfig.host}:${databaseConfig.port}/${databaseConfig.databaseName}"
        username = databaseConfig.username
        password = databaseConfig.password
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
    }
    val dataSource = HikariDataSource(connectionPoolConfig)
    Database.connect(dataSource)

    dataSource.connection.use {
        val database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(JdbcConnection(it))
        val liquibase = Liquibase(
            "changelog/db.changelog-master.yaml",
            ClassLoaderResourceAccessor(),
            database
        )
        liquibase.update("")

    }
}

data class FeedbackDatabaseConfig(
    val host: String,
    val port: Int,
    val databaseName: String,
    val username: String,
    val password: String,
)