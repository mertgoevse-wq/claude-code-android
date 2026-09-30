package dev.ccandroid.core

import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * The structured log record from `docs/11-operations/logging.md`.
 *
 * A record is data, not a formatted line: the writers decide the format, the
 * [RedactingLogTree] decides what may leave.
 */
public data class LogRecord(
    val timestampMillis: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val attributes: Map<String, String> = emptyMap(),
)

/**
 * A sink for log records. Implementations exist for logcat and the rotating
 * file log; both are wired behind a [RedactingLogTree], never directly.
 */
public fun interface LogWriter {
    public fun write(record: LogRecord)
}

/**
 * The redaction pass from `docs/11-operations/logging.md`. Runs on every
 * record before it reaches any writer — it is not a filter a call site can
 * forget to apply.
 *
 * Secrets are replaced with their prefix plus `…redacted`, so a log stays
 * diagnosable without carrying the value. The value-shape table here is the
 * log-path authority; `DefaultRedactor` remains the redactor for raw
 * non-record text (exports), and the two tables are deliberately not merged
 * because their replacement markers serve different readers.
 *
 * @param knownSecrets values already stored in the `SecretStore`, mapped to
 *   their profile id. Empty until P2-9 wires the store in; the seam exists
 *   because a shape rule cannot recognise a user-typed key.
 */
public class LogRedactor(
    private val knownSecrets: Map<String, String> = emptyMap(),
) {

    /**
     * A shape rule and how many characters of the match to keep as the
     * visible prefix. `prefixLength < 0` replaces the whole match.
     */
    private class ShapeRule(pattern: String, val prefixLength: Int) {
        val matcher: Pattern = Pattern.compile(pattern)
    }

    private val shapeRules = listOf(
        // Order matters: the longer prefixes must win over the shorter ones.
        ShapeRule("sk-ant-[A-Za-z0-9_-]{10,}", 7),
        ShapeRule("sk-(proj-)?[A-Za-z0-9]{20,}", 3),
        ShapeRule("ghp_[A-Za-z0-9]{20,}", 4),
        ShapeRule("github_pat_[A-Za-z0-9_]{30,}", 11),
        ShapeRule("AIza[0-9A-Za-z_-]{30,}", 4),
        ShapeRule("AKIA[0-9A-Z]{16}", 4),
        ShapeRule("xox[baprs]-[A-Za-z0-9-]{10,}", 5),
        ShapeRule("sk_live_[A-Za-z0-9]{20,}", 8),
        ShapeRule("GOCSPX-[A-Za-z0-9_-]{20,}", 7),
        ShapeRule("-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----", -1),
        // A JWT: three base64url segments. Length-checked so dotted version
        // strings ("1.2.3") are never caught by it.
        ShapeRule("[A-Za-z0-9_-]{16,}\\.[A-Za-z0-9_-]{16,}\\.[A-Za-z0-9_-]{16,}", -1),
        ShapeRule("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]{8,}", -2),
        ShapeRule("(?i)([?&](?:token|key|secret|access_token)=)[^&\\s]+", -2),
    )

    /** Attribute names whose value is a secret, from the logging contract. */
    private val secretNames = setOf("apikey", "token", "secret", "password", "authorization", "cookie")

    /** Attribute names that identify a person, redacted even on debug builds. */
    private val identityNames = setOf("email", "username", "fullname")

    /** Attribute names whose value is content that never enters a log. */
    private val contentNames = setOf("content", "prompt", "body", "diff")

    /** Tool-call IO: the input may echo a key a shell command printed. */
    private val toolIoNames = setOf("input", "output")

    /**
     * Redacts free text by value shape only. This is the helper writers must
     * also run over anything they format themselves, such as a throwable's
     * message.
     */
    public fun redactText(text: String): String {
        var result = applyKnownSecrets(text)
        for (rule in shapeRules) {
            result = replaceAll(result, rule)
        }
        return result
    }

    /** Redacts one attribute value, by attribute name first, then by shape. */
    public fun redactAttribute(name: String, value: String): String {
        val key = name.lowercase(Locale.US)
        val byName = when {
            key in secretNames || key in identityNames || key in toolIoNames -> REDACTED
            key == "path" ->
                value.split('/')
                    .filter { it.isNotBlank() }
                    .takeLast(2)
                    .joinToString("/")
            key in contentNames -> "<omitted: ${value.length} chars>"
            else -> null
        }
        val shaped = applyKnownSecrets(byName ?: redactText(value))
        return if (shaped.length > MAX_ATTRIBUTE_LENGTH) {
            shaped.take(MAX_ATTRIBUTE_LENGTH) + "…truncated (${value.length} chars)"
        } else {
            shaped
        }
    }

    /** Redacts a whole record: message, every attribute, and the throwable. */
    public fun redact(record: LogRecord): LogRecord = record.copy(
        message = redactText(record.message),
        attributes = record.attributes.mapValues { (name, value) -> redactAttribute(name, value) },
        throwable = record.throwable?.let { Throwable(redactText(it.message ?: it.toString()), it.cause) },
    )

    private fun applyKnownSecrets(text: String): String {
        if (knownSecrets.isEmpty()) return text
        var result = text
        for ((value, profileId) in knownSecrets) {
            if (value.isNotEmpty()) result = result.replace(value, "<secret:$profileId>")
        }
        return result
    }

    private fun replaceAll(text: String, rule: ShapeRule): String {
        val matcher = rule.matcher.matcher(text)
        val sb = StringBuilder()
        while (matcher.find()) {
            val replacement = when {
                rule.prefixLength == -2 -> matcher.group(1) + REDACTED
                rule.prefixLength >= 0 -> matcher.group().take(rule.prefixLength) + REDACTED
                else -> REDACTED
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement))
        }
        matcher.appendTail(sb)
        return sb.toString()
    }

    private companion object {
        const val REDACTED = "…redacted"
        const val MAX_ATTRIBUTE_LENGTH = 512
    }
}

/**
 * The only way a record reaches a writer. Every [LogWriter] sits behind this
 * tree, so the redaction pass is structural, not a convention.
 */
public class RedactingLogTree(
    private val delegate: LogWriter,
    private val redactor: LogRedactor = LogRedactor(),
) : LogWriter {
    override fun write(record: LogRecord) {
        delegate.write(redactor.redact(record))
    }
}
