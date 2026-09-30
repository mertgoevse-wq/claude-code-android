# Data model

Every entity, every field, and why the field exists. Fields here without a consumer do not get added; that is the rule this file exists to enforce.

**Conventions:** primary keys are ULIDs (sortable by creation, no coordination, no collision on merge). Timestamps are epoch milliseconds, UTC. Enums are stored as their string name and are migrated additively. All monetary values are stored as integer micro-USD to avoid float drift.

## Entity map

```
Project ─┬─ Conversation ─┬─ Turn ─┬─ Message
         │                │        └─ MessagePart
         │                └─ ToolInvocation ── ToolResult
         ├─ ProjectSetting
         ├─ SkillInstall
         ├─ RemoteTarget
         ├─ Run ─┬─ Plan ── PlanStep
         │       ├─ FileChange ── DiffEntry
         │       ├─ VerificationRun ── TestResult
         │       ├─ CostRecord
         │       └─ Checkpoint
         └─ Branch
Provider ── ModelSpec
SecretProfile
SessionLogEntry   (append-only, references anything)
NotificationEvent
AppSetting
```

## Project

The unit of work and the unit of trust. Autonomy lives here, not globally, because a throwaway script and a production app must not behave the same.

| Field | Type | Notes |
|---|---|---|
| `id` | ULID | PK |
| `name` | String | Shown on the card |
| `kind` | Enum | `LOCAL`, `CLONED`, `REMOTE` |
| `path` | String | Absolute path on the device, or the path on the runner |
| `backendId` | String? | Which execution backend runs it. Null for an unassigned project. |
| `vcsProvider` | String? | `GITHUB` or null for a non-repository project |
| `remoteOwner` | String? | `owner` |
| `remoteName` | String? | `repo` |
| `isPrivate` | Boolean | Always `true`. Stored explicitly so the invariant is visible, and asserted. |
| `defaultBranch` | String? | `main`, `master`, or whatever. Not assumed. |
| `description` | String? | Optional description of the project. |
| `isArchived` | Boolean | Archiving hides a project; it never deletes anything. |
| `lastRunAt` | Long? | Denormalised for sorting |
| `createdAt` / `updatedAt` | Long | |

**Index:** `(isArchived, lastRunAt)` for the project list; a unique index on `(remoteOwner, remoteName)` so a repository cannot be added twice.

**Invariant:** a project with `kind = LOCAL` has `vcsProvider = null`. A project with `kind = CLONED` must have all four remote fields.

## ProjectSetting

Separate from `Project` because it changes often and is read on every run. A separate table means a settings write does not contend with a project rename.

| Field | Type | Notes |
|---|---|---|
| `projectId` | ULID | PK, FK |
| `autonomyLevel` | Enum | `ASK_EVERYTHING`, `ASK_RISKY`, `AUTO_WITH_CHECKPOINTS`, `FULL_AUTO` |
| `modelProviderId` | String | FK |
| `modelId` | String | e.g. `claude-sonnet-4-5` |
| `verifyCommands` | List&lt;String&gt; | Serialised. Empty means "detect", not "skip". |
| `retryBudget` | Int | -1 means unlimited |
| `allowedTools` | List&lt;String&gt; | Empty means the level's default set |
| `deniedTools` | List&lt;String&gt; | Always unioned with the hard block list |
| `permissionMode` | Enum | The CLI mode mapped from the autonomy level |
| `offloadPolicy` | Enum | `NEVER`, `WHEN_HEAVY`, `ALWAYS` |
| `branchPrefix` | String | Default `task/` |
| `costAdvisoryThresholdUsd` | Long? | Soft warning only. Never a hard stop. |
| `autoCommit` | Boolean | Default true |
| `autoPr` | Boolean | Default true |
| `notificationsEnabled` | Boolean | Default true |

**Invariant test:** `autonomyLevel = FULL_AUTO` does not imply a relaxed deny list. The test asserts `deniedTools` always contains the hard blocks regardless of level.

## Conversation and Turn

A conversation is a thread of turns against a project. Separating them keeps a message list queryable without loading the tool tree.

