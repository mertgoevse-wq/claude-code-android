# Permissions

What the app asks about, what it refuses outright, and why the refusal lives in our layer rather than in a prompt.

## The two categories

| Category | Meaning | Changeable |
|---|---|---|
| **A permission** | Something the app asks about. The user answers yes or no, now. | Yes, per level, per run |
| **A hard block** | Something the app refuses. There is no yes. | **Never** |

Conflating the two is the central design error this file exists to prevent. A block that looks like a permission is a block somebody will try to override, and a permission that is quietly a block is a surprise.

## The five hard blocks

| # | Rule | What is refused | The exact reason shown |
|---|---|---|---|
| 1 | Never delete | `rm`, `rmdir`, `unlink`, `git rm`, `git push --delete`, `git branch -D`, `git tag -d`, a file write that truncates a non-empty file with a delete flag, an app-level delete, a skill uninstall outside the audited path | "Diese Aktion würde etwas löschen. Das ist in dieser App nicht möglich." |
| 2 | Never spend | Any request to a paid API, any subscription flow, any purchase, any upgrade path, any registry that charges, any paid tier | "Diese Aktion würde Geld kosten. Das ist in dieser App nicht möglich." |
| 3 | Never publish | A visibility change to public, a public upload, a release, a package publish, a gist, a "make public" toggle | "Dieses Repository bleibt privat." |
| 4 | Never push to the default branch | Any push whose ref is the default branch | "Auf den Hauptzweig wird nie hochgeladen." |
| 5 | Never hide | Any attempt to disable the log, to skip a log write, to alter a log entry, to run with logging off | "Der Verlauf wird immer aufgezeichnet." |

## Why the block is in our layer

At `FULL_AUTO` the CLI runs with `bypassPermissions` and asks for nothing. So a block expressed as a system prompt is a suggestion, and a suggestion in a 200.000-token context at hour three of an unattended run is a suggestion that has decayed.

The block is therefore a Kotlin function, consulted before any tool dispatch, in the app's own orchestration layer. It cannot be skipped by a permission mode, because the permission mode configures the *engine's* behaviour and this is not the engine's behaviour.

```
AgentEvent → Orchestration → PermissionResolver → HardBlockPolicy → Backend
                                  ↑                     ↓
                            the answer           allow / ask / refuse
```

`HardBlockPolicy` returns one of three things, and there is no fourth:

```kotlin
sealed interface Decision {
    data object Allow : Decision
    data class Ask(val reason: String, val consequence: Consequence) : Decision
    data class Refuse(val rule: HardBlock, val reason: String) : Decision
}
```

## What is refused, precisely

Refusing by pattern is a blocklist, and blocklists are only as good as their enumeration. So the check is **intent-based**, with several signals.

### Deletion

| Signal | Example |
|---|---|
| The command | `rm`, `rmdir`, `unlink`, `shred`, `truncate -s 0` |
| A command chain | `git add -A && git reset --hard` |
| A redirect that truncates | `> existing-file` |
| A tool with a delete parameter | `delete_file`, `remove_dir` |
| A git subcommand | `rm`, `push --delete`, `branch -D`, `tag -d`, `clean`, `reset --hard` |
| The API | Any `GitHubClient` delete method — which does not exist |

| Approach | Detail |
|---|---|
| Parse, do not match | The command is tokenised and the subcommand inspected, so `rm` as an argument to something harmless is not a deletion |
| A shell chain is walked | Every segment of `&&`, `\|\|`, `;`, and a pipeline is inspected, so a deletion hidden behind a conditional is caught |
| Substitution is resolved first | `$(...)` and backticks are expanded and the result inspected, so `rm $VAR` is caught whatever `$VAR` holds. **TBD: a variable holding a deletion cannot be seen at inspection time; the policy also watches the resulting `FileChange` events and refuses a run that produced a deletion, after the fact, with a clear report.** |
| The allowlist side | Where possible the *capability* is absent: `GitCommandBuilder` has no delete path at all, by type |

**The after-the-fact check matters and is honest about its limit.** A sufficiently indirect command could delete a file without the static analysis seeing it. So the run's `FileChange` events are checked as they arrive, and if a file the run had not previously touched disappears, the run stops and reports it. It cannot undo it, and it does not pretend to. This is a documented limitation, not a solved problem.

