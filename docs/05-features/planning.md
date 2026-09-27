# Planning

What the agent intends to do, shown before it does it, editable until that stops being possible.

## Why a plan exists

Three reasons, all of them about the user rather than about the agent:

1. **A wrong direction is free to change before the first edit and expensive afterwards.** Showing the plan lets somebody redirect in thirty seconds instead of finding out in a diff.
2. **A long run needs a visible shape.** Somebody who left for an hour comes back and needs to know not "what happened" but "how far along is it, and is the next step the one I was worried about".
3. **Acceptance criteria are the input to verification.** A step that says "add tests" cannot be checked. A step that says "add tests, they must pass" can.

## Producing the plan

The planner asks the engine for a structured plan, using the JSON-schema output mode.

```
claude -p "Erstelle einen Plan für: {aufgabe}" \
  --output-format json --json-schema '{...}'
```

The schema asks for steps with a title, an acceptance criterion, and whether the step is a checkpoint. The result is validated against the schema before it is shown; an invalid plan is retried once and then reported as a planner failure, not displayed half-formed.

| Field | Type | Why |
|---|---|---|
| `title` | String | Shown in the list, 1 line, in the user's language |
| `acceptance` | String | What "done" means for this step. Fed to the verifier and shown in the UI. |
| `isCheckpoint` | Boolean | A point where the run pauses, per `AUTO_WITH_CHECKPOINTS` |
| `estimate` | String, optional | A rough guess, shown but never trusted |

## Presentation

```
Plan · 4 Schritte · Schritt 3 läuft                    Bearbeiten
──────────────────────────────────────────────────────────
✓ 1  Prüfbefehle aus dem Projekt erkennen        erledigt · 4 s
✓ 2  Prüfbefehle im Code konfigurierbar machen  erledigt · 1 Min
⟐ 3  Tests für die neuen Befehle schreiben       läuft · 45 s
   ⌐ 4 Tests müssen die neuen Befehle abdecken
⊟ 4  Alles bauen und prüfen                      wartet
   ⌐ 4 / 5 Prüfbefehle müssen bestehen
```

| Element | Rule |
|---|---|
| Header | The step count and the current step. Always visible when collapsed. |
| A completed step | A `✓`, struck through in `textTertiary`. **Never removed** — a plan that erases its history cannot be reviewed afterwards. |
| The active step | The mark's state follows it |
| An active step's criterion | Shown indented below, in `bodySmall`. This is the promise being kept right now. |
| A checkpoint step | A `⊟` glyph and the caption "Prüfpunkt" |
| A failed step | A `✕` in `danger`, with the failure reason and the attempt count |
| Collapsed | One line: "Plan · 4 Schritte · Schritt 3 läuft" |
| The retry counter | On the step that failed, and in the header when the run is in `RETRYING` |

## Editing before approval

At `AUTO_WITH_CHECKPOINTS` and above, the plan pauses after it is produced. The plan is editable.

| Action | Behaviour |
|---|---|
| Reorder | Drag, with a haptic per position change |
| Retitle | Inline |
| Edit the acceptance criterion | Inline, multi-line |
| Remove a step | A minus button, with a confirm naming the step. Removing a step from a plan is not deleting anything; it is changing an intention. |
| Add a step | At any position, with a title and a criterion |
| Toggle a checkpoint | Any step can become one |
| Approve | Primary: "Plan freigeben" |
| Ask for a different plan | "Anderen Plan" — re-runs the planner with the user's feedback as an additional instruction |

A change to the plan is announced: "Schritt 2: „{new}“". The log records every edit with its before and after, because a plan somebody edited and the agent then followed is worth being able to reconstruct.

**The plan is a proposal, not a contract.** The agent may discover during the run that a step is wrong. When it does, it proposes a change rather than silently diverging, and the change appears in the plan as a new step marked "vorgeschlagen". At `ASK_EVERYTHING` and `ASK_RISKY`, a proposed plan change is a permission request. At the levels above, it is a checkpoint.

## Plan and verification

Acceptance criteria are not decoration; they are the input to judging.

| Rule | Detail |
|---|---|
| A criterion is shown to the user, always | Next to its step, during the run |
| A criterion is given to the verifier | As context for what the step should achieve |
| The verdict is stated per step | A completed step shows ✓, and the run summary links a step to the evidence that supports it |
| A criterion the verifier cannot evaluate | Said plainly: "Nicht automatisch prüfbar" on the step, rather than a green tick that means nothing |
| A step with a weak criterion | The planner is told to prefer a checkable criterion. "Verbessert den Code" is rejected by a validation pass, which asks again. |

