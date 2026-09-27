# Screen 02 — Chat list

Every conversation, grouped, searchable, with a new chat always one tap away.

## Purpose

Be the answer to "what was I working on?" in under two seconds, and be the launch point for the next task.

## Structure

### Top bar

| Position | Element |
|---|---|
| Left | The mark at 28 dp, `IDLE`, plus the app name in `titleMedium` |
| Centre | None |
| Right | A search icon button, and a "Neuer Chat" icon button |

No title, no back arrow. This is a root destination.

### Body

A scrolling list of conversations, grouped by recency.

| Group header | Contents |
|---|---|
| Heute | Conversations from today, newest first |
| Gestern | Yesterday's |
| Letzte 7 Tage | |
| Letzte 30 Tage | |
| Älter | Everything else, collapsed by month |

Group headers: `labelMedium`, `textTertiary`, `space7` above and `space3` below. A group with a single item still shows its header, because the date is information.

### Row

| Slot | Content | Rule |
|---|---|---|
| Leading (40 dp) | `StatusBadge` if the run is active or has a notable outcome, otherwise the project mark | Never both |
| Title | `bodyMedium`, one line, ellipsis | The conversation title |
| Subtitle | `bodySmall`, `textSecondary`, one line, ellipsis | The last message's first 90 characters, or the last tool summary if the last item was a tool |
| Trailing top | `labelSmall`, `textTertiary` | Time: "14:32" today, "Gestern", "12. Sep" this year |
| Trailing bottom | Cost, `labelSmall`, `textTertiary`, with `~` if estimated | Only when the conversation has cost |
| Badges | Autonomy chip, if not the project default | |

| Interaction | Behaviour |
|---|---|
| Tap | Opens the chat |
| Long press | A sheet: Umbenennen, Duplizieren, Archivieren, Exportieren. **No delete.** |
| Swipe | **Nothing.** No swipe actions in this app. |

The absence of swipe is deliberate and repeated here because it is the most tempting thing to add. See `03-design/anti-slop-rules.md` rule 20 and the no-swipe rule in `spacing-and-layout.md`.

### "Neuer Chat" behaviour

Tapping the icon, or the primary FAB on the empty state, opens the new-chat screen with the most recently used project preselected. A long press on the button, or a long press on the FAB, opens a project picker directly, skipping the preselection. Someone with six projects does not want the wrong one preselected five times.

## States

| State | Content |
|---|---|
| No conversations | `empty-chat.svg` at 96 dp, `titleMedium` "Noch keine Unterhaltungen", `bodySmall` "Wähle ein Projekt und beschreibe, was gebaut werden soll.", primary action "Neuer Chat" |
| Loading | Nothing. The list renders as soon as the first query returns, which is a local database read and takes under 20 ms. A spinner here would be theatre. |
| Searching | The list filters as you type. A conversation matches on title, on message content, and on project name. |
| Search, no results | "Keine Unterhaltung passt zu „{query}“." with a "Suche löschen" action |
| Search, searching | Results appear immediately; the index is local and complete |
| A run is active | The row shows a running badge, and the mark in the top bar goes to that run's state |
| Several runs active | The mark reflects the most recent; the row badges are per conversation |
| Offline | No difference. A local list works offline. Running a new one is blocked at the composer, with the reason. |
| Archive | A separate group at the end, collapsed, with "Archiv anzeigen" |

## The active-run banner

When at least one run is active, a single banner appears above the list, directly under the top bar:

```
⟐  claude-code-android · liest build.gradle.kts        vor 2 Min.
```

| Property | Value |
|---|---|
| Tap | Opens that chat |
| Background | `accentSubtle` |
| Text | Project, current tool, elapsed time — all live |
| Multiple runs | Shows the most recent, with a count: "und 2 weitere" |
| Dismiss | Not dismissible. It is a fact, not a notification. |
| Colour | `accentText`, so it does not compete with a `danger` badge below it |

This banner is the reason a user can close the app and still know something is happening. It is a persistent, glanceable status, not a toast.

## Search

| Property | Value |
|---|---|
| Fields | Conversation title, message content, project name, file paths mentioned |
| Trigger | The search icon, or a search gesture from the top bar |
| Presentation | The list filters in place. A separate results screen is not used. |
| Recent searches | Six, below the field, when there is history. Tap to reuse. Tapping the `×` on one removes it. |
| Matching | Substring, case-insensitive, diacritic-insensitive, so "grosse" finds "große" |
| Highlighting | Matches are bolded in the row, not coloured. Colour plus bold would be redundant. |
| No results | States the query and offers to clear it |

## What this screen does not do

| Absent | Reason |
|---|---|
| Delete | Nothing is deleted |
| Pin, favourite, or archive-on-auto | A rename is enough; a pin system is a settings system nobody asked for |
| Swipe actions | See above |
| A "sort by" menu | Recency is the only useful order for a work log |
| Conversation folders | That is what projects are for |
| Unread badges | Nothing is unread. A run that needs attention notifies you, and the row shows a status. An unread count is a guilt mechanic. |
| A count badge on the tab | The tab shows an active-run dot, not a number |

## Accessibility

| Requirement | Implementation |
|---|---|
| Group headers | Real headings, so TalkBack can jump between days |
| Row | One merged node: "Unterhaltung, {title}, {subtitle}, {time}, {status}" |
| Status | Announced as text, never as a colour |
| The active banner | A live region. It is the one place on this screen that updates while a run proceeds. |
| Mark | "Claude Code, keine Unterhaltung ausgewählt" |
| TalkBack navigation | 44 rows is about 60 stops on a long list. Group headers are the escape hatch. |
| Font scale 1.3 | The subtitle wraps to two lines, the trailing column stays one line, the row grows |
| Target | The whole row is one 48 dp-plus target; long press opens the sheet |

## Testing

| Test | Type |
|---|---|
| `ChatListEmpty` | Screenshot, both themes |
| `ChatListPopulated` | Screenshot, with 3 groups, 12 rows, badges, a long title, a 90-character subtitle |
| `ChatListSearch` | UI — typing filters, matching highlights, empty state, recent searches |
| `ChatListLongPress` | UI — the sheet has four actions and no delete |
| `ChatListActiveBanner` | UI — appears with one run, shows a count with three, updates its text, taps through |
| `ChatListFontScale` | Screenshot at 1.3 |
| `ChatListRotation` | E2E — scroll position and search query survive a rotation |
| `ChatListNoDelete` | E2E — asserts no delete affordance exists anywhere in the hierarchy |
