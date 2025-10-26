package no.javazone.feedback.domain.generators

fun interface ExternalIdGenerator {
    fun generate(): String
}