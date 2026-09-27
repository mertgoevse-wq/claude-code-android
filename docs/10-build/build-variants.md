# Build variants

Five variants. Each exists for a specific reason, and none of them is "just to test something".

## The matrix

| Variant | Application ID | Signed | Minified | Debuggable | Purpose |
|---|---|---|---|---|---|
| `debug` | `dev.ccandroid.debug` | Debug key | No | Yes | Daily development |
| `release` | `dev.ccandroid` | Release key | Yes | No | The shipped app |
| `dist` | `dev.ccandroid.dist` | Release key | Yes | No | A self-build artifact, used by `05-features/self-update.md` and by a runner that builds the app |
| `benchmark` | `dev.ccandroid.benchmark` | Debug key | No | Yes, plus profileable | Macrobenchmark and performance gates |
| `nightly` | `dev.ccandroid.nightly` | Release key | Yes, but with the crash symbol file uploaded | No | Canary builds for the maintainer and a small circle |

`dist` is a separate variant rather than a flavour of `release` because it is a genuinely different artifact: it carries a build-time manifest of the runtime version and the provider defaults it was built against, and it is what a phone builds for itself. Two APKs with the same version but different runtime manifests must not overwrite each other, so they need distinct application IDs.

## Why application ID suffixes

`debug`, `benchmark`, and `nightly` each have their own application ID, so all three can sit on a device next to the real app, with separate data directories, separate Keystore material, and separate permissions grants. This is what makes a nightly canary test safe: it cannot see the release app's projects, keys, or logs.

Only `release` and `dist` share the `dev.ccandroid` package. They differ by build type, so they cannot coexist — which is correct, because they carry identical data semantics.

## Build type configuration

### `debug`

```kotlin
debug {
    applicationIdSuffix = ".debug"
    isMinifyEnabled = false
    isDebuggable = true
    buildConfigField("boolean", "VERBOSE_LOGGING", "true")
    buildConfigField("String", "RUNTIME_CHANNEL", "\"nightly-build\"")
    // A test-signed key, never the real one — see signing-and-keystores.md
}
```

Verbose logging is on, the crash reporter is routed to a local sink, and network security config permits cleartext to localhost only (needed by the fake SSH server in instrumented tests).

