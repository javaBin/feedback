package no.javazone.feedback.database.repository

import no.javazone.feedback.database.models.FeedbackChannels
import no.javazone.feedback.database.models.RatingTypes
import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackChannel
import no.javazone.feedback.domain.FeedbackChannelRatingCategory
import no.javazone.feedback.domain.persistence.FeedbackRepository
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
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

            val ratingCategories = RatingTypes.batchInsert(channel.ratings) { rating ->
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
                    ratings = ratingCategories
                )
            }.first()
        }

    }

    override fun submitFeedback(feedback: Feedback): Feedback {
        TODO()
    }
}