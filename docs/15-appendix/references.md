# References

Every external source this project depends on, with its URL and the date it was last checked.

## The rule about this file

A citation here is a **claim that a person looked at the source**. Therefore:

- Every entry has an access date. An entry without one is a placeholder, not a reference.
- A URL is never reconstructed from memory. If a link is in doubt, it is marked `TBD — verify at build time` and stays that way until someone opens it.
- A number taken from a source is quoted with the source and the date, because quota numbers and version numbers move.
- If a fact could not be verified, it is written as unverified. It is never smoothed over with a plausible value.

The purpose is not completeness. It is that a reader can tell, for any factual claim in the documentation set, whether it was checked or guessed.

**Last full review of this file:** 2026-09-27.

---

## 1. Anthropic and Claude Code

| Source | URL | Accessed | Used for |
|---|---|---|---|
| Claude Code documentation | `TBD — verify at build time` | — | The engine's flags, output formats, and permission modes, recorded in `01-research/agent-sdk-and-cli-surface.md` |
| Claude Code distribution / download endpoint for `linux-arm64` | `TBD — verify at build time` | — | The engine download in `06-runtime/native-profile.md` |
| Published checksum list for the engine binary | `TBD — verify at build time` | — | Checksum verification. **If this cannot be found, the design changes** — see R1 in `14-build-plan/risk-register.md` |
| Upstream issue: official Android build | recorded in the spec as `anthropics/claude-code#50270` — **verify the number and that the issue exists before citing it in a commit or the README** | — | R2 in the risk register; the unofficial status in `01-research/legal-and-trademark.md` |
| Anthropic Messages API | `TBD — verify at build time` | — | The `ANTHROPIC` provider kind in `07-integrations/providers.md` |
| Anthropic pricing | `TBD — verify at build time` | — | The cost model in `05-features/chat-and-streaming.md`. **Never hardcoded; the app reads what the response reports** |

All five are marked unverified because the exact URLs and the current shape of these endpoints are build-time facts. `13-process/claude-code-instructions.md` §5 forbids inventing them.

## 2. Android platform

| Source | URL | Accessed | Used for |
|---|---|---|---|
| Android developer documentation | `https://developer.android.com/` | 2026-09-27 | Everything in `10-build/` and `11-operations/` |
| Jetpack Security — Keystore | `https://developer.android.com/privacy-and-security/keystore` | 2026-09-27 | `SecretStore` in `07-integrations/secrets.md`; T9 in the threat model |
| BiometricPrompt | `https://developer.android.com/identity/sign-in/biometric-auth` | 2026-09-27 | `BiometricGate`; the app lock in `05-features/` |
| Foreground services | `https://developer.android.com/develop/background-work/services/fgs` | 2026-09-27 | `AgentForegroundService`; R7 and R14 |
| Network security configuration | `https://developer.android.com/privacy-and-security/security-config` | 2026-09-27 | T10 in the threat model; the host allowlist in `11-operations/privacy.md` |
| App backup and restore | `https://developer.android.com/develop/backup-and-restore` | 2026-09-27 | Secret exclusion in `11-operations/backup-and-restore.md` |
| `FLAG_SECURE` | `https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_SECURE` | 2026-09-27 | T4 in the threat model |
| Android Virtualization Framework | `https://developer.android.com/topic/architecture/avf` | 2026-09-27 | The experimental profile in `06-runtime/avf-profile.md` |
| WorkManager | `https://developer.android.com/topic/libraries/architecture/background-work` | 2026-09-27 | Resumable work in `06-runtime/bootstrap-state-machine.md` |
| Gradle dependency verification | `https://docs.gradle.org/current/userguide/dependency_verification.html` | 2026-09-27 | `gradle/verification-metadata.xml` in `12-delivery/f-droid.md` |
| Gradle configuration cache | `https://docs.gradle.org/current/userguide/configuration_cache.html` | 2026-09-27 | The cache rule in `10-build/gradle-setup.md` |
| Kotlin Multiplatform | `https://kotlinlang.org/docs/multiplatform.html` | 2026-09-27 | The `shared/` layer; D5 and D20 |

