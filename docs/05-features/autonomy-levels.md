# Autonomy levels

The setting that decides how much the app asks, per project. The most consequential thing a user chooses, which is why it is a button in the composer and not a buried preference.

## The four levels

### `ASK_EVERYTHING`

| Aspect | Behaviour |
|---|---|
| Read, Grep, Glob | Ask |
| Edit, Write | Ask |
| Bash, any command | Ask |
| Network calls | Ask |
| Git operations | Ask |
| MCP tools | Ask |
| The agent's own retries | Ask |

For an unfamiliar repository, a repository with production data, or a first run. Annoying on purpose.

### `ASK_RISKY`

The default for new projects. Read-only and reversible work runs free.

| Runs without asking | Asks first |
|---|---|
| Read, Grep, Glob | Edit, Write |
| List, stat | Bash |
| `git status`, `git diff`, `git log` | Network calls |
| Project verification commands | `git commit` — actually no |
| Subagents | `git push` |
| | `git commit` is automatic; `git push` is automatic **after verification passes** |

| Decision | Reasoning |
|---|---|
| Edits ask | An edit is reversible in git, but a first-time user has not yet seen a diff, and asking once per edit on a 40-file refactor is unusable. |
| Commands ask | A command is the only tool that can do something the other tools cannot. |
| Commit automatic | A local commit is a safety measure, not a publication. It makes the work recoverable, and it happens on a branch. |
| Push automatic after green | The whole point of the product. A push to a private feature branch after tests pass is the deliverable. |
| The hard blocks apply | Unchanged. See below. |

### `AUTO_WITH_CHECKPOINTS`

Nothing asks, except at defined points.

| Runs without asking | Stops at a checkpoint |
|---|---|
| Everything `ASK_RISKY` runs | — |
| Everything a run may do at all | After the plan is produced, before the first push, before a dependency install, before switching the runtime profile, at the end of a failed retry budget |

Checkpoints are visible in the plan as `⊟` markers, and the app announces each one it reaches. A user at this level knows exactly where a run will pause before they start it.

The default checkpoints, in order:

| # | Checkpoint | Why here |
|---|---|---|
| 1 | After the plan | The whole direction is still free to change at no cost |
| 2 | Before installing dependencies | This is where money can leak, in the sense of a slow build and a mutated lockfile |
| 3 | Before the first push | The moment the work becomes visible to others |
| 4 | After a retry budget is exhausted | Before a fifth identical failure burns more money |
| 5 | Before switching the runtime profile | A 2 GB download on a metered connection |

Each is a report-and-continue, not a stop-and-wait. The sheet says what just happened and offers "Weiter" and "Hier anhalten". The run continues by default after 60 seconds, because a checkpoint that requires a tap is an approval prompt with extra steps, and this level was chosen precisely to avoid those. **The 60-second timeout is visible, counts down, and can be changed to "never" in the project settings.**

### `FULL_AUTO`

Nothing asks at all.

| Still never happens | Enforced by |
|---|---|
| Deleting a file, branch, tag, or repository | `HardBlockPolicy` |
| Spending money on any service | `HardBlockPolicy` |
| Making anything public | `HardBlockPolicy` |
| Pushing to the default branch | `PushPolicy` |
| Hiding an action from the log | `LogRepository` is append-only |

The tool is still given a reduced allow-list at this level: destructive Bash patterns are not in the allowed set, because the policy would refuse them anyway, and refusing on every occurrence wastes a turn. The refusal is recorded and the agent continues.

**Selecting `FULL_AUTO` takes a two-step confirm.** The sheet says:

> Claude läuft ohne zu fragen. Du bekommst keine Erlaubnisfragen mehr, nur noch eine Meldung wenn etwas fertig ist.
>
> Gelöscht, bezahlt, veröffentlicht und auf den Hauptzweig hochgeladen wird **nie** — auch in diesem Modus nicht.
>
> Kosten laufen ohne Limit weiter, bis der Lauf fertig ist. Ein Abbruch ist jederzeit möglich.

The second paragraph is repeated because it is the one belief a user could form and the one thing that must not be left unsaid. The third is stated because the app shows cost but does not cap it, per the user's decision.

Lowering the level is one tap during a run, with no confirm, and takes effect at the next tool call. Raising it takes two steps, mid-run as well as in the settings.

## How a level maps to the engine

| Level | Permission mode | Allowed tools | Notes |
|---|---|---|---|
| `ASK_EVERYTHING` | `default` | The full read set, plus everything else in the ask list | The engine asks; the app renders the sheet |
| `ASK_RISKY` | `acceptEdits` | Read, Grep, Glob, Edit, Write, Bash, and the project's verification tools | The app asks about Bash and network itself |
| `AUTO_WITH_CHECKPOINTS` | `bypassPermissions` | The above, minus the destructive patterns | The app gates the checkpoints |
| `FULL_AUTO` | `bypassPermissions` | The same reduced set | The app gates nothing but the hard blocks |

