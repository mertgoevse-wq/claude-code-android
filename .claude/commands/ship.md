---
description: Commit, branch, push, open a PR. Never the default branch.
---

# /ship

Turn the work in the working tree into a commit on a task branch, pushed, with
a pull request. This is the last step of every task, not an end-of-phase ritual.

## 1. Check what is actually changing

```bash
git status
git diff
```

If the diff touches a file the task did not require, **unstage it**. A commit
that changes three files because all three were open in the editor is not one
logical change.

## 2. Branch check

```bash
git rev-parse --abbrev-ref HEAD
```

`main` means stop. `git switch -c task/<slug>`, commit there, open a PR. Hard
block 4. The root commit was the only documented exception.

## 3. Stage precisely

```bash
git add -p        # or explicit paths
git status --short
```

Never `git add -A` or `git add .`. Generated files CI needs (the version
catalog, Room schemas, Paparazzi baselines) **are** staged. Build output,
keystores, and `local.properties` never are — `.gitignore` should already say
so; if it does not, fix it in this commit.

## 4. Verify before committing

```bash
./gradlew check
bash scripts/check-no-secrets.sh
```

A commit that has not passed the gate does not get pushed. If the gate is red,
use `/fix`.

## 5. Commit

Message format from `docs/13-process/git-strategy.md` section 2:

```
<type>(<scope>): <subject, imperative, <= 72 chars>

<what changed, and why>
<the alternative rejected, if there was one>
<the doc or ADR this is traced to>

Co-Authored-By: Claude <model> <noreply@anthropic.com>
```

Rules: the body says **why**, not what. A doc change is in the same commit. No
secrets, ever.

## 6. Push and open the PR

```bash
git push -u origin task/<slug>
gh pr create --title "<same as the commit subject>" --body "$(cat <<'EOF'
## What
## Why
## What to look at
## Verification — the commands that were actually run, and their results
EOF
)"
```

The PR body lists the **commands that were run**, not intentions.

## 7. Never

`--force` to a shared branch, `--no-verify`, `--amend` after a push, `git reset
--hard`, or any command that discards work. History is append-only.