### Spending

| Signal | Example |
|---|---|
| A known paid domain in a command | An API host with a pricing page |
| An install command from a paid source | A paid registry, a commercial service's installer |
| A payment URL | Any |
| A request the provider's own response rejects as billing | `PROVIDER_BILLING` |

| Approach | Detail |
|---|---|
| There is no path to spend | The app has no card field, no billing integration, and no purchase flow. This block is close to vacuous, and that is the point. |
| A command that would | Refused, with a reason naming what it appears to be doing |
| A paid tool the user genuinely wants | **They do it in the terminal themselves.** The app refusing is a statement about the app, not a claim about the user. The terminal is not blocked, and a person can install anything they like by hand. That is a deliberate gap, and it is the right one: the user can always act for themselves; what the app must not do is take their money without them. |

### Publishing

| Signal | Example |
|---|---|
| A visibility change | Any |
| A release creation | `gh release create`, an API call |
| A package publish | `npm publish`, `cargo publish`, `mcp push` |
| A push to a public remote whose repository is public | — |
| A gist | — |

| Approach | Detail |
|---|---|
| The repository is always created private | There is no code path that creates one publicly |
| The API has no visibility method | Absent by construction |
| A publish command is refused | With the reason |
| A package registry is a real gap | Publishing an npm package is legitimate work. It is refused, and the person can do it in the terminal. The log records the refusal, so the gap is visible rather than hidden. |

### The default branch

| Signal | Approach |
|---|---|
| A push whose ref is the default branch | The branch name is resolved against the remote's actual default, not a guess of `main` or `master`. A repository's default is fetched, not assumed. |
| A push with no ref | Treated as the current branch, and the current branch is resolved the same way |
| A push with `--all` or `--mirror` | Refused: they can touch the default branch |
| The comparison | Case-insensitive, and normalised for `refs/heads/` prefixes |

This is the one block that also has a structural enforcement: `PushPolicy` throws before the git call is constructed. There is no path to the push that does not pass through it.

### Hiding

| Signal | Refused |
|---|---|
| A setting to disable logging | Does not exist in the settings schema. Attempting to construct it is a compile error. |
| A run option to skip a log write | Does not exist |
| An edit to an existing log entry | The repository has no update method |
| A deletion of a log entry | The repository has no delete method |
| `--silent` or an equivalent on a command whose output would be logged | The output is logged anyway, and the attempt is recorded |

The repository API is the enforcement. Two methods that do not exist cannot be called, and a static check asserts the signatures.

## The four levels

Per `05-features/autonomy-levels.md`. The mapping from level to the CLI's permission mode and the tool set:

| Level | CLI mode | Asks about | Runs free |
|---|---|---|---|
| `ASK_EVERYTHING` | `default` | Everything | Nothing |
| `ASK_RISKY` | `acceptEdits` | Commands, network, git publication | Reads, greps, verification commands, subagents |
| `AUTO_WITH_CHECKPOINTS` | `bypassPermissions` | Checkpoints only | Everything permitted |
| `FULL_AUTO` | `bypassPermissions` | Nothing | Everything permitted |

At the two upper levels the engine asks for nothing, so the app's own `Ask` decisions have to come from somewhere else: from the run's own logic, at the checkpoints, per `05-features/planning.md`.

## The permission request

Produced by the resolver, rendered by the sheet in `04-screens/12-terminal.md`'s sibling, `component-library.md`.

| Field | Source |
|---|---|
| The action | The tool name and its target, in plain language |
| The specific change | The diff for an edit, the exact command for a bash call |
| What cannot be undone | From the tool's semantics. An edit in a git project is reversible; a `curl` to an external service is not, and the sheet says which. |
| Why it is asking | The level, and the specific rule that triggered the question |
| The options | Allow once, allow for this run, deny. Always in that order. |

