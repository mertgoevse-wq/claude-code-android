package dev.ccandroid.ui.terminal

import androidx.compose.ui.graphics.Color
import dev.ccandroid.ui.theme.CcColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The colours a terminal actually draws, and the one rule applied to them.
 *
 * A shell can ask for a colour that is invisible on this background — ANSI
 * black is the famous one, but a program that emits a 256-colour index picks
 * up the whole cube and the greyscale ramp too. So the rule is not a special
 * case for black: any computed colour that fails contrast is replaced by the
 * default text colour. What survives is a log you can read.
 */
public object AnsiPalette {

    /** The contrast a terminal's own text has to clear. 3:1 for minimum readability against dark background. */
    private const val MINIMUM_CONTRAST = 3.0

    private val NAMES = listOf(
        "black", "red", "green", "yellow", "blue", "magenta", "cyan", "white",
        "brightBlack", "brightRed", "brightGreen", "brightYellow",
        "brightBlue", "brightMagenta", "brightCyan", "brightWhite",
    )

    /**
     * The sixteen fixed assignments, in ANSI order.
     *
     * The single source of truth for both this object and [Ansi256]: two copies
     * of the same sixteen colours is how the palette and the cube end up
     * disagreeing about what index 3 means.
     */
    internal val FIXED: List<Color> = listOf(
        CcColors.terminal.black, CcColors.terminal.red,
        CcColors.terminal.green, CcColors.terminal.yellow,
        CcColors.terminal.blue, CcColors.terminal.magenta,
        CcColors.terminal.cyan, CcColors.terminal.white,
        CcColors.terminal.brightBlack, CcColors.terminal.brightRed,
        CcColors.terminal.brightGreen, CcColors.terminal.brightYellow,
        CcColors.terminal.brightBlue, CcColors.terminal.brightMagenta,
        CcColors.terminal.brightCyan, CcColors.terminal.brightWhite,
    )

    /** The token name of an ANSI index, for a test message or a legend. */
    public fun name(index: Int): String {
        require(index in NAMES.indices) { "ANSI index out of range: $index" }
        return NAMES[index]
    }

    /**
     * The colour to draw [color] as a foreground.
     *
     * The sixteen fixed assignments are returned as they are, including black:
     * substituting them would make a program that means "black" show up as
     * "white", and the renderer cannot tell the two apart at the call site. The
     * computed colours, which the program did not choose deliberately, are the
     * ones that get replaced.
     */
    public fun foreground(color: AnsiColor): Color = when (color) {
        is AnsiColor.Ansi -> fixed(color.index)
        is AnsiColor.Indexed ->
            if (color.index in NAMES.indices) {
                fixed(color.index)
            } else {
                computed(color.index)
            }
        is AnsiColor.Rgb -> Color(color.red, color.green, color.blue)
    }

    /** The colour to draw [color] as a background. Same rule as [foreground]. */
    public fun background(color: AnsiColor): Color = foreground(color)

    /**
     * [candidate] if it can be read on [background], otherwise the default
     * text colour.
     *
     * Public because a caller rendering a colour the parser never saw — a
     * diff marker, a link, a colour from a tool's own output — has to be able
     * to ask the same question the palette asks.
     */
    public fun readableOn(candidate: Color, background: Color): Color {
        if (contrast(candidate, background) >= MINIMUM_CONTRAST) {
            return candidate
        }
        val onLight = fixed(0)
        val onDark = fixed(7)
        return if (contrast(onDark, background) >= contrast(onLight, background)) onDark else onLight
    }

    private fun computed(index: Int): Color {
        val argb = Ansi256.argbOrNull(index) ?: return fixed(7)
        return readableOn(Color(argb), CcColors.terminalBackground)
    }

    private fun fixed(index: Int): Color {
        require(index in FIXED.indices) { "ANSI index out of range: $index" }
        return FIXED[index]
    }

    private fun channel(value: Float): Double {
        val c = value.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)

    internal fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}
