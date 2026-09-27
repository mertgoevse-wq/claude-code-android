# Logging

A hand-rolled logger with redaction built into the call, not bolted on. The reason is simple: this app handles API keys, prompts, and file contents, and a logging framework that is easy to call without thinking is a logging framework that eventually writes a key to a file.

## Why not a framework

Timber, SLF4J, and every other option are fine libraries that all share one property: `Log.d(TAG, message)` is one short call away from writing anything at all. Given that a single `Log.d` in the wrong place leaks a provider key into logcat, which on a pre-Android-10 device any app with `READ_LOGS` could read, and which persists in the system's log buffer regardless, the friction is a feature.

## The interface

```kotlin
interface LogWriter {
    fun write(record: LogRecord)
}

data class LogRecord(
    val timestampMillis: Long,
    val level: Level,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val attributes: Map<String, String> = emptyMap(),
)
```

Two implementations: `AndroidLogWriter` (logcat, `release` builds redacts) and `FileLogWriter` (a rotating file in the app's private storage, used by the diagnostics export).

## The call sites

```kotlin
// A plain message. Safe: no interpolation of user data, no keys.
log.info("RuntimeProfile", "Native runtime installed", mapOf("version" to "2.1.211"))

// User or environment data goes through a typed field, which is redacted by type.
log.info("Project", "Opened project", mapOf("name" to project.name, "path" to project.displayPath))

// A secret never reaches the logger, because there is no overload that takes one.
log.warn("Secrets", "Key profile unreadable", mapOf("profileId" to id))
```

There is no `log.debug("…$token")` that compiles with a string. The typed-attribute form is the only one that accepts environment data, and `LogRedactor` inspects the attribute *values* by shape.

## Levels

| Level | Release | Debug | Contains |
|---|---|---|---|
| `VERBOSE` | no | no | Never used. It exists only so that a proposal to add it can be rejected. |
| `DEBUG` | no | yes | Internal decisions: which branch, which profile, which retry |
| `INFO` | yes | yes | Lifecycle: session start, run end, runtime install, verification result |
| `WARN` | yes | yes | Something was handled: a retry, a fallback, a deprecation |
| `ERROR` | yes | yes | Something failed and a person may need to know |

`ERROR` is not a synonym for "unhandled exception". An unhandled exception goes to crash reporting (`crash-reporting.md`); `log.error` is for failures we handled and recorded.

## Redaction

`LogRedactor` runs on every record before it reaches any writer. It is not a filter someone can forget to apply.

### By value shape

| Shape | Action |
|---|---|
| `sk-ant-…`, `sk-…`, `ghp_…`, `github_pat_…`, `AIza…`, `AKIA…` | Replaced with the prefix and `…redacted` |
| `Bearer <token>` | `Bearer …redacted` |
| Anything matching a value in the `SecretStore` | Replaced with `<secret:profileId>` |
| A JWT (three base64 segments) | Replaced entirely |
| A URL with a `token`, `key`, `secret`, or `access_token` query parameter | The parameter value is replaced |

### By attribute name

| Attribute name | Action |
|---|---|
| `apiKey`, `token`, `secret`, `password`, `authorization`, `cookie` | Value replaced unconditionally |
| `email`, `userName`, `fullName` | Value replaced, even in a debug build |
| `path` | Reduced to the last two segments — the project-relative path only |
| `content`, `prompt`, `body`, `diff` | Replaced with `<omitted: N chars>`; content never goes to a log file |
| `input`, `output` of a tool call | Replaced; a tool's input may contain a key that a shell command would echo |

### By length

Any single string attribute over 512 characters is truncated with a marker. Logs are for diagnosis; a 4 MB stack of JSON in a log file helps nobody.

## What is never logged, in any build

- API keys, tokens, cookies, `Authorization` headers — by shape and by name.
- Full prompt text or assistant replies. The *length* and a hash are logged so two records can be correlated without the content.
- File contents. A diff is logged as a summary: files changed, insertions, deletions, and a path.
- Environment variables of the engine process.
- Anything from a `.env` file, ever.

## Storage and rotation

| Sink | Location | Rotation | Retention |
|---|---|---|---|
| Logcat | system | OS-managed | Whatever the OS keeps |
| File log | `files/logs/app.log` | 5 MB per file, 4 files | 20 MB total, oldest deleted first |
| Run log | `files/logs/run-<uuid>.log` | 2 MB | Kept with the run record; **never deleted on rotation, only with the run** |
| Transparency log | the database, append-only | Never rotated silently | For the life of the project record |

The run log is separate from the app log because it has a different lifetime: a run's log belongs to that run, is exported with it, and is kept even after the general log rotates. Losing a run's diagnostics because the app log rotated is exactly the failure this separation prevents.

## Logcat and the release build

`AndroidLogWriter` in a `release` build:

- Filters to `WARN` and above.
- Passes every record through `LogRedactor` — the same one, so logcat and the file log cannot diverge in what they hide.
- Adds no tag prefix, because the release build has no business being debuggable.

On Android 10 and above, `android:loggable="false"` in the release manifest, plus the rule that no component in the release build writes to logcat from a background thread. The file log is the release build's diagnostic channel, and the diagnostics export is how a user gets it to us.

## Performance

Logging is not free, so:

- Records below `INFO` are constructed lazily — the caller passes a lambda, not an interpolated string, so nothing is built when the level is off.
- The file writer batches: records are appended to an in-memory queue and flushed every 250 ms or every 64 records, whichever comes first, on a single-threaded dispatcher.
- The file writer never runs on the main thread. The main thread touches the logger at most once per run lifecycle event.
- A malformed record (a throwable with a huge stack, a message over 64 KB) is truncated rather than allowed to allocate.

A micro-benchmark asserts that a disabled `DEBUG` call costs under 50 ns. If that regresses, the lazy-lambda optimisation was lost and the check catches it.

## Inspection

| Question | Where |
|---|---|
| What did the app do during this run? | The transparency log in the UI — the authoritative record |
| Why did the app make that decision? | The run log, filtered by tag |
| What crashed? | Crash reporting |
| What is the app's general state? | The diagnostics export (`diagnostics-export.md`) |

The transparency log and the log file overlap on purpose. The transparency log is designed to be read by a user; the log file is designed to be read by a maintainer. They are not the same artifact and neither replaces the other.
