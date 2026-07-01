package com.aeon.app.ui.components.feature

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.core.AeonLinearProgress
import com.aeon.app.ui.components.core.AeonProgressSize
import com.aeon.app.ui.components.core.AeonProgressTone
import com.aeon.app.ui.theme.AeonComponentShapes
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing
import com.aeon.app.ui.theme.AeonInsightTokens
import com.aeon.app.ui.theme.AeonMotionAlpha
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM INSIGHT CARD
 *
 * Purpose:
 * A reusable insight component for Aeon analytics, AI suggestions,
 * weekly reviews, habit analysis, finance alerts, mood patterns,
 * focus reports, and life score explanations.
 *
 * Senior UI/UX Rule:
 * Insight cards should feel intelligent, calm, and actionable.
 * They should explain what changed, why it matters, and what the user
 * can do next.
 */


// ----------------------------------------------------
// Insight Tone
// ----------------------------------------------------

enum class InsightTone {
    Brand,
    AI,
    Focus,
    Habit,
    Health,
    Finance,
    Mood,
    Goal,
    Learning,
    Relationship,
    Success,
    Warning,
    Danger,
    Neutral
}


// ----------------------------------------------------
// Insight Priority
// ----------------------------------------------------

enum class InsightPriority {
    Low,
    Medium,
    High,
    Critical
}


// ----------------------------------------------------
// Insight Trend
// ----------------------------------------------------

enum class InsightTrend {
    Up,
    Down,
    Stable,
    None
}


// ----------------------------------------------------
// Insight Metric
// ----------------------------------------------------

@Immutable
data class InsightMetric(
    val label: String,
    val value: String,
    val tone: InsightTone = InsightTone.Neutral
)


// ----------------------------------------------------
// Insight Tag
// ----------------------------------------------------

@Immutable
data class InsightTag(
    val label: String,
    val tone: InsightTone = InsightTone.Neutral
)


// ----------------------------------------------------
// Main Insight Card
// ----------------------------------------------------

@Composable
fun InsightCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    eyebrow: String = "Insight",
    tone: InsightTone = InsightTone.Brand,
    priority: InsightPriority = InsightPriority.Medium,
    trend: InsightTrend = InsightTrend.None,
    trendText: String? = null,
    score: Int? = null,
    progress: Float? = null,
    metrics: List<InsightMetric> = emptyList(),
    tags: List<InsightTag> = emptyList(),
    expanded: Boolean = false,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors
    val accentColor = insightToneColor(tone)
    val priorityColor = insightPriorityColor(priority)

    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else AeonMotionAlpha.Disabled,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Standard
        ),
        label = "insight_card_alpha"
    )

    val animatedAccentColor by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Standard
        ),
        label = "insight_card_accent"
    )

    AeonCard(
        modifier = modifier
            .defaultMinSize(minHeight = 156.dp)
            .alpha(alpha),
        variant = AeonCardVariant.Insight,
        enabled = enabled,
        onClick = onClick,
        backgroundBrush = insightCardBrush(tone),
        borderColor = animatedAccentColor.copy(alpha = 0.24f),
        contentPadding = PaddingValues(AeonInsightTokens.CardPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.Large)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
                verticalAlignment = Alignment.Top
            ) {
                InsightIconContainer(
                    tone = tone,
                    icon = icon
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = eyebrow.uppercase(),
                            style = AeonTextStyles.Micro,
                            color = animatedAccentColor,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        InsightPriorityBadge(
                            priority = priority,
                            color = priorityColor
                        )
                    }

                    Text(
                        text = title,
                        style = AeonTextStyles.InsightTitle,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = description,
                        style = AeonTextStyles.InsightBody,
                        color = colors.textSecondary,
                        maxLines = if (expanded) 8 else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trend != InsightTrend.None || trendText != null || score != null) {
                InsightMetaRow(
                    trend = trend,
                    trendText = trendText,
                    score = score,
                    tone = tone
                )
            }

            if (progress != null) {
                AeonLinearProgress(
                    progress = progress,
                    tone = insightProgressTone(tone),
                    size = AeonProgressSize.Medium,
                    progressColor = animatedAccentColor,
                    showLabel = false
                )
            }

            AnimatedVisibility(
                visible = expanded && metrics.isNotEmpty(),
                enter = fadeIn(
                    animationSpec = tween(AeonDuration.Medium)
                ) + slideInVertically(
                    initialOffsetY = { it / 5 },
                    animationSpec = tween(
                        durationMillis = AeonDuration.Medium,
                        easing = AeonEasing.Decelerate
                    )
                )
            ) {
                InsightMetricsGrid(
                    metrics = metrics
                )
            }

            if (tags.isNotEmpty()) {
                InsightTags(
                    tags = tags
                )
            }

            if (actionText != null && onActionClick != null) {
                AeonButton(
                    text = actionText,
                    onClick = onActionClick,
                    variant = insightButtonVariant(tone),
                    size = AeonButtonSize.Medium,
                    fullWidth = true
                )
            }
        }
    }
}


