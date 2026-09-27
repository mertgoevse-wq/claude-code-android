# CLAUDE.md

Instructions for an agent building this repository. You have no prior context; everything you need is here or in a file named here. Read the file before you change the thing.

## What this project is

An unofficial Android app that runs the real Claude Code engine on a phone, chat-first, with an autonomous loop that plans, edits, tests, fixes its own failures, commits, and opens a pull request. It is independent and unaffiliated with Anthropic. The full contract is `claude-code-android-spec.md` at the repository root — read the section you need before acting.

## Working principles

- **Think before coding.** State your assumptions in the commit body. If a task is ambiguous, present the two readings and pick one explicitly — do not silently choose. If a cheaper path exists, say so before writing the expensive one.
- **Simplicity first.** The minimum that solves the actual task. No speculative abstraction, no unrequested configuration, no "we might need this later".
- **Surgical changes.** Touch only what the task requires. No drive-by refactors, no reformatting a file you were not asked to change, no tidying neighbouring code.
- **Goal-driven execution.** Name how the change is verified — a test, a command — before you implement it, then loop until it passes. "It looks right" is not a verification.

## Start here

| If you are… | Read first |
|---|---|
| Starting a phase | `docs/14-build-plan/phase-plan.md`, then your task in `docs/14-build-plan/task-breakdown.md` |
| Writing code | `docs/02-architecture/module-map.md`, then `docs/02-architecture/layer-contracts.md` |
| Writing a feature | The matching file in `docs/05-features/` — it specifies the failure behaviour too |
| Writing a screen | The matching file in `docs/04-screens/`, then `docs/03-design/design-tokens.md` |
| Reviewing a change | `docs/13-process/code-review.md` |
| Blocked | `docs/13-process/claude-code-instructions.md` §9 |

## Commands

```bash
./gradlew assembleDebug          # build
./gradlew check                  # the gate: lint, detekt, ktlint, all unit and integration tests
./gradlew :shared:domain:check   # one module, for the inner loop
./gradlew koverVerify            # coverage thresholds
./gradlew e2eTest                # the five journeys, on an emulator
./tools/ci.sh                    # everything CI runs, locally
./tools/format.sh                # the formatters
```

A task is not done when it works. It is done when `./gradlew check` is green. Never mark a gate as passing without running it.

The loop is: **read the task → name the verification → write the failing test → make it pass → run the gate → commit → push**.

## Git: commit and push after every step

The operator runs Claude Code with `--dangerously-skip-permissions`, so **nothing in this repository depends on a permission prompt to be safe**. The automation is therefore explicit and unconditional, not advisory:

| After | You must |
|---|---|
| A task in `task-breakdown.md` reaches done | `git add` the files you touched, commit with the message format, push to the current `task/<slug>` branch |
| A task fails and you fixed it | The fix is a separate commit, so the history shows the red build and its repair |
| A gate goes red for a reason you could not fix | Commit the diagnosis and the blocker report in `progress-log.md`, push, then stop |
| A phase completes | Commit, push, open a PR, tick the boxes in `release-checklist.md` |
| A doc changes | In the same commit as the code, and pushed with it |

```bash
git add -p                                  # stage only what the task required
git commit                                  # message format: docs/13-process/git-strategy.md
git push -u origin task/<slug>
```

**Staging.** Stage by path or with `-p`, never `git add -A` / `git add .` on a shared tree. `git status` before every commit; if a file you did not touch appears staged, unstage it and say why in the commit body. `.gitignore` must cover `build/`, `.gradle/`, `local.properties`, `*.jks`, `*.keystore`, and `.claude/settings.local.json`.

**Pushing.** Every push goes to a `task/<slug>` branch. Hard block 4 forbids the default branch. If a push is rejected, fix forward — no force-push to a shared branch, ever. The `task/<slug>` branch itself may be force-pushed only with `--force-with-lease` and only when nobody else has pushed to it.

**Never** commit a secret, a keystore, a `local.properties`, a build output, or a real API key. `scripts/check-no-secrets.sh` runs in the pre-commit hook and in CI; if it fires, the value is rotated, not deleted from the message.

## Skills, plugins, and hooks

The operator grants broad permission, so the automation kit is part of the contract, not an optional extra. Enable what the task needs, from the list in `docs/13-process/claude-code-instructions.md` §10:

