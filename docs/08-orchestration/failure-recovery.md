# Failure recovery

What the app does when something breaks, at every level, and the difference between recovering, resuming, and giving up.

## The three recoveries

| Recovery | Meaning | When |
|---|---|---|
| **Retry** | The same thing again, after a fix | A fixable failure, within the budget |
| **Resume** | The same run, continued, from where it stopped | An interruption: the process died, the app was killed, the device rebooted |
| **Restart** | The same task, from the beginning, on a new branch | A run that failed in a way that retrying cannot address |

They are different, and conflating them is how a tool loses a user's work. A retry continues the conversation. A resume continues the conversation. A restart does not, and says so.

## The failure inventory

| Failure | Where | Recovery | Notes |
|---|---|---|---|
| The provider is overloaded | Engine | Retry, with the server's own delay | Surfaced as "Versuch 2 von 5" from the `api_retry` event, not from our own counter |
| The provider is rate limited | Engine | Retry, after the indicated delay | Never a tight loop; that is how a rate limit becomes a ban |
| Authentication failed | Engine | **Stop** | Nothing will fix it without a key change |
| Out of credit | Engine | **Stop** | A fact, not a transient condition |
| The model does not exist | Engine | **Stop**, with the available models | |
| A build failed | Verification | **Retry**, per `05-features/retry-and-self-healing.md` | The main case the app is built for |
| A test failed | Verification | Retry | |
| A lint error | Verification | Retry | |
| A tool is missing | Verification | **Stop**, with an install offer or a runner | Retrying a missing Gradle fixes nothing |
| A permission is pending | Orchestration | Wait, indefinitely | Never times out into an answer |
| The engine process died | Process | **Resume** | The session is intact |
| The app was killed | Process | **Resume**, offered | Never silent; a silent restart can duplicate a commit |
| The device rebooted | Process | **Resume**, offered | |
| The app was updated | Process | **Resume**, offered | |
| The network dropped | Either | **Resume** for a local run; a retry for a request | Never counted as a failure for a local run |
| No network at all | Either | **Resume** when it returns | The run is `INTERRUPTED`, not `FAILED` |
| Out of storage | Environment | **Stop**, with the exact shortfall | Never deletes anything to make room |
| Out of memory | Environment | Interrupt the newest run, with a reason | Stated which one and why |
| Thermal throttling | Environment | Continue, slower | The elapsed time shows it |
| The battery died | Environment | **Resume** | The working tree is intact |
| A host key changed | Runner | **Stop**, loudly | Possibly a new server, possibly an attack. Both fingerprints shown, a typed confirmation required. |
| A runner is offline | Runner | **Stop**, with a link | Never falls back to the phone silently |
| A runner's quota is exhausted | Runner | **Stop**, with the date and an alternative | |
| A push was rejected | Git | Retry on a transient rejection; **stop** on a permission problem | |
| A branch already exists | Git | **Stop**, with a different name offered | Never force-replaced |
| A merge is in progress | Git | **Stop**, with a terminal link | Applying edits into a half-resolved state is how work is lost |
| The default branch would be pushed to | Git | **Stop** | A hard block, not a recoverable error |
| GitHub is rate limited | GitHub | Wait until the reset, and say when | |
| The GitHub token expired | GitHub | **Stop**, with the sign-in screen | A refresh is attempted once first |
| The repository is not visible | GitHub | **Stop**, naming the likely cause | A 404 is not reported as "deleted" |
| A skill is invalid | Skills | **Stop** that skill; the run continues | A broken skill is visible, not silently skipped |
| A skill install conflicts | Skills | **Stop**, with a per-file choice | Never a silent overwrite |
| A session was not found | Session | **Restart** as a new conversation with the transcript as context | The old file is left on disk |
| A database write failed | Data | **Stop** | A run with an incomplete transcript is not trustworthy |
| A log write failed | Data | Retry once, then continue | A missing log entry is bad, not fatal |
| An event could not be parsed | Mapping | Record raw, skip, **continue** | One bad line must not lose an hour |
| A checkpoint timed out | Orchestration | Continue after the countdown | The countdown is visible |
| The retry budget is exhausted | Orchestration | **Stop**, with a report | |
| The anti-loop rule fired | Orchestration | **Stop**, with a report | Regardless of the budget |
| A memory leak in the app | App | The crash handler, and a local crash report | |

## What counts as a retry

