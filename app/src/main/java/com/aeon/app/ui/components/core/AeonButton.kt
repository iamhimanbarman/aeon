package com.aeon.app.ui.components.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.theme.AeonButtonTokens
import com.aeon.app.ui.theme.AeonComponentMotion
import com.aeon.app.ui.theme.AeonMotionAlpha
import com.aeon.app.ui.theme.AeonProgressTokens
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM BUTTON SYSTEM
 *
 * Purpose:
 * Reusable premium button foundation for Aeon.
 *
 * Senior UI/UX Rule:
 * Buttons must feel decisive, calm, and touch-friendly.
 * Never use random height, radius, padding, typography, alpha, or colors.
 *
 * Use:
 * - Primary for main action
 * - Secondary for supporting action
 * - Tonal for soft action
 * - Ghost for quiet action
 * - Premium for life score / achievement CTA
 * - Danger only for destructive actions
 */


// ----------------------------------------------------
// Button Variants
// ----------------------------------------------------

enum class AeonButtonVariant {
    Primary,
    Secondary,
    Tonal,
    Ghost,
    Premium,
    Success,
    Danger
}


// ----------------------------------------------------
// Button Sizes
// ----------------------------------------------------

enum class AeonButtonSize {
    Small,
    Medium,
    Large,
    Pill
}


// ----------------------------------------------------
// Resolved Button Token
// ----------------------------------------------------

@Immutable
data class AeonResolvedButtonToken(
    val height: Dp,
    val minWidth: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val iconSize: Dp,
    val iconGap: Dp,
    val shape: Shape,
    val textStyle: TextStyle
)


// ----------------------------------------------------
// Main Text Button API
// ----------------------------------------------------

@Composable
fun AeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AeonButtonVariant = AeonButtonVariant.Primary,
    size: AeonButtonSize = AeonButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    AeonButtonSurface(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        loading = loading,
        fullWidth = fullWidth,
        interactionSource = interactionSource
    ) {
        AeonButtonContent(
            text = text,
            variant = variant,
            size = size,
            loading = loading,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon
        )
    }
}


// ----------------------------------------------------
// Custom Content Button API
// Use when button content is not only text.
// ----------------------------------------------------

@Composable
fun AeonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AeonButtonVariant = AeonButtonVariant.Primary,
    size: AeonButtonSize = AeonButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    AeonButtonSurface(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        loading = loading,
        fullWidth = fullWidth,
        interactionSource = interactionSource,
        content = content
    )
}


// ----------------------------------------------------
// Button Surface
// ----------------------------------------------------

@Composable
private fun AeonButtonSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    variant: AeonButtonVariant,
    size: AeonButtonSize,
    enabled: Boolean,
    loading: Boolean,
    fullWidth: Boolean,
    interactionSource: MutableInteractionSource,
    content: @Composable RowScope.() -> Unit
) {
    val token = aeonButtonToken(size)
    val colors = aeonButtonColors(variant = variant, enabled = enabled)
    val border = aeonButtonBorder(variant = variant, enabled = enabled)

    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleTarget = when {
        !enabled -> 1f
        loading -> 1f
        isPressed -> AeonButtonTokens.PressedScale
        else -> AeonButtonTokens.DefaultScale
    }

    val scale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = tween(
            durationMillis = AeonComponentMotion.ButtonPressDuration
        ),
        label = "aeon_button_press_scale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .semantics { role = Role.Button }
            .scale(scale)
            .heightIn(min = token.height)
            .then(
                if (fullWidth) {
                    Modifier.defaultMinSize(
                        minWidth = Dp.Infinity,
                        minHeight = token.height
                    )
                } else {
                    Modifier.defaultMinSize(
                        minWidth = token.minWidth,
                        minHeight = token.height
                    )
                }
            ),
        enabled = enabled && !loading,
        shape = token.shape,
        color = colors.container,
        contentColor = colors.content,
        tonalElevation = colors.tonalElevation,
        shadowElevation = colors.shadowElevation,
        border = border,
        interactionSource = interactionSource
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colors.content
        ) {
            ProvideTextStyle(value = token.textStyle) {
                Row(
                    modifier = Modifier.padding(
                        PaddingValues(
                            horizontal = token.horizontalPadding,
                            vertical = token.verticalPadding
                        )
                    ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}


// ----------------------------------------------------
// Text Button Content
// ----------------------------------------------------

@Composable
private fun AeonButtonContent(
    text: String,
    variant: AeonButtonVariant,
    size: AeonButtonSize,
    loading: Boolean,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?
) {
    val token = aeonButtonToken(size)
    val contentColor = LocalContentColor.current

    Box(
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(
                animationSpec = tween(AeonComponentMotion.ButtonReleaseDuration)
            ),
            exit = fadeOut(
                animationSpec = tween(AeonComponentMotion.ButtonPressDuration)
            )
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(AeonProgressTokens.CircularSizeSmall),
                color = contentColor,
                strokeWidth = AeonProgressTokens.StrokeWidthSmall
            )
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier.size(token.iconSize),
                    contentAlignment = Alignment.Center
                ) {
                    leadingIcon()
                }
            }

            if (leadingIcon != null) {
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.size(token.iconGap)
                )
            }

            Text(
                text = text,
                style = token.textStyle,
                color = if (loading) {
                    Color.Transparent
                } else {
                    contentColor
                }
            )

            if (trailingIcon != null) {
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.size(token.iconGap)
                )
            }

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
}


