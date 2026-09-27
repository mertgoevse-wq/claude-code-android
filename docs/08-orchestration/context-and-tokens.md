# Context and token budgeting

A context window is finite. A two-hour run fills it. What happens when it fills decides whether a task completes or quietly degrades.

## The problem, concretely

A long coding run accumulates: the task, the plan, every tool call, every tool result, every file read, every build log, and every message. A single `gradlew test` can produce 40.000 tokens of output. Three of those and the window is gone.

What the engine does when the window fills is context compaction: it summarises the conversation so far and continues with the summary. That works, and it loses detail. The question for this app is when to trigger it, and what the user sees.

## What the app knows

| Knows | Does not know |
|---|---|
| The model's context limit, from the bundled table or the user's metadata | How the engine counts tokens for a given model |
| An estimate of the tokens used so far, from the usage events | The exact token count of a tool result before it is sent |
| The size of each tool output, in bytes | How many tokens those bytes become for this model |
| A rough bytes-to-tokens ratio, ~3.5 for code, ~4 for prose | Whether the engine's prompt is longer than we think |
| The model's price | The current price of a model not in the table |

**The app's context tracking is an estimate, and it is labelled as one.** A bar showing "72 %" that is really 62 % would be worse than no bar, because a person would trust it. The bar's tooltip says: "Geschätzt aus der Ausgabemenge. Der genaue Wert kommt vom Modell."

## The estimate

```
estimatedTokens = sum(inputTokens from usage events)
                + sum(outputTokens from usage events)
                + estimatedTokensFromPendingToolOutput(pendingBytes)
```

| Term | How |
|---|---|
| Reported usage | Taken as-is from the provider |
| Pending tool output | Bytes since the last usage event, at 3.5 bytes per token for code and 4 for prose, detected by content |
| The engine's own prompt | A fixed estimate per model from the table, because the system prompt and tool definitions are substantial and constant |
| Attached files | Their real size at the same ratio |

| Why an estimate | The engine does not report a context usage figure, so anything precise would be invented. |
| Why show it anyway | A person watching a long run needs to know it is approaching a limit. A bar that says "estimate" and is roughly right is useful. A missing bar is not. |
| The label | Always. In the tooltip, in the cost sheet, and in the log. |

## Thresholds

| Level | Share of the limit | What happens | What the user sees |
|---|---|---|---|
| Normal | < 70 % | Nothing | The bar is invisible |
| Warning | 70–90 % | Nothing automatic | The bar appears in `warning`, and the composer's `⊕Modell` chip gets a subtle dot |
| High | 90–95 % | A compaction **offer** in the notification and the run header | "Kontext fast voll (92 %). Jetzt verdichten?" with an action |
| Critical | > 95 % | A compaction is **prepared**, and the offer is repeated | The same, with the countdown |
| Full | 100 % | The engine compacts on its own. The app does nothing, because there is nothing to do. | The bar is `danger` and the run continues |

**The app never compacts without the user's knowledge, and never silently below 90 %.** Compaction loses detail. A person whose task depends on a detail that compaction dropped should know that it happened, and should be able to read the summary before it is used.

## Compaction

| Aspect | Behaviour |
|---|---|
| What it is | The engine summarising the conversation so far, then continuing with the summary |
| Triggered by the user | A "Verdichten" action in the run header, and in the notification above 90 % |
| Triggered by the app | Never automatically below 90 %. Above 95 % the offer repeats, and after 5 minutes of the offer being unanswered, the app compacts and says so loudly. A run that stalls on an unanswered offer is worse than a run that compacts. |
| The summary | Shown, expandable, in the transcript, marked "Verdichtung". A person can read what the engine decided to keep. |
| What is preserved explicitly | The plan, the file changes so far, the verification results so far, the pending messages, the permission decisions. These are in the summary text, because losing a plan is worse than losing a build log. |
| What is lost | Tool output detail, the exact wording of earlier messages |
| In the log | A `STATE` entry, with the token counts before and after and the summary's size |
| Cost | The compaction is a model call, and its cost is added to the run. It is not free, and the app shows that. |

### The explicit preservation list

The app injects a short structured block into the compaction request, so the things that must survive do:

```
Behalte unbedingt:
- Den Plan und den Zustand jedes Schritts
- Die bis jetzt geänderten Dateien
- Die Ergebnisse der bisherigen Prüfungen
- Offene Erlaubnisfragen
- Die Kosten und das Modell
```

This is a small intervention with a large effect. The default compaction keeps what is conversationally salient; the plan's step states are not conversational, so without this they are the first thing lost.

## Context growth over a run

What actually fills a window, and what the app can do about each.

| Contributor | Size | The app's lever |
|---|---|---|
| The engine's system prompt and tool definitions | 10–20 k tokens, fixed | None. Inherent. |
| The task and the plan | 1–3 k | The plan is capped at 15 steps for display, and the injected plan is capped at 20 steps |
| Tool results: build logs | Up to 40 k each | **The preview cap is 2 k, but the full output goes to the engine.** A cap here would mean the engine cannot see the real error. The lever is the app prompting the agent to redirect long output to a file and reference the path, which the CLAUDE.md template instructs. |
| Tool results: file reads | Up to 20 k each | None. The engine needs the file. |
| Messages | 1–5 k each | The user is writing these. |
| Subagent output | Varies | Forwarded, per the CLI flag. A lever would be not forwarding, which loses information. Not done. |

**The build log is the real problem, and the honest answer is not to truncate it.** Truncating an error message produces a run that fails for a reason nobody can see, which is worse than a run that runs out of context and compacts. So the app does not truncate, and instead:

