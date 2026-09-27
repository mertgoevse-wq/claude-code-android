# Subagents and parallelism

A second Claude Code inside the first one, and two runs at once. Both are supported; both are bounded.

## Subagents

### What they are

Claude Code can start another instance of itself to do a scoped job. The child has its own context, its own tools, and its own conversation. The parent sees the child's result, not its reasoning, unless forwarding is enabled.

### Rendering

| Aspect | Behaviour |
|---|---|
| Detection | A message with a non-null `parent_tool_use_id` |
| Nesting depth in the UI | **One.** Deeper is shown as a count: "3 Unteragenten". |
| The spawning tool card | Expands to contain the child's cards, indented 16 dp, with a collapsed state of its own |
| The child's tool calls | Full cards, with the same plain-language titles |
| The child's text | Shown when forwarding is available, in a collapsed card |
| The child's cost | Attributed to the same run, so the cost meter is correct |
| An unknown or unnamed child | "Unteragent", and its tools below. Never an empty card. |

**One level, always.** A hierarchy the user has to expand mentally to understand what their phone is doing is a worse interface than a flat list with a count. Somebody debugging a run reads the log, and the log is fully nested.

### Forwarding

| Flag | What it adds |
|---|---|
| `--forward-subagent-text`, or `CLAUDE_CODE_FORWARD_SUBAGENT_TEXT=1` | The child's text and thinking blocks, so its output is visible rather than only its tool calls |
| Without it | The child's `tool_use` and `tool_result` blocks only |

| Decision | Forwarding is enabled. |
| Why | Without it, a subagent's entire contribution is invisible, and a run that spawns four of them looks like it is doing nothing. |
| The cost | More context in the parent, so compaction arrives sooner. Shown in the context sheet. |
| Version | Requires Claude Code 2.1.211 or later for the text, 2.1.265 for a forked skill's text. The app feature-detects: forwarding is requested, and if the version does not support it the UI degrades to tool cards only. |
| The degradation | Silent, and stated once in the log: "Text von Unteragenten wird von dieser Claude-Code-Version nicht übertragen. Es werden nur Werkzeugaufrufe angezeigt." |

### Interruption

| Rule | Detail |
|---|---|
| Interrupting the parent | Interrupts the children. The process group is the mechanism, per `06-runtime/process-supervision.md`. |
| A child outliving the parent | Cannot, by construction. It is in the same group. |
| Verified | A test asserts the whole tree is gone, including a child that spawned a grandchild. |
| A child that hangs | Killed with the group. A run that cannot be stopped is a run the user has lost control of, and that is the failure this prevents. |

### Limits

| Limit | Value | Why |
|---|---|---|
| Nesting depth | 2, configurable in Advanced settings | Depth 3 makes the log unreadable and the cost unpredictable |
| Concurrent children | Whatever the model decides, bounded by the machine | The app does not second-guess the agent's scheduling |
| Cost | Counted in full | A subagent is a model call like any other |
| Context | Counted against the parent's | Forwarded text enters the parent's window |
| The hard blocks | Apply identically | A child inherits the run's policy, with no weaker variant. A test asserts a destructive tool from a child is refused. |

**The child inherits the parent's policy with no weaker variant.** This is worth stating because a child is a different process with a different conversation, and a policy that applied only to the parent would be trivially bypassed by asking the agent to delegate the forbidden action. The policy is applied at the tool-dispatch layer of every process the run owns, and a test asserts it.

### Skills as subagents

| Aspect | Behaviour |
|---|---|
| A skill that runs in a subagent | Detected via the `Skill` tool call with a `parent_tool_use_id` |
| Rendering | The same nesting, one level |
| A forked skill's own subagents | Counted, not nested. Forwarding requires 2.1.275 or later; the app degrades to a count. |
| Unlabelled | A skill run as a subagent is labelled with the skill's name, so a user can see which skill did what |

## Parallelism

### Two runs at once

| Situation | Behaviour |
|---|---|
| Two different projects | **Allowed.** Two foreground services' worth of work, two wake locks. |
| The same project, two conversations | **Refused**, with a link to the running one. Two runs in one working directory produce a state neither intended, and a per-project mutex is the only thing preventing it. |
| The same project, one running and one queued | The second is refused, not queued. A silent queue would look like the app had lost the request. |
| On the phone | Up to 3 concurrent runs |
| On a runner | Up to `maxConcurrentRuns`, probed |
| On GitHub Actions | 1, because a job is a job |

| The refusal message | "In {project} läuft bereits etwas." with a link. Not "Fehler". |

### Concurrent verification

