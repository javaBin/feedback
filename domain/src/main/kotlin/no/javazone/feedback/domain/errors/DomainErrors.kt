package no.javazone.feedback.domain.errors

sealed class DomainErrors : Exception()

class ChannelNotFoundError(channelId: String) : DomainErrors() {
    override val message: String = "Channel with id $channelId not found."
}