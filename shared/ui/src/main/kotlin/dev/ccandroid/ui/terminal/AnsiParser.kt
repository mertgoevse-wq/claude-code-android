package dev.ccandroid.ui.terminal

/**
 * Turns a byte stream into styled spans.
 *
 * This is a rendering block, not a terminal emulator, and the difference is
 * the whole design. An emulator applies what it understands and drops the rest,
 * because a log is a log and a user's `clear` is their business. Here the
 * opposite is required: no input may lose a character. A sequence this parser
 * does not understand is rendered as it arrived, because a build log that
 * silently drops a third of its bytes is worse than one that shows a stray
 * `ESC[2J` in the margin.
 */
public object AnsiParser {

    // Spelled out rather than typed literally: an invisible character in a
    // source file is invisible in a diff too, so a lost ESC would read as a
    // test of escape parsing while testing nothing.
    private const val ESC = '\u001B'
    private const val CSI = '['

    /** Parameter and intermediate bytes of a control sequence. */
    private const val PARAMETER_LAST = '?'

    /** The byte that ends a control sequence, and names what it does. */
    private const val FINAL_FIRST = '@'
    private const val FINAL_LAST = '~'

    /**
     * Parse [input] into spans, in order, with no character lost.
     *
     * Adjacent spans never share a style, so a renderer can draw each span
     * without consulting its neighbour. Pure: the same input always gives the
     * same list, and nothing is retained between calls.
     */
    public fun parse(input: String): List<AnsiSpan> {
        if (input.isEmpty()) return emptyList()

        val spans = mutableListOf<AnsiSpan>()
        val pending = StringBuilder()
        var style = AnsiStyle.DEFAULT
        var index = 0

        while (index < input.length) {
            val char = input[index]
            if (char != ESC) {
                pending.append(char)
                index++
                continue
            }

            val end = sequenceEnd(input, index)
            if (end < 0) {
                // An unterminated sequence: the stream was cut mid-sequence.
                // Render what arrived rather than losing it to a truncated read.
                pending.append(input, index, input.length)
                break
            }

            if (input[index + 1] != CSI) {
                // ESC followed by anything else is not a control sequence we
                // implement. A cursor-position report echoed back to the
                // program, for instance, arrives here and must stay visible.
                pending.append(input, index, end)
            } else if (input[end - 1] == 'm') {
                val newStyle = applySgr(style, input.substring(index + 2, end - 1)) ?: style
                if (newStyle != style) {
                    if (pending.isNotEmpty()) {
                        spans.add(AnsiSpan(pending.toString(), style))
                        pending.clear()
                    }
                    style = newStyle
                }
            } else {
                // A complete sequence that is not SGR: a cursor move, an erase,
                // a device report. Applying it is an emulator's job and this
                // block is not one, so it is shown rather than swallowed.
                pending.append(input, index, end)
            }
            index = end
        }

        if (pending.isNotEmpty()) spans.add(AnsiSpan(pending.toString(), style))
        return merge(spans)
    }

    /**
     * The index just past this sequence, or -1 if it never ends.
     *
     * A sequence ends at the first final byte. Parameter bytes are consumed
     * while looking for it, which is what makes a truncated stream detectable
     * rather than mistaken for text.
     */
    private fun sequenceEnd(input: String, start: Int): Int {
        var index = start + 1
        if (index >= input.length) return -1
        if (input[index] != CSI) return index + 1
        index++
        while (index < input.length && input[index] <= PARAMETER_LAST) index++
        return if (index < input.length && input[index] in FINAL_FIRST..FINAL_LAST) {
            index + 1
        } else {
            -1
        }
    }

