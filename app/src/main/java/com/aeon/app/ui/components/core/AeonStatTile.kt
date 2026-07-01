package com.aeon.app.ui.components.core

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.theme.AeonCardSpacing
import com.aeon.app.ui.theme.AeonComponentShapes
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing
import com.aeon.app.ui.theme.AeonMotionAlpha
import com.aeon.app.ui.theme.AeonSize
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM STAT TILE
 *
 * Purpose:
 * A reusable metric tile for Aeon dashboards.
 *
 * Use for:
 * - Life score mini stats
 * - Focus today
 * - Habits completed
 * - Expenses today
 * - Sleep hours
 * - Mood score
 * - Study time
 * - Goal progress
 *
 * Senior UI/UX Rule:
 * Stat tiles must be highly scannable.
 * The value should be visually dominant, the label should explain context,
 * and optional trend/progress should be subtle.
 */


// ----------------------------------------------------
// Stat Tile Variant
// ----------------------------------------------------

enum class AeonStatTileVariant {
    Default,
    Compact,
    Hero,
    Minimal
}


// ----------------------------------------------------
// Stat Tile Tone
// ----------------------------------------------------

enum class AeonStatTileTone {
    Neutral,
    Brand,
    Premium,
    Success,
    Warning,
    Danger,
    Info,
    Focus,
    Finance,
    Health,
    Mood
}


// ----------------------------------------------------
// Trend Direction
// ----------------------------------------------------

enum class AeonTrendDirection {
    Up,
    Down,
    Neutral
}


// ----------------------------------------------------
// Resolved Token
// ----------------------------------------------------

@Immutable
private data class AeonStatTileToken(
    val minHeight: Dp,
    val contentPadding: PaddingValues,
    val iconContainerSize: Dp,
    val iconSize: Dp,
    val valueStyle: TextStyle,
    val labelStyle: TextStyle,
    val captionStyle: TextStyle,
    val gap: Dp,
    val progressHeight: Dp
)


// ----------------------------------------------------
// Main Stat Tile
// ----------------------------------------------------

@Composable
fun AeonStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    trendText: String? = null,
    trendDirection: AeonTrendDirection = AeonTrendDirection.Neutral,
    progress: Float? = null,
    variant: AeonStatTileVariant = AeonStatTileVariant.Default,
    tone: AeonStatTileTone = AeonStatTileTone.Neutral,
    icon: (@Composable () -> Unit)? = null,
    backgroundBrush: Brush? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors
    val token = aeonStatTileToken(variant)
    val toneColor = aeonStatTileToneColor(tone)

    val animatedToneColor by animateColorAsState(
        targetValue = toneColor,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Standard
        ),
        label = "aeon_stat_tile_tone_color"
    )

    val cardVariant = when (variant) {
        AeonStatTileVariant.Hero -> AeonCardVariant.Hero
        AeonStatTileVariant.Compact -> AeonCardVariant.Compact
        AeonStatTileVariant.Minimal -> AeonCardVariant.Compact
        AeonStatTileVariant.Default -> AeonCardVariant.Default
    }

    AeonCard(
        modifier = modifier.heightIn(min = token.minHeight),
        variant = cardVariant,
        onClick = onClick,
        backgroundBrush = backgroundBrush,
        containerColor = if (variant == AeonStatTileVariant.Minimal) {
            Color.Transparent
        } else {
            null
        },
        borderColor = if (variant == AeonStatTileVariant.Minimal) {
            Color.Transparent
        } else {
            colors.borderSoft
        },
        contentPadding = token.contentPadding
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(token.gap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = value,
                        style = token.valueStyle,
                        color = if (tone == AeonStatTileTone.Neutral) {
                            colors.textPrimary
                        } else {
                            animatedToneColor
                        }
                    )

                    Spacer(modifier = Modifier.height(AeonSpacing.XSmall))

                    Text(
                        text = label,
                        style = token.labelStyle,
                        color = colors.textSecondary
                    )
                }

                if (icon != null) {
                    AeonStatIconContainer(
                        toneColor = animatedToneColor,
                        iconContainerSize = token.iconContainerSize,
                        iconSize = token.iconSize,
                        icon = icon
                    )
                }
            }

            if (caption != null || trendText != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (caption != null) {
                        Text(
                            text = caption,
                            style = token.captionStyle,
                            color = colors.textTertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (trendText != null) {
                        AeonTrendBadge(
                            text = trendText,
                            direction = trendDirection
                        )
                    }
                }
            }

            if (progress != null) {
                AeonStatProgressBar(
                    progress = progress,
                    color = animatedToneColor,
                    height = token.progressHeight
                )
            }
        }
    }
}


