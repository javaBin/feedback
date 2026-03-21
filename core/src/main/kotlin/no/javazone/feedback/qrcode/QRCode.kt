package no.javazone.feedback.qrcode

import no.javazone.feedback.domain.FeedbackChannel
import qrcode.QRCode

class QRCodeGenerator(private val logoFilePath: String = "duke_small.png") {
    fun generateQrCodeBytes(feedbackChannel: FeedbackChannel): ByteArray {
        val logoBytes = this.javaClass.classLoader.getResourceAsStream(logoFilePath)?.readBytes()
        return QRCode
            .ofSquares()
            .withSize(40)
            .withLogo(logo = logoBytes, width = 100, height = 100)
            .build(data = "https://feedback.java.no/session/${feedbackChannel.externalId}")
            .renderToBytes()
    }
}
