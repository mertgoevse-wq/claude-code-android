# Convention plugins

One plugin per concern, applied to every module that shares that concern. The point is that a module cannot forget a rule, because the rule is in the plugin it must apply to exist.

`build-logic/` is an included build, so its plugins are available by id to every module without a classpath entry.

## Why this exists

Without convention plugins, the settings that matter — `minSdk`, the language level, `explicitApi`, test options, Compose configuration, packaging excludes — are repeated in twenty `build.gradle.kts` files and drift within a week. With them, there is one place per rule and a module that misbehaves cannot compile.

## The plugins

### `cc.android.library`

Applied by every library module. Provides: Android library plugin, Kotlin Android, the shared `compileSdk`/`minSdk`, Compose, KSP, and the default JVM target.

```kotlin
plugins {
    id("cc.android.library")
}

android {
    namespace = "dev.ccandroid.<module>"
    buildFeatures { compose = true; buildConfig = false }
    defaultConfig { consumerProguardFiles("consumer-rules.pro") }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}
```

Fixed by the plugin, not by the module:

| Setting | Value |
|---|---|
| `minSdk` | 26 |
| `compileSdk` | 35 |
| `targetSdk` | 35 (application modules only) |
| Java/Kotlin target | 17 bytecode, JDK 21 toolchain |
| `testOptions.unitTests.isReturnDefaultValues` | true — a real default per layer, not a crash |
| `packaging.resources.excludes` | `/META-INF/{AL2.0,LGPL2.1}`, `META-INF/*.version`, `kotlin/**`, `DebugProbesKt.bin` |
| `lint.abortOnError` | true |
| `lint.warningsAsErrors` | true in CI |
| `buildConfig` | false, opt-in per module |

### `cc.android.application`

Everything from the library plugin, plus: application plugin, `versionCode`/`versionName` from the root project, ProGuard/R8 defaults, signing config wired from `keystore.properties` **only** when it exists, and a `dist` build type that is the one shipped to a runner.

```kotlin
plugins { id("cc.android.application") }

android {
    defaultConfig {
        applicationId = "dev.ccandroid"
        targetSdk = 35
        versionCode = rootProject.extra["versionCode"] as Int
        versionName = rootProject.extra["versionName"] as String
        testInstrumentationRunner = "dev.ccandroid.HiltTestRunner"
    }
    signingConfigs {
        // Created only if keystore.properties is present. Debug and CI never have one,
        // so a debug build is never accidentally signed with the release key.
        if (rootProject.file("keystore.properties").exists()) {
            create("release") { /* loads from keystore.properties */ }
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; isMinifyEnabled = false }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
}
```

`applicationIdSuffix = ".debug"` means the debug app and the release app coexist on one device. The debug build is a separate app with separate data, which is why a manual-key test run cannot contaminate a real install.

### `cc.android.hilt`

Applies the Hilt plugin and KSP, and adds the `hiltAggregateDepsAndPackages` task to `check`.

### `cc.android.room`

Applies the Room plugin and KSP, sets `room.schemaLocation` to a committed directory, and sets `room.generateKotlin` to true.

**The schema is exported and committed.** `schemas/…/1.json`, `2.json`, … exist in the repository, and a test asserts that a schema change without a migration fails the build. See `02-architecture/data-migrations.md`.

### `cc.jvm.library`

The pure-Kotlin modules: `:core:model`, `:core:permissions`, `:core:verification`, and the pure part of `:core:agent`.

```kotlin
plugins { id("cc.jvm.library") }

kotlin {
    explicitApi()   // every public declaration needs an explicit visibility and return type
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors.set(true)
        freeCompilerArgs.addAll("-Xjvm-default=all")
    }
}
```

`explicitApi()` is what forces the domain layer to be a deliberate, documented surface. It is a little friction and it is worth it: these modules are the ones with no framework holding them honest.

### `cc.compose`

The design-system modules. Enables Compose, applies the compiler plugin, and — importantly — makes the design tokens a hard dependency:

```kotlin
plugins { id("cc.compose") }

dependencies {
    implementation(project(":core:designsystem"))
}
```

Nothing in a feature module may reference `androidx.compose.material3.*` colours or typography directly. Detekt enforces it (`ForbiddenImports` rule in `config/detekt.yml`); a direct reference to a Material colour is a build failure. The tokens are the only source of colour, spacing, type, and motion in this app. See `03-design/design-tokens.md`.

### `cc.kotlin.test`

Applied by the unit test source set. Sets JUnit 4 as the runner, enables the coroutines test dispatcher, and wires the `FakeSecretStore` and the network-denying HTTP factory so a unit test physically cannot reach the internet.

### `cc.android.instrumented`

Applies `cc.android.library` plus the instrumentation runner, `testInstrumentationRunnerArguments` with the emulator's `clearPackageData`, and the `NetworkSecurityConfig` that denies cleartext in test builds.

### `cc.android.screenshot`

Configures Paparazzi and Roborazzi with the four rendering dimensions from `09-testing/screenshot-tests.md` — theme, font scale, size class — and makes `recordPaparazziDebug` a first-class task.

## `build-logic/` layout

```
build-logic/
  settings.gradle.kts
  convention/
    build.gradle.kts          # no dependencies except the Kotlin DSL and compileOnly AGP
    src/main/kotlin/
      AndroidLibraryConventionPlugin.kt
      AndroidApplicationConventionPlugin.kt
      AndroidHiltConventionPlugin.kt
      AndroidRoomConventionPlugin.kt
      AndroidInstrumentedConventionPlugin.kt
      AndroidScreenshotConventionPlugin.kt
      JvmLibraryConventionPlugin.kt
      ComposeConventionPlugin.kt
      KotlinTestConventionPlugin.kt
  gradle/libs.versions.toml   # the build's own version catalog, separate from the app's
```

`build-logic` compiles against AGP, Kotlin, and Hilt as `compileOnly`. It has no runtime dependencies and it never resolves the app's own dependencies.

## The rules a convention plugin can express, and the ones it cannot

| Rule | Where it lives |
|---|---|
| SDK levels, language level, test options | Convention plugin |
| Compose and token enforcement | Convention plugin + detekt |
| `explicitApi()` on pure modules | Convention plugin |
| Schema export | Convention plugin |
| Layer boundaries (`:feature:*` must not import `:core:database`) | **Not expressible as configuration.** Enforced by a custom detekt rule and by `dependency-analysis`; see `02-architecture/layer-contracts.md`. |
| No hard blocks outside the executor | **Not expressible.** Enforced by a custom static check in `13-process/definition-of-done.md`. |

Anything a plugin cannot express is a test or a static check. A rule that exists only in a code review comment is not a rule.

## Adding a plugin

1. It must fix a rule that has been applied by hand in three or more modules, or fix a rule that a module can forget.
2. It must be idempotent — applying it twice changes nothing.
3. It must not read the environment or the filesystem outside the project at configuration time (configuration cache, see `gradle-setup.md`).
4. It needs a test: a small fixture module in `build-logic/src/test` that applies the plugin and asserts the resulting configuration.
