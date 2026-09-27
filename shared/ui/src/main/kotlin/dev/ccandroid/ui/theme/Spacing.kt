package dev.ccandroid.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A 4 pt base scale, named by step rather than by intent, so that a component's
 * padding is obvious from reading it. Values are binding; see
 * `docs/03-design/design-tokens.md`.
 */
public object CcSpacing {
    public val space1: Dp = 2.dp // icon-to-label inside a chip
    public val space2: Dp = 4.dp // between related inline elements
    public val space3: Dp = 8.dp // inside a button, chip padding
    public val space4: Dp = 12.dp // card internal padding
    public val space5: Dp = 16.dp // between cards, compact screen margin
    public val space6: Dp = 20.dp // card internal, generous
    public val space7: Dp = 24.dp // between sections of a list, medium screen margin
    public val space8: Dp = 32.dp // between major sections, expanded screen margin
    public val space9: Dp = 40.dp // above a page title
    public val space10: Dp = 48.dp // screen vertical rhythm

    /**
     * Minimum touch target. The UI rules require 48 dp for every interactive
     * element regardless of its visual size, so this is a floor a component
     * grows to rather than a suggestion.
     */
    public val minTouchTarget: Dp = 48.dp
}
