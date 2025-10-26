package no.javazone.feedback.database.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object RatingTypes : LongIdTable("rating_type") {
    val channelId = long("channel_id").references(FeedbackChannels.id)
    val ratingName = varchar("rating_name", 50)
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(channelId, ratingName)
    }
}

