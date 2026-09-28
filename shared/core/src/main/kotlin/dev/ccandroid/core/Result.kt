package dev.ccandroid.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Outcome<T> sealed interface for every fallible operation.
 * Guarantees typed errors via AppError instead of bare exceptions.
 */
@Serializable
public sealed interface Outcome<out T> {

    @Serializable
    public data class Success<out T>(val value: T) : Outcome<T>

    @Serializable
    public data class Failure(val error: AppError) : Outcome<Nothing>

    public val isSuccess: Boolean
        get() = this is Success

    public val isFailure: Boolean
        get() = this is Failure

    public fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    public fun errorOrNull(): AppError? = when (this) {
        is Success -> null
        is Failure -> error
    }

    public fun <R> map(transform: (T) -> R): Outcome<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    public fun <R> flatMap(transform: (T) -> Outcome<R>): Outcome<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
    }

    public fun onSuccess(action: (T) -> Unit): Outcome<T> {
        if (this is Success) action(value)
        return this
    }

    public fun onFailure(action: (AppError) -> Unit): Outcome<T> {
        if (this is Failure) action(error)
        return this
    }

    /**
     * Throws the error if this is a Failure, otherwise returns the value.
     * Use sparingly - prefer flatMap/map for chaining.
     */
    public fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw OutcomeException(error)
    }
}

/**
 * Exception wrapper for Outcome failures to enable throwing in getOrThrow.
 */
public class OutcomeException(val error: AppError) : Exception(
    "${error.code}: ${error.messageEn}",
)

/**
 * Wraps a suspend block into an Outcome, catching any Throwable and converting
 * it to a generic AppError.Simple.
 */
public suspend fun <T> tryCatch(block: suspend () -> T): Outcome<T> {
    try {
        return Outcome.Success(block())
    } catch (e: OutcomeException) {
        throw e
    } catch (e: Throwable) {
        return Outcome.Failure(
            AppError.Simple(
                code = ErrorCode.UNKNOWN_ERROR,
                messageDe = "Unerwarteter Fehler: ${e.message}",
                messageEn = "Unexpected error: ${e.message}",
                retryable = false,
                details = e.javaClass.simpleName,
            )
        )
    }
}