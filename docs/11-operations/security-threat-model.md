# Security threat model

Written **before** the first security-relevant file, as the spec requires. If the code and this document disagree, one of them is a bug and `/doc-sync` decides which.

This is a threat model for an app that holds a developer's API keys, their source code, and full agent transcripts, and that executes an autonomous program on their behalf. It is not a general mobile-security essay. It covers the paths that actually exist in this app, and it says out loud where we are not defending.

## 1. What we are defending

| Asset | Where it lives | Worst outcome if lost |
|---|---|---|
| Provider API key / GitHub token | Android Keystore, referenced as `SecretRef` | Financial loss, access to the user's private repos |
| Private source code | App-private `filesDir/cca/` | Undisclosed disclosure of unreleased work |
| Conversation transcripts and tool output | Room DB, app-private | Same, plus credentials that were pasted into chat |
| Git identity and remote config | `.git/config`, credential helper | Pushes to the wrong repository |
| Autonomy decisions | Per-project policy, hard blocks in code | The agent spends money, deletes something, publishes something |
| The user's battery and time | Foreground service, wake locks | Runaway cost, drained phone |

Everything else — UI polish, animation, chat layout — is not security-relevant and is not in scope here.

## 2. Who we are defending against

| Adversary | Capability | Realistic? | Our stance |
|---|---|---|---|
| **Curious person with the phone** (the non-technical persona, a shared device, a shoulder-surfer) | Unlocked physical access to the device, no root | Very | Primary threat. App lock, `FLAG_SECURE`, no secrets in `SharedPreferences` |
| **Malicious or careless prompt content** — text inside a repo, a README, a dependency's output, an issue comment | Can inject text that the agent reads | Very | Redaction pass, permission layer, hard blocks, skill diff preview |
| **Malicious skill or plugin** | Arbitrary instructions shipped by a third party, installed in one tap | Very | Every install shows the exact file diff before writing; skills are data, not code, but they steer the agent |
| **Compromised dependency** | Runs in our process at build time or at runtime | Low but non-zero | Minimal dependency set, each justified in `10-build/dependency-versions.md`; no analytics SDK; CI secret scan |
| **Network attacker / hostile Wi-Fi** | Sees or alters traffic | Low on TLS, real on a compromised CA or a pinning-hostile network | TLS enforced, cleartext disabled, tokens never sent to unlisted hosts |
| **Another app on the device** | Android sandbox, no root | Low | App-private storage, no `MANAGE_EXTERNAL_STORAGE`, no exported components beyond the ones in the manifest doc |
| **Rooted device owner** | Full filesystem | Their own phone | Out of scope. Keystore-bound keys resist extraction, plaintext does not. Stated plainly in `privacy.md` |
| **Anthropic, GitHub, or a provider** | Sees prompts, responses, and any Git traffic we send | Certain, and not adversarial | Documented in `privacy.md`; the user can point the app at their own endpoint |
| **Our own bug** | A defect in redaction, logging, or the permission layer | Certain | This is why the permission layer is a single testable class and why logging is a redacting tree, not a convention |

The most important row is **row 2 and row 3**. The threat that matters for this product is not a stranger with the phone. It is *untrusted text steering an autonomous agent that has real credentials.* Everything in `08-orchestration/permissions.md` exists because of that.

## 3. Trust boundaries

```
┌─ untrusted ───────────────────────────────────────────────┐
│ repo contents · issue text · skill markdown · model output │
│ (content the agent reads, never content the app trusts)   │
└───────────────┬──────────────────────────────────────────┘
                │ parsed, never executed
┌───────────────▼──────────────────────────────────────────┐
│ app boundary: redaction · permission policy · hard blocks  │
└───────────────┬──────────────────────────────────────────┘
                │ only PolicyResult.Allow reaches a tool
┌───────────────▼──────────────────────────────────────────┐
│ execution boundary: backend · process tree · PTY           │
└───────────────┬──────────────────────────────────────────┘
                │ git, restricted by policy
┌───────────────▼──────────────────────────────────────────┐
│ remote boundary: GitHub · provider APIs · remote runners   │
└───────────────────────────────────────────────────────────┘
```

Two rules follow from the diagram, and they are the two most testable things in the app:

1. **No path from a tool call to a side effect that skips `PermissionPolicy`.** A tool that wants to run is a value; the policy decides; only `Allow` produces execution. Adding a new tool without a policy case is a compile-time or test-time failure, not a review comment.
2. **Untrusted text is data.** Repo files, skill bodies, and model output are never concatenated into a shell string, a path, or a URL without passing through `CommandBuilder` / `SafePath`. Command injection via a file named `; rm -rf` is a design error here, not a missing escape.

## 4. Threats and controls

Format: threat → controls → verification. Every row is testable; the last column names where.

