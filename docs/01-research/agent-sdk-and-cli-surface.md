# The Claude Code CLI and Agent SDK surface we depend on

**Status:** verified 2026-09-27 against the official documentation. Every item here is a dependency. If one changes, `06-runtime/native-profile.md` or `02-architecture/event-protocol.md` changes with it, and a contract test fails.

## Which surface we use

We drive Claude Code as a **subprocess**, not by embedding the Agent SDK. Reason: the Agent SDK bundles a native binary and expects a normal Linux/Node process environment. On a phone, downloading the CLI directly gives us control over the version, the checksum, and the patching, which is the entire problem we are solving anyway.

Consequence: the CLI's stdout contract is our integration surface, and it is versioned by accident rather than by promise. Everything below is therefore wrapped, tested against recorded fixtures, and designed to fail loudly.

## Invocation

Baseline for a non-interactive run:

```bash
claude -p "<prompt>" \
  --output-format stream-json \
  --verbose \
  --include-partial-messages \
  --permission-mode acceptEdits \
  --allowedTools "Read,Edit,Glob,Grep,Bash" \
  --add-dir /path/to/project
```

### Output formats

| Format | Shape | Use |
|---|---|---|
| `text` | Plain final answer | Debugging only. Useless for a live UI. |
| `json` | Single object with `result`, `session_id`, cost, usage | Final summary after a run. |
| `stream-json` | Newline-delimited JSON, one object per line | **Our primary mode.** |
| `stream-json` + `--include-partial-messages` | Adds token-level deltas | **Live typing effect.** |

`--verbose` is required alongside `stream-json`; without it the output is not what we expect. This is a known footgun and is asserted in a contract test.

### Streaming events we consume

Deltas arrive as `stream_event` objects wrapping a provider event; text arrives as `text_delta`:

```json
{"type":"stream_event","event":{"delta":{"type":"text_delta","text":"..."}}}
```

Subagent messages are ordinary assistant and user messages carrying a non-null `parent_tool_use_id`. The first message from a subagent is a *user* message carrying the prompt that drove it. Forwarding subagent text and thinking requires `--forward-subagent-text` or `CLAUDE_CODE_FORWARD_SUBAGENT_TEXT`, on Claude Code 2.1.211 or later. Without it, we see only tool use and tool results from subagents.

We enable forwarding, and we build the nesting tree from `parent_tool_use_id`. This is how the UI nests a subagent's work under the tool call that spawned it.

### Structured output

```bash
claude -p "Extract the function names" \
  --output-format json \
  --json-schema '{"type":"object","properties":{"functions":{"type":"array","items":{"type":"string"}}},"required":["functions"]}'
```

The schema is validated by the CLI and a bad schema exits non-zero with a diagnostic. `format` is accepted but treated as an annotation, not enforced.

We use this for exactly one thing: asking the planner for a machine-readable plan. Everything else is streamed text.

### Retry events

A retryable API failure emits a `system` message with `subtype: "api_retry"` before retrying:

```json
{
  "type": "system",
  "subtype": "api_retry",
  "attempt": 1,
  "max_retries": 5,
  "retry_delay_ms": 2000,
  "error_status": 529,
  "error": "overloaded"
}
```

`error` is one of: `authentication_failed`, `oauth_org_not_allowed`, `account_on_hold`, `billing_error`, `rate_limit`, `overloaded`, `invalid_request`, `model_not_found`, `server_error`, `max_output_tokens`, `cloud_credential_error`, `unknown`.

The app surfaces this as a real state — "Anthropic ist überlastet, Versuch 2 von 5" — rather than a silent stall. This is the single most important thing to show during a long run, because the alternative is a user assuming the app has hung.

## Permission modes

| Mode | Behaviour | Where we use it |
|---|---|---|
| `default` | Asks for most things | `ASK_EVERYTHING` |
| `acceptEdits` | Auto-approves file edits, still asks for commands | `ASK_RISKY` |
| `plan` | No changes permitted at all | The read-only "just plan it" mode |
| `bypassPermissions` | Nothing asks | `FULL_AUTO` |

**Our hard blocks survive this.** At `bypassPermissions` the CLI itself will not ask. That is precisely why `HardBlockPolicy` sits in our own executor layer, in front of the tool dispatch, and not in a post-hoc filter. See `08-orchestration/permissions.md`.

## Bare mode