**Conversation:** `id`, `projectId`, `title` (generated from the first user message, editable), `sessionId` (the CLI session id, for resume), `backendId`, `isArchived`, `createdAt`, `updatedAt`, `lastMessagePreview`.

**Turn:** `id`, `conversationId`, `index`, `userMessageId`, `startedAt`, `endedAt`, `state` (`PLANNING`, `RUNNING`, `AWAITING_PERMISSION`, `VERIFYING`, `DONE`, `FAILED`, `INTERRUPTED`, `CANCELLED`), `runId?`, `costUsd`, `attemptCount`.

**Message:** `id`, `turnId`, `role` (`USER`, `ASSISTANT`, `SYSTEM`), `createdAt`, `renderedMarkdown`, `parentToolUseId?` (for nesting subagents), `isRedacted` (true when the redaction pass altered it).

**MessagePart:** `id`, `messageId`, `kind` (`TEXT`, `CODE`, `THINKING`, `TOOL_CARD`, `DIFF_SUMMARY`, `ERROR`), `ordinal`, `payloadJson`, `collapsed` (user's own fold state, remembered per device).

**Indexes:** `Turn(conversationId, index)` unique; `Message(turnId, createdAt)`; `MessagePart(messageId, ordinal)` unique.

**Invariant:** a message is never edited or deleted. A redaction is a flag plus a replacement payload, so the original text is not retained in a column that a future reader might mistake for what was sent.

## ToolInvocation

The activity log, in structured form. This is what becomes a card in the chat and a row in the transparency view.

| Field | Type | Notes |
|---|---|---|
| `id` | ULID | PK |
| `turnId` | ULID | FK |
| `toolUseId` | String | The engine's id, for correlating start and result |
| `parentToolUseId` | String? | Non-null means a subagent |
| `name` | String | `Read`, `Edit`, `Bash`, … |
| `titleDe` / `titleEn` | String | Precomputed plain-language labels, so the UI never translates a tool name at runtime |
| `targetPath` | String? | The file or directory, when there is one |
| `inputJson` | String | Redacted before storage |
| `status` | Enum | `PENDING`, `RUNNING`, `DONE`, `ERROR`, `DENIED` |
| `startedAt` / `endedAt` | Long? | Elapsed time comes free |
| `outputPreview` | String? | First 2 KB, for the card |
| `outputRef` | String? | Path to the full log file on disk; the database does not hold megabytes |
| `isDestructive` | Boolean | Computed once by the policy, used for the warning icon |

**Index:** `ToolInvocation(turnId, startedAt)`, and a unique index on `(turnId, toolUseId)` so a retried tool call does not double-insert.

## Plan and PlanStep

| PlanStep | Notes |
|---|---|
| `id`, `planId`, `ordinal` | |
| `titleDe` / `titleEn` | Plain language, editable by the user |
| `acceptanceCriteria` | Text. What "done" means for this step. The verifier uses it. |
| `state` | `PENDING`, `ACTIVE`, `DONE`, `FAILED`, `SKIPPED` |
| `isCheckpoint` | A step where the run must stop and report |
| `startedAt` / `endedAt` | |
| `attemptCount` | Per step, for the retry budget |

**Invariant:** a plan with zero steps is invalid. An empty plan means the planner failed, and the app says so rather than treating it as "nothing to do".

## FileChange and DiffEntry

| FileChange | Notes |
|---|---|
| `id`, `runId`, `path` | |
| `changeType` | `ADDED`, `MODIFIED`, `DELETED`, `RENAMED` |
| `oldPath` | Set only for `RENAMED` |
| `linesAdded`, `linesRemoved` | From `git diff --numstat` |
| `binary` | Binary files get no hunks, and the UI says "binäre Datei" rather than rendering nothing |
| `hunksJson` | Hunk headers only, so the list view is cheap |

| DiffEntry | Notes |
|---|---|
| `fileChangeId`, `ordinal` | |
| `oldStart`, `oldLines`, `newStart`, `newLines` | |
| `linesJson` | The actual lines, each with kind and text |
| `decision` | `PENDING`, `ACCEPTED`, `REVERTED` — the user's per-hunk choice |
| `revertCommitId` | Set when reverted, so a revert is itself traceable |

**Invariant:** `decision = REVERTED` never deletes a file from disk. It applies an inverse patch, and the original content stays in git history. This is how "never delete" and "revert is available" coexist.

## VerificationRun and TestResult

| VerificationRun | Notes |
|---|---|
| `id`, `runId`, `attempt` | Attempt starts at 1 |
| `state` | `NOT_STARTED`, `RUNNING`, `PASSED`, `FAILED`, `ERROR`, `UNVERIFIED` |
| `commandCount` | |
| `durationMs` | |
| `logRef` | Path to the full output |
| `judgedAt` | Null until `Judge` has decided |

| TestResult | Notes |
|---|---|
| `verificationRunId`, `ordinal` | |
| `command` | Exactly what was run, including the working directory |
| `exitCode` | |
| `durationMs` | |
| `summaryLine` | The line a human would quote, extracted by a per-toolchain parser |
| `failedTestNames` | Serialised list, for the "3 tests failed" summary |
| `parserUsed` | `GRADLE`, `NPM`, `PYTEST`, `CARGO`, `GO`, `GENERIC`, `NONE` |

**Invariant:** `UNVERIFIED` is a first-class state, not an error and not a pass. A run with `UNVERIFIED` can never be shown as finished. `05-features/verification.md` defines the display rules.

## CostRecord

| Field | Notes |
|---|---|
| `id`, `runId`, `projectId`, `conversationId` | |
| `inputTokens`, `outputTokens`, `cacheReadTokens`, `cacheCreationTokens` | Integers. Zero when the provider does not report them |
| `costUsdMicros` | Integer micro-USD. Never a float. |
| `isEstimated` | True until the provider's final usage arrives |
| `modelId`, `providerId` | Denormalised, so a price change later does not rewrite history |
| `inputPricePerMtok`, `outputPricePerMtok` | The prices used for the estimate, recorded so an old record can be re-costed |

**Invariant:** the cost of a run is the sum of its `CostRecord`s, and an estimate is never overwritten silently. If the reported total differs from the estimate, both are kept and the UI shows the transition. See `05-features/chat-and-streaming.md`.

## Run

The execution record. One task, one run, one branch, at most one PR.

| Field | Notes |
|---|---|
| `id`, `projectId`, `conversationId`, `turnId` | |
| `backendId`, `backendProfile` | Which machine and which runtime profile |
| `state` | `CREATED`, `PREPARING`, `PLANNING`, `RUNNING`, `AWAITING_PERMISSION`, `VERIFYING`, `RETRYING`, `COMMITTING`, `PUSHING`, `DONE`, `FAILED`, `INTERRUPTED`, `CANCELLED`, `OFFLOADED` |
| `taskText` | What the user asked, verbatim |
| `branchName` | `null` until the branch exists |
| `commitSha` | `null` until committed |
| `pullRequestUrl` | `null` until opened |
| `attemptCount`, `maxAttempts` | For the display of "3 of 10" |
| `isOffloaded` | True if the run moved to a runner |
| `errorId` | FK to the error that ended it, if any |
| `startedAt` / `endedAt` / `durationMs` | |

**Indexes:** `Run(projectId, startedAt)`, `Run(state)` for "what is running right now".

## Provider and SecretProfile

| Provider | Notes |
|---|---|
| `id`, `name`, `kind` | `ANTHROPIC`, `OPENAI_CHAT`, `OPENAI_RESPONSES`, `CUSTOM` |
| `baseUrl`, `pathTemplate` | For `CUSTOM` |
| `secretProfileId` | FK, never the key itself |
| `headersJson` | For gateways |
| `isEnabled`, `isDefault` | One default provider |
| `lastTestedAt`, `lastTestResult` | For the connection test badge |
| `discoveredModels` | Cache of `/v1/models`, refreshed on test |

| SecretProfile | Notes |
|---|---|
| `id`, `name` | "Privat", "Arbeit", "Test" |
| `cipherText` | The encrypted key, from the Keystore |
| `iv`, `keyAlias` | Never the key material itself; the key lives in the Keystore |
| `hint` | Last four characters, so the user can tell profiles apart |
| `createdAt`, `lastUsedAt` | |

**Invariant:** no table other than `SecretProfile` holds key material. A test greps the schema for suspicious column names and fails on a hit.

## Skill and SkillInstall

| Skill | Notes |
|---|---|
| `id`, `name`, `description` | From frontmatter |
| `sourceKind` | `BUILTIN`, `GITHUB`, `ARCHIVE`, `AI_GENERATED` |
| `sourceUrl`, `sourceRef` | For updates |
| `contentHash` | For update detection |
| `isValid`, `validationError` | A broken skill is visible, not ignored |
| `createdAt`, `updatedAt` | |

| SkillInstall | Notes |
|---|---|
| `skillId`, `scope` | `GLOBAL` or `PROJECT` |
| `projectId` | Null when global |
| `installPath` | Where on disk |
| `isEnabled` | |
| `installedAt` | |

**Invariant:** the unique index is `(name, scope, projectId)`. Two installs of the same skill in the same scope cannot coexist, because the second would silently shadow the first.

## RemoteTarget

| Field | Notes |
|---|---|
| `id`, `projectId?` | Null means a runner available to all projects |
| `name` | "Mein Server", "Oracle Free" |
| `kind` | `SSH`, `ORACLE`, `GITHUB_ACTIONS` |
| `host`, `port`, `user` | For SSH |
| `secretProfileId` | The key or token |
| `status` | `UNKNOWN`, `ONLINE`, `OFFLINE`, `UNREACHABLE`, `QUOTA_EXCEEDED` |
| `lastProbeAt`, `lastProbeMessage` | |
| `capabilitiesJson` | Cores, RAM, disk, toolchain versions, probed not assumed |
| `isEnabled` | |

**Invariant:** capabilities are always the result of a probe. A remote target is never assumed to have a toolchain until it has run it.

## SessionLogEntry

Append-only. The transparency record. This table has no update and no delete path in the repository API, and a test asserts that.

| Field | Notes |
|---|---|
| `id` | ULID, PK |
| `timestamp` | |
| `runId?`, `projectId?`, `conversationId?` | Nullable so global events can be logged |
| `category` | `COMMAND`, `DIFF`, `PERMISSION`, `ERROR`, `COST`, `STATE`, `SYSTEM`, `NETWORK` |
| `severity` | `INFO`, `WARN`, `ERROR` |
| `message` | Redacted |
| `detailJson` | Redacted |
| `durationMs` | |

**Query patterns:** by run, by project, by time range, by category and severity. The transparency view and the diagnostics export both read from here.

## NotificationEvent

| Field | Notes |
|---|---|
| `id`, `runId?` | |
| `channel` | `RUN_DONE`, `RUN_FAILED`, `PERMISSION`, `GITHUB`, `RUNNER`, `UPDATE` |
| `titleDe` / `titleEn` | Precomputed; a notification is built without leaving the app |
| `bodyDe` / `bodyEn` | |
| `actionJson` | Buttons, e.g. Approve/Deny |
| `deliveredAt` | Null until sent |
| `readAt` | |

Notifications are built from precomputed text rather than formatted at post time, so a notification can be delivered from a background worker without constructing UI strings on the main thread.

## AppSetting

A key-value table for anything not worth a column: last used project, onboarding state, terminal font size, theme mode, language, notification battery threshold, diagnostics bundle version, update check timestamp.

**Invariant:** secrets are never in `AppSetting`. A test asserts that no value matching a key-shaped pattern can be written there.

## Deletion policy

There is no `delete` on any entity except through one audited path: **erase local project data**, which is a user-initiated, explicitly confirmed action in Settings that clears the database rows and the app-private files for projects the user marks for removal. It is not exposed in the project UI, it never touches a remote repository, never touches a branch, and never touches git history. The transparency log records it.

**This is the single exception to "never delete", and it is about the app's own cache of a project, not about the user's work.** It exists because otherwise there is no way to free storage. It cannot be triggered by the agent, at any autonomy level.
