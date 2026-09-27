# PTY and terminal

A shell that believes it is talking to a terminal, and a renderer that shows what it writes.

## Why a PTY at all

A pipe is not a terminal. A program that checks `isatty` behaves differently against a pipe: no colours, no cursor control, no line editing, different buffering, and a different idea of when to flush.

```
$ echo $TERM | cat        # against a pipe
                        # empty
$ echo $TERM             # against a terminal
xterm-256color
```

Without a PTY, every tool a developer expects degrades into its most primitive mode. The terminal is not a nice-to-have; it is what makes Gradle, npm, and every CLI tool usable at all.

## The bridge

```
┌──────────────────┐   bytes    ┌───────────────┐   events   ┌──────────────┐
│ Compose terminal │ ◀───────── │  TerminalModel │ ◀────────── │  Ptysession   │
│    renderer      │            │  cells + state │             │  (native)     │
└──────────────────┘            └───────────────┘             └──────┬───────┘
                                                                       │ fd
                                                          ┌────────────┴────────────┐
                                                          │  the shell process     │
                                                          └─────────────────────────┘
```

### The native side

A small JNI library, `ccapty`, in this repository, with three responsibilities:

| Function | What it does |
|---|---|
| `open(cols, rows)` | `posix_openpt`, `grantpt`, `unlockpt`, `ptsname`, then `open` the slave |
| `start(argv, env, cwd)` | `fork`, `setsid`, `ioctl(TIOCSCTTY)`, `dup2` the slave to 0/1/2, `exec` |
| `read/write/resize/signal` | Thin wrappers, plus `TIOCSWINSZ` and `kill` |

| Requirement | Reason |
|---|---|
| It is `setsid` and `TIOCSCTTY` | So the shell becomes a session leader with a controlling terminal, which is what a terminal means to a program |
| It runs in the app's own process group | For the group kill, per `06-runtime/process-supervision.md` |
| Reads happen on a dedicated thread | A blocking `read` on a coroutine dispatcher starves the pool |
| Writes are on the caller's thread | A keystroke must not queue behind a slow reader |
| `resize` sends `SIGWINCH` | A full-screen program redraws on the new size, or it does not redraw at all |

`JNA` is used for the file-descriptor plumbing and a direct JNI binding for the rest, because the JNI surface is small and a bridge library would be more code than the thing it wraps.

### Allocation

| Property | Behaviour |
|---|---|
| One PTY per terminal session | Not per process. A run's engine and the user's shell are separate PTYs where both exist. |
| Columns and rows | From the renderer's measured size, clamped to 20–500 and 5–200 |
| Initial size | 80 × 24, then immediately resized to the real size |
| Release | On session close, in a `finally`. A leaked PTY is a leaked file descriptor, and a test asserts the count returns to baseline. |
| Failure | `RUNTIME_PTY_FAILED` with the specific reason, a retry, and a fallback to pipes |

**The pipe fallback** matters. If a PTY cannot be allocated, the app does not fail the feature. It runs the process with pipes, and the terminal shows a persistent notice: "Kein echtes Terminal. Manche Programme zeigen weniger Farben." A degraded terminal is honest; a missing one is useless.

## The renderer

A `Canvas` drawing a grid of cells. Not a `TextView` per line, and not a web view.

| Property | Value |
|---|---|
| Grid | `rows × cols` cells |
| Cell | A code point, a foreground, a background, and a set of attributes |
| Font | JetBrains Mono, so every glyph is the same width |
| Measurement | Advance width measured once per font size. A monospace font with a wrong measurement is the classic terminal bug. |
| Wide characters | CJK and emoji occupy two cells, with the second cell marked as a continuation. Getting this wrong shifts every line after it. |
| Combining characters | Attached to the previous cell, not given their own |
| Drawing | One `Canvas` pass, with a cell-level dirty region. A full redraw of an 80 × 24 grid is cheap, but a full redraw on every streamed byte is not. |
| Scrolling | A ring buffer of lines. Only the visible window is drawn. |
| Selection | A range of cells, with drag handles, and a copy that reconstructs the text from the grid |

## Colour

