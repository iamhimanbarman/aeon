package com.aeon.app.ui.components.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.theme.AeonChipTokens
import com.aeon.app.ui.theme.AeonComponentMotion
import com.aeon.app.ui.theme.AeonMotionAlpha
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM CHIP SYSTEM
 *
 * Purpose:
 * Premium compact selection/action chips for Aeon.
 *
 * Use for:
 * - Goal filters
 * - Mood tags
 * - Habit categories
 * - AI prompt chips
 * - Insight filters
 * - Focus mode selectors
 * - Time period selectors
 * - Status badges
 *
 * Senior UI/UX Rule:
 * Chips should be compact, scannable, and calm.
 * Do not make them too colorful unless they communicate state or category.
 */


// ----------------------------------------------------
// Chip Variants
// ----------------------------------------------------

enum class AeonChipVariant {
    Default,
    Tonal,
    Outline,
    Ghost,
    Filled,
    Premium,
    Success,
    Warning,
    Danger,
    Info
}


// ----------------------------------------------------
// Chip Sizes
// ----------------------------------------------------

enum class AeonChipSize {
    Compact,
    Medium,
    Large
}


// ----------------------------------------------------
// Resolved Token
// ----------------------------------------------------

@Immutable
private data class AeonResolvedChipToken(
    val height: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val iconSize: Dp,
    val iconGap: Dp,
    val shape: Shape,
    val textStyle: TextStyle
)


// ----------------------------------------------------
// Main Aeon Chip
// ----------------------------------------------------

@Composable
fun AeonChip(
    text: String,
    modifier: Modifier = Modifier,
    variant: AeonChipVariant = AeonChipVariant.Default,
    size: AeonChipSize = AeonChipSize.Medium,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    AeonChipSurface(
        modifier = modifier,
        variant = variant,
        size = size,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        AeonChipContent(
            text = text,
            size = size,
            selected = selected,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon
        )
    }
}


// ----------------------------------------------------
// Custom Content Chip
// ----------------------------------------------------

@Composable
fun AeonChip(
    modifier: Modifier = Modifier,
    variant: AeonChipVariant = AeonChipVariant.Default,
    size: AeonChipSize = AeonChipSize.Medium,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    AeonChipSurface(
        modifier = modifier,
        variant = variant,
        size = size,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        interactionSource = interactionSource,
        content = content
    )
}


// ----------------------------------------------------
// Selectable Chip
// ----------------------------------------------------

@Composable
fun AeonSelectableChip(
    text: String,
    selected: Boolean,
    onSelectedChange: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AeonChipVariant = AeonChipVariant.Tonal,
    size: AeonChipSize = AeonChipSize.Medium,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    AeonChip(
        text = text,
        modifier = modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onSelectedChange
        ),
        variant = variant,
        size = size,
        selected = selected,
        enabled = enabled,
        onClick = null,
        leadingIcon = leadingIcon
    )
}


// ----------------------------------------------------
// Premium Status Chip
// ----------------------------------------------------

@Composable
fun AeonStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    variant: AeonChipVariant = AeonChipVariant.Success,
    size: AeonChipSize = AeonChipSize.Compact,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    AeonChip(
        text = text,
        modifier = modifier,
        variant = variant,
        size = size,
        selected = false,
        enabled = true,
        onClick = null,
        leadingIcon = leadingIcon
    )
}


// ----------------------------------------------------
// Chip Surface
// ----------------------------------------------------

@Composable
private fun AeonChipSurface(
    modifier: Modifier,
    variant: AeonChipVariant,
    size: AeonChipSize,
    selected: Boolean,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    interactionSource: MutableInteractionSource,
    content: @Composable RowScope.() -> Unit
) {
    val token = aeonChipToken(size)
    val colors = aeonChipColors(
        variant = variant,
        selected = selected,
        enabled = enabled
    )

    val border = aeonChipBorder(
        variant = variant,
        selected = selected,
        enabled = enabled
    )

    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isPressed -> AeonChipTokens.PressedScale
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = AeonComponentMotion.ChipSelectDuration
        ),
        label = "aeon_chip_press_scale"
    )

    val containerColor by animateColorAsState(
        targetValue = colors.container,
        animationSpec = tween(
            durationMillis = AeonComponentMotion.ChipSelectDuration
        ),
        label = "aeon_chip_container_color"
    )

    val contentColor by animateColorAsState(
        targetValue = colors.content,
        animationSpec = tween(
            durationMillis = AeonComponentMotion.ChipSelectDuration
        ),
        label = "aeon_chip_content_color"
    )

    val chipModifier = modifier
        .scale(scale)
        .defaultMinSize(minHeight = token.height)

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = chipModifier,
            enabled = enabled,
            shape = token.shape,
            color = containerColor,
            contentColor = contentColor,
            border = border,
            interactionSource = interactionSource
        ) {
            AeonChipInnerContent(
                token = token,
                contentColor = contentColor,
                content = content
            )
        }
    } else {
        Surface(
            modifier = chipModifier,
            shape = token.shape,
            color = containerColor,
            contentColor = contentColor,
            border = border
        ) {
            AeonChipInnerContent(
                token = token,
                contentColor = contentColor,
                content = content
            )
        }
    }
}


