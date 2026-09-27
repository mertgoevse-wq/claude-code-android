# F-Droid

F-Droid is the honest-distribution channel: a reproducible build from source, no proprietary binaries, no tracking, no signature gate beyond the app's own.

It is also the strictest channel we support, because F-Droid's build environment has no Google services and no preinstalled SDK wizardry. If the app builds for F-Droid, it builds anywhere.

## 1. What the recipe must satisfy

| Requirement | How we meet it |
|---|---|
| Builds from source only | `./gradlew assembleRelease` with a pinned Gradle wrapper and a checked-in version catalog. No `curl \| sh` in the build |
| No proprietary dependencies | Enforced by a script, see §2 |
| Reproducible | The build runs in a clean container with a pinned JDK and Android SDK; `gradle/verification-metadata.xml` pins every dependency checksum |
| `LICENSE` present | Apache 2.0, at the repository root |
| Metadata complete | `fastlane/metadata/android/en-US` is the single source for the F-Droid listing too |
| Version name matches | F-Droid derives the version from the tag; `version.txt` must equal the tag, and a mismatch fails the build |
| No anti-feature violations | No tracking, no ads, no non-free services. The app has none by construction (`11-operations/telemetry.md`) |

## 2. The FOSS check

`tools/check_foss_dependencies.sh` walks the resolved dependency graph and fails on any coordinate that is not on `docs/15-appendix/references.md` → FOSS allowlist. It runs in CI for every build, not only for the F-Droid job, because a proprietary dependency is a problem for the project regardless of the channel.

Known exceptions are listed in `10-build/dependency-versions.md` with the reason, the licence, and the exact coordinate. There is currently one category to reason about:

| Dependency | Status | Reasoning |
|---|---|---|
| `com.android.tools.build:gradle` | Build-time only | The Android Gradle Plugin is Apache 2.0 and is not shipped in the APK. F-Droid accepts build tooling that is not part of the produced binary |
| AndroidX, Compose, Room, Ktor, Koin, kotlinx.* | FOSS | Apache 2.0 / Apache 2.0, all buildable from source |
| Claude Code `linux-arm64` binary | **Downloaded at runtime, not bundled** | This is the reason the F-Droid recipe has a note: the app installs the engine itself, from Anthropic's CDN, with a checksum check. It is not redistributed in the repository or the APK |
| Google Play Services | Not a dependency | The app has no play-services dependency. This was a deliberate choice: it removes the proprietary dependency and makes the F-Droid build possible |

The last row is the design decision that makes this channel possible. Nothing in the app needs play services, so nothing depends on them.

## 3. The recipe

`fastlane/metadata/android/en-US` supplies the listing. The recipe lives at `tools/fdroid/` and declares:

```yaml
# excerpt — the full recipe is in the repository
Build:
  subdir: app
  after_prepare:
    - sed -i "s/^\(minSdkVersion=\).*/\1$(grep -o 'minSdk = [0-9]*' gradle.properties | grep -o '[0-9]*')/" build.gradle.kts
  gradlePluginPlugins:
    - org.gradle.tooling.dependency-substitution
```

| Field | Value |
|---|---|
| Build type | Gradle |
| Subdirectory | `app` |
| Min SDK | Read from `gradle.properties`, never hardcoded in the recipe |
| Target SDK | Same |
| NDK | Not required. The app downloads the engine rather than compiling it |
| Architectures | `arm64-v8a` only. The engine is `linux-arm64`; shipping x86 slices would add size for no benefit |
| Auto-update | Enabled, `updateCheckMode: disabled` — the app's own update check is off by default in `dist`, and F-Droid's channel is the recommended one in the README |

## 4. What a user gets that a Play user does not

- A build they can audit.
- No `PLAY_REVIEW` anything, no proprietary code in the process.
- Updates on F-Droid's schedule rather than Play's.
- No telemetry at all, which is true of every channel here anyway.

## 5. Known limitations, stated

| Limitation | Handling |
|---|---|
| The engine is downloaded at runtime, so the F-Droid build alone is not fully offline-capable | Documented in the README's honest-limitations section. The alternative — bundling a proprietary binary — is not something F-Droid would accept |
| Reproducibility is best-effort | Android build tooling is not bit-reproducible. We pin everything we can and publish the SHA-256 of the artefact we built, so a difference is detectable even when it is not explainable |
| Play Protect warns on sideloading | The README says so before the user installs, not after |
| No tablet-specific listing images | Play gets tablet screenshots; F-Droid gets the phone set. A known gap, listed in `release-checklist.md` as a follow-up, not hidden |

## 6. Verification before submitting

1. `./gradlew assembleRelease` in a clean container with no `~/.gradle` cache.
2. `scripts/verify_release.sh` on the artefact.
3. A clean-room install on a device with no Google services: the app starts, the runtime bootstrap runs, `claude --version` succeeds.
4. `fastlane downloadMetadata` produces a listing with no missing fields.

A F-Droid submission that fails at step 3 costs a maintainer a week of queue time. We do it locally first.

## Depends on

`10-build/dependency-versions.md` · `12-delivery/apk-distribution.md` · `12-delivery/play-store.md` · `11-operations/privacy.md` · `15-appendix/references.md`
