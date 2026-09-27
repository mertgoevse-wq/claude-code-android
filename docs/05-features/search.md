# Search

One search box, several kinds of content, and results that say where they came from.

## Purpose

Somebody who left a project for two weeks and comes back with "it was something about the test that kept failing" needs to find that conversation. Today the content lives in four places — conversations, projects, skills, and run history — and four search boxes is four things to try.

## Where search appears

| Screen | Scope | Default |
|---|---|---|
| Chat list | Conversations, their messages, and the projects they belong to | Recent |
| Projects | Names, paths, owners, branch names | Recent |
| Skills | Names, descriptions, content | Recent |
| Run history | Task text, branches, commit hashes, log entries | Recent |
| The project detail's history | Runs for this project | Recent |
| The composer's `@` | File paths | Path prefix, per `05-features/attachments.md` |

A global search is **not** added. It would be a seventh place with its own syntax and its own ranking, and the value over six scoped boxes is small.

## What is searched

| Kind | Indexed fields |
|---|---|
| Conversations | Title, every message's text, the project name, file paths mentioned, tool names |
| Projects | Name, path, owner, repository name, branch names |
| Skills | Name, description, body, source URL, scope |
| Runs | Task text, branch, commit hash, plan steps, tool call targets, log messages |
| Files | Names always, and content for text files, when explicitly requested |

Everything is local. There is no remote search, because there is no server. The index is a local FTS table plus a small in-memory index for paths, rebuilt on demand and incrementally maintained.

## Query behaviour

| Aspect | Behaviour |
|---|---|
| Matching | Substring, case-insensitive, diacritic-insensitive. "grosse" finds "große", "Strasse" finds "Straße" |
| As you type | Results update on a 200 ms debounce. Faster feels laggy, slower feels broken. |
| Highlighting | The matched run is `accentText` and semibold. Not a background highlight, which makes a long message unreadable. |
| Ranking | Title match > beginning of body > middle of body > tool target > path |
| Recency | Ties are broken by recency, so an old exact title beats a new mid-text match |
| No operators | No `AND`, `OR`, quotes, or wildcards. A person typing a task description should not have to learn a query language. |
| Phrases work anyway | Typing several words matches them in sequence. It is a substring search, and that is enough. |
| Nothing found | "Nichts gefunden für „{query}“." with the scope stated and a link to search the other scopes |

**The deliberate absence of operators is the most important decision in this document.** A search box that fails to understand an ordinary phrase is worse than one that has no advanced mode, because the user does not know what they are missing. A substring search over a few thousand items is fast enough that operators buy nothing.

## Recent and saved searches

| Feature | Behaviour |
|---|---|
| Recent | The last six queries per screen, offered when the field is empty. Removable individually. |
| Saved | Not implemented. Six recents cover a person's actual pattern of looking for the same kind of thing twice. |

## The result list

Results are grouped by kind, and the groups are ordered by where the match is most likely to be what the user wanted.

```
Unterhaltungen · 3
────────────────────────────────────────
▸ Prüfung schlägt fehl
  claude-code-android · 14. Sep
  …bei der Prüfung schlägt der Test fehl, weil…

Projekte · 1
────────────────────────────────────────
▸ android-test-utils
  privat · mert/android-test-utils

Skills · 1
────────────────────────────────────────
▸ gradle-test-helper
  Global · Hilft bei Gradle-Testproblemen
```

| Property | Behaviour |
|---|---|
| Grouping | By kind, with a count per group. This is what tells the user where to look. |
| A result row | The matched text in context, the containing thing, and when |
| Tapping | Navigates to it, with the matched text scrolled into view and briefly highlighted |
| Highlighting after navigation | A `slow` fade, not a permanent highlight. A permanent highlight makes the next search ambiguous. |
| A run result | Tapping opens the run, scrolled to the matching log entry or tool call |
| A file result | Tapping opens the file at the matching line |
| A path result | Tapping opens the file browser at that path |

## Scoped content search

Searching inside a project's files is separate, deliberate, and slower, because it is a real scan.

