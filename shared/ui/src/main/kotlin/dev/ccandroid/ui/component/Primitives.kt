package dev.ccandroid.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ccandroid.ui.theme.CcMotion
import dev.ccandroid.ui.theme.CcShape
import dev.ccandroid.ui.theme.CcSpacing
import dev.ccandroid.ui.theme.CcTheme

/**
 * Low-level primitive UI components conforming strictly to `docs/03-design/component-library.md`.
 * Every component takes tokens from [CcTheme], never literals.
 */

// ============================================================================
// 1. AppScaffold
// ============================================================================

/**
 * Screen skeleton taking slots. Handles background, layout insets, and top/bottom/floating bars.
 */
@Composable
public fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floating: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val tokens = CcTheme.tokens
    Scaffold(
        modifier = modifier.background(tokens.colors.background),
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floating,
        snackbarHost = snackbarHost,
        containerColor = tokens.colors.background,
        contentColor = tokens.colors.textPrimary,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content(innerPadding)
        }
    }
}

// ============================================================================
// 2. Card
// ============================================================================

/**
 * Surface for related information or actions.
 * Never combines both a border and a shadow.
 * Selected adds a 2 dp accent border, not a background change.
 */
@Composable
public fun Card(
    modifier: Modifier = Modifier,
    surfaceColor: Color = CcTheme.tokens.colors.surface,
    borderColor: Color = CcTheme.tokens.colors.border,
    radius: RoundedCornerShape = CcShape.large,
    padding: Dp = CcSpacing.space5,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    content: @Composable () -> Unit,
) {
    val tokens = CcTheme.tokens
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null && enabled) 0.98f else 1.0f,
        animationSpec = tween(durationMillis = CcMotion.fastMillis),
        label = "cardPressScale",
    )

    val effectiveBorderColor = if (selected) tokens.colors.accent else borderColor
    val effectiveBorderWidth = if (selected) 2.dp else 1.dp

    var boxModifier = modifier
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .clip(radius)
        .background(surfaceColor, radius)
        .border(effectiveBorderWidth, effectiveBorderColor, radius)

    if (onClick != null) {
        boxModifier = boxModifier.clickable(
            interactionSource = interactionSource,
            indication = null, // Custom press scale is used
            enabled = enabled,
            onClick = onClick,
        )
    }

    Box(
        modifier = boxModifier.padding(padding),
        contentAlignment = Alignment.TopStart,
    ) {
        content()
    }
}

// ============================================================================
// 3. Button
// ============================================================================

public enum class ButtonVariant {
    Primary,
    Secondary,
    Tertiary,
    Danger,
}

public enum class ButtonSize(public val height: Dp, public val horizontalPadding: Dp) {
    Small(36.dp, CcSpacing.space3),
    Medium(48.dp, CcSpacing.space5),
    Large(56.dp, CcSpacing.space6),
}

@Composable
public fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    label: @Composable () -> Unit,
) {
    val tokens = CcTheme.tokens
    val colors = tokens.colors

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.97f else 1.0f,
        animationSpec = tween(durationMillis = CcMotion.fastMillis),
        label = "buttonPressScale",
    )

    val containerColor = when (variant) {
        ButtonVariant.Primary -> if (enabled) colors.accent else colors.surfaceSubtle
        ButtonVariant.Secondary -> Color.Transparent
        ButtonVariant.Tertiary -> Color.Transparent
        ButtonVariant.Danger -> if (enabled) colors.danger else colors.surfaceSubtle
    }

    val contentColor = when (variant) {
        ButtonVariant.Primary -> if (enabled) colors.onAccent else colors.textTertiary
        ButtonVariant.Secondary -> if (enabled) colors.textPrimary else colors.textTertiary
        ButtonVariant.Tertiary -> if (enabled) colors.accentText else colors.textTertiary
        ButtonVariant.Danger -> if (enabled) Color.White else colors.textTertiary
    }

    val borderColor = when (variant) {
        ButtonVariant.Secondary -> if (enabled) colors.borderStrong else colors.border
        else -> Color.Transparent
    }

    val shape = CcShape.medium

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .defaultMinSize(minHeight = size.height, minWidth = CcSpacing.minTouchTarget)
            .clip(shape)
            .background(containerColor, shape)
            .then(
                if (variant == ButtonVariant.Secondary) {
                    Modifier.border(1.dp, borderColor, shape)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick,
            )
            .padding(horizontal = size.horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(CcSpacing.space2))
            } else if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(CcSpacing.space2))
            }

            Box(contentAlignment = Alignment.Center) {
                label()
            }

            if (trailingIcon != null && !isLoading) {
                Spacer(modifier = Modifier.width(CcSpacing.space2))
                trailingIcon()
            }
        }
    }
}

