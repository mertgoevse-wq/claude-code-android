# Builder instructions

How an agent must behave while building this project, written so that a fresh agent with zero context does the right thing. This is the human-readable companion to `CLAUDE.md` at the repository root and to the `.claude/` kit; the root file is the short version that gets loaded automatically, this is the full contract.

## 1. The one-paragraph version

Build the project described in `claude-code-android-spec.md`, one phase at a time, in the order given. Before writing code, read the docs for the area you are touching. Make each task pass `./gradlew check` before moving on. Update the documentation in the same commit as the behaviour it describes. Never delete anything, never spend money, never make anything public, never push to the default branch, never hide anything. When you cannot finish, write the exact blocker into `docs/14-build-plan/progress-log.md` and stop. Do not guess, and do not fake.

## 2. Before you write anything

| Read | Why |
|---|---|
| `CLAUDE.md` | The pinned versions, the commands, the hard blocks |
| `14-build-plan/phase-plan.md` → your phase | What this phase is and what "done" means for it |
| `14-build-plan/task-breakdown.md` → your task | The acceptance criteria for the specific task |
| `02-architecture/module-map.md` | Which module the code goes in |
| `02-architecture/layer-contracts.md` | What that module may not do |
| The doc for the feature or screen | The behaviour contract, before the code |
| `09-testing/` for the layer you touch | How it gets tested, and to what threshold |

If any of those is missing, write it first. A missing document is not a reason to guess; it is a task.

## 3. Order of work

1. **Docs before code** for a new area. A behaviour nobody wrote down is a behaviour you will get wrong.
2. **Test first** for every unit of behaviour: a failing test, then the implementation.
3. **Smallest correct change.** Not the change you would design if you had a week; the change that makes the test pass and touches nothing else.
4. **One task per commit**, with the doc in the same commit.
5. **Gate, then move on.** A task is not done when it works; it is done when `./gradlew check` is green.

## 4. Hard blocks — no exceptions, no modes, no overrides

| # | Block | What it means in practice |
|---|---|---|
| 1 | **Never delete** | No file deletion, no branch or tag deletion, no repo deletion, no `--force` to a shared branch, no removing a test to make a suite green. If something must go, move it to a quarantine directory and write why |
| 2 | **Never spend money** | No paid API, no subscription, no purchase, no paid tier, no free trial that requires a card. A configuration that would cost the user money is refused |
| 3 | **Never make anything public** | Private repositories only. No publishing an artefact, a gist, or a repo publicly |
| 4 | **Never push to the default branch** | Every push goes to `task/<slug>`. CI, the pre-push hook, and the permission layer each enforce this |
| 5 | **Never hide anything** | Every command, diff, decision, error, and cost is recorded and visible. No `--no-verify`, no suppressed output, no skipped gate reported as passing |

These are enforced in three places on purpose: `HardBlockPolicy` in the code, the Claude Code permission layer, and repository settings. If you find yourself reasoning about whether a block applies, the answer is that it applies.

There is no autonomy level, no environment variable, and no flag that turns one of them off. That is the design.

## 5. Things you must not invent

| Thing | Do this instead |
|---|---|
| A version number | Look it up, pin it in `gradle/libs.versions.toml`, and record the date in `10-build/dependency-versions.md` |
| A free-tier quota | Look it up, record the access date, re-verify at build time |
| An API, flag, or event format | Verify against the real binary or the real documentation. All parsing goes through `AgentEventMapper` with a fixture test |
| A checksum | Compute it or cite the publisher |
| A licence | Look it up, and add it to `THIRD_PARTY_NOTICES.md` |
| A measurement | Measure it, or write `TBD — verify at build time` |
| A file path or API that "should be there" | Check. An invented path is the most common silent failure in a generated codebase |

If the answer cannot be found, the honest output is `TBD — verify at build time` plus a line in the risk register. A plausible guess is a defect with a citation.

## 6. Quality rules

