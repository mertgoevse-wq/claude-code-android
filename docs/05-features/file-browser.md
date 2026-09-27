# File browser

Looking at what is in a project, without leaving the app and without a full file manager.

## Purpose

Let somebody see a project's structure, read a file, and find the thing a message referred to. Not manage the device's storage — this is a view onto one project.

## Structure

### Entry points

| From | Where |
|---|---|
| The project detail's Fähigkeiten section | "Dateien ansehen" |
| A tool card naming a file | Tapping the target opens the file |
| A diff viewer's file header | "Datei ansehen" |
| A message mentioning a path | Tapping the path opens the file |
| A run's log | Tapping a path opens the file |
| The composer's `@` | Selecting a file opens it in a preview pane rather than inserting it, with an explicit "Erwähnen" action |

## The tree

A lazy, expandable tree, one directory level at a time.

| Element | Content |
|---|---|
| A directory row | A chevron, a folder glyph, the name, an optional child count, an optional size |
| A file row | A type glyph, the name, an optional size, an optional git status marker |
| Indent | 16 dp per level, up to 6 levels. Deeper than that, the path is shown instead of more indentation. |
| Sorting | Directories first, then files, both alphabetical, case-insensitive |
| Hidden files | Shown, with a filter toggle, defaulting to showing them. A `.gitignore`d file is dimmed. |
| Binary files | Marked. Tapping opens the external handler, not a broken preview. |
| Large files | Over 1 MB, the size is shown. Tapping asks whether to open a preview or the external handler. |
| Symlinks | Shown with a link glyph and their target |
| Empty directories | Shown, because an empty directory is a fact |

### Git status markers

| State | Marker | Colour |
|---|---|---|
| Modified | `M` | `warning` |
| Added, staged | `A` | `success` |
| Untracked | `?` | `info` |
| Renamed | `R` | `info` |
| Deleted, in the working tree | `D` | `danger` |
| Ignored | dimmed, no marker | — |
| Clean | nothing | — |

Every marker is a letter, not only a colour, per the anti-slop rule on colour-only meaning.

## The file viewer

| Feature | Behaviour |
|---|---|
| Syntax highlighting | The token set from `color-and-contrast.md`, for the languages the highlighter supports |
| Unsupported language | Plain monospace, which is honest. No attempted highlighting that gets it wrong. |
| Line numbers | A 40 dp gutter, toggleable |
| Wrapping | Off by default, with a toggle. Wrapping a line of code lies about its length. |
| Horizontal scroll | With a 16 dp shadow on the edge that has more content, so it is discoverable |
| Very long files | Virtualised, with the line count in the header |
| Binary | Not previewed. A message with the file's type and size, and a button to open it externally. |
| Encoding | UTF-8 assumed, with a fallback that shows the file with a visible note rather than mojibake |
| Line endings | Preserved on display. A file with CRLF shows the fact in the status line, because it matters for diffs. |

### Editing

**The app does not edit files in the file viewer.** Editing is the agent's job, or the terminal's, or an external editor's.

This is a scope decision, and a deliberate one:

| Reason | Detail |
|---|---|
| An in-app editor would need a full text-editing stack | Undo, redo, find and replace, selection across a virtualised list, encoding handling — that is a second application |
| Two editors means two sources of truth | Somebody would edit in the viewer, run the agent, and get an unexpected conflict |
| The agent is the reason the file changed | A manual edit in between would make the run's diff confusing and the log incomplete |
| The terminal already works | With a real editor, on a real filesystem, and it is one tap away |

The viewer's actions are: copy the path, copy the content, open externally, and "In Chat erwähnen". That is all.

## The path

A persistent breadcrumb, always visible above the tree, because a tree fifteen levels deep without a path is a maze.

```
proj / src / main / kotlin / dev / ccandroid
```

| Aspect | Behaviour |
|---|---|
| Tapping a segment | Navigates to that level |
| Overflow | At narrow widths, the middle segments collapse to `…` |
| Copy | The full relative path, and the absolute path, both copyable |
| An `@` insertion | Inserts the relative path |

## Searching

| Property | Behaviour |
|---|---|
| Scope | The current directory subtree, or the whole project with a toggle |
| Fields | File name, and optionally the content, for text files under 1 MB |
| Name search | Instant, over the loaded tree |
| Content search | A background scan with progress, cancelable, capped at 2000 results |
| Results | Grouped by file, with the matching line and a context line |
| A binary file | Skipped for content search, with a count of skipped files |
| A large project | A warning with the file count and an estimated time, before starting |
| No results | "Keine Treffer in {scope}." |

