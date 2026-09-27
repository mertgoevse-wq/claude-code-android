# Glossary

Every term this project uses, in plain language first and precise meaning second. Read this before reading anything else in `docs/`.

## The engine

**Claude Code** — Anthropic's coding agent. A program that reads files, writes code, runs commands, and fixes errors, driven by a conversation. It normally runs in a terminal on a computer. This app runs the same program on a phone.

**Agent loop** — The repeating cycle Claude Code runs: think, call a tool, read the result, think again, until it decides it is finished.

**Tool call** — One action Claude Code performs, such as reading a file or running a command. In this app it appears as a card in the chat.

**Tool result** — What came back from a tool call. Sometimes success, sometimes an error, sometimes thousands of lines of output.

**Permission prompt** — Claude Code normally stops and asks a human before doing something sensitive. This app replaces that dialog with its own sheet, and at higher autonomy levels skips it.

**Permission mode** — The setting that decides which of those prompts appear. `default` asks, `acceptEdits` auto-approves file changes, `plan` allows no changes at all.

**Headless / print mode** — Running Claude Code without a terminal UI, driven by a program. `claude -p "..."` is the simplest example. This is how the app runs it.

**stream-json** — The output format where Claude Code emits one JSON object per line as it works, so a program can show progress live instead of waiting for the end.

**Subagent** — A second Claude Code instance started by the first one to do a scoped job. Its messages are tagged so a UI can nest them.

**Context window** — How much text the model can hold in mind at once. Long runs fill it up, and the app manages what stays.

**MCP** — Model Context Protocol. A standard way to give Claude Code extra tools from an external program. Optional in this app.

**CLAUDE.md** — A file in a project that tells Claude Code how to work on that project. The app reads it and shows it, and it can be edited in-app.

**Skill** — A reusable instruction bundle, in a folder with a `SKILL.md`, that teaches Claude Code a procedure. Installed per project or globally.

**Plugin** — A packaged bundle of skills, commands, and configuration that can be installed as a unit.

**Slash command** — A shortcut you type, like `/compact`, that expands to something longer.

**Hook** — A script that runs automatically at a defined moment, for example after every tool call.

## The phone side

**Bionic** — The C library Android ships. Linux programs usually expect glibc instead, and do not run on Bionic without help.

**glibc** — The C library most Linux programs are built against. The reason Claude Code's binary will not just run on Android.

**glibc-runner** — Termux's shim that makes glibc-linked Linux binaries run on Android. This app installs it as part of its own runtime, without Termux.

**ELF interpreter** — The loader a Linux binary asks for at startup, recorded inside the binary. Claude Code's binary points at glibc's loader; the installer rewrites that pointer so Android's shim handles it.

**Checksum** — A fingerprint of a file. If the fingerprint matches, the file arrived intact. The app refuses to install a runtime whose checksum does not match Anthropic's published list.

**proot** — A tool that pretends to be a full Linux system using `ptrace` instead of a real virtual machine. Cheap, no root, slower, and some system calls behave differently.

**proot-distro** — The Termux package that installs and manages a whole Linux distribution with proot.

**AVF** — Android's built-in Virtualization Framework. On a few devices it runs a real Linux virtual machine with no root and no Termux. Currently Pixel 6 and later on Android 16, and not on Snapdragon.

**aarch64 / arm64** — The 64-bit ARM architecture nearly every modern Android phone uses. Claude Code ships an `arm64` Linux binary. A 32-bit phone cannot run it.

**PTY** — Pseudo-terminal. The kernel object that makes a program believe it is talking to a terminal, which is why colours, cursor movement, and line editing work. Required for a believable terminal.

**ANSI escape codes** — The byte sequences a program writes to a terminal to set colours, move the cursor, and so on. A terminal that does not understand them shows raw garbage.

**Foreground service** — An Android service with a permanent notification, which the system will not kill. The only reliable way to run a long job on a phone.

**Keystore** — The Android system service that holds cryptographic keys in secure hardware where possible. Secrets are encrypted with a key from here, so they are unreadable even from a device backup.

## The project side

**Project** — A folder of code the app works on, plus its settings.