// ----------------------------------------------------
// Horizontal Stat Tile
// Good for settings, summaries, compact dashboard rows.
// ----------------------------------------------------

@Composable
fun AeonHorizontalStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    tone: AeonStatTileTone = AeonStatTileTone.Neutral,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors
    val toneColor = aeonStatTileToneColor(tone)

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        onClick = onClick,
        contentPadding = PaddingValues(AeonCardSpacing.CompactPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                AeonStatIconContainer(
                    toneColor = toneColor,
                    iconContainerSize = 42.dp,
                    iconSize = AeonSize.IconMedium,
                    icon = icon
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = AeonTextStyles.CardSubtitle,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(AeonSpacing.XSmall))

                Text(
                    text = value,
                    style = AeonTextStyles.StatNumber,
                    color = if (tone == AeonStatTileTone.Neutral) {
                        colors.textPrimary
                    } else {
                        toneColor
                    }
                )

                if (caption != null) {
                    Spacer(modifier = Modifier.height(AeonSpacing.XSmall))

                    Text(
                        text = caption,
                        style = AeonTextStyles.Caption,
                        color = colors.textTertiary
                    )
                }
            }
        }
    }
}


// ----------------------------------------------------
// Mini Stat Tile
// Good for small dashboard metric rows.
// ----------------------------------------------------

@Composable
fun AeonMiniStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tone: AeonStatTileTone = AeonStatTileTone.Neutral,
    icon: (@Composable () -> Unit)? = null
) {
    AeonStatTile(
        value = value,
        label = label,
        modifier = modifier,
        variant = AeonStatTileVariant.Compact,
        tone = tone,
        icon = icon
    )
}


// ----------------------------------------------------
// Icon Container
// ----------------------------------------------------

@Composable
private fun AeonStatIconContainer(
    toneColor: Color,
    iconContainerSize: Dp,
    iconSize: Dp,
    icon: @Composable () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Surface(
        modifier = Modifier.size(iconContainerSize),
        shape = AeonComponentShapes.IconButton,
        color = toneColor.copy(
            alpha = if (colors.isDark) 0.16f else 0.10f
        ),
        contentColor = toneColor
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides toneColor
            ) {
                Box(
                    modifier = Modifier.size(iconSize),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }
        }
    }
}


// ----------------------------------------------------
// Trend Badge
// ----------------------------------------------------

@Composable
private fun AeonTrendBadge(
    text: String,
    direction: AeonTrendDirection
) {
    val colors = AeonThemeTokens.colors

    val badgeColor = when (direction) {
        AeonTrendDirection.Up -> colors.success
        AeonTrendDirection.Down -> colors.error
        AeonTrendDirection.Neutral -> colors.textTertiary
    }

    val badgeBackground = when (direction) {
        AeonTrendDirection.Up -> colors.successSoft
        AeonTrendDirection.Down -> colors.errorSoft
        AeonTrendDirection.Neutral -> colors.surfaceMuted
    }

    Surface(
        shape = AeonComponentShapes.Badge,
        color = badgeBackground.copy(
            alpha = if (colors.isDark) 0.72f else 1f
        ),
        contentColor = badgeColor
    ) {
        Text(
            text = text,
            style = AeonTextStyles.Micro,
            color = badgeColor,
            modifier = Modifier.padding(
                horizontal = AeonSpacing.Small,
                vertical = AeonSpacing.XSmall
            )
        )
    }
}


