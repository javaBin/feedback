package no.javazone.feedback.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.jetbrains.exposed.sql.Database
import kotlin.use

fun setupDatabase() {
    val connectionPoolConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://localhost:5432/feedback"
        username = "feedback"
        password = "feedback"
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