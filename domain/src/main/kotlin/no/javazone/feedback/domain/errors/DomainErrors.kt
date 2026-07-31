package no.javazone.feedback.domain.errors

sealed class DomainErrors(
    override val message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class ChannelNotFoundError(channelId: String) :
    DomainErrors("Channel with id $channelId not found.")

class ChannelClosedError(channelId: String) :
    DomainErrors("Channel with id $channelId is closed for feedback.")

class ExternalIdAlreadyExistsError(externalId: String, cause: Throwable? = null) :
    DomainErrors("Channel with external id $externalId already exists.", cause)

class ExternalIdGenerationException :
    DomainErrors("Failed to generate a unique external id after multiple attempts.")