// ----------------------------------------------------
// Progress Bar
// ----------------------------------------------------

@Composable
private fun AeonStatProgressBar(
    progress: Float,
    color: Color,
    height: Dp
) {
    val colors = AeonThemeTokens.colors

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = AeonDuration.Slow,
            easing = AeonEasing.Emphasized
        ),
        label = "aeon_stat_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(AeonComponentShapes.Chip)
            .background(colors.surfaceMuted)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(AeonComponentShapes.Chip)
                .background(color)
        )
    }
}


// ----------------------------------------------------
// Token Resolver
// ----------------------------------------------------

private fun aeonStatTileToken(
    variant: AeonStatTileVariant
): AeonStatTileToken {
    return when (variant) {
        AeonStatTileVariant.Compact -> AeonStatTileToken(
            minHeight = 88.dp,
            contentPadding = PaddingValues(AeonCardSpacing.CompactPadding),
            iconContainerSize = 38.dp,
            iconSize = AeonSize.IconSmall,
            valueStyle = AeonTextStyles.MoneySmall,
            labelStyle = AeonTextStyles.Caption,
            captionStyle = AeonTextStyles.Micro,
            gap = AeonSpacing.Small,
            progressHeight = 6.dp
        )

        AeonStatTileVariant.Default -> AeonStatTileToken(
            minHeight = 118.dp,
            contentPadding = PaddingValues(AeonCardSpacing.Padding),
            iconContainerSize = 44.dp,
            iconSize = AeonSize.IconMedium,
            valueStyle = AeonTextStyles.StatNumber,
            labelStyle = AeonTextStyles.CardSubtitle,
            captionStyle = AeonTextStyles.Caption,
            gap = AeonSpacing.Medium,
            progressHeight = 8.dp
        )

        AeonStatTileVariant.Hero -> AeonStatTileToken(
            minHeight = 148.dp,
            contentPadding = PaddingValues(AeonCardSpacing.HeroPadding),
            iconContainerSize = 52.dp,
            iconSize = AeonSize.IconLarge,
            valueStyle = AeonTextStyles.HeroMetric,
            labelStyle = AeonTextStyles.CardSubtitle,
            captionStyle = AeonTextStyles.Caption,
            gap = AeonSpacing.Large,
            progressHeight = 10.dp
        )

        AeonStatTileVariant.Minimal -> AeonStatTileToken(
            minHeight = 72.dp,
            contentPadding = PaddingValues(AeonSpacing.Small),
            iconContainerSize = 36.dp,
            iconSize = AeonSize.IconSmall,
            valueStyle = AeonTextStyles.StatNumber,
            labelStyle = AeonTextStyles.Caption,
            captionStyle = AeonTextStyles.Micro,
            gap = AeonSpacing.Small,
            progressHeight = 6.dp
        )
    }
}


// ----------------------------------------------------
// Tone Color Resolver
// ----------------------------------------------------

@Composable
private fun aeonStatTileToneColor(
    tone: AeonStatTileTone
): Color {
    val colors = AeonThemeTokens.colors

    return when (tone) {
        AeonStatTileTone.Neutral -> colors.textPrimary
        AeonStatTileTone.Brand -> colors.brand
        AeonStatTileTone.Premium -> colors.premiumGold
        AeonStatTileTone.Success -> colors.success
        AeonStatTileTone.Warning -> colors.warning
        AeonStatTileTone.Danger -> colors.error
        AeonStatTileTone.Info -> colors.info
        AeonStatTileTone.Focus -> colors.focus
        AeonStatTileTone.Finance -> colors.finance
        AeonStatTileTone.Health -> colors.health
        AeonStatTileTone.Mood -> colors.mood
    }
}
