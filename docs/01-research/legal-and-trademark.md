# Legal and trademark

**This is an engineering document, not legal advice.** It exists so that a build decision is never made without knowing the constraint. Anyone with real questions about their own use should ask a lawyer.

## What we are and are not

| | |
|---|---|
| We are | An independent, open-source client that runs the publicly distributed Claude Code binary and talks to a provider API with the user's own key |
| We are not | Affiliated with, endorsed by, sponsored by, or reviewed by Anthropic |
| We do | Distribute the app publicly, in the Play Store, F-Droid, and as an APK |
| We do not | Resell access, proxy a subscription, redistribute a binary we did not download ourselves, or collect telemetry |
| We will not | Offer a claude.ai subscription login, because Anthropic does not permit third-party applications to do so |

## The subscription boundary

Anthropic's documentation for the Agent SDK is explicit: third-party developers are not allowed to offer claude.ai login or subscription rate limits for their products, including agents built on the SDK. The documented alternatives are the API key, Amazon Bedrock, Claude on AWS, Google Cloud's Agent Platform, and Microsoft Foundry.

**Consequence, and it is a product decision, not a limitation we regret:** the app is BYOK-only. There is no "sign in with your Claude account". This is why the onboarding asks for a key, why the provider screen is a first-class feature, and why supporting OpenAI-compatible providers matters — the user is paying a provider directly, always.

## Names and marks

**Project name:** `claude-code-android`. This is a factual description of what the app does — a port of Claude Code for Android — and it is used descriptively.

**Risks we manage:**

| Risk | Mitigation |
|---|---|
| The app name reads as an official product | "Unofficial" appears in the app name's subtitle context, on the About screen, in the Play listing, and at the top of the README. It cannot be missed. |
| Someone types `claude code android` expecting the official thing | The Play listing description opens with the unofficial status. |
| A fork keeps the name | Fork guidance in the README asks forks to rename, and explains why in one paragraph. |

**Do not use as the app's own mark:** the Claude wordmark, the Claude starburst, the Anthropic logo, or any lookalike of them. The app ships its own mark, specified in `03-design/brand-assets.md`, with its own source files and its own history.

**May we call our animated character something similar?** No, and this is exactly where a shortcut would create trouble. Our mark is a distinct geometric form with eyes. It is not a starburst, not a claw, and not a crab. If someone put them side by side, nobody would confuse them. That test is the standard.

## What we download, and from whom

| Artifact | Source | Verification |
|---|---|---|
| Claude Code binary | Anthropic's own CDN | Checksum against Anthropic's published list |
| glibc-runner, patchelf | Termux project release artifacts | Checksum against the Termux release's published list |
| proot-distro image | The distribution's own servers | The image's own signature, plus checksum of the download |
| All app dependencies | Maven Central / Google Maven | Gradle dependency verification with pinned checksums |
| All app assets | Drawn or generated in this project | n/a |

We download from official hosts. We do not vendor another community project's build scripts, and we do not mirror a binary that another project assembled. When we implement a documented technique, we link to the source in `15-appendix/references.md`.

**On the checksum limitation, stated honestly:** a checksum published by the same host as the binary catches corruption and truncation. It is not a code signature, and it does not prove the host has not been compromised. The app's security documentation says this plainly rather than implying a guarantee we cannot make.

## Dependencies and licences

| Dependency group | Licence | Obligation |
|---|---|---|
| AndroidX, Jetpack Compose | Apache 2.0 | Attribution in `THIRD_PARTY_NOTICES.md` |
| Kotlin, kotlinx.* | Apache 2.0 | Attribution |
| Ktor | Apache 2.0 | Attribution |
| Room | Apache 2.0 | Attribution |
| Koin | Apache 2.0 | Attribution |
| Termux glibc-runner | GPL-3.0 (Termux) | **Affected.** See below. |
| Paparazzi (test only) | Apache 2.0 | Attribution |
| Our own code | Apache 2.0 | Our `LICENSE` |
| Claude Code binary | Anthropic's terms | Downloaded at runtime by the user, not redistributed by us |

### The GPL question, and how we handle it

Termux is GPL-3.0. We do not vendor its source into our repository. We download its release artifacts at runtime, on the user's device, at the user's request, exactly as the user would from a package manager. The app's own source is Apache 2.0.

**This is a deliberate boundary, and it is a decision that could be challenged.** Two consequences we accept:

1. The repository must never contain Termux source or a derivative of it. It contains a downloader, a checksum, and a launcher.
2. If anyone determines that this distribution crosses the GPL boundary, the correct response is to switch the runtime source to a non-GLP-constrained equivalent, or to relicense. The abstraction in `06-runtime/execution-backends.md` exists partly so that this is a one-file change.

**Open:** a legal review of the runtime download path would be reassuring. **TBD — flagged for the maintainer, not a blocker for the build.**

## Privacy claims we must be able to back

The app says it has no telemetry. For that to be true, and for the claim to be in a Play listing, we must be able to show it:

| Claim | How we show it |
|---|---|
| No analytics SDK | The dependency list contains none. Enforced by a CI check against an allowlist. |
| No crash upload | The crash reporter is local; export is a user action producing a file. |
| No servers of ours | There is no domain, no backend, and no telemetry endpoint in the codebase. A CI check greps for common analytics hosts and fails on a match. |
| Keys stay on the device | Keys are Keystore-encrypted and sent only to the configured provider. A redaction test asserts they never reach a log. |
| Code stays where you put it | A project runs on the phone or on a runner you chose. Offload always names the host first. |

A Play Store data safety form must be filled in to match this. If a future dependency changes any of the above, the claim changes with it. `11-operations/privacy.md` is the source of truth and must be updated in the same commit.

## Distribution

| Channel | Requirement |
|---|---|
| GitHub (APK) | A release, the checksum, and the build instructions in the README. Anyone can rebuild and compare. |
| F-Droid | Must be buildable from source with reproducible steps, or the reason for a non-reproducible step documented. A binary-only submission needs an explanation. |
| Play Store | Developer account, the data safety form, privacy policy URL, content rating, and target API level compliance. The app is free and contains no ads, no purchases, and no in-app billing — which is also the honest reflection of a hard rule. |

## What we will refuse

A feature request that requires breaking one of the five hard rules, storing a key in plain text, or uploading a project to a server we control. These are not negotiable through configuration, support request, or update. If the app becomes less useful because of them, that is the intended outcome.
