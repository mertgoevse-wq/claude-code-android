# Troubleshooting

Organised by **symptom**, because that is how people arrive. Each entry says what you will see, what it actually means, and what to do — in that order.

If a problem is not here, the app's own diagnostics bundle is the right next step: Settings → About → Export diagnostics. It includes the logs, the environment, and the configuration with secrets stripped. See `11-operations/diagnostics-export.md`.

## Contents

1. [The runtime will not set up](#1-the-runtime-will-not-set-up)
2. [The engine installed but nothing runs](#2-the-engine-installed-but-nothing-runs)
3. [No response from the model](#3-no-response-from-the-model)
4. [GitHub problems](#4-github-problems)
5. [The run stopped and I do not know why](#5-the-run-stopped-and-i-do-not-know-why)
6. [It is very slow](#6-it-is-very-slow)
7. [The terminal is broken](#7-the-terminal-is-broken)
8. [The app itself is misbehaving](#8-the-app-itself-is-misbehaving)
9. [Build problems (for developers)](#9-build-problems-for-developers)
10. [When nothing above helps](#10-when-nothing-above-helps)

---

## 1. The runtime will not set up

### "The download keeps failing"

The bootstrap downloads several artefacts over your network. A failure here is almost always the network, not the app.

- Switch to a different network. Mobile data often works where a captive-portal Wi-Fi does not.
- Free storage first: the native profile needs roughly 500 MB, the proot profile roughly 2 GB. `06-runtime/proot-profile.md`.
- The bootstrap is resumable. Kill the app and reopen it; it continues from the failed step, it does not start over.

### "Checksum mismatch"

The downloaded file does not match the published checksum. **The app refuses to run it, and that is correct behaviour.**

- Do not retry in a loop. A repeated mismatch means either a corrupted transfer or something intercepting your traffic.
- Delete the runtime from Settings → Storage → Runtime and download again once.
- If it happens on every attempt, export diagnostics and report it. Include the expected and actual hashes from the error message.
- See R1 in `14-build-plan/risk-register.md`.

### "The glibc patch failed"

The ELF patch did not produce a runnable binary.

- Switch to the proot profile: Settings → Runtime → Profile → proot Ubuntu. It is slower and larger, and it usually works.
- Do not attempt to patch the binary by hand. The app owns that directory, and a hand-patched binary is indistinguishable from a tampered one to the next verification.

### "Not enough storage" / "Installation failed"

- Free space, then reopen the app. The bootstrap resumes.
- On a device with expandable storage, verify the volume is not read-only.
- Uninstalling removes everything including the runtime; there is no partial uninstall.

### "The app was killed during setup"

Expected on some devices, and handled: the app resumes from where it stopped. If it restarts from step 1 every time, that is a bug — the resumability guarantee in `06-runtime/bootstrap-state-machine.md` is explicit. Export diagnostics and report it.

---

## 2. The engine installed but nothing runs

### "claude --version shows nothing"

Open Settings → About → Runtime, and run the check. The three things it verifies are: the binary is present, the checksum matches, and the interpreter resolves.

- If the binary is missing, reinstall the runtime.
- If the interpreter does not resolve, the `glibc-runner` package did not install correctly. Reinstall the runtime; if it recurs, the device's Android version is likely unsupported — see `06-runtime/environment-diagnostics.md` for the supported range.

### "Unsupported device" / "CPU is not supported"

The engine is a `linux-arm64` binary. A 32-bit or x86-only device is not supported, and the app says so at first run rather than failing later.

- The experimental AVF profile does not help here: it is unavailable on Qualcomm Snapdragon upstream, which covers a large share of Android devices. The app detects and says so.

### "It worked yesterday and does not work today"

Almost always an engine update that changed something. Check Settings → About → Runtime for the installed version, and the release notes for the previous one.

- The app keeps the last known-good version and rolls back automatically if a new one fails twice. If you are on a rolled-back version, the details are in the transparency log for the failed run.
- See R1. This is the risk we predicted, and the rollback is the mitigation.

---

## 3. No response from the model

### "Test connection fails"

The button in the provider editor reports a specific reason, not a generic error. Read it — it distinguishes unreachable host, bad key, wrong model, and wrong dialect.

| Reason | Do |
|---|---|
| Host unreachable | Check the base URL, including the scheme and any trailing path. Check the network. |
| Key rejected | Confirm the key is for that provider. Keys from one provider do not work with another. |
| Model not found | The model list is fetched from the provider. Pick from what it returns, or add the model id manually if you know it is correct. |
| Dialect mismatch | An OpenAI-compatible server against the `ANTHROPIC` kind, or the reverse. Change the provider kind. `07-integrations/providers.md` |

### "The model responds but the run does nothing"

- Check the autonomy level. At `ASK_EVERYTHING`, every tool call waits for a tap, so a run can look idle indefinitely.
- Look for a permission request. It appears in the chat, and as a notification. Approve, deny, or allow always.
- Check the plan. If a plan exists with unchecked steps, the run is waiting for you to confirm it.

### "It streams and then stops with no error"

- Look at the transparency log for the last event. A truncated stream is usually a network drop, and the log shows whether the run ended cleanly or the connection died.
- If the log ends mid-tool-call, re-run the task. Partial work is preserved on a `wip/` branch, so nothing is lost.

### "It costs too much"

- The cost meter is per run, per project, per day, and per month, and it updates during the run.
- Set an advisory threshold in Settings → Cost. It warns; it does not stop the run. The app shows cost and does not cap it — a hard cap is not a decision this app makes for you (`D23`).
- A cheaper model for routine work is the effective lever. `05-features/model-selection.md`.

---

## 4. GitHub problems

### "Sign-in failed"

- OAuth needs a registered app. If you are building from source, use the **personal access token** path instead: Settings → Providers → GitHub → Add token. `07-integrations/github-auth.md`.
- A PAT needs the right scopes. The app's scope checklist in the token screen names them; a fine-grained token needs repository access to the repos you intend to use.
- A token that was fine yesterday may have been revoked, or may have expired. Re-validate it in the provider screen.

### "The push was blocked"

This is the hard rule working, not a failure. Pushes are gated on `verifyProject()` passing.

- The blocking reason is shown, with the actual failing command and its output.
- The commit is not lost; it is on the local branch. Fix the failure, re-run, and push.
- If the verification command itself is wrong, edit it in the project settings. Detection is heuristic and can be wrong. `05-features/verification.md`.

### "The push was refused because of the branch"

The current branch resolves to the repository's default branch. Pushes there are refused with no override.

- Create a task branch and re-run. The app does this automatically per task; this happens when the project was left on the default branch.
- See hard block 4.

### "The PR did not open"

- A PR is opened only when the push succeeded and verification was green. Check both.
- GitHub notifications are polled; after opening a PR, the notification can take up to the polling interval to arrive. Set it lower in Settings → Notifications if that matters.
- If a PR was opened but is not visible, check whether the repository is private and your token has access to it.

---

## 5. The run stopped and I do not know why

Work through these in order. The transparency log answers most of them directly.

| Symptom | Meaning | Action |
|---|---|---|
| "Retry budget exhausted" | The verifier kept failing after N attempts. N is your configured budget. | Read the report. It names the failing command and the last error. Fix, or raise the budget |
| "Stuck" / "no progress" | Anti-loop detection aborted a repeating step. | The log shows the repeated command. It is usually a genuinely stuck build |
| "Verdict: failed" | The judge decided the task did not succeed, even if the agent said it was done. | The judge is not the agent's self-report. The verification output is the evidence |
| Nothing in the UI, notification says done | The run finished and you missed it | Chat list → the run → its summary and cost |
| The run vanished | The process service was killed | Diagnostics export; look for an OOM or a system kill entry |

**The transparency log is append-only.** Nothing is edited or removed, by design (`D25`). If a run's record looks incomplete, that is a bug worth reporting rather than a settings issue.

---

## 6. It is very slow

| Cause | What you can do |
|---|---|
| Heavy build on the phone | Use the proot profile, or offload to a remote runner. The native profile lacks a full userland, so Gradle and Node are slow |
| A cold proot bootstrap | It is a multi-gigabyte download. It is resumable and it happens once |
| The device is thermally throttling | Long runs on a phone are hot. Wait, or offload |
| The network | Streaming pauses when the connection stalls; the log shows the pauses |
| The retry loop | A run that keeps failing makes many API calls and takes many minutes. Watch the retry counter |

Long runs also cost battery. The foreground service notification shows what is running and for how long, and a wake lock is held only while a run is active. See R7.

---

## 7. The terminal is broken

### "No colours" / "The output looks wrong"

- The renderer supports 16, 256, and truecolor. If output is monochrome, `NO_COLOR` or `TERM=dumb` is set in the environment.
- The terminal pane is a peer of the chat, not a separate app. Switch between them with the split view or the floating sheet.

### "Ctrl+C does nothing"

- The keyboard row has a dedicated `Ctrl+C` that is always reachable. If you are typing a command rather than sending a signal, check the key row is expanded.
- If a process ignores SIGINT, the run's cancel path kills the whole process tree, not just the foreground process. `06-runtime/process-supervision.md`.

### "The session was lost"

- Terminal sessions persist. If one is missing after a reboot, the process was killed and the session was not resumable.
- The scrollback is a ring buffer, so a very long session shows the most recent N KB, not everything. Export the run for the full record.

---

## 8. The app itself is misbehaving

### "It will not unlock"

- After five failed attempts the lock falls back to the device credential.
- If the credential is also unavailable, uninstall and reinstall. **Projects are stored on the device; uninstalling removes them.** If they are on GitHub, clone again.

### "The screen is blank in the recents switcher"

That is `FLAG_SECURE` working. Turn it off in Settings → Security if you would rather see previews.

### "The app is using too much storage"

Settings → Storage shows what is using what: runtime, projects, logs, exports.

- Logs and exports are the usual cause, and both are safe to clear. The transparency log is cleared **only** if you choose to; nothing prunes it silently.
- Uninstalling removes everything, including Keystore entries. `11-operations/backup-and-restore.md`.

### "Backups are not restoring my keys"

Correct and intentional. Secrets are excluded from Android auto-backup. Keys live in the Keystore on the device that created them, which is exactly the property that makes them safe.

- Project content can be backed up as an explicit, user-initiated action if you want it.

---

## 9. Build problems (for developers)

| Symptom | Cause | Fix |
|---|---|---|
| `Unsupported class file major version` | Wrong JDK | Use the version pinned in `gradle/libs.versions.toml` |
| `Execution failed for task ':app:mergeReleaseResources'` | A missing SDK component | Sync with the pinned `compileSdk` |
| Build is suddenly much slower | Configuration cache disabled, usually by a build script reading the environment | Fix the script. `10-build/gradle-setup.md` |
| Tests pass locally, fail in CI | Locale, case-sensitivity, or a stale cache | `./tools/ci.sh` locally reproduces it |
| `check_doc_manifest.py` fails | A doc is missing or still a stub | Write it, or amend the manifest in the spec |
| `check_no_android_imports_in_shared.py` fails | A platform import leaked into `shared/` | Move the code to `androidApp`. There is no suppression |
| Coverage below the gate | A new uncovered branch | Add the test. The threshold is not lowered |
| Paparazzi diff | An intended or unintended visual change | Look at both images. If intended, accept the baseline and say why in the commit |

---

## 10. When nothing above helps

1. **Export diagnostics.** Settings → About → Export diagnostics. Logs, environment, and redacted configuration in one file.
2. **Search the transparency log** for the run. It has the exact commands, their exit codes, and the verifier's output.
3. **Report it** with: the app version, the device model, the Android version, the runtime profile, the diagnostics bundle, and the run ID. That is everything needed to reproduce it.
4. **Check `14-build-plan/risk-register.md`.** If your problem is listed, someone else has hit it and its mitigation may already tell you what to do.

A report that includes a diagnostics bundle is roughly ten times more likely to be fixed in the next release than one that describes the problem in words.
