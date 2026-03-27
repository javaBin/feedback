package no.javazone.feedback.domain

import no.javazone.feedback.domain.generators.ExternalIdGenerator

internal class SequentialIdGenerator(vararg ids: String) : ExternalIdGenerator {
    private val iterator = ids.iterator()

    override fun generate(): String {
        return iterator.next()
    }
}