`--bare` skips auto-discovery of hooks, skills, custom commands, subagents, plugins, MCP servers, auto memory, and `CLAUDE.md`. It is faster and deterministic, and it is the recommended mode for scripted and SDK use.

We do **not** use `--bare` for normal runs, because skills and `CLAUDE.md` are features. We do use it for internal smoke tests and CI, where determinism matters more than capability.

A directory added with `--add-dir` is a partial exception: bare mode loads skills from its `.claude/skills/`, but still skips its `.claude/commands/` and `.claude/agents/`.

## Sessions

- `--continue` resumes the most recent session.
- `--session-id <id>` resumes a specific one.
- `--resume` brings up a picker, which is not useful for us; we track session IDs ourselves.
- A session ID appears in the JSON result. We store it, so a conversation survives the process.

Sessions are stored as files on disk. Killing the process does not lose the conversation, which is what makes interruption safe.

## Exit codes and signals

| Code | Meaning | Our behaviour |
|---|---|---|
| `0` | Success | Judge runs. "Done" depends on verification, not on this code. |
| non-zero | The run failed | Read the error, classify it, decide whether to retry. |
| `143` | Terminated by SIGTERM | We treat this as *cancelled*, not failed. The turn was left unfinished and no result was recorded. |
| — | SIGINT | The correct way to end a turn cleanly rather than kill the process. We send SIGINT first, wait 2 s, then SIGTERM. |

On SIGTERM the CLI terminates the whole process tree of any running Bash command, runs `SessionEnd` hooks, and exits. While exiting it starts no new tool call and sends no new model request.

After an interrupted turn, the session resumes with the interrupted turn left as it is, and the next prompt drives the conversation. Setting `CLAUDE_CODE_RESUME_INTERRUPTED_TURN=1` makes the next resume continue that turn instead. We set it, because "continue what you were doing" is what the user expects after tapping stop-and-resume.

## Timeouts and background work

- A background Bash task started during a run is terminated about five seconds after the result and stdin close.
- A background subagent or workflow keeps the process open until it completes.
- Waiting ends after 10 minutes of continuous idle by default, tunable with `CLAUDE_CODE_PRINT_BG_WAIT_CEILING_MS`; `0` waits forever.
- A Monitor watch makes the CLI wait for the watch, capped at the same 10 minutes; the watch itself times out after five minutes.

We set the wait ceiling generously and rely on our own cancellation, because we want a long build to be allowed to finish while we still retain the ability to stop it.

## Stdin

Non-interactive mode reads stdin, so we can pipe data in and redirect out. Piped stdin is capped at 10 MB; beyond that the CLI exits with a clear error and a non-zero status. We therefore write large inputs to a file in the project and reference the path in the prompt. This matters for large log files and diffs, which is a common case for us.

If stdin cannot be read, the CLI warns to stderr and continues with the prompt from the command line. Newer versions do not crash.

## Authentication

- `ANTHROPIC_API_KEY` in the environment. The SDK does not load `.env` files, and neither do we; we inject the environment explicitly.
- Amazon Bedrock: `CLAUDE_CODE_USE_BEDROCK=1` plus AWS credentials.
- Claude on AWS: `CLAUDE_CODE_USE_ANTHROPIC_AWS=1` plus a workspace id.
- Google Cloud's Agent Platform: `CLAUDE_CODE_USE_VERTEX=1`.
- Microsoft Foundry: `CLAUDE_CODE_USE_FOUNDRY=1`.
- `apiKeyHelper` in settings JSON, for a key that must be fetched at runtime.

**We use the API-key path exclusively.** A claude.ai subscription login is not supported, not offered, and not possible: Anthropic does not permit third-party applications to offer claude.ai login or subscription rate limits, including agents built on the Agent SDK.

## Things this project depends on that could break

| Dependency | Risk | Mitigation |
|---|---|---|
| `--output-format stream-json` shape | High | Contract tests against recorded fixtures; a change fails the build |
| `--verbose` requirement | Medium | Asserted in a contract test with a clear failure message |
| `parent_tool_use_id` semantics | Medium | Feature-detected: absent means we render subagents flat |
| `api_retry` event | Medium | Absence degrades to a generic progress spinner, not a crash |
| Permission mode names | High | Validated at startup; an unknown mode stops the server and lists valid ones |
| `CLAUDE_CODE_RESUME_INTERRUPTED_TURN` | Low | Optional; we degrade to manual resume |
| Exit code 143 semantics | Low | Tested by actually sending both signals |
