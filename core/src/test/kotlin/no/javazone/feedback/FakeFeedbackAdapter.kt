package no.javazone.feedback

import java.util.concurrent.atomic.AtomicLong

class FakeFeedbackAdapter : FeedbackAdapter {
    private val channels = mutableListOf<FeedbackChannel>()
    private val idGenerator = AtomicLong(1000L)

    override fun createFeedbackChannel(input: FeedbackChannelCreationDTO): FeedbackChannel {
        val channel = FeedbackChannel(
            id = idGenerator.incrementAndGet(),
            title = input.title,
            speakers = input.speakers,
            channelPrefix = input.channelPrefix
        )
        channels.add(channel)
        return channel
    }
}