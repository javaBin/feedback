package no.javazone.feedback

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object FeedbackChannels : LongIdTable("feedback_channel") {
    val title = varchar("title", 255)
    val speakers = array<String>("speakers")
    val externalId = varchar("external_id", 255)
    val createdAt = timestamp("created_at")
}

object RatingTypes : LongIdTable("rating_type") {
    val channelId = long("channel_id")
    val ratingName = varchar("rating_name", 50)
    val createdAt = timestamp("created_at")
}

object Feedbacks : LongIdTable("feedback") {
    val channelId = long("channel_id")
    val detailedComment = text("detailed_comment").nullable()
    val createdAt = timestamp("created_at")
}

object FeedbackRatings : LongIdTable("feedback_rating") {
    val feedbackId = long("feedback_id")
    val ratingTypeId = long("rating_type_id")
    val ratingValue = integer("rating_value")
    val createdAt = timestamp("created_at")
}
