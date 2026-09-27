package dev.ccandroid.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreTest {

    @Test
    fun `outcome success returns value and true for isSuccess`() {
        val outcome: Outcome<String> = Outcome.Success("hello")
        assertTrue(outcome.isSuccess)
        assertFalse(outcome.isFailure)
        assertEquals("hello", outcome.getOrNull())
        assertNull(outcome.errorOrNull())
    }

    @Test
    fun `outcome failure returns error and true for isFailure`() {
        val error = AppError.Simple(
            code = ErrorCode.PROVIDER_AUTH_FAILED,
            messageDe = "Schlüssel abgelehnt",
            messageEn = "Key rejected"
        )
        val outcome: Outcome<String> = Outcome.Failure(error)
        assertFalse(outcome.isSuccess)
        assertTrue(outcome.isFailure)
        assertNull(outcome.getOrNull())
        assertEquals(error, outcome.errorOrNull())
    }

    @Test
    fun `redactor strips anthropic api keys`() {
        val redactor = DefaultRedactor()
        val text = "Calling api with sk-ant-test-0000000000000000 and query"
        val redacted = redactor.redact(text)
        assertFalse(redacted.contains("sk-ant-test"))
        assertTrue(redacted.contains("[REDACTED_SECRET]"))
    }

    @Test
    fun `redactor strips github personal access tokens`() {
        val redactor = DefaultRedactor()
        val text = "Token ghp_test00000000000000000000000000000000 used"
        val redacted = redactor.redact(text)
        assertFalse(redacted.contains("ghp_test"))
        assertTrue(redacted.contains("[REDACTED_SECRET]"))
    }

    @Test
    fun `id generator produces non-empty prefixed string`() {
        val generator = DefaultIdGenerator()
        val id = generator.newId("test")
        assertNotNull(id)
        assertTrue(id.startsWith("test_"))
        assertEquals(31, id.length) // "test_" (5) + 10 (timestamp) + 16 (random)
    }

    @Test
    fun `app error verification failure formats message properly`() {
        val error = AppError.VerificationFailure(
            failedCount = 2,
            totalCount = 5
        )
        assertEquals("2 von 5 Prüfungen fehlgeschlagen.", error.messageDe)
        assertEquals("2 of 5 checks failed.", error.messageEn)
        assertTrue(error.retryable)
    }
}
