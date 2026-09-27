package dev.ccandroid.core

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
}

public inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

public inline fun <T, R> Outcome<T>.flatMap(transform: (T) -> Outcome<R>): Outcome<R> = when (this) {
    is Outcome.Success -> transform(value)
    is Outcome.Failure -> this
}

public inline fun <T> Outcome<T>.onSuccess(action: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) action(value)
    return this
}

public inline fun <T> Outcome<T>.onFailure(action: (AppError) -> Unit): Outcome<T> {
    if (this is Outcome.Failure) action(error)
    return this
}