| Aspect | Behaviour |
|---|---|
| 16 ANSI | The palette in `03-design/design-tokens.md` |
| 256 colour | The xterm cube and greyscale ramp, computed from the index |
| Truecolor | 24-bit, from `38;2;r;g;b` |
| Bold | Bright variant for the 8 base colours, otherwise a synthetic bold. A real monospace font's bold is wider, which would break the grid, so bright-not-bold is correct here. |
| Underline, reverse, dim | Supported, with dim implemented as an opacity rather than a colour change |
| Default foreground and background | The theme's terminal tokens, and they are the shim's own, per the design decision that the terminal is always dark |
| The cursor | A block by default, a bar and a underline as settings, blinking at 530 ms, hidden when unfocused |

## Control sequences

The set a real terminal must handle, and what each does here.

| Sequence | Action |
|---|---|
| `CSI n G`, `CSI n C`, `CSI n D` | Cursor to column, right, left |
| `CSI n A`, `CSI n B` | Cursor up, down |
| `CSI n H`, `CSI n ; n H` | Cursor position, 1-based |
| `CSI n J` | Erase from the cursor to the end, to the beginning, or all |
| `CSI n K` | Erase the line, in three variants |
| `CSI n m` | SGR: reset, bold, dim, italic, underline, reverse, foreground, background, 256, truecolor |
| `CSI n ; m r` | The scroll region. Full-screen programs use it, and without it their output is unreadable. |
| `CSI n S`, `CSI n T` | Scroll up and down within the region |
| `ESC M`, `ESC D` | Reverse index, line feed |
| `ESC 7`, `ESC 8` | Save and restore the cursor |
| `ESC [ ? 25 h/l` | Cursor visible, hidden |
| `ESC [ ? 1049 h/l` | The alternate screen buffer, which is how `vim` and `htop` work. A terminal without it cannot run them. |
| `ESC ( B` | Reset the character set |
| `OSC 0;title` | The window title, shown in the top bar |
| `ESC ] 8;;url` | A hyperlink, which modern shells emit for file paths |
| `BEL` | Ignored, or as a haptic if enabled |
| `\b`, `\r`, `\t` | Backspace, carriage return, tab stops at every 8 columns |

**A sequence not in this list is consumed and ignored, never rendered as text.** A terminal that prints `^[[?25l` to the user has failed. The parser's default branch is "unknown sequence, skip it, log it", and a test feeds a corpus of real terminal output and asserts that no escape byte ever reaches the grid.

## The keyboard

| Key | Bytes | Notes |
|---|---|---|
| Printable | The UTF-8 encoding | |
| Enter | `\r` | |
| Backspace | `\x7f` | DEL, which is what a modern terminal sends |
| Tab | `\t` | |
| Esc | `\x1b` | |
| Arrows | `\x1b[A` … `\x1b[D` | |
| Home, End | `\x1b[H`, `\x1b[F` | And the application-cursor-mode variants, which some programs use |
| Delete | `\x1b[3~` | |
| Page up, down | `\x1b[5~`, `\x1b[6~` | Also scrolls the viewport |
| Ctrl + letter | The control code, `letter & 0x1f` | Ctrl-A is `0x01`, Ctrl-C is `0x03` |
| Alt + letter | `\x1b` + the letter | The meta prefix |
| Ctrl + arrow | The modified sequence | Word-wise motion in readline |
| Shift + arrows | The extended sequence | Selection in some programs |

| Key-row property | Value |
|---|---|
| Sticky modifiers | A visible lit state, applying once and releasing |
| A long press on a modifier | Sends the control character directly, so `Ctrl` is also a key |
| Key width | 44 dp minimum, 48 dp tall, scrolling horizontally rather than shrinking |
| The row | Present only when the terminal has focus, hidden when the chat composer does |
| Paste | A field that opens the clipboard, for on-screen keyboards without a paste key |
| A hardware keyboard | Fully supported, with the bindings in `04-screens/12-terminal.md` |

## Line discipline

The kernel's line discipline handles canonical mode, echo, and signals. The app does not reimplement it.