// ----------------------------------------------------
// Compact Insight Card
// ----------------------------------------------------

@Composable
fun CompactInsightCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    tone: InsightTone = InsightTone.Brand,
    trendText: String? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors
    val accentColor = insightToneColor(tone)

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        onClick = onClick,
        borderColor = accentColor.copy(alpha = 0.22f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InsightIconContainer(
                tone = tone,
                icon = icon,
                compact = true
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Text(
                    text = title,
                    style = AeonTextStyles.CardTitle,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (description != null) {
                    Text(
                        text = description,
                        style = AeonTextStyles.Caption,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trendText != null) {
                AeonChip(
                    text = trendText,
                    variant = insightChipVariant(tone),
                    size = AeonChipSize.Compact
                )
            }
        }
    }
}


// ----------------------------------------------------
// AI Insight Card
// ----------------------------------------------------

@Composable
fun AIInsightCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    confidence: Int? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    InsightCard(
        title = title,
        description = description,
        modifier = modifier,
        eyebrow = "Aeon AI",
        tone = InsightTone.AI,
        priority = InsightPriority.Medium,
        score = confidence,
        tags = listOf(
            InsightTag("Personalized", InsightTone.AI),
            InsightTag("Private", InsightTone.Brand)
        ),
        actionText = actionText,
        onActionClick = onActionClick,
        onClick = onClick
    )
}


// ----------------------------------------------------
// Warning Insight Card
// ----------------------------------------------------

@Composable
fun WarningInsightCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    InsightCard(
        title = title,
        description = description,
        modifier = modifier,
        eyebrow = "Needs Attention",
        tone = InsightTone.Warning,
        priority = InsightPriority.High,
        trend = InsightTrend.Down,
        actionText = actionText,
        onActionClick = onActionClick
    )
}


// ----------------------------------------------------
// Icon Container
// ----------------------------------------------------

@Composable
private fun InsightIconContainer(
    tone: InsightTone,
    icon: (@Composable () -> Unit)?,
    compact: Boolean = false
) {
    val colors = AeonThemeTokens.colors
    val accentColor = insightToneColor(tone)
    val size = if (compact) 42.dp else 52.dp
    val iconSize = if (compact) 20.dp else 26.dp

    Surface(
        modifier = Modifier.size(size),
        shape = AeonComponentShapes.IconButton,
        color = accentColor.copy(alpha = if (colors.isDark) 0.16f else 0.10f),
        contentColor = accentColor,
        border = BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.22f)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides accentColor
                ) {
                    Box(
                        modifier = Modifier.size(iconSize),
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }
                }
            } else {
                Text(
                    text = insightToneSymbol(tone),
                    style = if (compact) {
                        AeonTextStyles.CardTitle
                    } else {
                        AeonTextStyles.StatNumber
                    },
                    color = accentColor
                )
            }
        }
    }
}


// ----------------------------------------------------
// Priority Badge
// ----------------------------------------------------

