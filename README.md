<div align="center">

# Claude Code Android

**Type one sentence. The app picks a project. Claude Code plans, writes, tests, debugs, fixes its own failures, commits, and tells you when it is done. You can put the phone in your pocket in between.**

> **Unofficial project.** Independent and not affiliated with, endorsed by, or sponsored by Anthropic. All names and marks belong to their owners. This project ships its own mark and its own assets.

[![Build](https://github.com/mertgoevse-wq/claude-code-android/actions/workflows/ci.yml/badge.svg)](https://github.com/mertgoevse-wq/claude-code-android/actions/workflows/ci.yml) · [![Release](https://img.shields.io/github/v/release/mertgoevse-wq/claude-code-android.svg)](https://github.com/mertgoevse-wq/claude-code-android/releases) · [![Licence](https://img.shields.io/github/license/mertgoevse-wq/claude-code-android.svg)](LICENSE) · [![F-Droid](https://img.shields.io/badge/F--Droid-F-Droid-green.svg)](https://f-droid.org/)

</div>

<!-- Replace with a real capture from docs/04-screens/04-chat-detail.md once a build exists.
<img src="docs/assets/chat-detail-light.png" alt="A chat with Claude Code running: a plan at the top, a tool card mid-run, the terminal below, and a cost meter in the header" width="720"> -->

## What it is

A native Android app that runs the real Claude Code engine on your phone. Give it a project, tell it what to do, watch it work, review the diff, and get a pull request. Every screen is a native Compose screen — no WebView, no embedded browser — and the engine is a real process on your device.

## What it does

| | |
|---|---|
| **Runs the agent on the phone** | Installs and verifies its own Linux runtime, then runs Claude Code locally. No Termux setup, no computer. |
| **Works while you do not** | A foreground service keeps a long run alive when the screen is off, with an honest notification showing what it is doing and what it is spending. |
| **Fixes its own failures** | Detects your build and test commands, verifies every change, diagnoses failures, and retries within a budget **you** set. |
| **Shows you everything** | Every command, diff, decision, error, and cost in an append-only log. Export it. |
| **Never does certain things** | Cannot delete, cannot spend money, cannot make anything public, cannot push to your default branch, cannot hide what it did. |
| **Brings your own key** | Anthropic, OpenAI-compatible, or any provider you can point at. Keys live in the Android Keystore. |

## Install

**F-Droid (recommended)** — a reproducible build from source, no proprietary dependencies:

<a href="https://f-droid.org/packages/dev.claudecode.android/"><img src="https://img.shields.io/f-droid/v/dev.claudecode.android.svg" alt="Get it on F-Droid"></a>

**Direct APK** — from the [releases page](https://github.com/mertgoevse-wq/claude-code-android/releases). The SHA-256 is published next to every file, and the app verifies it before installing an update. Play Protect will warn you about sideloading; that is expected, and it is the same warning any APK install produces.

Requires Android 8 or newer on an `arm64` device. The first run installs a Linux runtime — that is a few hundred megabytes and a few minutes, and it is resumable if you interrupt it.

**From a clone:**

```bash
git clone https://github.com/mertgoevse-wq/claude-code-android.git
cd claude-code-android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

You need your own API key. There is no subscription login.

## How it works

```
┌──────────────────────────────────────────────────────────┐
│  ui (Compose)        chat · projects · skills · terminal  │
│                      settings · diff · github · onboarding │
├──────────────────────────────────────────────────────────┤
│  orchestration       autonomous loop · verifier · retries │
│                      planner · permissions · recovery      │
├───────────┬──────────────┬───────────┬──────────────────┤
│  data     │  runtime     │  vcs      │  skills          │
│  Room     │  bootstrap   │  git+API  │  install/manage   │
│  Ktor     │  supervisor  │  branches │  marketplace      │
│  cost     │  PTY+term    │  auth ×2  │                  │
├───────────┴──────────────┴───────────┴──────────────────┤
│  domain  ·  core (Result, dispatchers, redaction, fs)     │
├──────────────────────────────────────────────────────────┤
│  ExecutionBackend                                        │
│    AndroidLocalBackend · SshBackend · CloudRunnerBackend  │
│    GithubActionsBackend                                   │
└──────────────────────────────────────────────────────────┘
```

One abstraction decides where the engine runs: on the phone, on a home PC over SSH, on a free cloud VM, or in GitHub Actions. The UI, the event protocol, and the permissions are identical in all four — a session record simply names the backend. That is also the iOS door: everything portable lives in `shared/`, and CI keeps that layer free of Android imports.

The hard part is the bottom of the stack. Anthropic ships Claude Code as a glibc-linked Linux `arm64` binary; Android uses Bionic and will not run it. The app solves that by installing a glibc shim, verifying the engine against its published checksum, and repointing the binary's ELF interpreter — and by shipping a full proot-based Ubuntu profile as a fallback, with automatic rollback to the last known-good version.

## Honest limitations

- **Unofficial and unaffiliated** with Anthropic. The name is used descriptively; no assets are copied, and the mark is our own.
- **The engine is a patched Linux binary.** Heavy builds (Gradle, Node, the Android SDK) are slow on a phone — use the proot profile, or offload to a remote runner.
- **Qualcomm Snapdragon devices** have no virtual-machine profile. It is experimental and optional, and nothing depends on it.
- **Bring your own key.** There is no claude.ai subscription login, by design.
- **Long runs consume battery.** A phone is not a server. The app shows elapsed time and cost as it goes, and offers remote offload for the long ones.
- **There is no iOS build.** The shared layer is prepared and CI-checked; nothing more is claimed.
- **Free-tier remote runners depend on someone else's quota.** The app checks live at setup rather than trusting a number in a document.
- **Verification commands are detected heuristically.** They are editable, and a wrong guess produces a red build rather than a false green.
- **This project is young.** There is no release yet.

## Safety

The agent is autonomous, so these five rules are enforced in the permission layer, at the agent permission layer, and in repository settings — and are covered by tests:

1. **Never deletes** anything: no file, no branch, no tag, no repository.
2. **Never spends money**: no paid API, no purchase, no subscription.
3. **Never makes anything public**: private repositories only.
4. **Never pushes to the default branch.**
5. **Never hides anything**: every command, diff, decision, error, and cost is recorded and visible.

No autonomy level can reach them. Secrets stay in the Keystore, a redaction pass scrubs key-shaped strings from anything sent to a model, and untrusted text — repository files, skill markdown, model output — is parsed as data and never becomes a command. The full threat model is in [`docs/11-operations/security-threat-model.md`](docs/11-operations/security-threat-model.md).

## Documentation

135 documents, indexed in [`docs/00-vision/README.md`](docs/00-vision/README.md) with a reading order for your role.

| Section | Count | What lives there |
|---|---|---|
| `00-vision` | 7 | What we are building, for whom, and what "finished" means |
| `01-research` | 8 | Verified external facts: runtimes on Android, APIs, auth, free hosting, legal |
| `02-architecture` | 10 | Module map, data model, event protocol, state machines, decisions |
| `03-design` | 12 | Tokens, type, colour, motion, the animated mark, the anti-slop bans |
| `04-screens` | 14 | One contract per screen: layout, states, interactions, navigation |
| `05-features` | 16 | How each feature actually behaves, including failure behaviour |
| `06-runtime` | 8 | Installing and supervising Claude Code on the phone |
| `07-integrations` | 8 | GitHub, providers, secrets, notifications, remote runners |
| `08-orchestration` | 6 | The autonomy engine: permissions, sessions, context, recovery |
| `09-testing` | 10 | What is tested, how, and to what threshold |
| `10-build` | 7 | Gradle, versions, variants, signing, static analysis |
| `11-operations` | 7 | Logging, crashes, diagnostics, backup, privacy, threat model |
| `12-delivery` | 5 | APK, F-Droid, Play Store, README, release checklist |
| `13-process` | 6 | How we work: workflow, git, review, definition of done, AI use |
| `14-build-plan` | 6 | The seven phases, every task, dependencies, risks, progress |
| `15-appendix` | 5 | References, extended glossary, troubleshooting, FAQ, changelog |

Start with [`00-vision/scope.md`](docs/00-vision/scope.md) if you want to know what it deliberately does not do, and [`15-appendix/faq.md`](docs/15-appendix/faq.md) if you have a question.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md). The short version: one logical change per PR, tests that fail without the change, the documentation updated in the same commit, and `./gradlew check` green. Agents are welcome — read [`CLAUDE.md`](CLAUDE.md) and [`docs/13-process/claude-code-instructions.md`](docs/13-process/claude-code-instructions.md) first.

```bash
./gradlew check     # the gate: lint, detekt, ktlint, and all tests
```

## Licence and notices

Licensed under the [Apache License 2.0](LICENSE). Third-party dependencies, their licences, and the terms covering the Claude Code engine — which is downloaded at runtime and is **not** redistributed here — are in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

Claude Code is a trademark of Anthropic PBC. This project is unofficial and uses the name only to describe what it does.
