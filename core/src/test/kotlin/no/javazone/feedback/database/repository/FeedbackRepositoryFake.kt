package no.javazone.feedback.database.repository

import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackChannel

class FeedbackRepositoryFake : FeedbackRepository {
    override fun intializeChannel(channel: FeedbackChannel): FeedbackChannel {
        TODO("Not yet implemented")
    }

    override fun submitFeedback(feedback: Feedback): Feedback {
        TODO("Not yet implemented")
    }
}