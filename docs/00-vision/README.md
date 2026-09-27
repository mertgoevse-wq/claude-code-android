# Documentation index

135 documents. This page tells you which one to open.

The project spec is `claude-code-android-spec.md` at the repository root. It is the contract. These documents are its working detail: the decisions, the measurements, and the per-screen and per-feature contracts that code is written against.

## Reading order by role

**Building the app right now** → `13-process/claude-code-instructions.md` → the file for your current phase in `14-build-plan/` → `09-testing/` for how to prove it works → `10-build/` for the commands.

**Understanding the design** → `02-architecture/system-overview.md` → `03-design/design-tokens.md` → `03-design/anti-slop-rules.md` → the screen file you are about to touch in `04-screens/`.

**Reviewing a pull request** → `13-process/code-review.md` → `13-process/definition-of-done.md` → `03-design/anti-slop-rules.md` → `08-orchestration/permissions.md` (hard blocks).

**New to the project** → `00-vision/vision.md` → `00-vision/glossary.md` → `01-research/claude-code-runtimes-on-android.md` → `02-architecture/system-overview.md`.

**Shipping a release** → `12-delivery/release-checklist.md` → `11-operations/privacy.md` → `09-testing/performance-budgets.md`.

## Sections

| Section | Count | What lives there |
|---|---|---|
| `00-vision` | 7 | What we are building, for whom, and what "finished" means |
| `01-research` | 8 | Verified external facts: runtimes on Android, APIs, auth, free hosting, legal |
| `02-architecture` | 10 | Module map, data model, event protocol, state machines, decisions |
| `03-design` | 12 | Tokens, type, colour, motion, the animated mark, the anti-slop bans |
| `04-screens` | 14 | One contract per screen: layout, states, interactions, navigation |
| `05-features` | 16 | How each feature actually behaves, including failure behaviour |
| `06-runtime` | 8 | Installing and supervising Claude Code on the phone |
| `07-integrations` | 8 | GitHub, providers, secrets, notifications, remote runners |
| `08-orchestration` | 6 | The autonomy engine: permissions, sessions, context, recovery |
| `09-testing` | 10 | What is tested, how, and to what threshold |
| `10-build` | 7 | Gradle, versions, variants, signing, static analysis |
| `11-operations` | 7 | Logging, crashes, diagnostics, backup, privacy, threat model |
| `12-delivery` | 5 | APK, F-Droid, Play Store, README, release checklist |
| `13-process` | 6 | How we work: workflow, git, review, definition of done, AI use |
| `14-build-plan` | 6 | The seven phases, every task, dependencies, risks, progress |
| `15-appendix` | 5 | References, extended glossary, troubleshooting, FAQ, changelog |
| root | 3 | `README.md`, `CLAUDE.md`, `THIRD_PARTY_NOTICES.md` |

## Rules for these documents

1. **A document that describes behaviour that no longer exists is a bug.** If you change behaviour, you change the document in the same commit. `/doc-sync` exists for exactly this.
2. **Measurements get measured, not guessed.** Colours carry their contrast ratio. Sizes carry their value. Quotas carry the date they were checked. An unverified number is written as `TBD — verify at build time`, never as a plausible guess.
3. **One document, one job.** If a file needs two "and"s in its title, split it.
4. **No restating the spec.** The spec holds the decision. This folder holds the detail. Link, do not duplicate.
5. **Example code in these docs is real code**, copied from the repository, not invented. If it is illustrative, it says so.