    /**
     * The style after applying one SGR parameter list, or null if it is
     * malformed and must be ignored whole.
     *
     * A malformed list is not a partial application. Half-reading `38;2;` and
     * setting a red channel of zero would be worse than ignoring the line: the
     * program asked for something it did not finish saying.
     */
    private fun applySgr(style: AnsiStyle, params: String): AnsiStyle? {
        if (params.isEmpty()) return AnsiStyle.RESET

        val numbers = params.split(';').map { it.toIntOrNull() ?: return null }
        var result = if (style == AnsiStyle.DEFAULT &&
            numbers.any { it in 40..47 || it in 100..107 || it == 48 } &&
            numbers.none { it in 30..37 || it in 90..97 || it == 38 }
        ) {
            style.copy(foreground = null)
        } else {
            style
        }
        var index = 0
        while (index < numbers.size) {
            when (val code = numbers[index]) {
                0 -> result = AnsiStyle.RESET
                1 -> result = result.copy(bold = true)
                2 -> result = result.copy(dim = true)
                3 -> result = result.copy(italic = true)
                4 -> result = result.copy(underline = true)
                7 -> result = result.copy(inverse = true)
                9 -> result = result.copy(strikethrough = true)
                21, 22 -> result = result.copy(bold = false, dim = false)
                23 -> result = result.copy(italic = false)
                24 -> result = result.copy(underline = false)
                27 -> result = result.copy(inverse = false)
                29 -> result = result.copy(strikethrough = false)
                38, 48 -> {
                    val consumed = extended(result, numbers, index, code == 38) ?: return null
                    result = consumed.first
                    index += consumed.second
                }
                in 30..37 -> result = result.copy(foreground = AnsiColor.Ansi(code - 30))
                39 -> result = result.copy(foreground = null)
                in 40..47 -> result = result.copy(background = AnsiColor.Ansi(code - 40))
                49 -> result = result.copy(background = null)
                in 90..97 -> result = result.copy(foreground = AnsiColor.Ansi(code - 90 + 8))
                in 100..107 -> result = result.copy(background = AnsiColor.Ansi(code - 100 + 8))
                // An unknown parameter changes nothing. Programs and shell
                // integrations emit codes this app has no business acting on,
                // and refusing to render their output over one would be a
                // worse failure than ignoring the code.
                else -> Unit
            }
            index++
        }
        return result
    }

    /**
     * Apply a 38 or 48 extended-colour argument, returning the new style and
     * how many further parameters it consumed, or null if malformed.
     *
     * An out-of-range channel or index is ignored rather than clamped, so the
     * style that was already in force is what gets drawn.
     */
    private fun extended(
        style: AnsiStyle,
        numbers: List<Int>,
        start: Int,
        foreground: Boolean,
    ): Pair<AnsiStyle, Int>? {
        val mode = numbers.getOrNull(start + 1) ?: return null
        return when (mode) {
            5 -> {
                val index = numbers.getOrNull(start + 2) ?: return null
                if (index in 0..255) {
                    val color = AnsiColor.Indexed(index)
                    val updated =
                        if (foreground) style.copy(foreground = color) else style.copy(background = color)
                    updated to 2
                } else {
                    style to 2
                }
            }
            2 -> {
                val red = numbers.getOrNull(start + 2) ?: return null
                val green = numbers.getOrNull(start + 3) ?: return null
                val blue = numbers.getOrNull(start + 4) ?: return null
                if (red in 0..255 && green in 0..255 && blue in 0..255) {
                    val color = AnsiColor.Rgb(red, green, blue)
                    val updated =
                        if (foreground) style.copy(foreground = color) else style.copy(background = color)
                    updated to 4
                } else {
                    style to 4
                }
            }
            else -> null
        }
    }

    /**
     * Fuse neighbours that share a style.
     *
     * A program that re-states the colour it is already using must not split a
     * word in two, and a renderer that has to compare each span with the one
     * before it to avoid a visible seam is a renderer with a bug in it.
     */
    private fun merge(spans: List<AnsiSpan>): List<AnsiSpan> {
        if (spans.size < 2) return spans
        val merged = mutableListOf(spans.first())
        for (span in spans.drop(1)) {
            val previous = merged.last()
            if (previous.style == span.style) {
                merged[merged.lastIndex] = previous.copy(text = previous.text + span.text)
            } else {
                merged.add(span)
            }
        }
        return merged
    }
}
