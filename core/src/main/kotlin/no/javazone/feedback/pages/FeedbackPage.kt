package no.javazone.feedback.pages

import kotlinx.html.*
import no.javazone.feedback.domain.FeedbackChannel

fun HTML.landingPage(channels: List<FeedbackChannel>) {
    head {
        meta { charset = "utf-8" }
        meta {
            name = "viewport"
            content = "width=device-width, initial-scale=1"
        }
        title { +"Feedback Channels" }
        link {
            rel = "stylesheet"
            href = "/static/css/feedback.css"
        }
    }
    body {
        main("landing") {
            div("landing-header") {
                h1 { +"Feedback Channels" }
                p { +"Scan a QR code or tap a card to leave feedback for a session." }
            }
            if (channels.isEmpty()) {
                div("card empty-state") {
                    p { +"No feedback channels have been created yet." }
                }
            } else {
                div("channel-grid") {
                    channels.forEach { channel ->
                        a(href = "/session/${channel.externalId}", classes = "channel-card card") {
                            img(alt = "QR code for ${channel.title}") {
                                src = "/v1/feedback/channel/${channel.externalId}/qrcode"
                                width = "200"
                                height = "200"
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

fun HTML.feedbackPage(channel: FeedbackChannel) {
    head {
        meta { charset = "utf-8" }
        meta {
            name = "viewport"
            content = "width=device-width, initial-scale=1"
        }
        title { +"${channel.title} - Feedback" }
        link {
            rel = "stylesheet"
            href = "/static/css/feedback.css"
        }
        script { src = "https://unpkg.com/htmx.org@2.0.4" }
    }
    body {
        main {
            div("card") {
                h1 { +channel.title }
                p("speakers") {
                    +channel.speakers.joinToString(", ")
                }

                form {
                    id = "feedback-form"
                    attributes["data-channel-id"] = channel.externalId

                    channel.ratingCategories.forEach { category ->
                        fieldSet("rating-group") {
                            legend { +category.name }
                            div("rating-buttons") {
                                for (score in 1..5) {
                                    input(InputType.radio) {
                                        name = "rating-${category.id}"
                                        value = "$score"
                                        id = "rating-${category.id}-$score"
                                        required = true
                                    }
                                    label {
                                        htmlFor = "rating-${category.id}-$score"
                                        +"$score"
                                    }
                                }
                            }
                        }
                    }

                    div("comment-group") {
                        label {
                            htmlFor = "detailed-comment"
                            +"Comments (optional)"
                        }
                        textArea {
                            id = "detailed-comment"
                            name = "detailedComment"
                            rows = "4"
                            placeholder = "Share your thoughts..."
                        }
                    }

                    button(type = ButtonType.submit) {
                        id = "submit-btn"
                        +"Submit Feedback"
                    }

                    p("error-message") {
                        id = "error-message"
                        attributes["aria-live"] = "polite"
                    }
                }
            }
        }

        script { src = "/static/js/feedback.js" }
    }
}

fun HTML.thankYouFragment() {
    body {
        div("card thank-you") {
            div("checkmark") { +"\u2713" }
            h1 { +"Thank you!" }
            p { +"Your feedback has been submitted." }
        }
    }
}
