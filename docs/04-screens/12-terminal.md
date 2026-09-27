# Screen 12 — Terminal

A real terminal, in the app, that survives being backgrounded. For the person who wants to type a command themselves.

## Purpose

Provide a genuine shell, not a decorative one. Somebody who installed this app has a terminal inside it, with a working prompt, working tools, and working Ctrl+C.

## Structure

### Top bar

| Position | Element |
|---|---|
| Left | Back |
| Centre | The session's identity: a project glyph and a one-line label — "claude-code-android · bash" or "Lauf · liest build.gradle.kts" |
| Right | An overflow menu |

| Menu item | Behaviour |
|---|---|
| Neue Sitzung | Opens a confirm if a run is attached; otherwise starts a fresh shell |
| Sitzungen | The list of this project's sessions, with the last-used timestamp and a still-running marker |
| Schriftgröße | A slider, 10–20 sp, applied immediately |
| Verlauf leeren | Clears the visible scrollback. **The session is not killed**, and the note says so: "Nur die Anzeige wird geleert. Die Sitzung läuft weiter." |
| In Datei kopieren | Copies the visible scrollback to a file in the project, or to the share sheet |
| Keyboard-Einstellungen | Opens the system settings page for the app's input method |

There is no "Sitzung beenden" that kills a run, and there is no delete.

### The terminal surface

A `Canvas` renderer over a ring buffer. The full feature set is in `component-library.md`.

| Property | Value |
|---|---|
| Background | Always dark, both themes |
| Font | JetBrains Mono, user-set size |
| Line height | 1.35 × the font size |
| Scrollback | 5000 lines, configurable up to 20.000 |
| Cursor | A block, blinking at 530 ms, hidden when unfocused |
| Selection | Long press, then drag handles, then copy |
| Wide characters | CJK and emoji occupy two cells, correctly. A terminal that miscounts them is a terminal nobody trusts. |
| Combining characters | Rendered, not dropped |

### The keyboard row

A persistent row above the system keyboard while the terminal has focus.

| Key | Action |
|---|---|
| `Ctrl` | Modifier, sticky, with a visual state. Tapping a key after it applies the combination and releases the modifier. |
| `Alt` | Same |
| `Esc` | Sends 0x1B |
| `Tab` | Sends 0x09 |
| `Ctrl` `C` | Sends SIGINT. A dedicated button, because interrupting is the most common terminal action and it must not need two thumbs |
| `Ctrl` `D` | Sends EOF |
| `Ctrl` `Z` | Sends SIGTSTP |
| `Ctrl` `L` | Clears the screen |
| Arrows | Arrow keys, with `Ctrl` for word-wise motion |
| `↑` `↓` | Command history, persisted per project |
| `Home` `End` | Line start and end |
| `PgUp` `PgDn` | Scroll the viewport without sending to the process |
| Pipe | `|` |
| Tilde | `~` |
| Slash | `/` |
| `-` `_` | |
| Paste | A field that opens the clipboard, for on-screen keyboards without a paste key |

Modifier state is visible: a sticky `Ctrl` stays lit until used or dismissed, and a long press on `Ctrl` sends the literal control character. Without the lit state, sticky modifiers are a source of mystery behaviour.

## Interaction

| Action | Result |
|---|---|
| Tap the terminal | Focus it, position the cursor at the tap, show the keyboard row |
| Type | Sends bytes to the PTY |
| Enter | Sends a newline |
| Swipe down on the top bar | Nothing. There is no pull-to-refresh; a terminal is not a web page. |
| Two-finger tap | Paste from the clipboard |
| Long press on output | Select |
| Tap the session label | The session list |
| Back | Blurs the keyboard first, then leaves the screen. The session keeps running. |

**Back does not kill the session.** A terminal in a coding tool is a workspace, and leaving it must not destroy work. The app is explicit about this in the session list: a running session has a marker and a "läuft seit {time}" label, and stopping it is a separate, explicit action that warns about what a running process loses.

## Sessions

One persistent shell per project, created on first use and reused thereafter. The split view in `05-chat-with-terminal.md` attaches to it.

| Property | Value |
|---|---|
| Shell | The runtime profile's default shell: `sh` on the native profile, `bash` on proot |
| Working directory | The project root, with the current directory shown in the prompt |
| Environment | The app's own, plus the project's variables, plus a `CCA_*` set: `CCA_PROJECT_ID`, `CCA_RUN_ID`, `CCA_BACKEND` |
| History | Persisted per project, 1000 entries, deduplicated, with `HISTCONTROL=ignoreboth` |
| Idle timeout | None. An idle shell stays open until the app is closed or the session is explicitly ended. |

