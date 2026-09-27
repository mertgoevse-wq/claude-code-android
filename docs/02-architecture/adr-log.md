# Architecture decision records

One entry per significant decision: what was decided, what was rejected, and what it costs us. New decisions get a new number; an existing decision is superseded by a new entry that says so, never edited in place.

Status values: `Accepted`, `Superseded by ADR-NNNN`, `Revisited — still stands`.

---

## ADR-001 — Native Android with Kotlin, structured for iOS later

**Status:** Accepted

**Context.** The app must be fast, fluid, and beautiful on Android, and the user wants an iOS port eventually. It must not be a web app.

**Decision.** Kotlin, Jetpack Compose, Kotlin Multiplatform with the shared code in `shared/`.

**Rejected.**
- *React Native or Flutter.* A single codebase for both platforms, at the cost of the platform's own idioms and some fluidity on Android. Android comes first and is the harder platform to make look native.
- *Retrofitting portability later.* Cheaper now, far more expensive later. A retrofit touches every file.
- *Pure Android with a shared UI only in theory.* The user explicitly asked for the seam from day one.

**Consequences.** The build is slightly heavier and some Android-specific APIs need an `expect`/`actual` seam. In exchange, an iOS port is a new target rather than a rewrite. We accept the discipline of `shared/` having no Android imports, enforced by a CI check.

**Revisit if** the iOS port is abandoned. Then `shared/` merges into `androidApp/` and the seam dissolves. That is a mechanical change and a deliberate one.

---

## ADR-002 — Subprocess CLI, not the embedded Agent SDK

**Status:** Accepted

**Context.** The Agent SDK bundles a native binary and expects a normal Linux process environment. We need to control the binary's provenance, its version, and how it is patched for Android.

**Decision.** Download the CLI, verify it, patch it, run it as a subprocess, and parse `stream-json`.

**Rejected.**
- *The TypeScript or Python Agent SDK.* Structured message objects and permission callbacks are genuinely nicer. But it manages the binary itself, which removes exactly the control we need, and it does not help with the glibc problem — the SDK's binary has the same problem on Android.
- *Reimplementing the agent loop.* Out of the question, and legally closed off.

**Consequences.** We own a line-format integration that Anthropic changes without a deprecation cycle. Mitigated by `AgentEventMapper` being the only place that knows the format, by contract tests against recorded fixtures, and by a rule that a change fails the build rather than degrading silently. In exchange we can roll back a broken version, probe before swapping, and pin exactly.

**Revisit if** Anthropic publishes a stable output-format contract, or offers an Android-target SDK build.

---

## ADR-003 — One seam: `ExecutionBackend`

**Status:** Accepted

**Context.** The engine must run on the phone now and on a server later, with the same interface.

**Decision.** Every location is an `ExecutionBackend` implementation. Nothing above it knows where it runs.

**Rejected.**
- *A phone-only app with a remote feature later.* Would have made the remote path a bolt-on with its own UI, its own event handling, and its own bugs.
- *An SSH-only client.* The user asked for local-first, and local-first means the phone can do it alone.

**Consequences.** Some features are honestly not portable — building an APK is local-only, a home PC has a different filesystem. We model that with `BackendCapabilities` rather than pretending otherwise. A new location is one file plus one setup screen.

---

## ADR-004 — Verification decides success

**Status:** Accepted

**Context.** An agent reports that it is done. That report is a claim.

**Decision.** `Verifier` runs the project's own commands. `Judge` decides from exit codes. Only then can a run reach `DONE`. No verification commands means the state is `UNVERIFIED`, which is visually distinct and never green.

**Rejected.**
- *Trusting the agent's completion claim.* It is the single biggest source of false confidence in every AI coding tool.
- *Trusting the test command only.* A single command misses typecheck, lint, and build failures. A failure in any of them is a failure.

**Consequences.** Verification is slow, and some projects have no verification at all, so the app has to teach them to have one. The run takes longer to reach a confident "done". This is the correct trade: a slower honest answer beats a fast wrong one, and it is the main thing this app does that others do not.

---

## ADR-005 — Three autonomy levels plus one, per project

**Status:** Accepted

**Context.** Two personas with opposite needs: one who wants one button, one who wants control. And a scratch script should not behave like production.

