# SPEC — claude-code-android

**A full-featured Android port of Claude Code, driven from a polished, native, chat-first mobile UI.**

| Field | Value |
|---|---|
| Spec version | 1.0.0 |
| Date | 2026-09-27 |
| Status | Ready for autonomous build |
| Project / repo name | `claude-code-android` |
| Working directory | `/home/mert/claude-code-android` (currently empty, not a git repo) |
| Primary platform | Android (native Kotlin) |
| Secondary platform | iOS (prepared for later, via shared code layer) |
| App display name (working title) | `Claude Code Android` |
| Package id | `dev.claudecode.android` |
| Document language | English (code, identifiers, all `.md` files) |
| UI languages | German (default) + English, switchable in settings |
| Builder | Claude Code, one-shot, autonomous, no manual steps |

---

## 0. How to read this document

This is the single source of truth for an autonomous build. It was produced from a user interview.

- **Section 1** states *what* is being built, in product terms.
- **Section 2** records every decision made during the interview, and *why*.
- **Sections 3–17** are the technical contract: stack, architecture, runtime, UI design system, features, integrations, autonomy engine, security, testing, delivery.
- **Section 18** is the **135-file markdown documentation manifest** (well beyond the 80+ you asked for) — the files that must exist in `docs/` before/while code is written, and which make the whole build reproducible and self-describing.
- **Section 19** is the **source file manifest** — the ~150 project files that must be generated, with one-line purpose each. No file may be invented outside this list without updating this spec.
- **Section 20** is the **Claude Code autonomy kit** — `CLAUDE.md`, skills, subagents, slash commands, hooks, MCP config. This is what removes all human intervention from the build.
- **Section 21** is the **phased one-shot build plan** with per-phase acceptance criteria.
- **Section 22** lists the user's hard rules (non-negotiables) and quality gates.
- **Section 23** lists assumptions and open risks.

If a rule here conflicts with a convenience, the rule wins. If something is genuinely unknown, the build must **research it and write the finding into the relevant doc** — it must not guess silently.

---

## 1. Product definition

### 1.1 What this is

A native Android application that is a **port of Claude Code**: it runs the real Claude Code engine, gives it projects to work on, lets the user talk to it in a chat, watches it work, reviews and approves its changes, and keeps the results in GitHub — all from a phone.

It is **not** a wrapper around a website. There is no WebView, no embedded browser, no HTML UI. Every screen is a native Compose screen.

### 1.2 The core promise

> Type one sentence. The app picks a project. Claude Code plans, writes, tests, debugs, fixes its own failures, commits, and tells you when it is done. You can put the phone in your pocket in between.

### 1.3 The four primary flows

1. **Project + order** — open app, tap project, type the order, everything runs.
2. **Build a whole app from an idea** — describe an app, Claude scaffolds, builds, tests, and creates a private GitHub repo.
3. **Take over existing code** — pick a GitHub repo, ask for repairs/improvements, review the diff, accept.
4. **Accept the finished thing** — notification when done, notification when stuck, one tap to review.

### 1.4 Non-goals (v1)

- Shipping to the public App Store review in v1 (Play Store metadata and signing are prepared, publishing is a later phase).
- Native iOS binary (only the shared code layer and stubs).
- Training, fine-tuning, or voice input.
- Any marketplace that requires a running server in v1 (the skill marketplace reads a static JSON index; a hosted index is an optional later step).

---

## 2. Interview decision log

Every row is a locked decision. Changing one requires an explicit spec amendment.

| # | Area | Decision |
|---|---|---|
| D1 | Where the engine runs | **On the phone first**, on a PC/server later. Both must be supported through one abstraction. |
| D2 | Free hosting | Use free tiers where possible. Oracle Cloud Always Free VPS is the primary remote runner; GitHub Actions is the secondary; a home PC over the network is the third. |
| D3 | Authentication | **BYOK only** — bring your own key. No claude.ai / Pro / Max subscription login. |
| D4 | Provider support | Anthropic-compatible **and** OpenAI-compatible APIs, plus a user-managed list of custom providers of any kind. Test connection on demand. Multiple named keys (personal/work/test), switchable. |
| D5 | Stack | Native Android with Kotlin first; code is split from day one so it can be ported to iOS later. |
| D6 | Distribution | Signed APK by hand, F-Droid, and Play Store (all three prepared). |
| D7 | Privacy | Local-first. The app must be fully usable with no third-party server. |
| D8 | Local environment | The app sets up its **own** Linux/Claude runtime in one tap. No Termux dependency for the default path. Optional full Ubuntu (proot) path offered as a second profile. |
| D9 | Autonomy | **Per-project policy**, user-adjustable: 4 levels from "ask for everything" to "everything allowed". |
| D10 | GitHub auth | Both OAuth (one-tap sign-in) and personal access token entry. |
| D11 | Background work | Work continues in the background (foreground service + notifications). Long tasks may be offloaded to a remote runner automatically. |
| D12 | Skills | Install from GitHub URL, create with AI, list/toggle/edit/delete, browse a marketplace, and scope skills per-project or globally. |
| D13 | Visual identity | **Very close to 1:1 the real Claude app** (cream background, orange starburst, rounded cards, calm typography) — plus a dark theme. |
| D14 | Logo animation | Both: calm/breathing when idle, lively/character-driven when working. |
| D15 | Navigation | **Bottom tab bar** (Chat, Projects, Skills, Terminal, Settings) with a collapsible history drawer. |
| D16 | Activity display | All five: collapsible cards, live streaming, a task plan, a side-by-side terminal, and diffs. |
| D17 | Git behaviour | Auto-save (commit) after each run; branch + PR per task; **tests must pass before any push**; **private repos only**; **never delete**. |
| D18 | Self-healing | Retry count is user-configurable (3 / 10 / 50 / unlimited). |
| D19 | App language | German default, English switchable, architecture allows downloaded additional languages. |
| D20 | iOS readiness | Shared logic layer from day one (KMP), not retrofitted. |
| D21 | Testing | **Exhaustive**: unit, integration, UI, screenshot tests, plus static analysis and coverage gates. |
| D22 | Security | Keys in the Android Keystore; app lock via biometrics/PIN. |
| D23 | Cost | Track and display spend per run; **no hard spending cap** (user chose "show only"). |
| D24 | Notifications | On completion, on stuck/failure, and on GitHub state changes (PR comments, checks, reviews). |
| D25 | Hard rules | **Never delete anything. Never spend money. Never hide anything — full transparent log.** Never make anything public. |
| D26 | Build style | **One-shot, staged** (7 phases), fully autonomous, using the design skills (`/design`, `impeccable`, and friends). |
| D27 | Anti-slop | The GitHub README must be beautified; the whole project must avoid generic AI-generated design. |
| D28 | Self-improvement | The app can update itself: fetch new versions, rebuild itself, auto-fix bugs found in the field. |
| D29 | Deliverable of this phase | This spec must lead to **80+ `.md` files** that let Claude Code build the entire project autonomously. |

---

## 3. Technical stack

### 3.1 Languages and frameworks

| Layer | Choice | Notes |
|---|---|---|
| UI | **Kotlin + Jetpack Compose** (via Compose Multiplatform where shared) | Android first; CMP keeps the iOS door open. |
| Language | Kotlin 2.x | `explicitApi()` on, strict compiler args on. |
| Architecture | MVVM + unidirectional data flow, `kotlinx.coroutines` Flows | No Redux-style ceremony; StateFlow + sealed UI state. |
| DI | Koin (multiplatform) | Simple, no codegen, works in common code. |
| Networking | Ktor client (OkHttp/CIO) | SSE + streaming JSON parsing for agent output. |
| Storage | Room (KMP driver) for structured data, DataStore for settings | Room for projects/sessions/tool-calls; DataStore for preferences. |
| JSON | kotlinx.serialization | Shared between app and Claude Code bridge. |
| Terminal UI | Custom Compose terminal emulator on top of a PTY bridge | Bundled; see §7. |
| Build | Gradle KTS + version catalog + convention plugins | 3rd party deps must be justified in the relevant doc. |
| Static analysis | ktlint, detekt, kotlinx-lint, Kover (coverage) | Wired into `check`. |
| Testing | kotlin-test, Turbine, MockK, Robolectric, Compose UI test, Paparazzi (screenshot) | See §15. |
| Docs | Markdown only, no wiki tool | All 80+ files in `docs/`. |

