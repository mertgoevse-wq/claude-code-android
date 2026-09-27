# Screen 03 — New chat

The empty state. This screen is the product's first impression and the place a task begins.

## Purpose

Say one thing, choose one project, send. Nothing else. This screen must be usable by someone who has never written a line of code, in under ten seconds from arrival.

## Structure

```
┌─────────────────────────────────────┐
│  ✕              Mark (72dp)         │  top bar: close only
├─────────────────────────────────────┤
│                                     │
│         [ ambient wash ]            │
│           ◉  (72 dp)                │
│                                     │
│    Was soll gebaut werden?          │  serif displayLarge
│    (bodyLarge, max 3 lines)         │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ 📁 claude-code-android    ›   │  │  project selector
│  │    ASK_RISKY · Sonnet 4.5      │  │
│  └───────────────────────────────┘  │
│                                     │
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐  │
│  │ Beschreibe deinen Auftrag …   │  │
│  └───────────────────────────────┘  │
│  📎  @  ⊕Modell  ⊕Rechte  ⏹   [→] │  │
└─────────────────────────────────────┘
```

### The mark

72 dp, `IDLE`, with the ambient wash behind it at 4 % accent opacity. The only large instance of the mark in the app.

### The greeting

Serif, `displayLarge`, centred, one line where possible:

| Language | Text |
|---|---|
| German | "Was soll gebaut werden?" |
| English | "What should we build?" |

One line. Not a rotating list of prompts, not a set of suggestion chips. The screen says one thing and gets out of the way.

Below it, `bodyLarge`, `textSecondary`, centred, at most three lines, and only on the very first chat of an install:

> Claude plant, schreibt, prüft und legt den Stand in deinem Repository ab. Du siehst jeden Schritt.

That sentence is the whole product promise. It is worth three lines once, and it is not repeated on every new chat.

### The project selector

A single row, 56 dp, `surface` fill, `border` hairline, `radiusMedium`.

| Slot | Content |
|---|---|
| Leading | The repository or folder glyph |
| Title | The project name, `titleMedium` |
| Subtitle | Autonomy level and model, `labelSmall` — "ASK_RISKY · Sonnet 4.5" |
| Trailing | A chevron |

Tapping opens a sheet listing every project with a search field, each row showing the same autonomy and model, plus a trailing "Neues Projekt" row.

| Situation | Behaviour |
|---|---|
| One project | The selector is still shown, with the same affordance. Hiding it would make the first multi-project run a dead end. |
| No projects | The selector is replaced by a bordered notice: "Noch kein Projekt. Leg ein Repository an oder wähle einen Ordner." with an inline "Repository hinzufügen" secondary action. |
| No GitHub connection | Local folders are offered, with a note in the sheet: "GitHub ist nicht verbunden. Lokale Ordner funktionieren ohne Konto." |
| A project is archived | Not listed. The sheet links to the archive. |

### The composer

Identical to the chat screen's composer, so the transition into a conversation is continuous and the user learns it once. See `04-chat-detail.md` for the full specification.

The placeholder is specific, not "Nachricht schreiben":

| Language | Placeholder |
|---|---|
| German | "Beschreibe deinen Auftrag …" |
| English | "Describe your task …" |

### The action row

| Control | Purpose | Visibility |
|---|---|---|
| 📎 Attach | File, image, or a device file | Always |
| @ | Reference a file in the project, with completion | Always |
| ⊕Modell | Change the model for this run | Always |
| ⊕Rechte | Change the autonomy level for this run | Always |
| ⏹ | Stop, once a run starts | During a run |
| → Send | Start | When the composer has content |

`⊕Rechte` is a button, not a hidden setting, because autonomy is the single most consequential thing a user chooses and it must be visible at the moment of choosing. Tapping it opens a sheet with the four levels, the current one marked, and one line of consequence each:

| Level | German | English |
|---|---|---|
| `ASK_EVERYTHING` | "Ich frage bei jedem Schritt" | "I ask at every step" |
| `ASK_RISKY` | "Ich frage bei riskanten Schritten" | "I ask at risky steps" |
| `AUTO_WITH_CHECKPOINTS` | "Ich laufe selbst, melde mich an Checkpoints" | "I run myself, report at checkpoints" |
| `FULL_AUTO` | "Ich laufe selbst durch. Löschen und Bezahlen bleiben blockiert." | "I run straight through. Deleting and paying stay blocked." |

