package no.javazone.feedback.pages

import kotlinx.html.*
import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackChannel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ADMIN_OSLO: ZoneId = ZoneId.of("Europe/Oslo")
private val ADMIN_DATETIME_LOCAL: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
private val ADMIN_DISPLAY: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z").withZone(ADMIN_OSLO)

private fun Instant.toDatetimeLocalOslo(): String =
    LocalDateTime.ofInstant(this, ADMIN_OSLO).format(ADMIN_DATETIME_LOCAL)

private fun Instant.displayOslo(): String = ADMIN_DISPLAY.format(this)

fun HTML.adminDashboardPage(channels: List<FeedbackChannel>) {
    head {
        meta { charset = "utf-8" }
        meta {
            name = "viewport"
            content = "width=device-width, initial-scale=1"
        }
        title { +"Admin dashboard" }
        link {
            rel = "stylesheet"
            href = "/static/css/feedback.css"
        }
    }
    body {
        main("landing") {
            div("landing-header") {
                h1 { +"Admin dashboard" }
                p { +"Manage feedback channels" }
            }
            if (channels.isEmpty()) {
                div("card empty-state") {
                    p { +"No feedback channels have been created yet." }
                }
            } else {
                div("channel-grid") {
                    channels.forEach { channel ->
                        a(href = "/admin/channel/${channel.externalId}", classes = "channel-card card") {
                            img(alt = "QR code for ${channel.title}") {
                                src = "/v1/feedback/channel/${channel.externalId}/qrcode"
                                width = "200"
                                height = "200"
                                attributes["loading"] = "lazy"
                            }
                            div("channel-info") {
                                h2 { +channel.title }
                                p("speakers") {
                                    +channel.speakers.joinToString(", ")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun HTML.adminChannelPage(channel: FeedbackChannel, feedbacks: List<Feedback>, now: Instant) {
    val effectivelyOpen = channel.isEffectivelyOpen(now)
    head {
        meta { charset = "utf-8" }
        meta {
            name = "viewport"
            content = "width=device-width, initial-scale=1"
        }
        title { +"Admin - ${channel.title}" }
        link {
            rel = "stylesheet"
            href = "/static/css/feedback.css"
        }
    }
    body {
        main("admin-page") {
            div("admin-header") {
                h1 { +channel.title }
                p("speakers") { +channel.speakers.joinToString(", ") }
                p("channel-status") {
                    +"Status: "
                    span(if (effectivelyOpen) "status-open" else "status-closed") {
                        +if (effectivelyOpen) "Open" else "Closed"
                    }
                    +" \u00b7 ${feedbacks.size} feedback${if (feedbacks.size == 1) "" else "s"}"
                }
                p("channel-schedule-current") {
                    span("schedule-label") { +"Opens at" }
                    span("schedule-value") {
                        +(channel.opensAt?.displayOslo() ?: "not scheduled")
                    }
                    span("schedule-label") { +"Closes at" }
                    span("schedule-value") {
                        +(channel.closesAt?.displayOslo() ?: "not scheduled")
                    }
                    span("schedule-label") { +"Submissions" }
                    span("schedule-value") {
                        +(if (channel.isOpen) "enabled" else "disabled")
                    }
                }
            }

            detailsForm(channel)

            scheduleForm(channel)

            section("feedback-section") {
                h2("section-heading") {
                    +"Feedback"
                    span("section-count") { +"${feedbacks.size}" }
                }
                if (feedbacks.isEmpty()) {
                    div("card empty-state") {
                        p { +"No feedback has been submitted yet." }
                    }
                } else {
                    div("feedback-list") {
                        feedbacks.forEach { feedback ->
                            feedbackRow(channel, feedback)
                        }
                    }
                }
            }
        }
    }
}

private fun FlowContent.detailsForm(channel: FeedbackChannel) {
    details("details-form-wrapper") {
        summary("edit-toggle") { +"\u270e Edit details" }
        div("card details-form-card") {
            h2("section-heading") { +"Details" }
            p("section-hint") { +"One speaker per line." }
            form(
                method = FormMethod.post,
                action = "/admin/channel/${channel.externalId}/details",
            ) {
                classes = setOf("details-form")

                div("details-field") {
                    label {
                        htmlFor = "channel-title"
                        +"Title"
                    }
                    input(InputType.text) {
                        id = "channel-title"
                        name = "title"
                        value = channel.title
                        required = true
                    }
                }

                div("details-field") {
                    label {
                        htmlFor = "channel-speakers"
                        +"Speakers"
                    }
                    textArea {
                        id = "channel-speakers"
                        name = "speakers"
                        rows = "3"
                        +channel.speakers.joinToString("\n")
                    }
                }

                div("feedback-actions") {
                    button(type = ButtonType.submit, classes = "save-btn") { +"Save details" }
                }
            }
        }
    }
}

private fun FlowContent.scheduleForm(channel: FeedbackChannel) {
    div("card schedule-form-card") {
        h2("section-heading") { +"Schedule" }
        p("section-hint") {
            +"Times are in Europe/Oslo. Leave a field empty to remove that schedule."
        }
        form(
            method = FormMethod.post,
            action = "/admin/channel/${channel.externalId}/schedule",
        ) {
            classes = setOf("schedule-form")

            div("schedule-master-switch") {
                label("switch-label") {
                    input(InputType.checkBox) {
                        name = "isOpen"
                        value = "true"
                        checked = channel.isOpen
                    }
                    span { +"Enable feedback submissions" }
                }
                p("switch-hint") {
                    +"Must be enabled for feedback to be accepted. The scheduled window below further restricts when submissions are allowed."
                }
            }

            div("schedule-field") {
                label {
                    htmlFor = "opens-at"
                    +"Opens at"
                }
                input(InputType.dateTimeLocal) {
                    id = "opens-at"
                    name = "opensAt"
                    value = channel.opensAt?.toDatetimeLocalOslo() ?: ""
                }
            }

            div("schedule-field") {
                label {
                    htmlFor = "closes-at"
                    +"Closes at"
                }
                input(InputType.dateTimeLocal) {
                    id = "closes-at"
                    name = "closesAt"
                    value = channel.closesAt?.toDatetimeLocalOslo() ?: ""
                }
            }

            div("feedback-actions") {
                button(type = ButtonType.submit, classes = "save-btn") { +"Save schedule" }
            }
        }
    }
}

private fun FlowContent.feedbackRow(channel: FeedbackChannel, feedback: Feedback) {
    div("card feedback-row") {
        form(
            method = FormMethod.post,
            action = "/admin/channel/${channel.externalId}/feedback/${feedback.id}/update",
        ) {
            classes = setOf("feedback-edit-form")

            feedback.ratings.forEach { rating ->
                fieldSet("rating-group") {
                    legend { +rating.name }
                    div("rating-buttons") {
                        for (score in 1..5) {
                            input(InputType.radio) {
                                name = "rating-${rating.id}"
                                value = "$score"
                                id = "feedback-${feedback.id}-rating-${rating.id}-$score"
                                checked = rating.value == score
                                required = true
                            }
                            label {
                                htmlFor = "feedback-${feedback.id}-rating-${rating.id}-$score"
                                +"$score"
                            }
                        }
                    }
                }
            }

            div("comment-group") {
                label {
                    htmlFor = "feedback-${feedback.id}-comment"
                    +"Comment"
                }
                textArea {
                    id = "feedback-${feedback.id}-comment"
                    name = "detailedComment"
                    rows = "3"
                    +(feedback.comment ?: "")
                }
            }

            div("feedback-actions") {
                button(type = ButtonType.submit, classes = "save-btn") { +"Save" }
            }
        }

        form(
            method = FormMethod.post,
            action = "/admin/channel/${channel.externalId}/feedback/${feedback.id}/delete",
        ) {
            classes = setOf("feedback-delete-form")
            button(type = ButtonType.submit, classes = "delete-btn") {
                onClick = "return confirm('Delete this feedback?');"
                +"Delete"
            }
        }
    }
}