| When | Use |
|---|---|
| Any code change | The `implementer` and `tester` subagents, and the `test-authoring` skill |
| Any UI change | The `designer` subagent and the `ui-design` skill; name the skill used in `docs/13-process/ai-usage-policy.md` |
| Anything touching the runtime bootstrap | The `runtime-bootstrap` skill. Do not improvise here |
| Anything touching `shared/` | The `kmp-shared` skill, before writing the import |
| Any commit, branch, or PR | The `github-safety` skill |
| A gate is red | `/fix`, up to the retry budget |
| Docs change | `/doc-sync` |
| A doc is being written | The `docs-authoring` skill |
| A release | The `release` skill and the release checklist |

Plugins and MCP servers are **additive only**: the build must succeed with none configured. A plugin that is required for the build is a defect, and `.mcp.json` stays optional. See `docs/07-integrations/mcp.md`.

**Hooks are configured, not assumed.** `docs/13-process/claude-code-instructions.md` §10 specifies the hooks that must exist in `.claude/settings.json`: format and lint after each write, read the error after each build, capture test output. If a hook is missing, add it — that is a task, not an excuse to skip the check.

## Vibe-coding readiness

This repository is written to be built by an agent with no prior context, and the app it builds is meant to be vibe-coded against: describe an app, and the agent scaffolds, builds, tests, and opens a private PR. Both directions are the same discipline — the docs are the contract, the gate is the verification, and the commit history is the record.

What that requires of you now, while the code does not exist yet:

- Every task in `docs/14-build-plan/task-breakdown.md` is written so it can be picked up cold: ID, dependencies, and a checkable acceptance line. No task says "handle the rest".
- Every behaviour a person would otherwise have to infer is written down, including the failure path.
- The gate is a single command with a documented threshold, so "is it done" is never a judgement call.
- No invented versions, quotas, checksums, or APIs. An honest `TBD — verify at build time` is a valid deliverable; a plausible number is a defect.

## Versions

**Never invent a version number.** Every version is pinned in `gradle/libs.versions.toml` and recorded with its lookup date in `docs/10-build/dependency-versions.md`. If you need a version that is not there: look it up, pin it, record the date, and say where you looked. The same rule applies to free-tier quotas, checksums, API shapes, and licences. A plausible guess is a defect with a citation.

## Stack

| Layer | Choice |
|---|---|
| UI | Kotlin + Jetpack Compose, via Compose Multiplatform for the shared design system |
| Language | Kotlin 2.x, `explicitApi()` on, strict compiler args |
| Architecture | MVVM + unidirectional data flow, `StateFlow` + sealed UI state |
| DI | Koin (multiplatform) |
| Networking | Ktor client |
| Storage | Room for structured data, DataStore for settings |
| Serialisation | kotlinx.serialization |
| Build | Gradle KTS, version catalog, convention plugins, configuration cache on |
| Analysis | ktlint, detekt, lint, Kover |
| Testing | kotlin-test, Turbine, MockK, Robolectric, Compose UI test, Paparazzi |
| Modules | `shared/{core,domain,data,runtime,orchestration,skills,vcs,ui}`, `androidApp`, `iosApp` (stub) |

The repository slug is written `mertgoevse-wq/claude-code-android` throughout these docs because the GitHub organisation does not exist yet. **Replace `mertgoevse-wq` in `README.md` and `THIRD_PARTY_NOTICES.md` before the first push**, and search for it again before the first release.

## Code rules

1. **Layer discipline.** `shared/core` and `shared/domain` import nothing from Android — `tools/check_no_android_imports_in_shared.py` fails the build otherwise, and there is no suppression. No DAO is called from a ViewModel. Feature code never branches on which execution backend or runtime profile is active.
2. **Test first, and a real test.** A test that passes with the change reverted is a blocker. Cover boundaries, error paths, and the empty case.
3. **Coverage is a gate.** ≥ 90 % on `shared/domain`, ≥ 80 % on `shared/data`. Never lower a threshold to make a build pass.
4. **Typed errors.** Every failure gets a code, a user-facing message, a retryability, and a recovery action, from `docs/02-architecture/error-taxonomy.md`. No bare exceptions at a repository boundary.
5. **Structured concurrency.** No `GlobalScope`, no `Thread.sleep`, no `!!` in production code. Cancellation propagates.
6. **Nothing is hidden.** The raw output ring buffer is complete and always visible. Hard block 5.
7. **Comments explain why.** A comment restating the code is deleted. A `TODO` needs an owner and a link.
8. **Secrets by reference.** `SecretRef` everywhere except the Keystore. No key in a log, an export, a diff, or a fixture.

## Hard blocks — never, in any mode, for any user