`FULL_AUTO`'s line ends with the hard blocks, every time it is shown. It is the only place a user could believe the app would delete something, so that belief is closed before it forms.

Choosing a level here applies to this run. The project's default is unchanged, and the sheet says so: "Standard für dieses Projekt: ASK_RISKY. Änderung gilt nur für diesen Lauf."

## States

| State | Behaviour |
|---|---|
| Empty composer | Send is disabled, with the reason being its own absence. A disabled, unexplained Send is acceptable here because there is genuinely nothing to send. |
| Composer with content | Send becomes primary and enabled |
| Runtime not ready | The composer is present and editable, but Send opens a sheet explaining the runtime is not set up, with a button to the setup screen. Typing is never blocked. |
| No project, no local folder | A bordered notice above the composer with a "Ordner wählen" action |
| Provider not configured | A notice above the composer: "Kein Anbieter eingerichtet" with a link. Send is disabled, and the notice says why. |
| Offline | Send is disabled, and the reason is on the button area: "Offline. Der Lauf startet, sobald du wieder online bist." The composer stays editable. |
| A run is already active for this project | A notice: "In diesem Projekt läuft bereits etwas." with a link to it. Starting a second concurrent run is refused, per `concurrency-model.md`. |
| Very long text | The composer grows to 6 lines, then scrolls internally. It never covers more than 40 % of the screen. |
| First chat, model not chosen | The project default is used silently, and the model is visible in the action row. Never a modal asking for a model. |

## Send behaviour

| Step | What happens |
|---|---|
| 1 | The screen transitions to the chat, and the user message appears immediately at the bottom |
| 2 | A run state of `CREATED` and then `PREPARING` |
| 3 | Within about 2 seconds, either a `PlanProposed` card or a first `ToolStarted` card appears |
| 4 | The mark goes `IDLE` → `THINKING` |
| 5 | The composer clears and remains, ready for a follow-up while the run proceeds |

**Step 1 is unconditional.** The user's message is in the transcript before anything else happens, so even if the run fails immediately, the user can see what they asked for and start again. An optimistic UI is correct for a chat.

## Accessibility

| Requirement | Implementation |
|---|---|
| Mark | "Claude Code, bereit" |
| Greeting | An `h1` heading |
| Project selector | "Projekt: {name}. Rechte: {level}. Modell: {model}. Wechseln." — the level is in the name, because the level is the consequence |
| The action row | Each control labelled with its purpose, not its icon name: "Datei anhängen", "Datei im Projekt erwähnen", "Modell wählen", "Rechte ändern", "Senden" |
| Send, disabled | "Senden ist nicht möglich, weil {reason}" |
| The four-level sheet | A single announcement with all four levels and which is selected, so a screen reader user does not have to swipe four times to hear the current state |
| The greeting paragraph | Read after the heading, once. It is not a live region. |
| Font scale 1.3 | The greeting wraps to two lines; the layout absorbs it. The composer stays pinned. |

## Testing

| Test | Type |
|---|---|
| `NewChatEmpty` | Screenshot, both themes, both languages, at 1.0 and 1.3 font scale |
| `NewChatNoProject` | Screenshot, with the notice |
| `NewChatOffline` | Screenshot, with the composer filled and Send disabled |
| `NewChatProjectSheet` | UI — the sheet lists projects, search filters, the new-project row is present |
| `NewChatAutonomySheet` | UI — four levels, current marked, the FULL_AUTO line mentions the hard blocks, and choosing applies to this run only |
| `NewChatSendOptimistic` | E2E — the message is in the transcript within 100 ms of the tap, before the run state exists |
| `NewChatRuntimeNotReady` | UI — Send opens the explanation sheet, typing was never blocked |
| `NewChatConcurrentRun` | UI — a second run for a busy project is refused with a link to the running one |
| `NewChatLongText` | Screenshot with 2000 characters, asserting the composer caps and scrolls |
