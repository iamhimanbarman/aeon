package com.aeon.app.ui.components.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.theme.AeonCardTokens
import com.aeon.app.ui.theme.AeonComponentMotion
import com.aeon.app.ui.theme.AeonInsightTokens
import com.aeon.app.ui.theme.AeonLifeScoreTokens
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM CARD SYSTEM
 *
 * Purpose:
 * A reusable premium card foundation for Aeon.
 *
 * Senior UI/UX Rule:
 * Cards create hierarchy, rhythm, and trust.
 * Do not use random shapes, paddings, elevations, borders, or colors.
 *
 * Use this for:
 * - Dashboard cards
 * - Insight cards
 * - Settings tiles
 * - Feature summary cards
 * - Life management modules
 * - Premium glass containers
 */


// ----------------------------------------------------
// Card Variants
// ----------------------------------------------------

enum class AeonCardVariant {
    Default,
    Compact,
    Elevated,
    Hero,
    Glass,
    LifeScore,
    Insight
}


// ----------------------------------------------------
// Internal Resolved Card Token
// ----------------------------------------------------

@Immutable
data class AeonResolvedCardToken(
    val shape: Shape,
    val padding: Dp,
    val elevation: Dp,
    val borderWidth: Dp
)


// ----------------------------------------------------
// Main Aeon Card
// ----------------------------------------------------

@Composable
fun AeonCard(
    modifier: Modifier = Modifier,
    variant: AeonCardVariant = AeonCardVariant.Default,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
    onClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
    borderColor: Color? = null,
    backgroundBrush: Brush? = null,
    contentPadding: PaddingValues? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AeonThemeTokens.colors
    val token = aeonCardToken(variant)

    val resolvedContainerColor = containerColor ?: aeonCardContainerColor(variant)
    val resolvedContentColor = contentColor ?: colors.textPrimary
    val resolvedBorderColor = borderColor ?: aeonCardBorderColor(variant)
    val resolvedPadding = contentPadding ?: PaddingValues(token.padding)

    val isPressed by interactionSource.collectIsPressedAsState()

    val targetScale = when {
        !enabled -> 1f
        onClick == null -> 1f
        isPressed -> AeonComponentMotion.CardPressedScale
        else -> AeonComponentMotion.CardDefaultScale
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = AeonComponentMotion.CardPressDuration
        ),
        label = "aeon_card_press_scale"
    )

    val cardModifier = modifier
        .then(
            if (fullWidth) {
                Modifier.fillMaxWidth()
            } else {
                Modifier
            }
        )
        .scale(animatedScale)

    val cardBorder = BorderStroke(
        width = token.borderWidth,
        color = resolvedBorderColor
    )

    val cardColors = CardDefaults.cardColors(
        containerColor = if (backgroundBrush == null) {
            resolvedContainerColor
        } else {
            Color.Transparent
        },
        contentColor = resolvedContentColor,
        disabledContainerColor = resolvedContainerColor.copy(alpha = 0.56f),
        disabledContentColor = resolvedContentColor.copy(alpha = 0.45f)
    )

    val cardElevation = CardDefaults.cardElevation(
        defaultElevation = token.elevation,
        pressedElevation = 0.dp,
        focusedElevation = token.elevation,
        hoveredElevation = token.elevation,
        disabledElevation = 0.dp
    )

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            enabled = enabled,
            shape = token.shape,
            colors = cardColors,
            elevation = cardElevation,
            border = cardBorder,
            interactionSource = interactionSource
        ) {
            AeonCardContent(
                shape = token.shape,
                backgroundBrush = backgroundBrush,
                contentPadding = resolvedPadding,
                contentColor = resolvedContentColor,
                content = content
            )
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = token.shape,
            colors = cardColors,
            elevation = cardElevation,
            border = cardBorder
        ) {
            AeonCardContent(
                shape = token.shape,
                backgroundBrush = backgroundBrush,
                contentPadding = resolvedPadding,
                contentColor = resolvedContentColor,
                content = content
            )
        }
    }
}


// ----------------------------------------------------
// Card Content Wrapper
// ----------------------------------------------------

@Composable
private fun AeonCardContent(
    shape: Shape,
    backgroundBrush: Brush?,
    contentPadding: PaddingValues,
    contentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .then(
                if (backgroundBrush != null) {
                    Modifier.background(
                        brush = backgroundBrush,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .fillMaxWidth()
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            ProvideTextStyle(
                value = AeonTextStyles.CardSubtitle
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(contentPadding),
                    content = content
                )
            }
        }
    }
}


// ----------------------------------------------------
// Convenience Cards
// ----------------------------------------------------

@Composable
fun AeonElevatedCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Elevated,
        enabled = enabled,
        onClick = onClick,
        contentPadding = contentPadding,
        content = content
    )
}


@Composable
fun AeonHeroCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    backgroundBrush: Brush? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Hero,
        enabled = enabled,
        onClick = onClick,
        backgroundBrush = backgroundBrush,
        contentPadding = contentPadding,
        content = content
    )
}


@Composable
fun AeonGlassCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Glass,
        enabled = enabled,
        onClick = onClick,
        containerColor = colors.surfaceGlass,
        borderColor = colors.borderSoft,
        contentPadding = contentPadding,
        content = content
    )
}


