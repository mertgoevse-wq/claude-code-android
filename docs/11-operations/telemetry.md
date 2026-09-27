# Telemetry

There is none. This document exists to say what that means precisely, and to make the decision hard to reverse by accident.

## The decision

**The app collects no telemetry.** No analytics SDK, no usage counters sent anywhere, no crash reporting unless a person opts in, no remote configuration, no feature flags fetched at runtime, no A/B assignment, no session recording, no heatmaps, no funnel tracking, no "anonymous" identifiers.

## What "none" means here, exactly

| Thing | Status |
|---|---|
| An analytics SDK as a dependency | Not present. `dependency-analysis` reports the absence; a PR that adds one is rejected without a design discussion, not a privacy one. |
| A code path that sends a usage event | None exists. A network request to an unlisted host fails the CI check in `10-build/static-analysis.md`. |
| An event queue flushed on app start | None. The app makes no request on launch except the self-update check, which is off by default in `dist` and a plain conditional GET otherwise. |
| A device or user identifier | None. No `AD_ID`, no `ANDROID_ID`, no install ID, no fingerprint. |
| A remote config fetch | None. Every feature is decided in code, which means every feature is in the APK. |

## Why

Three reasons, in order of how much they matter.

**It is not needed.** The product's value is local: the user's code, the user's keys, the user's phone. Nothing about improving it requires knowing what other people do. Crash reports, when a person chooses to send them, cover the one case where outside information genuinely helps.

**It is a liability, not a feature.** Telemetry in an app that holds API keys, source code, and full conversation transcripts is a promise about handling the most sensitive data a developer has. A breach of that promise is not a bug that gets patched quietly; it is the end of the project's credibility. Not shipping the capability removes the class of failure entirely.

**Users of a developer tool notice.** This audience reads manifests, checks installed packages, and knows what a tracking SDK looks like. A presence would cost more trust than any insight could buy back.

## What is measured instead — on the device, by the user

Three counters, visible in Settings → Über die App, stored locally, never transmitted:

| Counter | What it counts | Why it is useful |
|---|---|---|
| Runs | Completed and failed runs, since install | Lets a user see their own usage |
| Success rate | Runs reaching `DONE` with a `PASSED` verification | Tells the user how well the tool is working for *them* |
| Token and cost totals | Per model, per project, cumulative | The cost meter in `11-operations` is a feature, not telemetry |

They exist because a person using an agent to write code will want to know what it has spent. The number is displayed prominently, is always local, and can be reset with one tap — resetting does not delete history, it only zeroes the counter, and it says so.

## The one number we publish

A crash-free rate, computed from opted-in crash reports only, reported in `docs/00-vision/success-metrics.md` **with its sample size stated**. Reporting "99.4 % crash free" without saying "of 41 opted-in installations" would be the exact kind of number this document exists to prevent.

## How the decision is protected

| Protection | Mechanism |
|---|---|
| No SDK sneaks in | `dependency-analysis` in CI; the app module's dependency list is reviewed on every PR |
| No manual event call sneaks in | `scripts/check-no-secrets.sh` is extended into `scripts/check-no-analytics.sh`, which fails the build on a known analytics symbol or a class name matching a known SDK package |
| No unexpected request | The network-security config permits only the configured hosts; a unit test asserts the allowlist matches the documented list in `privacy.md` |
| No remote config | There is no interface for it, so there is nothing to call |
| The decision stays written down | This file, referenced from `privacy.md`, `adr-log.md`, and the README |

`scripts/check-no-analytics.sh` runs a symbol scan over the release APK:

```bash
# Fails on: Firebase, Adjust, AppsFlyer, Amplitude, Mixpanel, Sentry (unless opted in
# at build time), Segment, Datadog, New Relic, Bugsnag, App Center, and 20 more.
./gradlew :app:assembleRelease
scripts/check-no-analytics.sh app/build/outputs/apk/release/app-release.apk
```

The allowlist is empty. If a crash-reporting client is ever bundled, it is a build-time decision, off by default, and the script has a comment saying why.

## The escape hatch, stated in advance

If a future maintainer decides this is wrong, the correct sequence is:

1. Write an ADR superseding the relevant entry in `adr-log.md`, with the reasoning.
2. Add the SDK behind a build-time flag, off by default.
3. Add a visible, per-purpose switch in Settings, defaulting to off, described in plain German and English.
4. Update `privacy.md` with the new destination, the new data, and the retention.
5. Add the SDK to the `check-no-analytics.sh` allowlist **with a comment pointing at the ADR**, so the next person to see it learns that it was a decision rather than an oversight.

None of those steps is a default, and none of them happens by accident.