| Situation | Behaviour |
|---|---|
| Two projects verifying at once | Allowed, on separate processes |
| One project verifying while a run works in it | **Allowed.** Verification reads; a run writes. A verification that fails mid-run because the run changed a file is a real annoyance, and the app handles it honestly: the verification is marked as having run against a moving tree, and the result is labelled. |
| The same project verifying twice | A per-project mutex serialises it. Two concurrent builds in one directory fight over the build directory. |

The second row is the honest version. Refusing it would be simpler and would make a normal workflow impossible: fix something by hand, run verification, watch a run finish. The label is the compromise.

### Parallel tool calls within a run

The engine can call several tools at once. The app's job is to render it.

| Aspect | Behaviour |
|---|---|
| Rendering | Cards in the order the engine emitted them, correlated by `toolUseId`. A list, not a tree, unless a parent id exists. |
| Elapsed time | Per card |
| A failure in one | The others continue. A failure is a card state, not a run state. |
| The permission queue | One at a time. The engine asks serially, and the sheet shows "2 weitere" if more are queued. |
| Interruption | Kills all of them, by the process group |

### Parallel projects in a worktree

| Aspect | Behaviour |
|---|---|
| Isolation | When enabled, each run gets a `git worktree` under the app's own directory |
| The benefit | Two runs on the same repository without a conflict |
| The cost | Disk, and a merge step afterwards |
| Removal | **Never automatically.** A worktree is a directory with the user's work in it. It is removed only through the audited erase path, and the storage screen says which worktrees exist and how much they use. |
| Off by default | It doubles the disk usage, and most people have one run at a time |

## Fan-out visibility

Somebody watching two runs needs to know both are alive.

| Location | Content |
|---|---|
| The tab bar | A dot, and a count when more than one is running |
| The chat list | A banner showing the most recent, with a count |
| The project list | Running projects first, with an indicator on the card |
| The notifications | One ongoing notification showing the most recent, with a "2 weitere" action that lists them |
| The About screen | How many are running, and how long the longest has been going |
| The log | Every run, correlated, so a post-mortem can interleave them |

**The tab bar dot and not a badge number.** A number invites a count of things to worry about; a dot invites a glance.

## Failure interaction

| Situation | Behaviour |
|---|---|
| One of two runs fails | The other continues. A failure is per run. |
| Both fail | Two notifications, and the run list shows both |
| One is offloaded and the other is local | Two different backends, two different hosts, both reported honestly |
| The device runs out of memory | The lower-priority run, the one with less progress, is interrupted with a reason. Never silently. Which is lower priority is stated: the most recently started, because the older one has more invested. |
| Thermal throttling | Both slow down. The elapsed times show it. |
| The battery | The advisory fires, once, not per run |

## Testing

| Test | Type | What it proves |
|---|---|---|
| `SubagentDetected` | E2E | A run that spawns a subagent produces nested cards, one level deep |
| `DeepNestingCounted` | E2E | Depth 3 is shown as a count, not nested |
| `ForwardingEnabled` | E2E | The flag is passed, and subagent text appears |
| `ForwardingDegrades` | E2E | An engine that does not support forwarding produces tool cards only, and the log says so once |
| `ChildCostCounted` | E2E | A subagent's usage is in the run's cost |
| `ChildInterrupt` | E2E | Interrupting the parent kills a child and a grandchild, verified by a tree walk |
| `ChildInheritsPolicy` | E2E | A destructive tool from a child is refused exactly as from the parent, at every level |
| `ChildDepthLimit` | E2E | A third level is refused or counted, per the setting |
| `SkillSubagentLabelled` | E2E | A skill run as a subagent is labelled with the skill's name |
| `TwoProjectsParallel` | E2E | Two projects run concurrently, with two indicators |
| `SameProjectRefused` | E2E | A second run for a busy project is refused with a link, and is not queued |
| `ConcurrencyLimit` | E2E | The fourth concurrent run is refused, naming the three running |
| `ActionsLimitOne` | E2E | An Actions backend refuses a second concurrent run |
| `VerificationDuringRun` | E2E | A verification during an active run completes and is labelled as having run against a moving tree |
| `VerifySerialisedPerProject` | Integration | Two verifications on one project are serialised |
| `ParallelToolRendering` | UI | Parallel tool calls render as a list with per-card state, and one failure does not affect the others |
| `PermissionQueueSerial` | UI | Permissions are shown one at a time with a "2 weitere" count |
| `OneFailureDoesNotStopOther` | E2E | One run fails, the other completes |
| `MemoryPressureInterruptsNewest` | E2E | The most recently started run is interrupted, with a reason, and it is stated which one |
| `WorktreeNeverRemoved` | E2E | Worktrees survive every run and are removed only by the audited erase path |
| `FanOutVisibility` | UI | A dot on the tab, a banner on the chat list, a card indicator on the project list, and a count in the notification |
| `NoBadgeCount` | UI | Asserts no numeric badge appears on the tab bar |