1. The project's `CLAUDE.md` template instructs the agent to redirect long command output to a file and reference the path.
2. The planner is told to prefer a targeted command over a full build when diagnosing.
3. Compaction is offered early enough that the loss is graceful.
4. The user can trigger it at any time.

## Per-model limits

| Model class | Context | Notes |
|---|---|---|
| Current frontier | 200 k | The default. |
| A long-context variant | 1 M | Where the provider offers one, and the app's estimate is calibrated for the standard figure |
| A local small-context model | 8–32 k | Real on a self-hosted runner. The warning threshold becomes critical much earlier, and the bar's scale is correct because it uses the real limit. |
| Unknown | 128 k assumed | Shown as "angenommen", not as a fact |

**An unknown limit is an assumption and is labelled.** Assuming 128 k and saying so is useful; assuming it silently is not.

## Cost interaction

| Concern | Behaviour |
|---|---|
| Compaction costs tokens | Added to the run's `CostRecord`, and visible in the cost sheet |
| A cheaper model for compaction | **Not done.** Switching models changes the answer's provenance, per `05-features/model-selection.md`. |
| The cost of the whole run | Unaffected by the budget feature. The app shows cost and never caps it, per the user's decision. |
| A long run's cost | Shown per run, per project, per day, and per month, per `06-integrations/providers.md` |
| An advisory threshold | A per-project soft warning, default off. A warning, never a stop. |

## Overflow behaviour

What actually happens when the window is full, in order.

| Step | Behaviour |
|---|---|
| 1 | The engine compacts on its own, or the app compacts after 5 minutes of an unanswered offer |
| 2 | The compaction is shown in the transcript and the log |
| 3 | The run continues with the summary |
| 4 | If the plan's states were lost despite the injection, the app restores them from its own database into the next turn. The app has the plan; the engine does not need to remember it. |
| 5 | If the window fills again within a few turns, the app offers to start a fresh conversation with a structured handoff: the task, the plan's states, the files changed, the verification results, and the summary. Explicitly, and with a confirm. |
| 6 | If the run cannot proceed, it fails with a specific error: "Der Kontext ist voll und die Verdichtung reicht nicht mehr. Lauf mit Übergabe fortsetzen?" |

**The app can restore the plan from its own database, which is a genuine advantage over a bare terminal.** The state a user cares about most is not lost by compaction because the app never trusted the context to be the only copy.

## The context bar

| Property | Value |
|---|---|
| Where | Above the composer's action row, 2 dp |
| When | Above 70 % |
| Fill | The estimated share of the limit |
| Colour | `textTertiary` below 70, `warning` at 70–90, `danger` above 90 |
| With a label | "72 %" at `labelSmall`, so it is not colour alone |
| Tapping | The context sheet: the estimate, the model, the limit, the assumption if unknown, the compaction action, and the cost so far |
| Never | On a lock-screen-relevant surface. A bar in the notification would be noise. |
| The tooltip | "Geschätzt aus der Ausgabemenge." Always. |

## What the app does not do

| Not | Reason |
|---|---|
| Truncate a tool result to save context | It produces a failure nobody can diagnose |
| Summarise tool results itself | The app is not a model. A hand-rolled summariser would be worse than the engine's compaction. |
| Switch models to save context | Provenance, per `05-features/model-selection.md` |
| Cap the context | Not ours to cap. The engine decides its own behaviour. |
| Compact below 90 % without asking | Detail loss, and the user is the one who knows whether the detail matters |
| Show a precise-looking number | An estimate labelled as an estimate, or nothing |

## Testing

| Test | Type |
|---|---|
| `EstimateAccurateWithinBand` | Unit — a fixture run's estimate is within 20 % of the reported usage, and the test asserts the band so a regression is visible |
| `EstimateLabelled` | UI — the tooltip says "geschätzt", and the assertion checks for it |
| `UnknownLimitLabelled` | UI — an unknown model shows "angenommen 128k" |
| `Thresholds` | Unit — 70, 90, 95, 100, each producing the documented behaviour |
| `NoAutoCompactBelowNinety` | E2E — a run at 85 % is never compacted without a tap |
| `OfferAtNinety' | E2E — above 90 % the offer appears, with the action |
| `CompactAfterUnansweredOffer` | E2E — after 5 minutes, the app compacts and says so |
| `SummaryShown` | E2E — the summary is in the transcript, marked, and expandable |
| `PreservationInjected` | Unit — the injected block contains the plan, the changed files, the verification results, the open permissions, the cost, and the model |
| `LogEntry` | Integration — every compaction has a log entry with the before and after counts |
| `CompactionCost` | E2E — the compaction's tokens are in the run's cost |
| `PlanRestoredFromDatabase` | E2E — after a compaction that lost the plan's states, the next turn has them again |
| `NoResultTruncation` | Static — no code truncates a tool result before it reaches the engine
| `BuildLogNotTruncated' | E2E — a 40.000-token build log reaches the engine intact |
| `HandoffOnRepeatedOverflow' | E2E — a second overflow offers a fresh conversation with a structured handoff |
| `OverflowErrorSpecific' | E2E — a run that cannot proceed fails with the documented message
| `BarOnlyWhenRelevant' | UI — the bar is invisible below 70 % and visible above, with a label
| `LocalSmallContext' | E2E — a 8k model reaches the critical threshold correctly, because the bar uses the real limit
| `NeverCompactSilentlyAtNinetyFive' | E2E — above 95 % the offer repeats and the eventual automatic compaction is announced
