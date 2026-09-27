# Claude Code on Android: what actually works

**Status:** verified 2026-09-27. External facts expire. Re-verify before relying on them; the runtime is the part of this project most likely to break.

## The problem, precisely

Anthropic distributes Claude Code as a **glibc-linked Linux binary** for `linux-arm64`, `linux-x64`, `macOS`, and Windows. It also publishes a JavaScript entry point in older releases.

Android's libc is **Bionic**. A glibc-linked binary requests `/lib64/ld-linux-aarch64.so.1` at startup. That file does not exist on Android, so the kernel refuses to load the binary:

```
ENOENT /lib64/ld-linux-aarch64.so.1
```

There is no official Android build. Upstream tracking issue: `anthropics/claude-code#50270`. The fix upstream would be either a `bun android-arm64` build target or a static musl build. Neither exists as of this writing.

Everything below is a workaround. The app's job is to hide that honestly: it tells the user which profile is active and what it costs.

## Path A — Native Termux-style patching (we ship this as the default)

**How it works:**

1. Install Termux's `glibc-runner` and `patchelf-glibc` packages into the app's own private prefix. This does **not** require the Termux app; we vendor the packages.
2. Download the official `linux-arm64` claude binary from Anthropic's CDN.
3. Verify it against Anthropic's published checksum list. Both the binary and the checksum come from the same host, so this catches corruption and truncation. It is **not** code signing and does not prove the source is uncompromised.
4. Patch the ELF interpreter field with `patchelf` so the binary loads through our glibc-runner instead of the real glibc loader.
5. Install a wrapper script that checks for a new release once per day and updates transparently, keeping a copy of the last known-good version.

**Cost:** roughly 5–10 minutes to install, a few hundred megabytes of storage. A working Node/Bun runtime and the binary.

**What works:** `claude --version`, `claude -p`, streaming output, all tools that shell out to coreutils, git, and basic file operations.

**What does not work:** anything expecting a full glibc filesystem layout, a package manager, or a complete userland. Node tooling that spawns processes aggressively can be flaky. This is why Profile B exists.

**Known side effect:** some community installers force Claude Code's DNS lookups to public resolvers (`8.8.8.8` / `8.8.4.4`) to dodge a connectivity hang on some Android networks. That overrides a VPN, split-tunnel, or Pi-hole. **We make this opt-in with a visible warning in the security screen** rather than applying it silently. See `11-operations/security-threat-model.md`.

**Provenance note:** the patching approach is a widely used community pattern, documented in projects such as `ferrumclaudepilgrim/claude-code-android`. We implement it ourselves against Anthropic's published checksums. We do not vendor another project's installer.

## Path B — proot Ubuntu (we ship this as an option)

**How it works:** `proot-distro` installs a full Ubuntu image; inside it, Anthropic's official installer puts Claude Code where it belongs. `process.platform` reports `linux`, glibc is real, and all normal Linux conventions apply.

**Cost:** roughly 2 GB of storage, several minutes to install.

**When it is the right choice:** heavy work. Gradle builds, the Android SDK, Node package installs, native compilation, anything expecting a package manager. On a phone this is slow, but it works, and offloading to a remote runner is better still.

**Why it is not the default:** two gigabytes and a multi-minute install to say hello. A user who just wants to try the app should not pay that up front. The UI offers the upgrade when a project actually needs a toolchain, not before.

**proot's limits:** `ptrace`-based, so no real `fork()` semantics for some programs, no hardware acceleration, and syscall differences that occasionally surface. It is good enough for builds, not for VMs.

## Path C — Android's built-in VM (experimental, detected but not depended on)

**How it works:** Android 16 exposes a Linux development environment built on the Android Virtualization Framework, with a real kernel and a Debian image.

**Requirements:** Pixel 6 or later, Android 16 or later, developer options enabled. Qualcomm Snapdragon devices are **not** supported. Some Exynos devices (Galaxy S26 / S26+) are reported to work but are not lab-verified.

**Why it is in the codebase at all:** it is the only path that could be called genuinely native. It is also the narrowest. We detect support, we present it as experimental, and nothing in the app depends on it. If it never ships, no user notices.

## What the app does

| Profile | Status | Storage | Install | Use for |
|---|---|---|---|---|
| **A — Native** | Default | ~300–500 MB | 5–10 min | Chat, edits, git, most tasks |
| **B — proot Ubuntu** | Optional, offered on demand | ~2 GB | 5–15 min | Builds, toolchains, anything needing a package manager |
| **C — AVF** | Experimental, hardware-gated | OS-managed | OS-managed | Future |

All three are described by one `RuntimeProfile` data class. Feature code never branches on which is active — see `06-runtime/execution-backends.md`. Switching profiles is a settings change that re-runs the bootstrap, keeping project data untouched.

## Failure modes we must handle

| Failure | What the user sees | What we do |
|---|---|---|
| Binary download truncated | "Install unvollständig" with a retry | Checksum mismatch is a hard abort. We never install an unverified binary. |
| Checksum list unreachable | "Prüfsummen nicht erreichbar" | Abort. A binary we cannot verify is a binary we will not run. |
| glibc-runner missing or wrong ABI | "Laufzeitumgebung beschädigt" | Health check detects it, offers a repair that re-downloads the runtime without touching projects. |
| New Claude Code release breaks the patch | Launch fails after an auto-update | The updater launches the new version once in a sandboxed probe. If it does not reach a version banner, we roll back to the last known-good version and tell the user. |
| 32-bit device (`armv7l`, `armv8l`) | Blocked at first run, before anything is downloaded | Unsupported. We detect `uname -m` semantics early and stop with a clear message rather than failing mid-download. |
| Storage exhausted mid-install | "Nicht genug Speicher: X MB fehlen" | Report the required amount. Never delete anything to make room — that is a hard block. |
| Network drops during install | Progress freezes at a step | The step is idempotent and resumable. Re-entering the app resumes at the failed step, not step one. |

## What we do not do

- We do not patch a binary in a way that disables a security check.
- We do not ship another community project's installer. We implement against Anthropic's published artifacts.
- We do not hard-code a pinned Claude Code version and call it done; the update path is part of the product, and its failure handling is a feature, not an afterthought.
- We do not depend on any single profile. If native patching stops working upstream, the app degrades to proot or to a remote runner rather than becoming useless.

## How to re-verify this document

1. Check whether `anthropics/claude-code#50270` has been closed or a release note mentions an Android build. If yes, path A collapses into "just download the official build" and this document shrinks.
2. Check the current Claude Code release for the `linux-arm64` artifact and its published checksum.
3. Re-read the Termux glibc-runner documentation for ABI changes.
4. Re-check the AVF device list in the current Android release notes.

Record the date and the outcome in `15-appendix/references.md`.
