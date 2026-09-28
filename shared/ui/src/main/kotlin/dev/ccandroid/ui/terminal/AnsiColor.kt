package dev.ccandroid.ui.terminal

import androidx.compose.ui.graphics.Color

/**
 * A colour as a program asked for it, not as it is drawn.
 *
 * Keeping the three cases apart is what lets [AnsiPalette] apply one rule —
 * "unreadable is replaced" — to every colour a shell can emit without the
 * renderer needing to know which of them it is looking at.
 */
public sealed interface AnsiColor {
    /** One of the sixteen fixed ANSI assignments, 0 to 15. */
    public data class Ansi(public val index: Int) : AnsiColor

    /** The xterm 256 colour space, 0 to 255. 0 to 15 is the ANSI block. */
    public data class Indexed(public val index: Int) : AnsiColor

    /** A 24-bit colour, taken verbatim. Nothing is substituted for these. */
    public data class Rgb(
        public val red: Int,
        public val green: Int,
        public val blue: Int,
    ) : AnsiColor
}

/**
 * Everything a sequence can set, and nothing that survives a reset.
 *
 * A null colour means "unstyled", not "black". The renderer resolves a null to
 * the terminal default, so [DEFAULT] and [RESET] draw identically while
 * answering different questions: one says "nothing has styled this yet", the
 * other says "the program took the styling back off".
 */
public data class AnsiStyle(
    public val foreground: AnsiColor? = null,
    public val background: AnsiColor? = null,
    public val bold: Boolean = false,
    public val dim: Boolean = false,
    public val italic: Boolean = false,
    public val underline: Boolean = false,
    public val inverse: Boolean = false,
    public val strikethrough: Boolean = false,
) {
    public companion object {
        /**
         * What a stream starts as: the terminal's default text colour, which is
         * ANSI white. An emitted sequence replaces it wholesale.
         */
        public val DEFAULT: AnsiStyle = AnsiStyle(foreground = AnsiColor.Ansi(7))

        /** What SGR 0 produces: no colour chosen, no attribute set. */
        public val RESET: AnsiStyle = AnsiStyle()
    }
}

/**
 * A run of text that shares one style.
 *
 * The parser guarantees that two adjacent spans never carry the same style, so
 * a renderer can draw a span without checking the one before it.
 */
public data class AnsiSpan(
    public val text: String,
    public val style: AnsiStyle,
) {
    /**
     * The colour this span's text is drawn in.
     *
     * [inverse] is a swap rather than a colour, so it is resolved here and not
     * in the palette: an inverse span draws in whatever is behind it. With an
     * explicit background it draws in that background, which is what a program
     * asking for reverse video on top of a colour block expects.
     */
    public fun foregroundColor(defaultBackground: Color): Color {
        if (style.inverse) {
            return style.background?.let { AnsiPalette.foreground(it) } ?: defaultBackground
        }
        return style.foreground?.let { AnsiPalette.foreground(it) } ?: defaultBackground
    }

    /**
     * The colour behind this span, or transparent where there is none.
     *
     * Transparent rather than a nullable return, so a caller painting a
     * background does not branch on every span to avoid painting nothing.
     */
    public fun backgroundColor(): Color {
        style.background?.let { return AnsiPalette.foreground(it) }
        if (style.inverse) {
            style.foreground?.let { return AnsiPalette.foreground(it) }
        }
        return Color.Transparent
    }
}