| Aspect | Behaviour |
|---|---|
| Trigger | Explicit. "In Dateien suchen" inside the project, or the composer's `@` with a mode toggle. Never automatic. |
| Warning | For a project over 5.000 text files: "{n} Dateien werden durchsucht. Das kann {minutes} dauern." with the option to offload to a runner. |
| Progress | A progress line with the count scanned, cancelable, and the partial results kept |
| Cap | 2.000 results, then it stops and says so. A list of 40.000 matches is not a result. |
| Binary files | Skipped, with the count reported |
| Files over 1 MB | Skipped for content, still matched by name, with the count reported |
| `.gitignore`d files | Skipped, and the count reported. Searching a build directory for 40.000 generated files is not a search. |
| The index | Not persisted. A content index would be a copy of the user's code in a database nobody asked for, and it would go stale on every run. |

**No persisted content index.** This is a privacy decision with a performance cost, and the cost is accepted: a scan takes minutes instead of milliseconds, and in exchange no copy of the user's source code sits in a search database that would need its own invalidation, its own backup policy, and its own place in the privacy documentation.

## Performance

| Concern | Approach |
|---|---|
| The message index | SQLite FTS, maintained on every write, so a streaming message is searchable as it arrives |
| The path index | An in-memory trie built from the project tree, rebuilt when the tree changes |
| Ranking | Computed at query time over the candidate set. FTS gives candidates; the ranking is ours. |
| A large conversation set | 10.000 conversations: the query stays under 50 ms, asserted by a benchmark test with that fixture |
| Debounce | 200 ms, with the previous query cancelled |
| Cancellable | Every query is a cancellable coroutine; a new query cancels the last |
| No network | Guaranteed. A test asserts that a search issues zero network calls. |

## What the search is not

| Not | Reason |
|---|---|
| A semantic or embedding search | The content is code, transcripts, and German. A vector search would add a model, a cost, and a network call to a feature that works perfectly well with a substring. |
| A regex mode | Same reasoning as no operators. |
| Cross-device | There is no server. |
| A knowledge base | It searches what the app has, not what the internet has. |
| Searching the device | This is a coding tool. It searches the user's projects, not their photos. |

## Accessibility

| Requirement | Implementation |
|---|---|
| The search field | A labelled field, with a clear button and a labelled search icon |
| Results | A list, grouped by kind, with the group headers as headings so a screen reader can jump between kinds |
| A result row | "{kind}, {title}, {context}, {when}" with the match position stated: "Treffer im Text" |
| Result count | Announced after a search completes: "12 Treffer in 3 Bereichen" |
| Live region | The result count is polite. The results themselves are not announced item by item, because a search typed quickly would produce a stream of speech. |
| The empty state | The query is read back, so a voice user knows what was searched for |
| Highlighting after navigation | Announced: "Treffer hervorgehoben, Zeile {n}" |
| Font scale | Results wrap to three lines; the matched text is never truncated to keep a highlight on one line |
| Target | 48 dp result rows, the clear button 48 dp |

## Testing

| Test | Type |
|---|---|
| `SearchAllKinds` | UI — a query matches in every kind and the groups appear with the right counts |
| `SearchDiacritics` | Unit — "grosse" finds "große", "Strasse" finds "Straße", both directions |
| `SearchRanking` | Unit — a title match outranks a mid-body match, and a tie is broken by recency |
| `SearchDebounce` | UI — typing 10 characters produces one query, not ten |
| `SearchCancellation` | Integration — a slow query is cancelled by a new one and does not deliver a stale result |
| `SearchNoOperators` | UI — typing `AND` or `"` is treated as literal text and returns results rather than an error |
| `SearchEmptyState` | Screenshot with the query and the scope |
| `SearchNavigationHighlight` | E2E — tapping a result navigates, scrolls to the match, highlights, and the highlight fades |
| `SearchNoNetwork` | Integration — a search issues zero network calls, asserted |
| `SearchPerformance` | Performance — 10.000 conversations, query under 50 ms |
| `ContentSearchWarning` | UI — a 6.000-file project produces the warning with a count and an offload offer |
| `ContentSearchCaps` | E2E — 3.000 matches caps at 2.000 and says so |
| `ContentSearchSkips` | E2E — binary, over-1-MB, and ignored files are skipped and each count is reported |
| `ContentSearchNoIndex` | E2E — after a run modifies a file, a new search finds the new content without an index rebuild step |
| `SearchAccessibility` | UI — group headers are headings, the result count is announced, highlighting is announced |
| `SearchRecent` | UI — six recents, individually removable, per screen |
