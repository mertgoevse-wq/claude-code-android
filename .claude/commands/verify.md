---
description: Run the full quality gate and report honestly
---

# /verify

Run every gate and report the **real** result. No gate is reported as passing
without being run. Reporting a red gate as green is hard block 5.

## The gate

```bash
./gradlew check                                      # lint, detekt, ktlint, all tests
./gradlew :shared:domain:koverVerify                 # >= 90% on shared/domain
./gradlew :shared:data:koverVerify                   # >= 80% on shared/data
python3 tools/check_doc_manifest.py                  # 135 docs, non-stub
python3 tools/check_source_manifest.py               # every listed source file
python3 tools/check_no_android_imports_in_shared.py  # layer purity
bash scripts/check-no-secrets.sh
bash scripts/check-no-analytics.sh
```

## Slower gates — run on request, and before a release

```bash
./gradlew e2eTest                          # the five journeys, on an emulator
./gradlew validatePaparazziDebug            # screenshot baselines
./gradlew :app:connectedAndroidTest         # UI, accessibility, real device
```

## How to report

One table. Every row is a command that was actually executed.

| Gate | Command | Result |
|---|---|---|
| Assemble | `./gradlew assembleDebug` | pass / fail / not run |
| Check | `./gradlew check` | … |
| Coverage | `koverVerify` | … |
| Doc manifest | `check_doc_manifest.py` | … |
| Source manifest | `check_source_manifest.py` | … |
| Layer purity | `check_no_android_imports_in_shared.py` | … |
| Secrets | `check-no-secrets.sh` | … |
| No analytics | `check-no-analytics.sh` | … |
| Hard blocks | `:shared:orchestration:test --tests '*HardBlock*'` | … |

Rules:

- **"Not run" is a valid result.** Pretending is not.
- On a failure, paste the **first** `e:` or `error:` line, not the last.
- Never lower a threshold to make a gate pass. A red coverage gate is fixed
  with a test.
- If a gate cannot run at all (no device, no SDK, no network), say so and name
  what a person must do. Do not mark it passing.
