# Module map

Each module, what it is responsible for, what it exposes, and what it may not depend on. "May not depend on" is enforced by `tools/check_no_android_imports_in_shared.py` and by Gradle module boundaries, not by good intentions.

## Gradle module graph

```
                        ┌──────────────┐
                        │   :app       │  Android application, DI wiring, services
                        └──────┬───────┘
             ┌─────────────────┼─────────────────┐
             │                 │                 │
      ┌──────┴──────┐  ┌───────┴───────┐  ┌──────┴──────┐
      │ :ui-feature │  │  :android-    │  │ :runtime    │
      │ Compose UI  │  │  platform     │  │ bootstrap,  │
      │ (KMP)       │  │  Keystore,    │  │ supervise,  │
      │             │  │  foreground   │  │ pty         │
      └──────┬──────┘  └───────┬───────┘  └──────┬──────┘
             │                 │                 │
     ┌───────┼─────────────────┼─────────────────┤
     │       │                 │                 │
┌────┴────┐┌─┴──────────────┐┌──┴──────────┐
│ :domain ││ :data          ││ :core       │
│ pure    ││ Room, Ktor,    ││ Result,     │
│ Kotlin  ││ repositories   ││ dispatchers │
└────┬────┘└─┬──────────────┘└──┬──────────┘
     │        │                 │
     └────────┴─────────────────┘
              │
      ┌───────┴────────┐
      │ :orchestration │  plan · verify · judge · retry
      └───────┬────────┘
              │
      ┌───────┼────────┬──────────────┐
      │       │        │              │
┌─────┴──┐┌──┴─────┐┌─┴──────┐┌──────┴─────┐
│ :vcs   ││ :skills││ :providers│ :execution │
│ git,   ││ parse, ││ Anthropic │ backends   │
│ GitHub ││ install││ OpenAI,   │ (interface │
│        ││        ││ custom    │  + impls)  │
└────────┘└────────┘└──────────┘└────────────┘
```

## Responsibilities

### `core`

Foundation types with no opinion about the domain.

**Exposes:** `Outcome<T>`, `AppError`, dispatchers, `TimeProvider`, `IdGenerator`, `Logger`, `Redactor`, platform gateways (`ProcessGateway`, `CryptoGateway`, `FileSystemGateway`, `NetworkMonitor`, `ClipboardGateway`, `PlatformCapabilities`).

**May not depend on:** anything except the Kotlin standard library and kotlinx.

**Rule:** every platform operation goes through an `expect` declaration here. No other module declares an `expect`.

### `domain`

Models and business rules. The part you would unit-test if you could only test one thing.

**Exposes:** `Project`, `Conversation`, `Turn`, `Message`, `ToolInvocation`, `Plan`, `FileChange`, `DiffEntry`, `VerificationRun`, `CostRecord`, `SkillDescriptor`, `ProviderConfig`, `AutonomyLevel`; policies `HardBlockPolicy`, `PermissionResolver`, `BudgetPolicy`; use cases `RunProjectTask`, `ApprovePermission`, `InterruptRun`, `VerifyProject`, `CommitAndProposePullRequest`.

**May not depend on:** Room, Ktor, Koin, Android, Compose, the CLI. Pure Kotlin plus `kotlinx.coroutines` for `Flow` and the serializable annotations.

**Rule:** this is where "what is true" lives. A rule that could be expressed as a pure function belongs here, not in a repository.

### `data`

Persistence and network.

**Exposes:** `AppDatabase`, DAOs, repositories, `AgentStreamClient`, `CostEstimator`, DataStore-backed settings.

**May not depend on:** UI, orchestration. Repositories return domain types and `Outcome`; they never emit UI events.

**Rule:** no ViewModel touches a DAO. Not "prefer not to" — not at all.

### `runtime`

Making Claude Code exist and stay alive on a device.

**Exposes:** `RuntimeProfile`, `BootstrapManager`, `BootstrapStateMachine`, installers, `ProcessSupervisor`, `ProcessTree`, `PtyBridge`, `TerminalSession`, `HealthCheck`.

**May not depend on:** UI, domain orchestration, git.

**Rule:** every step is idempotent and resumable. Code here is the most likely to be wrong on a real device, so it is the most heavily tested with fakes and the most carefully reported to the user.

### `execution`

Where Claude Code actually runs.

**Exposes:** `ExecutionBackend`, `BackendCapabilities`, `AgentRequest`, `BackendState`, and the four implementations.

