package no.javazone.feedback.domain.errors

sealed class DomainErrors(
    override val message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class ChannelNotFoundError(channelId: String) :
    DomainErrors("Channel with id $channelId not found.")

class ChannelClosedError(channelId: String) :
    DomainErrors("Channel with id $channelId is closed for feedback.")

class FeedbackNotFoundError(feedbackId: Long) :
    DomainErrors("Feedback with id $feedbackId not found.")