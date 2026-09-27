# Secrets

API keys and tokens: how they are stored, how they are used, and how they never end up anywhere they should not.

## What counts as a secret here

| Secret | Where it lives | Notes |
|---|---|---|
| A provider API key | The Keystore, encrypted | Never in the database, never in a field, never in a log |
| A GitHub token | The Keystore, encrypted | Same rules |
| An SSH private key | The Keystore, encrypted | Generated on the device, never displayed after creation |
| The repository token for self-repair | The Keystore, encrypted | Scoped to one repository |
| A key file path | The Keystore | Not a value, but still sensitive, so encrypted |

| Not a secret | Why |
|---|---|
| A provider's base URL | Configuration, and it appears in the UI by design |
| A model id | Configuration |
| A project path | It appears in the file browser, in the run header, and in the log |
| The last four characters of a key | A hint for telling profiles apart. Deliberately four, and never more. |

## Storage

```
Android Keystore
  └── AES-256-GCM key, hardware-backed where the device has it
        └── encrypts → SecretProfile.cipherText  (in the Room database)
```

| Layer | What |
|---|---|
| The key | Generated in the Keystore with `KeyGenParameterSpec`, `PURPOSE_ENCRYPT` and `PURPOSE_DECRYPT`, `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`, no user authentication required, so the app can decrypt in the background while a run proceeds. |
| The ciphertext | The IV, the tag, and the ciphertext, base64, in a dedicated column in a dedicated table |
| Why not `EncryptedSharedPreferences` | It works, but it puts the key material and the ciphertext in the same store and hides the construction. Doing it explicitly is four lines and one fewer dependency. |
| Why not the DataStore | It is plaintext by design. Settings are not secrets. |
| Hardware backing | Where the device has a secure element, the Keystore key is hardware-backed. Where it does not, it is software-backed. The app does not claim a guarantee the device cannot make, and the security screen says which applies. |
| Backups | The Keystore key is not included in an Android backup, so a restored ciphertext cannot be decrypted. Nothing to exclude, because nothing is recoverable. |
| Uninstall | The key and the ciphertext are both removed |
| The secret in memory | Only for the duration of one request. Cleared afterwards, and never assigned to a field. |

## `CryptoGateway`

The only way anything in the app touches a key. An `expect` in `shared/core`, an `actual` in `androidApp`.

```kotlin
interface CryptoGateway {
    fun generateKey(alias: String): Boolean
    fun encrypt(alias: String, plaintext: ByteArray): ByteArray
    fun decrypt(alias: String, ciphertext: ByteArray): ByteArray
    fun isHardwareBacked(alias: String): Boolean
    fun deleteKey(alias: String)
}
```

| Rule | Enforcement |
|---|---|
| Only `androidApp` implements it | The Keystore import appears in one file |
| Only `KeystoreSecretStore` calls it | A static check; the database repository stores ciphertext and has no access to the plaintext |
| A `SecretRef` is a value type | An id, never a key. A `SecretRef` cannot be printed usefully, which removes a whole class of accident. |
| The plaintext is never returned to a caller that would store it | The decrypt result goes straight into a header and is dropped |

## The redaction pass

The second layer, and the one that catches what the first misses.

```kotlin
object Redactor {
    private val patterns = listOf(
        Regex("""\bsk-ant-[A-Za-z0-9_-]{20,}"""),            // Anthropic
        Regex("""\bsk-[A-Za-z0-9]{32,}"""),                  // OpenAI and others
        Regex("""\bghp_[A-Za-z0-9]{36}"""),                  // GitHub classic
        Regex("""\bgithub_pat_[A-Za-z0-9_]{50,}"""),         // GitHub fine-grained
        Regex("""\bgho_[A-Za-z0-9]{36}"""),                  // GitHub OAuth
        Regex("""\bAKIA[0-9A-Z]{16}"""),                     // AWS access key id
        Regex("""\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}"""), // JWT
        Regex("""(?i)(authorization\s*[:=]\s*)(bearer\s+)?\S+"""),
        Regex("""(?i)(api[_-]?key|token|secret|password)\s*[:=]\s*\S+"""),
        Regex("""-----BEGIN [A-Z ]*PRIVATE KEY-----"""),
    )
    fun redact(text: String): String
}
```

