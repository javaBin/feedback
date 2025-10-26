package no.javazone.feedback.domain.persistence

import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackChannel

interface FeedbackRepository {
    fun intializeChannel(channel: FeedbackChannel): FeedbackChannel
    fun submitFeedback(feedback: Feedback, feedbackChannel: FeedbackChannel): Feedback
    fun findByChannelId(channelId: String): FeedbackChannel?
}