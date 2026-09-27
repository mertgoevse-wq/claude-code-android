# Release checklist

A release is a checklist, not a feeling. Every box below is either ticked or the release does not ship. The checklist is copied into the release issue and ticked there, so the history is in the repository rather than in someone's head.

Rule: **no box is ticked on someone else's behalf.** If a step was skipped, the box stays empty and the release waits.

## 0. Release metadata

| Field | Value |
|---|---|
| Version | from `version.txt` |
| Tag | `v<versionName>` |
| Date | — |
| Build | commit SHA |
| Verified by | the person or agent that ran the gate |
| Channels | F-Droid / direct APK / Play (closed) |

## 1. The gates — all must be green

These are the same gates as `claude-code-android-spec.md` §22.2, and a red one stops the release.

| # | Gate | Command | Result |
|---|---|---|---|
| 1.1 | Assemble debug | `./gradlew assembleDebug` | ☐ |
| 1.2 | Full check (lint, detekt, ktlint, tests) | `./gradlew check` | ☐ |
| 1.3 | Unit coverage `shared/domain` ≥ 90 % | `./gradlew :shared:domain:koverVerify` | ☐ |
| 1.4 | Integration coverage `shared/data` ≥ 80 % | `./gradlew :shared:data:koverVerify` | ☐ |
| 1.5 | All 5 E2E journeys on an emulator | `./gradlew e2eTest` | ☐ |
| 1.6 | Screenshot baselines, no unexplained diffs | `./gradlew validatePaparazziDebug` | ☐ |
| 1.7 | Doc manifest: 135 docs present and non-stub | `tools/check_doc_manifest.py` | ☐ |
| 1.8 | Source manifest: every listed file present | `tools/check_source_manifest.py` | ☐ |
| 1.9 | Shared-layer purity: no Android imports | `tools/check_no_android_imports_in_shared.py` | ☐ |
| 1.10 | Secret scan clean | `scripts/check-no-secrets.sh` | ☐ |
| 1.11 | Analytics scan clean | `scripts/check-no-analytics.sh` | ☐ |
| 1.12 | Hard-block tests pass, including refusal of delete | `./gradlew :shared:orchestration:test --tests '*HardBlock*'` | ☐ |
| 1.13 | FOSS dependency check | `tools/check_foss_dependencies.sh` | ☐ |
| 1.14 | No critical accessibility findings | `./gradlew :app:connectedAndroidTest` (a11y suite) | ☐ |

## 2. On-device verification — a real device, not an emulator

| # | Check | Result |
|---|---|---|
| 2.1 | Fresh install on a phone with no prior app data; onboarding completes | ☐ |
| 2.2 | Runtime bootstrap reaches `READY`; `claude --version` runs | ☐ |
| 2.3 | A headless prompt streams a response back | ☐ |
| 2.4 | Screen off for 10 minutes mid-run; the run survives and the notification is correct | ☐ |
| 2.5 | Battery saver on; the run continues | ☐ |
| 2.6 | A task on a private test repo produces a green branch and an open PR | ☐ |
| 2.7 | A red test suite blocks the push, with the reason shown | ☐ |
| 2.8 | Interrupt mid-run; partial work lands on a `wip/` branch and is recoverable | ☐ |
| 2.9 | Retry budget exhausted; the report is clear and the counter is honest | ☐ |
| 2.10 | The app lock works after the grace period | ☐ |
| 2.11 | No secret appears in logcat during a full run | ☐ |
| 2.12 | Uninstall removes every trace (`filesDir/cca`, databases, Keystore entries) | ☐ |
| 2.13 | German and English both complete a task | ☐ |
| 2.14 | Dark theme reviewed on a real OLED panel, not only in a screenshot test | ☐ |

## 3. Security and privacy

| # | Check | Result |
|---|---|---|
| 3.1 | Threat model re-read; any changed control updated in the same commit | ☐ |
| 3.2 | `privacy.md` matches the shipped network allowlist, host for host | ☐ |
| 3.3 | Network security config: cleartext disabled, allowlist unchanged or the change is documented | ☐ |
| 3.4 | Secrets absent from Android auto-backup; verified in a restore onto a new device | ☐ |
| 3.5 | `FLAG_SECURE` behaves on the chat and terminal screens | ☐ |
| 3.6 | Signing fingerprint in the committed allowlist | ☐ |
| 3.7 | No new dependency without a line in `10-build/dependency-versions.md` and a licence review | ☐ |
| 3.8 | Privacy policy page is reachable and current | ☐ |

## 4. Artefact

| # | Check | Result |
|---|---|---|
| 4.1 | `assembleRelease` and `bundleRelease` from a clean checkout | ☐ |
| 4.2 | `scripts/verify_release.sh` passes, all six sub-checks | ☐ |
| 4.3 | `versionCode` strictly greater than the previous release | ☐ |
| 4.4 | SHA-256 recorded and published | ☐ |
| 4.5 | APK installs over the previous release without data loss (migration path tested) | ☐ |
| 4.6 | Downgrade is refused cleanly, not by a crash | ☐ |
| 4.7 | Release notes written: what changed, what broke, what to do | ☐ |

