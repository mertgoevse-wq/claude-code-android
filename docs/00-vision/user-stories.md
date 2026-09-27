# User stories

Each story: **as a** / **I want** / **so that**, with acceptance criteria that can be turned into a test. Stories are grouped by the journey they belong to, not by screen, because most of them cross screens.

## Journey 1 — First run

| ID | Story | Acceptance |
|---|---|---|
| US-1.1 | As a first-time user, I want the app to explain in plain German what it will do to my phone before it does anything, so I can decide with real information. | The onboarding screen lists: downloads ~500 MB, installs a Linux runtime and the Claude Code binary, makes network calls to my AI provider and to GitHub. It names my provider only after I enter it. No network call happens before I tap the start button. |
| US-1.2 | As a first-time user, I want one button that sets everything up, so I am not copying commands between apps. | Tapping "Runtime einrichten" runs the whole bootstrap. Progress is visible per step. Leaving the app and returning resumes, it does not restart. |
| US-1.3 | As a first-time user, I want to paste my key and see immediately that it works, so I know before I start a project. | After pasting, a connection test runs automatically. It reports one of: key valid, key rejected, URL unreachable, model not found — each with a specific next action. |
| US-1.4 | As a first-time user, I want to protect the app with my fingerprint, so a borrowed phone does not expose my keys. | Biometric prompt on launch and on wake after the configured grace period. A working PIN or device-credential fallback exists. |
| US-1.5 | As a first-time user, I want to skip GitHub for now, so I can try the app before creating anything. | "Später" is available and equal in visual weight to the primary action. The app is fully usable for a local project with no GitHub account. |

## Journey 2 — A one-command task

| ID | Story | Acceptance |
|---|---|---|
| US-2.1 | As a user, I want to type an order and press one button, so that I do not have to configure anything per run. | From the chat screen: type, pick a project (or accept the remembered one), press send. The run starts without further questions at `ASK_RISKY` or above. |
| US-2.2 | As a user, I want to see the plan before the work starts, so I can stop a wrong direction early. | The plan appears within 30 seconds of sending. Each step has a name and an acceptance criterion. I can edit a step, reorder, remove, or add one before approving. |
| US-2.3 | As a user, I want to see what it is doing right now, so I know it is not stuck. | Every tool call appears as a card within 1 second of starting, labelled in plain language ("Datei gelesen: build.gradle.kts"). A running tool shows elapsed time. |
| US-2.4 | As a user, I want the interface to stay responsive while it works for hours, so my phone remains usable. | Streaming text never blocks input. The composer stays enabled. The list scrolls without dropping frames while output streams. |
| US-2.5 | As a user, I want to stop it, so I can change my mind. | The stop button is always reachable during a run. Within 3 seconds the process tree is gone, partial work is committed on a `wip/` branch, and the state is saved. I can resume or abandon. |
| US-2.6 | As a user, I want to know what it cost, so I am not surprised by the bill. | A running total is visible during the run. The final cost, token counts, and session ID are in the summary. |

## Journey 3 — Reviewing and accepting

| ID | Story | Acceptance |
|---|---|---|
| US-3.1 | As a user, I want to see exactly what changed, so I can judge the work. | The diff viewer shows every changed file, unified or side by side, with syntax highlighting, and offers per-hunk accept and revert. |
| US-3.2 | As a user, I want to see whether it actually worked, so a plausible answer is not mistaken for a result. | The verification panel lists every command run, its exit code, and the pass/fail. A run that did not verify is labelled unverified and never says "fertig" without qualification. |
| US-3.3 | As a user, I want the finished work in GitHub without me touching git, so that nothing is lost. | After a green run: a branch, a commit, and a pull request exist, and I see the link. If any of these failed, I see which step and why. |
| US-3.4 | As a user, I want to decline a change without losing the rest, so I can keep the good parts. | Per-hunk revert produces a clean state. Reverting everything returns the branch to its pre-run commit without deleting history. |

## Journey 4 — Self-healing

| ID | Story | Acceptance |
|---|---|---|
| US-4.1 | As a user, I want it to fix its own failures, so I do not have to read stack traces. | After a failed verification, the app diagnoses, fixes, and re-verifies. The attempt counter is visible throughout. |
| US-4.2 | As a user, I want to decide how persistent it is, so I control the cost and the risk. | The retry budget is set per project: 3, 10, 50, or unlimited. The setting is one tap from the project screen. |
| US-4.3 | As a user, I want a clear stop instead of an infinite loop, so I am not billed for nothing. | When the budget is exhausted, the run ends, a plain-language report names what failed and what was tried, and I am notified. Anti-loop detection aborts a step that repeats without progress. |

## Journey 5 — Projects and repositories

| ID | Story | Acceptance |
|---|---|---|
| US-5.1 | As a user, I want to add a private repository, so the app works on my real code. | Add by URL or by picking from my GitHub account. Cloning shows progress. The privacy indicator is visible on the project card and is never wrong. |
| US-5.2 | As a user, I want to sign in to GitHub once, so I do not manage tokens by hand. | OAuth is the default path and explains what access it requests. A token path exists for people who prefer it, with a scope checklist and a validation button. |
| US-5.3 | As a user, I want per-project autonomy, so a scratch project does not demand my attention. | Four levels, one tap, applied to new runs immediately. The current level is visible on the project card and in the chat composer. |
| US-5.4 | As a user, I want a full history, so I can find what happened last month. | Every run is listed with date, project, task, outcome, cost, and link to its PR. Filterable by project and outcome. |

## Journey 6 — Skills

| ID | Story | Acceptance |
|---|---|---|
| US-6.1 | As a user, I want to install a skill from a GitHub URL, so I can reuse something that already exists. | Paste a URL. The app detects the skill, shows the full content, shows the files it will write, and asks once. |
| US-6.2 | As a user, I want to describe a skill and have Claude write it, so I do not have to learn the format. | A guided conversation produces a valid skill, shown for editing before saving. Validation errors are explained, not dumped. |
| US-6.3 | As a user, I want to know which skills are active and where, so I know what is shaping my work. | Each skill shows its scope (global or project), its state, and which projects it affects. Broken skills are visible with a repair action. |

## Journey 7 — Remote and self-improvement

| ID | Story | Acceptance |
|---|---|---|
| US-7.1 | As a user, I want long jobs to survive my pocket, so I can walk away. | With the screen off and the app backgrounded, the run continues and I receive a notification when it finishes or fails. |
| US-7.2 | As a user, I want to move a heavy job to a free server, so my phone does not have to do it. | A remote runner can be added (Oracle Always Free, GitHub Actions, or a home PC). A run can be moved, and it streams back identically. |
| US-7.3 | As a user, I want updates, so the app does not rot. | The app checks for a new version, shows what changed, and installs on my confirmation. It never installs silently. |
| US-7.4 | As a user, I want to report a crash with the logs attached, so a bug can be fixed. | A diagnostics export produces a redacted bundle: logs, config, environment. No key ever appears in it. |

## Journey 8 — Honesty

| ID | Story | Acceptance |
|---|---|---|
| US-8.1 | As a user, I want a complete activity log, so I can audit what happened. | Every command, diff, permission decision, error, and cost is in an append-only log, filterable, and exportable. Entries cannot be edited or removed. |
| US-8.2 | As a user, I want to see the raw terminal output too, so nothing is summarised away. | The terminal pane shows the unprocessed stream next to the summarised cards. |
| US-8.3 | As a user, I want the app to refuse dangerous actions, so a mistake is not destructive. | Deletion, spending, publishing, and default-branch pushes are refused with a plain-language reason, at every autonomy level, with no override. |