## 3. Linux compatibility

| Source | URL | Accessed | Used for |
|---|---|---|---|
| Termux packages — `glibc-runner` | `TBD — verify at build time` | — | The native profile in `06-runtime/native-profile.md`. **The exact package name, repository, and version must be confirmed at build time** |
| Termux packages — `patchelf-glibc` | `TBD — verify at build time` | — | The ELF interpreter patch. Same verification requirement |
| proot-distro | `https://github.com/termux/proot-distro` | 2026-09-27 | Profile B in `06-runtime/proot-profile.md` |
| proot | `https://github.com/termux/proot` | 2026-09-27 | The syscall interception layer Profile B depends on |
| patchelf | `https://github.com/NixOS/patchelf` | 2026-09-27 | The tool that rewrites the ELF interpreter field |
| ELF specification | `https://refspecs.linuxfoundation.org/elf/elfspec_ppc.pdf` | 2026-09-27 | The `PT_INTERP` field being rewritten |
| Android's Bionic libc | `https://source.android.com/docs/core/architecture/libs/bionic` | 2026-09-27 | Why the glibc binary cannot run unmodified — the core problem in `01-research/claude-code-runtimes-on-android.md` |

## 4. GitHub

| Source | URL | Accessed | Used for |
|---|---|---|---|
| REST API reference | `https://docs.github.com/en/rest` | 2026-09-27 | `07-integrations/github-api.md` |
| OAuth device flow | `https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps` | 2026-09-27 | `07-integrations/github-auth.md` |
| Fine-grained personal access tokens | `https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens` | 2026-09-27 | The zero-setup auth path; R6 |
| GitHub Actions usage limits | `https://docs.github.com/en/billing/managing-billing-for-your-products/managing-billing-for-github-actions` | — | `07-integrations/remote-runners.md`. **Verify the current free-minute numbers at build time and record the access date; they change** |
| GitHub Actions self-hosted runners | `https://docs.github.com/en/actions/hosting-your-own-runners` | 2026-09-27 | The "home PC as a runner" option |
| GitHub notification API and webhooks | `https://docs.github.com/en/webhooks` | 2026-09-27 | `07-integrations/github-notifications.md`; the polling fallback |

## 5. Free hosting

| Source | URL | Accessed | Used for |
|---|---|---|---|
| Oracle Cloud Always Free tier terms | `TBD — verify at build time` | — | The primary remote runner. **The quota and the terms both change; the app checks live at setup rather than trusting a number from a document** |
| Oracle Cloud documentation | `https://docs.oracle.com/en-us/iaas/Content/home.htm` | 2026-09-27 | Instance shape and A1 availability |
| GitHub Actions | see §4 | 2026-09-27 | The secondary remote runner |

The reason the Oracle row is unverified is the same as the quota rows everywhere else: the spec's D2 and R5 both say *verify quotas at build time*. A document that hardcodes a free-tier quota becomes a lie within a month, and a user who trusts it overpays or gets shut off. The app does a live check; this file records that the number was not hardcoded, and why.

## 6. Provider APIs

| Source | URL | Accessed | Used for |
|---|---|---|---|
| OpenAI Chat Completions | `https://platform.openai.com/docs/api-reference/chat` | 2026-09-27 | The `OPENAI_CHAT` provider kind |
| OpenAI Responses API | `https://platform.openai.com/docs/api-reference/responses` | 2026-09-27 | The `OPENAI_RESPONSES` provider kind |
| OpenAI-compatible servers (Ollama, vLLM, LM Studio) | `TBD — verify at build time` | — | The `CUSTOM` provider kind in `07-integrations/providers.md` |
| Server-sent events (the format, not a vendor) | `https://html.spec.whatwg.org/multipage/server-sent-events.html` | 2026-09-27 | The streaming parser in `shared/data` |

## 7. Testing

