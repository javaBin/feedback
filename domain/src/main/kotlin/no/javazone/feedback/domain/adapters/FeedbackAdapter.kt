package no.javazone.feedback.domain.adapters

import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackChannel
import no.javazone.feedback.domain.FeedbackChannelCreationInput
import no.javazone.feedback.domain.FeedbackWithChannel
import no.javazone.feedback.domain.errors.ChannelNotFoundError
import no.javazone.feedback.domain.generators.ExternalIdGenerator
import no.javazone.feedback.domain.persistence.FeedbackRepository

class FeedbackAdapter(
    private val repository: FeedbackRepository,
    private val externalIdGenerator: ExternalIdGenerator
) {
    fun createFeedbackChannel(input: FeedbackChannelCreationInput): FeedbackChannel {
        val channel = FeedbackChannel(
            title = input.title,
            speakers = input.speakers,
            externalId = "${input.channelTag}-${externalIdGenerator.generate()}",
            ratingCategories = input.ratings
        )
        return repository.intializeChannel(channel)
    }

    fun submitFeedback(channelId: String, feedback: Feedback): FeedbackWithChannel {
        val feedbackChannel = repository.findByChannelId(channelId)
            ?: throw ChannelNotFoundError("Channel with id $channelId does not exist")
        val createdFeedback = repository.submitFeedback(feedback, feedbackChannel)
        return FeedbackWithChannel(
            channel = feedbackChannel,
            feedback = createdFeedback
        )
    }
}