| Rule | Detail |
|---|---|
| Tests first, and meaningful | A test that passes with the change reverted is a blocker. Boundary cases, error paths, and the empty case are not optional |
| Coverage is a gate, not a goal | ≥ 90 % on `shared/domain`, ≥ 80 % on `shared/data`. A threshold is never lowered to make a build pass |
| No `!!`, no `GlobalScope`, no `Thread.sleep` in production code | Coroutines are structured; cancellation propagates |
| Layers are respected | `shared/core` and `shared/domain` import nothing from Android. No DAO in a ViewModel |
| Errors are typed | Every failure gets a code, a user-facing message, a retryability, and a recovery action, from `02-architecture/error-taxonomy.md` |
| Nothing is hidden | A raw output ring buffer, always visible |
| Comments explain why, not what | A comment restating the code is deleted |
| No dead code, no commented-out code, no `TODO` without an owner and a link | Hard block 5 in spirit: it is not hidden, it is clutter that pretends to be intent |

## 7. UI rules

The anti-slop rules in `03-design/anti-slop-rules.md` are binding. The short form:

- No default Material purple. No default Roboto. No stock gradient hero. No card-in-card-in-card.
- No emoji as UI icons. One documented icon set.
- Every screen: deliberate hierarchy, not uniform spacing.
- Motion is purposeful and under 300 ms for state changes. The only infinite animation is the logo.
- Touch targets ≥ 48 dp, text ≥ 14 sp, contrast AA in both themes.
- Every screen has all four states: loading, empty, error, content.
- Every screen names its design skill in `13-process/ai-usage-policy.md`.

Generated UI is judged by the same rules. If it looks like a template, it is rewritten, not polished.

## 8. When a gate fails

1. Read the actual error. Not the summary, not the last line.
2. Reproduce with the narrowest command that shows it.
3. Fix the cause. Do not suppress, do not skip, do not lower a threshold.
4. Re-run the gate.
5. If the same gate fails three times with three different causes, stop and write it up. Three failures means the design is wrong, not the third fix.

The retry budget in `05-features/retry-and-self-healing.md` applies to your own build exactly as it applies to the app's runs.

## 9. When you cannot finish

This is the most important section. A build that stops with an accurate blocker is useful. A build that continues past a blocker produces a plausible, broken app that looks finished.

Write into `docs/14-build-plan/progress-log.md`:

```markdown
### <phase> / <task id> — BLOCKED

**What was attempted:** one paragraph.
**The exact failure:** the command, the full error, the file and line.
**What I tried:** each attempt, numbered, with its result.
**What I believe the cause is:** your best analysis, clearly marked as analysis.
**What would unblock it:** the specific thing a human must decide or provide.
**State left behind:** which files are modified, which tests pass, what is safe to keep.
```

Then stop. Do not start the next task. Do not approximate the missing piece. Do not lower a gate to get green.

A blocker is a legitimate outcome. Reporting one accurately is the correct behaviour, and it is recorded as a success of the build process, not a failure of it.

## 9a. Model routing — two models, two jobs

Splitting the work is a token decision, not a hierarchy.

| Model | Does | Does not |
|---|---|---|
| **Opus** | Optimises the documents: architecture, contracts, screen specs, the design system, the README, and reviews of anything subtle | Write bulk implementation |
| **Sonnet** | Executes against the documents: implements, writes tests, runs gates, commits, pushes | Change a contract on its own initiative |

The rule that makes it work: **Opus makes the documents legible to a weaker
reader, so the executor never has to infer a contract.** A task that needs a
decision goes back to Opus, which writes the decision down. A task that only
needs the decision applied goes to the executor. Optimising a document for the
model that will *read* it is the highest-leverage token move in the project.

Spend tokens deliberately: do not re-run a green gate, do not read a document
you were not routed to, do not search the repository for something the task
named, and do not re-summarise what the progress log already records.

## 9b. One-shot, and the actual finish line

All seven phases run in one autonomous session, in order. The build stops when
the deliverable exists and runs — and the deliverable is an **APK installed and
running on the Galaxy A56**, not a green Gradle output.

Before declaring the build finished:

- `./gradlew assembleDebug` and `./gradlew check` green
- a signed release APK built, then `adb install -r` onto the Galaxy A56
- the app launched there, the bootstrap reaching `READY`, `claude --version`
  running, a headless prompt streaming back
- a task on a private test repo producing a green branch and an open PR

The A56 is a Snapdragon device, so the experimental AVF profile is unavailable
upstream. That is expected and is not reported as a failure.

If the device is unreachable, the correct output is: the state reached, the
exact `adb` command the operator must run, and everything pushed so the work can
be resumed. **Reporting the build as finished with a missing device is hard
block 5.**

Every step is committed and pushed, so a session that runs out of context or
tokens leaves a remote branch another model can continue from. That is the
whole reason the git rule exists.

