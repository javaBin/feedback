package no.javazone.feedback

interface FeedbackAdapter {
    fun createFeedbackChannel(input: FeedbackChannelCreationDTO): FeedbackChannelDTO
}