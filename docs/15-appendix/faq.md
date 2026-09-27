# Frequently asked questions

Answers to the questions people actually ask. Where an answer needs a document, the document is linked rather than restated.

## What is this?

An unofficial Android app that runs the real Claude Code engine on your phone, so you can give it a project, tell it what to do, and watch it work — then review the changes and get a pull request, without being at a computer.

It is not a wrapper around a website. There is no WebView and no embedded browser. Every screen is a native Compose screen, and the engine is a real process on your device.

It is an **independent, unofficial project**. It is not affiliated with, endorsed by, or sponsored by Anthropic. See `01-research/legal-and-trademark.md`.

## Do I need a Claude subscription?

No. The app is **BYOK** — bring your own API key. There is no claude.ai / Pro / Max sign-in, and none is planned (`D3`).

Your key is stored in the Android Keystore, referenced everywhere else as an opaque `SecretRef`, and never appears in a log, a crash report, an export, or a diff.

## Which providers work?

Four kinds, configured per project:

| Kind | What it is |
|---|---|
| `ANTHROPIC` | Anthropic's Messages API |
| `OPENAI_CHAT` | OpenAI Chat Completions and compatible servers |
| `OPENAI_RESPONSES` | OpenAI's Responses shape |
| `CUSTOM` | Anything speaking either dialect — Ollama, vLLM, LM Studio, OpenRouter, Groq, a self-hosted gateway |

Each provider has a **Test connection** button that tells you the actual reason for a failure: unreachable host, rejected key, unknown model, or wrong dialect. You can define several named key profiles (personal, work, test) and switch between them. `07-integrations/providers.md`.

## What does it cost me?

Your own API usage, at your provider's rates. The app shows:

- cost per run, per project, per day, and per month
- a live cost meter during the run
- token counts: input, output, cache read, cache create

The app **does not cap spending**. You chose display over a hard limit (`D23`). There is a soft advisory threshold you configure and can dismiss — it warns, it does not stop the run. `05-features/chat-and-streaming.md`.

## Is it safe to let an agent run on my code?

That is the central question, so the answer is specific rather than reassuring.

**What the agent cannot do, at any autonomy level, with no override:**

1. Delete anything — no file, branch, tag, or repository
2. Spend money — no paid API, no purchase, no subscription
3. Make anything public — private repositories only
4. Push to the default branch
5. Hide anything — every command, diff, decision, error, and cost is recorded and visible

These are the hard blocks (`08-orchestration/permissions.md`). They are enforced in the permission layer, denied at the agent permission layer, and set in repository settings. They are covered by tests, including the "attempt to delete is refused" case.

**What you control:** the per-project autonomy level.

| Level | Behaviour |
|---|---|
| `ASK_EVERYTHING` | Every tool call waits for a tap |
| `ASK_RISKY` | Read-only and reversible actions run free; writes, network, git publish wait |
| `AUTO_WITH_CHECKPOINTS` | Everything but hard blocks runs free; the agent stops at defined checkpoints |
| `FULL_AUTO` | Nothing asks |

New projects start at `ASK_RISKY`. Hard blocks apply at every level, including `FULL_AUTO`.

**What protects you technically:** secrets stay in the Keystore, a redaction pass scrubs key-shaped strings from anything sent to a model, untrusted text (repo files, skills, model output) is data and never becomes a command, and the full threat model is in `11-operations/security-threat-model.md`.

## What does it do with my code?

It sends the context a task needs to the provider **you configured** — the same way any AI coding tool works. That is the product.

What never leaves the device: your API keys, your GitHub token, your transcripts, your logs, and any project not involved in the run.

There is no analytics, no telemetry, and no device identifier of any kind. Not "anonymous" analytics — there is no analytics code in the app. `11-operations/telemetry.md`.

## Which phones work?

Android 8 or newer, `arm64`. The engine is a `linux-arm64` Linux binary, and the app makes a glibc-linked binary runnable on Android's Bionic libc.

- **Qualcomm Snapdragon devices:** everything works except the experimental virtual-machine profile, which is unavailable upstream on Snapdragon. That profile is optional and nothing depends on it.
- **32-bit or x86-only devices:** not supported, and the app says so at first run.
- **More context:** `01-research/claude-code-runtimes-on-android.md`.

## Why is the setup slower than a normal app's first run?

Because it is installing a Linux runtime. The app downloads a glibc shim, downloads and checksum-verifies the engine, patches its ELF interpreter, and verifies it launches — with a progress screen, per-step progress, and a cancel button.

It is resumable: if the app is killed, it continues from the step it was on, not from the beginning. `06-runtime/bootstrap-state-machine.md`.

## What is the difference between the two runtime profiles?

| | Native (default) | proot Ubuntu |
|---|---|---|
| Size | ~500 MB | ~2 GB |
| Speed | Fast to start | Slower |
| Use it for | Everyday agent work: edits, review, tests, git | Heavy builds that need a full userland: Gradle, Node, Android SDK |
| Reliability | Good, with automatic rollback | Very good, no patching involved |

