---
name: github-safety
description: Branch, commit, push, and PR rules, plus the five hard blocks as git behaviour. Use before any git write, and whenever a change is ready to ship. The operator runs with --dangerously-skip-permissions, so these rules are enforced by hooks, not by prompts.
---

# github-safety

The operator runs Claude Code with `--dangerously-skip-permissions`. Nothing
here may depend on a permission dialog, so the hooks in
`.claude/settings.json` are the enforcement and this skill is the reasoning.

## The five hard blocks, as git behaviour

| # | Block | In practice |
|---|---|---|
| 1 | **Never delete** | No file deletion, no `git push --delete`, no `git branch -D`, no `gh repo delete`, no `git clean -f`, no `--mirror`. Move to a quarantine directory and write why |
| 2 | **Never spend money** | No paid API, no subscription, no purchase, no paid tier. A configuration that would cost the user money is refused |
| 3 | **Never make anything public** | Private repositories only. No public artefacts, no public gists |
| 4 | **Never push to the default branch** | Every push targets `task/<slug>`. The root commit is the one documented exception |
| 5 | **Never hide anything** | No `--no-verify`, no skipped gate reported as passing, no suppressed output, no deleted test made green by deletion |

## The commit-and-push contract

**After every task: commit and push.** Not batched until the end of a session.
An interrupted build must leave a remote branch another machine — or another
model — can continue from.

```bash
git status                      # what is actually changing
git add -p                      # or explicit paths, never -A
./gradlew check                 # the gate, before the commit
bash scripts/check-no-secrets.sh
git commit                      # message format below
git push -u origin task/<slug>
gh pr create --title "..." --body "..."
```

- A **fix for a failed task is its own commit**, so the history shows the red
  build and the repair as two facts.
- A **blocker is committed** with the `progress-log.md` report, and then the
  build stops.
- `git status` before every commit. An unstaged file you did not touch goes back
  out, and the reason goes in the commit body.

## Message format

```
<type>(<scope>): <subject, imperative, <= 72 chars>

<what changed, and why>
<the alternative rejected, if there was one>
<the doc or ADR this traces to>

Co-Authored-By: Claude <model> <noreply@anthropic.com>
```

Types: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore`.
The body says **why**. A commit whose subject is "update stuff" is not finished.

## Never

- `git add -A`, `git add .`
- `git commit --no-verify`
- `git commit --amend` after a push
- `git reset --hard`, `git checkout .`, `git clean -f`
- `--force` to `main`, a release branch, or a tag. `--force-with-lease` on your
  own `task/<slug>` branch only, and only when nobody else has pushed to it
- Deleting a branch after merge. Hard block 1
- Making a repository public

## Branches

`task/<slug>` for a task, `fix/<slug>` for a defect, `release/<version>` cut
from `main` when the checklist is about to run. Kebab-case, lowercase. Created
from `main`, merged into `main` through a reviewed PR.

## The known gap

`main` has **no server-side branch protection**: GitHub gates it behind a paid
plan for private repositories, and hard blocks 2 and 3 forbid paying and forbid
going public. Hard block 4 therefore rests on the pre-push hook, the CI check,
and this skill. Recorded as R17 in `docs/14-build-plan/risk-register.md`. Do
not pretend the server enforces it.

## Before you push

```bash
./gradlew check
bash scripts/check-no-secrets.sh
python3 tools/check_doc_manifest.py
git rev-parse --abbrev-ref HEAD   # must not be main
```

If any of these is red, `/fix` first. If a gate cannot run, say so and name
what a human must do. Do not mark it passing.