@Composable
fun AeonCompactCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        enabled = enabled,
        onClick = onClick,
        contentPadding = contentPadding,
        content = content
    )
}


// ----------------------------------------------------
// Aeon Feature Card Variants
// ----------------------------------------------------

@Composable
fun AeonLifeScoreCardContainer(
    modifier: Modifier = Modifier,
    backgroundBrush: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.LifeScore,
        onClick = onClick,
        backgroundBrush = backgroundBrush,
        content = content
    )
}


@Composable
fun AeonInsightCardContainer(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Insight,
        onClick = onClick,
        content = content
    )
}


// ----------------------------------------------------
// Token Resolver
// ----------------------------------------------------

private fun aeonCardToken(
    variant: AeonCardVariant
): AeonResolvedCardToken {
    return when (variant) {
        AeonCardVariant.Default -> AeonResolvedCardToken(
            shape = AeonCardTokens.Default.shape,
            padding = AeonCardTokens.Default.padding,
            elevation = AeonCardTokens.Default.elevation,
            borderWidth = AeonCardTokens.Default.borderWidth
        )

        AeonCardVariant.Compact -> AeonResolvedCardToken(
            shape = AeonCardTokens.Compact.shape,
            padding = AeonCardTokens.Compact.padding,
            elevation = AeonCardTokens.Compact.elevation,
            borderWidth = AeonCardTokens.Compact.borderWidth
        )

        AeonCardVariant.Elevated -> AeonResolvedCardToken(
            shape = AeonCardTokens.Elevated.shape,
            padding = AeonCardTokens.Elevated.padding,
            elevation = AeonCardTokens.Elevated.elevation,
            borderWidth = AeonCardTokens.Elevated.borderWidth
        )

        AeonCardVariant.Hero -> AeonResolvedCardToken(
            shape = AeonCardTokens.Hero.shape,
            padding = AeonCardTokens.Hero.padding,
            elevation = AeonCardTokens.Hero.elevation,
            borderWidth = AeonCardTokens.Hero.borderWidth
        )

        AeonCardVariant.Glass -> AeonResolvedCardToken(
            shape = AeonCardTokens.Glass.shape,
            padding = AeonCardTokens.Glass.padding,
            elevation = AeonCardTokens.Glass.elevation,
            borderWidth = AeonCardTokens.Glass.borderWidth
        )

        AeonCardVariant.LifeScore -> AeonResolvedCardToken(
            shape = AeonLifeScoreTokens.CardShape,
            padding = AeonLifeScoreTokens.CardPadding,
            elevation = AeonLifeScoreTokens.CardElevation,
            borderWidth = 1.dp
        )

        AeonCardVariant.Insight -> AeonResolvedCardToken(
            shape = AeonInsightTokens.CardShape,
            padding = AeonInsightTokens.CardPadding,
            elevation = AeonInsightTokens.CardElevation,
            borderWidth = 1.dp
        )
    }
}


// ----------------------------------------------------
// Color Resolver
// ----------------------------------------------------

@Composable
private fun aeonCardContainerColor(
    variant: AeonCardVariant
): Color {
    val colors = AeonThemeTokens.colors

    return when (variant) {
        AeonCardVariant.Default -> colors.surface
        AeonCardVariant.Compact -> colors.surface
        AeonCardVariant.Elevated -> colors.surfaceElevated
        AeonCardVariant.Hero -> colors.surfaceElevated
        AeonCardVariant.Glass -> colors.surfaceGlass
        AeonCardVariant.LifeScore -> colors.surfaceElevated
        AeonCardVariant.Insight -> colors.surface
    }
}


@Composable
private fun aeonCardBorderColor(
    variant: AeonCardVariant
): Color {
    val colors = AeonThemeTokens.colors

    return when (variant) {
        AeonCardVariant.Default -> colors.borderSoft
        AeonCardVariant.Compact -> colors.borderSoft
        AeonCardVariant.Elevated -> colors.border
        AeonCardVariant.Hero -> colors.border
        AeonCardVariant.Glass -> colors.borderSoft
        AeonCardVariant.LifeScore -> colors.premiumGold.copy(alpha = 0.34f)
        AeonCardVariant.Insight -> colors.borderSoft
    }
}


// ----------------------------------------------------
// Premium Gradient Helpers
// ----------------------------------------------------

@Composable
fun aeonBrandCardBrush(): Brush {
    val colors = AeonThemeTokens.colors

    return Brush.linearGradient(
        colors = listOf(
            colors.brand.copy(alpha = 0.92f),
            colors.intelligence.copy(alpha = 0.80f),
            colors.calm.copy(alpha = 0.72f)
        )
    )
}


@Composable
fun aeonLifeScoreCardBrush(): Brush {
    val colors = AeonThemeTokens.colors

    return Brush.linearGradient(
        colors = listOf(
            colors.premiumGold.copy(alpha = 0.92f),
            colors.brand.copy(alpha = 0.88f)
        )
    )
}


@Composable
fun aeonCalmCardBrush(): Brush {
    val colors = AeonThemeTokens.colors

    return Brush.linearGradient(
        colors = listOf(
            colors.calm.copy(alpha = 0.82f),
            colors.intelligence.copy(alpha = 0.70f)
        )
    )
}
