package dev.ccandroid.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ccandroid.ui.theme.CcShape
import dev.ccandroid.ui.theme.CcSpacing
import dev.ccandroid.ui.theme.CcSyntaxPalette
import dev.ccandroid.ui.theme.CcTheme

/**
 * Syntax token types for basic code syntax highlighting in [CodeBlock].
 */
public enum class SyntaxTokenType {
    Keyword,
    StringLiteral,
    Comment,
    Number,
    Function,
    Type,
    Operator,
    Punctuation,
    Plain,
}

public data class SyntaxToken(
    val text: String,
    val type: SyntaxTokenType,
)

/**
 * Lightweight tokenizer for code blocks to avoid heavy dependencies while
 * supporting Kotlin, Shell, JSON, Python, etc.
 */
public object CodeBlockTokenizer {

    private val KOTLIN_KEYWORDS = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if",
        "in", "interface", "is", "null", "object", "package", "return", "super", "this",
        "throw", "true", "try", "typealias", "val", "var", "when", "while", "data",
        "sealed", "override", "private", "protected", "public", "internal", "import",
    )

    private val SHELL_KEYWORDS = setOf(
        "if", "then", "else", "elif", "fi", "case", "esac", "for", "while", "until",
        "do", "done", "in", "function", "select", "time", "export", "set", "unset",
    )

    private val COMMON_KEYWORDS = setOf(
        "def", "import", "from", "as", "class", "return", "if", "else", "elif",
        "try", "except", "finally", "with", "yield", "async", "await", "const", "let",
    )

    public fun normalizeLanguage(language: String?): String {
        return when (language?.trim()?.lowercase()) {
            "kt", "kotlin" -> "KOTLIN"
            "sh", "bash", "zsh", "shell" -> "BASH"
            "json" -> "JSON"
            "py", "python" -> "PYTHON"
            "js", "javascript" -> "JAVASCRIPT"
            "ts", "typescript" -> "TYPESCRIPT"
            "java" -> "JAVA"
            "xml", "html" -> "XML"
            "md", "markdown" -> "MARKDOWN"
            null, "" -> "TEXT"
            else -> language.trim().uppercase()
        }
    }

    public fun tokenize(code: String, language: String?): List<SyntaxToken> {
        val keywords = when (language?.trim()?.lowercase()) {
            "kt", "kotlin" -> KOTLIN_KEYWORDS
            "sh", "bash", "shell" -> SHELL_KEYWORDS
            else -> COMMON_KEYWORDS
        }

        val tokens = mutableListOf<SyntaxToken>()
        var index = 0
        val length = code.length

        while (index < length) {
            val char = code[index]

            // Comments
            if (char == '/' && index + 1 < length && code[index + 1] == '/') {
                val endOfLine = code.indexOf('\n', index).let { if (it == -1) length else it }
                tokens.add(SyntaxToken(code.substring(index, endOfLine), SyntaxTokenType.Comment))
                index = endOfLine
                continue
            }
            if (char == '#' && (language?.contains("sh") == true || language?.contains("py") == true)) {
                val endOfLine = code.indexOf('\n', index).let { if (it == -1) length else it }
                tokens.add(SyntaxToken(code.substring(index, endOfLine), SyntaxTokenType.Comment))
                index = endOfLine
                continue
            }

            // String literals
            if (char == '"' || char == '\'') {
                val quote = char
                val stringEnd = code.indexOf(quote, index + 1).let { if (it == -1) length else it + 1 }
                tokens.add(SyntaxToken(code.substring(index, stringEnd), SyntaxTokenType.StringLiteral))
                index = stringEnd
                continue
            }

            // Numbers
            if (char.isDigit()) {
                val numEnd = (index until length).firstOrNull { !code[it].isDigit() && code[it] != '.' } ?: length
                tokens.add(SyntaxToken(code.substring(index, numEnd), SyntaxTokenType.Number))
                index = numEnd
                continue
            }

            // Words / Identifiers
            if (char.isLetter() || char == '_') {
                val wordEnd = (index until length).firstOrNull { !code[it].isLetterOrDigit() && code[it] != '_' } ?: length
                val word = code.substring(index, wordEnd)
                val type = if (word in keywords) SyntaxTokenType.Keyword else SyntaxTokenType.Plain
                tokens.add(SyntaxToken(word, type))
                index = wordEnd
                continue
            }

            // Punctuation and whitespace
            tokens.add(SyntaxToken(char.toString(), SyntaxTokenType.Plain))
            index++
        }

        return tokens
    }

    public fun buildAnnotatedCode(
        code: String,
        language: String?,
        syntax: CcSyntaxPalette,
        textPrimary: Color,
    ): AnnotatedString {
        val tokens = tokenize(code, language)
        return buildAnnotatedString {
            tokens.forEach { token ->
                val color = when (token.type) {
                    SyntaxTokenType.Keyword -> syntax.keyword
                    SyntaxTokenType.StringLiteral -> syntax.string
                    SyntaxTokenType.Comment -> syntax.comment
                    SyntaxTokenType.Number -> syntax.number
                    SyntaxTokenType.Function -> syntax.function
                    SyntaxTokenType.Type -> syntax.type
                    SyntaxTokenType.Operator -> syntax.punctuation
                    SyntaxTokenType.Punctuation -> syntax.punctuation
                    SyntaxTokenType.Plain -> textPrimary
                }
                val style = if (token.type == SyntaxTokenType.Keyword) {
                    SpanStyle(color = color, fontWeight = FontWeight.SemiBold)
                } else {
                    SpanStyle(color = color)
                }
                pushStyle(style)
                append(token.text)
                pop()
            }
        }
    }
}

