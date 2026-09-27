# The proot Ubuntu profile

The optional profile, for work that needs a real Linux userland. Two gigabytes, several minutes, and it makes Gradle builds possible.

## What it is

A full Ubuntu installation running under `proot`, with Claude Code installed inside it by Anthropic's own installer.

| Property | Value |
|---|---|
| Mechanism | `proot`, a `ptrace`-based syscall translator |
| Root | Not required, and not used |
| Distribution | Ubuntu, from `proot-distro` |
| Engine | Installed by `https://claude.ai/install.sh` inside the guest |
| Storage | Roughly 2 GB |
| Install time | 5–15 minutes, depending on the network |
| `process.platform` | `linux`, so all Linux conventions apply |

## When to use it

| Use the native profile | Use proot |
|---|---|
| Reading and editing code | Building with Gradle |
| Chatting | Anything needing a package manager |
| Git operations | Node package installs |
| Running a few shell commands | Native compilation |
| Quick tasks | Anything whose verification command the phone cannot run |

**The app decides for the user, once, and says so.** When a run's verification fails with a missing toolchain, or when a project's inspection finds a toolchain the phone lacks, the run stops and offers:

> Dieses Projekt braucht *Gradle 8.7*. Auf diesem Gerät ist es nicht verfügbar.
>
> · Ubuntu-Laufzeit einrichten (2 GB, 5–15 Minuten)
> · Auf einem Server ausführen (empfohlen)
> · Trotzdem versuchen

Each option is a real choice with its cost stated. The app never silently switches profiles in the middle of a run, because a run that changes its environment underneath itself produces results nobody can reproduce.

## Installation

| Step | Behaviour |
|---|---|
| 1. `proot-distro` | Installed into the app's own prefix, with a checksum |
| 2. The Ubuntu image | Downloaded, with a progress indicator and a cancel. Verified against the image's own signature where one is published. |
| 3. First boot | `proot-distro login ubuntu`, which takes a minute or two and is where most of the perceived time goes |
| 4. The engine | Anthropic's official installer, run inside the guest. `process.platform` is `linux`, so it installs exactly as it does on a desktop. |
| 5. PATH | `$HOME/.local/bin` added to the shell profile |
| 6. Probe | The same probe as profile A: a version banner, then a streaming response |
| 7. A real build | The verification command of a known-good sample project, so the profile is proven to do its job rather than merely to start |

Step 7 matters. A profile that can run `claude --version` but cannot run Gradle is not a proot profile, it is a slower native profile. The health check requires a build to succeed.

## What is shared with profile A

| Shared | Separate |
|---|---|
| The engine's update logic | The prefix |
| The probe | The storage location |
| The health checks | The install path |
| The process supervision | The project data — which never moves |
| The permissions | |

Switching profiles does not touch a single project, conversation, or setting. The engine is re-installed; everything else is untouched. The app says this before the switch, and the switch is a confirm, not a tap.

## Performance

| Operation | Native | proot |
|---|---|---|
| `claude --version` | ~0.4 s | ~2 s |
| A model request, excluding the request | Negligible | Negligible |
| Reading a file | Fast | Fast |
| `git status` | Fast | Moderately slower |
| `npm install`, 200 packages | Unreliable | Slow but works |
| `./gradlew assembleDebug`, cold | Fails | 20–40 minutes |
| `./gradlew assembleDebug`, warm | Fails | 8–15 minutes |

The model request itself is not slower under proot. Everything local is.

**This is why a runner is usually the better answer.** A free Oracle instance does a cold Gradle build in about three minutes. A phone under proot does it in twenty. The app says this in the offer above, in the runner's own words: "Auf einem Server: etwa 3 Minuten. Auf diesem Gerät: etwa 25 Minuten. Der Akku wird dabei deutlich wärmer."

## proot's limits

Stated honestly, because they are real.

