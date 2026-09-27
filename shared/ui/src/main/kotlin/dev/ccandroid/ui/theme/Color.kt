package dev.ccandroid.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colour tokens. The single source of truth is `docs/03-design/design-tokens.md`;
 * every value here is transcribed from it, and every ratio quoted in the doc was
 * measured, never chosen by eye.
 *
 * A composable may not contain a colour literal. If a colour is missing it is
 * added to the document with a measured ratio first, then here — not the
 * other way round.
 */

/** Semantic colours that differ between the light and dark themes. */
public data class CcColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentText: Color,
    val accentSubtle: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val codeBackground: Color,
    val diffAddBackground: Color,
    val diffDelBackground: Color,
    val focusRing: Color,
)

/**
 * The terminal does not follow the theme, in either direction.
 *
 * An ANSI palette has fixed assignments — "white" is light, "black" is dark — so
 * a light terminal background makes the light ANSI colours invisible (white on
 * light is 1.04:1). Inverting the palette yields colours a shell does not expect.
 * Both themes therefore render the terminal on [terminalBackground] with the same
 * dark palette. This is deliberate and it is visible in the screenshots.
 */
public data class CcTerminalPalette(
    val black: Color,
    val red: Color,
    val green: Color,
    val yellow: Color,
    val blue: Color,
    val magenta: Color,
    val cyan: Color,
    val white: Color,
    val brightBlack: Color,
    val brightRed: Color,
    val brightGreen: Color,
    val brightYellow: Color,
    val brightBlue: Color,
    val brightMagenta: Color,
    val brightCyan: Color,
    val brightWhite: Color,
)

/**
 * The eight classes the syntax highlighter actually emits. A language with more
 * classes maps onto these; a language needing more than these is left
 * unhighlighted rather than highlighted badly.
 */
public data class CcSyntaxPalette(
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val type: Color,
    val function: Color,
    val punctuation: Color,
    val plain: Color,
)

/** Diff colours. Meaning is never carried by colour alone — see the gutter sign. */
public data class CcDiffPalette(
    val addBackground: Color,
    val addText: Color,
    val addMarker: Color,
    val delBackground: Color,
    val delText: Color,
    val delMarker: Color,
    val hunkHeader: Color,
)

public object CcColors {

    public val light: CcColorScheme = CcColorScheme(
        background = Color(0xFFFAF9F5),
        surface = Color(0xFFFFFFFF),
        surfaceSubtle = Color(0xFFF2F0EA),
        border = Color(0xFFE3E0D8),
        borderStrong = Color(0xFF948E82), // 3.09:1 — the boundary of an interactive control
        textPrimary = Color(0xFF1F1E1B), // 16.67:1
        textSecondary = Color(0xFF5C5A54), // 6.90:1
        textTertiary = Color(0xFF6E6B65), // 5.31:1
        accent = Color(0xFFB25133), // 5.10:1 with onAccent
        accentText = Color(0xFFA84C2E), // 5.33:1 — text on light surfaces
        accentSubtle = Color(0xFFF5E6E0),
        onAccent = Color(0xFFFFFFFF),
        success = Color(0xFF3F7D58),
        warning = Color(0xFF9A6B15),
        danger = Color(0xFFB23A2F),
        info = Color(0xFF3A6EA5),
        codeBackground = Color(0xFFF6F4EE),
        diffAddBackground = Color(0xFFE4F0E6),
        diffDelBackground = Color(0xFFF7E4E1),
        focusRing = Color(0xFF8F3F24), // 6.86:1
    )

    public val dark: CcColorScheme = CcColorScheme(
        background = Color(0xFF1A1917),
        surface = Color(0xFF232220),
        surfaceSubtle = Color(0xFF2C2A27),
        border = Color(0xFF3A3835),
        borderStrong = Color(0xFF74716D), // 3.27:1
        textPrimary = Color(0xFFF2F0EA), // 13.95:1
        textSecondary = Color(0xFFB4B0A8), // 7.35:1
        textTertiary = Color(0xFF99958F), // 5.34:1
        accent = Color(0xFFE08A66), // 6.05:1 with onAccent
        accentText = Color(0xFFE59572), // 6.69:1
        accentSubtle = Color(0xFF3A2A24),
        // Dark, not white. White on this accent is 2.63:1 and would fail AA.
        onAccent = Color(0xFF1A1917),
        success = Color(0xFF6FBE8C),
        warning = Color(0xFFD9A94A),
        danger = Color(0xFFE8796B),
        info = Color(0xFF79AEE0),
        codeBackground = Color(0xFF1F1F1C),
        diffAddBackground = Color(0xFF213026),
        diffDelBackground = Color(0xFF3A2724),
        focusRing = Color(0xFFE59572), // 7.42:1
    )

