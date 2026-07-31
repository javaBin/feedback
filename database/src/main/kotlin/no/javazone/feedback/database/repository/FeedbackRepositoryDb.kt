package no.javazone.feedback.database.repository

import no.javazone.feedback.database.models.FeedbackChannels
import no.javazone.feedback.database.models.FeedbackRatings
import no.javazone.feedback.database.models.Feedbacks
import no.javazone.feedback.database.models.RatingTypes
import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackChannel
import no.javazone.feedback.domain.FeedbackChannelRatingCategory
import no.javazone.feedback.domain.FeedbackRating
import no.javazone.feedback.domain.errors.ExternalIdAlreadyExistsError
import no.javazone.feedback.domain.persistence.FeedbackRepository
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.postgresql.util.PSQLState
import java.time.Instant

object FeedbackRepositoryDb : FeedbackRepository {
    override fun intializeChannel(channel: FeedbackChannel): FeedbackChannel {
        try {
            return transaction {
                val createdChannelId = FeedbackChannels.insertReturning {
                    it[title] = channel.title
                    it[speakers] = channel.speakers
                    it[externalId] = channel.externalId
                    it[isOpen] = channel.isOpen
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
                        ratingCategories = ratingCategories,
                        isOpen = it[FeedbackChannels.isOpen]
                    )
                }.first()
            }
        } catch (e: ExposedSQLException) {
            if (e.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
                throw ExternalIdAlreadyExistsError(channel.externalId, cause = e)
            }
            throw e
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

    override fun findAllChannels(): List<FeedbackChannel> {
        return transaction {
            val results = FeedbackChannels.selectAll()

            results
                .groupBy { it[FeedbackChannels.id].value }
                .map { (_, rows) ->
                    val firstRow = rows.first()
                    FeedbackChannel(
                        id = firstRow[FeedbackChannels.id].value,
                        title = firstRow[FeedbackChannels.title],
                        speakers = firstRow[FeedbackChannels.speakers],
                        externalId = firstRow[FeedbackChannels.externalId],
                        ratingCategories = emptyList(),
                        isOpen = firstRow[FeedbackChannels.isOpen]
                    )
                }
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
                    ratingCategories = ratingCategories,
                    isOpen = firstRow[FeedbackChannels.isOpen]
                )
            }
        }
    }

    override fun updateChannel(channel: FeedbackChannel): FeedbackChannel? {
        return transaction {
            val updated = FeedbackChannels.update({ FeedbackChannels.id eq channel.id }) {
                it[title] = channel.title
                it[speakers] = channel.speakers
                it[isOpen] = channel.isOpen
            }
            if (updated == 0) null else findByChannelId(channel.externalId)
        }
    }

    override fun findFeedbacksByChannelId(channelId: Long): List<Feedback> {
        return transaction {
            val rows = Feedbacks
                .join(FeedbackRatings, JoinType.LEFT) { Feedbacks.id eq FeedbackRatings.feedbackId }
                .join(RatingTypes, JoinType.LEFT) { FeedbackRatings.ratingTypeId eq RatingTypes.id }
                .selectAll()
                .where { Feedbacks.channelId eq channelId }
                .orderBy(Feedbacks.createdAt to SortOrder.DESC)
                .toList()

            rows.groupBy { it[Feedbacks.id].value }
                .map { (feedbackId, group) ->
                    val first = group.first()
                    val ratings = group
                        .filter { it.getOrNull(FeedbackRatings.id) != null }
                        .map {
                            FeedbackRating(
                                id = it[FeedbackRatings.id].value,
                                name = it[RatingTypes.ratingName],
                                typeId = it[FeedbackRatings.ratingTypeId],
                                value = it[FeedbackRatings.ratingValue],
                            )
                        }
                    Feedback(
                        id = feedbackId,
                        comment = first[Feedbacks.detailedComment],
                        ratings = ratings,
                    )
                }
        }
    }

    override fun findFeedbackById(feedbackId: Long): Feedback? {
        return transaction {
            val rows = Feedbacks
                .join(FeedbackRatings, JoinType.LEFT) { Feedbacks.id eq FeedbackRatings.feedbackId }
                .join(RatingTypes, JoinType.LEFT) { FeedbackRatings.ratingTypeId eq RatingTypes.id }
                .selectAll()
                .where { Feedbacks.id eq feedbackId }
                .toList()

            if (rows.isEmpty()) {
                null
            } else {
                val first = rows.first()
                val ratings = rows
                    .filter { it.getOrNull(FeedbackRatings.id) != null }
                    .map {
                        FeedbackRating(
                            id = it[FeedbackRatings.id].value,
                            name = it[RatingTypes.ratingName],
                            typeId = it[FeedbackRatings.ratingTypeId],
                            value = it[FeedbackRatings.ratingValue],
                        )
                    }
                Feedback(
                    id = first[Feedbacks.id].value,
                    comment = first[Feedbacks.detailedComment],
                    ratings = ratings,
                )
            }
        }
    }

    override fun updateFeedback(feedbackId: Long, comment: String?, ratings: Map<Long, Int>): Feedback? {
        return transaction {
            val updated = Feedbacks.update({ Feedbacks.id eq feedbackId }) {
                it[detailedComment] = comment
            }
            if (updated == 0) {
                return@transaction null
            }
            ratings.forEach { (ratingId, newValue) ->
                FeedbackRatings.update({
                    (FeedbackRatings.id eq ratingId) and (FeedbackRatings.feedbackId eq feedbackId)
                }) {
                    it[ratingValue] = newValue
                }
            }
            findFeedbackById(feedbackId)
        }
    }

    override fun deleteFeedback(feedbackId: Long): Boolean {
        return transaction {
            FeedbackRatings.deleteWhere { FeedbackRatings.feedbackId eq feedbackId }
            Feedbacks.deleteWhere { Feedbacks.id eq feedbackId } > 0
        }
    }
}