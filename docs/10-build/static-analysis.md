# Static analysis

Everything the build refuses to proceed without. Each check exists because it catches a class of mistake that a test does not.

## The tools

| Tool | Catches | Runs |
|---|---|---|
| Android Lint | API-level mistakes, resource mistakes, accessibility defaults, Compose mistakes | Every build |
| detekt (+ `detekt-formatting`) | Kotlin complexity, smells, and our custom project rules | Every build |
| ktlint via Spotless | Formatting | Every build |
| `dependency-analysis` | Unused and mis-scoped dependencies | Every build; transitives weekly |
| A custom secret scan | Credentials in the repository | Every build |
| A custom layer check | Illegal module dependencies | Every build |
| A custom hard-block check | The five safety rules living anywhere but the executor | Every build |

## Android Lint

Enabled checks are the defaults plus a curated list. The baseline file is committed and **only shrinks** — a new lint warning is a build failure, an existing one is a documented debt with a ticket number next to it.

```xml
<!-- app/lint.xml -->
<lint>
    <issue id="HardcodedText"         severity="error" />
    <issue id="Overdraw"              severity="warning" />
    <issue id="UnusedResources"       severity="error" />
    <issue id="ContentDescription"    severity="error" />
    <issue id="LabelFor"              severity="error" />
    <issue id="TouchTargetSizeCheck"  severity="error" />
    <issue id="TextFields"            severity="error" />
    <issue id="Autofill"              severity="warning" />
    <issue id="MonochromeLauncherIcon" severity="warning" />
    <issue id="GradleDependency"      severity="error" />
    <issue id="AndroidGradlePluginVersion" severity="error" />
    <issue id="ObsoleteSdkInt"        severity="error" />
    <issue id="NewApi"                severity="error" />
    <issue id="MissingPermission"     severity="error" />
</lint>
```

`HardcodedText` as an error is the practical teeth behind the localisation requirement (`02-architecture/adr-log.md`, precomputed localized strings). It is not about German; it is about the fact that a hardcoded string is a string that cannot be changed without a build.

`NewApi` as an error is what keeps minSdk 26 honest. Version checks in code must be lint-detectable (`@RequiresApi`, `Build.VERSION.SDK_INT` in the lint-recognised form), not `if (Build.VERSION.SDK_INT >= 30)` buried in a helper.

## detekt

### The rules that matter here

| Rule | Setting | Why |
|---|---|---|
| `LongMethod` | 60 lines | A 200-line function is a design problem, not a style problem |
| `TooManyFunctions` | 12 per class | Classes that do two things split into two classes |
| `ComplexMethod` | 12 | Same |
| `NestedBlockDepth` | 4 | Deep nesting is where a hard block could hide |
| `ReturnCount` | 3 | Early return beats a flag variable |
| `ThrowsCount` | 4 | |
| `MagicNumber` | active, exceptions listed | Spacing and timing values must come from tokens |
| `ForbiddenComment` | TODO/FIXME forbidden | A TODO without a ticket is a lie. The message must be `// ticket: CC-123`. |
| `MaxLineLength` | 120 | |
| `SwallowedException` | error, allow-list narrow | An empty `catch` is a bug, not a style choice |
| `InstanceOfCheckForException` | error | |
| `UseDataClass` | warning | |
| `UnnecessaryAbstractClass` | error | |
| `GlobalCoroutineUsage` | error | Never `GlobalScope` — see `02-architecture/concurrency-model.md` |

`ForbiddenComment` is unusual and deliberate. A `TODO` without a reference is a comment that will outlive the person who wrote it, describing work nobody is tracking. A `TODO` with a ticket can be audited.

### MagicNumber with exceptions

`MagicNumber` is enabled with a list of allowed contexts: array indices, obvious bit flags, and test assertions. Timing and spacing constants are *not* exempt — a `300L` delay in production code must be `MotionTokens.slow`, which is a token someone can change in one place.

### The custom detekt rules

Three rules live in `build-logic/src/main/kotlin/dev/ccandroid/lint`:

#### `NoMaterialAppearance`

Reports any reference to a `MaterialTheme.colorScheme.*` or `MaterialTheme.typography.*` outside `:core:designsystem`. A hardcoded `#B25133` in a feature module is the same violation with worse taste.

