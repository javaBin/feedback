package no.javazone.feedback.database.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object Feedbacks : LongIdTable("feedback") {
    val channelId = long("channel_id").references(FeedbackChannels.id)
    val detailedComment = text("detailed_comment").nullable()
    val createdAt = timestamp("created_at")
}