| Counts | Does not count |
|---|---|
| A verification attempt after a fix | A successful turn |
| The `api_retry` event from the engine | The app's own re-sending of a request, which it does not do |
| A retry after a transient push rejection | A user-initiated "Nochmal" |
| — | A manual "Verifizieren", which is not a retry |
| — | A resume, which continues rather than retries |

**The retry budget is per failure type.** A compile error fixed and then a test failure is two budgets, because they are two different problems. A budget shared across types would exhaust on a project that is making progress.

## What does not count as a failure

| Event | Why it is not a failure |
|---|---|
| A run with no changes | There was nothing to do. Shown as an answer, not as a failure. |
| A verification that was never run, because there were no changes | Same |
| An unverified run | A legitimate outcome for a project without checks, honestly labelled |
| A denied permission | A decision, recorded, and the run continues |
| A refused hard-block action | A refusal, recorded, and the run continues |
| A skill that was already installed | A no-op, shown as such |
| A provider that did not support model discovery | Not supported, not broken |
| The app being killed by Android | An interruption, resumable |
| A stale probe on a runner | A cache miss, re-probed |

## The escalation ladder

```
1. Retry, within the budget, if the failure is classified fixable
2. Retry, with an increased backoff, if the provider said to
3. Wait, indefinitely, if a human is the bottleneck
4. Stop, with a specific reason and an action, if retrying cannot help
5. Stop, with a report, if the budget is exhausted
6. Stop, with a report, if the anti-loop rule fires
7. Offer a resume, if the run was interrupted rather than failed
8. Offer a restart, if a resume cannot continue
```

There is no step 9. A run that reaches step 8's offer and is not taken is left as `FAILED` or `INTERRUPTED`, with its record, its log, and its diff intact.

## Backoff

| Source | Delay |
|---|---|
| The engine's `api_retry` | The server's `retry_delay_ms`, exactly. The app does not second-guess it. |
| The app's own connection attempts | 2 s, 5 s, 15 s, 45 s, then every 2 minutes, up to 10 attempts |
| A push rejection | 3 s, 10 s, 30 s, then a stop. GitHub rejects transiently for a short window, and a longer one is a real problem. |
| A runner that is offline | 30 s, 2 min, 10 min, then a stop and a notification |
| A verification failure | Immediate. There is nothing to wait for: a build does not become passing by waiting. |

Jitter is applied to the app's own backoff, ±20 %, so several devices do not retry in lockstep. The engine's delay is used exactly, because the server chose it.

## The report

Every terminal failure produces one. This is the artefact somebody reads a week later.

```
Fehlgeschlagen nach 3 Versuchen
──────────────────────────────────────────────────────────
Aufgabe
  Unterstützung für benutzerdefinierte Prüfbefehle

Fehler
  ./gradlew test → Exit 1
  ProjectRepositoryTest.shouldReturnProjectByName:87
  expected:<true> but was:<false>

Klassifikation
  TestFailure  ·  nicht behoben

Versuche
  1  Repository-Abfrage gelesen, Test gelesen. Keine Ursache gefunden.
  2  Mock angepasst. Test schlägt weiterhin an derselben Stelle fehl.
  3  Gleicher Fehler. Abbruch nach Regel "gleicher Fehler dreimal".

Was vermutlich fehlt
  Der Test erwartet true, aber getProjectByName gibt null zurück,
  wenn das Repository leer ist. Der Test setzt keinen Seed.
  Das ist vermutlich ein Fehler im Test, nicht im Code.

Nächster Schritt
  Im Test vor der Abfrage einen Seed einsetzen.
  ProjectRepositoryTest.kt:82

Änderungen (7 Dateien, +48 −12)
  [Diff ansehen ]

Kosten         0,94 $  ·  3 Versuche
Zweig          task/verify-commands  ·  Commit a3f19c2
Lauf           01HQ8X2M4K7P…
Gerät          Dieses Gerät, Profil Native
Autonomie      ASK_RISKY
```

| Rule | Detail |
|---|---|
| The task, verbatim | |
| The error, with the parsed detail | Not the raw wall of output; the full output is one tap away |
| The classification | So a person can see whether the app understood the problem |
| Every attempt, with what it did | A list of actions, not "tried 3 times" |
| A labelled inference | "Was vermutlich fehlt", explicitly a guess |
| A next step, when one can be inferred | A file and a line |
| The changes | So the work is not lost even though the run failed |
| The cost | Somebody paid this |
| The identifiers | The run, the branch, the commit, the device, the profile, the level |

**A report that only says "it failed" is a failed report.** The whole point of collecting the classified error, the per-attempt actions, and the inferred cause is that this is the artefact that makes the next attempt cheap.

