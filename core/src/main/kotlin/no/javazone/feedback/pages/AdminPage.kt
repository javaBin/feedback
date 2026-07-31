package no.javazone.feedback.pages

import kotlinx.html.*
import no.javazone.feedback.domain.Feedback
import no.javazone.feedback.domain.FeedbackChannel

fun HTML.adminChannelPage(channel: FeedbackChannel, feedbacks: List<Feedback>) {
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
                    span(if (channel.isOpen) "status-open" else "status-closed") {
                        +if (channel.isOpen) "Open" else "Closed"
                    }
                    +" \u00b7 ${feedbacks.size} feedback${if (feedbacks.size == 1) "" else "s"}"
                }
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