**Version rule:** Claude Code, its Agent SDK, and Android/Gradle plugin versions move fast. The build must check the current stable version of each at build time, pin it in `gradle/libs.versions.toml`, and record the choice in `docs/02-architecture/dependency-versions.md`. **Never invent a version number.**

### 3.2 Repository layout

```
claude-code-android/
├── CLAUDE.md
├── README.md
├── LICENSE
├── .editorconfig
├── .gitignore
├── .gitattributes
├── .claude/                  # skills, agents, commands, settings, hooks  (§20)
├── docs/                     # the 80+ markdown files                   (§18)
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── shared/                   # KMP: everything portable
│   ├── core/                 # Result, dispatchers, platform gateways
│   ├── domain/               # models + use cases, zero framework deps
│   ├── data/                 # Room, repositories, provider clients
│   ├── runtime/              # Linux bootstrap, process supervision, PTY
│   ├── orchestration/        # autonomous loop, verifier, retry budget
│   ├── skills/               # skill install/manage subsystem
│   ├── vcs/                  # git + GitHub subsystem
│   └── ui/                   # Compose Multiplatform design system + features
├── androidApp/               # Android-only: services, keystore, notifications
├── iosApp/                   # Xcode project stub + Swift entry point (scaffold only)
├── tools/                    # shell/python helpers, manifest verifiers
└── fastlane/                 # metadata, screenshots, release notes
```

Rule: `shared/domain` and `shared/core` must have **no** Android imports. A CI check enforces this.

---

## 4. Architecture overview

```
┌──────────────────────────────────────────────────────────────┐
│  ui (Compose)          chat · projects · skills · terminal   │
│                        settings · diff · github · onboarding  │
├──────────────────────────────────────────────────────────────┤
│  presentation state      ViewModels, UiState sealed classes    │
├──────────────────────────────────────────────────────────────┤
│  domain                 use cases, policies, budget, plans     │
├──────────────────────────────────────────────────────────────┤
│  orchestration          autonomous loop · verifier · retries   │
│                        planner · self-improvement              │
├───────────────┬──────────────┬───────────────┬───────────────┤
│  data         │  runtime     │  vcs          │  skills       │
│  Room repos   │  bootstrap   │  git + API    │  installer    │
│  Ktor clients │  supervisor  │  branches/PR  │  generator    │
│  cost meter   │  PTY + term  │  auth (2 ways)│  marketplace  │
├───────────────┴──────────────┴───────────────┴───────────────┤
│  core         Result · dispatchers · crypto · fs · platform   │
├──────────────────────────────────────────────────────────────┤
│  ExecutionBackend (interface)                                │
│    ├── AndroidLocalBackend   (phone: own Linux + Claude Code) │
│    ├── SshBackend            (home PC)                        │
│    ├── CloudRunnerBackend    (Oracle Always Free VPS)         │
│    └── GithubActionsBackend  (free CI minutes)                │
└──────────────────────────────────────────────────────────────┘
```

### 4.1 The central abstraction: `ExecutionBackend`

Everything above it is platform-agnostic. The only thing that changes between "runs on the phone" and "runs on my server" is which implementation is bound.

```kotlin
interface ExecutionBackend {
    val id: BackendId
    val capabilities: BackendCapabilities   // supportsPty, supportsLongRunning, maxMemoryMb, ...
    suspend fun prepare(): PrepareResult
    suspend fun stream(request: AgentRequest): Flow<AgentEvent>
    suspend fun send(message: OutboundMessage)                 // interrupt, permission answer
    suspend fun state(): BackendState
    fun events(): Flow<AgentEvent>
}
```

`AgentEvent` is a sealed hierarchy covering: session start, text delta, thinking delta, tool start, tool progress, tool result, permission request, plan update, cost update, error, done, retry.

This is the single most important interface in the codebase. §18 doc `06-runtime/execution-backends.md` defines it in full.

---

## 5. The local runtime (hardest part, most important)

### 5.1 The problem