| Rule | Detail |
|---|---|
| It never times out into an answer | It waits indefinitely, with a notification after 30 minutes. Guessing "yes" would be a security failure; guessing "no" would silently change behaviour. |
| "For this run" is session-scoped | Never written to a project setting. A permission given once is a decision about once. |
| A denial does not end the run | The agent is told and continues |
| A block is shown as a block | The allow buttons are disabled and the sheet says which rule is in the way and that it cannot be turned off |
| An unknown tool | Asked about, always, at every level. An unrecognised tool is a permission, never an allow. |
| A tool from an MCP server | Asked about, always, and blocked by the same policy. A third-party tool is not trusted because it came from a configuration file. |

## Interaction with the retry loop

| Situation | Behaviour |
|---|---|
| A fix at `ASK_EVERYTHING` | Asks for each attempt |
| A fix at `ASK_RISKY` | Asks about the command |
| A fix at the upper levels | Runs. The retry counter and the report are the only signals, plus the two notifications: one when it starts retrying, one when it gives up. |
| A fix that would delete | Refused, always |
| A fix that would weaken a check | Refused, always. Per `05-features/retry-and-self-healing.md`, this is detected by watching the diff: a weakened assertion, a deleted test, a skipped test, a disabled lint rule, a relaxed compiler flag, a lowered threshold. |
| A fix touching more than 30 files | Escalated, at every level |

The never-weaken rule deserves emphasis: it is the failure mode a self-healing system actually has. A model asked to make a failing test pass will make the test not fail. Detecting it is a diff inspection, and refusing it is a hard block with a specific message naming what was seen.

## Testing

| Test | Type |
|---|---|
| `Matrix` | Unit — every level × every tool category × every hard block, exhaustively. `Allow`, `Ask`, or `Refuse`, never anything else. |
| `FiveRulesAllLevels` | Unit — each of the five rules refuses at all four levels, including `FULL_AUTO` |
| `NoOverrideParameter` | Static — `HardBlockPolicy` has no parameter, parameter, or escape hatch |
| `DecisionClosedSet` | Unit — the `Decision` type has exactly three cases, asserted |
| `DeletePatterns` | Unit — a corpus of deletion commands, including chains, substitutions, and redirections |
| `DeleteChainWalked` | Unit — `x && rm y` and `x; rm y` and `x | rm y` are all caught |
| `DeleteSubstitutionExpanded` | Unit — `rm $(echo /tmp/x)` is caught after expansion |
| `DeleteAfterTheFact` | E2E — a run that produced a deletion despite the static check stops and reports it, and the report says the deletion happened |
| `GitNoDeletePath` | Static — `GitCommandBuilder` has no delete, clean, reset, or force subcommand, by type |
| `DefaultBranchFetched` | E2E — a repository whose default is `develop` is resolved as `develop`, not guessed as `main` |
| `PushAllRefused` | Unit — `--all` and `--mirror` are refused |
| `PushPolicyBeforeConstruction` | Static — `PushPolicy` runs before the command is built |
| `SpendNoPath` | Static — no card field, no billing API, no purchase flow in the codebase |
| `TerminalNotBlocked` | E2E — a paid install typed in the terminal is not blocked, and the log records the terminal's own content |
| `LogNoUpdateDelete` | Static — the log repository has no update and no delete method |
| `NoSilentFlag` | E2E — `--silent` on a logged command does not remove the log entry, and the attempt is recorded |
| `UnknownToolAlwaysAsks` | Unit — an unrecognised tool is `Ask` at every level |
| `McpToolSamePolicy` | E2E — a destructive tool from an MCP server is refused exactly like a local one |
| `PermissionNeverTimesOut` | E2E — a permission left unanswered stays pending for 24 hours and is never resolved by the app |
| `AlwaysIsRunScoped` | E2E — "always allow" holds for the run and does not survive into the next one |
| `DenyContinuesRun` | E2E — a denial is recorded, the agent continues, and the run reaches a terminal state on its own |
| `BlockShowsAsBlock` | UI — the allow buttons are disabled where a block applies, and the sheet names the rule |
| `NeverWeakenCheck` | E2E — six weakening patterns are each refused with the specific reason |
| `FixFileCountEscalation` | E2E — a fix touching more than 30 files escalates at every level |
| `ReasonIsPlain` | Unit — every refusal reason is one sentence, in plain language, naming the rule |
| `NoExcuseInReason` | Unit — no refusal reason blames the user or implies a mistake |