// ----------------------------------------------------
// Chip Inner Content
// ----------------------------------------------------

@Composable
private fun AeonChipInnerContent(
    token: AeonResolvedChipToken,
    contentColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor
    ) {
        ProvideTextStyle(value = token.textStyle) {
            Row(
                modifier = Modifier.padding(
                    horizontal = token.horizontalPadding,
                    vertical = token.verticalPadding
                ),
                horizontalArrangement = Arrangement.spacedBy(
                    token.iconGap,
                    Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}


// ----------------------------------------------------
// Chip Text Content
// ----------------------------------------------------

@Composable
private fun AeonChipContent(
    text: String,
    size: AeonChipSize,
    selected: Boolean,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?
) {
    val token = aeonChipToken(size)

    AnimatedVisibility(
        visible = leadingIcon != null,
        enter = fadeIn(
            animationSpec = tween(AeonComponentMotion.ChipSelectDuration)
        ),
        exit = fadeOut(
            animationSpec = tween(AeonComponentMotion.ChipSelectDuration)
        )
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier.size(token.iconSize),
                contentAlignment = Alignment.Center
            ) {
                leadingIcon()
            }
        }
    }

    Text(
        text = text,
        style = token.textStyle,
        color = LocalContentColor.current
    )

    AnimatedVisibility(
        visible = selected || trailingIcon != null,
        enter = fadeIn(
            animationSpec = tween(AeonComponentMotion.ChipSelectDuration)
        ),
        exit = fadeOut(
            animationSpec = tween(AeonComponentMotion.ChipSelectDuration)
        )
    ) {
        if (trailingIcon != null) {
            Box(
                modifier = Modifier.size(token.iconSize),
                contentAlignment = Alignment.Center
            ) {
                trailingIcon()
            }
        }
    }
}


// ----------------------------------------------------
// Token Resolver
// ----------------------------------------------------

private fun aeonChipToken(
    size: AeonChipSize
): AeonResolvedChipToken {
    return when (size) {
        AeonChipSize.Compact -> AeonResolvedChipToken(
            height = AeonChipTokens.CompactHeight,
            horizontalPadding = AeonChipTokens.CompactHorizontalPadding,
            verticalPadding = AeonChipTokens.CompactVerticalPadding,
            iconSize = AeonChipTokens.IconSize,
            iconGap = AeonChipTokens.IconTextGap,
            shape = AeonChipTokens.Shape,
            textStyle = AeonTextStyles.Micro
        )

        AeonChipSize.Medium -> AeonResolvedChipToken(
            height = AeonChipTokens.Height,
            horizontalPadding = AeonChipTokens.HorizontalPadding,
            verticalPadding = AeonChipTokens.VerticalPadding,
            iconSize = AeonChipTokens.IconSize,
            iconGap = AeonChipTokens.IconTextGap,
            shape = AeonChipTokens.Shape,
            textStyle = AeonTextStyles.Caption
        )

        AeonChipSize.Large -> AeonResolvedChipToken(
            height = AeonChipTokens.LargeHeight,
            horizontalPadding = AeonChipTokens.HorizontalPadding + 2.dp,
            verticalPadding = AeonChipTokens.VerticalPadding,
            iconSize = AeonChipTokens.IconSize + 2.dp,
            iconGap = AeonChipTokens.IconTextGap,
            shape = AeonChipTokens.Shape,
            textStyle = AeonTextStyles.ButtonMedium
        )
    }
}


// ----------------------------------------------------
// Color Model
// ----------------------------------------------------

@Immutable
private data class AeonChipColorToken(
    val container: Color,
    val content: Color
)


// ----------------------------------------------------
// Color Resolver
// ----------------------------------------------------

@Composable
private fun aeonChipColors(
    variant: AeonChipVariant,
    selected: Boolean,
    enabled: Boolean
): AeonChipColorToken {
    val colors = AeonThemeTokens.colors
    val disabledAlpha = if (enabled) 1f else AeonMotionAlpha.Disabled

    val selectedAlpha = if (colors.isDark) 0.22f else 0.12f
    val softAlpha = if (colors.isDark) 0.16f else 0.10f

    val accent = when (variant) {
        AeonChipVariant.Default -> colors.textSecondary
        AeonChipVariant.Tonal -> colors.brand
        AeonChipVariant.Outline -> colors.brand
        AeonChipVariant.Ghost -> colors.textSecondary
        AeonChipVariant.Filled -> colors.brand
        AeonChipVariant.Premium -> colors.premiumGold
        AeonChipVariant.Success -> colors.success
        AeonChipVariant.Warning -> colors.warning
        AeonChipVariant.Danger -> colors.error
        AeonChipVariant.Info -> colors.info
    }

    return when (variant) {
        AeonChipVariant.Default -> AeonChipColorToken(
            container = if (selected) {
                colors.brand.copy(alpha = selectedAlpha)
            } else {
                colors.surfaceMuted
            },
            content = if (selected) {
                colors.brand.copy(alpha = disabledAlpha)
            } else {
                colors.textSecondary.copy(alpha = disabledAlpha)
            }
        )

        AeonChipVariant.Tonal -> AeonChipColorToken(
            container = accent.copy(alpha = if (selected) selectedAlpha else softAlpha),
            content = accent.copy(alpha = disabledAlpha)
        )

        AeonChipVariant.Outline -> AeonChipColorToken(
            container = if (selected) {
                accent.copy(alpha = selectedAlpha)
            } else {
                Color.Transparent
            },
            content = accent.copy(alpha = disabledAlpha)
        )

        AeonChipVariant.Ghost -> AeonChipColorToken(
            container = if (selected) {
                accent.copy(alpha = selectedAlpha)
            } else {
                Color.Transparent
            },
            content = accent.copy(alpha = disabledAlpha)
        )

        AeonChipVariant.Filled -> AeonChipColorToken(
            container = if (selected) {
                colors.brand
            } else {
                colors.surfaceElevated
            }.copy(alpha = disabledAlpha),
            content = if (selected) {
                Color.White
            } else {
                colors.textPrimary
            }.copy(alpha = disabledAlpha)
        )

        AeonChipVariant.Premium -> AeonChipColorToken(
            container = accent.copy(alpha = if (selected) 1f else softAlpha),
            content = if (selected) {
                Color(0xFF111318).copy(alpha = disabledAlpha)
            } else {
                accent.copy(alpha = disabledAlpha)
            }
        )

        AeonChipVariant.Success,
        AeonChipVariant.Warning,
        AeonChipVariant.Danger,
        AeonChipVariant.Info -> AeonChipColorToken(
            container = accent.copy(alpha = if (selected) 0.24f else softAlpha),
            content = accent.copy(alpha = disabledAlpha)
        )
    }
}


// ----------------------------------------------------
// Border Resolver
// ----------------------------------------------------

@Composable
private fun aeonChipBorder(
    variant: AeonChipVariant,
    selected: Boolean,
    enabled: Boolean
): BorderStroke? {
    val colors = AeonThemeTokens.colors
    val alpha = if (enabled) 1f else AeonMotionAlpha.Disabled

    val accent = when (variant) {
        AeonChipVariant.Premium -> colors.premiumGold
        AeonChipVariant.Success -> colors.success
        AeonChipVariant.Warning -> colors.warning
        AeonChipVariant.Danger -> colors.error
        AeonChipVariant.Info -> colors.info
        else -> colors.brand
    }

    return when (variant) {
        AeonChipVariant.Outline -> BorderStroke(
            width = 1.dp,
            color = accent.copy(alpha = if (selected) 0.70f * alpha else 0.34f * alpha)
        )

        AeonChipVariant.Ghost -> BorderStroke(
            width = 1.dp,
            color = colors.borderSoft.copy(alpha = alpha)
        )

        AeonChipVariant.Default -> BorderStroke(
            width = 1.dp,
            color = colors.borderSoft.copy(alpha = alpha)
        )

        AeonChipVariant.Tonal,
        AeonChipVariant.Premium,
        AeonChipVariant.Success,
        AeonChipVariant.Warning,
        AeonChipVariant.Danger,
        AeonChipVariant.Info -> BorderStroke(
            width = 1.dp,
            color = accent.copy(alpha = if (selected) 0.34f * alpha else 0.18f * alpha)
        )

        AeonChipVariant.Filled -> null
    }
}
