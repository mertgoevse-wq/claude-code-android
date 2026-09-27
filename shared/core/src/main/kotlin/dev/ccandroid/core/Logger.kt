package dev.ccandroid.core

public enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

public interface Logger {
    public fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
    public fun debug(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    public fun info(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    public fun warn(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, tag, message, throwable)
    public fun error(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)
}

public class RedactingLogger(
    private val redactor: Redactor = DefaultRedactor(),
    private val minLevel: LogLevel = LogLevel.DEBUG
) : Logger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (level.ordinal < minLevel.ordinal) return
        val sanitized = redactor.redact(message)
        println("[$level] $tag: $sanitized")
        throwable?.let {
            println("Exception: ${it.message}")
        }
    }
}
