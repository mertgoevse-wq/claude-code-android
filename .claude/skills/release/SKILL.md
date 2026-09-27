---
name: release
description: Build, verify, sign, package, and install a release, and produce the working APK on the Galaxy A56. Use when cutting a release, when packaging artefacts, or when the deliverable is an installable APK.
---

# release

The deliverable of this project is **an APK that is installed and running on a
real device**, not a green Gradle output. A release that has not been installed
and launched is not a release.

## The device

A **Samsung Galaxy A56**: Snapdragon, `arm64`, Android 15/16.

- No AVF virtual-machine profile. Unsupported on Snapdragon upstream. The app
  degrades to the native profile, and this is not a defect to report.
- Test against the device, not only the emulator. The Phase 3 runtime exit
  criteria and the release checklist both require it.

## The flow

```bash
./gradlew clean check                       # every gate, or nothing ships
./gradlew bundleRelease assembleRelease
fastlane android build --configuration release
scripts/verify_release.sh app/build/outputs/apk/release/app-release.apk
```

`verify_release.sh` fails on the first problem:

1. Certificate fingerprint not in the committed allowlist
2. `versionCode` not strictly greater than the last release
3. Debuggable flag, test instrumentation, or secret backup in the manifest
4. A tracking symbol in the artefact
5. A secret pattern in the resources or assets
6. Wrong package id, `minSdk`, or `targetSdk` for the variant

There is no `--force`.

## Install on the A56

```bash
adb devices                                  # the A56 must be visible
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n dev.claudecode.android/.MainActivity
adb logcat -c && adb logcat -s ClaudeCodeAndroid:* | head -200
```

Then the checklist in `docs/12-delivery/release-checklist.md` section 2, on the
device: onboarding completes from a clean install, the runtime bootstrap reaches
`READY`, `claude --version` runs, a prompt streams back, screen-off for ten
minutes does not kill the run, a private-repo task produces a green branch and
an open PR, a red suite blocks the push, interruption lands on a `wip/` branch,
budget exhaustion reports clearly, and German and English both complete a task.

If the device is not connected, **say so and name the command the operator must
run**. Do not report the build as finished.

## Versioning

`version.txt` is the single source, edited in exactly one commit per release.
`MAJOR.MINOR.PATCH`, `versionCode` strictly increasing and never reused. A
commit never bumps it silently.

## Signing

The release keystore is never in the repository, never in a release asset, never
in a CI log. The fingerprint allowlist is committed. Recovery is documented in
`docs/10-build/signing-and-keystores.md`. A fingerprint mismatch stops
everything.

## Channels

- **F-Droid** — the recommended one. Clean-room build, no Gradle cache, no
  Google services. `tools/check_foss_dependencies.sh` must pass.
- **Direct APK** — SHA-256 published next to the file, unofficial notice before
  the download link, Play Protect warning stated before the user taps.
- **Play** — metadata prepared, publishing is not in v1. Do not upload.

## Release notes

Written by hand from the merged commit subjects. Generated notes say "fix:
update stuff", and this project's changelog is part of its quality. The same
text goes in `docs/15-appendix/changelog.md` and in the GitHub release body.

## Before you call it done

```bash
./gradlew check
python3 tools/check_doc_manifest.py
bash scripts/check-no-secrets.sh
git status                                  # clean
```

Every box in the release checklist, signed and dated, committed and pushed.
