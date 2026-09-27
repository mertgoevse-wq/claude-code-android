# AI usage policy

AI wrote a large part of this project. That is not a disclosure of a problem; it is a statement of how the work was done, and this document exists so the use is **visible, attributable, and reviewable** instead of invisible and unexamined.

The rule underneath everything: **generated output has the same bar as written output.** A screen generated in one pass is reviewed as carefully as one written by hand, and generic output is rejected rather than polished.

## 1. Two different kinds of AI use here

| Kind | What it is | Where it is allowed | Disclosure |
|---|---|---|---|
| **Code generation** | An agent writes implementation and tests against a spec | Anywhere, behind the normal review | This document; commit trailers; PR descriptions |
| **Design generation** | A design skill produces a direction, a layout, a visual system | Only where an entry in §4 names the skill | This document, one row per screen |

Design generation is tracked per screen because §22.3 requires it: *"Every UI screen must have a `docs/13-process/ai-usage-policy.md` entry naming the design skill used."* The table in §4 is that ledger, and it is the review artefact.

## 2. What we use, and for what

| Skill | Used for | Not used for |
|---|---|---|
| `docs-authoring` | Structure, voice, and the rule that measurements are measured | Prose that restates the spec instead of linking to it |
| `test-authoring` | Test patterns, fixture design, boundary cases | Asserting a behaviour the test does not actually pin |
| `runtime-bootstrap` | Working on the state machine, process supervision, and the PTY safely | Anything that writes to `filesDir/cca/` outside a controlled test |
| `kmp-shared` | Keeping `shared/` portable | Retrofitting Android imports into shared code "just this once" |
| `github-safety` | Branch, commit, PR, and hard-block rules | Anything that relaxes a hard block |
| `ui-design` | Delegating to the installed design skills for a screen | Applying a design direction without reviewing the result |
| `android-compose` | Compose and Material 3 patterns for this project | Default Material theming, which `03-design/anti-slop-rules.md` bans |
| `release` | Build, sign, package, publish metadata | Skipping a checklist box |
| `architect` | Docs before code, decision records | Writing code that contradicts a decision |
| `implementer` | Implementation against a spec plus tests | Implementing beyond the task |
| `tester` | Writing and running tests, reporting failures precisely | Reporting a failure as a pass, or fixing the test to match the bug |
| `debugger` | Root-causing a failure, proposing the minimal fix | Broad refactors while debugging |
| `designer` | Owning the design system, applying the design skills | Unreviewed visual changes |
| `reviewer` | Checking a change against the spec, the anti-slop rules, and the hard blocks | Approving |

## 3. Rules

| # | Rule | Enforcement |
|---|---|---|
| 1 | Generated code is reviewed like written code. Always. | `13-process/code-review.md` |
| 2 | A test that does not fail without the change is a blocker, generated or not | Review checklist |
| 3 | No behavioural change without a doc change in the same commit | CI doc check + review |
| 4 | Generic output is rejected, not polished. If a screen looks like a template, it is rewritten | `reviewer` blocker |
| 5 | No AI-generated asset ships without a licence and provenance note in `03-design/brand-assets.md` | Review |
| 6 | No model-generated text in the UI that the user did not ask for: no fake testimonials, no invented reviews, no placeholder names that look real | Review blocker |
| 7 | No emoji as UI icons, and no emoji in documentation except shields.io badges in the badge row | `03-design/anti-slop-rules.md` |
| 8 | Version numbers, quota numbers, and checksums are **looked up**, never generated. A fabricated version is a blocker | §5 |
| 9 | A generated dependency is never added without a licence review and a line in `10-build/dependency-versions.md` | `tools/check_foss_dependencies.sh` |
| 10 | The agent reports a real blocker and stops. It never approximates past one | `13-process/claude-code-instructions.md` |
| 11 | Nothing is marked green that was not run | Hard block 5 |
| 12 | The `--no-verify` flag is a blocker in every context | Hard block 5 |

## 4. The screen ledger

One row per screen: the design skill that shaped it, and the reviewer verdict. `impeccable` and the `design-library` skills cover the general visual pass; the specific ones below were chosen per screen because the screen's problem matched the skill's strength.

