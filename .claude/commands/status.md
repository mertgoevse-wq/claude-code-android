---
description: Honest progress against the build plan
---

# /status

Report where the build actually is. This command reads the repository; it does
not rely on what a previous session believed.

## 1. Read the sources of truth

```bash
head -30 docs/14-build-plan/progress-log.md
git log --oneline -15
git status --short
git branch --show-current
```

## 2. Report, in this shape

**Phase:** `<n> — <name>`, or "between phases".

| Task | State | Evidence |
|---|---|---|
| `P<n>-<id> <short name>` | done / in progress / not started / blocked | the commit SHA, or the test that passes, or the blocker report |

**Gate:** green / red / not run — with the command that proves it.

**Working tree:** clean, or the list of uncommitted paths.

**Unpushed commits:** the count. Anything above zero means an interrupted build
that another machine cannot continue from; that is the first thing to fix.

**Blockers:** the list from the progress log, each with its task ID.

**Next task:** exactly one task ID, from `docs/14-build-plan/task-breakdown.md`.

**Milestone:** which of M0–M8, and what its verification still needs.

## 3. Rules

- **A task is done only if the gate was run and passed.** "Looks implemented"
  is not a state; "the acceptance test passes" is.
- **Red is reported as red.** A status that flatters the build is worse than no
  status, because the next session trusts it.
- If the log and the repository disagree, the repository wins, and the
  correction is said out loud.
- No estimates, no encouragement, no summary of what the project is. Those
  belong in `README.md`. This command answers "where are we and what is next".