| Limit | Consequence |
|---|---|
| `ptrace`-based, not a real VM | Some syscalls behave differently. Most programs are unaffected; a few are. |
| No hardware virtualisation | Nothing that needs a hypervisor works. Not relevant for builds. |
| `fork` semantics | Some process-heavy tooling is slow or occasionally wrong under proot. |
| No kernel modules | Expected, and not needed. |
| Nested proot | Does not work. Not needed. |
| Performance overhead | Every syscall is translated. Fine for builds, poor for anything doing millions of small syscalls. |
| 2 GB of the user's storage | Real, and the main reason this is not the default. |

## Interaction with the hard blocks

**None.** The hard blocks live in the app's permission layer, above the backend, per ADR-006. A proot Ubuntu with a full userland and a package manager can do exactly as much damage as any Linux machine — and is blocked identically. The policy inspects intent, not the tool name, and that is why switching profiles does not change what is permitted.

## Storage management

| Concern | Behaviour |
|---|---|
| Size | 2 GB, shown before the install with the exact requirement |
| Growth | Builds generate caches. The storage screen breaks it down: image, engine, build caches, projects |
| Cleanup | A per-category cleanup. A "Caches leeren" action that removes build outputs, with a count and a size, and never touches a project file or a conversation. |
| Never | The app never deletes a project, a conversation, or a source file to free space. Build caches are the app's own artefacts and are the only thing it removes. |
| Uninstalling the profile | A confirm, and the image plus the engine are removed. Projects, conversations, and settings are untouched. The user keeps the native profile. |
| Disk pressure | Below 1 GB free, the app offers a cache cleanup and, if accepted, moves heavy projects to a runner. It never auto-deletes. |

## The decision to make it the default on a device with space

It is not, and this is a deliberate decision.

| Reason | Detail |
|---|---|
| 2 GB and 15 minutes before anything works | Somebody who installs this app to try it should not pay that. |
| `M2` is 85 % of first runs | The first run is chat, reading, and editing. Native covers it. |
| The failure mode of choosing wrong is a slow onboarding | Which loses most people at the first hurdle |
| The upgrade path is one tap | Once a user actually needs a build, offering it is natural |

The app tracks which projects have needed a toolchain and offers the upgrade at the point of need, which is both cheaper for the user and a better-converting moment than a setup screen asking about 2 GB before saying hello.

## Testing

| Test | Type |
|---|---|
| `InstallSteps` | E2E — all seven steps complete and the profile reaches `READY` |
| `InstallResumable` | E2E — interrupted at each step, resumed |
| `ImageVerified` | E2E — an unverified image is not extracted |
| `PlatformReportsLinux` | E2E — `process.platform` is `linux` inside the guest, which is the point of the profile |
| `OfficialInstaller` | E2E — the engine is installed by Anthropic's installer, not by a patched binary |
| `ProbeBothChecks` | E2E — a version banner and a streaming response, as for profile A |
| `RealBuildSucceeds` | E2E, nightly — a known-good Gradle project builds. Without this the profile is not proven. |
| `SwitchPreservesEverything` | E2E — switching from native to proot and back leaves projects, conversations, and settings byte-identical |
| `SwitchIsConfirmed` | UI — the switch requires a confirmation that states what is and is not touched |
| `NoSilentProfileSwitch` | E2E — a run never changes profile mid-execution |
| `ToolchainOffer` | E2E — a missing toolchain produces the three-option offer with the estimated durations, and it names the runner as recommended |
| `StorageBreakdown` | Screenshot — the storage screen attributes bytes to image, engine, caches, and projects |
| `CacheCleanupSafe` | E2E — a cache cleanup removes build outputs and touches no source file, conversation, or setting |
| `CacheCleanupConfirmed` | UI — the count and size are shown before the action |
| `UninstallKeepsProjects` | E2E — removing the profile removes the image and the engine and nothing else |
| `HardBlocksIdentical` | E2E — a hard block is refused under proot exactly as under native, at every level |
| `PerformanceRecorded` | Benchmark — the table above is generated, not typed, and a regression beyond 30 % fails the benchmark |
