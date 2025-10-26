package no.javazone.feedback.database.repository

import no.javazone.feedback.database.models.FeedbackChannels
import no.javazone.feedback.database.models.FeedbackRatings
import no.javazone.feedback.database.models.Feedbacks
import no.javazone.feedback.database.models.RatingTypes
import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackChannel
import no.javazone.feedback.domain.FeedbackChannelRatingCategory
import no.javazone.feedback.domain.FeedbackRating
import no.javazone.feedback.domain.persistence.FeedbackRepository
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object FeedbackRepositoryDb : FeedbackRepository {
    override fun intializeChannel(channel: FeedbackChannel): FeedbackChannel {
        return transaction {
            val createdChannelId = FeedbackChannels.insertReturning {
                it[title] = channel.title
                it[speakers] = channel.speakers
                it[externalId] = channel.externalId
            }.map {
                it[FeedbackChannels.id]
            }.first()

            val ratingCategories = RatingTypes.batchInsert(channel.ratingCategories) { rating ->
                this[RatingTypes.channelId] = createdChannelId.value
                this[RatingTypes.ratingName] = rating.name
                this[RatingTypes.createdAt] = Instant.now()
            }.map {
                FeedbackChannelRatingCategory(
                    id = it[RatingTypes.id].value,
                    name = it[RatingTypes.ratingName]
                )
            }

            FeedbackChannels.selectAll().where {
                FeedbackChannels.id eq createdChannelId
            }.map {
                FeedbackChannel(
                    id = it[FeedbackChannels.id].value,
                    title = it[FeedbackChannels.title],
                    speakers = it[FeedbackChannels.speakers],
                    externalId = it[FeedbackChannels.externalId],
                    ratingCategories = ratingCategories
                )
            }.first()
        }

    }

    override fun submitFeedback(feedback: Feedback, feedbackChannel: FeedbackChannel): Feedback {
        return transaction {
            val feedbackId = Feedbacks.insertReturning {
                it[channelId] = feedbackChannel.id
                it[detailedComment] = feedback.comment
            }.map { it[Feedbacks.id] }.first()

            val channelCategoryMap = feedbackChannel.ratingCategories.associateBy { it.id }

            val ratings = FeedbackRatings.batchInsert(feedback.ratings) {
                this[FeedbackRatings.feedbackId] = feedbackId.value
                this[FeedbackRatings.ratingTypeId] = it.id
                this[FeedbackRatings.ratingValue] = it.value
                this[FeedbackRatings.createdAt] = Instant.now()
            }.map {
                FeedbackRating(
                    id = it[FeedbackRatings.id].value,
                    name = channelCategoryMap[it[FeedbackRatings.ratingTypeId]]?.name ?: "Unknown",
                    typeId = it[FeedbackRatings.ratingTypeId],
                    value = it[FeedbackRatings.ratingValue]
                )
            }

            Feedback(
                id = feedbackId.value,
                comment = feedback.comment,
                ratings = ratings
            )
        }
    }

    override fun findByChannelId(channelId: String): FeedbackChannel? {
        return transaction {
            val results = FeedbackChannels.join(otherTable = RatingTypes, joinType = JoinType.INNER) {
                FeedbackChannels.id eq RatingTypes.channelId
            }
                .selectAll()
                .where { FeedbackChannels.externalId eq channelId }
                .toList()


            if (results.isEmpty()) {
                null
            } else {
                val firstRow = results.first()
                val ratingCategories = results.map {
                    FeedbackChannelRatingCategory(
                        id = it[RatingTypes.id].value,
                        name = it[RatingTypes.ratingName]
                    )
                }

                FeedbackChannel(
                    id = firstRow[FeedbackChannels.id].value,
                    title = firstRow[FeedbackChannels.title],
                    speakers = firstRow[FeedbackChannels.speakers],
                    externalId = firstRow[FeedbackChannels.externalId],
                    ratingCategories = ratingCategories
                )
            }
        }
    }
}