`CCA_RUN_ID` is set when a session is attached to a run, so a command typed in the terminal during a run can be correlated with it in the log. That is a transparency feature, not a convenience: the same output appears in both the terminal and the log, correctly attributed.

## States

| State | Behaviour |
|---|---|
| Starting | The prompt appears as soon as the shell starts. No spinner: a terminal that is starting shows nothing, and that is authentic. |
| Running a command | The prompt line stays, a new line is written, output streams live. There is no "running" indicator, because a real terminal does not have one. |
| Command finished | The prompt returns. The exit code is available via a long press on the prompt line: "Exit-Code: 0". |
| Command failed | The prompt returns with a visible failure. A `danger` marker appears on the prompt line with the non-zero exit code. |
| Command still running, screen locked | The foreground service keeps the PTY. On return, the output is there. |
| The session died | "Die Sitzung wurde beendet." with "Neue Sitzung". The scrollback is kept and readable. |
| PTY allocation failed | The specific error from `06-runtime/pty-and-terminal.md`, with a retry. A terminal that says "something went wrong" is worse than no terminal. |
| Storage full while writing scrollback | The scrollback stops growing and says so once, in the status line. The session continues. |
| A run is active in this project | The session label shows the run's state, and a new-session action warns |

## The "quiet is not stuck" line

A coding tool's terminal goes quiet during a long build. Nothing on screen distinguishes "compiling" from "hung", and the difference matters enormously to a user watching a phone.

So the terminal carries one addition that a real terminal does not:

```
┌─────────────────────────────────────┐
│ ▸ Läuft seit 4:12 · seit 38 s ohne Ausgabe │  status line, only while busy
└─────────────────────────────────────┘
```

| Rule | Behaviour |
|---|---|
| Visibility | Only while a foreground process is running and has produced no output for 30 s |
| Content | Elapsed total, and time since the last output |
| On output | Disappears immediately |
| Colour | `textTertiary`. It is information, not an alarm. |
| Monospace | The elapsed times are tabular, so the number does not jitter |
| Never | It never guesses why there is no output. It states the fact. |

This is the only place the app adds something a real terminal lacks, and it exists because the alternative — silence that could mean either — is the single most stressful thing about monitoring work on a phone.

## Accessibility

The honest position, stated plainly rather than hidden:

| Requirement | Status |
|---|---|
| Every key is labelled | Yes, with the actual character |
| Key combinations are reachable | Yes, `Ctrl` and `Alt` are real keys |
| Live announcements | **No.** A terminal is a byte stream; a screen reader following it produces continuous unusable speech. |
| An accessible equivalent | **Yes**, and this is the point: every tool call the agent makes appears as a tool card in the chat, in words, with its state. The terminal is the raw view; the tool cards are the accessible one. |
| Manual reading | A "Letzte Ausgabe vorlesen" action reads the last 20 lines on demand |
| Session state | The status line and the top bar label are the accessible equivalents of a running process |
| Exit codes | Announced on the prompt line's long press |
| Target | Every key at least 44 dp wide and 48 dp tall. A terminal key row with 32 dp keys is unusable with a thumb. |
| Font size | 20 sp makes the key row scroll horizontally rather than shrink the keys |

## Testing

| Test | Type |
|---|---|
| `TerminalPrompt` | Screenshot — a prompt, a cursor, and the key row, in both themes (both dark, per the design decision) |
| `TerminalOutput` | Screenshot with colour, a wrapped line, a long line, and a unicode line |
| `TerminalColors` | Screenshot with all 16 ANSI colours and a 256-colour sequence |
| `TerminalControlSequences` | E2E — clear line, clear screen, carriage return, backspace, tab stops, scroll region, cursor moves, all rendered correctly |
| `TerminalResize` | E2E — the window size change sends SIGWINCH and a full-screen program re-renders correctly |
| `TerminalCtrlC` | E2E — the dedicated button sends SIGINT and a sleeping process dies |
| `TerminalStickyModifier` | UI — `Ctrl` is lit, applies once, releases, and a long press sends the control character |
| `TerminalHistory` | E2E — 1000 entries persist across a session restart, and `↑` walks them |
| `TerminalWideChars` | Screenshot with CJK, emoji, and combining characters, asserting correct cell counts |
| `TerminalQuietLine` | E2E — a sleeping command shows the status line after 30 s, and it disappears on output |
| `TerminalBackSurvives` | E2E — leaving and returning keeps the session and its scrollback |
| `TerminalKeyRowScroll` | UI — at 20 sp the key row scrolls and the keys stay at 48 dp |
| `TerminalSessionDied` | Screenshot with the scrollback intact and the restart action |
