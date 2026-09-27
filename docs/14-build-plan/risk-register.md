# Risk register

Every risk we can name, what we are doing about it, and — the part that is usually missing — **what would tell us it is happening**. A risk with no trigger is a worry, not a register entry.

Scoring: **L**ikelihood and **I**mpact, each 1–5. Exposure is L × I. Anything at 15 or above gets a mitigation with a named owner; anything at 6 or below is watched, not managed.

## Active risks

### R1 — The glibc patch breaks on a future engine release

| Field | Value |
|---|---|
| **Exposure** | L4 × I5 = **20** |
| **Source** | The engine is a glibc-linked Linux binary that we repoint at a patched ELF interpreter. We do not control either side of that. |
| **Mitigation** | Checksum verification before every launch. A launch that fails twice rolls back to the last known-good version. The version checker validates the new binary before swapping it in. The proot profile is a maintained fallback, not a plan B in a drawer. |
| **Trigger** | `claude --version` fails after a version check, twice, on any device |
| **Owner** | Whoever is working on `06-runtime/native-profile.md` |
| **Residual** | Medium. A breaking upstream change is plausible on any release; rollback limits the blast radius to one session, not the app. |

### R2 — No official Android build exists, and one may appear

| Field | Value |
|---|---|
| **Exposure** | L2 × I4 = **8** |
| **Source** | Upstream has no Android build (issue anthropics/claude-code#50270). If one appears, our whole approach may be unnecessary. |
| **Mitigation** | All engine access is behind `ExecutionBackend`. An official build becomes a new backend and a deprecation notice, not a rewrite. The upstream issue is tracked in `01-research/claude-code-runtimes-on-android.md`. |
| **Trigger** | An official Android artefact appears in the vendor's distribution |
| **Owner** | Maintainer |
| **Residual** | Low, and it is a good risk. |

### R3 — proot images are large and slow

| Field | Value |
|---|---|
| **Exposure** | L5 × I3 = **15** |
| **Source** | ~2 GB of image, slow syscalls, long bootstraps |
| **Mitigation** | The profile is opt-in and the cost is shown before the user commits. It is never the default. The native profile is the default and stays. |
| **Trigger** | Bootstrap abandonment rate above 30 % in the bootstrap state machine |
| **Owner** | `06-runtime/proot-profile.md` |
| **Residual** | Medium. It will be slow; we would rather it be honest about that. |

### R4 — Upstream changes flags, output, or events

| Field | Value |
|---|---|
| **Exposure** | L4 × I4 = **16** |
| **Source** | We parse the engine's output. A format change breaks streaming. |
| **Mitigation** | Every parse goes through `AgentEventMapper` with fixture contract tests recorded from real output. A change fails loudly in tests instead of silently in production. Unknown events are surfaced as unknown, not dropped. |
| **Trigger** | A contract test fails after an engine version bump |
| **Owner** | `02-architecture/event-protocol.md` |
| **Residual** | Low, as long as no code bypasses the mapper. The CI rule that nothing parses engine output directly is what keeps it low. |

### R5 — Free-tier cloud quotas change without notice

| Field | Value |
|---|---|
| **Exposure** | L4 × I3 = **12** |
| **Source** | Oracle Always Free, GitHub Actions free minutes, and a home PC are all outside our control |
| **Mitigation** | A live quota check at setup, shown in the UI. Graceful degradation with an honest message. Offload is opt-in; the local path is never taken away. Quotas are verified at build time and carry an access date. |
| **Trigger** | A quota check returns less than the configured minimum |
| **Owner** | `07-integrations/remote-runners.md` |
| **Residual** | Medium. We cannot control someone else's free tier; we can control whether the app pretends otherwise. |

### R6 — GitHub OAuth needs a registered app

| Field | Value |
|---|---|
| **Exposure** | L5 × I2 = **10** |
| **Source** | A one-tap sign-in needs an OAuth or GitHub App registered by a maintainer. A new maintainer cannot do it unilaterally. |
| **Mitigation** | Both flows ship. PAT is the zero-setup path, and the app says exactly how to create one, with a scope checklist. |
| **Trigger** | OAuth fails to initialise at first run |
| **Owner** | `07-integrations/github-auth.md` |
| **Residual** | Low. This is a solved problem with a documented fallback. |

### R7 — Long runs drain the battery

| Field | Value |
|---|---|
| **Exposure** | L5 × I3 = **15** |
| **Source** | An agent running builds on a phone is a CPU and network workload for an hour |
| **Mitigation** | A foreground service with a persistent, honest notification that shows what is running. A wake lock only while a run is active. Remote offload offered. A visible note about battery and time, in the UI, before the first long run — not in the documentation. |
| **Trigger** | A run ends with a battery delta the user finds surprising |
| **Owner** | `05-features/background-execution.md` |
| **Residual** | Medium, and unfixable. A phone is not a server. The product says so. |

### R8 — Visual similarity to the reference app has legal edges

| Field | Value |
|---|---|
| **Exposure** | L3 × I4 = **12** |
| **Source** | D13 asks for a very close visual match to someone else's app |
| **Mitigation** | Our own mark, our own assets, our own illustration set. Unofficial status in the hero, the About screen, the listing, and the README. Everything traceable in `01-research/legal-and-trademark.md`. |
| **Trigger** | Any takedown request, or any asset traceable to the original app appearing in the repository |
| **Owner** | Maintainer |
| **Residual** | Medium. Similarity of *style* is not infringement; similarity of *assets* is. The line is drawn explicitly and the assets are ours. |

### R9 — The documentation set drifts from the code

| Field | Value |
|---|---|
| **Exposure** | L5 × I3 = **15** |
| **Source** | 135 documents is a lot to keep true. The failure mode is subtle: a document that describes behaviour that no longer exists, discovered by a user. |
| **Mitigation** | `tools/check_doc_manifest.py` in CI catches missing and stubbed docs. `/doc-sync` exists for updates. A doc change is required in the same commit as a behaviour change — a review blocker, not a suggestion. `14-build-plan/progress-log.md` is updated every phase. |
| **Trigger** | A review comment, a stale link check, or an agent reporting a doc that contradicts the code |
| **Owner** | Everyone |
| **Residual** | Medium and permanent. This is the cost of the documentation-heavy approach, accepted knowingly. |

### R10 — Generated UI looks generic

| Field | Value |
|---|---|
| **Exposure** | L4 × I3 = **12** |
| **Source** | The most likely visual outcome of producing many screens quickly |
| **Mitigation** | `03-design/anti-slop-rules.md` is binding. Every screen names its design skill in `13-process/ai-usage-policy.md`. A design-review pass per screen in Phase 7. The `reviewer` rejects generic output as a blocker. Screenshot baselines force the comparison to be looked at. |
| **Trigger** | A reviewer calling a screen "template-looking" — which it is |
| **Owner** | `designer` |
| **Residual** | Low-medium, if the passes actually happen. Skipping them makes it certain. |

### R11 — A screen-reader or large-font regression makes the app unusable for someone

| Field | Value |
|---|---|
| **Exposure** | L3 × I4 = **12** |
| **Source** | Custom components are exactly where accessibility is lost: streaming text, the diff viewer, the terminal, the animated mark |
| **Mitigation** | WCAG AA targets in `03-design/accessibility.md`. Labels on every interactive element. A UI test per screen at 200 % font scale. Reduce-motion fallback. The accessibility pass in Phase 7 is a release gate, not a task. |
| **Trigger** | An accessibility finding classified critical |
| **Owner** | `03-design/accessibility.md` |
| **Residual** | Low-medium. The terminal and the streaming view are genuinely hard; they get their own tests. |

### R12 — The iOS readiness claim is untested

| Field | Value |
|---|---|
| **Exposure** | L4 × I2 = **8** |
| **Source** | We say the shared layer is portable to iOS. Nobody has run it on iOS. |
| **Mitigation** | The claim is scoped: a shared layer, a stub, and a CI purity check. Nothing more is promised, in the README or anywhere else. |
| **Trigger** | Someone asking for an iOS build |
| **Owner** | Maintainer |
| **Residual** | Low, because the claim is small. The risk is only in inflating it later. |

### R13 — A user's existing work is lost to a migration or a crash

| Field | Value |
|---|---|
| **Exposure** | L2 × I5 = **10** |
| **Source** | Room migrations, a runtime replacement that fails, an update that goes wrong |
| **Mitigation** | Migrations are tested at every step and the destructive-change rule is explicit. The engine rolls back automatically. The session record is append-only. Auto-save commits after every run, so the *code* is recoverable from git even if the app is not. |
| **Trigger** | Any field-reported data loss |
| **Owner** | `02-architecture/data-migrations.md` |
| **Residual** | Low. The worst case — the database — is recoverable from the git-committed state of the user's project. |

### R14 — The runtime bootstrap is interrupted by a phone call, a reboot, or the OS

| Field | Value |
|---|---|
| **Exposure** | L4 × I3 = **12** |
| **Source** | Android kills background work, reboots, and runs out of memory |
| **Mitigation** | Every bootstrap step is idempotent and resumable. A `BootReceiver` resumes a run that was interrupted by a reboot. The foreground service keeps the process alive. A killed app resumes, never restarts. |
| **Trigger** | A bootstrap that restarts from step 1 instead of resuming |
| **Owner** | `06-runtime/bootstrap-state-machine.md` |
| **Residual** | Low-medium. Tested in E2E, including the deliberate-kill case. |

### R15 — A user installs a skill that behaves maliciously

| Field | Value |
|---|---|
| **Exposure** | L3 × I4 = **12** |
| **Source** | Skills steer the agent. A malicious skill is an injection vector. |
| **Mitigation** | The install diff is always shown. Scoping is explicit. Skills are validated. Uninstall is a move to quarantine, never a delete. The redaction pass and the permission layer sit between skill text and any action. |
| **Trigger** | A skill that requests a permission outside its stated purpose |
| **Owner** | `shared/skills` |
| **Residual** | Medium. This is a structural risk of the feature, mitigated but not removed. |

### R16 — Cost surprises the user

| Field | Value |
|---|---|
| **Exposure** | L3 × I3 = **9** |
| **Source** | The app has no hard spending cap (D23). A long autonomous run on an expensive model can cost real money. |
| **Mitigation** | A soft advisory threshold the user configures and can dismiss. The cost meter is per run, per project, per day, and per month. The meter is visible *during* the run, not only afterwards. |
| **Trigger** | A single run exceeding the advisory threshold |
| **Owner** | `05-features/chat-and-streaming.md` |
| **Residual** | Medium, and it is a deliberate product decision. The user chose display over a cap. |

### R17 — `main` has no server-side protection on the free plan

| Field | Value |
|---|---|
| **Exposure** | L3 × I4 = **12** |
| **Source** | The repository is private, and GitHub gates branch protection and rulesets behind GitHub Pro for private repositories. The API refuses with *"Upgrade to GitHub Pro or make this repository public to enable this feature."* Hard block 2 forbids paying, and hard block 3 forbids making the repository public, so the protection cannot be bought or unlocked. |
| **Mitigation** | Hard block 4 is enforced at the three layers that do not need a paid plan: the pre-push hook, the CI check on the push workflow, and the `PreToolUse` git hook in `.claude/settings.json`. The root-commit exception is written down. Until CI exists, the operator is the enforcement, and the rule is stated in `CLAUDE.md` at the top of every session. |
| **Trigger** | A commit lands on `main` that did not arrive through a PR |
| **Owner** | Maintainer |
| **Residual** | Medium. A paid plan would close it in one click; the project chose not to pay. Revisit only if the maintainer ever decides the cost of Pro is worth the guarantee — that is a decision for a human, and it would be an amendment to hard block 2, not an agent's call. |

## Watched, not managed

| # | Risk | L | I | Why it is not managed |
|---|---|---|---|---|
| W1 | An upstream licence change on the engine | 1 | 4 | The engine is downloaded at runtime, never redistributed. There is nothing to relicence. |
| W2 | A device with an unusual ABI | 2 | 2 | The native profile is arm64. x86 devices are not targeted; the app says so at first run. |
| W3 | A dependency becoming unmaintained | 2 | 3 | Small dependency set, each justified. A yank is a one-line version change. |
| W4 | A reviewer disagreeing with the anti-slop rules | 3 | 1 | The rules are written down. Disagreement is resolved by the rules, not by preference. |

## Assumptions, stated rather than hidden

| # | Assumption | If it turns out false |
|---|---|---|
| A1 | The user has an Anthropic API key and will use one. No subscription login. | The app is unusable for a claude.ai subscriber. This is a deliberate product decision (D3), documented in `00-vision/scope.md` |
| A2 | The user has a Google account for Play, but publishing is not in v1. | F-Droid and the direct APK carry the distribution. Nothing else changes |
| A3 | The target device is the user's own current phone | The performance budgets are tuned for a modern phone, not a budget device from 2019 |
| A4 | The user can install a debug APK by hand | The onboarding flow includes this as a step with a screenshot |
| A5 | The engine continues to be distributed as a `linux-arm64` binary | R1 and R2 apply. The `ExecutionBackend` boundary is what keeps this survivable |

## Review cadence

- After every phase: re-score every risk, and close the ones that no longer apply. A register that only grows is not a register.
- After any field report: add a row or move an existing one's score.
- Before every release: confirm every mitigation is still implemented. A mitigation that was quietly removed is a new risk, at full exposure.

## Depends on

`14-build-plan/phase-plan.md` · `14-build-plan/milestones.md` · `14-build-plan/progress-log.md` · `11-operations/security-threat-model.md` · `claude-code-android-spec.md` §23
