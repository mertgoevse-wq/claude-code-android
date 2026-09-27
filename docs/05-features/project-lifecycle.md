# Project lifecycle

From a folder to a project, through configuration, to archive. With one delete, in one place, under three confirmations.

## The states

| State | Meaning | Reachable by |
|---|---|---|
| `DETECTED` | Inspected, nothing saved | A preview before saving |
| `NEW` | Added, no run yet | The user |
| `READY` | Runtime and provider available | Automatic |
| `NEEDS_RUNTIME` | A toolchain the project needs is missing | Automatic, on a failed run |
| `NEEDS_PROVIDER` | No provider configured or the key fails | Automatic, on a connection test |
| `BUSY` | A run is active | Automatic |
| `DIRTY` | Uncommitted changes exist | Automatic, from git status |
| `DETACHED` | The working tree is on a detached head | Automatic |
| `BROKEN` | The path is gone, or git is inconsistent | Automatic |
| `ARCHIVED` | Hidden from the main list | The user |
| `AWAITING_ERASURE` | The erase was confirmed and is in progress | The user, three times |

The project card shows `NEEDS_RUNTIME`, `NEEDS_PROVIDER`, `DIRTY` and `DETACHED` as chips; `BUSY` as a running indicator; `BROKEN` as a `danger` badge. `ARCHIVED` moves the project out of the list. `DETECTED` never reaches a card.

## Adding

Three routes, specified in `04-screens/09-add-project.md`. The common tail:

### Inspection

Before anything is saved, the project is inspected and the result is shown.

| Check | Result |
|---|---|
| Path exists and is readable | Yes / no |
| Writable | Yes / no. If no, the project is refused up front — the app must never create a project it cannot write to. |
| Git repository | Yes / no, and if no, the "einrichten" offer |
| Working tree state | Clean, or {n} changed files, or detached |
| Default branch | The name, or "unbekannt" |
| Size | The repository size, and a clone-time estimate above 500 MB |
| Detected commands | The proposed verification commands |
| Available toolchains | What the phone can actually run |
| Missing toolchains | What the project needs and the phone lacks |

**A wrong folder is caught in the first second this way.** The alternative — discovering after a twenty-minute clone that the user picked the wrong directory — is the kind of error that makes an app unusable once.

### Setup

| Step | Detail |
|---|---|
| Name | Prefilled from the directory or repository name, editable |
| Path | Shown, editable for a local project, fixed for a clone |
| Backend | "Dieses Gerät" by default; a runner can be assigned now or later |
| Autonomy | `ASK_RISKY`, the global default |
| Model | The project default |
| Verification commands | The detected ones, or the user's |
| Skills | "Alle globalen Skills" by default |

### The first run's first act

On a project that has never run, the planner's first job is to read the project and confirm the verification commands actually work. A project whose detection produced `./gradlew test` but which has no Gradle wrapper fails in the first minute with "Prüfbefehl nicht gefunden: ./gradlew" and a link to the terminal — rather than twenty minutes later inside a build.

## Configuration

Everything in `04-screens/08-project-detail.md` Section 3. The lifecycle rules that are not obvious:

| Rule | Reasoning |
|---|---|
| Autonomy is per project, and changing it does not affect a running task | The run continues under the level it started with, and the log records which. |
| A project can have its own verification commands, overriding the detection | Detection is a guess; the user is not |
| A project can be assigned to a runner, or inherit the global offload policy | Flexibility without a setting per project for a global concern |
| Changing the model mid-project is allowed and is logged | A different model can produce a different result, and that must be explainable later |
| A project can be archived without losing anything | Archiving hides it. It is not deletion and it is reversible. |
| Archiving stops nothing | An archived project with an active run finishes its run |

## The working tree during a run

The rules that keep a run from destroying work, which is the main risk in an unattended coding tool.

| Situation | Behaviour |
|---|---|
| Clean tree | A branch is created from the current HEAD, the run proceeds |
| Uncommitted changes | A branch is created from the current state, the changes come along and stay uncommitted. The diff viewer shows them in a "nicht committet" section. The user decides. |
| Detached head | The run creates a branch at the current commit, so nothing depends on a commit nothing points at |
| On a branch with no upstream | Normal. The run pushes and sets the upstream on its own feature branch. |
| On the default branch | The run never pushes to it. It creates a branch first. |
| A merge or rebase in progress | The run stops: "Im Projekt läuft gerade ein Merge oder Rebase." with a link to the terminal. Continuing would apply edits into a half-resolved state. |
| Untracked files | Preserved. Never staged unless the run's commit explicitly includes them, and the diff shows them separately. |
| Ignored files | Never touched, never staged, never listed in the diff. `.gitignore` is respected exactly. |
| A `.gitignore` that ignores the app's own files | The app does not fight it. If a user's `.gitignore` excludes everything, the commit is empty and the app says so. |
| A file larger than 10 MB committed | The app does not prevent it. A hook may. The run's report notes it. |
| A submodule | Detected, reported, and not touched. The app does not recurse into a submodule without asking. |

## Archiving

| Aspect | Behaviour |
|---|---|
| Trigger | The user, from the project card or the detail screen |
| Confirm | "Archivieren? Das Projekt verschwindet aus der Liste. Nichts wird gelöscht. Der Ordner und die Historie bleiben." |
| What moves | Only the list membership. The project appears under "Archiv" in the project list. |
| What stays | The folder, the database rows, the conversations, the run history, the branches, the commits, the pull requests |
| An active run | Continues. The project is not archived out from under a running task. The run completes and its result is where it always was. |
| Reversible | "Wiederherstellen", which brings it back with its last-used date intact |
| The log | An `ARCHIVE` and a `RESTORE` entry |

