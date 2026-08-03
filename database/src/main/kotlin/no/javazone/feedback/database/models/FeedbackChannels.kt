package no.javazone.feedback.database.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object FeedbackChannels : LongIdTable("feedback_channel") {
    val title = varchar("title", 255)
    val speakers = array<String>("speakers")
    val externalId = varchar("external_id", 255)
    val isOpen = bool("is_open").default(false)
    val opensAt = timestampWithTimeZone("opens_at").nullable()
    val closesAt = timestampWithTimeZone("closes_at").nullable()
    val createdAt = timestamp("created_at")
}