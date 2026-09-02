package no.javazone.feedback.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.javazone.feedback.domain.FeedbackChannel
import no.javazone.feedback.domain.adapters.FeedbackAdapter
import no.javazone.feedback.domain.errors.ChannelClosedError
import no.javazone.feedback.domain.errors.ChannelNotFoundError
import no.javazone.feedback.qrcode.QRCodeGenerator
import no.javazone.feedback.request.channel.FeedbackChannelCreationDTO
import no.javazone.feedback.request.channel.FeedbackChannelRatingCategoryDTO
import no.javazone.feedback.request.channel.FeedbackChannelUpdateDTO
import no.javazone.feedback.request.channel.FeedbackCreationDTO
import no.javazone.feedback.request.channel.FeedbackDTO
import no.javazone.feedback.request.channel.FeedbackRatingDTO
import no.javazone.feedback.request.channel.toDTO
import java.time.Clock
import java.time.Instant

fun Route.feedbackChannelRoutes(
    feedbackAdapter: FeedbackAdapter,
    qrCodeGenerator: QRCodeGenerator,
    clock: Clock,
) {
    route("/v1/feedback/channel") {
        // Public routes
        post("{channelId}/submit-feedback") {
            val channelId = call.parameters["channelId"] ?: return@post call.respond(
                HttpStatusCode.NotFound,
                "Missing externalId",
            )

            val feedbackInput = call.receive<FeedbackCreationDTO>()

            application.log.info("Feedback received on ${clock.instant()}: $feedbackInput \n\n ${call.request.headers}")


            val createdFeedback = try {
                feedbackAdapter.submitFeedback(
                    channelId = channelId,
                    feedback = feedbackInput.toDomain(),
                    now = clock.instant(),
                )
            } catch (e: ChannelNotFoundError) {
                return@post call.respond(HttpStatusCode.NotFound, e.message)
            } catch (e: ChannelClosedError) {
                return@post call.respond(HttpStatusCode.Forbidden, e.message)
            }

            val feedbackDto = createdFeedback.let { feedbackWithComment ->
                val ratingCategories = feedbackWithComment.channel.ratingCategories.associateBy { it.id }

                FeedbackDTO(
                    id = feedbackWithComment.feedback.id,
                    channel = feedbackWithComment.channel.toDTO(),
                    detailedComment = feedbackWithComment.feedback.comment,
                    ratings = feedbackWithComment.feedback.ratings.map { rating ->
                        FeedbackRatingDTO(
                            id = rating.id,
                            category = with(ratingCategories[rating.typeId]) {
                                FeedbackChannelRatingCategoryDTO(
                                    id = this?.id ?: 0,
                                    title = this?.name ?: "Unknown",
                                )
                            },
                            score = rating.value,
                        )
                    },
                )
            }

            call.respond(feedbackDto)
        }

        get("{channelId}/qrcode") {
            val channelId = call.parameters["channelId"] ?: return@get call.respond(
                HttpStatusCode.NotFound,
                "Missing externalId",
            )

            val qrCodeBytes = feedbackAdapter.generateQrCode(
                channelId = channelId,
                qrCodeGenerator = qrCodeGenerator::generateQrCodeBytes,
            ) ?: return@get call.respond(
                HttpStatusCode.NotFound,
                "Channel with id $channelId does not exist",
            )

            call.response.header(HttpHeaders.CacheControl, "public, max-age=86400, immutable")
            call.respondOutputStream(
                contentType = ContentType.Image.PNG,
                status = HttpStatusCode.OK,
            ) {
                write(qrCodeBytes)
            }
        }

        // Admin routes (basic auth)
        authenticate("admin") {
            post {
                val input = call.receive<FeedbackChannelCreationDTO>()

                val channel = feedbackAdapter.createFeedbackChannel(
                    input = input.toDomain(),
                )

                call.respond(channel.toDTO())
            }

            patch("{channelId}") {
                val channelId = call.parameters["channelId"] ?: return@patch call.respond(
                    HttpStatusCode.NotFound,
                    "Missing externalId",
                )
                val updateInput = call.receive<FeedbackChannelUpdateDTO>()
                val existing = feedbackAdapter.findChannel(channelId)
                    ?: return@patch call.respond(HttpStatusCode.NotFound, "Channel with id $channelId not found.")
                val newOpensAt = when (val v = updateInput.opensAt) {
                    null -> existing.opensAt
                    "" -> null
                    else -> Instant.parse(v)
                }
                val newClosesAt = when (val v = updateInput.closesAt) {
                    null -> existing.closesAt
                    "" -> null
                    else -> Instant.parse(v)
                }
                val merged = try {
                    FeedbackChannel(
                        id = existing.id,
                        title = updateInput.title ?: existing.title,
                        speakers = updateInput.speakers ?: existing.speakers,
                        externalId = existing.externalId,
                        ratingCategories = existing.ratingCategories,
                        isOpen = updateInput.isOpen ?: existing.isOpen,
                        opensAt = newOpensAt,
                        closesAt = newClosesAt,
                    )
                } catch (e: IllegalArgumentException) {
                    return@patch call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid input")
                }
                val updated = try {
                    feedbackAdapter.updateChannel(merged)
                } catch (e: ChannelNotFoundError) {
                    return@patch call.respond(HttpStatusCode.NotFound, e.message)
                }
                call.respond(updated.toDTO())
            }
        }
    }
}
