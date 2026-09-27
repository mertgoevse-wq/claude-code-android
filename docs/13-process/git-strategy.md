# Git strategy

The rules for how this repository moves. Short, because a strategy that needs a paragraph to explain is a strategy nobody follows.

## 1. Branches

| Branch | Lifetime | Rule |
|---|---|---|
| `main` | Forever | Protected. Receives commits only through a merged, reviewed PR. **Never pushed to directly, by anyone, including an agent with a valid token** |
| `develop` | Optional | Not used. One long-lived integration branch alongside `main` buys a second thing to keep in sync and buys nothing for a project of this size |
| `task/<slug>` | Days | One per task from `14-build-plan/task-breakdown.md`. Created from `main`, merged into `main` |
| `fix/<slug>` | Hours | Same shape, for a defect found after a merge |
| `release/<version>` | One day | Cut from `main` when the release checklist is about to be run. Exists so a release is not assembled from a moving target |

Rules:

- Branch names are kebab-case, lowercase, no ticket numbers unless a tracker is ever adopted.
- A branch lives as long as the task. A branch that is three weeks old is a branch that will conflict badly; split the work.
- **Branches are never deleted.** Hard block 1 applies to branches as much as to files. A merged branch is left in place and labelled.
- Tags are never deleted, and never moved. A tag that must change is a new tag.

## 2. Commits

Format: Conventional Commits, with a body that answers *why*.

```
<type>(<scope>): <subject in the imperative, ≤ 72 chars>

<what changed and, more importantly, why it changed>
<the alternative that was rejected, if there was one>
<the ticket, the ADR, or the doc this is traced to>
```

| Type | Use for |
|---|---|
| `feat` | A new behaviour a user can observe |
| `fix` | A defect fix |
| `refactor` | No behaviour change. If the diff is not readable as "no behaviour change", it is not a refactor |
| `perf` | A measured improvement, with the measurement in the body |
| `test` | Tests only |
| `docs` | Documentation only |
| `build` | Gradle, dependencies, toolchain |
| `ci` | Workflow changes |
| `chore` | Everything else that is not worth a type of its own |

Rules that are enforced in review, not by a hook:

- **The subject says why when the why is not obvious.** "fix: handle empty stream" is what. "fix: don't drop the final tool call when the stream closes before done" is why.
- **One logical change per commit.** A commit that touches the runtime and the README because both were on your screen is two commits.
- **No "fixup!" or "squash!" commits** on a shared branch; use `--amend` locally before pushing.
- **No secrets, ever.** `scripts/check-no-secrets.sh` runs in the pre-commit hook and in CI. A secret that reaches history is a rotation, not a `git push --force`.
- **Generated files are committed** (version catalog, Room schemas, Paparazzi baselines) because CI and a fresh clone must see the same files we see.

## 3. Pull requests

| Rule | Detail |
|---|---|
| Size | One task. If a PR touches more than ~400 lines of non-test, non-generated code, it is split |
| Title | The same format as the commit, because it becomes the changelog entry |
| Body | What changed, why, how it was verified, what a reviewer should look at hardest |
| Gates | All CI gates green before merge. No exceptions, including "it's a doc change" — the doc manifest check is the cheapest gate we have and it catches real drift |
| Review | One approving review for a normal change. Two for a change touching `shared/runtime`, `shared/orchestration`, secrets, or the permission layer |
| Merge | Squash for a branch with a messy history; rebase-merge for a clean one. Never a merge commit that produces a "Merge branch" message in `main`'s log with no information in it |
| Branch deletion | Not automatic, because of hard block 1. GitHub's "delete branch" button is disabled in repository settings |

### The reviewer contract

A reviewer reads, in this order:

1. The description — is this the right task?
2. The doc change, if any — is the behaviour described accurately?
3. The tests — would these fail without the change?
4. The implementation — the last 20 % is where the risk is

A review comment must say one of: **blocker** (must change before merge), **should** (my preference, your call), or **nit** (optional, ignore freely). Unlabelled comments are noise and are treated as noise.

## 4. Version bumps

| Change | Bump | Who |
|---|---|---|
| A new user-visible feature | MINOR | Release checklist |
| A bug fix | PATCH | Release checklist |
| A change to a dependency, the toolchain, or the runtime bootstrap | MINOR | Release checklist, with a changelog note about the upgrade path |
| A hard block added or tightened | MINOR, and a highlight in the changelog | Release checklist |
| A hard block removed or relaxed | **Requires a spec amendment.** Not a maintainer decision | Nobody, without an amendment |

`version.txt` is edited in exactly one commit per release, in the release PR, and nothing else touches it.

## 5. Hard blocks, in git terms

These are enforced in three places — `HardBlockPolicy`, the agent permission layer, and repository settings — because a rule enforced in one place is a rule that gets bypassed.

