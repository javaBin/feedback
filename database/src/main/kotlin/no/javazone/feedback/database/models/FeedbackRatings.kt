package no.javazone.feedback.database.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object FeedbackRatings : LongIdTable("feedback_rating") {
    val feedbackId = long("feedback_id")
    val ratingTypeId = long("rating_type_id")
    val ratingValue = integer("rating_value")
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(feedbackId, ratingTypeId)
    }
}
