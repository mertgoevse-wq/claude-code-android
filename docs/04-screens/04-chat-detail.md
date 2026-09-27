# Screen 04 — Chat detail

The main screen. Everything that makes this app worth using happens here.

## Purpose

Show what the agent is doing, in real time, without the user having to read a terminal. Let them interrupt. Let them look at the raw output if they want to. Never hide anything.

## Structure

```
┌─────────────────────────────────────┐
│ ‹  ◉ claude-code-android     ⏹  ⋯  │  top bar, collapses on scroll
├─────────────────────────────────────┤
│ [active-run banner, if any]         │
│                                     │
│  ── Plan ────────────────────── 4 › │  plan card, collapsible
│                                     │
│  Ich schaue mir zuerst an, wie das  │  assistant message
│  Projekt aufgebaut ist.             │
│                                     │
│  ┌──────────────────────────────┐   │
│  │ ⟐  Datei gelesen · build.gradle │  │  tool card, running
│  │ ──────────────────────────────│   │  travelling bar
│  └──────────────────────────────┘   │
│  ┌──────────────────────────────┐   │
│  │ ✓  47 Prüfungen bestanden  ›  │  │  tool card, done, collapsed
│  └──────────────────────────────┘   │
│                                     │
│  ✓ Geprüft · 3 Dateien · 0,04 $     │  run summary
│                                     │
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐  │
│  │ Nachricht an Claude …         │  │
│  └───────────────────────────────┘  │
│  📎 @ ⊕Modell ⊕Rechte   ~0,04 $  → │  action row + cost
└─────────────────────────────────────┘
```

### Top bar

| Position | Behaviour |
|---|---|
| Left | Back, then the mark at 28 dp in the run's state, then the project name |
| Right | Stop (during a run), then an overflow menu |

**The mark carries the run state** and is the only element in the top bar that changes during a run. The project name is `titleMedium`, ellipsised at 20 characters.

**Collapse on scroll.** Scrolling down past 40 dp collapses the top bar to a slim version: the mark and the project name remain, the back arrow moves to an edge tap target. Scrolling up restores it. The stop button is never hidden by this — it moves to the action row, where the user's thumb already is.

| Overflow menu | Action |
|---|---|
| Terminal öffnen | Opens the split view |
| Verlauf ansehen | Run history for this conversation |
| Umbenennen | Renames the conversation |
| Exportieren | Exports the transcript |
| Archive | Archives the conversation |

No delete. The menu is the fourth place that absence is deliberate.

### Message list

A `LazyColumn`, newest at the bottom, reverse-ordered, auto-scrolling while the user is at the bottom.

| Message type | Presentation |
|---|---|
| User | Right-aligned, `surfaceSubtle` fill, `radiusLarge` with a reduced bottom-right corner, max 88 % of the column, `bodyMedium` |
| Assistant | Full width, no fill, `bodyLarge`, markdown rendered |
| Thinking | A collapsed card, `bodySmall` in `textTertiary`, marked "Denkt nach", expandable |
| System notice | Full width, centred, `labelSmall`, `textTertiary`: "Berechtigung erteilt", "Lauf unterbrochen" |
| Run summary | The card at the end of a run, per `component-library.md` |
| Error | A bordered card, `danger` glyph, the plain-language first line, the raw detail collapsed |

**Assistant messages are not bubbles.** A long answer in a bubble is a lie about its length and makes long-form reading worse. Only user messages are bubbles, and only because a user's own words deserve to be visually separable from the machine's.

### The plan card

Always at the top of the current run, collapsed to a single line, expandable.

```
Plan · 4 Schritte · Schritt 3 läuft              ›
```

Expanded: each step with a state glyph, its title, and its acceptance criteria. A checkpoint step is marked `⊟` with "Prüfpunkt". A completed step is struck through in `textTertiary` — not removed, because a plan that erases its history cannot be reviewed.

Before the run starts, the plan is editable: reorder, retitle, remove, add. Once running, it is read-only.

### Tool cards

Per `component-library.md`. Collapsed by default, one line, plain language. Appearing on `ToolStarted`, not on completion.

The card's collapse state is per invocation and remembered on the device. A user who expands every `Bash` card to watch output does so once, not on every run.

A long run produces a long list of cards. The list does not auto-scroll away from a message the user is reading; auto-scroll engages only when the user is already at the bottom.

### The run summary card

At the end of a run, in this order:

| Row | Content |
|---|---|
| Status | The `StatusBadge`. **`UNVERIFIED` leads.** |
| Task | The original order, `bodySmall`, 2 lines |
| Changes | "{n} Dateien geändert, +{a} −{r}", tappable to the diff viewer |
| Verification | "3 Prüfungen, 2 bestanden, 1 fehlgeschlagen: {command}", or "Keine Prüfbefehle — nicht geprüft" |
| Cost | "{cost} US-Dollar · {in} Eingabe · {out} Ausgabe", tappable to the cost sheet |
| Git | Branch, commit hash (7 chars, monospace), and a PR link if it exists |
| Actions | Diff ansehen, Terminal, Nochmal, Unterhaltung teilen (text only, no visibility change) |

`Nochmal` re-runs the same task as a new run on a new branch, keeping the old one. It is the most-used action after a failure, and it does not overwrite anything.

### The composer

Identical to the new-chat screen's, with these additions during a run:

| Addition | Placement |
|---|---|
| Stop button | Left of Send, `danger` outline, 48 dp, with a 120 ms press scale |
| Cost | In the action row, right-aligned, `labelSmall`, tabular figures, `~` when estimated |
| Context indicator | A thin 2 dp bar above the action row, showing context fill; `warning` at 70 %, `danger` at 90 % |
| Terminal toggle | A `terminal` icon that opens the split view |

