# Diagnostics export

One button that produces a file a maintainer can actually diagnose from, with nothing in it that must not leave the device.

## What one tap produces

Settings → Help → "Diagnose exportieren". A share sheet opens with a single `.zip`. The user sees exactly what is in it before sharing: a summary sheet with the item list, sizes, and the redaction notice.

## Contents

| Item | What it is | Typical size |
|---|---|---|
| `summary.txt` | One page: app version, variant, Android version, device, ABI, runtime profile and version, storage used, whether crash reporting is on | 2 KB |
| `environment/` | The output of `06-runtime/environment-diagnostics.md`'s health checks, machine-readable and human-readable | 20 KB |
| `logs/app.log*` | The general log, rotated files, already redacted | up to 20 MB |
| `logs/run-<id>.log` | Only if the user selected a run; the run's own log | up to 2 MB |
| `runs/<id>/transcript.log` | The transparency log as text, one line per event, **content omitted** (see below) | up to 5 MB |
| `runs/<id>/summary.json` | Run metadata: prompt *length* and hash, model, autonomy level, every tool call with name and exit code, every verification result, token counts, cost, duration, retries | 50 KB |
| `runtime/manifest.json` | The runtime manifest: Claude Code version, archive checksum, patch record, install steps and their outcomes | 5 KB |
| `runtime/probe.txt` | The output of the runtime probe | 5 KB |
| `crashes/` | Recent crash reports, if the user has reporting on | up to 200 KB |
| `settings.json` | Non-secret settings, redacted. Autonomy levels, theme, notification preferences, enabled features | 5 KB |
| `keystore.json` | **Metadata only**: which key profiles exist, when they were created, which provider kind. No key material, no key hashes that could be used as identifiers. | 2 KB |
| `android/` | `dumpsys` excerpts the maintainer asked for, run on demand | 200 KB |

## What is deliberately absent

| Absent | Why |
|---|---|
| API keys, tokens, or anything that could authenticate | The point of the exercise is diagnosis, not access |
| Prompt text and assistant replies | Your content is yours |
| File contents and diffs | Same |
| Project names and absolute paths | Replaced by a salted hash (`projectHash`), stable within the export so the same project can be recognised, different between exports so two exports cannot be linked |
| Screenshots, camera roll, contacts, location | Never collected in the first place |
| The database itself | Only the derived, redacted text |

If a bug genuinely requires the prompt text to diagnose, the support conversation is where that happens, with the user pasting it deliberately. Not in a diagnostics file that gets attached to a public issue by accident.

## Redaction

Two layers, both mandatory.

**At write time.** Everything in the archive comes from sources that already redact — the log writer, the transparency-log exporter, the settings serialiser. A redacted value never existed in the file being zipped.

**At export time.** A final pass over the assembled archive before the zip is written:

```
scripts/scrub-diagnostics.py
  - Replaces any value matching a key pattern with <redacted>
  - Replaces absolute paths outside the app's own directories with <path>
  - Replaces email addresses with <email>
  - Replaces UUIDs that are not run IDs with <id>
  - Prints a report: N substitutions, by category
  - Fails if a key pattern matched inside an unredacted file → the export is aborted
```

The abort is important. A scrubber that silently produces an incomplete file is worse than one that stops.

## The user-facing summary

Before the share sheet, the user sees:

> **Diagnose-Export**
>
> Enthält: App-Version und Geräteinformationen, redigierte Protokolle, Laufzusammenfassungen, keine API-Schlüssel, keine Nachrichteninhalte, keine Dateien aus deinen Projekten.
> Gesamtgröße: 1,4 MB · 23 Dateien
>
> 12 Platzhalter wurden ersetzt.

Two buttons: *Teilen* and *Abbrechen*. And a link explaining what each item is, in the app, not in a web page.

## Sharing

The export is a `.zip` in the app's cache directory, handed to the share sheet via a `FileProvider` URI with a one-shot read grant. The file is deleted when the share sheet is dismissed or after one hour, whichever comes first. It is never written to shared storage.

The exported file name is `cc-android-diagnostics-<appVersion>-<timestamp>.zip` — no user identifier in the file name, because file names leak through sharing metadata more often than anyone expects.

## Reading one as a maintainer

```bash
unzip cc-android-diagnostics-0.4.0-20260927T101500Z.zip
cat summary.txt
cat environment/checks.json | jq '.[] | select(.status != "pass")'
cat runs/*/summary.json | jq '.verification'
tail -200 logs/app.log
```

The order a maintainer should read it in: `summary.txt`, then the failed health checks, then the failing verification result, then the log. The export is ordered so that the answer to "what happened" is usually in the first two files.

## Correlating exports

Two exports from the same installation can be correlated by a random per-installation identifier generated at first launch, included in `summary.txt`. It identifies an *installation*, not a person, it is not derived from any device identifier, and a reinstall changes it. A user can clear it from Settings, which also clears the ability to correlate anything.

## Sizes

| Situation | Expected size |
|---|---|
| Fresh install, one short run | < 500 KB |
| Active daily use, a week of runs | 3–8 MB |
| Weeks of heavy use | Capped at 25 MB — the oldest run logs are dropped and the summary says so |

The cap is enforced by dropping the oldest run logs and recording in `summary.txt` that N logs were omitted. An export that is silently incomplete is the failure mode this avoids.
