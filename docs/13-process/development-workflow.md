# Development workflow

How work actually happens in this repository, from a fresh clone to a merged change. Written for a person or an agent with no context.

The workflow is deliberately small. A project this size fails not from lack of process but from drift: a doc that no longer matches the code, a design rule that quietly stopped applying, a build that is red because nobody ran the gate. The workflow below is shaped around preventing those three things.

## 1. One-time setup

```bash
git clone https://github.com/mertgoevse-wq/claude-code-android.git
cd claude-code-android

# JDK version is pinned in gradle/libs.versions.toml — use exactly that one.
# Android SDK: the compileSdk/targetSdk from the same file.
# NDK: not required.

./gradlew --version          # confirms the JDK matches the pinned toolchain
./gradlew check              # the full gate; must be green on an empty checkout
```

If `./gradlew check` is not green on a fresh clone, that is a bug in the repository, not in your setup. Stop and report it.

Optional, and required before touching the runtime layer:

```bash
adb devices                  # a real device or an emulator
./gradlew :shared:runtime:connectedDebugAndroidTest
```

## 2. The inner loop

| Step | Command | Time budget |
|---|---|---|
| 1. Pick the smallest task | `docs/14-build-plan/task-breakdown.md` | — |
| 2. Branch | `git switch -c task/<slug>` | — |
| 3. Write the failing test first | — | — |
| 4. Make it pass | `./gradlew :shared:domain:test --tests '*TheThing*'` | < 60 s |
| 5. Make it right | `./gradlew ktlintFormat detekt` | < 60 s |
| 6. Run the gate for the affected modules | `./gradlew :shared:domain:check` | < 3 min |
| 7. Full gate before commit | `./gradlew check` | < 15 min |
| 8. Doc sync | `/doc-sync` | — |
| 9. Commit and push | `git add -p` → `git commit` → `git push` | — |
| 10. Open a PR | `gh pr create` | — |

**A task is not done at step 6.** Step 7 is the gate. A change that passes its own module's tests and fails `./gradlew check` is a failed change, and the most common way to waste an afternoon is to discover that in review.

**Step 9 is not optional and not batched until later.** Every task ends in a commit and a push to its own branch. The operator runs Claude Code with `--dangerously-skip-permissions`, so nothing here relies on a prompt being answered: the loop is enforced by the `Stop` hook in `.claude/settings.json` and by `13-process/git-strategy.md` §6, not by a permission dialog. See `CLAUDE.md` → "Git: commit and push after every step".

## 3. The slow gates, and when they run

| Gate | When | Why not every keystroke |
|---|---|---|
| `./gradlew check` | Every commit | The full suite is the definition of done, but a 15-minute gate is not a feedback loop |
| E2E on emulator | Every PR, and nightly | An emulator boot costs minutes and is flaky under load |
| Screenshot validation | Every PR | Paparazzi diffs are fast, but reviewing baselines is a human activity |
| Performance macrobenchmark | Nightly, and before a release | Needs a stable device and a quiet machine |
| FOSS dependency check | Every PR | Cheap, but not a per-keystroke concern |
| Doc manifest check | Every PR | Cheap, and it is the thing that keeps 135 documents honest |
| Release gates | Per release | `12-delivery/release-checklist.md` |

The configuration cache is on. If a build script reads the environment, the cache is silently disabled and builds get slow; if you see that, fix the script rather than adding a flag.

## 4. Where code goes

Before writing, the destination is decided by `02-architecture/module-map.md`, and the answer is almost always "further down" than instinct suggests.

| If the code… | It goes in | And must not |
|---|---|---|
| Talks about a provider, a path, a clock, a file | `shared/core` | Import anything Android |
| Encodes a rule about the product | `shared/domain` | Know a DAO, a ViewModel, or a database |
| Touches Room, Ktor, or a filesystem | `shared/data` | Be called directly from a ViewModel |
| Runs or supervises a process | `shared/runtime` | Branch on the current execution backend |
| Decides whether something may happen | `shared/orchestration` | Ask the UI for permission |
| Is a screen | `shared/ui` + `androidApp` | Contain business logic |
| Is Android-only | `androidApp` | Duplicate something that belongs in `shared/` |

`tools/check_no_android_imports_in_shared.py` fails the build if `shared/core` or `shared/domain` imports `android.*` or `androidx.*`. This is not negotiable and not fixable with a suppression comment; that is the point of the check.

## 5. When something fails

| Failure | First move | Then |
|---|---|---|
| A test fails after your change | Read the actual assertion, not the test name | Reproduce with `--tests` on the single test; do not run the whole suite to find out why |
| The build does not compile | Read the first error, not the last | Fix it before reading the rest; cascades produce noise |
| A screenshot diff appears | Open both images side by side | Either it is intended — accept the baseline and say why in the commit — or it is not, and the change is wrong |
| Coverage drops below the threshold | Find the uncovered branch you just added | A threshold failure is never fixed by lowering the threshold |
| The doc manifest check fails | `tools/check_doc_manifest.py --list` | A missing doc is written or the manifest in the spec is amended. It is never silenced |
| CI fails on a machine that passes locally | `./tools/ci.sh` locally | Configuration cache, file-system case sensitivity, and locale are the usual three |
| A gate is red and the fix is not obvious | `/fix` | Up to the retry budget in `05-features/retry-and-self-healing.md`, which applies to our own build as much as to the app's runs |

## 6. Definition of working

A change is working when all of these are true. `13-process/definition-of-done.md` is the long form.

1. It compiles and the new behaviour is covered by a test that fails without the change.
2. `./gradlew check` is green.
3. The layer rules hold; nothing was added to a layer that may not know about it.
4. The doc that describes this behaviour is updated **in the same commit**.
5. The commit message says why, not what.
6. The anti-slop rules are respected if any UI is involved, and `13-process/ai-usage-policy.md` names the skill used.
7. No hard block was relaxed, no key is in the diff, and no telemetry appeared.

## 7. Agent-assisted work

When an agent does the work, the following are non-negotiable, and they are enforced by `.claude/settings.json` and the `reviewer` subagent:

| Rule | Why |
|---|---|
| The agent may not push to the default branch | Hard block 4 |
| The agent may not delete anything | Hard block 1 |
| The agent may not open a paid API or a subscription | Hard block 2 |
| The agent must not mark a gate as passing without running it | A red gate reported as green is the worst possible failure of an agent build |
| The agent must record a real blocker in `14-build-plan/progress-log.md` and stop | Guessing past a blocker is how a build produces a plausible, broken app |

An agent that cannot complete a task reports the blocker with the exact error, the file, and the line. It does not approximate.

## Depends on

`13-process/git-strategy.md` · `13-process/code-review.md` · `13-process/definition-of-done.md` · `13-process/claude-code-instructions.md` · `10-build/local-build-and-run.md` · `14-build-plan/task-breakdown.md`
