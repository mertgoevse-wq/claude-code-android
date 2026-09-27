# BOOTSTRAP

The one file a fresh Claude Code session reads to start the build autonomously.
If you were pointed here and you have no other context, this is your order.

Read this, then `CLAUDE.md`, then work. Do not ask permission for anything
that is not a hard block.

---

## 0. Get oriented, once

```bash
head -40 docs/14-build-plan/progress-log.md   # where the build actually is
git log --oneline -5
git status --short
```

Do not re-read what the log already records. Do not re-summarise it. Read it
once, then continue.

## 1. The loop, for every task

1. Read the task in `docs/14-build-plan/task-breakdown.md` — its ID, its
   dependencies, its acceptance line
2. Read the document in `docs/` that owns the behaviour, **including its
   failure behaviour**
3. Write the failing test first
4. Implement the minimum that makes it pass
5. `./gradlew :shared:domain:check` for the module, then `./gradlew check`
6. Update the doc in the same commit
7. **Commit and push.** Not batched until the end of the session

```bash
git add -p
git commit
git push -u origin task/<slug>
gh pr create --title "<commit subject>" --body "…"
```

A fix for a failed task is its own commit. A blocker is committed with the
`progress-log.md` report, and then the build stops.

## 2. What finished looks like

All seven phases green, and then:

- `./gradlew check` green
- a signed release APK built from a clean checkout
- `adb install -r app/build/outputs/apk/release/app-release.apk` onto the
  **Galaxy A56**
- the app launched there, the runtime bootstrap at `READY`, `claude --version`
  running
- a task on a private test repo producing a green branch and an open PR

**A green Gradle output is not a finish line.** The deliverable is an APK that
is installed and running on the device. If the device is not connected, say so,
name the `adb` command, and keep everything pushed — but do not report the
build as finished.

The A56 is a Snapdragon device, so the experimental AVF profile is unavailable
upstream. That is expected, not a defect, and the app must not present it as a
loss.

## 3. Hard blocks — never, in any mode

1. **Never delete.** No file, branch, tag, or repository. Move to a quarantine
   directory and write why.
2. **Never spend money.** No paid API, no subscription, no purchase.
3. **Never make anything public.** Private repositories only.
4. **Never push to the default branch.** Every push is `task/<slug>`.
5. **Never hide anything.** Every command, diff, decision, error, and cost is
   recorded and visible.

The operator runs with `--dangerously-skip-permissions`, so these are enforced
by the hooks in `tools/hook_*.sh` and the deny rules in
`.claude/settings.json`, not by a dialog.

## 4. Turn on the tools the task needs

| Task | Enable |
|---|---|
| Code | `implementer` + `tester` subagents, `test-authoring` skill |
| UI | `designer` subagent, `ui-design` skill, then the specialist from the routing table |
| Runtime bootstrap | `runtime-bootstrap` skill. This area is not improvised |
| Anything in `shared/` | `kmp-shared` skill, before the import |
| Any git operation | `github-safety` skill |
| A document | `docs-authoring` skill |
| A red gate | `/fix` |
| A release | `/release` |

Plugins and MCP servers are additive only. The build must succeed with none.

## 5. Design is not optional

Every screen and the README go through the installed design skills. Search
`design-library` for the specialist matching the screen's problem, start from
`impeccable`, then apply it.

**Banned, no exceptions and no subtle use:** liquid glass, glassmorphism,
neomorphism, neumorphic elevation, brutalism as a style (correct inside the
terminal pane, wrong on app chrome), skeuomorphism. A skill that produces one
has its output **discarded, not softened**.

Every screen gets a row in the table in `docs/13-process/ai-usage-policy.md`.
A screen with no row is a blocker.

## 6. Models

**Opus** writes and optimises the documents, so the contract is legible.
**The executor** implements against them and does not change a contract on its
own initiative. A task that needs a decision goes back to Opus, which writes the
decision down.

Tokens are scarce: do not re-run a green gate, do not read a document you were
not routed to, do not search the repository for something the task named.

## 7. When you cannot finish

Write a blocker into `docs/14-build-plan/progress-log.md` in that file's
format: the command, the verbatim error, each numbered attempt, the analysis
marked as analysis, and what a human must decide. Commit it, push it, and stop.

Three failures with three different causes means the design is wrong. Do not
lower a gate, skip a test, use `--no-verify`, or approximate past a blocker.

Reporting an accurate blocker is a success. Producing a plausible, broken app
is the failure this project is designed to avoid.

## 8. Every step leaves a remote branch

An interrupted session — out of context, out of tokens, out of time — must leave
a branch another model can pick up. That is the whole reason for the commit and
push rule. A step that is not committed leaves no evidence, and hard block 5
says nothing is hidden.
