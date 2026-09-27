# Code review

Review is the last gate a change passes through before it becomes the thing every other change builds on. This document defines what a reviewer looks at, in what order, and what makes a review fail.

The short version: a review checks **correctness, contract, and honesty**. Style is a tool's job, and the tools are already wired into `./gradlew check`.

## 1. What a reviewer is actually for

| Purpose | Not the purpose |
|---|---|
| Catching a change that breaks a contract | Catching a formatting nit |
| Checking the tests would fail without the change | Rewriting the implementation |
| Checking the docs describe the new behaviour | Judging taste in variable names |
| Blocking an unsafe or dishonest change | Proving the reviewer read every line |
| Keeping the hard blocks true | Being a gate that teaches nothing |

If a change passes all of the above, it merges. "I'd have done it differently" is not a blocker, and a reviewer who blocks on it is the problem.

## 2. Review order

Read in this order, and stop early if something is wrong:

1. **Description** — is this the right task, at the right size? A PR that solves a different problem than the one described is a blocker before any line is read.
2. **Doc diff** — does the documentation say what the code now does? A behavioural change with no doc change is a blocker, not a nit. This is the rule that keeps 135 documents from becoming fiction.
3. **Tests** — for each test, ask: *would this fail if the production change were reverted?* A test that passes either way is worse than no test, because it is a false promise about coverage.
4. **Layer rules** — does a `shared/domain` file now know about Room? Does a ViewModel call a DAO? Does `shared/core` import `android.*`? These are blockers, cheaply checked.
5. **Hard blocks** — does the diff delete anything, relax a block, add a network host, or contain a key-shaped string? Blockers, always.
6. **Concurrency** — a new coroutine scope, a new `GlobalScope`, a new mutable shared state, a `!!`. All require justification in the body.
7. **Error paths** — does every new failure have a user-facing message, a retryability, and a recovery action? An unhandled failure in a background run is invisible, and invisible is the thing this project exists to avoid.
8. **The implementation** — correctness first, clarity second.

## 3. Comment labels

Every comment carries exactly one label. Unlabelled comments are ignored, and a reviewer who does not label their comments is asked to.

| Label | Meaning | Blocks merge |
|---|---|---|
| **blocker** | Must change. The change is wrong, unsafe, undocumented, or breaks a contract | Yes |
| **should** | A real improvement, not a correctness issue. The author may decline with a reason | No |
| **nit** | Optional. Formatting, naming, ordering. Applied only if the author is already touching the line | No |
| **question** | The reviewer does not understand something and wants to. Not a demand | No |

A blocker always explains the failure it prevents. "This is wrong" is not a review comment; "this resumes the flow without checking the job, so a cancelled run continues and commits after the user pressed stop" is.

## 4. Automatic blockers

These do not need a reviewer to notice them. `reviewer` catches them, and the CI gates catch most of them first.

| Pattern | Verdict |
|---|---|
| A file deleted | blocker — hard block 1. Reverting is the fix; if the deletion is genuinely required, the spec is amended first |
| A branch or tag delete, `push --delete`, `gh repo delete` | blocker — hard block 1 |
| Anything that spends money | blocker — hard block 2 |
| A change that would make a repo or artefact public | blocker — hard block 3 |
| A commit to `main` other than a merge | blocker — hard block 4 |
| A new network host not in `privacy.md` | blocker |
| A new dependency without a `dependency-versions.md` line | blocker |
| A key, token, or credential-shaped string | blocker, and rotate |
| A behavioural change with no doc change | blocker |
| A test skipped, ignored, or deleted to make a suite green | blocker — hard block 5 |
| `@Ignore`, `@Disabled`, `runAllTests = false`, or a lowered coverage threshold | blocker until justified in the body |
| A new analytics symbol | blocker |
| `TODO` without an owner and a link | should, at minimum |
| A new `!!`, `GlobalScope`, or `Thread.sleep` | should; blockers when in a path that can crash or leak |

## 5. Anti-slop enforcement in review

D27 and §22.3. This is a **blocker**, not a preference.

| Check | Blocker condition |
|---|---|
| Every changed screen has an `ai-usage-policy.md` entry naming the design skill used | Missing entry |
| Output is not generic | Default Material purple, default Roboto, a stock gradient hero, card-inside-card-inside-card nesting, uniform spacing with no hierarchy |
| No emoji as UI icons | An emoji in an icon slot, or a second icon set appearing beside the documented one |
| Motion is purposeful | A state change over 300 ms, an infinite animation that is not the logo, motion with no purpose |
| Touch targets and type | Below 48 dp, below 14 sp, contrast below AA in either theme |
| Both themes reviewed | Only light, or only dark, for a screen that ships in both |
| README | Any banned word from `12-delivery/github-readme-guide.md`, any claimed feature that does not exist |

A reviewer who is told "this was generated" does not lower the bar. Generated code is reviewed exactly like written code, and generic output is a rejection, not a comment.

## 6. What a reviewer must not do

- Approve a change they did not read. "Looks fine" from a skim is worse than no review.
- Fix the change in the PR instead of commenting. That removes the author's context and hides the decision.
- Ask for a test to be added *after* merge.
- Rework an unrelated area discovered during review. New file, new task.
- Block on style that a formatter or linter owns.
- Approve their own change without a second reviewer for the high-risk paths in §1's "two reviews" rule.

## 7. Time

| Response time | Target |
|---|---|
| First response on a PR | 1 working day |
| Review of a `fix/` branch | 4 working hours |
| Review of a large task branch | 2 working days, with a first response within 1 |
| A review left idle for a week | Closed with a note, not left hanging. The branch survives; the PR number is retired in `main` |

A review that is slower than this is a review that is not happening. A branch that is three weeks old will need a rebase and a second review anyway, so closing it early is kinder than merging it late.

## 8. The agent reviewer

The `reviewer` subagent runs on every PR and applies the tables in §2–§5 mechanically. It is:

- **Required to** apply the automatic blockers, the anti-slop checklist, and the doc-change rule.
- **Expected to** find the doc and test gaps an inattentive human skips.
- **Not permitted to** approve. A green agent review is one input, never the decision. The human decides; when there is no human, the agent review plus a green gate is the standard, and a blocker it finds still stops the merge.
- **Not permitted to** modify the branch. It reports; the author fixes.

## Depends on

`13-process/development-workflow.md` · `13-process/git-strategy.md` · `13-process/definition-of-done.md` · `13-process/ai-usage-policy.md` · `03-design/anti-slop-rules.md` · `08-orchestration/permissions.md` · `claude-code-android-spec.md` §22
