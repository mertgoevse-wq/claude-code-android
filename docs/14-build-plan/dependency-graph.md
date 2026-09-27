# Dependency graph

Which tasks unblock which, what the critical path is, and what can run in parallel. Read this before scheduling anything, because the phase order in `14-build-plan/phase-plan.md` is not the same as the dependency order.

## 1. Rules that produce the graph

Three rules generate almost every edge in it:

| Rule | Example |
|---|---|
| **Documents before code** | Every `P0-2` document must exist before the task it describes starts, because the task is written *from* the document |
| **Interface before implementation** | `ExecutionBackend` (P3-1) precedes every backend, because a second implementation written against a moving interface is a rewrite |
| **Gate after each vertical slice** | A feature is not done until its test, its doc, and the gate are green — so the gate task is not a phase-end event, it is a per-task requirement |

## 2. The critical path

```
P0-1 → P0-3 → P0-4 → P0-5 → P0-7 → P2-1 → P2-6 → P2-8
                                              ↓
                                    P3-1 → P3-2 → P3-3 → P3-4 → P3-5
                                                              ↓
                                                     P3-11 → P4-1 → P4-8
                                                                        ↓
                                                            P5-3 → P5-4 → P5-5
                                                                        ↓
                                                            P7-10 → P7-12
```

Read that bottom line carefully. The path from "empty repository" to "the definition of done" runs through the runtime, because everything the user cares about is downstream of Claude Code actually running on the phone. A week spent on the design system is a week well spent, but it is not on the critical path, and neither is a beautiful diff viewer.

The two longest single edges are `P2-8 → P3-11` and `P3-4 → P3-5`. `P3-4` is the ELF patch, and it is the only task where the plan is genuinely uncertain.

## 3. Phase 0

```
P0-1 ──┬─► P0-2 ──► P0-11, P0-12
       │
       ├─► P0-3 ──► P0-4 ──► P0-5 ──► P0-6 ──► P0-10 ──► P0-20
       │                    │                        │
       │                    └─► P0-7 ──► P0-8 ──► P0-9│
       │                              │                 │
       │                              ├─► P0-13         │
       │                              │                 │
       │                              └─► P0-16 ──► P0-17, P0-18, P0-19
       └─► P0-14, P0-15
```

| Parallel with | Note |
|---|---|
| P0-2 (all docs) | Independent of everything except P0-1. It is 135 documents of writing and can proceed alongside the entire Gradle setup |
| P0-11…P0-15 (checkers) | Independent of the Gradle work, given P0-2 |
| P0-14, P0-15 | Trivial and independent; do them first, they are cheap and they protect everything after |
| P0-17, P0-18, P0-19 | Can be written in parallel with each other; they need P0-16 but not each other |

**The trap in Phase 0:** writing P0-4 (versions) before P0-2's `dependency-versions.md` exists means recording the lookups in the wrong place. Do the doc first, even as a stub.

## 4. Phase 1

```
P1-1 ──┬─► P1-2 ─┐
       ├─► P1-3 ─┤
       ├─► P1-4 ─┼─► P1-6 ──► P1-7 ──► P1-8 ──► P1-13 ──► P1-14
       ├─► P1-5 ─┤      │           │
       └─► P1-10 ┘      │           └──► P1-9 ──► P3-9
                       └──► P1-11 ──► P1-12
```

P1-2 through P1-5 and P1-10 are five independent pieces of specification. They are written in parallel, merged into one token set, and then everything else depends on P1-6.

**P1-12 depends on P0-9**, not on the UI: the mark's working state machine is fed by `AgentEvent`s, so the event hierarchy has to exist. This edge is easy to miss and expensive to get wrong.

## 5. Phase 2

```
P2-1 ──► P2-2, P2-3, P2-4, P2-5        (four parallel use-case groups)
  └─► P2-6 ──► P2-7 ──► P2-8 ──► P2-9, P2-11, P2-12
                       └─► P2-13
P0-8 ──► P2-10
P0-9 ──► P2-13
```

| Parallel with | Note |
|---|---|
| P2-2 … P2-5 | Four independent use-case groups. Parallelising them is the single biggest scheduling win in this phase |
| P2-7 and P2-9 | P2-9 (secrets) depends on repositories existing but not on migrations. Starting it early de-risks the most security-sensitive task in the project |
| P2-10 and P2-12 | Independent of the repositories. Both can be done during P2-6 |