// ----------------------------------------------------
// Button Color Model
// ----------------------------------------------------

@Immutable
private data class AeonButtonColorToken(
    val container: Color,
    val content: Color,
    val tonalElevation: Dp,
    val shadowElevation: Dp
)


// ----------------------------------------------------
// Token Resolver
// ----------------------------------------------------

@Composable
private fun aeonButtonToken(
    size: AeonButtonSize
): AeonResolvedButtonToken {
    return when (size) {
        AeonButtonSize.Small -> AeonResolvedButtonToken(
            height = AeonButtonTokens.Small.height,
            minWidth = AeonButtonTokens.Small.minWidth,
            horizontalPadding = AeonButtonTokens.Small.horizontalPadding,
            verticalPadding = AeonButtonTokens.Small.verticalPadding,
            iconSize = AeonButtonTokens.Small.iconSize,
            iconGap = AeonButtonTokens.Small.iconGap,
            shape = AeonButtonTokens.Small.shape,
            textStyle = AeonTextStyles.ButtonMedium
        )

        AeonButtonSize.Medium -> AeonResolvedButtonToken(
            height = AeonButtonTokens.Primary.height,
            minWidth = AeonButtonTokens.Primary.minWidth,
            horizontalPadding = AeonButtonTokens.Primary.horizontalPadding,
            verticalPadding = AeonButtonTokens.Primary.verticalPadding,
            iconSize = AeonButtonTokens.Primary.iconSize,
            iconGap = AeonButtonTokens.Primary.iconGap,
            shape = AeonButtonTokens.Primary.shape,
            textStyle = AeonTextStyles.ButtonMedium
        )

        AeonButtonSize.Large -> AeonResolvedButtonToken(
            height = AeonButtonTokens.Large.height,
            minWidth = AeonButtonTokens.Large.minWidth,
            horizontalPadding = AeonButtonTokens.Large.horizontalPadding,
            verticalPadding = AeonButtonTokens.Large.verticalPadding,
            iconSize = AeonButtonTokens.Large.iconSize,
            iconGap = AeonButtonTokens.Large.iconGap,
            shape = AeonButtonTokens.Large.shape,
            textStyle = AeonTextStyles.ButtonLarge
        )

        AeonButtonSize.Pill -> AeonResolvedButtonToken(
            height = AeonButtonTokens.Pill.height,
            minWidth = AeonButtonTokens.Pill.minWidth,
            horizontalPadding = AeonButtonTokens.Pill.horizontalPadding,
            verticalPadding = AeonButtonTokens.Pill.verticalPadding,
            iconSize = AeonButtonTokens.Pill.iconSize,
            iconGap = AeonButtonTokens.Pill.iconGap,
            shape = AeonButtonTokens.Pill.shape,
            textStyle = AeonTextStyles.ButtonMedium
        )
    }
}


// ----------------------------------------------------
// Color Resolver
// ----------------------------------------------------