Message: `Use DesignTokens.colors. instead of MaterialTheme colours; the app's palette is defined in 03-design/color-and-contrast.md.`

#### `NoEmojiInStrings`

Reports any string literal containing a character above the Basic Multilingual Plane, or a variation selector, in `src/main`. Emoji in a product are a design-system failure, and finding them in a diff is far easier than finding them in a screenshot review.

Message: `No emoji. Use an icon from the vendored Tabler set — see 03-design/illustration-set.md.`

#### `NoSwipeToDismiss`

Reports `SwipeToDismiss`, `DismissableState`, and `Modifier.pointerInput` with a horizontal drag handler in `src/main`. Nothing a user authored may be dismissible by a gesture without an explicit confirmation, and a swipe is not explicit.

Message: `Swipe-to-delete is prohibited. Offer an explicit action in a menu or a detail screen, confirmed where destructive.`

## The custom shell checks

Three checks are easier to write as scripts than as linters, and they cover the rules that matter most.

### `scripts/check-layer-boundaries.sh`

Walks the Kotlin sources, builds the module import graph, and asserts it matches `02-architecture/layer-contracts.md`:

| Rule | Error message |
|---|---|
| `:feature:*` must not import another `:feature:*` | `Features communicate through navigation, not through each other.` |
| `:core:model` must import nothing but `:core:common` and the JDK | `The model is the bottom of the graph.` |
| `:core:permissions` must not import `:core:database` or `:core:agent` | `The permission evaluator is pure; if it needs the database, the design is wrong.` |
| `:core:verification` must not import `:core:agent` | `Verification judges output; it does not know who produced it.` |
| `:core:agent` must not import `androidx.compose.*` | `The agent layer is not UI.` |
| No module may import `kotlinx.coroutines.GlobalScope` | |

### `scripts/check-hard-blocks.sh`

Asserts that the five hard-block rules from `08-orchestration/permissions.md` are enforced in `:core:permissions`, and that the refusal logic is not duplicated or weakened anywhere else:

```
scripts/check-hard-blocks.sh
  ✓ HARD_BLOCK_DELETION is evaluated only in PermissionEvaluator
  ✓ No module other than :core:permissions constructs a HardBlock
  ✓ No build variant sets HARD_BLOCKS_ENABLED to false
  ✓ No code path catches and discards a PermissionDenied result
  ✓ The "never weaken a check" rule is present in the retry policy
  ✓ No file in src/main contains a bypass flag
```

The fourth and sixth lines matter most. A `catch (e: PermissionDenied) { }` anywhere in `src/main` is a defect, and a `if (BuildConfig.DEBUG) skipChecks()` is a defect wearing a disguise.

### `scripts/check-no-secrets.sh`

The pattern list from `09-testing/test-data-safety.md`, run over the working tree and, nightly, over the git history.

## Baseline files

| Tool | Baseline | Policy |
|---|---|---|
| Lint | `app/lint-baseline.xml` | Committed, may shrink, may not grow |
| detekt | none | No baseline. A new finding is fixed or the rule is changed deliberately in a pull request that says why. |
| ktlint | none | Formatting is never baselined. |

The detekt no-baseline policy is strict on purpose. A detekt baseline is where technical debt goes to be forgotten, and this repository is small enough that the findings are all worth fixing.

## Running it

```bash
./gradlew check            # everything: lint, detekt, spotless, all tests
./gradlew lint             # Android Lint only
./gradlew detekt           # static analysis only
./gradlew spotlessCheck    # formatting only
./gradlew spotlessApply    # fix formatting
./gradlew fixDeps          # remove unused dependencies, then verify

./scripts/check-layer-boundaries.sh
./scripts/check-hard-blocks.sh
./scripts/check-no-secrets.sh
```

In CI, `check` runs with `-Dorg.gradle.warning.mode=fail` and `--configuration-cache-problems=fail`, so a new deprecation warning from a dependency is as blocking as a lint error.

## What is deliberately not automated

**Visual taste.** Whether a screen looks good is a review question, not a lint question. The golden images catch *change*; they cannot tell you the original was right. `03-design/anti-slop-rules.md` is a checklist applied by a human, and `12-delivery/github-readme-guide.md` is explicit that the README is judged the same way.
