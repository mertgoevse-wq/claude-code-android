# Third-party notices

This project uses third-party software. This file lists it, states the licence, and — where the terms require it — says what this project does and does not do with it.

Two entries need reading before anything else: the **Claude Code engine** (§1), which is not redistributed, and the **Anthropic trademarks** (§5).

The dependency list is generated from the resolved Gradle graph by `tools/check_foss_dependencies.sh` and reviewed on every change. If a dependency appears in the build and not in this file, that is a build failure, not an oversight.

## 1. Claude Code (the engine)

| Field | Value |
|---|---|
| Product | Claude Code, by Anthropic PBC |
| Distribution | Downloaded **at runtime** from Anthropic's distribution endpoint by the app itself |
| Redistribution in this repository | **None.** The binary is not committed, not vendored, and not included in any APK or AAB we publish |
| Verification | The app checks the published checksum before the binary is used, and refuses to run a binary that does not match |
| Terms | Governed by Anthropic's own terms, obtained at download time from Anthropic. Those terms apply between the user and Anthropic |
| Licence | **Proprietary.** Not open source, not redistributable, not covered by this project's Apache 2.0 licence |

This project is an independent, unofficial client. It does not modify, re-license, wrap in a way that alters, or redistribute the engine. It downloads it as a user would, verifies it, and runs it under the user's own API key and their own account relationship with Anthropic.

Users are responsible for complying with Anthropic's terms when they use the engine. Nothing in this repository grants any rights to it.

**Related:** `docs/06-runtime/native-profile.md` (how the engine is obtained and verified) and `docs/01-research/legal-and-trademark.md` (the unofficial status and why it exists).

## 2. Application dependencies

All licences below are permissive and compatible with this project's Apache 2.0. Verify each against the source repository at the pinned version before shipping a release.

### Kotlin and JetBrains

| Component | Licence | Source |
|---|---|---|
| Kotlin standard library, coroutines, serialization | Apache 2.0 | https://github.com/JetBrains/kotlin |
| Ktor client | Apache 2.0 | https://github.com/ktorio/ktor |
| Koin | Apache 2.0 | https://github.com/InsertKoinIO/koin |

### Android and Jetpack

| Component | Licence | Source |
|---|---|---|
| Android Gradle Plugin (build-time only) | Apache 2.0 | https://developer.android.com/build |
| AndroidX, Jetpack Compose, Room, DataStore, WorkManager, Biometric | Apache 2.0 | https://developer.android.com/jetpack |

The Android Gradle Plugin is used to build and is **not** part of the shipped application.

### Platform compatibility layer

| Component | Licence | Source |
|---|---|---|
| `glibc-runner` (Termux package) | GPL-2.0-or-later, per the Termux packages repository — **verify at build time** | Termux packages repository |
| `patchelf-glibc` (Termux package) | GPL-2.0-or-later, per the Termux packages repository — **verify at build time** | Termux packages repository |
| patchelf | GPL-3.0-or-later | https://github.com/NixOS/patchelf |
| proot | GPL-2.0 | https://github.com/termux/proot |
| proot-distro | GPL-2.0 | https://github.com/termux/proot-distro |

**Read this row carefully.** The glibc shim and the ELF patching tool are **GPL-licensed**, and they are the mechanism by which a glibc-linked binary runs on Android. They are downloaded at runtime, like the engine, and are not linked into the application binary. This project's own source remains Apache 2.0.

The exact package names, versions, and licences of the Termux packages must be confirmed at build time and recorded in `docs/10-build/dependency-versions.md`; the entries above are the expected shape, not verified facts. If a licence turns out to be incompatible with the distribution model we use, that is a design change, and it belongs in `docs/14-build-plan/risk-register.md`.

### Test and analysis tooling

| Component | Licence | Source |
|---|---|---|
| kotlin-test | Apache 2.0 | https://github.com/JetBrains/kotlin |
| Turbine | Apache 2.0 | https://github.com/cashapp/turbine |
| MockK | Apache 2.0 | https://github.com/mockk/mockk |
| Paparazzi | Apache 2.0 | https://github.com/cashapp/paparazzi |
| Kover | Apache 2.0 | https://github.com/KotlinDevTools/kover |
| detekt | Apache 2.0 | https://github.com/detekt/detekt |
| ktlint | Apache 2.0 | https://github.com/pinterest/ktlint |
| Robolectric | Apache 2.0 | https://github.com/robolectric/robolectric |

### Icon and illustration sets

The application's icons and illustrations are **this project's own work**, described in `docs/03-design/brand-assets.md`, and are licensed under the same Apache 2.0 as the source.

**No icon font, no illustration pack, and no asset from any other application is bundled or copied.** Where a dependency ships its own assets, they are used under that dependency's licence and are listed above.

### Fonts

The typeface pairing is documented in `docs/03-design/typography.md` with the licence for each family. Every font bundled in the application must be licensed for redistribution — typically SIL Open Font License 1.1. No font is bundled without its licence recorded there.

## 3. Build and CI tooling

| Component | Licence |
|---|---|
| Gradle | Apache 2.0 |
| GitHub Actions | GitHub Terms of Service; no third-party action is used without being listed here |
| fastlane | MIT |

## 4. Standards referenced

Not redistributed; referenced in documentation only.

| Standard | Body |
|---|---|
| WCAG 2.1 | W3C Recommendation |
| ELF specification | Linux Foundation |
| Server-sent events | WHATWG HTML Living Standard |

## 5. Trademarks

**Claude Code**, **Claude**, and **Anthropic** are trademarks of Anthropic PBC. **GitHub** is a trademark of GitHub, Inc. **Android**, **Play**, and the Google Play logo are trademarks of Google LLC. **F-Droid** is a project of F-Droid.

This project is an **independent, unofficial** work. It is not affiliated with, endorsed by, sponsored by, or approved by Anthropic PBC or any of the other trademark holders listed above.

Trademarked names are used **only descriptively** — to identify the software this project works with, and to describe compatibility. No trademarked name is used as this project's own name, in its domain name, in its application id, or in a way that suggests sponsorship or affiliation. **No trademarked logo, wordmark, icon, or other brand asset is copied, adapted, or included in this project.** The application ships its own mark, described in `docs/03-design/brand-assets.md`, which is inspired by the general visual language of the reference application and is visually distinct from it.

The unofficial status is stated in the README hero, in the application's About screen, in the F-Droid and Play listings, and in `docs/01-research/legal-and-trademark.md`. It is not a footnote.

## 6. This project

| Field | Value |
|---|---|
| Licence | Apache License 2.0 — see [`LICENSE`](LICENSE) |
| Copyright | The individual contributors to this repository, as recorded in the git history |
| Assets | Original work of this project, Apache 2.0 |

## 7. Keeping this file correct

| Trigger | Action |
|---|---|
| A dependency is added or removed | Update §2 in the same commit, with the licence verified from the source |
| A licence is found to be different from what is recorded here | Stop the release, correct the file, and assess the distribution implications. This is a `12-delivery/release-checklist.md` item |
| A trademarked name or asset appears in the project | Remove the asset. This is a hard block under `13-process/code-review.md` |
| The engine's terms change | Update §1 in the same commit |

`tools/check_foss_dependencies.sh` compares the resolved dependency graph against this file and fails the build on an unlisted component. The check runs for every build, not only for the F-Droid recipe, because a licence problem is a problem regardless of the channel.

## Depends on

`01-research/legal-and-trademark.md` · `10-build/dependency-versions.md` · `03-design/brand-assets.md` · `06-runtime/native-profile.md` · `12-delivery/f-droid.md` · `LICENSE` · `NOTICE`
