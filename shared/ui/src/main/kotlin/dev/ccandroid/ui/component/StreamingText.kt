package dev.ccandroid.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.ccandroid.ui.theme.CcColorScheme
import dev.ccandroid.ui.theme.CcShape
import dev.ccandroid.ui.theme.CcSpacing
import dev.ccandroid.ui.theme.CcTheme

public sealed interface MarkdownBlock {
    public data class Heading(val level: Int, val text: String) : MarkdownBlock
    public data class Paragraph(val text: String) : MarkdownBlock
    public data class BulletList(val items: List<String>) : MarkdownBlock
    public data class NumberedList(val items: List<String>) : MarkdownBlock
    public data class Code(val language: String?, val code: String, val isOpen: Boolean) : MarkdownBlock
}

public sealed interface InlineSegment {
    public data class Plain(val text: String) : InlineSegment
    public data class Bold(val text: String) : InlineSegment
    public data class Italic(val text: String) : InlineSegment
    public data class InlineCode(val code: String) : InlineSegment
    public data class Link(val label: String, val url: String) : InlineSegment
}

public object MarkdownParser {

    public fun parseBlocks(markdown: String, isStreaming: Boolean): List<MarkdownBlock> {
        val lines = markdown.lines()
        val blocks = mutableListOf<MarkdownBlock>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            // Check for code fence
            if (line.trimStart().startsWith("```")) {
                val rawLang = line.trimStart().removePrefix("```").trim()
                val language = if (rawLang.isEmpty()) null else rawLang
                val codeLines = mutableListOf<String>()
                var closed = false
                i++
                while (i < lines.size) {
                    if (lines[i].trimStart().startsWith("```")) {
                        closed = true
                        i++
                        break
                    } else {
                        codeLines.add(lines[i])
                        i++
                    }
                }
                val isOpen = !closed && isStreaming
                blocks.add(MarkdownBlock.Code(language = language, code = codeLines.joinToString("\n"), isOpen = isOpen))
                continue
            }

            // Headings
            if (line.startsWith("#")) {
                val hashes = line.takeWhile { it == '#' }
                val level = hashes.length.coerceIn(1, 6)
                val text = line.removePrefix(hashes).trim()
                blocks.add(MarkdownBlock.Heading(level = level, text = text))
                i++
                continue
            }

            // Bullet list item
            if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
                val bulletItems = mutableListOf<String>()
                while (i < lines.size && (lines[i].trimStart().startsWith("- ") || lines[i].trimStart().startsWith("* "))) {
                    val itemText = lines[i].trimStart().substring(2).trim()
                    bulletItems.add(itemText)
                    i++
                }
                blocks.add(MarkdownBlock.BulletList(bulletItems))
                continue
            }

            // Numbered list item
            val numberedMatch = Regex("^\\s*(\\d+)\\.\\s+(.*)").find(line)
            if (numberedMatch != null) {
                val numberedItems = mutableListOf<String>()
                while (i < lines.size) {
                    val match = Regex("^\\s*(\\d+)\\.\\s+(.*)").find(lines[i])
                    if (match != null) {
                        numberedItems.add(match.groupValues[2])
                        i++
                    } else {
                        break
                    }
                }
                blocks.add(MarkdownBlock.NumberedList(numberedItems))
                continue
            }

            // Empty line
            if (line.isBlank()) {
                i++
                continue
            }

            // Paragraph
            val paraLines = mutableListOf<String>()
            while (i < lines.size && lines[i].isNotBlank() &&
                !lines[i].startsWith("#") &&
                !lines[i].trimStart().startsWith("```") &&
                !lines[i].trimStart().startsWith("- ") &&
                !lines[i].trimStart().startsWith("* ") &&
                Regex("^\\s*\\d+\\.\\s+").find(lines[i]) == null
            ) {
                paraLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.Paragraph(paraLines.joinToString("\n")))
        }

        return blocks
    }

    public fun parseInline(text: String): List<InlineSegment> {
        val segments = mutableListOf<InlineSegment>()
        var index = 0
        val length = text.length

        while (index < length) {
            // Bold **text**
            if (text.startsWith("**", index)) {
                val endIndex = text.indexOf("**", index + 2)
                if (endIndex != -1) {
                    segments.add(InlineSegment.Bold(text.substring(index + 2, endIndex)))
                    index = endIndex + 2
                    continue
                }
            }

            // Inline code `code`
            if (text[index] == '`') {
                val endIndex = text.indexOf('`', index + 1)
                if (endIndex != -1) {
                    segments.add(InlineSegment.InlineCode(text.substring(index + 1, endIndex)))
                    index = endIndex + 1
                    continue
                }
            }

            // Link [label](url)
            if (text[index] == '[') {
                val closeBracket = text.indexOf(']', index + 1)
                if (closeBracket != -1 && closeBracket + 1 < length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val label = text.substring(index + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen)
                        segments.add(InlineSegment.Link(label = label, url = url))
                        index = closeParen + 1
                        continue
                    }
                }
            }

            // Italic *text*
            if (text[index] == '*' && (index == 0 || text[index - 1] != '*')) {
                val endIndex = text.indexOf('*', index + 1)
                if (endIndex != -1 && (endIndex + 1 == length || text[endIndex + 1] != '*')) {
                    segments.add(InlineSegment.Italic(text.substring(index + 1, endIndex)))
                    index = endIndex + 1
                    continue
                }
            }

            // Plain text
            val nextSpecial = listOf(
                text.indexOf("**", index).let { if (it == -1) length else it },
                text.indexOf('`', index).let { if (it == -1) length else it },
                text.indexOf('[', index).let { if (it == -1) length else it },
                text.indexOf('*', index).let { if (it == -1) length else it },
            ).minOrNull() ?: length

            if (nextSpecial > index) {
                segments.add(InlineSegment.Plain(text.substring(index, nextSpecial)))
                index = nextSpecial
            } else {
                segments.add(InlineSegment.Plain(text[index].toString()))
                index++
            }
        }

        return segments
    }

    public fun buildAnnotatedText(text: String, colors: CcColorScheme): AnnotatedString {
        val segments = parseInline(text)
        return buildAnnotatedString {
            segments.forEach { segment ->
                when (segment) {
                    is InlineSegment.Plain -> {
                        append(segment.text)
                    }
                    is InlineSegment.Bold -> {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.textPrimary))
                        append(segment.text)
                        pop()
                    }
                    is InlineSegment.Italic -> {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = colors.textPrimary))
                        append(segment.text)
                        pop()
                    }
                    is InlineSegment.InlineCode -> {
                        pushStyle(
                            SpanStyle(
                                background = colors.surfaceSubtle,
                                color = colors.accentText,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                        append(" ${segment.code} ")
                        pop()
                    }
                    is InlineSegment.Link -> {
                        pushStringAnnotation(tag = "URL", annotation = segment.url)
                        pushStyle(
                            SpanStyle(
                                color = colors.accentText,
                                textDecoration = TextDecoration.Underline,
                            ),
                        )
                        append(segment.label)
                        pop()
                        pop()
                    }
                }
            }
        }
    }
}

