# Blocker B2 — the build machine is the target device

**Status:** open. Needs one action from the operator, and it is 20 seconds of tapping.

**Date:** 2026-09-27
**Supersedes:** the B1 entry in `progress-log.md` ("no Galaxy A56 attached"), which was
true of adb and wrong about the situation.

---

## 1. What B1 got wrong, and why

The previous session reported `adb devices` listing zero targets and concluded no phone
was available. That command's output was correct. The inference was not.

`uname -a` reports:

```
Linux localhost 6.17.0-PRoot-Distro #1 SMP PREEMPT_DYNAMIC aarch64 GNU/Linux
```

`PRoot-Distro` plus `aarch64` means **this build machine is itself an Android phone,
and Claude Code is running on it inside Termux under PRoot.** The device was never
missing. adb was missing a *transport*, because there is no USB host to attach to.

This is worth stating plainly because it inverts the project's own instruction. Every
`adb`-centric command in `BOOTSTRAP.md` §2 and `CLAUDE.md` assumes a build host and a
separate target. Here they are the same machine, which makes the whole class of
"external device" commands unavailable and replaces it with a different class: the
phone is reachable as its own TCP target.

## 2. What works, verified

| Capability | Result | Evidence |
|---|---|---|
| Gradle build, release APK | **works** | `BUILD SUCCESSFUL in 1m 13s`, 806 927 bytes |
| Full toolchain on aarch64 | **works** | SDK `aapt2` is x86-64; a qemu wrapper under `android-sdk/aapt2-qemu/` bridges it |
| Write to shared storage | **works** | `/storage/emulated/0/Download/ccandroid.apk` written, 806 927 bytes |
| Launch an app | **works, with a fix** | see §3 |
| Install an app without a human tap | **blocked** | see §4 |

The x86-64 `aapt2` on an aarch64 host is a real hazard and it is worth not rediscovering
later: `build-tools/35.0.0/aapt2` is `ELF 64-bit LSB pie executable, x86-64`. Termux
also ships its own aarch64 `aapt2`, which links against `/system/bin/linker64`. Either
route works; the qemu wrapper is the one the build is currently pinned to.

## 3. The one real fix discovered this session

Termux's `am` wrapper passes `user -2` (`USER_ALL`) to `startActivityAsUser`. Termux
does not hold `INTERACT_ACROSS_USERS_FULL`, so **every** launch fails:

```
java.lang.SecurityException: Permission Denial: startActivityAsUser asks to run as
user -2 but is calling from uid u0a275; this requires
android.permission.INTERACT_ACROSS_USERS_FULL or android.permission.INTERACT_ACROSS_USERS
```

Passing the user explicitly works. This is a defect in the previous session's tooling
expectation, not a device limitation:

```bash
/data/data/com.termux/files/usr/bin/am start --user 0 -a android.intent.action.VIEW -d "https://example.com"
# Starting: Intent { act=android.intent.action.VIEW dat=https://example.com/... }
```

Any launch command in this project must carry `--user 0`.

## 4. Why the install is blocked, exactly

Installation needs `INSTALL_PACKAGES`, held only by `shell` and `root`. We are neither:
`id` gives `uid=1000(mert) groups=1000(mert),1077,3003(aid_inet),9997(aid_everybody)…`.

`su` exists but is password-gated (`su: Authentication failure`) — assuming a Magisk
install, unverified.

Everything that would install silently was tried and is ruled out by evidence, not
assumption:

| Attempt | Result | Why |
|---|---|---|
| `/system/bin/pm install` | `Operation not permitted` (rc 126) | no `INSTALL_PACKAGES` |
| `/system/bin/cmd` | rc 126 | same |
| `/system/bin/app_process` | rc 134 (SIGABRT) | SELinux denies Termux's domain |
| `/system/bin/getprop` | `Operation not permitted` | even read-only system calls are sealed |
| `adb install` | no device | no transport exists |
| `adb connect` to own wlan IP `10.198.20.88` | all ports closed | wireless debugging is off |
| port scan 37000–37999 and 5555/5556/8080 | nothing open | same |
| `adb mdns services` | `unknown host service` | mDNS unavailable in this build |

The one route that does open is launching the system package installer, and it needs a
human tap on "Install":

```bash
cp androidApp/build/outputs/apk/release/androidApp-release.apk /storage/emulated/0/Download/ccandroid.apk
/data/data/com.termux/files/usr/bin/am start --user 0 \
  -a android.intent.action.VIEW \
  -t application/vnd.android.package-archive \
  -d "file:///storage/emulated/0/Download/ccandroid.apk"
```

The intent fires. A permission dialog appears. **Nobody can press it but the operator.**

`tools/wireless_debug_watch.sh` implements the one path that removes the human from the
loop entirely: enable wireless debugging, and adb running inside PRoot reaches the
phone's own dynamic port, giving fully automated `adb install`, `adb shell`, and
`adb logcat` thereafter.

## 5. What the operator must do

Pick one. Option A is twenty seconds and unblocks everything permanently.

### Option A — enable wireless debugging (recommended, unlocks full adb)

1. Settings → About phone → tap **Build number** seven times
2. Settings → Developer options → **Wireless debugging** → On
3. Back in Termux, run the watcher and follow what it prints:

```bash
cd ~/claude-code-android && ./tools/wireless_debug_watch.sh
```

It prints the pairing port the moment it opens. Then on the phone, in
**Wireless debugging → Pair device with pairing code**, enter the code. After pairing,
run:

```bash
adb connect 10.198.20.88:<connect-port>
adb devices
```

Once `adb devices` lists this phone, every remaining install is automated and no
further operator action is needed for the rest of the build.

### Option B — one manual tap per install

```bash
cd ~/claude-code-android
cp androidApp/build/outputs/apk/release/androidApp-release.apk /storage/emulated/0/Download/ccandroid.apk
/data/data/com.termux/files/usr/bin/am start --user 0 \
  -a android.intent.action.VIEW -t application/vnd.android.package-archive \
  -d "file:///storage/emulated/0/Download/ccandroid.apk"
```

Then tap Install on the phone.

## 6. Why this does not stop the build

The install is blocked. The **build** is not. Per `BOOTSTRAP.md` §2, the deliverable is
an installed, running APK — and that clause cannot be satisfied until §5 happens. But
Phase 1 is design-system work whose verification is `./gradlew check`, and that gate runs
on this machine today. The build therefore continues through P1-7 … P1-14 and into
Phase 2, and the install is re-attempted the moment a transport exists.

What is **not** permitted is reporting the deliverable as finished. The project exists to
prevent a plausible, broken app; a Phase 1 report that says "a working app" would be
exactly that failure. The honest current state is: *a green build of a design system,
with the app one operator tap away from installing.*