@Composable
private fun aeonButtonColors(
    variant: AeonButtonVariant,
    enabled: Boolean
): AeonButtonColorToken {
    val colors = AeonThemeTokens.colors

    val alpha = if (enabled) 1f else AeonMotionAlpha.Disabled

    return when (variant) {
        AeonButtonVariant.Primary -> AeonButtonColorToken(
            container = colors.brand.copy(alpha = alpha),
            content = Color.White.copy(alpha = alpha),
            tonalElevation = AeonButtonTokens.ElevatedElevation,
            shadowElevation = AeonButtonTokens.Elevation
        )

        AeonButtonVariant.Secondary -> AeonButtonColorToken(
            container = colors.surfaceElevated.copy(alpha = alpha),
            content = colors.textPrimary.copy(alpha = alpha),
            tonalElevation = AeonButtonTokens.Elevation,
            shadowElevation = AeonButtonTokens.Elevation
        )

        AeonButtonVariant.Tonal -> AeonButtonColorToken(
            container = colors.brandSoft.copy(alpha = if (enabled) 1f else 0.32f),
            content = if (colors.isDark) {
                colors.textPrimary.copy(alpha = alpha)
            } else {
                colors.brandDeep.copy(alpha = alpha)
            },
            tonalElevation = AeonButtonTokens.Elevation,
            shadowElevation = AeonButtonTokens.Elevation
        )

        AeonButtonVariant.Ghost -> AeonButtonColorToken(
            container = Color.Transparent,
            content = colors.textSecondary.copy(alpha = alpha),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        )

        AeonButtonVariant.Premium -> AeonButtonColorToken(
            container = colors.premiumGold.copy(alpha = alpha),
            content = Color(0xFF111318).copy(alpha = alpha),
            tonalElevation = AeonButtonTokens.ElevatedElevation,
            shadowElevation = AeonButtonTokens.ElevatedElevation
        )

        AeonButtonVariant.Success -> AeonButtonColorToken(
            container = colors.success.copy(alpha = alpha),
            content = Color.White.copy(alpha = alpha),
            tonalElevation = AeonButtonTokens.ElevatedElevation,
            shadowElevation = AeonButtonTokens.Elevation
        )

        AeonButtonVariant.Danger -> AeonButtonColorToken(
            container = colors.error.copy(alpha = alpha),
            content = Color.White.copy(alpha = alpha),
            tonalElevation = AeonButtonTokens.ElevatedElevation,
            shadowElevation = AeonButtonTokens.Elevation
        )
    }
}


// ----------------------------------------------------
// Border Resolver
// ----------------------------------------------------

@Composable
private fun aeonButtonBorder(
    variant: AeonButtonVariant,
    enabled: Boolean
): BorderStroke? {
    val colors = AeonThemeTokens.colors
    val alpha = if (enabled) 1f else AeonMotionAlpha.Disabled

    return when (variant) {
        AeonButtonVariant.Secondary -> BorderStroke(
            width = 1.dp,
            color = colors.border.copy(alpha = alpha)
        )

        AeonButtonVariant.Ghost -> BorderStroke(
            width = 1.dp,
            color = colors.borderSoft.copy(alpha = alpha)
        )

        AeonButtonVariant.Tonal -> BorderStroke(
            width = 1.dp,
            color = colors.brand.copy(alpha = 0.20f * alpha)
        )

        else -> null
    }
}


// ----------------------------------------------------
// Convenience Buttons
// ----------------------------------------------------

@Composable
fun AeonPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    AeonButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = AeonButtonVariant.Primary,
        enabled = enabled,
        loading = loading,
        fullWidth = fullWidth,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}


@Composable
fun AeonSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    AeonButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = AeonButtonVariant.Secondary,
        enabled = enabled,
        loading = loading,
        fullWidth = fullWidth,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}


@Composable
fun AeonGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = false
) {
    AeonButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = AeonButtonVariant.Ghost,
        enabled = enabled,
        fullWidth = fullWidth
    )
}


@Composable
fun AeonPremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false
) {
    AeonButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = AeonButtonVariant.Premium,
        enabled = enabled,
        loading = loading,
        fullWidth = fullWidth
    )
}
