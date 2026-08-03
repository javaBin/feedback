package no.javazone.feedback.qrcode

import no.javazone.feedback.domain.FeedbackChannel
import qrcode.QRCode
import qrcode.raw.ErrorCorrectionLevel

class QRCodeGenerator(private val logoFilePath: String = "duke.png") {
    fun generateQrCodeBytes(feedbackChannel: FeedbackChannel): ByteArray {
        val logoBytes = this.javaClass.classLoader.getResourceAsStream(logoFilePath)?.readBytes()
        return QRCode
            .ofSquares()
            .withSize(40)
            .withErrorCorrectionLevel(ErrorCorrectionLevel.VERY_HIGH)
            .withLogo(logo = logoBytes, width = 200, height = 200)
            .build(data = "https://f.java.no/${feedbackChannel.externalId}")
            .renderToBytes()
    }
}
