---
description: Begin a phase from the build plan after checking its preconditions
argument-hint: <phase-number>
---

# /phase-start

Start Phase `$ARGUMENTS` of `docs/14-build-plan/phase-plan.md`.

## 1. Read, in this order

1. `docs/14-build-plan/phase-plan.md` — the phase, its entry conditions, its exit criteria
2. `docs/14-build-plan/task-breakdown.md` — every task assigned to it, with IDs, dependencies, acceptance
3. `docs/14-build-plan/dependency-graph.md` — what in this phase can run in parallel
4. `docs/14-build-plan/risk-register.md` — the risks that this phase is exposed to
5. `docs/14-build-plan/progress-log.md` — where the build actually is

## 2. Verify the entry conditions

Run them. Do not assume them. If an entry condition is unmet, say which one and
stop; a phase started on a false entry condition produces failures that look
like the phase's fault.

## 3. Create the branch

```bash
git switch -c task/phase-<N>-<slug>
```

Never commit to `main`. Hard block 4.

## 4. Execute task by task

For each task, in dependency order:

1. Read the task's own document in `docs/` before writing anything
2. Write the failing test first
3. Implement the minimum that makes it pass
4. Run the narrow gate for the module: `./gradlew :shared:domain:check`
5. Run the full gate: `./gradlew check`
6. Update the doc in the same commit
7. Commit and push — `/ship`

Do not batch commits "until the end of the phase". An interrupted build must
leave a remote branch another machine can continue from.

## 5. Parallel work

Tasks listed as parallel in the dependency graph go to subagents: `implementer`
for code, `tester` for tests, `designer` for UI. Two agents must not edit the
same file; if the graph says they would, run them sequentially.

## 6. Report at the end of the phase

- Every task ID reached done
- Every "done when" clause, with the command that proved it
- What was skipped, and why — an empty "skipped" list is suspicious, check it
- Progress-log entry, committed and pushed
- Milestone status from `docs/14-build-plan/milestones.md`