/**
 * Streaming markdown text component compliant with `docs/03-design/component-library.md`.
 * Appends deltas without reflowing outer lists, renders markdown blocks and blinking cursor.
 */
@Composable
public fun StreamingText(
    text: String,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    onLinkClick: ((String) -> Unit)? = null,
) {
    val tokens = CcTheme.tokens
    val colors = tokens.colors
    val typography = tokens.typography

    val blocks = remember(text, isStreaming) {
        MarkdownParser.parseBlocks(text, isStreaming)
    }

    val transition = rememberInfiniteTransition(label = "streamingCaret")
    val caretAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "caretAlpha",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        blocks.forEachIndexed { index, block ->
            val isLastBlock = index == blocks.size - 1

            when (block) {
                is MarkdownBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> typography.titleMedium
                        else -> typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = block.text,
                        style = style,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(vertical = CcSpacing.space2),
                    )
                }
                is MarkdownBlock.Paragraph -> {
                    val annotated = remember(block.text, colors) {
                        MarkdownParser.buildAnnotatedText(block.text, colors)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = CcSpacing.space1),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        ClickableText(
                            text = annotated,
                            style = typography.bodyLarge.copy(color = colors.textPrimary),
                            onClick = { offset ->
                                annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        onLinkClick?.invoke(annotation.item)
                                    }
                            },
                        )
                        if (isStreaming && isLastBlock) {
                            Text(
                                text = " ▋",
                                style = typography.bodyLarge,
                                color = colors.accent,
                                modifier = Modifier.alpha(caretAlpha),
                            )
                        }
                    }
                }
                is MarkdownBlock.BulletList -> {
                    Column(modifier = Modifier.padding(vertical = CcSpacing.space2)) {
                        block.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = CcSpacing.space1),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = "• ",
                                    style = typography.bodyLarge,
                                    color = colors.accentText,
                                    modifier = Modifier.padding(end = CcSpacing.space2),
                                )
                                val annotated = remember(item, colors) {
                                    MarkdownParser.buildAnnotatedText(item, colors)
                                }
                                ClickableText(
                                    text = annotated,
                                    style = typography.bodyLarge.copy(color = colors.textPrimary),
                                    onClick = { offset ->
                                        annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                            .firstOrNull()?.let { annotation ->
                                                onLinkClick?.invoke(annotation.item)
                                            }
                                    },
                                )
                            }
                        }
                    }
                }
                is MarkdownBlock.NumberedList -> {
                    Column(modifier = Modifier.padding(vertical = CcSpacing.space2)) {
                        block.items.forEachIndexed { itemIndex, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = CcSpacing.space1),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = "${itemIndex + 1}. ",
                                    style = typography.bodyLarge,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(end = CcSpacing.space2),
                                )
                                val annotated = remember(item, colors) {
                                    MarkdownParser.buildAnnotatedText(item, colors)
                                }
                                ClickableText(
                                    text = annotated,
                                    style = typography.bodyLarge.copy(color = colors.textPrimary),
                                    onClick = { offset ->
                                        annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                            .firstOrNull()?.let { annotation ->
                                                onLinkClick?.invoke(annotation.item)
                                            }
                                    },
                                )
                            }
                        }
                    }
                }
                is MarkdownBlock.Code -> {
                    Box(modifier = Modifier.padding(vertical = CcSpacing.space2)) {
                        CodeBlock(
                            code = block.code,
                            language = block.language,
                            showLineNumbers = false,
                        )
                    }
                }
            }
        }
    }
}
