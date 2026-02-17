package no.javazone.feedback.qrcode

import no.javazone.feedback.domain.FeedbackChannel
import qrcode.QRCode

fun generateQrCodeBytes(feedbackChannel: FeedbackChannel): ByteArray {
    return QRCode
        .ofSquares()
        .build(feedbackChannel.externalId)
        .renderToBytes()
}