## Recovery actions, offered where they apply

| Situation | Offered |
|---|---|
| An interrupted run | Fortsetzen, Verwerfen (ends the turn, keeps the branch) |
| A failed run, retryable | Nochmal (a new run, same task, new branch), with the previous one intact |
| A failed run, missing tool | Toolchain einrichten, Auf Server ausführen |
| A failed run, provider error | Anbieter wechseln, Schlüssel prüfen |
| A failed run, host key changed | Nichts. It needs a human to think about it. |
| A failed run, GitHub | Verbindung prüfen, Token erneuern |
| Budget exhausted | Budget erhöhen, Verlauf ansehen, Als Aufgabe neu starten |
| Anti-loop fired | Verlauf ansehen, Als Aufgabe neu starten, Problem melden |

"Verwerfen" appears exactly once in the whole app, in the interrupted-run case, and it ends a turn rather than deleting anything. Everywhere else, "Verwerfen" is not offered.

## Crash recovery in the app itself

The app crashing is a different class of problem from a run failing.

| On next launch | Behaviour |
|---|---|
| A run in a non-terminal state | Check whether its backend is alive, then offer a resume |
| A bootstrap in progress | Resume the state machine |
| A download in progress | Resume, with `Range` if the server supports it |
| A verification in progress | Mark it `INTERRUPTED`. Never resumed automatically, because a build that was interrupted may have left artefacts. |
| A terminal session | Available in the session list, with its scrollback |
| A pending permission | Preserved, and the notification is repeated. The user must still answer it. |
| An unsaved editor draft | Preserved in a draft file, and restored |

**A pending permission surviving a crash is the important one.** If it were dropped, the run would be stuck with no way to continue, and the person would not know why. It is preserved in the database, not in memory, precisely so this cannot happen.

## Testing

| Test | Type |
|---|---|
| `InventoryComplete` | Unit — every `AppError` in `02-architecture/error-taxonomy.md` maps to exactly one recovery in this document |
| `RetryClassification` | Unit — every failure fixture maps to retry, resume, or restart, exhaustively |
| `RetryBudgetPerType` | Unit — fixing one failure type does not consume another's budget |
| `BackoffSchedule` | Unit — the app's own backoff is 2/5/15/45/120 s with ±20 % jitter, asserted with a fake clock |
| `EngineDelayUsedExactly` | E2E — a provider's `retry_delay_ms` is honoured without modification |
| `NoSelfRetryLoop' | Static — the provider client has no retry, asserted, so two retry layers cannot multiply |
| `WaitIsIndefinite' | E2E — a pending permission waits 24 hours and is never resolved by the app |
| `StopNotRetry' | E2E — a missing toolchain, an auth failure, and an exhausted quota each stop after one attempt |
| `NoProgressIsNotFailure' | E2E — a run with no changes completes without a failure and without a green verification tick |
| `RefusalIsNotFailure' | E2E — a denied permission and a refused block both leave the run running |
| `EscalationLadder' | Unit — the ladder has exactly eight steps and no run can skip one |
| `ReportContainsEverything' | E2E — the report contains every required section, for each failure type |
| `ReportInferenceLabelled' | Unit — the inference section is titled as a guess and never asserts certainty |
| `InterruptionResume' | E2E — killed at each state, relaunched, offered a resume, and the resume continues the session |
| `NoSilentRestart' | E2E — nothing restarts without a tap |
| `NoDuplicateCommit' | E2E — a resumed or restarted run does not produce a second commit on the same branch |
| `VerificationInterrupted' | E2E — a verification interrupted by a crash is `INTERRUPTED` and never auto-resumed |
| `PermissionSurvivesCrash' | E2E — a pending permission is still pending after a crash, with the notification repeated |
| `DraftSurvivesCrash' | E2E — an unsaved skill editor draft is restored |
| `MalformedLineDoesNotFail' | E2E — a corrupt line mid-stream leaves the run healthy |
| `HostKeyChangeLoud' | UI — both fingerprints are shown and a typed confirmation is required |
| `OfflineRunnerDoesNotFallback' | E2E — an assigned, offline runner refuses rather than using the phone |
| `AntiLoopRegardlessOfBudget' | E2E — an unlimited budget does not prevent the anti-loop abort |
| `VerwerfenOnlyOnce' | UI — the word appears in exactly one place in the whole UI, asserted by a scan of the string resources |
| `CrashesRecorded' | E2E — a crash produces a signature, a count, and an offer to open a repair task |
