# Competitive landscape

What already exists, what it does well, what it does not, and where this project fits. Written to prevent us from building something that already works, and to be honest about the cases where it does not.

**Assessed 2026-09-27.** This space moves fast; re-assess each phase.

## The categories

### 1. The official remote-control path

Anthropic offers Remote Control: start `claude remote-control` on your machine, and the Claude mobile app talks to that session. The claude.ai site has the same thing in a browser.

**What it does well:** it is the real thing, made by the people who made it. Full fidelity, because it *is* Claude Code. Subscription included, so no key to manage.

**What it does not do:**

- Your computer must stay on and the `claude` process must keep running. A phone that works only when a laptop is awake is not a phone tool.
- **Subscription plans only.** API keys are explicitly not supported, which excludes people who pay per use.
- **No BYOK, no OpenAI-compatible providers, no self-hosted models.** The user is inside one ecosystem.
- It is a *window into a desktop session*, not a phone-native experience. It optimises for the person who walks away from their desk, not the person who is away from their desk entirely.
- It cannot create a project, set up a runtime, or bootstrap anything. It assumes a working machine.

**Where we differ:** we do not need the laptop. We also cost less per unit of work for someone who uses a key, and we work with a local model.

**Where we are worse:** we are unofficial, our runtimes are workarounds, and Remote Control does not have those problems.

**Honest conclusion:** for someone with a laptop that is always on and a Pro subscription, Remote Control is better. We are not competing with that person.

### 2. Termux plus Claude Code

Install Termux, patch a binary, run `claude` in a terminal.

**What it does well:** it works, it is free, it is fully general, and it does not pretend to be a product.

**What it does not do:** it is a terminal. There is no chat, no plan, no diff review, no cost meter, no notifications, no verification, no autonomy, no projects, no GitHub integration, and no sense of what the agent is doing. For a technical user on a keyboard-less phone this is close to unusable; on a tablet with a keyboard it is a perfectly good tool.

**Where we differ:** everything above is what this project is.

**Where we are worse:** Termux gives you a real shell and a package manager. We give you a purpose-built environment and an opinionated one.

### 3. Community Claude Code mobile apps

Several small projects connect a phone to a Claude Code session over SSH or a relay.

**Common shape:** a chat interface, a session list, an approval prompt, and a terminal view. Useful, and closer to us than Termux is.

**Typical gaps:** no verification step, so a run that *claims* success is shown as success; no plan; no diff review; no skills management; no remote-runner offload; no self-healing; thin testing; and often no tests at all.

**Where we differ:** verification and judging are first-class (`05-features/verification.md`), so "done" means the project's own tests passed. That single difference is the largest one.

**Where we are worse:** most of them are one person's weekend. Ours has to survive contact with a real user.

### 4. Cloud IDEs

Browser-accessible development environments.

**What they do well:** they run anywhere, they persist, and they are a real Linux machine.

**What they do not do:** they cost money, they need a browser or a flaky web view, they are not designed for touch, and they require an account with a vendor. The browser requirement alone disqualifies them for this project — a web app in a WebView is explicitly what the user said this must not be.

### 5. Mobile-first AI build services

Natural-language app builders aimed at non-developers.

**What they do well:** they are genuinely accessible to a non-technical person, which is a high bar and one we are aiming at.

**What they do not do:** you get their app, not your code. No repository, no git, no pull request, no local ownership, and a closed platform. It is the opposite of what this project is for.

## The gap we are filling

Nobody is shipping: **a phone-native, chat-first, verification-driven, BYOK client that runs the real Claude Code on the device, works with private repositories through git, and needs no server of anyone's.**

Each existing option owns one column of that sentence. We are trying to own all of them.

## Our real weaknesses

Stated here so the README and the About screen are not lying:

| Weakness | Why | Mitigation |
|---|---|---|
| Unofficial | No support contract, no guarantee the engine keeps working on Android | The runtime is behind an interface; two independent paths exist; the update checker fails safe |
| Runtime fragility | A patched binary is a workaround that a future release can break | Rollback, health checks, a second profile, and a remote runner as the final fallback |
| Storage | 300 MB to 2 GB | We say so before the first download and offer a remote runner instead |
| Speed | A phone is not a build machine | Offload, and the app tells you when offloading is faster |
| Solo project | Bus factor 1 | Extensive docs, a test suite, and an architecture that a second person can enter |
| No iOS app | Promised, not delivered | We do not claim it. The shared layer is there; the app is not. |

## What would make us wrong

- Anthropic ships an official Android build. Then half our work disappears and the app becomes a better frontend for it. That is a good outcome, not a failure.
- A better third-party client appears with verification built in. Then we should merge or step aside.
- The patched-binary approach stops working and proot is too slow. Then the honest answer is a thin client for a remote runner, and we should build that instead of pretending.

## Positioning sentence, for the README

> The real Claude Code, on your phone, with your own key, against your own private repositories — and it does not call a task finished until your tests pass.
