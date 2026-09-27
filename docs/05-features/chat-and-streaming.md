# Chat and streaming

How text, tool calls, plans, and cost reach the screen while an agent works.

## The streaming path

```
CLI stdout → AgentStreamClient → AgentEventMapper → 4 consumers
```

| Consumer | What it does | Where |
|---|---|---|
| `ChatViewModel` | Reduces events into `UiState` | Main |
| `ConversationRepository` | Persists, so the transcript survives a crash | IO |
| `LogRepository` | Appends to the immutable record | IO |
| `CostMeter` | Aggregates tokens and cost | Default |

Four consumers of one flow, fanned out with a bounded buffer. See `02-architecture/concurrency-model.md`.

## Text streaming

| Aspect | Behaviour |
|---|---|
| Rendering | `bodyLarge`, markdown, appended as deltas arrive |
| Animation | None on the text itself. Animating text that is arriving puts the display behind the stream. |
| Caret | A 1 dp × 1.15 em `accentText` line at the end, 530 ms cycle, hidden on reduced motion |
| Buffering | Deltas are coalesced per frame. Forty deltas in 16 ms produce one recomposition, not forty. |
| Chunk size | A delta larger than 16 KB is split for rendering. The data is never dropped. |
| Markdown | Rendered progressively. An unclosed fence renders as an open code block with the caret inside, not as inline backticks. |
| Scroll | Auto-scrolls only if the viewport is at the bottom. Otherwise a jump button appears with a count. |
| A message that arrives mid-scroll | Does not move the viewport. Never. |

## The cost meter

The rule that makes it trustworthy: **the number is never silently replaced.**

| Phase | Display |
|---|---|
| Before the first usage | "—", not "0,00 $". A cost that has not started is not zero. |
| During the run | The live estimate, prefixed with `~` |
| Provider usage arrives | The estimate animates to the actual over 320 ms. Both values are stored; the actual wins. |
| After | The actual, with no `~` |
| A rate-limit header arrives | A separate line: "Ratenlimit: 2400 von {n} genutzt" |
| On tap | A sheet: the token breakdown, cache read and creation, the model, the provider, the session id, and a per-message breakdown |

| Concern | Handling |
|---|---|
| Providers that do not report usage | The estimate is marked permanently as an estimate, with a note: "Dieser Anbieter meldet keine Tokenzahlen. Der Wert ist geschätzt." |
| Missing cache fields | Shown as "—" rather than 0, because a gateway that omits the field did not report zero |
| Price changes later | The prices used are stored per record, so old runs are never re-costed silently |
| Currency | USD only, labelled as an estimate in the sheet. No conversion, because a conversion rate is another thing that can be wrong. |
| A long run | The number is not a live region. It is announced on completion and on demand. |

## Tool cards

Per `component-library.md`. The details that matter at the feature level:

| Question | Answer |
|---|---|
| When does a card appear? | On `ToolStarted`, not on completion. A card that appears when the tool finishes leaves the user staring at nothing for thirty seconds. |
| What does it show collapsed? | A glyph, a plain-language title, the target, the state, the elapsed time |
| What does it show expanded? | The output, capped at 2 KB with a visible marker and a link to the full log |
| How is the title produced? | Precomputed in the user's language at record time, per ADR-010 |
| How long is a target? | 52 characters, truncated with a full path in the expanded body |
| What is nested? | Subagent tool cards, one level deep, with the child's collapsed state remembered |
| What is a destructive card marked as? | A `danger` glyph and a "cannot be undone" note, from `HardBlockPolicy` |
| What happens to a blocked tool? | The card shows `⊘ Blockiert` with the rule's reason. The run continues. |

## Thinking

Thinking blocks are collapsed by default, in `textTertiary`, labelled "Denkt nach".