**At `bypassPermissions` the engine asks for nothing**, which is exactly why `HardBlockPolicy` lives in our executor and not in a post-hoc filter. See ADR-006.

## Per project, with a default

| Scope | Behaviour |
|---|---|
| Global default | Applies to new projects. `ASK_RISKY`. |
| Per project | Set on the project, or per run from the composer's `⊕Rechte` button |
| A run override | Lasts for that run only. The sheet says so: "Gilt nur für diesen Lauf." |
| After the run | The project's own level applies again. An override is never sticky. |

Existing projects keep their level when the global default changes. Changing the default is not a mass edit of somebody's working setup.

## The composer's rights chip

| Appearance | Content |
|---|---|
| Location | In the composer action row, left of the cost |
| Label | The level in short form: "Alles", "Fragen", "Checkpoints", "Risiko" |
| Icon | A `shield` glyph, with the level shown by a count of filled bars: 1 for `ASK_EVERYTHING`, 2 for `ASK_RISKY`, 3 for `AUTO_WITH_CHECKPOINTS`, 4 for `FULL_AUTO`. The bars carry the meaning, not the colour. |
| Tap | The four-level sheet, with the current one marked, and each level's consequence in one line |
| During a run | Tapping shows the sheet and notes: "Änderung gilt ab dem nächsten Schritt." |

The chip is always visible during a run. A user should be able to see, at any moment, how autonomous the thing currently working is.

## What the app never does

| Never | Reason |
|---|---|
| Raise the level by itself | The agent cannot ask for more autonomy |
| Raise it as a workaround for a blocked action | A refusal is answered by the agent doing something else, not by the app loosening the rules |
| Apply a lower level retroactively to a running tool call | In-flight calls finish; the change applies at the next boundary |
| Let a level disable the transparency log | There is no such combination |
| Let a level disable verification | The cost and the time are the price of knowing, at every level |
| Hide the level from the user | It is in the composer, the project card, the chat header, and the log |

## The permission sheet's content, precisely

Because this is where the user's actual control lives, the sheet is specified in full.

| Position | Content |
|---|---|
| Title | "Erlaubnis nötig" |
| Line 1, `titleMedium` | The action: "Claude möchte `build.gradle.kts` ändern." |
| Line 2, `bodySmall` | The specific change, as a diff of the affected lines, or the exact command |
| Line 3, `bodySmall` | The consequence: "3 Zeilen werden geändert. Rückgängig über Git jederzeit möglich." / "Kann nicht rückgängig gemacht werden." |
| Line 4, `labelSmall` | Why it is asking: "Modus ASK_RISKY fragt vor Befehlen." |
| Blocked notice | Where a hard block is in the way: "Löschen ist in dieser App immer gesperrt. Dieser Schritt kann nicht erlaubt werden." with the allow buttons disabled and a "Verstanden" that closes the sheet and returns control to the agent. |
| Actions | "Erlauben" (primary), "Immer erlauben" (secondary, this run only), "Ablehnen" (tertiary) |

"Ablehnen" does not end the run. The agent is told it was refused and continues. A refusal ends a run only when the agent genuinely cannot proceed, and then the run ends with a report saying exactly which step was blocked and by which rule.

## Testing

| Test | Type |
|---|---|
| `AutonomyMatrix` | Unit — every level × every tool × every hard block, asserting ask / run / refuse. Exhaustive, no gaps. |
| `HardBlocksAtEveryLevel` | Unit — the five rules, at all four levels, including `FULL_AUTO` |
| `LevelToModeMapping` | Unit — the mapping table, exactly |
| `LevelNeverAutoRaised` | Integration — a run at `ASK_EVERYTHING` stays there for its whole life, whatever the agent requests |
| `LevelOverrideIsOneRun` | E2E — a per-run override applies, then the project's level returns |
| `LevelLowerMidRun` | E2E — lowering during a run takes effect at the next tool call, with no confirm |
| `LevelRaiseNeedsTwoSteps` | UI — the confirm cannot be dismissed by the first action |
| `LevelFullAutoSheetText` | UI — asserts the sheet contains the never-deleted, never-paid sentence, and that the allow buttons are disabled where a hard block applies |
| `LevelChipVisible` | UI — the chip is present in the composer, on the project card, and in the chat header |
| `CheckpointTimeout` | E2E — a checkpoint is shown, counts down, and continues; with the setting at "never", it does not continue |
| `PermissionTimesOutIntoNothing` | E2E — an unanswered permission never becomes an allow and never becomes a deny |
| `PermissionForRunOnly` | E2E — "Immer erlauben" holds for the run and does not survive into the next one |
| `DenyDoesNotEndRun` | E2E — a denied action is recorded, the agent continues, and the run reaches a terminal state on its own |