**Local project** — A folder on the phone.

**Remote project** — A folder on a server, worked on through a remote runner.

**Repository / repo** — A folder tracked by git, usually hosted on GitHub.

**Clone** — Copying a repository from a server onto a device.

**Commit** — A saved snapshot of the repository, with a message and a parent.

**Branch** — A line of commits that can diverge. Work happens on a branch, never on the default one.

**Default branch** — The branch a repository opens on: `main` or `master`. The app never pushes to it.

**Pull request (PR)** — A request on GitHub to merge one branch into another, where the changes can be reviewed and discussed.

**Diff** — The line-by-line difference between two versions of a file. Green lines added, red lines removed.

**Hunk** — One contiguous block of changed lines inside a diff. This app can accept or revert a single hunk.

**Staging area / index** — The list of changes queued for the next commit.

**Worktree** — A second working directory for the same repository, on a different branch. Useful for running two tasks in parallel.

**Repository privacy** — Whether a repository is private or public. This app only ever works with private ones.

**Upstream** — The original repository a fork came from.

## The work

**Task** — One thing the user asked for. One task produces one branch and at most one PR.

**Run** — One execution of Claude Code against a project, triggered by a task.

**Plan** — The ordered list of steps Claude proposes before working. Visible and editable by the user.

**Checkpoint** — A point where the run must stop and report, even at high autonomy, for example before a push.

**Verification** — Running the project's own build, typecheck, lint, and tests to find out whether the work actually succeeded. Separate from the agent's claim.

**Judge** — The step that decides pass or fail from the verification output, rather than from what the agent said.

**Retry budget** — How many times the app may try to fix a failure before it stops and asks.

**Self-healing** — Diagnose a failure, fix it, re-verify, repeat, within the budget.

**Anti-loop detection** — Aborting a step that repeats with no progress, so a run cannot burn a budget spinning.

**Handoff** — Moving a run to a remote runner because the phone cannot do it.

**Checkpoint restore** — Returning to the state before an interrupted run, without deleting anything.

## Providers and money

**Provider** — The service that answers model requests: Anthropic, OpenAI, OpenRouter, a self-hosted llama.cpp, anything.

**BYOK** — Bring Your Own Key. You supply the key; the app stores it and does not resell or proxy it.

**API key** — A long secret string that authenticates you with a provider.

**Anthropic-compatible** — An API that speaks the Anthropic message format. Includes Anthropic itself plus many gateways and self-hosted proxies.

**OpenAI-compatible** — An API that speaks the OpenAI chat format. The de facto standard, supported by most providers and local runtimes.

**Base URL** — The address of a provider's API. Changing it is how the same app talks to a different service.

**Custom provider** — A user-defined entry: a name, a URL, a key, a format, and a model list.

**Key profile** — A named key, so private, work, and test keys can coexist and be switched.

**Token** — A unit of text the model reads or writes. Priced per million tokens, and the basis of the cost meter.

**Cost** — What a run consumed, estimated from tokens and shown in USD. Displayed, never capped.

**Rate limit** — A provider's cap on requests per minute. The app backs off and shows it rather than failing silently.

## Version control hosting

**GitHub** — Where repositories are hosted.

**OAuth** — A sign-in flow where GitHub gives the app permission without the user sharing a password.

**Personal access token (PAT)** — A long string the user creates on GitHub and pastes into the app. Fine-grained tokens can be limited to specific repositories and permissions.

**Fine-grained token** — The modern, narrowly scoped kind of PAT. Preferred by this app.

**Scope** — What a token is allowed to do. The app requests the minimum and shows the list.

**Webhook** — A callback GitHub sends when something happens in a repository.

**CI / GitHub Actions** — GitHub's automation. Free minutes included; usable as a remote runner for short jobs.

**Runner** — A machine that executes jobs. Here: the phone, a server, a home PC, or a GitHub Actions runner.

**Oracle Cloud Always Free** — A permanently free virtual machine tier, generous enough to run Claude Code. The preferred remote runner.

**Always free / free tier** — Cloud capacity included at no cost, subject to quota and changeable terms. Verified before relying on it.
