# Dependency versions

Every direct dependency, pinned in `gradle/libs.versions.toml`, with the reason it is there. Versions come from one file; the update policy is at the bottom.

## The catalog

```toml
[versions]
agp = "8.9.1"
kotlin = "2.1.20"
ksp = "2.1.20-1.0.32"
composeBom = "2025.05.00"
room = "2.6.1"
coroutines = "1.9.0"
serialization = "1.7.3"
okhttp = "4.12.0"
retrofit = "2.11.0"
datastore = "1.1.1"
work = "2.9.1"
hilt = "2.53.1"
paparazzi = "1.3.5"
roborazzi = "1.26.0"
detekt = "1.23.7"
spotless = "6.25.0"
lifecycle = "2.8.7"
navigation = "2.8.5"
paging = "3.3.5"
sqlcipher = "4.6.1"
```

## Android and Kotlin

| Alias | Version | Why |
|---|---|---|
| `androidx.core:core-ktx` | BOM-adjacent 1.15.0 | Base APIs |
| `androidx.activity:activity-compose` | 1.10.0 | Compose host, `rememberLauncherForActivityResult` |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.8.7 | `collectAsStateWithLifecycle`, the correct default |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7 | ViewModel wiring |
| `androidx.lifecycle:lifecycle-process` | 2.8.7 | Foreground/background transitions |
| `androidx.navigation:navigation-compose` | 2.8.5 | Screen routing, typed routes |
| `androidx.navigation:navigation-compose` | 2.8.5 | |
| `androidx.paging:paging-compose` | 3.3.5 | Chat list and run history paging |
| `androidx.work:work-runtime-ktx` | 2.9.1 | Polling, download, deferred verification |
| `androidx.datastore:datastore-preferences` | 1.1.1 | Settings, non-secret preferences |
| `androidx.biometric:biometric` | 1.1.0 | App lock |
| `androidx.security:security-crypto` | 1.1.0-alpha06 | Only for the Keystore wrapper; see the note below |
| `androidx.sqlite:sqlite` | 2.4.0 | Bundled SQLite, for `SQLCipher` |

## Compose

The BOM pins everything Compose, so individual Compose artifacts carry no version:

| Alias | Artifact |
|---|---|
| `compose-bom` | `androidx.compose:compose-bom` |
| `compose.ui` | `androidx.compose.ui:ui` |
| `compose.ui.graphics` | `androidx.compose.ui:ui-graphics` |
| `compose.ui.tooling.preview` | `androidx.compose.ui:ui-tooling-preview` |
| `compose.material3` | `androidx.compose.material3:material3` |
| `compose.material3.window-size` | `androidx.compose.material3:material3-window-size-class` |
| `compose.animation` | `androidx.compose.animation:animation` |
| `compose.foundation` | `androidx.compose.foundation:foundation` |
| `compose.ui.test.junit4` | `androidx.compose.ui:ui-test-junit4` |
| `compose.ui.test.manifest` | `androidx.compose.ui:ui-test-manifest` |

Material 3 is the base, but the app's components are **hand-built on the design tokens** in `03-design/design-tokens.md`. Material components are used for behaviour (ripple, focus handling, semantics), never for their default look. See `03-design/anti-slop-rules.md`.

## Data

| Alias | Version | Why |
|---|---|---|
| `room-runtime`, `room-ktx`, `room-compiler` | 2.6.1 | Persistence. The schema is in `02-architecture/data-model.md`. |
| `sqlite-bundled` | 2.4.0 | Needed for SQLCipher, which replaces the platform SQLite |
| `sqlcipher-android` | 4.6.1 | Encrypted database. The transcripts contain prompts and code; they are encrypted at rest. |
| `kotlinx-serialization-json` | 1.7.3 | Event parsing. Chosen over Moshi/Gson for explicit null handling and Kotlin multiplatform portability. |
| `kotlinx-coroutines-core`, `-android`, `-test` | 1.9.0 | The concurrency model in `02-architecture/concurrency-model.md` is built on it |
| `okhttp` | 4.12.0 | Provider HTTP, GitHub API, runtime download |
| `retrofit`, `converter-kotlinx-serialization` | 2.11.0 | Typed GitHub and provider endpoints |
| `okhttp-logging-interceptor` | 4.12.0 | Debug builds only, with a redacting body logger |
| `datastore-preferences` | 1.1.1 | Settings |
| `paging-runtime`, `paging-compose` | 3.3.5 | Large lists |

## Injection