| Rule | Reason |
|---|---|
| Collapsed by default | A wall of reasoning is not information a user can act on. It is anxiety. |
| Expandable | Somebody debugging a wrong turn needs it. |
| Never streamed visibly | It arrives as deltas into a collapsed card whose content updates silently. The user sees the label, not a firehose. |
| Capped | 64 KB accumulated, then stopped. The rest is in the log. |
| Not stored in full in the transcript | The transcript keeps the collapsed summary and a log reference |

**The mark's `THINKING` state is the visible signal**, not the text. That is a deliberate inversion of the usual chat interfaces: the animation tells you it is thinking, the text is there if you want to know why.

## Subagents

| Aspect | Behaviour |
|---|---|
| Detection | A message with a non-null `parent_tool_use_id` |
| Forwarding | Enabled, so subagent text and thinking are forwarded where the engine supports it. Absent support degrades to tool cards only. |
| Nesting | One level deep in the UI. Deeper nesting is shown as a count: "3 Unteragenten". |
| The spawning tool card | Expands to show the child's cards, indented |
| Labelling | The child's name, plus its tool calls. A subagent is not an opaque "task". |
| Cost | Subagent usage is attributed to the same run and therefore to the same cost meter |
| Interrupting | Interrupting the parent interrupts the child. Verified by a test that checks the whole process tree. |

## Permission requests

The feature, not just the component. See `08-orchestration/permissions.md` for the policy.

| Aspect | Behaviour |
|---|---|
| Where it appears | A sheet over a dimmed message list, focus moved into it |
| What it says | The action, the specific change, what cannot be undone, why it is asking |
| Options | Allow once, allow for this run, deny. Always in that order. |
| Timeout | None. It waits indefinitely with a notification. An unanswered permission is never guessed. |
| "For this run" | Session-scoped, never written to a project setting |
| While blocked | The run is `AWAITING_PERMISSION`; the mark is `WAITING`; the stop button still works |
| A refusal | The card shows `⊘ Blockiert` with the rule, and the run continues |
| Many at once | The engine asks one at a time. If more arrive, they queue and the sheet shows "2 weitere" |
| Notification | Sent once, and again after 30 minutes, because a phone in a pocket for an hour should not be the only way to find out |

## Sending a follow-up mid-run

The composer stays enabled.

| Aspect | Behaviour |
|---|---|
| Queuing | The message is queued and delivered at the next turn boundary |
| Feedback | The composer shows "Wird nach dem aktuellen Schritt gesendet", and the queued message appears in the transcript immediately, marked as queued |
| Several queued | All delivered in order at the next boundary |
| Interrupting | The queue is preserved across an interrupt, so a resume delivers them |
| Permission | A queued message does not dismiss a pending permission. The user deals with the sheet first. |

This is what makes a long run followable. Blocking the composer would mean the user could not redirect something they can see going wrong until the agent finished doing it.

## Error display

Per `02-architecture/error-taxonomy.md`.

| Form | Where | Content |
|---|---|---|
| Inline | A card in the transcript | The plain-language first line, the raw detail collapsed behind "Technische Details" |
| Banner | Under the top bar | A run-ending error only, with the action |
| Snackbar | Transient | Only for something already resolved, like a permission granted |
| Full screen | Only if the app cannot function | With a restart action |
| Notification | When backgrounded | With a deep link to the exact message |

A single error is never rendered in two places at once. The banner replaces the inline card for a run-ending error, so the transcript does not show the same failure twice.

## What the transcript never does

| Never | Reason |
|---|---|
| Collapse older messages automatically | The user decides what is collapsed, per card, and that choice is remembered |
| Truncate a long message | A long answer is the answer |
| Drop a tool card | Every tool call in the log, always |
| Hide a refused action | A refusal is a result, and a hidden refusal looks like a silent failure |
| Rerender the whole list on a new message | Virtualised, with a keyed diff, so a streaming message does not cost a full relayout |
| Show a skeleton for message content | A message's shape is not predictable, and a skeleton of a paragraph is a lie |