| # | Rule | Git-level meaning |
|---|---|---|
| 1 | Never delete | No `rm` of a tracked file without a documented reason in the commit body; no branch or tag deletion; no `git push --delete`; no `gh repo delete`; no force-push to a shared branch |
| 2 | Never spend money | No paid API key, no subscription, no paid tier, no purchase. A configuration that would cost money is refused by the app and must not be committed |
| 3 | Never make anything public | Private repositories only. A commit that changes a repo to public, or a release that publishes an artefact publicly, is a blocker requiring an explicit human decision |
| 4 | Never push to the default branch | Every push goes to a task branch. CI enforces it; the pre-push hook enforces it locally; the permission layer denies the command to an agent. The repository's root commit is the single documented exception, recorded in §6 |
| 5 | Never hide anything | No squashed-away debugging, no deleted log lines, no `--no-verify`, no skipped test turned green by deletion. A failing test is left failing and reported |

### Force-push

`--force` is allowed **only** to a `task/<slug>` branch that nobody else has pushed to, and never with `--force-with-lease` disabled. `--force` to `main`, to a release branch, or to a tag is a hard block. `--no-verify` is a hard block everywhere: if a hook is wrong, the hook is fixed in its own commit.

## 6. Automation under `--dangerously-skip-permissions`

The operator runs Claude Code with `--dangerously-skip-permissions`. Nothing in this repository may therefore depend on a permission prompt for safety, and the git contract is enforced by hooks and CI rather than by a dialog.

### What must happen after every step

| Event | Required action |
|---|---|
| A task from `14-build-plan/task-breakdown.md` is done | `git add -p`, commit, push to `task/<slug>` |
| A task failed and was fixed | A separate commit, so the history shows the red build and its repair |
| A gate is red and the cause is unknown | Commit the diagnosis plus the `progress-log.md` blocker, push, stop |
| A phase completes | Commit, push, open a PR, tick the release checklist boxes that apply |
| A document changes | In the same commit as the behaviour it describes, pushed with it |

Nothing is batched "until the end of the session". A build that is interrupted must leave a remote branch that another machine — or another agent — can pick up and continue from.

### Staging rules

- Stage by path or with `git add -p`. **Never** `git add -A` or `git add .` on a shared tree.
- Run `git status` before every commit. If a file you did not touch is staged, unstage it and say why in the commit body.
- `.gitignore` covers `build/`, `.gradle/`, `local.properties`, `*.jks`, `*.keystore`, `.claude/settings.local.json`, `*.log`, and exported diagnostics bundles.
- Generated files that CI needs (version catalog, Room schemas, Paparazzi baselines) **are** committed.

### Push rules

- Every push targets `task/<slug>`. The default branch is never a push target (hard block 4).
- **The root commit is the one documented exception.** A repository cannot have a root commit on a branch that does not exist, so the initial push of `main` happens once, at `git init`. It is recorded in the commit body and in `14-build-plan/progress-log.md`. Nothing is ever pushed to `main` again.
- `--force` is used only on your own `task/<slug>` branch, only with `--force-with-lease`, and only when nobody else has pushed to it. Never on `main`, a release branch, or a tag.
- `--no-verify` is a hard block everywhere. If a hook is wrong, the hook is fixed in its own commit.
- A rejected push is fixed forward. History is append-only; that is the point.

### The hooks that enforce it

`docs/13-process/claude-code-instructions.md` §10 specifies the hook set. The git-relevant entries:

| Hook | Trigger | Action |
|---|---|---|
| `PreToolUse` on `Write`/`Edit` | any file written | Run the formatter and the linter fixer |
| `PostToolUse` on `Bash` matching `./gradlew` | build or gate run | On failure, read the error and fix it before continuing; on success, do not re-run |
| `PreToolUse` on `Bash` matching `git (push\|commit)` | any git write | `check-no-secrets.sh`, branch check against the default branch, `--no-verify` and `--force` rejection |
| `Stop` | the turn ends | The working tree must be clean, or the uncommitted work staged with an explicit reason in the commit body |
| `SessionStart` | a session begins | Read `progress-log.md` and report where the build actually is |

These hooks are what make "commit and push after every step" true rather than aspirational. A hook that is missing is a task in Phase 0, not a reason to skip the step.

### A gap worth stating plainly

GitHub gates branch protection on private repositories behind a paid plan, and hard blocks 2 and 3 mean the repository stays private and unpaid. So **`main` has no server-side protection**, and hard block 4 is enforced only by the pre-push hook, the CI check, and the agent's git hook.

That is a real reduction in defence, recorded as R17 in `14-build-plan/risk-register.md` rather than glossed over. The compensating control is that a direct push to `main` fails review: the change would arrive without the doc, the test, and the gate that every other change has to pass.

### Why this is a rule and not a preference

With permissions skipped, the agent has the same reach a human operator has. The commit history is the only durable record of what happened: which gate was red, which blocker was hit, which file was touched by accident. A step that is not committed leaves no evidence, and hard block 5 says nothing is hidden.

## 7. Tags and releases

- A tag is `v<versionName>`, annotated, signed with the release key.
- A tag is created once the release checklist is green, and never moved afterwards.
- Release notes are generated from the merged commit subjects, then **edited by a human or an agent that reads the diffs** — because generated changelogs say "fix: update stuff", and this project's changelog is part of its quality.
- The changelog lands in `15-appendix/changelog.md` and in the GitHub release body, identically.

## Depends on

`13-process/development-workflow.md` · `13-process/code-review.md` · `12-delivery/apk-distribution.md` · `10-build/signing-and-keystores.md` · `claude-code-android-spec.md` §22.1
