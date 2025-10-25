package no.javazone.feedback.database.repository

import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackChannel

interface FeedbackRepository {
    fun intializeChannel(channel: FeedbackChannel): FeedbackChannel
    fun submitFeedback(feedback: Feedback): Feedback
}