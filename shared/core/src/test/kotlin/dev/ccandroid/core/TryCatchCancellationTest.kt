package dev.ccandroid.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * tryCatch sits at the repository boundary, and code rule 5 says cancellation
 * propagates. A coroutine that is cancelled while a repository call is in
 * flight must arrive as a cancellation, not as a fake "unexpected error"
 * outcome: swallowing it makes the caller believe the operation failed
 * normally and keeps the coroutine pretending to work.
 */
class TryCatchCancellationTest {

    @Test
    fun `a cancellation passes through as a cancellation, not as a failure`() = runTest {
        val thrown = runCatching {
            tryCatch<Unit> { throw CancellationException("left the screen") }
        }.exceptionOrNull()

        assertTrue(
            "Cancellation must propagate, was: $thrown",
            thrown is CancellationException,
        )
    }

    @Test
    fun `a normal failure is still converted to an Outcome`() = runTest {
        val outcome = tryCatch<Unit> { throw IllegalStateException("db closed") }

        assertTrue(outcome.isFailure)
        assertEquals(ErrorCode.UNKNOWN_ERROR, outcome.errorOrNull()?.code)
    }
}