    /** Always dark. See the note on [CcTerminalPalette]. */
    public val terminalBackground: Color = Color(0xFF1F1F1C)

    /**
     * The ANSI palette, measured against [terminalBackground].
     *
     * The palette this replaces was transcribed from the *light* theme's
     * semantic colours and never re-measured against a dark background: 14 of
     * its 16 entries fell below AA, `green` at 2.60:1. These values are
     * derived from the dark theme's own measured hues instead, and every entry
     * except `black` now clears 4.5:1. ANSI assignments are still fixed, which
     * is what makes a terminal a terminal.
     *
     * `black` is the deliberate exception: ANSI black on a dark terminal is
     * inherently near-invisible, and every real terminal has the same property.
     * It is a fixed assignment, not an oversight, and the renderer is expected
     * to fall back to [white] when it would be unreadable.
     */
    public val terminal: CcTerminalPalette = CcTerminalPalette(
        black = Color(0xFF1F1E1B), // 1.01:1 — see the note above
        red = Color(0xFFE8796B), // 5.81:1
        green = Color(0xFF6FBE8C), // 7.41:1
        yellow = Color(0xFFD9A94A), // 7.65:1
        blue = Color(0xFF79AEE0), // 7.04:1
        magenta = Color(0xFFD79AD2), // 7.43:1
        cyan = Color(0xFF7FD4D8), // 9.69:1
        white = Color(0xFFD8D5CD), // 11.27:1
        brightBlack = Color(0xFF9C9891), // 5.75:1
        brightRed = Color(0xFFF29A8E), // 7.70:1
        brightGreen = Color(0xFF8FD3A6), // 9.47:1
        brightYellow = Color(0xFFE8C46B), // 9.87:1
        brightBlue = Color(0xFF9CC3E8), // 8.96:1
        brightMagenta = Color(0xFFE6B6E2), // 9.56:1
        brightCyan = Color(0xFF9FE2E6), // 11.40:1
        brightWhite = Color(0xFFF2F0EA), // 14.50:1
    )

    public val syntaxLight: CcSyntaxPalette = CcSyntaxPalette(
        keyword = Color(0xFF8F3F24),
        string = Color(0xFF2F6B45),
        number = Color(0xFF2F5C96),
        comment = Color(0xFF6E6B65),
        type = Color(0xFF8A4380),
        function = Color(0xFF1F6A6E),
        punctuation = Color(0xFF5C5A54),
        plain = Color(0xFF1F1E1B),
    )

    public val syntaxDark: CcSyntaxPalette = CcSyntaxPalette(
        keyword = Color(0xFFE59572),
        string = Color(0xFF6FBE8C),
        number = Color(0xFF79AEE0),
        comment = Color(0xFF99958F),
        type = Color(0xFFA05296),
        function = Color(0xFF79AEE0),
        punctuation = Color(0xFFB4B0A8),
        plain = Color(0xFFF2F0EA),
    )

    public val diffLight: CcDiffPalette = CcDiffPalette(
        addBackground = Color(0xFFE4F0E6),
        addText = Color(0xFF1F1E1B),
        addMarker = Color(0xFF3F7D58),
        delBackground = Color(0xFFF7E4E1),
        delText = Color(0xFF1F1E1B),
        delMarker = Color(0xFFB23A2F),
        hunkHeader = Color(0xFFF2F0EA),
    )

    public val diffDark: CcDiffPalette = CcDiffPalette(
        addBackground = Color(0xFF213026),
        addText = Color(0xFFF2F0EA),
        addMarker = Color(0xFF6FBE8C),
        delBackground = Color(0xFF3A2724),
        delText = Color(0xFFF2F0EA),
        delMarker = Color(0xFFE8796B),
        hunkHeader = Color(0xFF2C2A27),
    )
}