1. **Never delete** anything. No file deletion, no branch or tag deletion, no repository deletion, no `push --delete`, no force-push to a shared branch, no `--no-verify`. Moving something to a quarantine directory is not deleting it.
2. **Never spend money.** No paid API, no subscription, no purchase, no paid tier.
3. **Never make anything public.** Private repositories only.
4. **Never push to the default branch.** Every push goes to `task/<slug>`. The repository's root commit is the one documented exception, recorded in `13-process/git-strategy.md` §6.
5. **Never hide anything.** Every command, diff, decision, error, and cost is recorded and visible.

These are enforced in `HardBlockPolicy`, in tests, and at the agent permission layer. There is no setting, flag, or autonomy level that turns one off. If you are reasoning about whether a block applies, it applies.

## Documentation obligation

**A behaviour change updates the matching document in the same commit.** A document describing behaviour that no longer exists is a bug. `/doc-sync` exists for exactly this.

`tools/check_doc_manifest.py` runs in CI: all 135 documents must exist and be non-stub. If a document is missing, write it — that is a task, not a reason to guess.

## UI rules

`docs/03-design/anti-slop-rules.md` is binding. The short form:

- No default Material purple, no default Roboto, no stock gradient hero, no card-inside-card-inside-card.
- No emoji as UI icons. One documented icon set.
- Deliberate visual hierarchy per screen, not uniform spacing.
- Motion is purposeful, and state changes are under 300 ms. The only infinite animation is the logo.
- Touch targets ≥ 48 dp, text ≥ 14 sp, contrast AA in both themes.
- Every screen has four states: loading, empty, error, content. An empty state is not optional.
- Every screen has an entry in `docs/13-process/ai-usage-policy.md` naming the design skill used.
- Generated UI is judged by the same rules. If it looks like a template, rewrite it — do not polish it.

## The plan

Seven phases, in order, each ending green or the build stops. Details in `docs/14-build-plan/phase-plan.md`.

| Phase | Goal | Hard stop when |
|---|---|---|
| 0 Foundation | Repo, 135 docs, Gradle, convention plugins, CI, gates, `.claude/` kit | `./gradlew check` green on an empty app |
| 1 Design system | Tokens, theme, primitives, the animated mark | Every token used; both themes pass contrast |
| 2 Data and core | Domain, Room, repositories, Keystore secrets | Coverage met; a key round-trips and never appears in a log |
| 3 Runtime | Claude Code actually runs on the phone | `claude --version` runs and a prompt streams back |
| 4 Chat and projects | The whole chat experience end to end | The primary E2E journey passes on an emulator |
| 5 GitHub, skills, runners | Branch, PR, skills, remote offload | A private test repo yields a green branch and an open PR |
| 6 Verification and self-healing | Verifier, judge, retries, context, self-update | A broken project reaches green within budget |
| 7 Autonomy, polish, delivery | Background execution, polish, accessibility, README, release | Every §22.2 gate green; a signed release APK exists |

**Phase 3 is a hard stop.** If the engine does not run on a real device, the build stops and reports the blocker. There is no mock, no stub, and no "the UI is ready, the binary comes later".

## Definition of done

Per task: it works, a test that fails without the change exists, `./gradlew check` is green, layers hold, error paths are handled, the doc is updated in the same commit, no hard block was relaxed, no secret is in the diff, and the commit says why.

Per phase: every assigned task meets the above, every "done when" clause is met, the progress log is updated, and any deviation is a spec amendment rather than a silent difference.

Per project: all 135 documents exist and match the code, every source file in the manifest exists, all seven phases are green, a fresh clone plus one command produces a signed APK, the README is beautiful and honest, the hard blocks hold — and **a person who has never seen the app can go from install to a merged pull request without being asked a single question**. That last one is the real test. Everything else is a proxy for it.

## When you cannot finish

Write a blocker into `docs/14-build-plan/progress-log.md` in the format in that file, and stop. Include the exact command, the full error, the file and line, each attempt you made, and what a human must decide. Do not approximate past a blocker, do not start the next task, and do not lower a gate to get green.

Reporting an accurate blocker is a success. Producing a plausible, broken app is the failure this project is designed to avoid.

## Working notes

- The repository is in its documentation phase. `docs/14-build-plan/progress-log.md` says where it actually is.
- Nothing here overrides the spec. If this file and `claude-code-android-spec.md` disagree, the spec wins, and the fix is a change to this file.
- If a rule here is ambiguous, the fuller version in `docs/13-process/claude-code-instructions.md` governs.
