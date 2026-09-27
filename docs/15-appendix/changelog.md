# Changelog

All notable changes to this project are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) as described in `13-process/git-strategy.md` §4.

## How to read this file

- **Unreleased** is work that has happened on `main` and is not in a tagged release yet.
- Every version has a date, a link to the tag, and a section per change type.
- Entries say **what changed and why**, not what the diff did.
- A breaking change is called out explicitly and states the migration.

## Change types

| Type | Meaning |
|---|---|
| **Added** | New behaviour a user can notice |
| **Changed** | Existing behaviour that behaves differently |
| **Deprecated** | Still works, will be removed, named with its replacement |
| **Removed** | Gone. Always paired with what replaced it |
| **Fixed** | A defect fix |
| **Security** | A security-relevant fix, disclosed even when it is uncomfortable |
| **Docs** | Documentation only |
| **Build** | Toolchain, dependencies, CI |
| **Internal** | Refactors and changes with no user-visible effect |

---

## [Unreleased]

Nothing has been released. This project has not produced a build yet. The entry below is the work that has actually happened.

### Added — Documentation

- The full specification, `claude-code-android-spec.md`: the product definition, the locked decision log, the technical contract, the 135-document manifest, the ~150-file source manifest, the autonomy kit, the seven-phase build plan, the quality gates, the risks, and the definition of done.
- All **135 documents** in the §18 manifest, written before the code, so that the implementation is written against a contract rather than remembered afterwards:
  - `00-vision` — what this is, for whom, the scope and the non-goals, the user stories, the personas, the glossary, and the success metrics
  - `01-research` — the runtime survey, the engine's CLI surface, provider comparison, design reference audit, GitHub auth options, free remote-runner options, legal and trademark, and the competitive landscape
  - `02-architecture` — the system overview, module map, layer contracts, data model, migrations, concurrency model, error taxonomy, event protocol, state machines, and the ADR log
  - `03-design` — the binding design tokens, colour and contrast, typography, spacing, motion, the animated mark, the component library, the anti-slop rules, accessibility, responsive behaviour, brand assets, and the illustration set
  - `04-screens` — one contract per screen, all fourteen, each with layout, all five states, interactions, and navigation
  - `05-features` — sixteen feature specifications, each including its failure behaviour
  - `06-runtime` — the `ExecutionBackend` interface, the three runtime profiles, the bootstrap state machine, process supervision, the PTY and terminal, and environment diagnostics
  - `07-integrations` — GitHub API, auth, notifications, remote runners, notifications, providers, secrets, and MCP
  - `08-orchestration` — the request lifecycle, permissions, session management, context and tokens, subagents, and failure recovery
  - `09-testing` — the strategy and its nine companion documents, including the rule that no real key and no real repository appears in a test
  - `10-build` — Gradle setup, dependency versions, convention plugins, build variants, signing, static analysis, and local build and run
  - `11-operations` — logging, crash reporting, diagnostics export, backup and restore, privacy, telemetry, and the security threat model
  - `12-delivery` — APK distribution, F-Droid, Play Store, the README guide, and the release checklist
  - `13-process` — the development workflow, git strategy, code review, definition of done, AI usage policy, and the builder instructions
  - `14-build-plan` — the phase plan, a 122-task breakdown, the dependency graph, eight milestones, the risk register, and this build's progress log
  - `15-appendix` — references, the extended glossary, troubleshooting, the FAQ, and this changelog
- The three root documents: `README.md`, `CLAUDE.md`, and `THIRD_PARTY_NOTICES.md`.

### Added — The autonomy kit

- `CLAUDE.md` and the `.claude/` directory: 8 slash commands, 6 subagents, 8 project skills, and the settings file with the hard blocks denied at the permission layer.

### Added — Process decisions recorded as documents

- The hard blocks, stated in one place and enforced at three layers: never delete, never spend, never make anything public, never push to the default branch, never hide anything.
- No telemetry, as a decision with a written escape hatch rather than as an omission.
- BYOK authentication, with no subscription login, as an explicit product decision with the alternative recorded.
- The unofficial status and the own-brand rule, with what must not be copied and why.

### Not yet done

Everything else. There is no application code, no build, and no release. The plan is in `14-build-plan/phase-plan.md`; the progress is in `14-build-plan/progress-log.md`.

---

## Version history

_No tagged releases yet._

| Version | Date | Tag | Highlights |
|---|---|---|---|
| — | — | — | Nothing released. See [Unreleased](#unreleased) |

---

## Conventions for writing an entry

When a release is cut, add a section at the top:

```markdown
## [0.4.0] — 2026-XX-XX

### Added
- A user-visible capability, described in one line, with the why.

### Fixed
- A defect, described by its symptom rather than by its cause.

### Security
- A vulnerability and its impact, or "hardened X against Y".
```

Rules:

1. **Describe the change from outside.** "Runs a task in the background and keeps it alive when the screen is off" — not "added `AgentForegroundService`".
2. **Say why when the why is not obvious.** A reviewer reading the entry in six months should not have to open the diff.
3. **Name the breaking change loudly.** If someone upgrading has to do something, say exactly what.
4. **Every security fix gets an entry.** Quietly fixing a vulnerability is how a user finds out from someone else.
5. **Link the tag** in the comparison footer at the bottom of the file:

```markdown
[unreleased]: https://github.com/mertgoevse-wq/claude-code-android/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/mertgoevse-wq/claude-code-android/releases/tag/v0.4.0
```

6. **The GitHub release body and this file say the same thing.** Generated notes are edited before they ship; "fix: update stuff" is not a changelog entry.

## Depends on

`13-process/git-strategy.md` · `12-delivery/release-checklist.md` · `14-build-plan/progress-log.md` · `12-delivery/apk-distribution.md`