Both ship. The app recommends proot when a project needs a toolchain the native profile does not have, and says so before you commit. Profile B is the fallback if the ELF patch ever breaks.

## Can it work on my own server instead of my phone?

Yes. Four backends behind one interface, one UI, one event protocol:

| Backend | What it is |
|---|---|
| The phone | The default |
| A home PC over SSH | Your own machine on your network |
| Oracle Cloud Always Free VPS | The preferred free option; quotas are checked live at setup |
| GitHub Actions | Free CI minutes, with the usage shown |

Offload is opt-in per project, and the session record always names which backend ran the task. `07-integrations/remote-runners.md`.

## What happens if a run fails?

The app diagnoses, fixes, and re-verifies, up to a retry budget **you** set: 3, 10, 50, or unlimited. The counter is always visible.

When the budget runs out, it stops and writes a report naming the failing command, the last error, and what it tried. It does not loop, and it does not pretend the work succeeded. A step that repeats with no progress is detected and aborted. `05-features/retry-and-self-healing.md`.

If you interrupt, the run ends cleanly, partial work is committed to a `wip/` branch, and the state is preserved. Nothing is discarded — the app never deletes anything.

## Can I review what it did before it goes anywhere?

Yes, and you should.

- A **diff viewer** with side-by-side and unified views, syntax highlighting, and accept-or-revert per hunk.
- A **transparency log**: the append-only record of every command, its output, every decision, every error, and the cost. Exportable.
- A **commit after every successful run** on its own branch, so git itself is the record.
- A **pull request only when verification passed.** A red test suite blocks the push and tells you why.

## Does it need GitHub?

No. GitHub is how results reach a remote; a project can also be a folder on the phone. Two sign-in paths: OAuth (one tap, needs a registered app) and a personal access token (zero setup, with a scope checklist). Both are in `07-integrations/github-auth.md`.

## What are skills?

Markdown files with instructions that give the agent a capability. Install from a GitHub URL, create one with the AI, toggle it, edit it in-app, scope it globally or per project, and see exactly which projects it affects.

Before anything is written, the app shows the **file diff** of what installing a skill will do and where it goes. Broken skills are shown with a repair button rather than silently ignored. Uninstalling moves a skill to a quarantine directory — it never deletes it, because the app never deletes anything.

## Is there an iOS version?

No, and the claim is deliberately small: a shared code layer written to be portable, a stub project, and a CI check that keeps `shared/` free of Android imports. That is what is true. Nothing more is promised, and the README says the same thing. (`14-build-plan/risk-register.md` R12.)

## Is this on F-Droid / Google Play?

- **F-Droid:** yes, the recommended channel, and it builds from source with no proprietary dependencies.
- **Google Play:** the metadata, signing, and bundle are prepared. Publishing is not part of v1, and a staged rollout is the plan when it happens.

The direct APK is always available, with its SHA-256 published next to it. `12-delivery/apk-distribution.md`.

## Does it send telemetry?

No. No analytics SDK, no usage events, no crash reporting unless you explicitly opt in, no remote configuration, no device identifier, no A/B assignment, no session recording. There is no code path that could, and CI checks that no analytics library is added.

Three counters are kept **on the device** and shown to you: runs, success rate, and token and cost totals. They are a feature, not telemetry, and you can reset them with one tap. `11-operations/telemetry.md`.

## Can I trust what it did not do?

Yes, and the mechanism is the interesting part. Every tool call is a value that goes through a permission policy; only a permitted one becomes execution. The hard blocks sit below the autonomy level, so no setting can reach them. Untrusted text — repo files, skill markdown, model output — is parsed as data and never concatenated into a command. Adding a tool without a policy case fails a test, not a review.

`11-operations/security-threat-model.md` has the full table, and every row names where it is verified.

## Why does it take over my screen with a notification?

A long run must survive Android killing backgrounded processes, which it does by running as a foreground service with a persistent notification. That notification is also the honest progress indicator: what is running, how long, and what it is spending.

The wake lock is held only while a run is active, and nothing else keeps the CPU awake. `05-features/background-execution.md`.

## Will it drain my battery?

Yes, and the app will not pretend otherwise. An agent running builds on a phone is a real workload; a run can be hot and can take an hour. The foreground notification shows elapsed time and cost as it goes, remote offload is offered for the long ones, and the README says this plainly.

This is R7 in the risk register, and it is a real limitation rather than something to engineer around.

## Something is broken. What do I send you?

The diagnostics bundle: Settings → About → Export diagnostics. It contains the logs, the environment, and your configuration with secrets stripped. With that plus the run ID, a problem that would otherwise take a week to describe takes an hour.

`15-appendix/troubleshooting.md` covers the common failures, and each entry says what is actually happening rather than only what to try.

## Still unclear?

The documentation is indexed in `docs/00-vision/README.md`, with a reading order for your role. The two most useful starting points: `00-vision/scope.md` for what the app deliberately does not do, and `11-operations/security-threat-model.md` for what it will and will not let the agent do.
