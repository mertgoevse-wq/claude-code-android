# SPEC-ADDENDUM — Full port, Claude-app UI parity, multi-provider, device profiling

**An interview-derived amendment to `claude-code-android-spec.md`. It records what the user decided on 2026-09-30 and how to resume the build perfectly.**

| Field | Value |
|---|---|
| Addendum version | 1.0.0 |
| Date | 2026-09-30 |
| Status | Interviewed, ready to fold into the build plan |
| Parent spec | `claude-code-android-spec.md` v1.0.0 (remains the base contract; unchanged decisions keep their IDs D1–D29) |
| Method | Repository audit + user interview (4 rounds, 16 questions, answers locked below as D30–D48) + web research (sources in §7) |
| Document language | English (project rule). The user reads German; the chat reply to this file's creation carries a German summary. |

---

## 0. How this file relates to the parent spec

- This is an **amendment, not a replacement**. On conflict, the parent spec wins **except** where a decision here explicitly amends it (each such row says so).
- The interview decision log (parent §2) is extended by **D30–D48** (§2 here). Changing one requires an explicit spec amendment, same rule as before.
- "No invented versions / quotas / APIs" applies to everything new here: items marked **TBD — verify at build time** must be verified, with the lookup date recorded in `docs/10-build/dependency-versions.md`.
- Nothing here relaxes a hard block or a gate. `./gradlew check` stays the single gate.

---

## 1. Where the project actually is (resume context, honest snapshot)

| Field | Value |
|---|---|
| Phase | **2 — Data and core**, tasks P2-1 … P2-8 done, **P2-9 in progress** |
| Build status | **Green** (`./gradlew check`, 314 actionable tasks) |
| Working tree | Uncommitted P2-9 slice: `SecretStore` interface (core), `KeystoreSecretStore` + `AndroidCryptoGateway` (androidApp), `SecretProfileService` / `AppSettingsShop` (data), use cases + tests, Room schema v2, new CI check `tools/check_typed_errors_at_boundaries.py`, English strings, and the matching `docs/02-architecture/error-taxonomy.md` update |
| Blocker | **B2 open** — the build machine is the target device (Galaxy A56); APK install needs one-time wireless-debugging pairing (`tools/wireless_debug_watch.sh`) or a manual tap per install. The build itself is not blocked. |
| Open risks | R1 (ELF patch), R9 (doc drift), R10 (ANSI palette verified) |
| Branch | `task/phase-2-data-and-core`, private repo `mertgoevse-wq/claude-code-android` |
| **Staleness warning** | `task_plan.md` still shows P2-3…P2-8 unchecked and "Next step: P2-3". It is **stale**. `docs/14-build-plan/progress-log.md` is authoritative. Resyncing `task_plan.md` is part of the next commit (§5, step 2). |
| **Fact correction (D48)** | Earlier docs (including `CLAUDE.md`) call the Galaxy A56 a **Snapdragon** device. It is **not**: the A56 runs a **Samsung Exynos 1580** (Xclipse 540 GPU, RDNA 3; 8/12 GB RAM; NPU rated up to 14.7 TOPS per Samsung). Consequences: (a) fix the wrong claim wherever it appears (doc-sync obligation, same commit as the next code change); (b) AVF support on Exynos 1580 is **not** "unavailable upstream because Snapdragon" — it is **TBD — verify by runtime probe**. The first-launch device survey (D36) records the truth per device; no profile may be assumed. |

---

## 2. New interview decisions (D30–D48)