| Alias | Version | Why |
|---|---|---|
| `hilt-android`, `hilt-compiler` | 2.53.1 | Dependency graph |
| `androidx.hilt:hilt-work` | 1.2.0 | Injecting into `Worker`s |
| `androidx.hilt:hilt-navigation-compose` | 1.2.0 | ViewModel injection at the screen |

Hilt is used for the graph but kept out of the domain layer. `:core:model`, `:core:permissions`, `:core:verification`, and `:core:agent`'s pure parts have **no Android and no Hilt dependency at all** — they are plain Kotlin libraries, which is what makes them testable on the JVM.

## Security

| Alias | Version | Note |
|---|---|---|
| `sqlcipher-android` | 4.6.1 | Database encryption |
| `androidx.biometric` | 1.1.0 | App lock |
| `tink` | 1.15.0 | Key handling for non-Keystore material (runner host keys, the export passphrase) |

`androidx.security:security-crypto` is deliberately **not** used. `EncryptedSharedPreferences` has been deprecated in practice, its behaviour around key invalidation is a common crash source, and we do our Keystore work directly. The version catalog carries a comment saying so, because someone will otherwise try to add it back.

## Testing

| Alias | Version | Why |
|---|---|---|
| `junit` | 4.13.2 | The stable base |
| `mockk` | 1.13.13 | Mocking for the unit layer; final classes, coroutines |
| `kotlinx-coroutines-test` | 1.9.0 | Virtual time |
| `robolectric` | 4.14.1 | Android framework on the JVM, for Room and parcelables |
| `room-testing` | 2.6.1 | In-memory databases |
| `paparazzi` | 1.3.5 | Golden images on the host JVM |
| `roborazzi` | 1.26.0 | The cases Paparazzi renders unfaithfully |
| `androidx.test.ext:junit` | 1.2.1 | Instrumented tests |
| `androidx.test.espresso:espresso-core` | 3.6.1 | Only where Compose tests are insufficient |
| `androidx.compose.ui:ui-test-junit4` | BOM | Compose UI tests |
| `androidx.benchmark:benchmark-macro-junit4` | 1.3.3 | Startup and memory |
| `mockwebserver` | 4.12.0 | Replayed HTTP |
| `quickcheck` | 0.6.0 | Property-based fuzzing of the parser |

## Static analysis and formatting

| Alias | Version |
|---|---|
| `detekt` (with `detekt-formatting`) | 1.23.7 |
| `spotless` | 6.25.0, ktlint 1.5.0 |
| `android-lint` | bundled with AGP |
| `dependency-analysis` | 1.4.4 |
| `gradle-dependency-substitution` | 1.2.0 |

## The update policy

| Kind of update | Cadence | Process |
|---|---|---|
| Security patch | Immediately | Out-of-band, with a regression pass on the affected area |
| Patch/minor of a used API | Monthly | One pull request, all deps of the same kind together, CI green |
| Major | Quarterly, and only when needed | A branch, a migration note in the changelog, and an explicit decision not to upgrade when the cost exceeds the benefit |
| Adding a new dependency | Per pull request | Requires a line in the PR description saying why an existing one cannot do it |

**Versioning scheme:** `major.minor.patch`, and the `major` is only bumped when we deliberately do the migration work. A major bump in a routine dependency PR is rejected.

**The lockfile is committed.** Dependabot opens grouped pull requests; a human reads the changelog, not just the diff. Automated version bumps do not merge on their own.

**Dependency hygiene gate.** `dependency-analysis` fails the build on an unused dependency and on a runtime dependency that is declared as `implementation` where `api` is required (or the reverse). Unused transitives are reported weekly, not gated, because the list is long and the risk is not.

**Binary size gate.** A CI step reports the APK size and the method count against `10-build/gradle-setup.md` and `09-testing/performance-budgets.md`. A regression over 500 KB requires an explanation.

## What is deliberately not a dependency

| Not used | Because |
|---|---|
| Retrofit for provider APIs | Providers differ too much in their streaming shapes; a hand-written SSE client over OkHttp is smaller and more testable. See `07-integrations/providers.md`. |
| Hilt in the domain layer | It would make pure logic require an Android runtime. |
| Glide / Coil | Images are local attachments only, downscaled before they are ever decoded. A hand-written decoder path with `ImageDecoder` is smaller than either. |
| A DI framework for the Compose layer | `hilt-navigation-compose` is enough. |
| kotlinx-datetime | minSdk 26 covers the desugaring need, and `java.time` is available. |
| A logging framework | See the reasoning in `11-operations/logging.md`. |
| A JSON schema validator at runtime | Validation happens against a hand-written validator, which produces better error messages. |
