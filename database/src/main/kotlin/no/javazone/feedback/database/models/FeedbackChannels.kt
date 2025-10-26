package no.javazone.feedback.database.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object FeedbackChannels : LongIdTable("feedback_channel") {
    val title = varchar("title", 255)
    val speakers = array<String>("speakers")
    val externalId = varchar("external_id", 255)
    val createdAt = timestamp("created_at")
}