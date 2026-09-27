---
name: android-compose
description: Compose and Material 3 patterns for this project - tokens over hardcoded values, structured UI state, edge-to-edge, performance rules, and the anti-slop constraints. Use whenever writing or changing a composable, a screen, or a theme.
---

# android-compose

Compose conventions for this repository. These are the rules; the design
decisions live in `docs/03-design/`.

## State

A screen is one immutable `UiState` sealed class and one event stream. No
mutable state in a composable, no state hoisted into a singleton, no
`remember` holding a value that should be in the `ViewModel`.

```kotlin
sealed interface ChatUiState {
    data object Loading : ChatUiState
    data object Empty : ChatUiState
    data class Error(val error: AppError) : ChatUiState
    data class Content(
        val messages: List<Message>,
        val plan: Plan?,
        val cost: CostSnapshot,
        val isRunning: Boolean,
    ) : ChatUiState
}
```

Every screen has all four states. The empty state is designed, not default.

## Tokens, never literals

```kotlin
// Wrong
Text("Done", fontSize = 14.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(8.dp))

// Right
Text(
    text = stringResource(R.string.run_done),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(MaterialTheme.spacing.small),
)
```

A hardcoded colour, size, radius, spacing, or duration in a composable is a
review blocker. If a design needs a token that does not exist, add it in this
order: `docs/03-design/design-tokens.md`, the theme, then the component.

## List performance

- `LazyColumn` with a **stable** `key` for every item. A key derived from
  position defeats the whole point.
- `contentType` per item type, so Compose reuses slots correctly.
- Streaming text must not re-layout the whole list per delta. The streaming
  message is a single item whose text updates; the rest of the list does not
  recompose.
- No `derivedStateOf` around something that recomputes per frame.
- Measure before optimising. A screenshot of a slow list is not evidence.

## Structure

- `Scaffold` at the top of a screen, once. Not nested three deep.
- `TopAppBar` for a title that scrolls away, `CenterAlignedTopAppBar` only when
  the title is the point.
- The bottom tab bar is the app shell, not a screen feature.
- Edge-to-edge by default: use `WindowInsets` from the scaffold, not
  `Modifier.systemBarsPadding()` sprinkled everywhere. One place reads insets.
- `Modifier` order matters: `padding` then `background` then `clickable`, so the
  touch target is the padded size.

## Accessibility, not as an afterthought

- Every `IconButton` gets a `contentDescription`. A decorative `Icon` gets
  `null` and is not a button.
- Touch targets >= 48 dp. `Modifier.minimumInteractiveComponentSize()` or an
  explicit size.
- A status is never colour alone. Glyph plus word, always.
- Custom semantics on the streaming text and the diff viewer: they are
  accessibility-hostile by default.
- Reduce motion: read the system setting and skip the animation, do not shorten
  it.

## Banned

- Default Material purple and default Roboto. The palette and the type
  pairing are ours and documented.
- Card-inside-card-inside-card. Nesting depth one or less.
- Liquid glass, neomorphism, glassmorphism, brutalism — see
  `docs/03-design/anti-slop-rules.md`. Not as a style, not subtly.
- Emoji as icons. One documented icon set.
- Animate everything. State changes under 300 ms; the only infinite animation
  is the logo.
- `!!` in a composable. A null in the UI is a state, not an assertion.
- `LaunchedEffect(Unit)` that fires a network call with no cancellation story.

## Review before moving on

```bash
./gradlew :shared:ui:check
./gradlew validatePaparazziDebug
```

Then look at the screenshot diffs. Then run `/ui-polish` for the design pass and
add the row to `docs/13-process/ai-usage-policy.md`.
