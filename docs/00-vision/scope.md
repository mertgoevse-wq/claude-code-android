# Scope

## In scope for v1

### Engine

- Running the real Claude Code on the device, in an app-managed Linux runtime, installed in one tap with checksum verification.
- A second, optional profile (proot Ubuntu) for work that needs a full Linux userland — Gradle builds, Node, native toolchains.
- A third, experimental profile (Android's built-in VM) offered only on hardware that supports it.
- Headless, non-interactive runs driven programmatically, with streamed structured output.
- Process supervision: the whole process tree can be cancelled, and a run survives screen-off.

### Experience

- A chat-first interface with live streaming, collapsible tool cards, a visible plan, a cost meter, and interruption.
- A real terminal, embedded, with PTY, ANSI colour, and a custom keyboard row.
- A diff viewer with per-hunk accept and revert.
- Per-project autonomy: four levels, from ask-everything to full-auto.
- Self-verification: the app runs the project's own build, typecheck, lint, and tests, and judges success itself rather than trusting the agent's word.

### Projects and version control

- Local folders, cloned repositories, and folders on a remote runner.
- Private GitHub repositories, authenticated by OAuth or by a fine-grained personal access token.
- Automatic branch per task, automatic commit after a green run, automatic pull request, tests required before any push.
- Never delete, never publish, never push to the default branch, never spend money.

### Providers

- Anthropic-compatible APIs, OpenAI-compatible APIs, and user-defined providers of any kind.
- Bring your own key, stored in the platform keystore, never logged, never sent anywhere except the provider.
- Multiple named key profiles per provider, switchable.
- A connection test per provider that says what is actually wrong.
- Per-run and per-project cost accounting in USD.

### Skills and plugins

- Install from a GitHub URL, with a preview of exactly which files will be written.
- Create a new skill conversationally, with validation and preview before saving.
- List, search, enable, disable, edit, duplicate, update, and uninstall.
- A marketplace: a static index shipped in the app, plus an optional remote index.
- Global scope and per-project scope, with the location always visible.

### Delivery

- A debug APK for daily use, a signed release APK, an app bundle for Play, and F-Droid metadata.
- CI that builds, tests, lints, and diffs screenshots on every change.

## Out of scope for v1

| Excluded | Why | Revisit |
|---|---|---|
| A shipping iOS binary | The shared layer is built for it; the Xcode app is a stub | When a Mac is part of the loop |
| Publishing to the Play Store | Metadata and signing are prepared; the review process is not ours to run | v1.1 |
| A hosted marketplace backend | A static index in the app covers the feature without a server we must fund | v1.1 |
| Voice input | Dictation exists in the OS; a second recogniser is scope we do not need | v1.2 |
| Model training, fine-tuning, or local model weights | Different product | Never, for this app |
| A general-purpose file manager | We open files; we do not manage the device | Never |
| Terminal sharing, port forwarding, or an SSH server | A bridge app, not a coding agent | Never |
| Multiple simultaneous agents on one project | Git worktrees make it possible, but the value is unclear and the conflict handling is real work | v1.2, behind an experiment flag |
| Reading and writing secrets out of a remote machine's environment | The blast radius is not acceptable | Never |

## Boundary cases, decided

**"Improve this existing app" on a repo that has no tests.** In scope. The app's first job on such a repo is to establish a verification command set, and to tell the user it is doing so rather than pretending the work is verified.

**A project that is a git repo whose default branch is called `master`.** In scope. The default-branch rule is name-agnostic.

**A user wants to run something that costs money.** Out of scope, permanently. The hard block is not configurable and there is no path to enable it.

**A user pastes an API key into the chat.** In scope, and dangerous. A redaction pass scrubs it before it reaches a model, the app tells the user it did, and the message is flagged in the transparency log.

**A run needs a tool that is not on the device** (a database, a browser, a specific SDK). In scope, via remote offload. The app offers to move the run to a runner rather than failing.

**A skill contains code that runs commands.** In scope, because that is what skills are. The install preview shows the full content, the action is privileged and confirmed, and the skill is attributed in the log.

## The hard boundary

The app runs the real Claude Code and drives it programmatically. It does not embed, reimplement, or fork the model or the agent loop, and it does not proxy a claude.ai subscription. It brings its own key and talks to a provider API. This boundary is a legal decision as much as a technical one; see `01-research/legal-and-trademark.md`.