@Composable
private fun InsightPriorityBadge(
    priority: InsightPriority,
    color: Color
) {
    val colors = AeonThemeTokens.colors

    Surface(
        shape = AeonComponentShapes.Badge,
        color = color.copy(alpha = if (colors.isDark) 0.16f else 0.10f),
        contentColor = color,
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.20f)
        )
    ) {
        Text(
            text = insightPriorityLabel(priority),
            style = AeonTextStyles.Micro,
            color = color,
            modifier = Modifier.padding(
                horizontal = AeonSpacing.Small,
                vertical = AeonSpacing.XSmall
            )
        )
    }
}


// ----------------------------------------------------
// Meta Row
// ----------------------------------------------------

@Composable
private fun InsightMetaRow(
    trend: InsightTrend,
    trendText: String?,
    score: Int?,
    tone: InsightTone
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (trend != InsightTrend.None || trendText != null) {
            AeonChip(
                text = trendText ?: insightTrendLabel(trend),
                variant = insightTrendChipVariant(trend),
                size = AeonChipSize.Compact
            )
        }

        if (score != null) {
            AeonChip(
                text = "${score.coerceIn(0, 100)}%",
                variant = insightChipVariant(tone),
                size = AeonChipSize.Compact
            )
        }
    }
}


// ----------------------------------------------------
// Metrics Grid
// ----------------------------------------------------