The composer stays enabled during a run. A user can send a follow-up mid-run; it is queued and delivered at the next turn boundary, and the composer shows "Wird nach dem aktuellen Schritt gesendet". Blocking the composer would make a long run unfollowable.

### The split terminal view

Toggled from the top bar, the action row, or a swipe from the right edge.

| Size | Layout |
|---|---|
| Compact | Terminal as a bottom sheet at 60 % height, over the chat |
| Compact landscape | 50 / 50 vertical |
| Medium and up | 60 / 40 vertical, or 65 / 35 horizontal |

The terminal has its own scrollback, its own focus, and its own keyboard row. Scrolling one does not scroll the other, because comparing an error in the log with the explanation above it is the primary reason to open both.

## States

| State | Behaviour |
|---|---|
| Loading a past conversation | A skeleton of two message shapes, matching the real content's structure, then the content. The shapes are predictable, which is what makes a skeleton honest here. |
| No run yet | The transcript plus the composer, no plan card, no stop button |
| Running | Plan card live, tool cards streaming, stop enabled, cost counting, mark in a working state |
| Awaiting permission | The message list dims to 60 % behind a `PermissionSheet`. The sheet is not dismissible by tapping outside. |
| Verifying | A verification panel card appears after the last tool card: steps with states, the current command in mono, elapsed time |
| Paused | The mark is `PAUSED`, a banner reads "Angehalten", the composer accepts input, and the stop button becomes a resume |
| Interrupted | A notice: "Der Lauf wurde unterbrochen. Die Arbeit ist gesichert." with two actions: Fortsetzen, Verwerfen (which abandons the turn and keeps the branch) |
| Finished, verified | The summary card with a `success` badge |
| Finished, unverified | The summary card with a `warning` badge, and it leads with "Nicht geprüft" |
| Failed | The summary card with a `danger` badge, a plain-language report, the retry counter, and Fortsetzen |
| Offline mid-run | A notice: "Keine Verbindung. Der Lauf läuft weiter, sobald sie zurück ist." The transcript is complete up to the last event |
| Very long transcript | Virtualised. A jump-to-bottom button appears when scrolled up, with a count of unseen messages. |
| Context nearly full | The context bar turns `warning`; above 90 % the mark's state becomes `THINKING` and a notice suggests compaction |

## Behaviour worth stating explicitly

**Auto-scroll yields.** If the user has scrolled up, new messages do not yank the view. A "N" button appears with the count of unseen messages. This is the difference between a chat you can read and a chat you fight.

**Interrupt is fast.** The stop button sends SIGINT, waits two seconds, then SIGTERM. The UI shows "Wird gestoppt" immediately and confirms within three seconds. A stop that takes visibly longer than three seconds leaves the user tapping again, and the second tap is on a screen whose state has changed.

**A refused action is not a failed run.** When a hard block refuses a tool call, the card shows `⊘ Blockiert` with the rule's plain-language reason, and the run continues. Only a run that cannot make progress ends.

**The transcript is the record.** Every event, including malformed lines and dropped events, is in the log. The screen can show fewer; the log cannot.

## Accessibility

| Requirement | Implementation |
|---|---|
| The mark | "Claude Code, {state in words}" — "denkt nach", "liest eine Datei", "wartet auf dich", "prüft", "läuft", "fertig, geprüft", "fertig, nicht geprüft", "unterbrochen", "fehlgeschlagen" |
| Streaming text | Announced on message completion, not per delta |
| Tool cards | Merged: "{tool}, {target}, {state}, {elapsed}. Details." |
| The plan | A list. Each step is a list item announcing its state. |
| The cost | Not a live region. Announced on completion and on demand. |
| The stop button | "Lauf stoppen" |
| The permission sheet | Focus moves into it, the background is marked, and the reason is read before the actions |
| Auto-scroll | A screen reader user is never moved; the jump-to-bottom button is the mechanism |
| The split terminal | Focus moves into it, and the chat is marked as a separate region |
| Font scale 1.3 | Messages wrap, the composer grows, tool card headers wrap to two lines, the action row wraps |
| Target | Every control 48 dp, including the stop button and the jump-to-bottom button |

## Testing

| Test | Type |
|---|---|
| `ChatDetailIdle` | Screenshot, both themes |
| `ChatDetailStreaming` | Screenshot mid-stream, with a tool card running and a partially streamed message |
| `ChatDetailPlanExpanded` | Screenshot with all five step states and a checkpoint |
| `ChatDetailPermissionSheet` | Screenshot, and a UI test asserting the three actions, their order, and the non-dismissal |
| `ChatDetailVerification` | Screenshot with 3 results, one failed, naming the command |
| `ChatDetailSummaryVerified` | Screenshot — `success` badge leads |
| `ChatDetailSummaryUnverified` | Screenshot — `warning` badge **leads**, and a test asserts its position is above the changes row |
| `ChatDetailRefusedBlock` | Screenshot with a `⊘ Blockiert` card and the reason, with the run still active |
| `ChatDetailSplitTerminal` | Screenshot at 4 size classes, with independent scroll assertions |
| `ChatDetailAutoScrollYields` | UI — scrolling up 20 messages and receiving 10 new ones does not move the viewport |
| `ChatDetailInterrupt` | E2E — the process tree is gone within 3 s, work is committed, state is preserved |
| `ChatDetailResume` | E2E — a resumed run continues the same session |
| `ChatDetailRotation` | E2E — mid-stream rotation loses nothing |
| `ChatDetailFontScale` | Screenshot at 1.3 for every state above |
