# Local build and run

Every command needed to go from a fresh clone to an installed, debuggable app, with what it should print and what to do when it does not.

## Prerequisites

| Requirement | Version | Check |
|---|---|---|
| JDK | 21 (Temurin recommended) | `java -version` |
| Android SDK | Platform 35, Build-Tools 35.0.0, Platform-Tools | `sdkmanager --list_installed` |
| Android Studio | Ladybug or newer, for the IDE experience only — the Gradle build does not need it | — |
| Device or emulator | API 26+, ideally API 34+ | `adb devices` |
| Rust toolchain | Only for the native helper, only if you change C code | `cargo --version` |

The Gradle wrapper downloads Gradle itself. Do not install Gradle.

## From clone to APK

```bash
# 1. Clone and point at your SDK
git clone https://github.com/<you>/claude-code-android.git
cd claude-code-android
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# 2. Sanity-check the toolchain
./gradlew --version
# Gradle 8.11.1, JVM 21 required

# 3. Build
./gradlew :app:assembleDebug
# BUILD SUCCESSFUL in 2m 41s

# 4. Install and run
./gradlew :app:installDebug
adb shell am start -n dev.ccandroid.debug/dev.ccandroid.MainActivity
```

If step 2 does not report JVM 21, `JAVA_HOME` is pointing somewhere else:

```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64   # adjust to your machine
```

## The command list

### Everyday

| Command | What it does |
|---|---|
| `./gradlew :app:assembleDebug` | Debug APK into `app/build/outputs/apk/debug/` |
| `./gradlew :app:installDebug` | Build and install on the connected device |
| `./gradlew :app:runDebug` | Install, launch, and stream logcat filtered to our tag |
| `./gradlew :app:assembleRelease` | Release APK; unsigned unless `keystore.properties` exists |
| `./gradlew clean` | Delete all build output. `rm -rf` of the same directories is faster. |

### Testing

| Command | Scope | Time |
|---|---|---|
| `./gradlew test` | Every unit test on the JVM | < 90 s |
| `./gradlew :core:permissions:test` | One module's tests | < 15 s |
| `./gradlew testDebugUnitTest` | Unit tests for the app module only | < 40 s |
| `./gradlew :app:connectedDebugAndroidTest` | Instrumented tests on the connected device | < 4 min |
| `./gradlew verifyPaparazziDebug` | Golden-image comparison | < 60 s |
| `./gradlew recordPaparazziDebug` | Accept new goldens (review the diff!) | < 60 s |
| `./gradlew :app:connectedBenchmarkAndroidTest` | Startup and memory benchmarks | < 6 min |
| `./gradlew check` | Lint, detekt, spotless, all JVM tests, goldens | < 5 min |

### Diagnostics

| Command | Purpose |
|---|---|
| `./gradlew :app:dependencies` | The full dependency graph for the app |
| `./gradlew buildEnvironment` | The resolved toolchain, repositories, and plugins |
| `./gradlew --scan` | Build scan upload (opt-in) |
| `./gradlew :app:assembleDebug --info` | Full task log, for a dependency resolution failure |
| `./gradlew --stop` | Kill the daemon — the first thing to try on a strange failure |
| `./gradlew --no-configuration-cache :app:assembleDebug` | Bypass the configuration cache when debugging a build script |
| `./scripts/check-hard-blocks.sh` | The safety invariants |
| `./scripts/check-layer-boundaries.sh` | The module import graph |
| `./scripts/check-no-secrets.sh` | The credential scan |

## Runtime setup inside the app

The app does not ship a Claude Code binary. On first run, onboarding walks through:

1. Choose a runtime profile — `native` (default, ~300 MB download) or `proot` (~2 GB, more compatible).
2. The download runs in a `WorkManager` job with a foreground-service notification. It survives the screen being off.
3. The archive's SHA-256 is verified against the pinned digest. **A mismatch aborts**; nothing is installed.
4. The binary is patched with `patchelf` for the Bionic environment, then probed: a trivial command is run and its output checked. A binary that downloads fine and does not run is caught here, not three hours into a task.
5. The runtime is marked ready, and the profile is shown in settings with its size and version.

To do this without the UI, for a developer:

```bash
adb shell am start -n dev.ccandroid.debug/dev.ccandroid.MainActivity \
  --ez deepLink runtimeSetup
```

To install a runtime built locally:

```bash
./gradlew :app:assembleDist -PclaudeVersion=2.1.211 -PruntimeChecksum=<sha256>
adb install -r app/build/outputs/apk/dist/app-dist.apk
```

## Running against a real provider

Only for manual verification. Never in a test (see `09-testing/test-data-safety.md`).

```bash
# The key is read from the environment and never written to disk by the build.
export ANTHROPIC_API_KEY=sk-ant-...
./gradlew :app:installDebug -PmanualKey
```

The debug build prints a persistent banner while a manual key is active, and the diagnostics screen shows `manual key: active`. The key lives in the app's Keystore-backed store, as it would for a real user.

## Common failures

| Symptom | Cause | Fix |
|---|---|---|
| `SDK location not found` | No `local.properties` | Write it; see step 1 |
| `Unsupported class file major version` | JDK 17 or 23 on the PATH | `export JAVA_HOME=…21…` |
| `Plugin [id: 'com.android.application'] was not found` | Repositories blocked by a network policy, or an `offline` flag | Run without `--offline`; check the proxy |
| `Duplicate class androidx.*` | A dependency added twice through different paths | `./gradlew :app:dependencies` and look for the duplicate |
| `Execution failed for task :core:database:kspDebugKotlin` | Room schema changed without a migration | See `02-architecture/data-migrations.md` — a test enforces this |
| Configuration cache problems | A build script read the environment | Add a provider; see `convention-plugins.md` |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | A release build over a debug build with the same ID, or a different signing key | Uninstall first: `adb uninstall dev.ccandroid.debug` |
| Goldens fail on a different machine | Font rendering differs | Paparazzi renders on the host with bundled fonts, so this should not happen; if it does, the golden was recorded with a different JDK. Re-record and say why. |
| The emulator is offline after a snapshot load | A known AVD bug | `adb kill-server && adb start-server`, then wipe the AVD |
| `adb: device unauthorized` | The USB debugging prompt was dismissed | Reconnect and accept on the device |

## Faster iteration

```properties
# gradle.properties.local — not committed, machine-specific
org.gradle.daemon=true
org.gradle.parallel=true
kotlin.incremental.useClasspathSnapshot=true
```

```bash
# Only re-run what changed
./gradlew :app:assembleDebug --configure-on-demand

# Skip the slow suites while iterating on UI
./gradlew :app:assembleDebug -x test -x lint -x verifyPaparazziDebug

# Live logcat from our tag only
adb logcat --pid=$(adb shell pidof dev.ccandroid.debug)
```

## Editor setup

| Editor | Settings |
|---|---|
| Android Studio | Uses the wrapper by default. Enable "Apply active variant" so a feature module's variant follows the app. |
| VS Code | Install the Kotlin and Gradle extensions. The wrapper is detected automatically. |
| Neovim | `java` points at JDK 21; run `./gradlew ktlintFormat` before commit. |

The `EditorConfig` file is committed, and ktlint enforces the Kotlin half of it, so formatting is identical regardless of editor.

## Verifying a change end to end

The full local gate before opening a pull request:

```bash
./gradlew clean check :app:assembleDebug
./scripts/check-layer-boundaries.sh
./scripts/check-hard-blocks.sh
```

If all three pass and the app runs on a device, the change is done by the standard in `13-process/definition-of-done.md`.