| Source | URL | Accessed | Used for |
|---|---|---|---|
| Paparazzi (screenshot tests on the JVM) | `https://github.com/cashapp/paparazzi` | 2026-09-27 | `09-testing/screenshot-tests.md` |
| Kover (Kotlin coverage) | `https://github.com/KotlinDevTools/kover` | 2026-09-27 | The coverage gates in `09-testing/test-strategy.md` |
| Turbine (Flow testing) | `https://github.com/cashapp/turbine` | 2026-09-27 | `09-testing/unit-tests.md` |
| MockK | `https://mockk.io/` | 2026-09-27 | Test doubles |
| Compose UI testing | `https://developer.android.com/develop/ui/compose/testing` | 2026-09-27 | `09-testing/ui-tests.md` |
| Macrobenchmark | `https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview` | 2026-09-27 | `09-testing/performance-budgets.md` |

## 8. Design and accessibility standards

| Source | URL | Accessed | Used for |
|---|---|---|---|
| WCAG 2.1 | `https://www.w3.org/TR/WCAG21/` | 2026-09-27 | The AA targets in `03-design/accessibility.md` |
| Material Design 3 | `https://m3.material.io/` | 2026-09-27 | Component structure — **not** the default palette, which `03-design/anti-slop-rules.md` bans |
| Compose animation APIs | `https://developer.android.com/develop/ui/compose/animation` | 2026-09-27 | The animated mark in `03-design/logo-animation.md` |

## 9. Distribution

| Source | URL | Accessed | Used for |
|---|---|---|---|
| F-Droid inclusion policy and build requirements | `https://f-droid.org/docs/Inclusion_Policy/` | 2026-09-27 | `12-delivery/f-droid.md` |
| F-Droid build recipes | `https://f-droid.org/docs/Build_Metadata_Reference/` | 2026-09-27 | The recipe in `12-delivery/f-droid.md` |
| Google Play Console publishing requirements | `https://support.google.com/googleplay/android-developer/answer/9859455` | 2026-09-27 | `12-delivery/play-store.md`. **The closed-testing numbers change; verify them at publish time** |
| Play data safety form | `https://support.google.com/googleplay/android-developer/answer/10787469` | 2026-09-27 | The form answers in `12-delivery/play-store.md` |
| Play App Bundle | `https://developer.android.com/guide/app-bundle` | 2026-09-27 | The AAB artefact |

## 10. FOSS dependency allowlist

Every third-party dependency in `10-build/dependency-versions.md` must appear in the F-Droid recipe with its licence, per `tools/check_foss_dependencies.sh`. Known entries at the time of writing:

| Dependency | Licence | Source |
|---|---|---|
| Kotlin / kotlinx.* | Apache 2.0 | `https://github.com/JetBrains/kotlin` |
| Jetpack (AndroidX, Compose) | Apache 2.0 | `https://developer.android.com/jetpack` |
| Room | Apache 2.0 | `https://developer.android.com/training/data-storage/room` |
| Ktor | Apache 2.0 | `https://github.com/ktorio/ktor` |
| Koin | Apache 2.0 | `https://github.com/InsertKoinIO/koin` |
| Ktor / Koin multiplatform builds | Apache 2.0 | `https://github.com/Kotlin/kotlinx-io`, per dependency as pinned |
| Paparazzi, Kover, Turbine, MockK | Apache 2.0 | The repositories in §7 |
| Android Gradle Plugin | Apache 2.0 | `https://developer.android.com/build` |
| Claude Code engine | Proprietary, **not redistributed** | Downloaded at runtime; see §1 and `12-delivery/f-droid.md` |

## 11. How to add an entry

1. Open the source and read it. Do not cite from memory.
2. Record the URL and today's date.
3. If it is a number that will change — a version, a quota, a price, a policy — say so next to it, and say when it must be re-checked.
4. If you could not verify it, write `TBD — verify at build time` and leave the URL empty. An honest gap is the correct entry; a plausible URL is not.

## Depends on

`01-research/*` · `10-build/dependency-versions.md` · `12-delivery/f-droid.md` · `13-process/claude-code-instructions.md` · `claude-code-android-spec.md` §0