| Aspect | Behaviour |
|---|---|
| Canonical mode | The default. A program that wants raw mode sets it with `tcsetattr`, and the bridge honours it. |
| Echo | On by default, which is why typing appears. A raw-mode program turns it off, and the app stops echoing too. |
| `ISIG` | On, so Ctrl-C generates SIGINT to the foreground process group. The app's Ctrl-C button sends the byte, the line discipline turns it into the signal, and the group kill is not needed. This is the correct path. |
| `IEXTEN` | Honoured |
| Window size | Set with `TIOCSWINSZ`, which also sends `SIGWINCH` |

## Performance

| Concern | Approach |
|---|---|
| Streaming a fast build | A bounded channel between the reader and the renderer. When the renderer is behind, output drops from the *live* view and is marked; the file keeps everything. A terminal that drops input makes the program misbehave, so the reader never drops — only the live preview can. |
| Redraws | Cell-level dirty tracking. A changed cell dirties a rectangle. |
| The font | Measured once per size, never per glyph |
| Long lines | Wrapped into the grid, with a marker column, so a 4.000-character line does not break the layout |
| Very large scrollback | A ring buffer of cells, not of strings. 5.000 lines × 80 cells is a bounded, predictable allocation. |
| Reflow on resize | The grid rewraps. Full-screen programs that redraw on `SIGWINCH` will overwrite it anyway. |
| A repaint under the finger | 60 fps, with a benchmark test at 200 lines per second of output |

## Accessibility, honestly

A terminal is not screen-reader navigable, and pretending otherwise would be worse than saying so.

| What is provided | What is not |
|---|---|
| Every key labelled by its character | Live announcement of output |
| A "Letzte Ausgabe vorlesen" action for the last 20 lines | Following a stream in real time |
| A status line naming the running process and its elapsed time | — |
| The tool cards in the chat, which are the accessible equivalent of everything a run does | — |
| Exit codes, on a long press | — |

The design justification: the terminal is a specialist surface, and the app's job is to make sure everything it does is *also* available somewhere accessible. The tool cards are that somewhere. They exist for every reason, and this is one of them.

## Testing

| Test | Type |
|---|---|
| `PtyAllocates` | Integration — a PTY opens, and the child reports `isatty` true and a non-empty `TERM` |
| `PtyLeakFree` | Integration — after 100 session open/close cycles, the file-descriptor count returns to baseline |
| `PtyFallback` | E2E — a PTY allocation failure falls back to pipes with a visible notice, and the shell still works |
| `ControlSequenceCorpus` | Unit — a corpus of real terminal output renders with no escape byte visible in the grid |
| `ScrollRegion` | Unit — a program that sets a region, prints a box, and clears it renders correctly |
| `AlternateScreen` | E2E — `vim` opens, draws, and closes, leaving the previous content intact |
| `TruecolorAnd256` | Screenshot — all three colour modes render correctly |
| `BoldIsNotWide` | Unit — bold text has the same cell width, and the grid does not shift |
| `WideCharacters` | Unit — CJK occupies two cells, emoji occupy two, combining marks attach, and the following line is not shifted |
| `WrapAndMarker` | Unit — a 4.000-character line wraps with a continuation marker |
| `ResizeSignal` | E2E — a resize sends `SIGWINCH` and a full-screen program redraws |
| `KeyEncoding` | Unit — every key in the row maps to the correct bytes, including the modifier combinations |
| `IsigCtrlC` | E2E — Ctrl-C sends the byte, the line discipline generates SIGINT, and a sleeping process dies |
| `StickyModifier` | UI — the modifier lights, applies once, releases, and a long press sends the control character |
| `RawMode` | E2E — a program that sets raw mode disables echo, and typing stops appearing |
| `FloodSafety` | Performance — `yes` at maximum rate for 10 s: the live view marks the drops, the file keeps everything, and memory is bounded |
| `Benchmark` | Performance — 200 lines per second at 60 fps |
| `Selection` | UI — select, drag, copy, and the reconstructed text matches the source |
| `NoLeakAfterCrash` | Integration — a renderer crash does not leak the PTY, and the next session works |
| `AccessibilityStated` | UI — asserts the "read the last 20 lines" action exists and that the design note is present in the code as a comment |
