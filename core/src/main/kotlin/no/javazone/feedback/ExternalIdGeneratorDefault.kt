package no.javazone.feedback

import java.security.SecureRandom

object ExternalIdGeneratorDefault : ExternalIdGenerator {
    private val secureRandom = SecureRandom()
    private const val CHARS: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    override fun generate(): String {
        return (1..4)
            .map { CHARS[secureRandom.nextInt(CHARS.length)] }
            .joinToString("")

    }

}