/**
 * Fenced code block adhering to `docs/03-design/component-library.md`.
 * Monospace text in JetBrains Mono with language tag and accessible copy button.
 */
@Composable
public fun CodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    language: String? = null,
    showLineNumbers: Boolean = false,
    onCopyClick: (() -> Unit)? = null,
) {
    val tokens = CcTheme.tokens
    val colors = tokens.colors
    val typography = tokens.typography
    val syntax = tokens.syntax
    val scrollState = rememberScrollState()
    var isCopied by remember { mutableStateOf(false) }

    val normalizedLang = remember(language) { CodeBlockTokenizer.normalizeLanguage(language) }
    val annotatedCode = remember(code, language, syntax, colors.textPrimary) {
        CodeBlockTokenizer.buildAnnotatedCode(code, language, syntax, colors.textPrimary)
    }

    val lines = remember(code) { code.split("\n") }
    val lineCount = lines.size
    val gutterWidth = remember(lineCount) { (lineCount.toString().length * 10 + 16).dp }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CcShape.medium)
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.border, CcShape.medium),
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = CcSpacing.space4, vertical = CcSpacing.space2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = normalizedLang,
                style = typography.monoSmall,
                color = colors.textTertiary,
                fontWeight = FontWeight.Medium,
            )

            if (onCopyClick != null) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                onCopyClick()
                                isCopied = true
                            },
                        )
                        .padding(horizontal = CcSpacing.space2, vertical = CcSpacing.space1),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isCopied) "Kopiert ✓" else "Kopieren",
                        style = typography.labelSmall,
                        color = if (isCopied) colors.success else colors.accentText,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Code content row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = CcSpacing.space3),
        ) {
            if (showLineNumbers) {
                Column(
                    modifier = Modifier
                        .width(gutterWidth)
                        .padding(end = CcSpacing.space2),
                    horizontalAlignment = Alignment.End,
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            text = i.toString(),
                            style = typography.monoSmall,
                            color = colors.textTertiary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.height(20.dp),
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
                    .padding(horizontal = CcSpacing.space3),
            ) {
                Text(
                    text = annotatedCode,
                    style = typography.monoSmall,
                    color = colors.textPrimary,
                    lineHeight = typography.monoSmall.lineHeight,
                )
            }
        }
    }
}
