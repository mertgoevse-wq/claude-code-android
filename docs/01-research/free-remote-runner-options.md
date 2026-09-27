# Free remote runners

Long jobs and heavy builds should not run on a phone. This document records the free options that were verified, what they are actually good for, and what happens when the free tier changes underneath us.

**Verified 2026-09-27. Cloud terms change. Every number below must be re-checked before it is shown to a user as a promise.**

## The requirement

| Need | Why |
|---|---|
| A machine that stays on | A run must survive the app being closed and the phone being idle |
| 2+ CPU cores | Builds are parallel |
| 8+ GB RAM | Gradle and Node are hungry; 1 GB is not enough |
| Persistent disk | Projects, dependencies, and the Claude Code install must survive a reboot |
| Outbound HTTPS | To the provider and to GitHub |
| No cost | Hard rule: the app never spends money |

## Option 1 — Oracle Cloud Always Free (preferred)

| Property | Value |
|---|---|
| Product | Ampere A1 compute, "Always Free" tier |
| Typical size | 4 OCPU and 24 GB RAM across the account, commonly taken as two instances of 2 OCPU / 12 GB |
| Disk | Up to 200 GB block volume, Always Free |
| Region | Home regions with available capacity; the capacity varies and is often exhausted |
| Network | Reasonable, metered above a free allowance |
| Expiry | None, but instances can be reclaimed if Always Free resources are idle for a long period |

**Why it is preferred:** it is a real, always-on Linux machine with a real userland. Claude Code installs with Anthropic's own installer, `process.platform` is `linux`, Gradle works, and there is no session limit to design around. For the persona who wants a project built overnight, this is the only free option that behaves like a computer.

**The catch, stated plainly:** signing up needs a card for verification even though nothing is charged, free capacity is frequently exhausted in popular regions, and capacity errors are common enough that the setup screen must handle failure as a normal outcome rather than an exception. Reclaiming an idle Always Free instance is also possible.

**Setup the app walks through:**

1. Create an Oracle account, verify the card.
2. Create an Always Free ARM instance, download the SSH private key.
3. Paste the public IP into the app's remote-runner screen.
4. The app generates a fresh key pair on the device, installs the public key via a one-time bootstrap command, and never asks for the Oracle private key.

The app never handles the cloud account password or the cloud private key. It holds an SSH key it generated and a host it was given.

**What the app checks before offering it:** reachability, SSH auth, `node --version` and `python3 --version`, free disk, and whether Claude Code is installed. It reports all of it in one screen.

## Option 2 — GitHub Actions (short jobs only)

| Property | Value |
|---|---|
| Free minutes | A monthly allowance on public repositories; private ones consume a smaller included pool |
| Session limit | Six hours per job on hosted runners |
| Disk | Ephemeral, ~14 GB of working space |
| Startup | Job queue plus container start, tens of seconds to a few minutes |
| Persistence | None between jobs, unless a cache or an artifact is used |

**Good for:** a verification run, a build, a test suite, a one-off task. Anything with a defined end.

**Bad for:** a project the agent works on for hours across many turns. A job that ends kills the session, and re-establishing the full conversation context each time is slow and expensive.

**What the app does with it:** runs a job, streams `AgentEvent`s back over the workflow log or a job artifact, and reports the result. The app's job model maps to a workflow run, and the session ID is stored in a repository variable so a follow-up job can resume it.

**This is the fallback runner**, not the primary. The app proposes it when a task looks like a build or a test run rather than an open-ended project.

## Option 3 — Home PC (over the network)

| Property | Value |
|---|---|
| Hardware | Whatever the user has |
| Cost | Zero, plus their electricity |
| Reachability | Needs a stable way in: Tailscale, a VPN, WireGuard, or an SSH tunnel |
| Always on | Only while the machine is on |

**Why it is real and not a consolation prize:** a home machine has the user's own toolchains, their own keys, their own disk, and no quota. For the persona who already has a server, this is the best runner, and it is the one that will still work in five years.

**What the app provides:** an SSH-based backend, a one-command bootstrap that installs Claude Code and the app's bridge agent on the target, and a key-based auth flow the user controls.

**Privacy note, stated in the UI:** a remote runner means code leaves the phone. The app says so before the first offload, names the exact host, and requires a one-time confirmation. It is never done implicitly.

## Comparison for the user

| | Oracle Always Free | GitHub Actions | Home PC |
|---|---|---|---|
| Cost | Free | Free within quota | Free |
| Card needed | Yes (verification only) | No | No |
| Stays on indefinitely | Yes | No | Only while on |
| Full Linux userland | Yes | Yes | Yes |
| Good for long agent sessions | Yes | No | Yes |
| Setup effort | Medium | Low | Low if already set up |
| Survives a quota change | Poorly | Poorly | Perfectly |
| Privacy | Code on a third-party machine | Code on GitHub's infrastructure | Code stays home |

**The app's recommendation, in order:** home PC if one exists, Oracle if not, GitHub Actions for short jobs only. This ordering is what the setup screen offers, with the reasoning stated in one line per option.

## Handling quota changes

Free tiers are withdrawn. This will happen. The app is built for it:

- Each runner is **probed on demand**, not trusted permanently. A failed probe marks the runner unavailable with the reason and proposes an alternative.
- No feature depends on a specific provider. `ExecutionBackend` is an interface; a new backend is one file and one screen.
- The offline-first promise holds regardless: **the app is fully functional with zero remote runners.** Offload is an optimisation, not a dependency.
- A provider's terms are summarised in the setup screen, with the date they were checked, so nobody is surprised by a change they did not read.

## What the app refuses to do

- Provision a paid instance. Ever. There is no "upgrade" button, no card field, and no path from a free tier to a paid one.
- Run a long job on a free tier that the provider's terms prohibit. If a provider says a workload is not allowed on its free tier, the app does not offer it.
- Hide where code is going. The target host is named in the confirmation, in the run header, and in the log.

## Open items

- Verify the current Oracle Always Free quota, the current regions with capacity, and the current idle-reclaim policy. **TBD — verify before phase 5 and record the date.**
- Verify the current GitHub Actions included minutes for public and private repositories on free plans. **TBD.**
- Verify whether a free GitHub-hosted runner allows the outbound connections we need and has a stable egress. **TBD.**
- Decide whether the app hosts a small relay for runners behind NAT. Leaning strongly towards **no**, because it would break the no-server promise. The app's answer is "use Tailscale". **TBD — confirm in phase 5.**
