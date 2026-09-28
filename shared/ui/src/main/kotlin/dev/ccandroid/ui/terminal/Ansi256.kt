package dev.ccandroid.ui.terminal

import androidx.compose.ui.graphics.Color

/**
 * The xterm 256 colour space, as arithmetic.
 *
 * This is the raw computation and nothing else: no contrast rule, no
 * substitution, no theme. It answers "what did the program ask for", so that
 * [AnsiPalette] can separately answer "what should be drawn", which is a
 * different question and belongs in a different place.
 */
public object Ansi256 {

    /**
     * The six levels of the colour cube's axes.
     *
     * Not a linear ramp between 0 and 255. xterm uses these six specific
     * values, and a cube built on a straight interpolation is off by up to 40
     * per channel — visible as a band across any gradient a program draws.
     */
    private val CUBE_LEVELS = intArrayOf(0, 95, 135, 175, 215, 255)

    /** The 6x6x6 cube occupies 16 to 231. */
    private const val CUBE_START = 16
    private const val CUBE_END = 231

    /** The greyscale ramp occupies 232 to 255, 24 steps of ten plus eight. */
    private const val GREY_START = 232
    private const val GREY_END = 255
    private const val GREY_BASE = 8
    private const val GREY_STEP = 10

    /** The channel value for one axis of the cube, where [axis] is 0 to 5. */
    public fun cubeLevel(axis: Int): Int {
        require(axis in CUBE_LEVELS.indices) { "cube axis out of range: $axis" }
        return CUBE_LEVELS[axis]
    }

    /** The greyscale level for a ramp index, where [index] is 232 to 255. */
    public fun greyLevel(index: Int): Int = GREY_BASE + GREY_STEP * (index - GREY_START)

    /**
     * The raw ARGB for an index, or null if the index is not in the space.
     *
     * Rejected rather than clamped on purpose: 300 is a corrupt byte, and
     * clamping it to 255 would draw a colour the program never asked for
     * instead of leaving the previous style alone.
     */
    public fun argbOrNull(index: Int): Int? = when {
        index in 0..15 -> argbOf(AnsiPalette.FIXED[index])
        index in CUBE_START..CUBE_END -> {
            val cell = index - CUBE_START
            val red = cubeLevel(cell / 36)
            val green = cubeLevel(cell % 36 / 6)
            val blue = cubeLevel(cell % 6)
            argbOf(red, green, blue)
        }
        index in GREY_START..GREY_END -> {
            val level = greyLevel(index)
            argbOf(level, level, level)
        }
        else -> null
    }

    /** The raw ARGB for an index. Fails loudly on an index outside the space. */
    public fun argb(index: Int): Int =
        requireNotNull(argbOrNull(index)) { "index outside the 256 colour space: $index" }

    private fun argbOf(color: Color): Int = argbOf(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
    )

    private fun argbOf(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}
