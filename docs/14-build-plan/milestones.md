# Milestones

Eight checkpoints, each one verifiable by someone who did not do the work. A milestone is reached when its verification passes, not when the work feels close to done.

## M0 — The repository is real

**Reached when:** a fresh clone plus `./gradlew check` is green, and all 135 documents exist.

| Verification | How |
|---|---|
| Build green on a fresh clone | `./gradlew check` |
| Docs present | `tools/check_doc_manifest.py` |
| Gates fail when they should | Deliberately break each checker, see it fail, revert |
| Versions are real | Every version in the catalog has a lookup date in `10-build/dependency-versions.md` |

**Why it matters:** everything after this depends on a green baseline. A red starting line makes every later failure ambiguous.

## M1 — It looks like something

**Reached when:** the design system exists, both themes pass contrast, and the animated mark runs through all its states.

| Verification | How |
|---|---|
| Every token used | A script that greps the theme for each token name |
| Contrast AA, both themes | The checker in `03-design/color-and-contrast.md` |
| Mark state machine covered | Screenshot test per state, plus the reduce-motion fallback |
| Anti-slop pass | `03-design/anti-slop-rules.md` checklist green |

**Why it matters:** this is the point at which the app stops looking like a template. It is cheap to do now and expensive to retrofit.

## M2 — It knows what it is doing

**Reached when:** the domain model, the database, the migrations, and the secrets all work, and the hard blocks are unreachable.

| Verification | How |
|---|---|
| Coverage | `shared/domain` ≥ 90 %, `shared/data` ≥ 80 % |
| Migrations | Every step tested, including the destructive-change rule |
| A key round-trips | Stored, retrieved, and absent from every log, export, and diff |
| Hard blocks | Every hard-block test passes, including "attempt to delete is refused" |
| Layer purity | `tools/check_no_android_imports_in_shared.py` |

**Why it matters:** the safety claim of the product is implemented here, or it is not implemented at all.

## M3 — It runs Claude Code

**Reached when:** on a real device or emulator, the app downloads the engine, verifies it, patches it, launches `claude --version`, runs a headless prompt, and streams the output back.

| Verification | How |
|---|---|
| The version command runs | Terminal pane in the app |
| A prompt streams back | A headless run with output visible |
| Resume works | Kill the app mid-bootstrap; it resumes |
| Cancellation is complete | A cancelled run leaves no child process |
| A checksum mismatch aborts | Corrupt a download; the app refuses to proceed |
| proot bootstrap completes | A full Ubuntu bootstrap on a real device |

**Why it matters:** this is the hard stop. If this milestone is not reached, there is no product, and the correct action is to report the blocker — not to continue building UI over a fictional engine.

**This is the milestone most likely to be missed**, and the one most likely to be *quietly* missed. "It launches on my emulator with a patched binary I built by hand" is not the milestone. The milestone is the reproducible path from a fresh install.

## M4 — You can watch it work

**Reached when:** the primary E2E journey passes on an emulator, and every screen in scope has all five states.

| Verification | How |
|---|---|
| E2E journey 1 | Open app → new chat → pick project → order → stream → interrupt → resume |
| Five states per screen | UI test per screen per state |
| Cost is accurate | Against the engine's own accounting |
| Interruption is recoverable | Partial work on a `wip/` branch |
| The log is complete | Export it and read it; nothing is missing |

**Why it matters:** this is the first milestone a user would call "the app".

## M5 — The result leaves the phone

**Reached when:** a task on a private test repo produces a green branch and an open PR, and a skill installs from a URL and works in a run.

| Verification | How |
|---|---|
| E2E journey 2 | Private test repo → green branch → open PR |
| E2E journey 3 | Install a skill from a URL → use it in a run |
| A red suite blocks the push | With a reason shown |
| The default branch is refused | With a test asserting the refusal |
| The install diff is shown | Every file, before anything is written |
| A remote run round-trips | Or degrades with an honest message |

**Why it matters:** this is the promise. Everything before it is preparation for one workflow: an order in, a pull request out.

## M6 — It can finish on its own

**Reached when:** a deliberately broken project reaches green within the retry budget, and the exhaustion path reports correctly.

| Verification | How |
|---|---|
| E2E journey 4 | Broken project → green within budget |
| The judge disagrees | At least once, the judge's verdict differs from the agent's claim |
| Anti-loop works | A genuinely stuck loop is aborted |
| Exhaustion reports | Clear report, notification, honest counter |
| The self-update verifies | Signature and checksum before install |

**Why it matters:** a judge that always agrees with the agent is decoration. The evidence for this milestone is a test where they disagree.

## M7 — You can install it

**Reached when:** every gate in spec §22.2 is green, a fresh clone produces a signed APK, and the README passes its own checklist.

| Verification | How |
|---|---|
| All §22.2 gates | The release checklist, section 1 |
| A signed release APK | From a fresh clone, one command |
| Clean-room F-Droid build | No Gradle cache, no Google services |
| The README's 12 points | `12-delivery/github-readme-guide.md` §4 |
| Documentation matches the code | `/doc-sync` produced no outstanding diff |

**Why it matters:** this is the milestone at which other people can use it, and at which the project's claims become checkable by strangers.

## M8 — Someone else can use it

**Reached when:** a person who has never seen the app goes from install to a merged pull request without being asked a single question.

| Verification | How |
|---|---|
| The full flow, unaided | A non-technical user, on their own phone, with no help |
| No blocking question at any step | Every prompt either has an obvious default or is genuinely required |
| Time to first pull request | Recorded, because it is the number that matters |
| What confused them | Written down, and the top three are fixed |

**Why it matters:** this is the project's definition of done, from the spec. It is deliberately the last milestone, and it is the only one that cannot be verified by the people who built the thing.

## Timeline

There is no committed date. Publishing one for a project whose riskiest task is an undocumented binary patch would be a promise we cannot keep.

What can be said honestly:

| Milestone | Rough effort from M0 | Dominant unknown |
|---|---|---|
| M0 | 1–2 weeks | None. This is mechanics |
| M1 | 1–2 weeks | Design judgement, not risk |
| M2 | 2–4 weeks | Coverage discipline |
| M3 | **2–8 weeks** | **The ELF patch. This range is wide because it is genuinely unknown** |
| M4 | 3–5 weeks | Streaming performance on a phone |
| M5 | 3–5 weeks | GitHub OAuth review friction, remote-runner quotas |
| M6 | 2–3 weeks | Judge quality |
| M7 | 2–4 weeks | Play and F-Droid review queues |
| M8 | 1 week, plus fixes | What users actually stumble on |

The range on M3 is the honest one. Everything before it is predictable work; everything after it depends on it. If the ELF patch fails, the fallback is the proot profile, which adds storage, slows every run, and pushes M3 out by weeks. Both paths are being built, and the native profile is the one being attempted first.

## When a milestone is missed

1. Record it in `14-build-plan/progress-log.md` with the date, what was expected, and what happened.
2. Update the estimate in `14-build-plan/task-breakdown.md` with the real number.
3. Check `14-build-plan/risk-register.md`: a missed milestone usually means a risk was under-scored, and the register is where that gets corrected.
4. Do **not** lower the milestone's bar. The bar is what makes it a milestone.

## Depends on

`14-build-plan/phase-plan.md` · `14-build-plan/task-breakdown.md` · `14-build-plan/risk-register.md` · `14-build-plan/progress-log.md` · `13-process/definition-of-done.md`
