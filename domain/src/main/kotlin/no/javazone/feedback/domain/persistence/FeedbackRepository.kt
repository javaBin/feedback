package no.javazone.feedback.domain.persistence

import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackChannel
import no.javazone.feedback.domain.FeedbackChannelCreationInput

interface FeedbackRepository {
    fun intializeChannel(input: FeedbackChannelCreationInput): FeedbackChannel
    fun submitFeedback(feedback: Feedback, feedbackChannel: FeedbackChannel): Feedback
    fun findByChannelId(channelId: String): FeedbackChannel?
    fun findAllChannels(): List<FeedbackChannel>
    fun updateChannel(channel: FeedbackChannel): FeedbackChannel?
    fun findFeedbacksByChannelId(channelId: Long): List<Feedback>
    fun findFeedbackById(feedbackId: Long): Feedback?
    fun updateFeedback(feedbackId: Long, comment: String?, ratings: Map<Long, Int>): Feedback?
    fun deleteFeedback(feedbackId: Long): Boolean
}
