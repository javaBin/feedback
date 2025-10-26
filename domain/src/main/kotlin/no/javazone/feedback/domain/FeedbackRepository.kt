package no.javazone.feedback.domain

interface FeedbackRepository {
    fun intializeChannel(channel: FeedbackChannel): FeedbackChannel
    fun submitFeedback(feedback: Feedback): Feedback
}