Anthropic ships Claude Code as a **glibc-linked Linux `linux-arm64` binary**. Android uses **Bionic** and will not run it out of the box. There is **no official Android build**. (Upstream issue: anthropics/claude-code#50270.)

### 5.2 Two runtime profiles (both shipped)

**Profile A — Native (default).**
1. Download Termux's `glibc-runner` and `patchelf-glibc` packages.
2. Download the official `linux-arm64` claude binary from Anthropic's CDN.
3. Verify it against Anthropic's published checksum list. Checksum mismatch → abort, tell the user, never proceed.
4. Patch the ELF interpreter to point at the glibc-runner.
5. Install a wrapper script in the app's private `bin/` that checks for a new version once per day and updates transparently.
6. Optional DNS fallback to public resolvers, **opt-in only**, with a visible warning in the security screen (an unconditional override would break VPNs and Pi-hole setups).

**Profile B — proot Ubuntu (optional, user-selected).**
1. `proot-distro` + Ubuntu image, ~2 GB.
2. Anthropic's official installer inside it, so `process.platform == "linux"` and all Linux tooling behaves normally.
3. Preferred for heavy builds (Gradle, Android SDK, Node) because Profile A lacks a full userland.

**Profile C — AVF Linux VM (experimental, Pixel 6+/Android 16+).**
Detect support, present it, do not depend on it. Qualcomm Snapdragon is unsupported upstream.

### 5.3 Rules for the runtime layer

- All three profiles are described by one `RuntimeProfile` data class; feature code never branches on profile.
- The bootstrap is a **state machine** with observable state, so the UI can show a progress screen with per-step progress and a cancel button.
- Every step is idempotent and resumable. A killed app must resume, not restart.
- The app owns its own storage (`filesDir/cca/`), never `/sdcard`, never a shared location. Uninstalling removes everything.
- Health checks run before every session: binary present, checksum ok, glibc ok, network ok, provider key ok. Fail fast with a specific, human-readable fix.

### 5.4 Process supervision

- Long-running processes use a **foreground service** with an ongoing notification, so Android does not kill them.
- The process tree is tracked: parent PID, children, and their PIDs, so a cancel can reliably kill the whole tree (Bash children too).
- A ring buffer (last N KB) of raw stdout/stderr is kept per session and is always visible in the terminal pane — nothing is hidden (D25).
- Screen off, app backgrounded, and battery saver must not kill the run. Wake lock only while a run is active, and only then.

---

## 6. Providers, keys, and cost

### 6.1 Provider model

```kotlin
data class ProviderConfig(
    val id: ProviderId,
    val name: String,                 // "Anthropic", "My Ollama", "OpenRouter"
    val kind: ProviderKind,           // ANTHROPIC | OPENAI_CHAT | OPENAI_RESPONSES | CUSTOM
    val baseUrl: String,
    val keyRef: SecretRef,            // never the key itself
    val models: List<ModelSpec>,
    val headers: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
)
```

- Anthropic-kind: `/v1/messages`.
- OpenAI-kind: `/v1/chat/completions`, plus `/v1/responses` for the Responses shape.
- CUSTOM: user picks kind + path template, so anything that speaks either dialect works (llama.cpp, vLLM, LM Studio, OpenRouter, Groq, Together, Bedrock/Vertex via their Anthropic-compatible gateways, a self-hosted gateway).
- A **Test connection** button per provider: validates URL reachability, key, and model list, and shows a clear pass/fail with the actual reason.

### 6.2 Secrets

- Keys are stored via the **Android Keystore** (`EncryptedSharedPreferences` / direct Keystore AES-GCM with a key held in hardware where available).
- Secrets are referenced by `SecretRef` everywhere else. No key ever appears in a log, a crash report, a screenshot, a diff, or an exported bundle.
- Multiple named key profiles per provider, switchable (personal / work / test).
- A "reveal key" action requires biometric re-auth and auto-hides after 15 seconds.
- The whole secrets module is behind an interface so a future iOS Keychain implementation is a single `actual`.

### 6.3 Cost tracking

- Every run accumulates: input tokens, output tokens, cache read, cache creation, and USD.
- Per-run, per-project, per-day, and per-month totals are shown.
- Claude Code's JSON result payload provides `total_cost_usd`; the app must also estimate when only deltas are available.
- Cost is displayed but **never hard-capped** (D23). A soft warning appears at a user-configurable advisory threshold, which the user can dismiss.

---

## 7. Terminal

The app is chat-first, but a real terminal must exist and must be excellent (D16).

- A PTY bridge: allocate a pseudo-terminal, run the shell, feed stdin, read stdout/stderr, handle resize (COLUMNS/LINES), and propagate exit codes.
- A custom Compose terminal renderer: monospace, ANSI colour support (16/256/truecolor), cursor control, selection, copy/paste, and a custom keyboard row with `Ctrl`, `Alt`, `Tab`, `Esc`, arrows, and a `Ctrl+C` that is always reachable.
- Sessions persist; you can leave and come back.
- The terminal is a **peer** to the chat, not a buried feature: an optional split view (chat above, terminal below) and a floating terminal sheet over the chat.
- Manual commands typed in the terminal are part of the same session log, so the record is complete and honest (D25).

---

## 8. Projects, Git, and GitHub

### 8.1 Project model

A project is either **local** (a folder on the phone), **cloned** (from a GitHub URL), or **remote** (a folder on a server). All three share the same feature surface.

### 8.2 Git policy (from D17, D25)

| Rule | Enforcement |
|---|---|
| Commit after every successful run | Automatic, with a generated message that names the task |
| New branch per task | Automatic: `task/<slug>-<short-id>` |
| Open a PR when the task completes | Automatic, only if tests are green |
| **Never push to the default branch** | Hard block, no override |
| **Never make a repo public** | Hard block, no override |
| **Never delete** a branch, tag, file, or repo | Hard block, no override. `git push --delete` and `gh repo delete` are filtered out at the tool-permission layer |
| **Never spend money** | Hard block: no paid APIs, no subscriptions, no purchases. Enforced in the permission policy and the settings screen |
| Full transparency | Every command, diff, and result is recorded in the session log and exportable |
| Tests before push | `verifyProject()` must pass; a red suite blocks the push and reports why |

### 8.3 Auth

- **OAuth**: GitHub App or OAuth App, device-flow or web callback, minimum scopes: `repo` (private), `read:user`, `notifications`. Tokens stored in the Keystore.
- **PAT**: fine-grained token entry with a scope checklist and a validation button.
- **Both**, per the user's D10. OAuth is the default and recommended path in the UI.
- Webhook or polling for D24 GitHub notifications (PR comments, CI checks, reviews). Polling fallback with a configurable interval.

---

## 9. Skills and plugins

A first-class subsystem, not a hack (D12).

| Capability | Detail |
|---|---|
| Install from GitHub | Paste a repo URL → detect `SKILL.md` / `.claude/skills/*` / plugin manifest → preview → install into the project or globally |
| Create with AI | "Create a skill" → guided conversation → Claude writes a valid skill (frontmatter, name, description, instructions) → preview → save |
| Manage | List, search, toggle (enable/disable), edit in-app, duplicate, update, uninstall, show which projects use it |
| Marketplace | Static JSON index shipped in-app + optional remote index URL; browse, search, one-tap install, ratings/summary from the index |
| Scoping | Global (`~/.claude/skills`) or per-project (`.claude/skills`); the UI always shows where a skill lives and which projects it affects |
| Validation | Every installed skill is parsed and validated (frontmatter present, name format, description length). Broken skills are shown with a repair button, never silently ignored |
| Security | Installing a skill is a privileged action: the UI shows exactly which files will be written and what they contain before confirming |

---

## 10. UI / design system

### 10.1 Visual identity (D13)

**Reference target: the real Claude app.** Cream/bone background, the orange starburst mark, generous rounded corners, quiet confident typography, high whitespace, restrained motion. A dark theme ships alongside and follows the system setting by default.

Concrete tokens are specified in `docs/03-design/design-tokens.md`, which must be written before any UI code and treated as binding. Minimum required token set:

- **Colours**: background surface ramp, card surface, border, text primary/secondary/tertiary, accent (Claude orange), success, warning, danger, info, code syntax theme, diff green/red, terminal ANSI 16-colour palette.
- **Typography**: display/headline/title/body/caption/mono scale, weights, line heights, letter spacing. No default Roboto everywhere; choose and document a deliberate pairing.
- **Spacing**: 4pt base scale, standard paddings, section gaps.
- **Radii**, **elevation**, **motion durations and curves**, **haptics**.

### 10.2 Logo animation (D14)

One animated mark, top-left, with two modes:

- **Idle**: slow breathing pulse (2.4–3.2 s), subtle scale 1.0 → 1.04, low amplitude, no distraction. Optionally a slow blink.
- **Working**: the mark becomes a character — eyes appear, it looks toward the direction of the current tool call, blinks, glances up while thinking, and settles when a step completes. Motion driven by a state machine fed by real `AgentEvent`s (thinking / reading / writing / running / waiting-for-you / error), not by a random timer.

Implementation: Compose `Canvas` + `Animatable`, single animation clock, respects "reduce motion" accessibility setting (falls back to a static mark plus a progress ring).

### 10.3 Navigation (D15)

Bottom tab bar: **Chat · Projects · Skills · Terminal · Settings**.
Plus: a collapsible history drawer (chat history, grouped by day/project), and a prominent "New chat" action.

### 10.4 Screens (all must be specified in `docs/03-design/screen-specs/`)

1. **Onboarding** — welcome, runtime setup, key entry, GitHub connect, biometric lock, first-run explanation of autonomy levels.
2. **Chat list** — recent conversations, grouped, search.
3. **New chat** — greeting screen with the animated mark, project selector, and the composer.
4. **Chat detail** — the heart: streaming messages, collapsible tool cards, live plan, cost meter, composer with attach/@file/image, mode chips (autonomy level, model, provider), stop/interrupt button.
5. **Chat with terminal** — split view.
6. **Diff viewer** — side-by-side and unified, syntax highlighted, accept/revert per hunk.
7. **Project list / detail** — repos, branches, last run, open PRs, run history.
8. **Add project** — clone URL, local folder, remote target; repo privacy indicator; default autonomy level.
9. **Skills browser / detail / editor / creator**.
10. **Terminal**.
11. **Settings** — providers & keys, autonomy, notifications, language, appearance, security, remote runners, storage, about, self-update.
12. **Remote runner setup** — Oracle Cloud, GitHub Actions, home PC, with guided setup and a connection test.
13. **Verification / Test report** — what was tested, results, pass/fail.
14. **Session log / Transparency view** — the full immutable record, exportable.

### 10.5 Anti-slop rules (D27) — binding

The build must use the available design skills (`/design`, `impeccable`, `craft`, `typeset`, `bolder`, `quieter`, `adapt`, `animate`, `polish`, `critique`, `design-review`, `mobile-android-design`, `vercel-react-*` where applicable, `writing-guidelines`, `web-design-guidelines`) and must enforce:

- No default Material purple, no default Roboto, no stock gradient hero, no generic card-in-card-in-card nesting.
- No emoji as UI icons. One coherent icon set, documented.
- Every screen has a deliberate visual hierarchy, not uniform spacing.
- Motion is purposeful and short (< 300 ms for state changes); no decorative infinite animation except the logo.
- Touch targets ≥ 48 dp; text ≥ 14 sp; contrast passes WCAG AA.
- Light and dark themes both reviewed.
- The GitHub README is written like a product page: hero, screenshot, one-line pitch, feature grid, install instructions, architecture diagram, honest limitations.

`docs/03-design/anti-slop-rules.md` lists the bans explicitly, and `docs/13-process/ai-usage-policy.md` records which skill was used for which screen.

---

## 11. The autonomy engine

This is what makes the app worth building (D9, D18).

### 11.1 Autonomy levels (per project)

| Level | Behaviour |
|---|---|
| `ASK_EVERYTHING` | Every tool call waits for a tap. Best for untrusted or critical repos. |
| `ASK_RISKY` | Read-only and reversible actions run free; writes, deletes, network, git publish wait for a tap. |
| `AUTO_WITH_CHECKPOINTS` | Everything except hard blocks runs free; the agent must stop and report at defined checkpoints (after plan, before push, before install of dependencies). |
| `FULL_AUTO` | Nothing asks. Hard blocks (D25) still apply and are not configurable. |

Defaults: new projects start at `ASK_RISKY`; a per-project override is always one tap away.

### 11.2 The autonomous loop

```
user order
  → Planner: decompose into a visible, editable plan (steps with acceptance criteria)
  → Executor: run Claude Code with a permission mode derived from the project level
  → Verifier: run the project's verification commands (build, typecheck, lint, tests)
  → Judge:   did the task actually succeed? (not just "did the agent stop")
  →   success → commit → branch → push (if green) → PR → notify
  →   failure → diagnose → fix → re-verify → consume retry budget → repeat
  →   budget exhausted → stop, write a clear report, notify, ask the user
```

- **Retry budget** is user-configurable: 3 / 10 / 50 / unlimited. The counter is always visible.
- **Verification commands** are detected per project type (Gradle, npm, cargo, pytest…) and editable by the user. If none are found, the agent is instructed to establish them first.
- **Judge** is a separate evaluation step, not the agent's own claim. The agent saying "done" is not accepted as done; the verifier decides.
- **Anti-loop protection**: a step that repeats with no progress is detected and aborted.
- **Interruption**: the user can stop at any time; the run ends cleanly, partial work is committed on a `wip/` branch, and the state is preserved.

### 11.3 Offloading to a remote runner (D11, D2)

- If a project is configured with a remote target, or the local profile is missing a needed toolchain, or the user picks "Run on server", the run is offloaded.
- Backends: Oracle Cloud Always Free VPS (preferred, 2 ARM cores / 12 GB class — verify current quota at build time), GitHub Actions (free minutes), home PC over SSH/Tailscale.
- Offloaded runs stream events back to the app exactly like local runs. Same UI, same protocol, one flag in the session record.

---

## 12. Notifications, background, and self-improvement

### 12.1 Notifications (D24)

- Run finished (with summary + cost).
- Run stuck / budget exhausted / hard error.
- Permission request waiting (actionable: Approve / Deny / Always allow).
- GitHub: PR opened, PR comment, CI check failed, review requested.
- Remote runner offline / back online.

### 12.2 Self-improvement (D28)

- **Update check** against a release feed, shown in-app, opt-in for auto-install.
- **Self-diagnostics bundle**: export logs, config (without secrets), and environment info for a bug report.
- **Crash reporting**: local-only by default; a pluggable backend, off unless the user enables it.
- **Field-error triage**: a local log store surfaces recurring error signatures; the user can turn a signature into a one-tap "ask Claude to fix this" task in the app's own repo. The app never silently modifies itself; it prepares a branch, a diff, and a PR, and a human merges.

---

## 13. Security model

| Concern | Control |
|---|---|
| API keys / GitHub token | Keystore-encrypted, `SecretRef` everywhere, never logged, never in diffs, never exported |
| App access | Biometric or device-credential lock on launch and on wake, with a configurable grace period |
| Code execution | Hard-block list enforced in the permission layer: money, deletion, publishing, default-branch push |
| Secrets in AI prompts | A redaction pass scrubs anything that looks like a key/token from data sent to a model |
| Network | Cleartext traffic disabled; certificate pinning not required but TLS enforced |
| Storage | Project data in app-private storage; a visible "delete app data" action in settings |
| Screen privacy | Optional `FLAG_SECURE` mode that hides content in the recents screenshot |
| Backups | Android auto-backup excluded for secrets; projects opt-in |
| Third-party content | Installing a skill or a plugin shows a diff of what will be written |
| Abuse | A project may not be configured to spend money; the app refuses such projects and says why |

Threat model lives in `docs/11-operations/security-threat-model.md`, written before the first security-relevant file.

---

## 14. Data model (summary)

Entities: `Provider`, `SecretProfile`, `Project`, `ProjectSetting`, `Conversation`, `Turn`, `Message`, `ToolInvocation`, `ToolResult`, `Plan`, `PlanStep`, `FileChange`, `DiffEntry`, `VerificationRun`, `TestResult`, `CostRecord`, `RunSummary`, `Skill`, `SkillInstall`, `MarketplaceEntry`, `RemoteTarget`, `RunnerStatus`, `SessionLogEntry`, `NotificationEvent`, `AppSetting`, `PendingPermission`.

Key relationships:
- Conversation → many Turns → many Messages
- Turn → many ToolInvocations → one ToolResult
- Run → one Plan (many PlanSteps), many FileChanges, one VerificationRun, one CostRecord
- Project → many Conversations, many Skills (scoped), one RemoteTarget (optional), one ProjectSetting (autonomy)
- SessionLogEntry is append-only and immutable (D25: never hide anything)

All access through repositories in `shared/data`. No DAO is called from a ViewModel.

---

## 15. Testing strategy (D21 — exhaustive)

| Layer | What | Target |
|---|---|---|
| Unit | Domain logic, parsers (SSE, stream-json, diff), state machines, cost math, permission policy, retry logic, skill validation | 90 % line coverage on `shared/domain` |
| Integration | Repositories against in-memory Room, Ktor against a mock server (MockEngine), runtime state machine with a fake filesystem | 80 % on `shared/data` |
| UI (Compose) | Every screen: render, interaction, navigation, empty/loading/error states, dark/light, large font | Every screen + every state |
| Screenshot (Paparazzi) | Golden images per screen per theme, with a review workflow | Baseline committed, diffs must be justified |
| E2E | The five real user journeys on an emulator: onboarding, one-command task, review a diff, install a skill, self-update check | All pass on every merge |
| Contract | `AgentEvent` schema compatibility; a recorded fixture stream from real Claude Code output | Cannot break silently |
| Performance | Startup time, cold bootstrap, list scrolling (macrobenchmark), memory | Budgets defined in `docs/09-testing/performance-budgets.md` |
| Accessibility | Contrast, touch targets, screen-reader labels, reduce-motion | No critical findings |

Additionally: `detekt`, `ktlint`, `lint` and Kover thresholds run in `./gradlew check` and gate the build. A build with a red gate is a failed build.

---

## 16. Delivery

- **Debug APK** for daily use, installable by hand.
- **Release APK** signed with a documented keystore; keystore never committed; documented recovery procedure.
- **App Bundle (AAB)** for Play Store, with Play listing, screenshots (phone + tablet), privacy policy, data-safety form filled in.
- **F-Droid** metadata: the app must build from source with only FOSS-available dependencies, or declare the reason.
- **CI**: GitHub Actions — build, check, all tests, lint, screenshot diff, and a nightly E2E run on an emulator.
- **Release checklist** in `docs/14-delivery/release-checklist.md`, signed off per release.

---

## 17. Legal and branding (D13, D25)

- This is an **unofficial, independent** project. It is not affiliated with, endorsed by, or sponsored by Anthropic.
- The name `Claude Code Android` is used descriptively. All logos, wordmarks, and brand assets are the property of their owners and are **not** copied. The app ships its **own** mark (documented in `docs/03-design/brand-assets.md`), inspired by but visually distinct from the reference.
- A `THIRD_PARTY_NOTICES.md` lists Anthropic, Claude Code, and all dependencies.
- A `NOTICE` in the README and in the app's About screen states the unofficial status clearly.
- Copyright headers on every file, with the license in `LICENSE`.

---

## 18. Documentation manifest — the 80+ `.md` files

**This is the deliverable that makes the build autonomous and reproducible.** All files live in `docs/`, are written in English, and must exist before the build can be considered complete. Every document states what it is for and what depends on it.

**Total: 135 markdown documents** (plus 3 root-level: `README.md`, `CLAUDE.md`, `THIRD_PARTY_NOTICES.md`). The number is a floor, not a target — if a document needs splitting, split it.

### 00 — Vision & scope (7)

| File | Purpose |
|---|---|
| `docs/00-vision/README.md` | Index of the whole doc set, reading order, and who each doc is for |
| `docs/00-vision/vision.md` | What this product is, the one-sentence promise, the problem it solves |
| `docs/00-vision/scope.md` | In scope / out of scope for v1, and the explicit non-goals |
| `docs/00-vision/user-stories.md` | Every user story, given/when/then, mapped to a screen |
| `docs/00-vision/personas.md` | The two personas: the non-technical owner and the hobbyist developer |
| `docs/00-vision/glossary.md` | Every domain term in plain language (CLI, SDK, PTY, tool call, worktree, …) |
| `docs/00-vision/success-metrics.md` | How we know the port succeeded: build success, first-task success rate, crash-free rate, task time |

### 01 — Research & decisions (8)

| File | Purpose |
|---|---|
| `docs/01-research/claude-code-runtimes-on-android.md` | The full survey: native glibc patch, proot, AVF, Termux. Findings, trade-offs, citations, what actually works today. **Must be re-verified at build time.** |
| `docs/01-research/agent-sdk-and-cli-surface.md` | Every flag, output format, permission mode, exit code, and streaming event we rely on |
| `docs/01-research/provider-api-comparison.md` | Anthropic Messages vs OpenAI Chat vs Responses vs custom; auth, streaming, tool use, pricing fields |
| `docs/01-research/design-reference-audit.md` | A careful teardown of the reference app's layout, spacing, type, motion, and states — as a description, not a copy of assets |
| `docs/01-research/github-auth-options.md` | OAuth App vs GitHub App vs fine-grained PAT; scopes; device flow; webhooks vs polling |
| `docs/01-research/free-remote-runner-options.md` | Oracle Always Free, GitHub Actions, home PC: quotas, setup, cost, reliability, current terms. **Verify quotas at build time.** |
| `docs/01-research/legal-and-trademark.md` | Unofficial status, mark usage, what must not be copied, licence obligations |
| `docs/01-research/competitive-landscape.md` | Other mobile Claude Code / Termux / cloud approaches and how this differs |

### 02 — Architecture (10)

| File | Purpose |
|---|---|
| `docs/02-architecture/system-overview.md` | The big diagram, the one page everyone reads first |
| `docs/02-architecture/module-map.md` | Every module, its responsibility, its public API, and its dependencies |
| `docs/02-architecture/layer-contracts.md` | What each layer may and may not do; the dependency rules that CI enforces |
| `docs/02-architecture/data-model.md` | Every entity, every field, every relation, with rationale |
| `docs/02-architecture/data-migrations.md` | Migration strategy, versioning, and the rule for destructive changes |
| `docs/02-architecture/concurrency-model.md` | Coroutine scopes, dispatchers, what runs where, cancellation, structured concurrency rules |
| `docs/02-architecture/error-taxonomy.md` | Every error class, its code, its user-facing message, its retryability, its recovery action |
| `docs/02-architecture/event-protocol.md` | `AgentEvent` and `OutboundMessage`: full schema, versioning, compatibility rules, fixture examples |
| `docs/02-architecture/state-machines.md` | Bootstrap, session, run, and skill-install state machines with every transition |
| `docs/02-architecture/adr-log.md` | Architecture Decision Records — one per significant choice, with alternatives and consequences |

### 03 — Design (12)

| File | Purpose |
|---|---|
| `docs/03-design/design-tokens.md` | The binding token set: colour, type, spacing, radii, elevation, motion, haptics |
| `docs/03-design/color-and-contrast.md` | Every colour with its light/dark value and its measured contrast ratio |
| `docs/03-design/typography.md` | The type scale, chosen families, weights, line heights, and why |
| `docs/03-design/spacing-and-layout.md` | The 4pt scale, screen grid, gutters, edge behaviour, tablet/landscape rules |
| `docs/03-design/motion.md` | Every animation: purpose, duration, curve, and the reduce-motion fallback |
| `docs/03-design/logo-animation.md` | The animated mark: state machine, timings, geometry, expressions, accessibility fallback |
| `docs/03-design/component-library.md` | Every reusable component, its variants, its states, and its usage rule |
| `docs/03-design/anti-slop-rules.md` | The explicit ban list and the review checklist. Binding on all UI code. |
| `docs/03-design/accessibility.md` | WCAG AA targets, screen-reader semantics, touch targets, reduce motion, font scaling |
| `docs/03-design/responsive.md` | Phone portrait/landscape, foldables, tablets, and window-size classes |
| `docs/03-design/brand-assets.md` | The app's own mark and wordmark, source, and permitted use |
| `docs/03-design/illustration-set.md` | Empty states, onboarding art, and icons — one coherent set, no emoji |

### 04 — Screen specifications (14)

One file per screen, each with layout, states (loading/empty/error/content), interactions, navigation, analytics-free telemetry hooks, and golden-image references.

`docs/04-screens/01-onboarding.md` · `02-chat-list.md` · `03-new-chat.md` · `04-chat-detail.md` · `05-chat-with-terminal.md` · `06-diff-viewer.md` · `07-project-list.md` · `08-project-detail.md` · `09-add-project.md` · `10-skills-browser.md` · `11-skill-editor.md` · `12-terminal.md` · `13-settings.md` · `14-remote-runner-setup.md`

### 05 — Feature specifications (16)

| File | Purpose |
|---|---|
| `docs/05-features/chat-and-streaming.md` | Message model, streaming, tool cards, plan display, cost meter, interrupt |
| `docs/05-features/autonomy-levels.md` | The four levels, exactly what each permits and blocks, and how to change one |
| `docs/05-features/verification.md` | How a project declares its build/test/lint commands, how they are detected, how results are judged |
| `docs/05-features/retry-and-self-healing.md` | Retry budget, diagnosis, fix, re-verify, anti-loop detection, exhaustion behaviour |
| `docs/05-features/planning.md` | How plans are produced, edited, and tracked; checkpoint semantics |
| `docs/05-features/project-lifecycle.md` | Create, clone, open, rename, archive, and the (never) delete rule |
| `docs/05-features/run-history.md` | What is recorded per run and how to inspect, export, and revisit it |
| `docs/05-features/transparency-log.md` | The append-only activity log: every command, diff, decision, and error |
| `docs/05-features/background-execution.md` | Foreground service, notifications, wake locks, screen-off behaviour, resumption |
| `docs/05-features/attachments.md` | Files, images, and @-references from the device into a run |
| `docs/05-features/model-selection.md` | Choosing a model per project and per run, with capability and cost hints |
| `docs/05-features/file-browser.md` | Project file tree, in-app viewing, and the path picker |
| `docs/05-features/search.md` | Search across conversations, projects, and skills |
| `docs/05-features/import-export.md` | Export a run, export a project, import into a project |
| `docs/05-features/self-update.md` | Checking, staging, and installing app updates; the self-build flow |
| `docs/05-features/field-error-triage.md` | Turning recurring error signatures into a one-tap fix task |

### 06 — Runtime (8)

| File | Purpose |
|---|---|
| `docs/06-runtime/execution-backends.md` | The `ExecutionBackend` interface, capabilities, and each implementation |
| `docs/06-runtime/native-profile.md` | glibc-runner install, binary patching, checksum verification, updates |
| `docs/06-runtime/proot-profile.md` | The Ubuntu profile, when to choose it, its cost in storage and time |
| `docs/06-runtime/avf-profile.md` | The experimental VM profile, detection, and its limits |
| `docs/06-runtime/bootstrap-state-machine.md` | Every step, its progress, its resumability, its failure and recovery |
| `docs/06-runtime/process-supervision.md` | Process trees, cancellation, signals, the ring buffer, exit codes |
| `docs/06-runtime/pty-and-terminal.md` | PTY allocation, resize, ANSI, rendering, keyboard, persistence |
| `docs/06-runtime/environment-diagnostics.md` | Pre-flight health checks and the user-facing "what's wrong, how to fix it" |

### 07 — Integrations (8)

`docs/07-integrations/github-api.md` (repos, branches, PRs, checks) · `github-auth.md` (both flows, scopes, refresh, revocation) · `github-notifications.md` · `remote-runners.md` (Oracle/GitHub Actions/home PC, setup and lifecycle) · `notifications.md` (channels, content, actions) · `providers.md` (kinds, model discovery, connection tests) · `secrets.md` (Keystore, profiles, redaction) · `mcp.md` (how MCP servers are configured and shown, if used)

### 08 — Orchestration (6)

`docs/08-orchestration/agent-request-lifecycle.md` · `permissions.md` (the hard-block list, exactly) · `session-management.md` (create, resume, fork, archive) · `context-and-tokens.md` (budgeting, compaction, cost) · `subagents-and-parallelism.md` · `failure-recovery.md`

### 09 — Testing (10)

`docs/09-testing/test-strategy.md` · `unit-tests.md` · `integration-tests.md` · `ui-tests.md` · `screenshot-tests.md` · `e2e-journeys.md` · `contract-tests.md` · `performance-budgets.md` · `fixtures-and-test-data.md` · `test-data-safety.md` (no real keys, no real repos in tests)

### 10 — Build & tooling (7)

`docs/10-build/gradle-setup.md` · `dependency-versions.md` · `convention-plugins.md` · `build-variants.md` · `signing-and-keystores.md` · `static-analysis.md` · `local-build-and-run.md` (the exact commands, from clone to installed APK)

### 11 — Operations (7)

`docs/11-operations/logging.md` · `crash-reporting.md` · `diagnostics-export.md` · `backup-and-restore.md` · `privacy.md` · `telemetry.md` (default: none) · `security-threat-model.md`

### 12 — Delivery (5)

`docs/12-delivery/apk-distribution.md` · `f-droid.md` · `play-store.md` · `github-readme-guide.md` (the README's structure, hero, screenshots, honest limitations) · `release-checklist.md`

### 13 — Process & AI usage (6)

`docs/13-process/development-workflow.md` · `git-strategy.md` (branching, commits, PRs, version bumps) · `code-review.md` · `definition-of-done.md` · `ai-usage-policy.md` (which skill is used where, anti-slop enforcement) · `claude-code-instructions.md` (how the builder must behave)

### 14 — Build plan (6)

`docs/14-build-plan/phase-plan.md` (all 7 phases) · `task-breakdown.md` (every task, with ID, phase, estimate, dependencies, acceptance) · `dependency-graph.md` · `milestones.md` · `risk-register.md` · `progress-log.md` (kept current as the build proceeds)

### 15 — Appendix (5)

`docs/15-appendix/references.md` (every external source, with URL and access date) · `glossary-extended.md` · `troubleshooting.md` · `faq.md` · `changelog.md`

**Plus 3 root-level documents:** `README.md`, `CLAUDE.md`, `THIRD_PARTY_NOTICES.md`.

---

## 19. Source file manifest

~150 files to generate. Each line is a commitment. No extra files without a spec amendment; no missing files without a documented exception.

### 19.1 `shared/core` — platform gateways (18)

| File | Purpose |
|---|---|
| `Result.kt` | `Outcome<T>` sealed type for every fallible call |
| `AppError.kt` | The error hierarchy mirroring `docs/02-architecture/error-taxonomy.md` |
| `Dispatchers.kt` | expect/actual dispatcher set (IO, Default, Main, Unconfined) |
| `TimeProvider.kt` | Injectable clock, so time-dependent code is testable |
| `IdGenerator.kt` | ULID-based ids for sessions, tasks, runs |
| `Logger.kt` | Structured, redactable, level-aware logging |
| `Redactor.kt` | Strips anything key-shaped from text before it is logged or sent |
| `BuildInfo.kt` | Version, build type, flavour, git sha |
| `FlowExt.kt` | debounce, sample, retryWithBackoff, stateIn helpers |
| `ProcessGateway.kt` | expect/actual process spawn, kill-tree, exit codes |
| `CryptoGateway.kt` | expect/actual encrypt/decrypt with a platform keystore |
| `FileSystemGateway.kt` | expect/actual fs: list, read, write, delete, stat, watch |
| `NetworkMonitor.kt` | expect/actual connectivity and metered-network awareness |
| `ClipboardGateway.kt` | Copy/paste bridge |
| `PlatformCapabilities.kt` | expect/actual capability flags used to gate features |
| `actual/Dispatchers.android.kt` | Android dispatcher bindings |
| `actual/ProcessGateway.android.kt` | Android process implementation |
| `actual/CryptoGateway.android.kt` | Android Keystore implementation |

### 19.2 `shared/domain` — models and use cases (16)

`Provider.kt` · `ModelSpec.kt` · `Project.kt` · `ProjectSettings.kt` · `AutonomyLevel.kt` · `Conversation.kt` · `Turn.kt` · `Message.kt` · `MessagePart.kt` · `ToolInvocation.kt` · `Plan.kt` · `FileChange.kt` · `DiffEntry.kt` · `VerificationRun.kt` · `CostRecord.kt` · `SkillDescriptor.kt`

Plus `use_case/RunProjectTask.kt`, `use_case/ApprovePermission.kt`, `use_case/InterruptRun.kt`, `use_case/VerifyProject.kt`, `use_case/CommitAndProposePullRequest.kt`, `policy/HardBlockPolicy.kt`, `policy/PermissionResolver.kt`, `policy/BudgetPolicy.kt`.

### 19.3 `shared/data` — persistence and network (20)

`db/AppDatabase.kt` · `db/Converters.kt` · `db/Migrations.kt` · `dao/ProjectDao.kt` · `dao/ConversationDao.kt` · `dao/TurnDao.kt` · `dao/ToolInvocationDao.kt` · `dao/SkillDao.kt` · `dao/CostDao.kt` · `dao/LogDao.kt` · `dao/ProviderDao.kt` · `dao/RemoteTargetDao.kt` · `repository/ProjectRepository.kt` · `repository/ConversationRepository.kt` · `repository/SkillRepository.kt` · `repository/ProviderRepository.kt` · `repository/LogRepository.kt` · `repository/SettingsRepository.kt` · `remote/AgentStreamClient.kt` (SSE + `stream-json` decoding) · `remote/CostEstimator.kt`

### 19.4 `shared/runtime` — the local engine (14)

`RuntimeProfile.kt` · `BootstrapManager.kt` · `state/BootstrapStateMachine.kt` · `install/GlibcRuntimeInstaller.kt` · `install/ClaudeBinaryInstaller.kt` · `install/ChecksumVerifier.kt` · `install/ProotDistroInstaller.kt` · `install/AvfDetector.kt` · `install/UpdateManager.kt` (daily version check) · `process/ProcessSupervisor.kt` · `process/ProcessTree.kt` · `pty/PtyBridge.kt` · `pty/TerminalSession.kt` · `diagnostics/HealthCheck.kt`

### 19.5 `shared/orchestration` — the autonomy engine (12)

`AgentSessionManager.kt` · `AgentRequestBuilder.kt` · `AgentEventMapper.kt` · `Planner.kt` · `Executor.kt` · `Verifier.kt` · `Judge.kt` · `RetryController.kt` · `AntiLoopDetector.kt` · `CheckpointManager.kt` · `ContextBudgeter.kt` · `SelfImprovementService.kt`

### 19.6 `shared/vcs` — git and GitHub (12)

`GitClient.kt` (a real git CLI wrapper, never a shell string) · `BranchManager.kt` · `CommitBuilder.kt` · `PushPolicy.kt` (the never-push-to-default rule) · `PullRequestService.kt` · `GitHubClient.kt` · `GitHubAuthManager.kt` (OAuth + PAT) · `OAuthDeviceFlow.kt` · `NotificationPoller.kt` · `RepoCloneService.kt` · `RateLimitMonitor.kt` · `GitCommandSanitizer.kt` (blocks destructive commands)

### 19.7 `shared/skills` — skills subsystem (8)

`SkillParser.kt` · `SkillValidator.kt` · `SkillInstaller.kt` (from URL, from archive, from AI) · `SkillGenerator.kt` (AI creation) · `SkillRegistry.kt` · `SkillScopeResolver.kt` · `MarketplaceClient.kt` · `SkillEditorModel.kt`

### 19.8 `shared/ui` — design system (14)

`theme/Color.kt` · `theme/Type.kt` · `theme/Spacing.kt` · `theme/Shape.kt` · `theme/Motion.kt` · `theme/Theme.kt` · `component/AnimatedClaudeMark.kt` · `component/StreamingText.kt` · `component/ToolCard.kt` · `component/PlanView.kt` · `component/CostMeter.kt` · `component/Composer.kt` · `component/DiffView.kt` · `component/Primitives.kt` (buttons, cards, sheets, chips, empty states)

### 19.9 `shared/ui/features` — feature screens (~30)

`chat/ChatListScreen.kt` · `chat/ChatViewModel.kt` · `chat/ChatUiState.kt` · `chat/ChatScreen.kt` · `chat/ChatTerminalSplit.kt` · `chat/NewChatScreen.kt` · `chat/MessageBubble.kt` · `chat/PermissionSheet.kt` · `chat/AutonomyChip.kt` · `chat/CostBadge.kt`
`projects/ProjectListScreen.kt` · `projects/ProjectListViewModel.kt` · `projects/ProjectDetailScreen.kt` · `projects/ProjectDetailViewModel.kt` · `projects/AddProjectScreen.kt` · `projects/BranchSheet.kt` · `projects/PullRequestList.kt`
`skills/SkillsBrowserScreen.kt` · `skills/SkillsViewModel.kt` · `skills/SkillEditorScreen.kt` · `skills/SkillCreatorScreen.kt` · `skills/MarketplaceScreen.kt`
`terminal/TerminalScreen.kt` · `terminal/TerminalViewModel.kt` · `terminal/TerminalRenderer.kt` · `terminal/TerminalKeyboardRow.kt`
`settings/SettingsScreen.kt` · `settings/SettingsViewModel.kt` · `settings/ProvidersScreen.kt` · `settings/ProviderEditorScreen.kt` · `settings/SecurityScreen.kt` · `settings/RemoteRunnerScreen.kt` · `settings/AboutScreen.kt` · `settings/LanguageScreen.kt`
`diff/DiffScreen.kt` · `diff/DiffViewModel.kt` · `onboarding/OnboardingScreen.kt` · `onboarding/OnboardingViewModel.kt` · `onboarding/RuntimeSetupScreen.kt` · `onboarding/KeySetupScreen.kt` · `onboarding/GitHubSetupScreen.kt`

### 19.10 `androidApp` — Android-specific (~24)

`AndroidManifest.xml` · `App.kt` · `MainActivity.kt` · `di/AppModule.kt` · `di/DataModule.kt` · `di/NetworkModule.kt` · `service/AgentForegroundService.kt` · `service/ServiceController.kt` · `service/NotificationChannels.kt` · `service/BootReceiver.kt` · `security/BiometricGate.kt` · `security/KeystoreSecretStore.kt` · `security/SecureFlagController.kt` · `storage/AndroidDatabaseFactory.kt` · `storage/DataStoreFactory.kt` · `storage/AppBackupManager.kt` · `network/AndroidHttpClient.kt` · `update/AppUpdater.kt` · `receiver/DeepLinkReceiver.kt` · `widget/ProjectWidget.kt` · `widget/QuickActionTile.kt` · `util/RedactingLogTree.kt` · `CrashReporter.kt` · `res/values/strings.xml` (German) · `res/values-en/strings.xml` (English)

### 19.11 `iosApp` — scaffold only (4)

`iosApp/README.md` (what is stubbed and why) · `iosApp/ClaudeCodeAndroidApp.swift` · `iosApp/ContentView.swift` · `iosApp/Info.plist` (keys mirrored)

### 19.12 `tools/` and `.claude/` (~20)

`tools/bootstrap_linux.sh` · `tools/install_claude.sh` · `tools/verify_checksums.sh` · `tools/check_doc_manifest.py` (fails the build if a doc is missing) · `tools/check_source_manifest.py` · `tools/check_no_android_imports_in_shared.py` · `tools/format.sh` · `tools/ci.sh` · plus the 12 `.claude/` files in §20.

### 19.13 Root (8)

`README.md` · `CLAUDE.md` · `LICENSE` · `NOTICE` · `THIRD_PARTY_NOTICES.md` · `.gitignore` · `.editorconfig` · `CONTRIBUTING.md`

---

## 20. The Claude Code autonomy kit

These files make the build itself run without a human. They are part of the deliverable.

### 20.1 `CLAUDE.md` (root)

Contains: what this project is, the tech stack with pinned versions, the build/test/lint commands, the code style rules, the **hard-block rules** (never delete, never spend, never publish, never push to default), the doc-update obligation (any behaviour change updates the matching doc), the anti-slop UI rules, the phase plan, and the definition of done. Written so a fresh agent with zero context does the right thing.

### 20.2 `.claude/settings.json`

- `permissions.allow` for the read-only and build commands the build may always run.
- `permissions.deny` for the hard blocks (destructive git, paid services, force push, deleting the default branch, publishing).
- `hooks`: after each file write → run the format+lint fixers; after each build → if red, immediately read the error and fix; on test failure → capture output into the task record.
- `env`: log level, no telemetry, deterministic test mode.

### 20.3 `.claude/commands/` — 8 slash commands

`/phase-start` (begin a phase from the plan) · `/verify` (run the full quality gate and report) · `/fix` (read the last failure and fix it, up to the retry budget) · `/ship` (commit, branch, push, open PR) · `/doc-sync` (update the docs that the last change affected) · `/ui-polish` (run the design skills over the changed screens) · `/release` (run the release checklist) · `/status` (progress against the plan).

### 20.4 `.claude/agents/` — 6 subagents

`architect` (design and write docs before code) · `implementer` (writes code against a spec + tests) · `tester` (writes and runs tests, reports failures precisely) · `debugger` (root-causes a failure and proposes the minimal fix) · `designer` (owns the design system and applies the design skills) · `reviewer` (checks a change against the spec, the anti-slop rules, and the hard blocks).

### 20.5 `.claude/skills/` — 8 project skills

`android-compose` (Compose + Material 3 patterns for this project) · `kmp-shared` (rules for keeping `shared/` portable) · `runtime-bootstrap` (how to work on the bootstrap safely) · `ui-design` (the design system and anti-slop rules, delegating to the installed design skills) · `github-safety` (branch/commit/PR rules, hard blocks) · `docs-authoring` (the doc structure and style) · `test-authoring` (test patterns and fixtures) · `release` (build, sign, package, publish metadata).

### 20.6 `.mcp.json` (optional, additive only)

Any MCP server added must be optional, must not be required for the build, and must be declared in the docs. The build must succeed with no MCP server configured.

---

## 21. The one-shot, staged build plan

Seven phases. Each phase ends green or the build stops and reports. No phase is skipped, and no phase is merged into the next.

### Phase 0 — Foundation
Docs skeleton (all 121 files stubbed with purpose and status), repo init, Gradle + version catalog + convention plugins, KMP module skeleton, CI, quality gates wired, manifest checkers.
**Done when:** `./gradlew check` runs, the doc manifest check passes, CI is green on an empty app.

### Phase 1 — Design system
Tokens, theme, primitives, the animated mark, the anti-slop rules applied to a design-review pass. All 12 design docs complete.
**Done when:** every token is used by at least one component, light and dark both pass contrast, the mark's state machine is covered by screenshot tests.

### Phase 2 — Data & core
Domain models, Room schema, repositories, secrets/Keystore, providers with connection tests, settings, i18n skeleton.
**Done when:** unit coverage ≥ 90 % on `shared/domain`, migrations tested, a key round-trips through the Keystore and never appears in a log.

### Phase 3 — Runtime
Bootstrap state machine, native profile (glibc + patched binary + checksum), proot profile, process supervision, PTY, terminal renderer, health checks.
**Done when:** on a real device or emulator, the app downloads, verifies, installs, launches `claude --version`, runs a headless prompt, and streams the output back. Failing this phase is a hard stop — do not fake it.

### Phase 4 — Chat & projects
The chat experience end to end: streaming, tool cards, plan, cost, permissions, interrupt, project list/detail, diff viewer, run history, transparency log.
**Done when:** the E2E journey "open app → new chat → pick project → send order → watch stream → interrupt → resume" passes on an emulator.

### Phase 5 — GitHub, skills, remote runners
Both auth flows, clone, branch, commit, test-gated push, PR, GitHub notifications; skills install/generate/manage/marketplace; Oracle/GitHub Actions/home PC backends with offload.
**Done when:** a task run against a private test repo produces a green branch and an open PR, and a skill installs from a URL and is usable in a run.

### Phase 6 — Verification & self-healing
Verifier, judge, retry controller, anti-loop detection, checkpoints, context budgeter, self-update, field-error triage.
**Done when:** a deliberately broken project is given a task and reaches green within the budget, and the budget-exhaustion path reports correctly.

### Phase 7 — Autonomy, polish & delivery
Background execution, notifications, all remaining screens, screenshot polish with the design skills, accessibility pass, performance pass, security pass, README, Play/F-Droid metadata, release checklist.
**Done when:** every quality gate in §22 is green, the README is beautified, and a signed release APK is produced.

### Definition of one-shot

All seven phases run in a single autonomous session, in order, with no human in the loop. On failure the builder applies `/fix` up to the retry budget, and if it still fails, records the exact blocker in `docs/14-build-plan/progress-log.md`, moves to nothing, and stops with a clear report.

---

## 22. Quality gates and non-negotiables

### 22.1 Hard blocks — never negotiable, in any mode, for any user

1. **Never delete** anything: no file deletion, no branch/tag deletion, no repo deletion.
2. **Never spend money**: no paid API, no subscription, no purchase, no paid tier.
3. **Never make anything public**: private repos only, no public artifacts.
4. **Never push to the default branch.**
5. **Never hide anything**: every command, diff, decision, error, and cost is recorded and visible.

These are enforced in `HardBlockPolicy`, verified by tests, and additionally denied at the Claude Code permission layer. No UI can enable them.

### 22.2 Gates — the build is not done until all are green

| Gate | Threshold |
|---|---|
| `./gradlew assembleDebug` | succeeds |
| `./gradlew check` (lint + detekt + ktlint + tests) | 0 errors |
| Unit coverage on `shared/domain` | ≥ 90 % |
| Integration coverage on `shared/data` | ≥ 80 % |
| Every screen | has UI tests for content, loading, empty, error, dark, and light |
| Screenshot tests | baselines committed; no unexplained diffs |
| E2E journeys | all 5 pass on an emulator |
| Accessibility | no critical findings; contrast AA; targets ≥ 48 dp |
| Doc manifest | all 135 docs present and non-stub |
| Source manifest | all listed files present |
| Shared-layer purity | no Android imports in `shared/core` or `shared/domain` |
| Secret scan | zero secrets in code, logs, fixtures, or the repo |
| Hard-block tests | all pass, including the "attempt to delete is refused" case |

### 22.3 Anti-slop enforcement

Every UI screen must have a `docs/13-process/ai-usage-policy.md` entry naming the design skill used. A reviewer subagent rejects generic output. The README gets the same treatment.

---

## 23. Assumptions and open risks

| # | Item | Status | Handling |
|---|---|---|---|
| R1 | No official Claude Code Android build exists | Confirmed by research | Two community profiles implemented; upstream issue tracked in `docs/01-research` |
| R2 | The glibc patch could break on a future Claude Code release | Real risk | Checksum + launch verification + automatic rollback to the last known-good version; the update checker validates before swapping |
| R3 | proot images are large and slow | Real risk | Profile is opt-in; the UI shows the cost up front |
| R4 | Anthropic may change CLI flags or output format | Real risk | All parsing is behind `AgentEventMapper` with fixture contract tests, so a change fails loudly in tests, not silently in production |
| R5 | Free-tier cloud quotas change | Real risk | Remote runner setup is verified at runtime with a live quota check, and the app degrades gracefully |
| R6 | GitHub OAuth needs a registered app | Known | Both OAuth and PAT work; PAT is the zero-setup path and the app says exactly how to create one |
| R7 | Long autonomous runs drain the phone battery | Real risk | Foreground service with an honest, visible battery/usage note; remote offload offered |
| R8 | The 1:1 visual similarity has legal edges | Real risk | Own mark, own assets, unofficial notice in the app and README; documented in `docs/01-research/legal-and-trademark.md` |
| R9 | iOS readiness is claimed but untested | Accepted | The iOS target is scaffolded and compiles no further; the shared layer has a CI check for portability, nothing more is promised |
| R10 | A 121-doc set can drift out of sync with the code | Real risk | `tools/check_doc_manifest.py` runs in CI; `docs/14-build-plan/progress-log.md` is updated every phase |

### Assumptions taken (state them, do not hide them)

- The user has an Anthropic API key and is willing to use one. A subscription login is not supported and is not planned.
- The user has a Google account for Play Store publishing, but publishing is not part of v1.
- The test device is the user's current phone; minimum SDK will be determined at build time and documented. Target: support Android 8+ with a modern-device-optimised experience.
- The user can install a debug APK by hand.

---

## 24. What "done" means

The project is done when:

1. All 135 documents exist, are non-stub, and match the code.
2. All ~150 source files listed in §19 exist and do what their one-liner says.
3. The seven phases are green against the gates in §22.
4. A fresh clone, one command, produces an installable signed APK.
5. On a real device, a non-technical user can: open the app, let it set up, paste a key, connect GitHub, pick a private repo, type an order, watch it work, approve the diff, and receive the PR — without a single question being asked of them.
6. The README is beautiful and honest.
7. The hard blocks hold.
