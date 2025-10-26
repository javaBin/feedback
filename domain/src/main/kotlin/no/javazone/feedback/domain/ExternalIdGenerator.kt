package no.javazone.feedback.domain

interface ExternalIdGenerator {
    fun generate(): String
}