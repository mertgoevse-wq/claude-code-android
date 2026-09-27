---
name: runtime-bootstrap
description: How to work on the Linux runtime bootstrap safely - the state machine, checksum verification, process supervision, and the device constraints. Use before touching shared/runtime, the profiles, the PTY, or anything that runs a process on the device.
---

# runtime-bootstrap

This is the hardest part of the project and the one with the least tolerance for
improvising. **Phase 3 is a hard stop**: if the engine does not run on a real
device, the build stops and reports the blocker. There is no mock and no stub.

## The target device

A **Samsung Galaxy A56**: Snapdragon, `arm64`, Android 15/16. Two consequences
that are not bugs and must not be reported as failures:

- **No AVF virtual-machine profile.** Unsupported on Snapdragon upstream. The
  app detects, offers the native profile, and never frames AVF as a loss.
- **The native profile is the default and the tested path.** proot is the
  fallback and the heavy-build path.

## The rules

1. **Own your storage.** Everything under `filesDir/cca/`. Never `/sdcard`,
   never a shared location. Uninstalling removes everything.
2. **Verify before you run.** Checksum against the publisher's list. A mismatch
   aborts with a message and never proceeds. No exceptions, no "just this once".
3. **Idempotent and resumable.** Every step can be re-run safely, and a killed
   app resumes from the step it was on rather than starting over. This is a
   property that is tested, not hoped for.
4. **Track the process tree.** Parent, children, and their PIDs. A cancel that
   leaves a shell child running is a bug, and it is the bug users notice first.
5. **Ring buffer, always visible.** The last N KB of raw stdout and stderr per
   session, in the terminal pane. Nothing is hidden — hard block 5.
6. **One interface.** `ExecutionBackend`. A second implementation is a new
   backend behind it, never a branch in a feature.
7. **Never delete.** Uninstall moves to a quarantine directory. The engine
   rollback keeps the previous version rather than removing it.

## Working on it safely

- Never write to `filesDir/cca/` from a test. Use a fake filesystem; the
  integration suite in `shared/runtime` exists for this.
- Never point the state machine at a real download during a test. The download
  step is behind an interface for exactly this reason.
- A process that runs for more than a second in a test is a test that will be
  flaky. Assert on state transitions, not on wall-clock time.
- The `native-profile.md` package names and versions are **verified at build
  time**, never recalled. If a package name is not confirmed, write
  `TBD — verify at build time` and stop; do not invent it.

## Before and after

```bash
# before
python3 tools/check_no_android_imports_in_shared.py
./gradlew :shared:runtime:check

# on a real device
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s ClaudeCodeAndroid:* | grep -i bootstrap
```

The phase's exit test, every time:

1. `claude --version` runs
2. A headless prompt streams output back into the app
3. Killing the app mid-bootstrap resumes rather than restarts
4. A cancelled run leaves no child process
5. A corrupted download is refused
6. The proot bootstrap completes on its own profile

If any of these fails, that is a **blocker** in the `progress-log.md` format:
command, verbatim error, numbered attempts, analysis marked as analysis, and
what a human must decide. Then stop. Do not build UI over a runtime that does
not work.