// ============================================================================
// 4. TextField
// ============================================================================

@Composable
public fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    placeholder: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val tokens = CcTheme.tokens
    val colors = tokens.colors

    val isError = !errorText.isNullOrEmpty()
    val borderColor = when {
        isError -> colors.danger
        !enabled -> colors.border
        else -> colors.borderStrong
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (!label.isNullOrEmpty()) {
            Text(
                text = label,
                style = tokens.typography.labelMedium,
                color = if (enabled) colors.textPrimary else colors.textTertiary,
                modifier = Modifier.padding(bottom = CcSpacing.space2),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clip(CcShape.medium)
                .background(if (enabled) colors.surface else colors.surfaceSubtle)
                .border(1.dp, borderColor, CcShape.medium)
                .padding(horizontal = CcSpacing.space4, vertical = CcSpacing.space3),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(CcSpacing.space3))
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && !placeholder.isNullOrEmpty()) {
                        Text(
                            text = placeholder,
                            style = tokens.typography.bodyMedium,
                            color = colors.textTertiary,
                        )
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled && !readOnly,
                        readOnly = readOnly,
                        textStyle = tokens.typography.bodyMedium.copy(
                            color = if (enabled) colors.textPrimary else colors.textTertiary,
                        ),
                        singleLine = singleLine,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        cursorBrush = SolidColor(colors.accent),
                    )
                }

                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(CcSpacing.space3))
                    trailingIcon()
                }
            }
        }

        // Helper or Error text with preserved height
        val message = errorText ?: helperText
        if (!message.isNullOrEmpty()) {
            Text(
                text = message,
                style = tokens.typography.bodySmall,
                color = if (isError) colors.danger else colors.textSecondary,
                modifier = Modifier.padding(top = CcSpacing.space1, start = CcSpacing.space1),
            )
        }
    }
}

// ============================================================================
// 5. Chip
// ============================================================================

public enum class ChipType {
    Filter,
    Status,
    Tag,
}

@Composable
public fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    type: ChipType = ChipType.Filter,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    statusColor: Color? = null,
) {
    val tokens = CcTheme.tokens
    val colors = tokens.colors

    val containerColor = when (type) {
        ChipType.Filter -> if (selected) colors.accentSubtle else colors.surface
        ChipType.Status -> statusColor?.copy(alpha = 0.15f) ?: colors.surfaceSubtle
        ChipType.Tag -> colors.surfaceSubtle
    }

    val contentColor = when (type) {
        ChipType.Filter -> if (selected) colors.accentText else colors.textPrimary
        ChipType.Status -> statusColor ?: colors.textPrimary
        ChipType.Tag -> colors.textPrimary
    }

    val borderColor = when (type) {
        ChipType.Filter -> if (selected) colors.accent else colors.border
        ChipType.Status -> statusColor ?: colors.border
        ChipType.Tag -> colors.border
    }

    val shape = CcShape.small

    var chipModifier = modifier
        .height(32.dp)
        .clip(shape)
        .background(containerColor, shape)
        .border(1.dp, borderColor, shape)
        .padding(horizontal = CcSpacing.space3)

    if (onClick != null) {
        chipModifier = chipModifier.clickable(onClick = onClick)
    }

    Row(
        modifier = chipModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(CcSpacing.space1))
        } else if (type == ChipType.Filter && selected) {
            Text(
                text = "✓",
                style = tokens.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
            )
            Spacer(modifier = Modifier.width(CcSpacing.space1))
        }

        Text(
            text = text,
            style = tokens.typography.labelMedium,
            color = contentColor,
        )

        if (type == ChipType.Tag && onRemove != null) {
            Spacer(modifier = Modifier.width(CcSpacing.space2))
            Text(
                text = "✕",
                style = tokens.typography.labelSmall,
                color = colors.textSecondary,
                modifier = Modifier.clickable(onClick = onRemove),
            )
        }
    }
}

// ============================================================================
// 6. StatusBadge
// ============================================================================

