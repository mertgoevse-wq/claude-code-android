---
description: Read the last failure, fix its cause, stay inside the retry budget
---

# /fix

Fix the last red gate. Read the error before touching anything.

## 1. Get the failure

```bash
git status --porcelain
./gradlew :shared:domain:test --tests '*TheThing*' 2>&1 | grep -B2 -A8 -E '^e:|FAILED|expected:'
```

Read the **first** `e:` or `error:` line. A wall of cascading errors is one
cause, and fixing the last one wastes the attempt.

## 2. Name the cause before fixing it

One sentence: "X fails because Y." If you cannot name Y, you do not understand
the failure yet, and the next edit is a guess.

## 3. Fix the cause, not the symptom

| Failure | Wrong fix | Right fix |
|---|---|---|
| Test fails | Change the assertion to match the output | Make the behaviour correct, or find out why the expectation was wrong |
| Coverage below the gate | Lower the threshold | Add the test for the uncovered branch |
| Build does not compile | `@Suppress` | Fix the call. An invented API is the usual cause |
| Screenshot diff | Accept the baseline | Look at both images. If it is not intended, the change is wrong |
| Doc manifest fails | Add the file to an ignore list | Write the document, or amend the manifest in the spec |

## 4. The retry budget

Three attempts. **Three failures with three different causes means the design
is wrong**, not that the third fix was wrong. Stop and write a blocker to
`docs/14-build-plan/progress-log.md` in the format that file specifies:

- the command
- the full error, verbatim
- each attempt, numbered, with its result
- the analysis, clearly marked as analysis
- what a human must decide

Then stop. Do not start the next task. Do not lower a gate. Do not approximate
past a blocker.

## 5. Commit the repair

A fix is its own commit, so the history shows the red build and the repair as
two facts. Then push.