@Composable
private fun InsightMetricsGrid(
    metrics: List<InsightMetric>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        metrics.take(3).forEach { metric ->
            InsightMetricTile(
                metric = metric,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
private fun InsightMetricTile(
    metric: InsightMetric,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val accentColor = insightToneColor(metric.tone)

    Surface(
        modifier = modifier,
        shape = AeonComponentShapes.CardSmall,
        color = colors.surfaceMuted.copy(alpha = if (colors.isDark) 0.72f else 1f),
        border = BorderStroke(
            width = 1.dp,
            color = colors.borderSoft
        )
    ) {
        Column(
            modifier = Modifier.padding(AeonSpacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
        ) {
            Text(
                text = metric.value,
                style = AeonTextStyles.StatNumber,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = metric.label,
                style = AeonTextStyles.Micro,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


// ----------------------------------------------------
// Tags
// ----------------------------------------------------

@Composable
private fun InsightTags(
    tags: List<InsightTag>
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
    ) {
        tags.take(5).forEach { tag ->
            AeonChip(
                text = tag.label,
                variant = insightChipVariant(tag.tone),
                size = AeonChipSize.Compact
            )
        }
    }
}


// ----------------------------------------------------
// Brush
// ----------------------------------------------------

@Composable
private fun insightCardBrush(
    tone: InsightTone
): Brush {
    val colors = AeonThemeTokens.colors
    val accentColor = insightToneColor(tone)

    return Brush.linearGradient(
        colors = listOf(
            colors.surface,
            accentColor.copy(alpha = if (colors.isDark) 0.12f else 0.07f),
            colors.surfaceElevated
        )
    )
}


// ----------------------------------------------------
// Resolvers
// ----------------------------------------------------

@Composable
private fun insightToneColor(
    tone: InsightTone
): Color {
    val colors = AeonThemeTokens.colors

    return when (tone) {
        InsightTone.Brand -> colors.brand
        InsightTone.AI -> colors.ai
        InsightTone.Focus -> colors.focus
        InsightTone.Habit -> colors.habit
        InsightTone.Health -> colors.health
        InsightTone.Finance -> colors.finance
        InsightTone.Mood -> colors.mood
        InsightTone.Goal -> colors.goal
        InsightTone.Learning -> colors.learning
        InsightTone.Relationship -> colors.relationship
        InsightTone.Success -> colors.success
        InsightTone.Warning -> colors.warning
        InsightTone.Danger -> colors.error
        InsightTone.Neutral -> colors.textTertiary
    }
}


@Composable
private fun insightPriorityColor(
    priority: InsightPriority
): Color {
    val colors = AeonThemeTokens.colors

    return when (priority) {
        InsightPriority.Low -> colors.textTertiary
        InsightPriority.Medium -> colors.info
        InsightPriority.High -> colors.warning
        InsightPriority.Critical -> colors.error
    }
}


private fun insightPriorityLabel(
    priority: InsightPriority
): String {
    return when (priority) {
        InsightPriority.Low -> "Low"
        InsightPriority.Medium -> "Medium"
        InsightPriority.High -> "High"
        InsightPriority.Critical -> "Critical"
    }
}


private fun insightTrendLabel(
    trend: InsightTrend
): String {
    return when (trend) {
        InsightTrend.Up -> "Improving"
        InsightTrend.Down -> "Declining"
        InsightTrend.Stable -> "Stable"
        InsightTrend.None -> "No trend"
    }
}


private fun insightTrendChipVariant(
    trend: InsightTrend
): AeonChipVariant {
    return when (trend) {
        InsightTrend.Up -> AeonChipVariant.Success
        InsightTrend.Down -> AeonChipVariant.Warning
        InsightTrend.Stable -> AeonChipVariant.Info
        InsightTrend.None -> AeonChipVariant.Ghost
    }
}


private fun insightChipVariant(
    tone: InsightTone
): AeonChipVariant {
    return when (tone) {
        InsightTone.Finance -> AeonChipVariant.Premium
        InsightTone.Health,
        InsightTone.Habit,
        InsightTone.Success -> AeonChipVariant.Success

        InsightTone.Warning -> AeonChipVariant.Warning
        InsightTone.Danger -> AeonChipVariant.Danger
        InsightTone.Focus,
        InsightTone.AI,
        InsightTone.Brand,
        InsightTone.Goal,
        InsightTone.Learning -> AeonChipVariant.Tonal

        InsightTone.Mood,
        InsightTone.Relationship -> AeonChipVariant.Info

        InsightTone.Neutral -> AeonChipVariant.Ghost
    }
}


private fun insightButtonVariant(
    tone: InsightTone
): AeonButtonVariant {
    return when (tone) {
        InsightTone.Finance -> AeonButtonVariant.Premium
        InsightTone.Health,
        InsightTone.Habit,
        InsightTone.Success -> AeonButtonVariant.Success

        InsightTone.Warning -> AeonButtonVariant.Tonal
        InsightTone.Danger -> AeonButtonVariant.Danger
        else -> AeonButtonVariant.Primary
    }
}


private fun insightProgressTone(
    tone: InsightTone
): AeonProgressTone {
    return when (tone) {
        InsightTone.Finance -> AeonProgressTone.Finance
        InsightTone.Health -> AeonProgressTone.Health
        InsightTone.Habit -> AeonProgressTone.Success
        InsightTone.Focus -> AeonProgressTone.Focus
        InsightTone.Mood -> AeonProgressTone.Mood
        InsightTone.Warning -> AeonProgressTone.Warning
        InsightTone.Danger -> AeonProgressTone.Danger
        InsightTone.Success -> AeonProgressTone.Success
        InsightTone.AI,
        InsightTone.Brand,
        InsightTone.Goal,
        InsightTone.Learning,
        InsightTone.Relationship -> AeonProgressTone.Brand

        InsightTone.Neutral -> AeonProgressTone.Neutral
    }
}


private fun insightToneSymbol(
    tone: InsightTone
): String {
    return when (tone) {
        InsightTone.Brand -> "✦"
        InsightTone.AI -> "AI"
        InsightTone.Focus -> "◉"
        InsightTone.Habit -> "✓"
        InsightTone.Health -> "+"
        InsightTone.Finance -> "₹"
        InsightTone.Mood -> "○"
        InsightTone.Goal -> "◆"
        InsightTone.Learning -> "⌁"
        InsightTone.Relationship -> "♡"
        InsightTone.Success -> "✓"
        InsightTone.Warning -> "!"
        InsightTone.Danger -> "×"
        InsightTone.Neutral -> "•"
    }
}