| Property | Behaviour |
|---|---|
| Shape-preserving | `sk-ant-api03-abc…xyz` becomes `sk-ant-api03-[redacted]…[redacted]`, so a reader sees that a key was there |
| Applied at every sink | Before a log write, before an exception message, before a crash report, before a diagnostics bundle, before a notification, before a file write of raw output |
| Idempotent | Redacting twice changes nothing the second time, so a double-redacted string is not mangled |
| Cheap enough for the hot path | A compiled regex set, applied only to strings crossing a boundary, never to the streaming path's per-byte data |
| Tested | A corpus of ten known key formats, seeded into every sink, asserted absent from every output |

**The redaction pass is a safety net, not the design.** The design is that a key is a `SecretRef` everywhere except at the one moment a header is built. The redaction catches the case where a key arrives inside data the app did not control — a user's message, a tool's output, an error page from a proxy.

## The user's own input

Somebody pastes a key into a chat message. This happens.

| Step | Behaviour |
|---|---|
| 1 | The redaction pass runs on the message text |
| 2 | If something was redacted, the message is flagged `isRedacted` and the user is told, in the transcript: "Ein Teil dieser Nachricht wurde ersetzt, weil er wie ein Schlüssel aussah." |
| 3 | The redacted text is what is sent to the model and what is persisted |
| 4 | The original is never stored in a column a reader might mistake for what was sent |

| Also | Behaviour |
|---|---|
| A key in a pasted log | Redacted, with the same notice |
| A key in an attached file | The file is **refused**, with the pattern named: "Diese Datei enthält einen API-Schlüssel und wird nicht angehängt." A refusal, not a redaction, because a file is not a place to partially rewrite somebody's data. |
| A key in a terminal command | The terminal is a real terminal. It is not redacted, because redacting a terminal would break it. The transparency log records that terminal output may contain a pasted key, in its own About panel, and the redaction applies to the log's copy. |
| A key in a git remote URL | `https://user:token@github.com/…` is a real pattern. The app refuses to use such a remote and says why: "Die Adresse enthält ein eingebettetes Token. Bitte nutze die Anmeldedaten." |

## Revealing a key

| Step | Behaviour |
|---|---|
| In the UI | The stored value is masked permanently. There is no "show" that reveals the stored value without a check. |
| Reveal | A fresh biometric authentication, every time |
| Duration | 15 seconds, then it re-masks itself |
| While revealed | A visible indicator, and a haptic when it re-masks |
| A screenshot while revealed | Not preventable. The reveal is a deliberate act by an authenticated user, and the indicator is there so a screenshot is visibly a choice. |
| The screen-reader tree | A revealed key is **not** in it, even after a successful authentication. A screen reader reading a secret aloud is worse than not being able to read it. |
| The log | A `SECURITY` entry, per `05-features/transparency-log.md`. A key reveal is exactly the kind of event worth noticing later. |

## Rotation

| Aspect | Behaviour |
|---|---|
| Editing a key | Replace it. The ciphertext is overwritten, not versioned. The old key is not kept "just in case", because a kept old key is a kept old secret. |
| The last four characters | Updated, so a profile is still identifiable |
| A changed key | The next request uses the new one. A failure is reported as an authentication failure with a link to the key's screen. |
| No automatic retry with the old value | There is no old value to retry with. |

## Deleting

| Deletes | Never |
|---|---|
| The ciphertext row | Anything at the provider. The app cannot know whether a key still exists there. |
| The Keystore key alias | Any other profile. Each profile has its own alias. |
| The reference from every provider that used it | A project, a conversation, a run, or a log entry. |
| The last four characters | — |

