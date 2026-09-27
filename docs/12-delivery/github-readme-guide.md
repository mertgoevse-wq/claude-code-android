# GitHub README guide

The README is the front door. It is the first thing a person sees, and it decides whether they install, read, or leave. D27 makes it explicit: the README gets the same anti-slop treatment as the UI.

This document is the contract for `README.md` at the repository root. A change to the README that violates this file is a review rejection, not a style opinion.

## 1. Structure, top to bottom

The order is not arbitrary. It follows what the reader needs at each moment: what is this, can I trust it, what does it do, how do I get it, how does it work, what can't it do, how do I help.

| # | Section | Purpose | Length ceiling |
|---|---|---|---|
| 1 | Hero | The mark, the one-line promise, one supporting line | 3 lines of text |
| 2 | Status badges | Build, licence, min SDK, release | 4 badges |
| 3 | Screenshot | The single best image: chat detail with a run in progress, light and dark | 1–2 images |
| 4 | What it is | Three sentences. The product, not the architecture | 3 sentences |
| 5 | Feature grid | Six tiles, one line each, real features only | 6 tiles |
| 6 | Install | One primary path, one alternative, exact commands | ≤ 20 lines |
| 7 | How it works | The architecture diagram, one paragraph | 1 diagram + 1 paragraph |
| 8 | Honest limitations | What this does not do, and what might break | 5–8 bullets |
| 9 | Safety | The hard blocks, stated as a feature | 5 bullets |
| 10 | Documentation | A table pointing into `docs/`, not a wall of links | 16 rows |
| 11 | Contributing | `CONTRIBUTING.md` in three lines, plus the dev command | 5 lines |
| 12 | Licence and notices | `LICENSE`, `THIRD_PARTY_NOTICES.md`, the unofficial notice | 5 lines |

Nothing goes above section 1. No "⭐ Star us" banner, no contributor avatars row, no sponsors block, no table of contents before the first sentence of value.

## 2. Section rules

### Hero

- The app's **own** mark, from `03-design/brand-assets.md`. Never a copied logo, never an Anthropic asset.
- The one-sentence promise, verbatim from `00-vision/vision.md`, so the README and the product cannot drift.
- A single supporting line that says what it is technically, in plain words.
- The unofficial status is visible **in the hero**, not only in the footer. A first-time reader must not have to hunt for it.

### Feature grid

Six tiles. Each is one line, starts with a verb, and describes something that works in the shipped build.

Banned, because these are the exact words that make a README read as generated: *seamless*, *powerful*, *revolutionary*, *cutting-edge*, *game-changing*, *effortless*, *robust*, *leverage*, *unlock*, *elevate*.

Banned in this README specifically: any feature that is on the roadmap. A reader who installs and finds the advertised thing missing does not read the rest of the file.

### Install

Two paths, in this order:

1. **F-Droid** — a badge, because it is the recommended channel and the most honest one.
2. **Direct APK** — a link, a SHA-256 line, and the sentence about Play Protect warning on sideloads, stated *before* the user taps, not after.

```bash
# Development, from a clone
git clone https://github.com/mertgoevse-wq/claude-code-android.git
cd claude-code-android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Honest limitations

The section that earns the rest of the README's credibility. It is not an apology; it is a specification of the edges.

Required entries:

- Unofficial and unaffiliated with Anthropic.
- The engine is a Linux `arm64` binary running through a patched ELF interpreter; Qualcomm Snapdragon has no AVF support, and heavy builds are slow on a phone.
- API keys are the user's own; there is no subscription login.
- Autonomous runs are long and consume battery. A foreground service keeps them alive, and the phone is not a server.
- No iOS build. The shared layer is prepared and untested there.
- Remote runners depend on free-tier quotas that change without notice.
- Verification commands are detected heuristically and can be wrong; they are editable.
- The project is young. Say the version and say it.

Every one of these is traceable to a risk in `14-build-plan/risk-register.md` or a non-goal in `00-vision/scope.md`. A limitation we cannot point at is a limitation we have not thought about, and it does not go in the README.

### Documentation table

One row per section, 16 rows, with the count and the one-line purpose. The reader should be able to find the right document in one click, which means the table is exactly the section table in `docs/00-vision/README.md`.

## 3. Visual rules

| Rule | Value |
|---|---|
| Diagram | One, hand-authored Mermaid or a committed SVG. It shows the `ExecutionBackend` boundary, because that is the one idea a reader cannot get from a feature list |
| Screenshots | Real captures, from `04-screens` golden images, at 2× density, both themes, no device bezels, no marketing lighting |
| Screenshot count | Three maximum above the fold. A wall of screenshots means the reader is doing your work |
| Alt text | Every image describes what it shows, not "screenshot" |
| Width | Content column ≤ 72 characters. Long lines are hard to read in a browser diff view |
| Emoji | None, in prose and in headings. The app bans emoji as UI icons; the README bans them as typography. Badge icons from shields.io are the exception, and only in the badge row |
| Tables | Used for genuinely tabular data — install paths, requirements, the doc index. Never for layout |
| Bold | One emphasis per paragraph. Bold in every bullet is a way of shouting that stops meaning anything |

## 4. Anti-slop checklist

Run before every README change. A single failure blocks the change.

- [ ] The first line states what the thing is, not what it is not.
- [ ] The unofficial notice is in the hero.
- [ ] No word from the banned list.
- [ ] Every feature claimed is in the shipped build.
- [ ] Every screenshot is a real capture.
- [ ] The limitations section exists and is not softened.
- [ ] The install path is one command, and it works from a fresh clone.
- [ ] The SHA-256 is present next to the direct download.
- [ ] No table of contents before the first paragraph.
- [ ] No star, fork, or contributor graphics.
- [ ] No "coming soon" in the feature list.
- [ ] Every internal link resolves, checked by `tools/check_doc_manifest.py`.
- [ ] Read aloud, it does not sound like a press release.

## 5. Localisation

The README is **English only**. The app is German-first, and German users arrive through F-Droid and the Play listing, which carry localised listings from `fastlane/metadata`. One good English README beats two mediocre ones, and a stale translation is worse than none.

If a second README is ever wanted, it goes in `docs/12-delivery/` as a translation source with its own owner, never as a fork of the English file.

## 6. How the README is kept true

| Claim in the README | Kept true by |
|---|---|
| Feature grid | The reviewer subagent checks each tile against `05-features/`; a feature without a doc is removed from the tile |
| Install commands | `./gradlew assembleDebug` in CI on every merge; a broken command is a red build |
| SHA-256 | Emitted by `fastlane` at build time, inserted by `scripts/verify_release.sh` |
| Architecture diagram | Mirrors `02-architecture/system-overview.md`; a change to either without the other is rejected |
| Limitations | Mirrors `00-vision/scope.md` and `14-build-plan/risk-register.md` |
| Status badges | The release badge points at the tag; the build badge is the CI workflow; nothing is hand-edited |

## Depends on

`00-vision/vision.md` · `00-vision/scope.md` · `02-architecture/system-overview.md` · `03-design/anti-slop-rules.md` · `03-design/brand-assets.md` · `01-research/legal-and-trademark.md` · `14-build-plan/risk-register.md` · `13-process/ai-usage-policy.md`