### `release`

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    isShrinkCode = true
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    buildConfigField("boolean", "VERBOSE_LOGGING", "false")
    buildConfigField("String", "RUNTIME_CHANNEL", "\"stable\"")
}
```

R8 full mode. `proguard-rules.pro` is short and hand-written; see the keep rules below.

### `dist`

```kotlin
dist {
    initWith(getByName("release"))
    matchingFallbacks += "release"
    applicationIdSuffix = ".dist"
    // A build-time manifest of what this APK expects at runtime
    buildConfigField(
        "String",
        "RUNTIME_MANIFEST",
        "\"\"\"{" +
            "\"claudeCodeVersion\": \"${providers.gradleProperty("claudeVersion").get()}\"," +
            "\"minSdkRuntime\": \"native-glibc\"," +
            "\"checksumSha256\": \"${providers.gradleProperty("runtimeChecksum").get()}\"" +
        "}\"\"\""
    )
}
```

A `dist` APK refuses to run against a runtime whose checksum does not match the one baked in. That is the difference between "the app updated" and "the runtime updated", and it is what makes self-update safe — see `05-features/self-update.md`.

### `benchmark`

```kotlin
benchmark {
    initWith(getByName("debug"))
    applicationIdSuffix = ".benchmark"
    isDebuggable = true
    isProfileable = true
    buildConfigField("boolean", "DISABLE_LOGGING", "true")
}
```

Logging is compiled out, not switched off at runtime. A benchmark that measures a `Log.d` is measuring the wrong thing.

### `nightly`

```kotlin
nightly {
    initWith(getByName("release"))
    applicationIdSuffix = ".nightly"
    // Debuggable is NOT enabled: a canary that can attach a debugger is a canary
    // whose crash reports are worthless as a signal.
    buildConfigField("String", "RUNTIME_CHANNEL", "\"nightly\"")
}
```

Nightly is minified, obfuscated, and undebuggable on purpose. It is the closest thing to production, and it is the build that catches obfuscation-only crashes — the R8 and crash-symbol pipeline is validated here, not at release.

## BuildConfig fields

The catalog of every field in the app, with its per-variant values. Anything not in this table is a build error.

| Field | debug | release | dist | benchmark | nightly |
|---|---|---|---|---|---|
| `VERBOSE_LOGGING` | true | false | false | false | false |
| `RUNTIME_CHANNEL` | `nightly-build` | `stable` | `dist` | `benchmark` | `nightly` |
| `RUNTIME_MANIFEST` | empty | empty | baked | empty | empty |
| `CRASH_REPORTING` | `local` | `opt-in` | `opt-in` | `none` | `opt-in` |
| `UPDATE_CHECK_URL` | GitHub | GitHub | GitHub | GitHub | GitHub |
| `DEFAULT_AUTONOMY_LEVEL` | 1 | 1 | 1 | 1 | 1 |
| `HARD_BLOCKS_ENABLED` | true | true | true | true | true |

`HARD_BLOCKS_ENABLED` exists only so a test can assert that it is `true` in **every** variant. There is no build that ships with the safety blocks off. A build configuration that sets it to `false` is rejected by `13-process/definition-of-done.md`; the field exists so that the assertion is a one-line test rather than a code-reading exercise.

## Keep rules

R8 full mode strips aggressively. The keep list is short because most reflection is behind explicit interfaces:

```proguard
# Room generates implementations; KSP handles it, but entities need their fields.
-keep class dev.ccandroid.core.database.entity.** { *; }

# Kotlinx Serialization: the generated serializers are looked up reflectively.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class dev.ccandroid.** {
    *** Companion;
}
-keepclasseswithmembers class dev.ccandroid.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Enum valueOf and entries, used in several places for stable names in the DB.
-keepclassmembers enum dev.ccandroid.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# The custom views the design system exposes to XML (dialog hosts, the terminal).
-keep class dev.ccandroid.core.designsystem.** extends android.view.View { *; }
```

SQLCipher, OkHttp, and Room ship their own consumer rules; we do not duplicate them.

**A keep rule that is no longer needed is deleted in the same pull request that makes it unnecessary.** Stale keep rules are the main cause of an APK that grew by 2 MB over a year, and a `dependency-analysis`-style check reports unreachable keep rules weekly.

## Source sets

```
src/main/            shipped
src/debug/           debug-only: the fake secret store fallback, verbose logcat
src/release/         release-only: R8 config, the crash symbol upload task
src/nightly/         canary-only: the canary channel header, a visible "nightly" badge
src/dist/            dist-only: the runtime manifest check
src/benchmark/       benchmark-only: the macrobenchmark module target
src/test/            unit tests
src/testFixtures/    shared test data (room-testing, fakes)
src/androidTest/     instrumented tests
src/screenshotTest/  Paparazzi/Roborazzi sources
```

Nothing in `src/debug` is referenced from `src/main` — a `debugImplementation` on a class that `main` needs is a compile error, which is the guarantee we want.

## Which variant CI builds

| Trigger | Variants | Gate |
|---|---|---|
| Every push | `debug` (assemble + unit + instrumented + screenshot) | Blocking |
| Every push to `main` | `dist` assemble, `release` assemble | Blocking |
| Nightly | `nightly` + crash-symbol pipeline + install on a device | Advisory, but a failure pages nobody — it opens an issue |
| Pre-release tag | All five | Blocking |

`release` is assembled but not installed on every push, because assembling it requires the release keystore, which CI does not have. See `signing-and-keystores.md` for how release signing actually happens.
