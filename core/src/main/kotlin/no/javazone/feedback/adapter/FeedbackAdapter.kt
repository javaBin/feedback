package no.javazone.feedback.adapter

import no.javazone.feedback.domain.FeedbackChannel
import no.javazone.feedback.request.channel.FeedbackChannelCreationDTO

interface FeedbackAdapter {
    fun createFeedbackChannel(input: FeedbackChannelCreationDTO): FeedbackChannel
}