The confirmation says exactly that: "Der Schlüssel wird von dieser App vergessen. Ob er bei {provider} noch gilt, kann sie nicht wissen. Widerrufe ihn dort, wenn du ihn nicht mehr brauchst." **The app is telling the user to go and revoke it, because the app cannot.** That sentence is the honest boundary of what a BYOK app can do about key lifecycle.

A provider referencing a deleted profile becomes `BROKEN` in the list, with a "Schlüssel zuordnen" action. It is not silently disabled, because a provider that quietly stopped working would fail in the middle of a run.

## Where a secret must never appear

| Sink | Enforcement | Test |
|---|---|---|
| The activity log | `Redactor` on the write path | Seeded in every category |
| A crash report | `Redactor` | Seeded in the log that preceded a crash |
| A diagnostics bundle | `Redactor`, plus a structural exclusion of `SecretProfile` | Seeded in every place the app can read |
| An exception message | Ktor's request redacted at construction | A 401 whose response body contains a key |
| A notification | The strings are precomputed and contain no secret | A unit test over every notification template |
| A file of raw output | `Redactor` before the write | A tool printing its own environment |
| The activity log's own export | `Redactor` | Seeded |
| The settings export | The table is structurally excluded | A test that no field of the export matches a key pattern |
| A screenshot | Not possible to control | An `isRedacted` flag, so a reader knows |
| The accessibility tree | Never, even on reveal | Asserted by a UI test |
| A GitHub issue, if the user pastes one | The user's choice | The log's About panel notes that exported logs are redacted and the user's own pastes are not ours to control |

## Testing

| Test | Type |
|---|---|
| `RoundTrip` | E2E — encrypt, store, decrypt, use, clear, for a provider key, a GitHub token, and an SSH key |
| `KeystoreOnlyInOneFile` | Static — the Keystore import appears only in `androidApp/.../KeystoreSecretStore.kt` |
| `NoPlaintextInDatabase` | Static — no column name suggests key material except the encrypted one, and its value is ciphertext |
| `HardwareBackingReported` | E2E — the security screen reports the device's actual backing, not a claim |
| `BackupUndecryptable` | E2E — a restored backup cannot decrypt a stored key |
| `UninstallDestroys` | E2E — after an uninstall and reinstall, no key is recoverable |
| `RedactionCorpus` | Unit — ten key formats, eleven sinks, zero occurrences in any output |
| `RedactionIdempotent` | Unit — redacting twice equals redacting once |
| `RedactionShapePreserving` | Unit — a redacted key still looks like a key, so a reader knows one was there |
| `UserMessageRedacted` | E2E — a pasted key is redacted, the notice appears, and the original is not stored |
| `KeyFileRefused` | E2E — an attachment containing a key is refused with the pattern named |
| `TokenInRemoteUrlRefused` | E2E — a remote URL with an embedded token is refused with the reason |
| `RevealRequiresBiometric` | E2E — without a successful check, no reveal |
| `RevealAutoHides` | E2E — after 15 seconds it re-masks, with a haptic |
| `RevealNotInAccessibility` | UI — a revealed key is absent from the accessibility tree, asserted |
| `RevealLogged` | Integration — every reveal produces a `SECURITY` log entry |
| `RotationNoOldValue` | E2E — after a rotation, the previous ciphertext is gone and there is nothing to fall back to |
| `DeleteHonest` | UI — the confirmation states the app cannot revoke at the provider, and names the provider |
| `DeleteBreaksProvider` | E2E — a provider whose key was deleted shows `BROKEN` with a repair action, not silently disabled |
| `SecretRefNotPrintable` | Unit — a `SecretRef`'s `toString` contains no key material |
| `TerminalNotRedacted` | E2E — terminal output is not rewritten, and the log's About panel says so |
