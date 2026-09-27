package dev.ccandroid.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

public fun <T> Flow<T>.retryWithBackoff(
    retries: Int = 3,
    initialDelayMillis: Long = 1000,
    maxDelayMillis: Long = 10000,
    factor: Double = 2.0,
    predicate: (Throwable) -> Boolean = { true }
): Flow<T> = flow {
    var currentDelay = initialDelayMillis
    var attempts = 0
    while (true) {
        try {
            collect { emit(it) }
            break
        } catch (e: Throwable) {
            attempts++
            if (attempts > retries || !predicate(e)) {
                throw e
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMillis)
        }
    }
}
