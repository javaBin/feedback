package no.javazone.feedback.domain.adapters

import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackChannel
import no.javazone.feedback.domain.FeedbackChannelCreationInput
import no.javazone.feedback.domain.FeedbackWithChannel
import no.javazone.feedback.domain.errors.ChannelNotFoundError
import no.javazone.feedback.domain.errors.ExternalIdAlreadyExistsError
import no.javazone.feedback.domain.errors.ExternalIdGenerationException
import no.javazone.feedback.domain.generators.ExternalIdGenerator
import no.javazone.feedback.domain.persistence.FeedbackRepository

class FeedbackAdapter(
    private val repository: FeedbackRepository,
    private val externalIdGenerator: ExternalIdGenerator
) {
    companion object {
        private const val MAX_RETRIES = 3
    }

    fun createFeedbackChannel(input: FeedbackChannelCreationInput): FeedbackChannel {
        repeat(MAX_RETRIES) {
            try {
                val channel = FeedbackChannel(
                    title = input.title,
                    speakers = input.speakers,
                    externalId = externalIdGenerator.generate(),
                    ratingCategories = input.ratings
                )
                return repository.intializeChannel(channel)
            } catch (_: ExternalIdAlreadyExistsError) {
                // retry with a new external id
            }
        }
        throw ExternalIdGenerationException()
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

    fun findChannel(channelId: String): FeedbackChannel? {
        return repository.findByChannelId(channelId)
    }

    fun findAllChannels(): List<FeedbackChannel> {
        return repository.findAllChannels()
    }

    fun generateQrCode(channelId: String, qrCodeGenerator: (FeedbackChannel) -> ByteArray): ByteArray? {
        return repository.findByChannelId(channelId)?.let {
            qrCodeGenerator(it)
        }
    }
}