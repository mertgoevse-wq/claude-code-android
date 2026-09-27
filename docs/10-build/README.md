# Build and tooling

How this project is built, pinned, analysed, signed, and run locally.

| File | For |
|---|---|
| `gradle-setup.md` | The toolchain, the wrapper, settings, properties, configuration cache |
| `dependency-versions.md` | Every dependency with its version and reason, and the update policy |
| `convention-plugins.md` | The rules a module cannot forget, and the ones only a test can enforce |
| `build-variants.md` | The five variants, their application IDs, and the keep rules |
| `signing-and-keystores.md` | The key hierarchy, the release pipeline, and fingerprint verification |
| `static-analysis.md` | Lint, detekt, the custom rules, and what is deliberately not automated |
| `local-build-and-run.md` | The exact commands, and what to do when one fails |

## The short version

- One version catalog, one wrapper, one JDK version. No exceptions.
- A module cannot forget a rule, because the rule is in the convention plugin it must apply to exist.
- `HARD_BLOCKS_ENABLED` is `true` in every variant, and a test asserts it.
- The configuration cache is on, so no build script reads the environment.
- Release signing happens in a pipeline that verifies the certificate fingerprint against a committed allowlist.
- The rules a static analyser cannot express are shell checks in `10-build/static-analysis.md`, not review comments.