**P2-4 is the security-critical task.** `HardBlockPolicy` lives in the permission policy, and it must be finished and reviewed before any UI that could offer a way around it exists.

## 6. Phase 3

```
P3-1 ──► P3-2 ──► P3-3 ──► P3-4 ──► P3-5 ──┐
  │           └─► P3-13, P3-14             │
  │                                        ▼
  ├─► P3-6 ──► P3-7 ──► P3-8 ──► P3-9 ──► P3-10
  │           └─► P3-11 ─────────────────────┘
  │                 └──► P3-12
  └─► P3-15
P1-9 ──► P3-9
```

| Parallel with | Note |
|---|---|
| P3-6 … P3-10 (process and terminal) | Independent of the engine download. The terminal can be fully built and tested against a plain shell long before Claude Code exists |
| P3-13, P3-14 (proot, AVF) | Both depend on P3-2 (the state machine), not on P3-4. They can be built in parallel with the native profile |
| P3-15 (foreground service) | Depends on P3-6, so it can be built while the engine download is still being debugged |

**P3-4 is the risk.** If the ELF patch does not work, the fallback is P3-13, which is an XL task and adds roughly 2 GB of storage. Both paths must be kept alive until P3-4 is proven on a real device.

**P3-11 needs P2-1** (the event model) — the app's `AgentEvent` hierarchy is written in Phase 2, not in Phase 3, so the mapper has a fixed target.

## 7. Phase 4

```
P4-1 ──► P4-2, P4-3, P4-4, P4-5
P4-6 ──► P4-7
P4-8 ──► P4-9, P4-15
P4-10 ──► P4-11 ──► P4-13, P4-14
P4-16, P4-17    (cross-cutting, need their feature areas)
P4-18 = the E2E gate over all of the above
```

| Parallel with | Note |
|---|---|
| P4-6 … P4-7 (chat list, new chat) | Independent of the detail screen. Build them while the streaming renderer is still being tuned |
| P4-10 … P4-14 (projects, diff, history) | A whole parallel track. It depends on P2-2, not on the chat |
| P4-16, P4-17 | Small, cross-cutting, and best done late, when the surfaces they touch are stable |

## 8. Phase 5

```
P5-1, P5-2 ──► P5-3 ──► P5-4 ──► P5-5 ──► P5-7
                 └─► P5-6                     │
                                                └─► P5-19 (E2E)
P5-8 ──► P5-9, P5-10, P5-11 ──► P5-12, P5-13, P5-14 ──► P5-20 (E2E)
P3-1, P5-1 ──► P5-15, P5-16, P5-17 ──► P5-18
```

Three independent tracks: **git/GitHub**, **skills**, **remote runners**. They share P5-1 and can proceed in parallel. The remote-runner track is the most deferrable; the skills track is the most self-contained.

## 9. Phase 6 and 7

```
P6-1 ──► P6-2 ──► P6-3 ──► P6-4 ──► P6-5, P6-6, P6-7 ──► P6-10
P2-9 ──► P6-8 ──► P6-11 (E2E, in Phase 7 numbering)
P4-15 ──► P6-9

P7-3 ──► P7-4, P7-5, P7-6, P7-7 ──► P7-10 ──► P7-12
P7-8 ──► P7-9
```

P7-4 through P7-7 are four independent passes over the same screens. They are the natural place to use subagents, because they are read-mostly, they have clear acceptance criteria, and they do not edit the same files at the same time. If they do, the conflicts are resolved by a human, not by an agent deciding which is right.

## 10. What blocks the whole project

Three things. If any of them stalls, everything downstream stalls, and the correct response is to report the blocker rather than to build against an assumed interface:

| Blocker | Blocks | Because |
|---|---|---|
| The engine runs on the device (P3-4) | P3-5, P3-11, and every feature phase | Without it there are no events, and everything downstream is written against a fiction |
| `HardBlockPolicy` complete (P2-4) | Every UI, every git operation, every autonomy level | The hard blocks are the product's safety claim. Building UI before it means retrofitting a guard onto every surface |
| The event protocol frozen (P2-1, P3-1) | P3-11, P4-1, P5-18, P6-3 | Three backends, four backends' worth of tests, and a judge all speak this protocol. A late change here is a rewrite |

## Depends on

`14-build-plan/phase-plan.md` · `14-build-plan/task-breakdown.md` · `14-build-plan/milestones.md` · `14-build-plan/risk-register.md`
