# Contract tests

The boundary between this app and Claude Code is a CLI's stdout. We do not control its format and it can change in any release. Contract tests are how we find out on a build machine rather than in a user's hands.

## What is contracted

| Contract | Direction | Risk if broken |
|---|---|---|
| `stream-json` event schema | in | Silent loss of text, invisible tool calls, wrong cost |
| CLI flags we pass | out | The run starts and then misbehaves in a way that is hard to attribute |
| Exit codes | in | A cancelled run reported as failed, or vice versa |
| System event names | in | Retry behaviour driven by the wrong signal |
| File layout after install | in | The binary is present but unusable |
| `claude --version` output shape | in | Version gating and bug workarounds applied wrongly |

## The recording

`scripts/record-contract.sh` runs the real `claude` binary once, with a fixed prompt, in a throwaway directory, against a key the maintainer supplies, and writes:

```
fixtures/contracts/<date>-<claude-version>/
  events.stream.jsonl      one JSON object per line, unmodified
  stderr.txt
  exit-code.txt
  argv.txt                 exactly what we passed
  env-sanitized.txt        environment with secrets removed, keys replaced by names
  version.txt
```

Recordings are committed. They are the only place a real provider's output enters the repository, and the sanitiser is what makes that safe.

## The pinned test

```kotlin
class EventContractTest {
    @Test
    fun `every event type in the 2.1.x recording maps to a known AgentEvent`() {
        for (line in Fixture.lines("2.1.211")) {
            val parsed = EventParser.parse(line)
            assertThat(parsed).isInstanceOf(KnownEvent::class.java)
        }
    }
}
```

Four assertions, per recording, per event type:

1. **Parseable.** Every line parses or becomes a `ParseFailed`. No exceptions.
2. **Known.** Every event maps to a subtype declared in `02-architecture/event-protocol.md`. An `UnknownEvent` here is a contract change, not a malformed line.
3. **Non-lossy.** Concatenating all `text_delta` values reproduces the final message text exactly. This is the assertion that catches a dropped event.
4. **Stable.** Fields we depend on are present with the expected type; a field that became optional, was renamed, or changed type fails the build.

## The compatibility rule

Contract tests do not block on a new event type. They block on a *broken* one.

| Change | Response |
|---|---|
| New event `type` we do not know | Test records it as `UnknownEvent` and **passes**. A follow-up task is created to handle it. |
| New field on a known event | Passes. Added to the parser if useful. |
| A field we depend on becomes optional | **Fails.** Requires a code change and a decision. |
| A field is renamed or changes type | **Fails.** |
| A field disappears | **Fails.** |
| Exit code semantics change | **Fails**, and the run state machine is re-examined |
| A new required flag | Detected by `argv.txt` comparison; **fails** |
| An existing flag is removed | **Fails** |

The rationale: an unknown event is a feature gap we can ship around; a changed field we depend on is a correctness bug we cannot.

## Flag contract

We assert the exact argv we construct, so a refactor that silently drops `--output-format stream-json` fails on a build machine.

| Flag | Why we depend on it |
|---|---|
| `--print` | Non-interactive; without it the CLI waits for a TTY |
| `--output-format stream-json` | Structured events instead of prose |
| `--verbose` | **Required** alongside `stream-json`; without it the output is not a full event stream |
| `--include-partial-messages` | Token-level deltas for the streaming UI |
| `--permission-mode` | Coarse alignment with our autonomy level; our executor remains the real gate |
| `--allowed-tools` / `--disallowed-tools` | Defence in depth, not the primary control |
| `--model` | Per-project and per-run selection |
| `--mcp-config` | Only when the user has MCP servers |
| `--session-id` / `--resume` / `--continue` | Session lifecycle |
| `--add-dir` | Attachment paths |
| `--json-schema` | Planner output in phase 4 |

A test asserts the constructed argv for each combination in `08-orchestration/agent-request-lifecycle.md` — 14 combinations, each with the exact expected argument vector.

## Version gating

We track a minimum and a known-good version:

| Constant | Value | Effect |
|---|---|---|
| `MINIMUM_SUPPORTED_CLAUDE_VERSION` | Set at build time from the runtime manifest | Below this, the runtime is rejected and the user is told to update |
| `KNOWN_GOOD_VERSION` | The version the contract recordings were taken from | Used to offer a rollback |
| `VERIFIED_VERSIONS` | A set | Anything outside the set gets a warning, not a block, if it satisfies the minimum |

Version parsing is a unit test: `2.1.211` sorts correctly, `2.10.0` sorts after `2.9.9`, a nightly string is handled, and a malformed string fails closed.

## The re-record ritual

1. A nightly job checks whether a newer `claude` release exists.
2. If yes, it records a new fixture set in a branch — no automatic merge.
3. The pull request shows the diff of *events*, not of code, and the compatibility table from this document is the review checklist.
4. A new event type is fine, and the reviewer writes down whether we want to handle it now.
5. A changed field is a bug fix with a test, merged deliberately.

## The failure mode this prevents

The worst realistic outcome for this product is a Claude Code update that changes a field name, so the app parses an empty message, shows an empty answer, and reports success. `DONE` would then be shown for a run that did nothing. The non-lossy assertion and the exit-code contract exist specifically to make that a failed build.