**Decision.** Four levels, stored per project, from `ASK_EVERYTHING` to `FULL_AUTO`. Raising to full-auto takes a two-step confirm; lowering takes one tap during a run, with no warning.

**Rejected.**
- *A global setting.* The wrong granularity; it forces the cautious user to be reckless or the reckless user to be nagged.
- *Per-tool toggles.* A matrix of switches nobody can reason about.
- *No autonomy at all, always ask.* Defeats the purpose. The user asked for a tool that works while they are elsewhere.

**Consequences.** Four code paths through permission resolution, all tested. The user can make a mistake that costs money in tokens, which is why the cost meter is always visible and why the retry budget is configurable.

---

## ADR-006 — Hard blocks live in our executor, not in a prompt

**Status:** Accepted

**Context.** Five rules that must hold: never delete, never spend, never publish, never push to the default branch, never hide. At `FULL_AUTO` the CLI itself asks for nothing.

**Decision.** `HardBlockPolicy` is consulted in the app's own permission layer, before any tool dispatch, regardless of the autonomy level or the permission mode. The rules are also denied in `.claude/settings.json` and enforced structurally where possible.

**Rejected.**
- *Asking the agent to respect them in a system prompt.* A prompt is a suggestion. At full autonomy with a long context, it is a suggestion that decays.
- *Filtering commands after the fact.* Too late, and a filter is a blocklist, which is only as good as its enumeration.
- *Removing destructive tools from the allowed list.* Necessary but insufficient: a legitimate `Bash` can delete. The policy inspects intent, not just the tool name.

**Consequences.** Some legitimate work is refused and the agent must be told and continue. That is the intended behaviour. Where possible the capability is absent by construction: the git command builder has no delete path at all, so there is nothing to filter.

---

## ADR-007 — Four runtime profiles behind one interface

**Status:** Accepted

**Context.** No official Android build exists. Three workarounds exist with very different costs.

**Decision.** Ship the patched native binary as the default, offer proot Ubuntu on demand, detect AVF as experimental, and route heavy work to a remote runner instead of fighting the phone.

**Rejected.**
- *proot only.* Two gigabytes and a multi-minute install before the user sees anything. It would lose most people during onboarding.
- *Remote only.* Directly contradicts the request, and fails for anyone without a card for a cloud account.
- *Termux integration.* Two applications, two lifecycles, and a dependency the user must install. The user asked for one tap.

**Consequences.** The most fragile part of the project. Mitigated by fail-closed verification, a probe before every version swap, automatic rollback, and three independent paths to a working engine. When upstream ships a real Android build, this ADR is the first thing to revisit.

---

## ADR-008 — BYOK only, no subscription login

**Status:** Accepted

**Context.** The user asked for their own key, and Anthropic does not permit third-party applications to offer claude.ai login or subscription access.

**Decision.** The app requires a key. Supporting OpenAI-compatible and custom providers is a first-class feature, not a workaround.

**Consequences.** The user must obtain a key before the app is useful, which is a real onboarding cost, and the app cannot be used with a Pro subscription they already pay for. In exchange there is no account, no telemetry problem, no rate-limit dependency on someone else's policy, and the app works with a self-hosted model.

**Revisit if** Anthropic offers an authorised third-party path.

---

## ADR-009 — The event stream is the only integration

**Status:** Accepted

**Context.** Four consumers need the engine's output: the chat, the log, the cost meter, the loop detector.

**Decision.** Everything consumes `Flow<AgentEvent>`. The log is a consumer, not a special case.

**Consequences.** The transparency record is complete by construction rather than by discipline, because the same events that render the chat write the log. A change to the mapping is visible in both places at once. The cost is a fan-out with a bounded buffer and a visible dropped-event counter, because unbounded buffering is a leak.

---

## ADR-010 — Precompute localised strings in the database

**Status:** Accepted

**Context.** Notifications are built in a background worker, potentially in a language different from the one the UI last used. Chat tool cards need "Datei gelesen: {path}" in the user's language.

**Decision.** Store `titleDe` and `titleEn` on the entity, computed when the tool call is recorded. Build notifications from stored text.

**Rejected.**
- *Resolving the string at notification time.* Needs the user's locale in a background context, and needs a resource lookup outside a UI scope.
- *Storing only a key and translating at render time.* Cheap, but breaks for background delivery and for a transcript read in a different language later.