## 5. Documentation

| # | Check | Result |
|---|---|---|
| 5.1 | Every behaviour change in this release has its doc updated in the same commit (`/doc-sync`) | ☐ |
| 5.2 | `15-appendix/changelog.md` has an entry for this version | ☐ |
| 5.3 | README matches reality: every claimed feature exists, every install command works | ☐ |
| 5.4 | Architecture diagram in README matches `02-architecture/system-overview.md` | ☐ |
| 5.5 | `14-build-plan/progress-log.md` is current | ☐ |
| 5.6 | Any `TBD — verify at build time` marker in a shipped feature is resolved or listed as a known gap | ☐ |

## 6. Per-channel

### F-Droid

| # | Check | Result |
|---|---|---|
| 6.1 | Clean-room build with no Gradle cache, no Google services on the device | ☐ |
| 6.2 | `fastlane downloadMetadata` produces a complete listing | ☐ |
| 6.3 | Version in the recipe matches the tag | ☐ |
| 6.4 | Update check in `dist` is off by default | ☐ |

### Direct APK

| # | Check | Result |
|---|---|---|
| 6.5 | Unofficial notice visible before the download link | ☐ |
| 6.6 | SHA-256 next to the file | ☐ |
| 6.7 | Play Protect warning mentioned before install | ☐ |
| 6.8 | No tracking parameters on the download URL | ☐ |

### Play Store (only when publishing)

| # | Check | Result |
|---|---|---|
| 6.9 | AAB signed and uploadable; version code not reused | ☐ |
| 6.10 | Data-safety form matches `11-operations/privacy.md` word for word | ☐ |
| 6.11 | Listing screenshots are current captures, both themes, phone and tablet | ☐ |
| 6.12 | Closed testing track requirements met, current numbers verified | ☐ |
| 6.13 | Rollout staged at 10 %, monitored by hand | ☐ |

## 7. Sign-off

| Field | Value |
|---|---|
| Gates green | ☐ |
| Device verification complete | ☐ |
| Security and privacy complete | ☐ |
| Artefact verified | ☐ |
| Documentation current | ☐ |
| Rollback plan written | ☐ |
| Approved by | — |

## 8. Rollback

| Situation | Action |
|---|---|
| Crash-free rate drops in the staged rollout | Halt the rollout in Play Console. Users on the previous version are unaffected |
| The release signature is wrong | Stop. Play cannot replace a signature, and the release is dead; fix forward with a new application id if necessary, and record it in `risk-register.md` |
| A runtime regression that breaks the bootstrap | The app's own rollback in `05-features/self-update.md` restores the last known-good engine; the release is pulled, the engine is fixed, and a patch release ships |
| A data-loss bug | Halt. Do not ask users to reinstall. Fix forward; the migration path in `02-architecture/data-migrations.md` is what makes this survivable |
| Documentation is wrong but the build is fine | Fix forward with a docs-only commit. Do not halt a working release for a README |

## 9. After the release

### 9.1 The launch video — `/brag`

**What it is, and what it is not.** `/brag` is a Claude Code skill from
`latent-spaces/brag` that renders a short launch video from the finished
project. It is a **marketing asset generated after the build**. It is not a
build dependency, it ships nothing into the APK, and no gate depends on it. A
failure here does not fail a release.

```bash
npx skills add https://github.com/latent-spaces/brag --skill brag
# then, from the finished project directory:
claude --dangerously-skip-permissions "let's /brag"
```

It writes to `brag-output/`: the plan, a composition brief, the share copy, and
`brag.mp4`.

**Prerequisites, verified rather than assumed:**

| Requirement | State at 2026-09-27 | Note |
|---|---|---|
| Node.js 22 or newer | **present** — v24.21.0 | Satisfied |
| FFmpeg on `PATH` | **MISSING** | The operator must install it. `npx hyperframes doctor` reports the rest |
| The `skills` CLI | present — 1.5.26 | Satisfied |
| A finished project with real screenshots | not yet — arrives with the APK | Run this last |

**The 17 MB question.** The installer's bundled music and sound effects are
third-party media, and their licence is not yet recorded in
`THIRD_PARTY_NOTICES.md` — so they are **not** committed. The skill's
`SKILL.md`, `references/`, and `scripts/` are tracked, which is what another
model needs to reproduce the step. The media comes back with the one-line
install above. If the media licences are looked up and added to the notices, the
assets can be vendored instead; until then, fetching beats vendoring an
unreviewed licence.

### 9.2 The rest

Within 24 hours:

Within 24 hours:

- ☐ `15-appendix/changelog.md` published with the tag.
- ☐ README badge points at the new tag.
- ☐ F-Droid metadata updated.
- ☐ `14-build-plan/progress-log.md` records the release and the next phase.
- ☐ Any field-error signature seen in the wild turned into a triage entry (`05-features/field-error-triage.md`).

## Depends on

`12-delivery/apk-distribution.md` · `12-delivery/f-droid.md` · `12-delivery/play-store.md` · `14-build-plan/progress-log.md` · `13-process/definition-of-done.md` · `10-build/signing-and-keystores.md`