**Content search over a large repository on a phone is slow, and pretending otherwise would be dishonest.** The warning exists for that reason. A remote runner can do it faster, and the scan offers to offload, with the same privacy confirm as any other offload.

## States

| State | Behaviour |
|---|---|
| Loading a directory | A progress line with the count read so far. A directory listing is fast, and a skeleton is a lie. |
| A directory is empty | "Dieser Ordner ist leer." |
| Permission denied | "Kein Zugriff auf diesen Ordner." with the path |
| The path vanished | "Dieser Pfad existiert nicht mehr." — a file can be removed by a run while the browser is open |
| A symlink loop | Detected, not followed, and reported: "Symbolischer Link führt zu sich selbst." |
| A very large directory, over 5000 entries | Virtualised, with a warning count and a search-first prompt |
| A file changed while open | The header shows "Geändert seit dem Öffnen" with a reload action. The app does not silently reload, because somebody reading a line would lose their place. |
| A binary file | Not previewed, with a message and an external open |
| Offline | Everything local works. Nothing else is involved. |

## Performance

A file tree on a phone is a place where naive implementations fall over.

| Concern | Approach |
|---|---|
| Listing | Read one directory at a time, lazily. Never walk the whole tree to render a root. |
| A huge directory | Virtualised, with a windowed adapter. Never materialise 50.000 entries. |
| Sorting | After the listing, not during. |
| Git status | From a single `git status --porcelain` call, mapped onto the loaded tree, refreshed on a debounce. Not a per-file call. |
| Caching | Directory listings are cached with their modification time. A refresh only re-reads what changed. |
| Content search | A background coroutine with a hard cancel and a result cap. |
| Syntax highlighting | Off the main thread, chunked, and skipped for files over 200 KB rather than blocking the scroll. |
| A huge file | Virtualised lines. A 5.000.000-line file opens and scrolls. |

## Accessibility

| Requirement | Implementation |
|---|---|
| The tree | A real tree structure, with `expanded` and `level` on every node, so TalkBack announces "Ordner src, aufgeklappt, Ebene 2" |
| A file row | "{name}, {type}, {size}, {git status}. Öffnen." |
| The breadcrumb | A list of links, each naming its level |
| A binary file | Announced before the tap, not discovered by the failure to render |
| Changed while open | A live region, because silent staleness is a lie |
| The content view | A scrollable text region, with the path as its label |
| Line numbers | Not announced per line. A screen reader user reads the content; the numbers are visual. |
| Font scale | Irrelevant to the terminal-style views, which are grids. The tree rows wrap their names. |
| Target | 48 dp rows; the file viewer is exempt from text scaling, like the terminal, because it is pre-formatted content. The exemption is stated. |

## Testing

| Test | Type |
|---|---|
| `TreeLazy` | Integration — the root is rendered without walking the tree; only the opened paths are read |
| `TreeSorting` | Unit — directories first, then alphabetical, case-insensitive |
| `TreeGitMarkers` | E2E — a modified, an untracked, an added, a renamed, and an ignored file each get the right letter marker |
| `TreeHugeDirectory` | Performance — 5.000 entries render and scroll without a frame drop |
| `TreeSymlinkLoop` | E2E — a self-referential link is reported, not followed |
| `TreeFileVanished` | UI — a file removed while open says so, with a reload action |
| `ViewerHighlighting` | Screenshot for each supported language, and a plain-monospace fallback for one that is not |
| `ViewerNoWrapDefault` | UI — wrapping is off, a long line scrolls horizontally, and the edge shadow is present |
| `ViewerBinary` | UI — a message with type and size, and an external open. Never a broken preview. |
| `ViewerChangedWhileOpen` | E2E — a run modifies the file, the header notes it, and the content does not silently change |
| `ViewerCRLF` | Screenshot — CRLF is visible in the status line |
| `ViewerNoEditing` | UI — asserts no editing affordance exists anywhere in the viewer |
| `SearchContentWarning` | UI — a large repository produces the warning with a count and the offload offer |
| `SearchContentSkipsBinary` | E2E — binary files are skipped and the count is reported |
| `SearchContentCancel` | UI — a long scan cancels promptly and keeps the partial results |
| `TreeAccessibility` | UI — nodes carry the expanded and level semantics |
| `ViewerHugeFile` | Performance — a 5.000.000-line file opens and scrolls |
