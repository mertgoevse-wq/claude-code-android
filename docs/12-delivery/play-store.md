# Play Store

Play Store is **prepared, not published**, in v1. Signing, the AAB, the listing, the privacy policy, and the data-safety form are all done and tested. Uploading is a later phase, and this document says what has to happen then.

## 1. Why prepared and not published

| Reason | Detail |
|---|---|
| Scope | D14 in the spec: shipping to public review in v1 was excluded. Preparing is reversible; a review rejection is not |
| Review friction | A developer-tool app holding API keys and executing code attracts questions. Answering them is easier with a real, testable build than with a mock |
| Legal | The unofficial status and the brand decision in `01-research/legal-and-trademark.md` are settled, but a listing makes them visible to a wider audience than a GitHub release does |

Nothing here blocks a release. A signed AAB exists and can be uploaded whenever the decision is made.

## 2. What is already done

| Item | Status | Where |
|---|---|---|
| App Bundle build | `bundleRelease` produces a signed AAB | `10-build/build-variants.md` |
| Application id | `dev.claudecode.android` | `gradle.properties` |
| Target SDK | Current stable, recorded with its date | `10-build/dependency-versions.md` |
| Data-safety form | Filled in from `11-operations/privacy.md`; answer: **no data collected, no data shared** | §4 |
| Privacy policy | Published as a page in the repository and linked from the listing | §5 |
| Store listing text | `fastlane/metadata/android/en-US` in German and English | §3 |
| Screenshots | Phone and 7" tablet, light and dark, per locale | `04-screens` golden images |
| Feature graphic | Own asset, own mark | `03-design/brand-assets.md` |
| Content rating | Questionnaire completed; the app has no user content, no ads, no chat with strangers | §3 |

## 3. The listing

| Field | Content rule |
|---|---|
| Title | `Claude Code Android` — descriptive use of the name, with the unofficial disclaimer in the description, per `01-research/legal-and-trademark.md` |
| Short description | One sentence, the promise from `00-vision/vision.md` |
| Full description | Problem → promise → what it does → what it does not do → install → honesty block. The honesty block is not optional |
| Category | Developer tools |
| Tags | At most five, factual |
| Support email, website | A real address a maintainer reads |
| Privacy policy URL | Required, and reachable without a login |
| Data safety | See §4 |
| Content rating | Completed |
| Ads | No |
| In-app purchases | No |
| Target audience | 18+ (the app executes arbitrary code and holds credentials) |

Screenshots: onboarding, chat with a run in progress, the diff viewer, the project list with an open PR, the terminal, settings. Real screenshots, never mockups — the anti-slop rules apply to marketing assets too.

## 4. The data-safety form, honestly

| Question | Answer | Justification |
|---|---|---|
| Does your app collect or share user data? | **No** | `11-operations/telemetry.md`. There is no analytics SDK and no event path |
| Is all user data encrypted in transit? | Yes where data leaves the device — HTTPS only, cleartext disabled | `11-operations/security-threat-model.md` T10 |
| Do you provide a way to request data deletion? | **Yes** — Settings → Storage → "Delete app data", which removes every project, conversation, and log | `05-features/project-lifecycle.md` |
| Does the app share data with a third party? | The user's prompts go to the provider **the user configured**. This is disclosed in the app and in the policy | `07-integrations/providers.md` |

The fourth row is the one that must be worded precisely. The app *does* send the user's content to an AI provider, because that is the product, and because the user chose the provider and pasted the key. The listing says so in plain words. Understating it would be the actual policy violation.

## 5. The privacy policy page

A single Markdown file, `docs/15-appendix/` adjacent, rendered to a static page, containing:

1. What the app is and that it is unofficial.
2. What leaves the device: prompts and code context, to the provider the user configured; git operations, to the repository the user configured.
3. What never leaves the device: keys, tokens, transcripts, logs, projects.
4. The data-safety answers from §4, in the same wording.
5. How to delete everything, and what deleting the app does.
6. No analytics, stated as a decision rather than an omission.
7. The licence and the third-party notices.

The page is updated in the same commit as any change to `11-operations/privacy.md`. `/doc-sync` enforces it.

## 6. Review handling

| Reviewer question | Answer |
|---|---|
| "Does the app execute code?" | Yes — that is its purpose. It executes in app-private storage, under a foreground service, with a visible transcript and hard blocks |
| "Does it download executables?" | Yes — the Claude Code engine, from the vendor's CDN, verified against a published checksum before it runs. Documented in `06-runtime/native-profile.md` |
| "Why does it need so much permission?" | Each permission maps to one capability, and the table lives in `fastlane/metadata/android/en-US` as a reviewer note |
| "Is it affiliated with Anthropic?" | No. Stated in the listing, the About screen, and the policy |
| "Can it spend money?" | No purchase path exists in the app. Cost of the user's own API key is displayed, never charged by us |

Prepared reviewer notes live in `fastlane/metadata/android/en-US/reviewer-notes.txt` so the answer exists before it is asked.

## 7. What happens when we publish

1. F-Droid submission first — it is faster, and it is the channel we recommend.
2. Play as a **closed testing track**, minimum 12 testers, 14 days. This is the requirement as of the current Play policy; **verify the current numbers at publish time** and record the check in `14-build-plan/progress-log.md`.
3. Only after the closed track is clean, an open production release at a staged rollout of 10 %.
4. The staged percentage is raised by hand, one step per day, watching crash-free rate from opted-in reports only (`11-operations/telemetry.md`).

## 8. Anti-features we must not introduce

To keep the F-Droid channel viable and the listing accurate, these are rejected at review like any other change:

| Anti-feature | Consequence if introduced |
|---|---|
| An analytics SDK | Rejected. Requires an ADR, and the F-Droid recipe must be updated |
| Play Services dependency | Rejected. Breaks the F-Droid build |
| Ads or a sponsored placement | Rejected |
| In-app purchase of any kind | Rejected, and violates a hard block |
| A proprietary binary in the APK | Rejected. The engine is downloaded at runtime, never bundled |

## Depends on

`12-delivery/apk-distribution.md` · `12-delivery/f-droid.md` · `11-operations/privacy.md` · `01-research/legal-and-trademark.md` · `12-delivery/release-checklist.md`