**Consequences.** Two columns per user-visible string, and a migration when a language is added. Adding a third language touches the schema; the alternative is a design that cannot say "Deutsch" and delivers English in a notification. Acceptable, and the migration is mechanical.

---

## ADR-011 — An unverified state that looks different from success

**Status:** Accepted

**Context.** A project with no tests produces a run that changes files and cannot be checked. Calling that "done" is the same lie as trusting the agent.

**Decision.** `UNVERIFIED` is a first-class verification state with its own visual treatment, and the run summary leads with it.

**Rejected.**
- *Treating "no tests" as pass.* That is a false green, the worst outcome available.
- *Blocking the run.* Refusing to work on a project without tests would make the app useless for exactly the personal projects one persona writes.

**Consequences.** A user can reach a completed-looking state that is honestly labelled as unchecked. The app nudges toward establishing verification commands, and the first thing the planner does on a project without them is set them up, told to the user rather than done silently.

---

## ADR-012 — Append-only log, with exactly one audited deletion path

**Status:** Accepted

**Context.** "Never hide anything" requires a log that cannot be quietly edited. "Never delete" must not make it impossible to free storage.

**Decision.** `SessionLogEntry` has no update and no delete in the repository API, asserted by a test. The one exception is an explicitly confirmed, user-initiated "erase local project data" in Settings, which clears the app's local cache of a project. It cannot be triggered by the agent, never touches a remote, never touches git history, and is itself logged.

**Consequences.** The database grows. There is a storage screen showing what is using space, and the erase path is explicit and audited. A log that can be edited is not a log.

---

## ADR-013 — Poll GitHub instead of using webhooks

**Status:** Accepted

**Context.** GitHub notifications for PR comments and check results. Webhooks are instant; they require a public endpoint, which means a server.

**Decision.** Polling, with an interval that adapts to foreground state, battery, and whether a run is active. No webhook, no server, no inbound port.

**Consequences.** Notifications arrive with 30 seconds to 30 minutes of delay, depending on state. The whole product rests on no third-party server being required, and a webhook would break that promise to buy latency nobody needs for a comment notification.

---

## ADR-014 — Our own mark, not a lookalike

**Status:** Accepted

**Context.** The design target is a specific app with a distinctive orange starburst. A starburst with eyes is a plausible interpretation of "animated identity that reflects state".

**Decision.** A geometrically distinct mark, specified with its own source and construction rules in `03-design/brand-assets.md`. The test: side by side, nobody would confuse them.

**Rejected.**
- *Using the reference mark.* It is not ours.
- *A starburst with eyes.* Too close, however well intended.

**Consequences.** The app is visually in the same family, not a copy. This is also why the About screen and the README state the unofficial status: resemblance is inevitable when the target is a specific design, and the honest response is disclosure rather than concealment.

---

## ADR-015 — Exhaustive testing is a gate, not a goal

**Status:** Accepted

**Context.** The user asked for thorough testing. The project runs an agent that edits code unattended, so a regression can destroy a user's project.

**Decision.** `./gradlew check` gates everything: 90 % coverage on domain, 80 % on data, UI tests for every screen in every state, screenshot baselines, five E2E journeys, static analysis, and the hard-block tests.

**Rejected.**
- *Test the important parts.* On an unattended code-editing agent, "important" is not knowable in advance.
- *Manual verification before release.* It does not scale and it is forgotten.

**Consequences.** Build times are long, screenshot diffs are a maintenance cost, and coverage thresholds can incentivise writing tests that execute lines rather than assert behaviour. The mitigation is that every threshold is paired with a behavioural test that would fail if the behaviour broke, and the thresholds themselves are not the definition of done.

---

## ADR-016 — One-shot build, staged, with a hard stop at phase 3

**Status:** Accepted

**Context.** The user wants the whole project built autonomously, without stopping to ask. A runtime that does not work makes everything above it fiction.

**Decision.** Seven phases, run in one autonomous session. Phase 3 is a genuine hard stop: if the patched binary will not run on the test device, the build stops and reports rather than continuing against a CLI that does not exist.

**Rejected.**
- *Continue and mark it as a known issue.* Every later phase would be verified against nothing, and the failures would be untraceable.
- *Stopping to ask a human.* The user asked for no interruptions, and this specific stop can be resolved by the user in minutes with a clear report.

**Consequences.** There is one point at which autonomy deliberately yields. Everything before and after it is genuinely hands-off.
