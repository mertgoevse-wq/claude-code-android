package dev.ccandroid.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ccandroid.ui.theme.CcMotion
import dev.ccandroid.ui.theme.CcShape
import dev.ccandroid.ui.theme.CcSpacing
import dev.ccandroid.ui.theme.CcTheme

public enum class ToolCardState(public val glyph: String) {
    Pending("○"),
    Running("⟐"),
    Done("✓"),
    Error("✕"),
    Denied("⊘"),
}

public object ToolCardUtils {
    public const val MAX_TARGET_CHARS: Int = 52
    public const val MAX_OUTPUT_BYTES: Int = 2048

    public fun truncateTarget(target: String): String {
        if (target.length <= MAX_TARGET_CHARS) return target
        val prefixLen = 24
        val suffixLen = 27
        return "${target.take(prefixLen)}…${target.takeLast(suffixLen)}"
    }

    public fun truncateOutput(output: String): Pair<String, Boolean> {
        val bytes = output.toByteArray(Charsets.UTF_8)
        if (bytes.size <= MAX_OUTPUT_BYTES) {
            return Pair(output, false)
        }
        val truncatedString = String(bytes, 0, MAX_OUTPUT_BYTES, Charsets.UTF_8)
        return Pair(truncatedString, true)
    }
}

/**
 * Collapsible activity card conforming to `docs/03-design/component-library.md`.
 */
@Composable
public fun ToolCard(
    title: String,
    state: ToolCardState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    target: String? = null,
    durationText: String? = null,
    isDestructive: Boolean = false,
    nestingLevel: Int = 0,
    expandedContent: (@Composable () -> Unit)? = null,
) {
    val tokens = CcTheme.tokens
    val colors = tokens.colors
    val typography = tokens.typography

    val stateColor = when (state) {
        ToolCardState.Pending -> colors.textTertiary
        ToolCardState.Running -> colors.accent
        ToolCardState.Done -> colors.success
        ToolCardState.Error, ToolCardState.Denied -> colors.danger
    }

    val truncatedTarget = target?.let { ToolCardUtils.truncateTarget(it) }

    val infiniteTransition = rememberInfiniteTransition(label = "toolCardRunning")
    val travellingProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "travellingProgress",
    )

    val indentPadding = (nestingLevel * 16).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = indentPadding)
            .clip(CcShape.medium)
            .background(colors.surface)
            .border(1.dp, if (isDestructive) colors.danger else colors.border, CcShape.medium),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable(
                    role = Role.Button,
                    onClick = onToggleExpand,
                )
                .padding(horizontal = CcSpacing.space4, vertical = CcSpacing.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Text(
                    text = state.glyph,
                    style = typography.labelMedium,
                    color = stateColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = CcSpacing.space2),
                )

                Text(
                    text = title,
                    style = typography.labelMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (truncatedTarget != null) {
                    Text(
                        text = " · ",
                        style = typography.labelMedium,
                        color = colors.textTertiary,
                    )
                    Text(
                        text = truncatedTarget,
                        style = typography.monoSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = CcSpacing.space2),
            ) {
                if (isDestructive) {
                    Text(
                        text = "⚠️ Nicht umkehrbar",
                        style = typography.labelSmall,
                        color = colors.danger,
                        modifier = Modifier.padding(end = CcSpacing.space2),
                    )
                }

                if (durationText != null) {
                    Text(
                        text = durationText,
                        style = typography.monoSmall,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(end = CcSpacing.space2),
                    )
                }

                Text(
                    text = if (isExpanded) "▲" else "▼",
                    style = typography.labelSmall,
                    color = colors.textTertiary,
                )
            }
        }

        // Running accent bar under header
        if (state == ToolCardState.Running) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(colors.surfaceSubtle),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(2.dp)
                        .align(Alignment.CenterStart)
                        .background(colors.accent),
                )
            }
        }

        // Expanded content
        AnimatedVisibility(
            visible = isExpanded && expandedContent != null,
            enter = expandVertically(animationSpec = tween(CcMotion.standardMillis)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(CcMotion.fastMillis)) + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceSubtle)
                    .border(
                        width = 1.dp,
                        color = colors.border,
                        shape = RectangleShape,
                    )
                    .padding(CcSpacing.space4),
            ) {
                expandedContent?.invoke()
            }
        }
    }
}
