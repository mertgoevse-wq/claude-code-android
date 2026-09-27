# The AVF profile

The experimental one. Android's built-in virtualisation, a real Linux kernel, and hardware support on a small number of devices.

## Status

**Experimental. Not depended upon. Nothing in the app breaks if it never ships.**

It is here because it is the only path that could be called genuinely native, and because ignoring a real option would be dishonest about what we know. It is also the narrowest, and the app says so.

## What it is

Android 16 exposes a Linux development environment built on the Android Virtualization Framework, with a real kernel, a Debian image, and no Termux and no root.

| Property | Value |
|---|---|
| Mechanism | Android's built-in hypervisor, AVF |
| Guest | Debian, provided by Android |
| Kernel | A real Linux kernel, not a translation layer |
| Root | Not needed, and not used |
| Termux | Not involved at all |
| Storage | Managed by the OS, not by us |
| Engine | Installed inside the guest with Anthropic's official installer, exactly as on a desktop |

## Requirements

| Requirement | Detail |
|---|---|
| Device | Pixel 6 or later |
| Android | 16 or later |
| Developer options | Enabled: Settings, System, Developer options, "Linux development environment" |
| If Developer options is missing | Settings, About phone, tap Build number seven times |
| If the toggle is still missing | The device does not support it. The app says so and stops. |
| Qualcomm Snapdragon | **Not supported.** |
| Exynos | The Galaxy S26 and S26+ are reported to work; not lab-verified by us |

The app detects all of this before offering the profile, and the detection result is a screen, not a hope.

## Why it might be the best path, eventually

| Property | Native | proot | AVF |
|---|---|---|---|
| Kernel | Android's | Android's | A real Linux kernel |
| Syscall fidelity | Shimmed | Translated | Native |
| Hardware virtualisation | No | No | Yes |
| `process.platform` | `linux` | `linux` | `linux` |
| Device support | Most arm64 phones | Most arm64 phones | A handful |
| Requires root | No | No | No |
| Requires Termux | No | No | No |

A real kernel means real Linux, which removes the entire class of "works under proot, except when". The reason it is not the answer today is the device list, and the device list is not ours to change.

## Detection

| Step | Behaviour |
|---|---|
| 1 | Android version ≥ 16 |
| 2 | The device is in the supported set. Matched on the build fingerprint, because there is no capability API for this. |
| 3 | The developer-options toggle is present and on |
| 4 | The guest image is available or downloadable |
| 5 | A probe: the guest launches, `uname -a` returns a Linux kernel, and the expected package manager is present |

A device that fails step 2 gets: "Dieses Gerät unterstützt die Linux-Virtualisierung nicht. Es kann auch mit Entwickleroptionen nicht aktiviert werden." and the other two profiles are offered. No attempt is made to force it.

The fingerprint list is a constant in the build configuration, dated, with a link to where it came from. A new device is added by editing one list.

## What the app does, and does not, do

| Does | Does not |
|---|---|
| Detect support | Install the guest. That is the user's action, in Android's own terminal. |
| Show the user the exact steps, with the Developer's-option path | Enable developer options for them. That is a system-level change and the user's decision. |
| Install Claude Code inside the guest, with a confirm | Modify any system setting |
| Probe and report | Require anything of the user's other apps |
| State clearly that it is experimental | Claim support for devices it has not been tested on |

**The app cannot install the guest.** Android's Linux terminal is a separate app, and the image download is its job. The app's contribution is a clear, correct, screenshot-free instruction list and then the install of Claude Code once the guest exists.

## The install, once the guest is up

| Step | Behaviour |
|---|---|
| 1 | Verify the guest: `uname`, the package manager, free disk |
| 2 | Show the command and ask: `curl -fsSL https://claude.ai/install.sh \| bash` with the full text visible before it is offered. Not a hidden pipe. |
| 3 | Run it, with output streamed and a cancel |
| 4 | Add `$HOME/.local/bin` to the profile, and verify it with the same probe as the other profiles |
| 5 | A real build, as for proot, so the profile is proven |

## Known limitations

| Limitation | Consequence |
|---|---|
| Pixel 6+ on Android 16+ only | Excludes most phones in use |
| No Snapdragon | Excludes most flagship phones outside the Pixel line |
| Not lab-verified on Exynos | Reported by a third party, not confirmed by us |
| Experimental in Android itself | The API, the image, and the behaviour can change without notice |
| Guest lifecycle is not ours | Android decides when it starts and stops |
| The user must enable developer options | A real barrier for a non-technical user, and the app does not pretend otherwise |

## What happens if it breaks

| Event | Response |
|---|---|
| Android changes the feature | Detection fails, the profile disappears, and the other two are unaffected |
| The guest image format changes | The install fails with a specific error, and the profile is marked unavailable |
| A run on it becomes unstable | The backend is marked degraded, new runs are refused with a reason, and existing ones finish |
| The user dislikes it | Switch back to native, one tap, nothing lost |

The last row is the important design property: **this profile is additive, and leaving it costs nothing.**

## Is it worth building?

| Argument for | Argument against |
|---|---|
| It is the only genuinely native path, and a real kernel removes a whole class of bugs | It reaches a small number of devices |
| If Android expands it, the app is already ready | It is experimental, and experimental things change without notice |
| It needs the least code of the three, because the hypervisor does the work | It requires the user to enable developer options, which the primary persona will not do |
| The detection and the instructions are small, and the information is valuable | Its users are early adopters who already have a terminal |

**Decision: ship detection and instructions, ship the install, do not invest further in it, and do not let any other part of the app know it exists.** That is what "additive" means in practice.

## Testing

| Test | Type |
|---|---|
| `DetectionMatrix` | Unit — every device and OS combination maps to supported or not, from the fingerprint list |
| `DetectionOnUnsupportedDevice` | UI — the specific explanation, with the other profiles offered, and no attempt to force it |
| `DetectionNoDeveloperOptions` | UI — the seven-tap path is described, and the app does not enable it |
| `NoSnapThroughClaim` | UI — asserts no screen claims support for a device outside the list |
| `ListDated` | Unit — the fingerprint list carries a date, and a test fails if it is older than 12 months |
| `InstallShowsCommand` | UI — the full install command is visible before it is offered, never a hidden pipe |
| `InstallAndProbe` | E2E — install, PATH, version banner, streaming response |
| `RealBuild` | E2E, nightly — a known-good build, as for proot |
| `NothingElseKnows` | Integration — a static check that no feature module references the AVF backend except the settings screen |
| `LeavingCostsNothing` | E2E — switching away leaves projects, conversations, and settings byte-identical |
| `DegradedRefusesNewRuns` | E2E — a degraded backend refuses new runs with a reason and lets existing ones finish |
| `ExperimentalEverywhere` | UI — asserts the word "experimentell" appears in the profile's own screen, in About, and in the runner comparison table |
