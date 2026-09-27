# Integration tests

Instrumented tests: real Android framework, real files, real processes, real SQLite. Slower and fewer than unit tests, and they cover the things a JVM test cannot honestly reach.

## What belongs here

| Area | Why it cannot be a unit test |
|---|---|
| Room with a real file database | The annotation processor, the query planner, and the WAL behaviour only exist on a device |
| `ProcessBuilder` and process trees | Fork, exec, signal delivery, and exit codes behave differently on Bionic than on glibc |
| `Parcelable` and `Bundle` round-trips | A parcel bug survives the JVM because the JVM has no parcels |
| WorkManager scheduling and constraints | Real alarms, real Doze behaviour |
| Keystore operations through the AndroidKeyStore provider | Only exists on a device |
| Notification channels, foreground services | Real binder calls |
| FileProvider URI grants | Real `ContentProvider` authorisation |
| Compose rendering and semantics | Real measure/layout, real accessibility tree |
| `SharedPreferences` and DataStore with real files | Atomicity and corruption behaviour |
| Encrypted file storage with real keys | JCE provider behaviour |

## The emulator matrix

| Job | API | ABI | Notes |
|---|---|---|---|
| Fast lane (default for every push) | 34 | `x86_64` | One image, one device, everything green |
| Oldest supported | 26 | `x86_64` | Guards against accidental API-level use |
| Arm64 native runtime | 34 | `arm64-v8a` | The profile that actually matters; runs nightly |
| Physical-device smoke | current | `arm64-v8a` | Before every release tag |
| Foldable | 34 | `x86_64` | Window-size class behaviour |

The fast lane is the only one that blocks a push. The others are nightly or pre-release, because a four-emulator matrix on every push is a queue nobody waits for.

## Test inventory

### 1. Persistence on a real database — 34 tests

| Group | Rows | Cases |
|---|---|---|
| Create and upgrade | 12 | Write a database at every historical schema version, migrate forward, assert data survives. One test per migration step. |
| Migration with real data | 10 | The same, but with a populated database: 500 sessions, 200 runs, the full transparency log. Assert row counts *and* spot-checked content, not just that the migration did not throw. |
| Transaction integrity | 6 | Kill the app mid-transaction; the database is either fully before or fully after, never half |
| Query performance | 6 | The chat-list query over 10 000 sessions under 50 ms; the search query under 300 ms; measured with `Trace` on a mid-range device |

### 2. Process supervision — 28 tests

| Group | Rows | Cases |
|---|---|---|
| Launch and exit | 6 | A trivial process; exit codes 0, 1, 42, 127 |
| Signal delivery | 6 | `SIGTERM` and `SIGINT` reach the child; `SIGKILL` always works; a process that ignores `SIGTERM` is killed after the grace period, and the log says so |
| Process tree | 6 | Killing a parent that spawned a shell which spawned the real worker: nothing is orphaned. Verified by scanning `/proc` for the descendants after the kill |
| Zombie reaping | 4 | A child that exits while nobody waits; no zombie accumulates over 1 000 spawn/kill cycles |
| Resource limits | 3 | A process that allocates 1 GB: the app survives, reports the memory pressure, and can kill it |
| The Bionic/glibc difference | 3 | A glibc-linked binary launched from the app fails with the documented `ENOENT`; the app recognises that specific failure and routes to the right diagnosis, rather than showing a generic crash |

### 3. Storage and Keystore — 20 tests

| Group | Rows | Cases |
|---|---|---|
| Key generation | 5 | AES key in AndroidKeyStore, GCM, biometric-bound where required |
| Encrypt/decrypt round-trip | 4 | Including a 100 KB value, an empty value, and a value with multi-byte characters |
| Tamper detection | 4 | A flipped ciphertext byte fails authentication and produces a specific error, never a partial plaintext |
| Key invalidation | 4 | After `setInvalidatedByBiometricEnrollment`, reads fail cleanly and the user is asked to re-enter — the app does not crash on the app lock path |
| FileProvider | 3 | A URI grant for a share intent, scoped to one file, and revoked after |

### 4. Background execution and notifications — 18 tests

| Group | Rows | Cases |
|---|---|---|
| Foreground service | 6 | Starts, promotes the notification, respects the type requirement, stops cleanly |
| WorkManager | 5 | Retry with backoff, constraints honoured, cancelled when the user stops the run |
| Notification actions | 4 | Stop, and the "show me" deep link resolve to the right screen |
| Doze and app-standby | 3 | A run started before Doze continues; the elapsed time is honest about it |

### 5. Compose rendering and semantics — 40 tests

UI-level Compose tests. Details in `ui-tests.md`; the integration-specific part is that these run on a device with the real font, the real resources, and the real accessibility stack.

| Group | Rows |
|---|---|
| Screen renders each state without crashing | 16 (4 screens × loading, empty, error, content) |
| Accessibility tree is correct | 8 |
| Font scaling to 200 % does not clip | 6 |
| Dark mode and dynamic colour are honoured | 6 |
| Process recreation keeps state | 4 |

## Rules for instrumented tests

**Each test owns its files.** A `temporaryFolder`-style rule per test, named after the test. Two tests sharing a directory is a test that passes on Tuesday and fails on Wednesday.

**No network, ever.** `NetworkSecurityConfig` in the debug manifest blocks all cleartext and the test rule installs a `MockWebServer` where a server is genuinely needed. A test that reaches the internet is a test that fails when the CI network does.

**No shared device state.** No `grantPermission` left behind, no `adb shell` residue, no shared SharedPreferences. A teardown rule asserts the app's data directory is empty afterwards.

**Real clocks are allowed, real waits are not.** A test that needs a 30-second WorkManager delay uses `WorkManagerTestInitHelper` to advance time. A test that sleeps 30 seconds is deleted.

**Assert on the user-visible result.** `assertThat(transportButton).assertIsDisplayed()` is fine. `assertThat(viewModel.state.value).isEqualTo(expected)` is a unit test that happens to be slow, and belongs in `unit-tests.md`.

## The physical-device suite

Runs on a real device before a release tag, because some things only break on real hardware and a test that cannot run in CI still has to exist:

| Test | Why it needs hardware |
|---|---|
| The native runtime profile installs and runs a trivial command | Real ARM, real page sizes |
| A full run survives the screen being off for 10 minutes | Real Doze, real wake-lock behaviour |
| Push notification arrives and opens the right session | Real FCM-free local notification path |
| Biometric unlock works on a real sensor | Emulator biometrics are a mock |
| The app survives a low-memory kill and offers resumption | Real LMKD |
| 20 minutes of sustained running does not thermal-throttle itself | Real thermals |