| Screen | Design skill | Notes | Verdict |
|---|---|---|---|
| `01-onboarding` | `impeccable` + `mobile-android-design` | First-run explanation of autonomy levels had to read as a decision, not a form | ✅ |
| `02-chat-list` | `impeccable` | Grouping by day and project; the empty state is a first-run prompt, not an illustration | ✅ |
| `03-new-chat` | `bolder` | The screen was flat and generic; the animated mark carries the personality | ✅ |
| `04-chat-detail` | `impeccable` + `critique` + `typeset` | The heart of the app; hierarchy, tool-card density, and the cost meter's placement | ✅ |
| `05-chat-with-terminal` | `adapt` | Split view across phone portrait, landscape, and tablet; a real layout problem | ✅ |
| `06-diff-viewer` | `typeset` | Monospace, line-height, and hunk legibility; the syntax theme is a design decision | ✅ |
| `07-project-list` | `layout` | Information density for repos, branches, last run, and open PRs | ✅ |
| `08-project-detail` | `layout` + `impeccable` | Tabs, run history, and the branch/PR state without becoming a dashboard | ✅ |
| `09-add-project` | `clarify` | The copy tells the user what a private repo means before they paste a URL | ✅ |
| `10-skills-browser` | `craft` + `impeccable` | Scoping, origin, and the file diff preview are the trust surface | ✅ |
| `11-skill-editor` | `craft` | A code editor that is honest about being a text editor with validation | ✅ |
| `12-terminal` | `impeccable` + `android-compose` | Monospace, ANSI palette, the keyboard row, and a `Ctrl+C` that is always reachable | ✅ |
| `13-settings` | `distill` + `impeccable` | Sixteen settings screens; ruthless about hierarchy so none of them is a wall | ✅ |
| `14-remote-runner-setup` | `clarify` + `impeccable` | Guided setup with a live quota check and an honest cost statement | ✅ |
| README | `website` + `craft` + `high-end-visual-design` | D27: the README is a product page, and it gets the same review | ✅ |
| Empty states and onboarding art | `illustration-set` doc + `craft` | One coherent set, no stock art, no emoji | ✅ |

A row with no skill and no verdict is a blocker. A screen with a `❌` verdict is not merged; it goes back through the design pass.

## 5. Things AI must not invent

| Thing | Correct behaviour | Why it matters |
|---|---|---|
| A library version | Look it up, record the date of the check | A wrong version does not compile, or compiles against an API that does not exist |
| A free-tier quota | Look it up, record the access date, and re-verify at build time | Quotas change without notice; a stale number sends someone over a limit |
| A checksum | Compute it from the artefact, or cite the publisher | A fabricated checksum is a security bug |
| An API flag or output format | Verify against the actual binary or the actual documentation | The parse fails silently in production and looks like a model problem |
| A licence | Look it up | Shipping under the wrong licence is a legal problem |
| A performance number | Measure it | A guessed number becomes a budget nobody can meet |
| A citation | Cite a real URL, or write `TBD — verify at build time` | A fabricated reference is worse than an honest gap |

## 6. What is disclosed, and where

| Artefact | Disclosure |
|---|---|
| The repository | This document, and a `Co-Authored-By` trailer on generated commits |
| The app's About screen | "Unofficial, independent project. Not affiliated with Anthropic." Nothing about how it was built — the user's interest is in whether it works and whether it is safe |
| The README | Nothing about generation, because the README is a product page and a note about tooling is noise. What it does do is state every limitation honestly |
| Third-party code | `THIRD_PARTY_NOTICES.md`, with licences, regardless of who wrote the code |
| Generated visual assets | Provenance and licence in `03-design/brand-assets.md` |

## 7. Reviewing AI-assisted work

Extra questions the `reviewer` asks when a change looks generated:

| Question | If the answer is bad |
|---|---|
| Would this test fail if the change were reverted? | Blocker |
| Is this the minimum change that solves the task? | Scope creep; split the PR |
| Does this file read like a person who knows the codebase? | Look for invented APIs, plausible-looking functions that do not exist, and confident doc links to files that were never created |
| Does the doc say something the code does not do? | Blocker |
| Is there a comment explaining a non-obvious decision? | Missing context is a `should` |
| Does the UI look like a template? | Blocker: rewrite, do not polish |

The invented-API failure is the most common real problem. A generated change that references a function that sounds right and does not exist fails to compile, which is loud; the dangerous version is a doc link or a config key that is quietly wrong. Those are what this checklist is aimed at.

## Depends on

`13-process/code-review.md` · `13-process/claude-code-instructions.md` · `03-design/anti-slop-rules.md` · `12-delivery/github-readme-guide.md` · `14-build-plan/progress-log.md`
