---
description: Run the release checklist box by box
---

# /release

Cut a release the way the checklist says, or do not cut one. Every box is
either ticked by whoever ran it, or it stays empty and the release waits.

## 1. Read the checklist

`docs/12-delivery/release-checklist.md` — sections 1 to 9. It is the contract.
This command walks it; it does not replace it.

## 2. Gates

```bash
./gradlew clean check
./gradlew bundleRelease assembleRelease
fastlane android build --configuration release
scripts/verify_release.sh app/build/outputs/apk/release/app-release.apk
```

`verify_release.sh` checks the certificate fingerprint against the committed
allowlist, the version code, the manifest flags, the analytics scan, the secret
scan, and the package metadata. A failure at any step stops the release.

## 3. A real device, not an emulator

The target device is a **Samsung Galaxy A56**: Snapdragon, `arm64`, Android 15
or 16. Two consequences that are not bugs:

- No AVF virtual-machine profile. It is unsupported on Snapdragon upstream, and
  the app must degrade to the native profile without mentioning AVF as a loss.
- Verify the bootstrap, a headless prompt, screen-off survival, a private-repo
  task producing a green branch and an open PR, interruption onto a `wip/`
  branch, and a German-and-English pass.

Full list in `docs/12-delivery/release-checklist.md` section 2.

## 4. Install the artefact on that device

```bash
adb devices                     # confirm the A56 is visible
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n dev.claudecode.android/.MainActivity
adb logcat -s ClaudeCodeAndroid:* | head -100
```

The deliverable of a build is an **APK that is installed and running on the
Galaxy A56**, not a green Gradle output. If the device is not connected, say
so and name the command the operator must run. Do not report the build as
finished.

## 5. Version and tag

`version.txt` is edited in exactly one commit per release. The tag is
`v<versionName>`, annotated, signed with the release key, and never moved
afterwards. Write the release notes by hand from the merged commit subjects —
generated notes say "fix: update stuff".

## 6. Per channel

- **F-Droid**: clean-room build, no Gradle cache, no Google services. See
  `docs/12-delivery/f-droid.md`.
- **Direct APK**: SHA-256 published next to the file, unofficial notice before
  the download link, Play Protect warning stated before install.
- **Play**: metadata prepared. Publishing is not in v1; do not upload.

## 7. Sign off, then ship

Every box in the checklist, signed and dated, committed and pushed. A release
with one unticked box is a release that waits.
