package no.javazone.feedback.database.repository

import no.javazone.feedback.database.models.FeedbackChannels
import no.javazone.feedback.database.models.Feedbacks
import no.javazone.feedback.database.models.RatingTypes
import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackChannel
import no.javazone.feedback.domain.FeedbackRepository
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.transactions.transaction

object FeedbackRepositoryDb : FeedbackRepository {
    override fun intializeChannel(channel: FeedbackChannel): FeedbackChannel {
        return transaction {
            val channel = FeedbackChannels.insertReturning {
                it[title] = channel.title
                it[speakers] = channel.speakers
                it[externalId] = channel.externalId
            }.map {
                FeedbackChannel(
                    id = it[FeedbackChannels.id].value,
                    title = it[FeedbackChannels.title],
                    speakers = it[FeedbackChannels.speakers],
                    externalId = it[FeedbackChannels.externalId]
                )
            }.first()

            RatingTypes.insert {
                it[channelId] = channel.id
                it[ratingName] = "usefulness"
            }

            channel
        }

    }

    override fun submitFeedback(feedback: Feedback): Feedback {
        TODO()
    }
}