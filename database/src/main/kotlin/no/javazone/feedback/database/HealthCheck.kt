package no.javazone.feedback.database

import org.jetbrains.exposed.sql.transactions.transaction

fun isDatabaseHealthy(): Boolean {
    return try {
        transaction { exec("SELECT 1") { it.next() } }
        true
    } catch (_: Exception) {
        false
    }
}