## The one deletion

Named "Lokale Daten dieses Projekts löschen", in the project detail's danger zone and in Settings.

| Confirmation | Content |
|---|---|
| 1 | "Lokale Daten löschen? Der lokale Ordner, die Unterhaltungen und der Verlauf dieses Projekts werden entfernt." |
| 2 | "Nicht entfernt: das GitHub-Repository, alle Zweige, die Git-Historie, alle Commits und alle Pull Requests. Auch der Ordner auf einem Nachschubserver bleibt." |
| 3 | "Dieser Vorgang wird im Aktivitätsprotokoll vermerkt. Er kann nicht rückgängig gemacht werden." |
| 4 | A typed confirmation: the project name, character for character |

### What it removes

| Removed | Detail |
|---|---|
| The local working copy | The whole directory, only if it is inside the app's own storage or one the user explicitly selected for this project |
| Conversations | The app's transcripts, not the agent's session files |
| Run history, cost records, log entries for this project | |
| Skill installs scoped to this project | Not global ones |
| The project row | |

### What it never removes

| Preserved | Reason |
|---|---|
| The GitHub repository | It is the user's, and it lives on their account |
| Remote branches and commits | Pushed by the user, on their account |
| The pull request and its comments | On GitHub |
| Git history anywhere | History is not a local artefact |
| The app's global log entry for this action | An erasure that left no trace would be the opposite of the transparency rule |
| The agent's session files | Left on disk. Removing them is a deletion, and they are small. |
| Global skills | Not part of this project |

### Who can trigger it

**Only the user.** Never the agent, at any autonomy level, through any tool. There is no path from a run to this operation: it is a UI-only action, in a screen a run cannot navigate to, requiring a typed string the agent does not know.

A test asserts that no code path in the orchestration layer calls the erase function, and that the function's only callers are the settings screen's ViewModel.

### The directory question

This is the one genuinely hard part, so it is spelled out.

The app refuses to remove a directory it did not create, unless the user selected that directory explicitly as this project's folder. A directory the user picked in the system picker and named as their project is theirs, and removing it is a deletion of their work — so the confirmations are worded for that case, and the confirmation count is the same but the wording changes:

> "Dieser Ordner wurde von dir ausgewählt, nicht von der App angelegt. Beim Löschen wird der komplette Ordner mit {n} Dateien ({size}) entfernt. Das lässt sich nicht rückgängig machen."

A directory inside the app's own storage gets the normal wording.

A directory that is a git repository with a remote, or with unpushed commits, is refused outright:

> "Dieses Projekt hat Commits, die nicht hochgeladen wurden, oder hängt an einem Repository. Lösche es zuerst über GitHub, oder nutze die Funktion in einem Ordner der App."

The reason is not caution for its own sake. An app that can erase a directory containing somebody's unpushed work is a tool that will eventually do so at the wrong moment, and the three confirmations reduce that probability without ever reaching zero. Refusing in the one case where a mistake is unrecoverable for the user is the honest response.

## Project-level metrics

Shown on the project detail, because a project is where a person would look.

| Metric | Why |
|---|---|
| Runs and their outcomes | The shape of the work |
| Median and worst verification time | Whether the project is fast to check |
| Failure types, most common first | What to fix |
| Cumulative cost, and the last 30 days | The price of the work |
| Time spent in retry loops | Whether self-healing is earning its cost |
| `never-weakened` count | Should be zero. A non-zero value is a bug in the detector. |

## Testing

| Test | Type |
|---|---|
| `Inspection` | Integration — every fixture project yields the documented inspection result |
| `InspectionReadOnly` | Integration — a read-only directory is refused at add time, not at first run |
| `FirstRunCommandCheck` | E2E — a project with a detection that cannot run fails in the first minute with the specific error |
| `DirtyTreeSurvives` | E2E — uncommitted changes before a run are still uncommitted after, and appear in the diff |
| `DetachedHeadGetsBranch` | E2E — a detached tree gets a branch before the run |
| `MergeInProgressBlocks` | E2E — a run refuses to start during a merge, with a terminal link |
| `UntrackedAndIgnored` | E2E — untracked preserved, ignored never touched, `.gitignore` respected exactly |
| `SubmoduleNotTouched` | E2E — a submodule is detected, reported, and unmodified |
| `ArchivePreservesEverything` | E2E — archive, then restore, and assert every row, file, and commit is intact |
| `ArchiveDuringRun` | E2E — archiving mid-run does not interrupt it |
| `EraseThreeConfirmations` | E2E — three confirms and a typed name; cancelling at any step changes nothing |
| `ErasePreservesRemote` | E2E — after the erase, the GitHub repository, its branches, its history, and its PR all still exist |
| `EraseRefusesUserDirWithUnpushed` | E2E — a directory with unpushed commits or a remote is refused, with the reason |
| `EraseOnlyUserCanTrigger` | Unit — a static check that the erase function's only callers are the settings ViewModel, and that no orchestration code path reaches it |
| `EraseIsLogged` | UI — the action is in the activity log, and the log survives the erasure |
| `EraseLeavesAgentSessions` | E2E — the session files are still on disk afterwards |
