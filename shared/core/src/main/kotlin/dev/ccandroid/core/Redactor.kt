package dev.ccandroid.core

import java.util.regex.Pattern

public interface Redactor {
    public fun redact(text: String): String
}

public class DefaultRedactor : Redactor {
    private val keyPatterns = listOf(
        Pattern.compile("sk-ant-[A-Za-z0-9_-]{10,}"),
        Pattern.compile("sk-(proj-)?[A-Za-z0-9]{20,}"),
        Pattern.compile("ghp_[A-Za-z0-9]{20,}"),
        Pattern.compile("github_pat_[A-Za-z0-9_]{30,}"),
        Pattern.compile("AIza[0-9A-Za-z_-]{30,}"),
        Pattern.compile("AKIA[0-9A-Z]{16}"),
        Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----"),
        Pattern.compile("xox[baprs]-[A-Za-z0-9-]{10,}"),
        Pattern.compile("sk_live_[A-Za-z0-9]{20,}"),
        Pattern.compile("GOCSPX-[A-Za-z0-9_-]{20,}")
    )

    override fun redact(text: String): String {
        var result = text
        for (pattern in keyPatterns) {
            val matcher = pattern.matcher(result)
            result = matcher.replaceAll("[REDACTED_SECRET]")
        }
        return result
    }
}