| # | Area | Decision |
|---|---|---|
| D30 | Sequencing | The existing 7-phase plan **continues**; every new requirement below becomes an **amendment task** with its own ID, dependencies, and acceptance line. Engine ("inner") and UI ("outer") are **equally weighted**; the agent chooses the concrete order that minimises risk. |
| D31 | UI parity trial | While the app is private, the UI is a **1:1 trial clone of the official Claude app**: structure, layout, look, and the chat experience, including the later left-side Chat / Projects / Claude-Code switcher. Before **any** public release: mandatory rebrand pass (own name + logo) and a legal/trademark review. Own branding and paid features may then diverge gradually. |
| D32 | Later switcher | The Claude-app-style **left side switcher** (Chat / Projects / Claude Code) is planned from the start as a Phase-6 navigation task (see D33). |
| D33 | Navigation now | **Bottom tab bar now** (Chat, Projects, Skills, Terminal, Settings — parent D15). Retrofit to the Claude-app side-menu layout is an explicit **Phase-6 task**, not an afterthought. *(Amends the trajectory of parent D15; the bar itself is unchanged for now.)* |
| D34 | Model picker | **Both**: a compact chip directly above the keyboard (model name + reasoning level, one tap to cycle) **and** a full bottom sheet on tap (provider list, model list, reasoning level, price preview). Reasoning level: off / medium / high, mapped per provider at implementation time. |
| D35 | Provider catalog + wizard | Providers are added via a **catalog** (per-provider card: German explanation, button opening the provider's signup page, paste-key field, connection test) **plus a guided setup wizard** (pre-filled base URL, step-by-step, test, done). Named catalog targets: **OpenRouter, Groq, Google AI Studio** — plus "many, many" custom providers: anything Anthropic-compatible or OpenAI-compatible, and free-credit promotional providers. Every catalog entry carries a **free-tier flag**; catalog data ships as local JSON (no hosted index, consistent with parent §1.4). |
| D36 | Device profiling | On **first launch, exactly once**, the app runs a **device survey** and persists a `DeviceProfile`: SoC/model, core layout, RAM, storage, GPU, NPU/AI-accelerator detection, Android version, thermal class, battery state, display refresh rate. The profile drives: (a) engine memory budget and parallel-tool-call limits, (b) UI fluidity tier (animations, list virtualisation), (c) battery strategy (full speed on charger / good battery; saver mode when low), (d) an **accelerator capability registry** used by later local-model features (D38). Re-survey is possible in Settings and runs automatically after an OS update. |
| D37 | Resource use | The app must use the hardware **as fully and as stably as possible**: CPU/RAM/GPU for a fluid UI and for on-device builds (Gradle workers, Kotlin daemon memory tuned from the `DeviceProfile`); the **NPU is reserved for future local helper/local-model features** (D38) — the Claude Code engine itself is a network LLM client and **cannot run on the NPU**; the docs must say so honestly rather than promising "NPU-powered Claude Code". |
| D38 | Local model (later) | A later, optional capability: run a **small local model** on-device (offline helper, summaries, suggestions; possibly a fallback engine for trivial tasks). Implementation must be researched **at build time** (LiteRT / LiteRT-LM, vendor SDKs, llama.cpp — pick by what the survey found), pinned, and dated. **TBD — verify at build time.** No invented model names or sizes. |
| D39 | Skills enforcement | **Build time:** every task in `task-breakdown.md` names at least one skill it will use, and a **new CI checker** verifies that the progress log records the skill actually used — a task without a recorded skill is not "done". **App side:** when the engine executes a task and a matching skill exists, it prefers to use at least one (soft default, visible in the task plan UI). |
| D40 | Plan B ordering | If the real Claude Code engine cannot be made to run on the device: (1) Native glibc-runner + ELF-patch profile on-device (parent §5.2 Profile A), (2) proot Ubuntu profile (Profile B), (3) **remote free backends** (home PC, Oracle Always Free, GitHub Actions — parent D2), and only then (4) **own engine from scratch**: a Kotlin-native agent loop speaking the same `AgentEvent` protocol (no Claude binary), reusing the whole UI/orchestration stack. The own-engine workstream is started **only** after (1)–(2) are documented as failed on the A56, each attempt recorded in the progress log. |
| D41 | Sandbox | The engine may touch: its own app-private storage, downloaded project directories, and **user-granted folders** (SAF persistable grants). Every grant is visible in Settings and revocable at any time. No access to photos, messages, contacts, or other apps. |
| D42 | Backup | Settings screen offers **export**: a versioned file containing projects, settings, provider configs, chat history — **never keys** (`SecretRef`s are exported as opaque references only; a key is re-entered on import). Import restores on a new device. The export dialog states exactly what is included (hard block 5: nothing hidden). |
| D43 | Offline behaviour | If connectivity drops mid-run, the run **pauses automatically and safely**, shows a status, and **resumes on reconnect** with a notification. All network operations must be idempotent/resumable (aligns with the runtime state machine, parent §5.3). |
| D44 | Onboarding | First launch runs a **full German setup wizard**: device survey (D36) → engine install with visible progress → provider choice + key entry (D35) → test message. **Every step is skippable** and can be completed later from Settings. At the end the user lands in the chat. |
| D45 | Acceptance test | "The app works" means **both** journeys pass **on the A56**: **(A)** one sentence → the app builds a small app, tests it, and produces a green branch + open PR on a private test repo, without further hand-holding; **(B)** pick an existing GitHub repo, describe a bug, the app locates and fixes it and presents the diff for review. This extends parent §"One-shot, and what finished means". |
| D46 | Naming | The working title **"Claude Code Android" stays** for repo and app. A rebrand task exists in Phase 6; before any public distribution: own name/logo plus trademark/legal review (Anthropic is a third-party mark; "unofficial, unaffiliated" notice stays). |
| D47 | Monetisation | **Preparation only**: the code keeps a clean seam so paid/premium features (or an Anthropic pitch/open-sourcing later) can be added without rewrites — e.g. feature flags and a licence-check boundary behind an interface. **No paywall, no licence code, no store billing is built now.** |
| D48 | Device identity fix | Records the Exynos-1580 correction of §1 (this row exists so the correction is auditable, not silent). |

---

## 3. Product deltas in detail

### 3.1 Port strategy and Plan B (D30, D40)

- The **primary goal is unchanged and explicit**: a **full-value Claude Code on Android** — the real engine, wired to a friendly UI. The user accepted that this depends on the glibc/proot routes working on this exact device.
- The user's fallback preference is an **own engine built from scratch** (Kotlin-native agent loop, same protocol, same UI), **after** the native attempt and proot are exhausted — not before. Existing partial work (orchestration, protocol, UI) is reused regardless.
- Phase 3's hard stop stays: if the engine does not run on a real device, the build stops and reports — but the stop now carries the **documented Plan B ladder** (D40) in the blocker report, so the operator's decision is a choice between pre-costed options, not research.

### 3.2 UI parity (D31–D33, D46)

- **Trial 1:1 while private.** Structure, navigation model, chat behaviour, visual identity of the official Claude app are mirrored as closely as legally and technically possible. Rationale: the user is a layperson, wants the most familiar experience, and may later diverge ("Fast 1:1" mix with own branding/paid features).
- **Conflict rule with anti-slop:** the parity decision **overrides** "if it looks like a template, rewrite it" for *structure and behaviour*; `docs/03-design/anti-slop-rules.md` continues to govern *craft quality* (spacing, contrast, motion budgets, icon set, four screen states). Banned aesthetics stay banned even in a clone. This resolution must be copied into `docs/03-design/design-tokens.md` and `docs/13-process/ai-usage-policy.md` when the parity tasks land.
- **Release gate (new, goes into `release-checklist.md`):** public distribution requires (a) rebrand to own name/logo, (b) legal/trademark review, (c) "unofficial" notice retained. Until then: private repo only (hard block 3).
- The **left switcher** (Chat / Projects / Claude Code, Claude-app style) is the Phase-6 navigation retrofit; the bottom bar remains until then (D33).

### 3.3 Providers, model picker, free tiers (D34, D35)

- The provider model of parent §6.1 is **kept** (kinds: ANTHROPIC / OPENAI_CHAT / OPENAI_RESPONSES / CUSTOM, `SecretRef`, multiple named keys, connection test). New on top:
  - **Catalog**: local JSON list of curated providers (OpenRouter, Groq, Google AI Studio first; extensible). Each entry: description (German + English), signup URL, pre-filled base URL, kind, free-tier flag, notes on quotas (**quotas are never invented in the catalog — link out, don't state numbers**, consistent with the version/quota rule).
  - **Setup wizard**: step-by-step first-run provider setup, embedded in onboarding (D44) and reachable from Settings.
  - **Free-first defaults**: the catalog's default sort puts free-tier-capable providers/models first; paid models are selectable but the spend meter (parent §6.3) is always visible. The app itself never charges money (hard block 2).
- **Model picker (chat box)**: compact chip above the input — provider/model name + reasoning level; tap opens the full sheet: provider group → model list → reasoning (off/medium/high, per-provider mapping), each model row shows cost hint where the API provides one, otherwise "unknown" (never a guessed price).

### 3.4 Device profiling (D36–D38, D48)

- **One-time survey on first launch** (and re-runnable from Settings; auto re-run after OS update). Persists a `DeviceProfile` in the app database (Room, new table — schema migration task).
- **Measured**: SoC name/parts, core cluster layout, total+available RAM, storage class, GPU, **NPU/AI accelerator** (vendor driver present? NNAPI/LiteRT accelerator delegate available? OpenCL present?), Android/One UI version, refresh rate, thermal status, battery level/charging.
- **Applied to**:
  | Concern | Derived setting |
  |---|---|
  | Engine runtime | memory budget, max parallel tool calls, ring-buffer sizes |
  | On-device builds (the A56 builds this repo in PRoot) | Gradle workers, Kotlin/Gradle daemon heap, test JVM parallelism (`gradle.properties` template written from the profile) |
  | UI fluidity | animation tier (full / reduced / minimal), list page size, blur/shadow tier |
  | Battery | speed profile when charging or > threshold; saver profile when low battery |
  | Accelerator registry | capability booleans consumed by local-model features (D38) — recorded even if unused for now |
- **Honesty rules**: NPU capability is stored as *facts*, never as marketing. The docs state plainly: Claude Code = network LLM; NPU serves only future local helpers (D37/D38). AVF availability is **probed at runtime**; the old "Snapdragon → unavailable" claim is wrong for this device and gets corrected (D48).

### 3.5 Skills enforcement (D39)

- **New CI check** (sibling of `check_typed_errors_at_boundaries.py`): for every task marked done in `task-breakdown.md`, the progress log must record at least one named skill used; violation fails `tools/ci.sh`. Must stay fast on-device.
- **App side**: the engine's task-plan format gains an optional `skills` field; when skills exist for a task, the engine uses ≥ 1 by default and shows which in the plan UI (fits parent D12 and is visible in the transparency log — hard block 5).

### 3.6 Sandbox, backup, offline (D41–D43)

- **Folder grants**: SAF persistable URI grants; a Settings screen lists every grant with a revoke button; the engine's file gateway rejects paths outside [app-private ∪ project dirs ∪ granted]. Typed error (`E-SANDBOX-PATH-DENIED` or matching taxonomy entry) when refused.
- **Export/import**: single versioned JSON (+ blobs) file; content manifest inside the file; keys excluded by construction (export code has no access to plaintext keys — enforced by `SecretRef` discipline). Import is additive and reports conflicts.
- **Offline**: connectivity change → run enters `PAUSED_OFFLINE` (a new, explicit state), UI + notification reflect it; on reconnect, resume; a run can always be cancelled from the paused state. No silent failures, no hidden state (hard block 5).

### 3.7 Onboarding (D44) and acceptance (D45)

- Wizard steps (each skippable, each completable later): **1** device survey with plain-German explanation of what is read and why (privacy: all local, nothing sent) → **2** engine install (Profile A) with per-step progress and cancel → **3** provider setup (catalog + key paste + test) → **4** test message → **5** (optional) GitHub connect. Empty/skip states are first-class (UI rules: four states per screen).
- **Acceptance = D45's two journeys**, run on the A56, recorded in the progress log with transcripts. These join the per-project definition of done as the final gate before "finished".

### 3.8 Monetisation seam (D47)

- One interface (e.g. a licensing/entitlements boundary) plus feature-flag plumbing, so premium features or an Anthropic pitch/open-sourcing can be added later without architectural surgery. Nothing user-visible ships now. Hard block 2 (never spend money) is untouched.

---

## 4. Impact on plan, docs, and tasks

### 4.1 New/amended tasks to add to `task-breakdown.md` (IDs follow the existing scheme; exact numbering at insertion)

| Proposed ID | Task | Phase |
|---|---|---|
| P2-10 | Resync `task_plan.md` with the progress log; fold this addendum's decisions into the breakdown | 2 |
| P4-x | Model picker: compact chip + bottom sheet (provider, model, reasoning) | 4 |
| P4-x | Provider catalog + setup wizard (local JSON, free-first sort, connection test) | 4 |
| P4-x | Onboarding wizard (skippable steps, German-first copy) | 4 |
| P4-x | Sandbox folder grants (SAF grant/revoke UI + engine enforcement + typed error) | 4 |
| P4-x | Backup export/import (versioned, key-free) | 4 |
| P5-x | Connectivity supervision: `PAUSED_OFFLINE` state machine + resume + notifications | 5 |
| P6-x | Navigation retrofit: Claude-style left switcher (Chat / Projects / Claude Code) | 6 |
| P6-x | Rebrand pass + release-checklist legal gate (name, logo, notices) | 6 |
| P6-x | Device-profile-driven tuning: UI tiers + `gradle.properties` template for on-device builds | 6 |
| P7-x | CI checker: skill-usage proof per done task | cross-cutting (land early) |
| P7-x | `DeviceProfile` survey + Room schema migration + Settings re-survey | runtime-adjacent (land with Phase 3 runtime work) |
| P7-x | Accelerator registry + local-model spike (**TBD — verify at build time**) | later/optional |
| P7-x | Monetisation seam (interface + flags only) | 6 |

### 4.2 New docs to add to the 135-file manifest (non-stub when their code lands)

- `docs/06-runtime/device-profiling.md` — the survey, the `DeviceProfile` schema, derived settings, honesty rules for NPU/AVF.
- `docs/06-runtime/local-models.md` — the later local-model capability, **TBD-marked** until researched and pinned.
- `docs/07-integrations/provider-catalog.md` — catalog format, free-tier flag policy, wizard flow.
- `docs/05-features/model-picker.md`, `docs/05-features/onboarding.md`, `docs/05-features/backup-export.md`, `docs/05-features/offline-pause.md`, `docs/05-features/folder-grants.md` — feature specs including failure behaviour (per CLAUDE.md's start-here table).
- `docs/03-design/ui-parity.md` — the D31 trial-clone rules, the anti-slop conflict resolution, and the rebrand/legal release gate.
- Update `CLAUDE.md` (Snapdragon → Exynos correction, pointer to this addendum), `docs/10-build/dependency-versions.md` (any new pins), `docs/13-process/ai-usage-policy.md` (skill rows for every new screen).

### 4.3 Unchanged invariants

- Hard blocks 1–5, the gates (`./gradlew check`, coverage 90/80, koverVerify), layer discipline, typed errors, `SecretRef` discipline, "never invent a version", doc-sync-in-same-commit: all apply to every new task above.

---

## 5. Resume runbook (exact order for the next session)

1. **Commit the pending P2-9 slice** on `task/phase-2-data-and-core`: stage only the touched files (the changed list in §1), include `docs/02-architecture/error-taxonomy.md` in the same commit (doc-sync), message per `docs/13-process/git-strategy.md`, then `git push`.
2. **Resync `task_plan.md`** with the progress log (P2-1…P2-8 done, P2-9 in progress; next step P2-10) — same commit as step 1 or immediately after.
3. **Insert the new tasks** from §4.1 into `task-breakdown.md` with real IDs, dependencies, acceptance lines; name the skill to be used per task (D39). Add the §4.2 docs to the manifest.
4. **Correct the device-fact docs** (Exynos 1580, AVF = probe-TBD) wherever "Snapdragon" appears (`CLAUDE.md` and friends) — one small commit, doc-only.
5. **Finish P2-9** per its acceptance line, run `./gradlew check`, progress-log entry, then proceed to **Phase 3 (Runtime)** — the port attempt that decides whether Plan B's ladder (D40) is ever needed.
6. On any blocker: the §3/§5 format, exact command, full error, attempts, and the D40 ladder in the blocker report; stop there.

---

## 6. Interview Q&A digest (for traceability)

| Round | Asked (plain-German) | Locked answer |
|---|---|---|
| 1 | Priority: inner engine vs outer UI vs existing plan | Plan continues; new wishes inserted as tasks; engine and UI equally good simultaneously; agent decides order (→ D30) |
| 1 | How 1:1 is the Claude-app look | Truly everything 1:1 for now incl. chat, as a trial; later own mix/branding; possible monetisation later; focus first on port + friendly UI (→ D31) |
| 1 | NPU expectations | Use everything; invent a method to involve NPUs broadly; one-time device survey at first start; CPU/RAM/GPU/NPU all best-effort (→ D36/D37) |
| 1 | Which free providers | OpenRouter, Groq, Google AI Studio + many, many more — anything OpenAI/Anthropic-compatible, free-credit promos, "opencode-style" ecosystem breadth (→ D35) |
| 2 | Navigation | Bottom bar now, Claude layout later (→ D32/D33) |
| 2 | Model picker form | Both combined: compact + full sheet (→ D34) |
| 2 | Free-provider onboarding for laypersons | Catalog + guided assistant (→ D35) |
| 2 | Device-check purposes | All four: build speed, UI fluidity, battery balance, accelerator detection (→ D36) |
| 3 | Skill rule strictness | Automatically verified in CI; no proof → task not done (→ D39) |
| 3 | Plan B | Own engine from scratch, but first try native on Android; reuse existing work (→ D40) |
| 3 | Sandbox reach | Own area + user-granted folders, visible and revocable (→ D41) |
| 3 | App name | Keep working title for now (→ D46) |
| 4 | First-start effort | Full wizard, every step skippable (→ D44) |
| 4 | The "it works" moment | Both journeys: idea→mini-app→PR **and** repo-repair flow (→ D45) |
| 4 | Uninstall/device change | Export file, keys excluded (→ D42) |
| 4 | No internet | Auto-pause + resume; later also run a local model offline (→ D43/D38) |
| 4 | Future monetisation | Prepare structure only (→ D47) |

---

## 7. Sources consulted (2026-09-30)

| Claim | Source |
|---|---|
| Galaxy A56 = Exynos 1580, Xclipse 540 (RDNA 3), 8/12 GB RAM, 128/256 GB | Wikipedia "Samsung Galaxy A56 5G"; phonearena spec sheet; Samsung Exynos 1580 product page (NPU "up to 14.7 TOPS") |
| Claude Code ≥ v2.1.113 ships a native glibc Linux binary; breaks Termux/Bionic; routes are glibc-runner or proot; proot is slow; AVF route exists upstream | anthropics/claude-code#50270; Termux community threads; cosyra.com 2026 guide; ferrumclaudepilgrim/claude-code-android |
| AnyClaw = Android app bundling OpenClaw + Codex CLI with a friendly chat/dashboard UI (the user's UX reference) | Google Play listing "AnyClaw: 5-in-1 AI Coding"; openclaw-android-assistant on GitHub |
| NNAPI is legacy; current on-device LLM path is LiteRT / LiteRT-LM with NPU/GPU delegates, vendor SDKs | Android NDK NNAPI guide; Google Developers blog "Building real-world on-device AI with LiteRT and NPU" (2026-04); Google AI Edge "Run LLMs using LiteRT-LM" (2026-09) |
| **TBD — verify at build time**: AVF/pKVM support on Exynos 1580; current LiteRT-LM and glibc-runner versions; current Claude Code binary version + checksums; per-provider free-tier quotas (link out, never state numbers) | — |