public enum class StatusState(
    public val glyph: String,
    public val labelEn: String,
    public val labelDe: String,
) {
    Verified("✓", "Verified", "Geprüft"),
    Unverified("?", "Unverified", "Ungeprüft"),
    Failed("✕", "Failed", "Fehlgeschlagen"),
    Refused("⊘", "Blocked", "Blockiert"),
    Running("◌", "Running", "Läuft"),
    Retrying("↻", "Attempt", "Versuch"),
    Committed("◆", "Committed", "Gesichert"),
    Pushed("↑", "Pushed", "Hochgeladen"),
    Interrupted("‖", "Interrupted", "Unterbrochen"),
}

@Composable
public fun StatusBadge(
    state: StatusState,
    modifier: Modifier = Modifier,
    useGerman: Boolean = false,
    retryCount: Int? = null,
) {
    val tokens = CcTheme.tokens
    val colors = tokens.colors

    val statusColor = when (state) {
        StatusState.Verified, StatusState.Committed, StatusState.Pushed -> colors.success
        StatusState.Unverified, StatusState.Retrying -> colors.warning
        StatusState.Failed, StatusState.Refused -> colors.danger
        StatusState.Running -> colors.accent
        StatusState.Interrupted -> colors.info
    }

    val labelText = when {
        state == StatusState.Retrying && retryCount != null ->
            if (useGerman) "${state.labelDe} $retryCount" else "${state.labelEn} $retryCount"
        useGerman -> state.labelDe
        else -> state.labelEn
    }

    Row(
        modifier = modifier
            .clip(CcShape.small)
            .background(statusColor.copy(alpha = 0.12f), CcShape.small)
            .border(1.dp, statusColor.copy(alpha = 0.4f), CcShape.small)
            .padding(horizontal = CcSpacing.space2, vertical = CcSpacing.space1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = state.glyph,
            style = tokens.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = statusColor,
        )
        Spacer(modifier = Modifier.width(CcSpacing.space1))
        Text(
            text = labelText,
            style = tokens.typography.labelSmall,
            color = statusColor,
        )
    }
}

// ============================================================================
// 7. Sheet & Dialog
// ============================================================================

/**
 * Bottom Sheet container following the spec:
 * Top grabber handle (32 dp wide, 4 dp tall), rounded top corners (24 dp),
 * surface background, and clean scrollable content.
 */
@Composable
public fun SheetContent(
    modifier: Modifier = Modifier,
    title: String? = null,
    actionRow: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val tokens = CcTheme.tokens
    val colors = tokens.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CcShape.sheet)
            .background(colors.surface, CcShape.sheet)
            .padding(top = CcSpacing.space3, bottom = CcSpacing.space5),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Grabber handle
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 4.dp)
                .clip(CcShape.full)
                .background(colors.borderStrong),
        )

        Spacer(modifier = Modifier.height(CcSpacing.space4))

        if (!title.isNullOrEmpty()) {
            Text(
                text = title,
                style = tokens.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CcSpacing.space5, vertical = CcSpacing.space2),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CcSpacing.space5),
        ) {
            content()
        }

        if (actionRow != null) {
            Spacer(modifier = Modifier.height(CcSpacing.space4))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CcSpacing.space5),
            ) {
                actionRow()
            }
        }
    }
}

/**
 * Confirmation dialog for binary decisions with a consequence.
 * Exactly two options, centered, clean elevation.
 */
@Composable
public fun DialogContent(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false,
) {
    val tokens = CcTheme.tokens
    val colors = tokens.colors

    Card(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = colors.surface,
        borderColor = colors.border,
        radius = CcShape.large,
        padding = CcSpacing.space6,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = tokens.typography.titleMedium,
                color = colors.textPrimary,
            )

            Spacer(modifier = Modifier.height(CcSpacing.space3))

            Text(
                text = message,
                style = tokens.typography.bodyMedium,
                color = colors.textSecondary,
            )

            Spacer(modifier = Modifier.height(CcSpacing.space6))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onDismiss,
                    variant = ButtonVariant.Tertiary,
                    size = ButtonSize.Small,
                    label = { Text(dismissLabel) },
                )

                Spacer(modifier = Modifier.width(CcSpacing.space3))

                Button(
                    onClick = onConfirm,
                    variant = if (isDanger) ButtonVariant.Danger else ButtonVariant.Primary,
                    size = ButtonSize.Small,
                    label = { Text(confirmLabel) },
                )
            }
        }
    }
}