**May not depend on:** UI, Room, orchestration. It knows about processes and streams, not about projects or chat.

**Rule:** one implementation per location. A new location is a new file and a new screen, and touches nothing above.

### `orchestration`

The autonomy engine.

**Exposes:** `AgentSessionManager`, `Planner`, `Executor`, `Verifier`, `Judge`, `RetryController`, `AntiLoopDetector`, `CheckpointManager`, `ContextBudgeter`, `SelfImprovementService`.

**May not depend on:** UI. It may depend on domain, execution, data, vcs.

**Rule:** orchestrators are deterministic given their inputs. Time, randomness, and I/O arrive through injected `TimeProvider` and repositories, which is what makes retry and loop logic testable.

### `vcs`

Git and GitHub.

**Exposes:** `GitClient`, `BranchManager`, `CommitBuilder`, `PushPolicy`, `PullRequestService`, `GitHubClient`, `GitHubAuthManager`, `NotificationPoller`, `GitCommandSanitizer`.

**May not depend on:** UI, runtime.

**Rule:** no command is ever built by string concatenation from user input. `GitCommandSanitizer` parses and re-emits from an allowlist of subcommands with typed arguments, so an injection is a type error rather than a runtime surprise. This is also what makes the never-delete rule structurally enforceable.

### `skills`

**Exposes:** `SkillParser`, `SkillValidator`, `SkillInstaller`, `SkillGenerator`, `SkillRegistry`, `SkillScopeResolver`, `MarketplaceClient`, `SkillEditorModel`.

**May not depend on:** UI, runtime.

**Rule:** installation is a preview-then-confirm operation. The installer returns a plan; the UI shows it; only a confirmed call writes.

### `providers`

**Exposes:** `ProviderConfig`, `ModelSpec`, `AnthropicClient`, `OpenAIClient`, `ConnectionTester`, `ModelDiscovery`.

**May not depend on:** UI.

**Rule:** a provider never sees a secret's value, only a `SecretRef` that it resolves at the moment of use, through `CryptoGateway`.

### `ui`

Compose Multiplatform, the design system, and every screen.

**Exposes:** screen composables, `DesignTokens`, `AnimatedClaudeMark`, shared components.

**May not depend on:** `core` platform `actual`s directly, `data` internals, Room.

**Rule:** screens are stateless where they can be. A screen takes a `UiState` and emits events. Screens that need to be pragmatic (the terminal) may hold local state, and say so in a comment.

### `androidApp`

The Android-specific shell.

**Exposes:** `MainActivity`, `App`, DI modules, `AgentForegroundService`, `NotificationChannels`, `BiometricGate`, `KeystoreSecretStore`, `AndroidDatabaseFactory`, `AppUpdater`, widgets, resources.

**May not depend on:** nothing. It is the top.

**Rule:** everything here is `actual` or platform glue. Business logic that lands here is a bug and should move down.

## Dependency rules, machine-checked

| Check | Tool | Fails the build when |
|---|---|---|
| Shared purity | `check_no_android_imports_in_shared.py` | `android.*` or `androidx.*` is imported in `shared/core` or `shared/domain` |
| Module boundaries | Gradle project dependencies | A module depends on something above it in the graph |
| Domain purity | Ktlint custom rule | `domain` imports `androidx.room`, `io.ktor`, or `org.koin` |
| Doc manifest | `check_doc_manifest.py` | A listed document is missing or is a stub |
| Source manifest | `check_source_manifest.py` | A listed file is missing, or an unlisted file was added |
| Analytics | `check_no_analytics.py` | A known analytics host or SDK appears in any module |

## Test location

| Module | Unit tests | Integration | Note |
|---|---|---|---|
| `core` | alongside | — | Fakes for every gateway |
| `domain` | alongside | — | No fakes needed; this is the point |
| `data` | — | against in-memory Room and MockEngine | — |
| `runtime` | against a fake filesystem | against a real subprocess in a container | The container job runs nightly |
| `execution` | against a fake CLI script emitting recorded fixtures | against a real `claude` binary on a runner | Nightly |
| `orchestration` | against fake backends and a fake clock | — | Deterministic by construction |
| `vcs` | against a real temp git repository | — | Real git, not a mock; it is cheap and catches real mistakes |
| `skills` | against a fixture corpus | — | Includes deliberately malformed skills |
| `ui` | — | Compose UI tests and screenshot tests | Per screen |
