package no.javazone.feedback

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.javazone.feedback.database.repository.FeedbackRepositoryDb
import no.javazone.feedback.domain.adapters.FeedbackAdapter
import no.javazone.feedback.domain.errors.ChannelNotFoundError
import no.javazone.feedback.domain.generators.ExternalIdGeneratorDefault
import no.javazone.feedback.pages.feedbackPage
import no.javazone.feedback.pages.thankYouFragment
import no.javazone.feedback.qrcode.QRCodeGenerator
import no.javazone.feedback.request.channel.FeedbackChannelCreationDTO
import no.javazone.feedback.request.channel.FeedbackChannelRatingCategoryDTO
import no.javazone.feedback.request.channel.FeedbackCreationDTO
import no.javazone.feedback.request.channel.FeedbackDTO
import no.javazone.feedback.request.channel.FeedbackRatingDTO
import no.javazone.feedback.request.channel.toDTO

fun Application.setupRouting() {
    routing {
        val feedbackAdapter = FeedbackAdapter(
            repository = FeedbackRepositoryDb,
            externalIdGenerator = ExternalIdGeneratorDefault
        )
        val qrCodeGenerator = QRCodeGenerator()

        staticResources("/static", "static")

        get("/{channelId}") {
            val channelId = call.parameters["channelId"]
                ?: return@get call.respond(HttpStatusCode.NotFound)
            val channel = feedbackAdapter.findChannel(channelId)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Channel not found")
            call.respondHtml { feedbackPage(channel) }
        }

        get("/{channelId}/thank-you") {
            call.respondHtml { thankYouFragment() }
        }

        route("/v1/feedback") {
            route("channel") {
                post {
                    val input = call.receive<FeedbackChannelCreationDTO>()

                    val channel = feedbackAdapter.createFeedbackChannel(
                        input = input.toDomain()
                    )

                    call.respond(channel.toDTO())
                }

                post("{channelId}/submit-feedback") {
                    val channelId = call.parameters["channelId"] ?: return@post call.respond(
                        HttpStatusCode.NotFound,
                        "Missing externalId"
                    )

                    val feedbackInput = call.receive<FeedbackCreationDTO>()

                    val createdFeedback = try {
                        feedbackAdapter.submitFeedback(
                            channelId = channelId,
                            feedback = feedbackInput.toDomain()
                        )
                    } catch (e: ChannelNotFoundError) {
                        return@post call.respond(
                            HttpStatusCode.NotFound,
                            e.message
                        )
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
                                            title = this?.name ?: "Unknown"
                                        )
                                    },
                                    score = rating.value
                                )
                            }
                        )
                    }

                    call.respond(feedbackDto)
                }

                get("{channelId}/qrcode") {
                    val channelId = call.parameters["channelId"] ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        "Missing externalId"
                    )

                    val qrCodeBytes = feedbackAdapter.generateQrCode(
                        channelId = channelId,
                        qrCodeGenerator = qrCodeGenerator::generateQrCodeBytes
                    ) ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        "Channel with id $channelId does not exist"
                    )

                    call.respondOutputStream(
                        contentType = ContentType.Image.PNG,
                        status = HttpStatusCode.OK,
                    ) {
                        write(qrCodeBytes)
                    }
                }
            }
        }
    }
}