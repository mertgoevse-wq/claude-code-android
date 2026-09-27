# Vision

## The sentence

**A phone that does the coding.** Open the app, say what you want, and Claude Code builds it, tests it, fixes its own failures, and hands you a pull request.

## The problem

Claude Code is good. It is also bound to a desk. Three things follow from that:

1. **It is unavailable exactly when you think of the idea.** Ideas do not arrive at a desk. They arrive on a train.
2. **The interface is a wall of monospace text.** Claude Code is superb at the work and mediocre at showing you what it is doing. Reading a scrollback to find out whether it changed `build.gradle.kts` is work.
3. **The loop breaks constantly.** You need a human to approve, to unstick, to read the error, to tell it to try again. The work is long, so this happens often.

A chat interface that Claude Code does not have, on the device you already carry, removes all three.

## The shape of the thing

Not a terminal with a chat skin. Not a WebView of `claude.ai`. A native app whose entire reason to exist is that it can sit on your phone and wait for you, and whose interface is built around one question: *what is it doing right now, and does it need me?*

That question produces the whole design:

- **Collapsible tool cards** — a glanceable answer to "what is it doing", not a wall of scrollback.
- **A visible plan** — you see the steps before and during, so the work is not a black box.
- **A terminal pane** — because when you *do* want the wall of monospace text, it is one tap away and it is real.
- **A permission sheet that is honest about the stakes** — it says what will change and what it cannot undo.
- **Autonomy levels per project** — because a throwaway script and your production app should not get the same answer to the same question.

## What the work is

A person who wants software built, does not want to supervise a terminal. They want to describe a result.

The app's job is to make the distance between a sentence and a working result as short as possible, without hiding anything in between. Both halves matter. The distance, and the honesty. An app that hides the steps gets abandoned the first time something goes wrong, because the user cannot tell whether it is working or stuck.

## The engine is not ours

This app does not reimplement Claude Code. It runs the real one, on the real phone, and gives it real projects. Everything hard in this project is the consequence of that: Anthropic ships a glibc Linux binary, Android speaks Bionic, and there is no official Android build. See `01-research/claude-code-runtimes-on-android.md` for the three paths we found and why two of them ship.

If Anthropic ever ships an Android build, `06-runtime/native-profile.md` is the only document that changes.

## What we refuse

These are not preferences. They are enforced in code, tested, and additionally denied at the Claude Code permission layer. No screen can enable them.

1. **Never delete anything.** Files, branches, tags, repositories. A refactor that wants to remove a file gets a new file and a note, not a deletion.
2. **Never spend money.** No paid API, no subscription, no purchase, no upgrade prompt that leads to a paid tier.
3. **Never make anything public.** Repositories are private. Artifacts are private. There is no "share" that changes visibility.
4. **Never push to the default branch.** Work lands on a branch and arrives as a pull request.
5. **Never hide anything.** Every command, diff, decision, error, and cent of spend is recorded and readable by the user.

Rule 1 exists because a phone is a lossy environment: dropped connections, killed processes, an agent mid-thought. Deletion is the one operation that cannot be undone when the undo is on a machine that is not reachable. Rule 5 exists because the whole value proposition is "you can leave this running and come back" — which requires that what happened is legible when you return.

## Non-negotiable quality

- The app works with no third-party server. Free hosting exists as an *option* for people who want it, never as a dependency.
- The build is reproducible: fresh clone, one command, installable APK.
- The first run is honest about what the app will do on their phone: download a few hundred megabytes, run binaries, make network calls to their AI provider and to GitHub.
- The interface is designed, not defaulted. No stock Material, no placeholder text, no emoji standing in for icons, no cards nested in cards. See `03-design/anti-slop-rules.md`.