## 10. The autonomy kit

| File | What it is for |
|---|---|
| `.claude/settings.json` | Permissions: what the agent may always run, and what is denied outright |
| `.claude/commands/phase-start.md` | Begin a phase from the plan, with its preconditions checked |
| `.claude/commands/verify.md` | Run the full quality gate and report honestly |
| `.claude/commands/fix.md` | Read the last failure and fix it, up to the retry budget |
| `.claude/commands/ship.md` | Commit, branch, push, open a PR — never to the default branch |
| `.claude/commands/doc-sync.md` | Update the docs the last change affected |
| `.claude/commands/ui-polish.md` | Run the design skills over the changed screens |
| `.claude/commands/release.md` | Run the release checklist, box by box |
| `.claude/commands/status.md` | Progress against the plan, honestly |
| `.claude/agents/*.md` | `architect`, `implementer`, `tester`, `debugger`, `designer`, `reviewer` |
| `.claude/skills/*.md` | `android-compose`, `kmp-shared`, `runtime-bootstrap`, `ui-design`, `github-safety`, `docs-authoring`, `test-authoring`, `release` |
| `.mcp.json` | Optional, additive only. The build must succeed with no MCP server configured |

The operator runs with `--dangerously-skip-permissions`, so these files are not a hypothetical kit: they are the load-bearing safety layer. The hooks in particular are what enforce the commit-and-push rule and the no-secrets rule, because nothing else will.

The commands are thin wrappers over the rules in this document. If a command and this file disagree, this file wins.

### 10.1 Enabling plugins and skills

The operator grants broad permission, so the agent is expected to **turn on the tools the task needs** rather than working bare-handed:

| Task shape | Enable |
|---|---|
| Code change | `implementer` + `tester` subagents, `test-authoring` skill |
| UI or screen change | `designer` subagent, `ui-design` skill, plus the design skill named in `docs/13-process/ai-usage-policy.md` |
| Runtime bootstrap work | `runtime-bootstrap` skill — non-negotiable, this area is not improvised |
| Anything under `shared/` | `kmp-shared` skill before the import is written |
| Any git operation | `github-safety` skill |
| Writing or changing a document | `docs-authoring` skill |
| A red gate | `/fix`, up to the retry budget |
| A release | `/release`, the `release` skill, and the checklist |

Rules for plugins and MCP servers:

- **Additive only.** The build must succeed with no plugin and no MCP server configured. A plugin the build requires is a defect, not a dependency.
- **Declared.** Every plugin or MCP server in use is written into `docs/07-integrations/mcp.md` and `docs/10-build/dependency-versions.md` in the same commit.
- **Reviewed like code.** A plugin runs inside the agent's loop. It gets the same review as a dependency and the same licence check.
- **A missing hook is a task, not an excuse.** If the `PostToolUse` hook on `./gradlew` is absent, add it in Phase 0.

### 10.2 Commit and push after every step

The operator runs Claude Code with `--dangerously-skip-permissions`, so the git contract cannot be enforced by a permission dialog. It is enforced by the hooks in §10 and by this rule:

**After every task, commit and push.** `git add -p`, a commit in the format from `13-process/git-strategy.md` §2, and `git push` to the current `task/<slug>` branch. A fix for a failed task is its own commit, so the history shows the red build and its repair. A blocker is committed with the `progress-log.md` report, and then the build stops. Nothing is batched until the end of a session.

Staging is by path or `-p`, never `-A`. `git status` before every commit. No `--force-with-lease` bypass, no `--no-verify`, no push to the default branch.

The reason is hard block 5. The commit history is the only durable record of which gate was red, which blocker was hit, and which file was touched by accident. A step that is not committed leaves no evidence.

## 11. The definition of done, for you

You are finished with a task when `13-process/definition-of-done.md` §1 is true for it. You are finished with a phase when §4 is true for it. You are finished with the project when §5 is true — and the real test in §5 item 5 is that a person who has never seen the app can go from install to a merged pull request without being asked a question.

If that is not true, the correct output is a progress log that says exactly where it stops.

## Depends on

`CLAUDE.md` · `13-process/development-workflow.md` · `13-process/code-review.md` · `13-process/definition-of-done.md` · `13-process/ai-usage-policy.md` · `14-build-plan/phase-plan.md` · `14-build-plan/progress-log.md` · `claude-code-android-spec.md` §20–24
