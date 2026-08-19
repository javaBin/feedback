package no.javazone.feedback.pages

import kotlinx.html.*
import no.javazone.feedback.domain.FeedbackChannel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val OSLO: ZoneId = ZoneId.of("Europe/Oslo")
private val DISPLAY_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.forLanguageTag("no")).withZone(OSLO)

private fun Instant.formatOslo(): String = DISPLAY_FORMAT.format(this)

fun HTML.codeEntryPage(code: String? = null, error: String? = null) {
    head {
        meta { charset = "utf-8" }
        meta {
            name = "viewport"
            content = "width=device-width, initial-scale=1"
        }
        title { +"JavaZone Feedback" }
        link {
            rel = "stylesheet"
            href = "/static/css/feedback.css"
        }
    }
    body {
        main {
            div("card") {
                h1 { +"Gi tilbakemelding" }
                p { +"Skriv inn den 4-tegns koden fra skjermen." }

                form(method = FormMethod.post, action = "/", classes = "code-form") {
                    label {
                        htmlFor = "code"
                        +"Kode"
                    }
                    input(InputType.text, classes = "code-input") {
                        id = "code"
                        name = "code"
                        value = code.orEmpty()
                        required = true
                        minLength = "4"
                        maxLength = "4"
                        pattern = "[A-Za-z0-9]{4}"
                        autoFocus = true
                        attributes["autocomplete"] = "off"
                        attributes["autocapitalize"] = "characters"
                        attributes["inputmode"] = "latin"
                    }

                    button(type = ButtonType.submit) { +"Gå til skjema" }
                }

                if (error != null) {
                    p("error-message") {
                        attributes["aria-live"] = "polite"
                        +error
                    }
                }
            }
        }
    }
}

fun HTML.feedbackPage(channel: FeedbackChannel, now: Instant, isAdmin: Boolean = false) {
    val effectivelyOpen = channel.isEffectivelyOpen(now)
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
            if (isAdmin) {
                a(
                    href = "/admin/channel/${channel.externalId}",
                    classes = "admin-edit-link",
                ) { +"Edit in admin" }
            }
            div("card") {
                h1 { +channel.title }
                p("speakers") {
                    +channel.speakers.joinToString(", ")
                }

                if (!effectivelyOpen) {
                    div("channel-closed-notice") {
                        val opensAt = channel.opensAt
                        val closesAt = channel.closesAt
                        when {
                            !channel.isOpen -> {
                                h2 { +"Feedback is not open yet" }
                                p { +"Please check again closer to when the session starts" }
                            }
                            opensAt != null && now.isBefore(opensAt) -> {
                                h2 { +"Feedback is not open yet" }
                                p { +"Opens ${opensAt.formatOslo()}" }
                            }
                            closesAt != null && !now.isBefore(closesAt) -> {
                                h2 { +"Feedback is closed" }
                                p { +"This session closed for feedback ${closesAt.formatOslo()}" }
                            }
                            else -> {
                                h2 { +"Feedback is not open" }
                                p { +"Please check again closer to when the session starts" }
                            }
                        }
                    }
                } else {
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
        }

        if (effectivelyOpen) {
            script { src = "/static/js/feedback.js" }
        }
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