| # | Threat | Controls | Verified by |
|---|---|---|---|
| T1 | API key leaves the device in a log, a crash report, or an export | `RedactingLogTree` at the logger boundary; `SecretStore` never returns a key except through a 15 s reveal path; `check-no-secrets.sh` scans code, fixtures, and the APK | `secrets/logging` unit test asserts a known key never appears in logcat; `09-testing/test-data-safety.md` |
| T2 | Key sent to a model in a prompt because a file or log contained it | `Redactor` scrubs key-shaped strings, `ghp_`/`sk-`-prefixed tokens, JWTs, and PEM blocks from all model-bound text, with a visible count of what was scrubbed | `shared/core` redaction test with 40 fixtures; a redaction counter is shown in the transparency log |
| T3 | Someone reads the app on a shared device | Biometric or device-credential gate on launch and wake, configurable grace period | `BiometricGate` UI test; `13-settings` screenshot of the locked state |
| T4 | Content shown in the recents screenshot or the task switcher | Optional `FLAG_SECURE` via `SecureFlagController`, on by default for the chat and terminal screens | Settings toggle test; manual check on a real device |
| T5 | The agent deletes a file, branch, tag, or repository | `HardBlockPolicy`, enforced in the permission layer, never configurable by any autonomy level, filtered again at the process/tool boundary | `22.1` hard-block tests, including the "attempt to delete is refused" case |
| T6 | The agent pushes to the default branch | Branch guard: the current branch is resolved before every push; `main`/`master`/the repo default is refused | Integration test against a local bare repo |
| T7 | The agent spends money | No paid API, no subscription, no purchase path exists in code; a project configured to use one is refused with the reason shown | `HardBlockPolicyTest`; grep for a purchase symbol in the CI secret scan |
| T8 | A malicious skill rewrites behaviour | Install always shows the file diff and the destination; scope is shown; broken skills fail validation rather than being ignored; uninstall is a move to a quarantine directory, never a delete | `skills` integration test; `10-skills-browser` UI test |
| T9 | Repo content injects a shell command | `CommandBuilder` with typed arguments, no string interpolation into `sh -c`; `SafePath` rejects traversal and symlink escape from the project root | `shared/runtime` command-construction test with hostile filenames |
| T10 | Cleartext or unexpected network egress | `network_security_config.xml` with `cleartextTrafficPermitted="false"`, an explicit host allowlist, unit test asserting the allowlist matches `privacy.md` | `07-integrations` allowlist test; CI check for new hosts |
| T11 | A remote runner receives more than it needs | Offload is opt-in per project, shown in the session record, and sends only the project directory and the prompt — never the device Keystore, never other projects' data | `remote-runners` transfer test; the session record names the backend |
| T12 | A malicious remote runner returns hostile output | Remote output is parsed as `AgentEvent` through the same mapper as local output; it is rendered as text, never evaluated | `08-orchestration` event-mapper test with a hostile fixture stream |
| T13 | Backup exfiltrates keys | Secrets are excluded from Android auto-backup; project backup is a separate, explicit, user-initiated action | `BackupManager` test; `11-operations/backup-and-restore.md` |
| T14 | A crash report contains user content | Crash reports are local-only by default; the outbound payload is a redacted, size-capped summary, and the user reviews it before sending | `CrashReporter` test asserting redaction and the size cap |
| T15 | The exported diagnostics bundle leaks secrets | The bundle runs the same redactor, lists its own contents, and refuses to build if a known secret pattern survives redaction | `diagnostics-export` test |
| T16 | The update path is tampered with | Release feed signature check, checksum before install, and the same fingerprint allowlist as signing | `AppUpdater` test; `10-build/signing-and-keystores.md` |
| T17 | Screen-off or battery saver kills a long run and loses work | Foreground service with a persistent notification, wake lock only while a run is active, run state persisted after every event | `AgentForegroundService` test; E2E "background a run and return" |
| T18 | An exported activity or receiver is reachable from another app | No exported components beyond the deep-link receiver, which validates its input and holds no permission | `AndroidManifest` lint; manifest review in `10-build/static-analysis.md` |

## 5. Authentication and authorisation, in one place

- **App access** — biometric first, device credential as fallback, grace period 0–60 s, configurable, default 0 (lock on every wake).
- **Secret access** — a reveal requires a fresh biometric prompt, is limited to 15 s, and is drawn to a screen covered by `FLAG_SECURE` while visible.
- **Project action** — the autonomy level governs tool calls; `HardBlockPolicy` governs the five rules in `22.1` and sits *below* the autonomy level, so no setting can reach it.
- **Remote action** — creating a branch, pushing, and opening a PR require the project to have a configured remote and a verified green run. A red verification cannot be overridden; it can only be re-run.

## 6. What we deliberately do not do

| Not doing | Why |
|---|---|
| Certificate pinning | It breaks users behind corporate proxies and breaks emergency rotation. TLS with platform trust plus an explicit host allowlist is the honest trade-off. Recorded in `adr-log.md` |
| Root/jailbreak detection | Detection is advisory, easily bypassed, and produces a nag rather than safety. The user is told once in the security screen, in one sentence, that a rooted device weakens the Keystore guarantee |
| Encrypting the project database beyond the filesystem | Android file-based encryption already covers app-private storage. A second layer would complicate backup and export for no threat we actually face |
| Obfuscation or anti-tamper | Adds build complexity and false assurance. The threat that matters is untrusted content, not reverse engineering |
| Blocking a run because a project looks risky | A heuristic "this repo is dangerous" check would be wrong constantly and would teach users to dismiss it. The permission layer and the visible transcript are the control |
| Rate-limiting or throttling the user | Cost is shown, with a soft advisory threshold. The user chose display over a hard cap (D23) |

## 7. Review trigger

This document is re-read, and updated, when any of these changes:

- a new tool is added to the agent's toolset
- a new network host is added
- a new data type reaches a log, an export, or a model prompt
- the autonomy levels change
- a new third-party dependency is added
- a security incident, however small, is reported by a user

## Depends on

`02-architecture/layer-contracts.md` · `08-orchestration/permissions.md` · `07-integrations/secrets.md` · `11-operations/privacy.md` · `11-operations/logging.md` · `09-testing/test-data-safety.md`