A criterion the app cannot check is displayed as unchecked even when the agent considers the step done. This is the same principle as `UNVERIFIED`, applied per step: "the agent says this is done" and "this is verified" are different claims, and only the second one gets a tick.

## States

| State | Behaviour |
|---|---|
| No plan yet | Nothing. The run header says "Plant". |
| Planning | The mark is `THINKING`. After 30 s, a line: "Plant seit 30 s" so a slow plan is not a hang. |
| Plan ready | The card appears with a `standard` animation |
| Plan invalid from the engine | Retried once, then: "Der Plan konnte nicht erstellt werden." with a "Ohne Plan starten" secondary action. The run can proceed without one, because refusing to work without a plan would break a legitimate short task. |
| Empty plan | A warning: "Der Plan ist leer." never a blank card |
| A step is edited | Announced in the log with the before and after |
| A step is added mid-run | Marked "hinzugefügt", and the current step index does not renumber the completed ones |
| A step is removed mid-run | Only a pending step can be removed. A completed one is struck through and kept. |
| A step fails | `✕`, the failure reason, the attempt count, and the run enters `RETRYING` |
| All steps done but verification failed | The plan is complete and the run is not. This is shown as two separate facts, because a completed plan is not a successful task. |
| A very long plan, over 15 steps | Collapsed to the current step plus the next two, with "12 weitere" |
| The user never approved | At levels where the plan pauses, the run waits indefinitely, with a notification. It does not time out into approval. |

## Plan and the autonomy level

| Level | Plan behaviour |
|---|---|
| `ASK_EVERYTHING` | The plan is shown and the run waits for approval, in addition to every other permission |
| `ASK_RISKY` | The plan is shown but does not pause. Editing is not offered; the run proceeds. |
| `AUTO_WITH_CHECKPOINTS` | The plan pauses for approval, and the plan-change proposals are checkpoints |
| `FULL_AUTO` | The plan is shown and does not pause. Changes are applied and shown as proposed. |

A user at `ASK_RISKY` who wants plan approval has `AUTO_WITH_CHECKPOINTS`. That is what the level is for, and the sheet for the level says so.

## The plan in the log and the commit

| Where | What |
|---|---|
| The activity log | Every plan event: produced, edited, step changed, approved, changed mid-run |
| The commit message | The plan's steps, as a body, so the history records what was intended |
| The run summary | The plan with its final states, and each step's outcome |
| The pull request body | The plan as a checklist, with the checkboxes reflecting verification, not the agent's belief |

The pull request body is the one that matters long-term. Somebody reviewing the change in six months gets the plan as a checklist where each box is ticked only if something verified it.

## Testing

| Test | Type |
|---|---|
| `PlanParsing` | Unit — valid, invalid, empty, and schema-violating planner outputs |
| `PlanInvalidRetriesOnce` | Unit — one retry, then the planner-failure path |
| `PlanEmptyWarns` | UI — an empty plan renders a warning, not a blank card |
| `PlanPauseByLevel` | E2E — each level's pause behaviour, exactly as the table |
| `PlanEditing` | E2E — reorder, retitle, edit the criterion, remove, add, toggle a checkpoint, and every change is in the log with before and after |
| `PlanNoTimeoutIntoApproval` | E2E — an unapproved plan waits indefinitely, and never proceeds by itself |
| `PlanStepVerification` | E2E — a step whose criterion cannot be checked shows "Nicht automatisch prüfbar" and no tick |
| `PlanCriterionToVerifier` | Integration — the criterion reaches the verifier as context |
| `PlanChangeMidRun` | E2E — at `ASK_RISKY` a proposed change asks; above, it is applied and shown as proposed |
| `PlanLong` | Screenshot with 18 steps, asserting the collapse behaviour |
| `PlanInCommitAndPr` | E2E — the plan appears in the commit body and the PR checklist reflects verification |
| `PlanIncompletePlanFailsTask` | E2E — all steps complete but verification failed shows both facts, separately |
| `PlanPlannerSlow` | Screenshot at 30 s, asserting the "Plant seit 30 s" line |
