package com.aeon.app.ui.components.feature

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.theme.AeonComponentShapes
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing
import com.aeon.app.ui.theme.AeonFeatureMotion
import com.aeon.app.ui.theme.AeonLifeScoreTokens
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM LIFE SCORE CARD
 *
 * Purpose:
 * Hero card for Aeon's central daily life score.
 *
 * Senior UI/UX Role:
 * This card should become the emotional center of the app.
 * It should quickly tell the user:
 * - How their day is going
 * - Why the score is high or low
 * - What area needs attention
 * - What action can improve the day
 *
 * Use on:
 * - Today Dashboard
 * - Insights Dashboard
 * - Weekly Review
 */


// ----------------------------------------------------
// Life Score State
// ----------------------------------------------------

enum class LifeScoreState {
    Excellent,
    Good,
    Balanced,
    NeedsAttention,
    Critical
}


// ----------------------------------------------------
// Breakdown Item
// ----------------------------------------------------

data class LifeScoreBreakdownItem(
    val label: String,
    val value: String,
    val state: LifeScoreState = LifeScoreState.Balanced,
    val icon: (@Composable () -> Unit)? = null
)


// ----------------------------------------------------
// Main Life Score Card
// ----------------------------------------------------

@Composable
fun LifeScoreCard(
    score: Int,
    modifier: Modifier = Modifier,
    title: String = "Life Score",
    subtitle: String = "Today’s personal balance",
    insight: String = "Your day is balanced. Keep your focus steady and avoid unnecessary distractions.",
    nextAction: String? = "Start a 25-minute focus session",
    scoreState: LifeScoreState = resolveLifeScoreState(score),
    breakdown: List<LifeScoreBreakdownItem> = emptyList(),
    animated: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors
    val safeScore = score.coerceIn(0, 100)

    val animatedScore by animateIntAsState(
        targetValue = safeScore,
        animationSpec = if (animated) {
            tween(
                durationMillis = AeonFeatureMotion.LifeScoreCountDuration,
                easing = AeonEasing.Emphasized
            )
        } else {
            tween(durationMillis = 0)
        },
        label = "life_score_count"
    )

    val stateColor = lifeScoreStateColor(scoreState)

    AeonCard(
        modifier = modifier.defaultMinSize(minHeight = AeonLifeScoreTokens.MinHeight),
        variant = AeonCardVariant.LifeScore,
        onClick = onClick,
        backgroundBrush = lifeScoreCardBrush(scoreState),
        borderColor = stateColor.copy(alpha = 0.28f),
        contentPadding = PaddingValues(AeonLifeScoreTokens.CardPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XLarge)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Large),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LifeScoreRing(
                    score = animatedScore,
                    state = scoreState,
                    modifier = Modifier.size(AeonLifeScoreTokens.RingSize)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
                ) {
                    Text(
                        text = title,
                        style = AeonTextStyles.InsightTitle,
                        color = colors.textPrimary
                    )

                    Text(
                        text = subtitle,
                        style = AeonTextStyles.CardSubtitle,
                        color = colors.textSecondary
                    )

                    LifeScoreStatusBadge(
                        state = scoreState
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
            ) {
                Text(
                    text = "Aeon Insight",
                    style = AeonTextStyles.Micro,
                    color = stateColor
                )

                Text(
                    text = insight,
                    style = AeonTextStyles.InsightBody,
                    color = colors.textSecondary
                )
            }

            if (nextAction != null) {
                LifeScoreNextAction(
                    text = nextAction,
                    state = scoreState
                )
            }

            if (breakdown.isNotEmpty()) {
                LifeScoreBreakdown(
                    items = breakdown
                )
            }
        }
    }
}


// ----------------------------------------------------
// Compact Life Score Card
// Use inside Insights or small dashboard areas.
// ----------------------------------------------------

@Composable
fun CompactLifeScoreCard(
    score: Int,
    modifier: Modifier = Modifier,
    label: String = "Life Score",
    scoreState: LifeScoreState = resolveLifeScoreState(score),
    onClick: (() -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors
    val safeScore = score.coerceIn(0, 100)
    val stateColor = lifeScoreStateColor(scoreState)

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        onClick = onClick,
        borderColor = stateColor.copy(alpha = 0.22f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LifeScoreRing(
                score = safeScore,
                state = scoreState,
                modifier = Modifier.size(72.dp),
                compact = true
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Text(
                    text = label,
                    style = AeonTextStyles.CardTitle,
                    color = colors.textPrimary
                )

                Text(
                    text = lifeScoreStateLabel(scoreState),
                    style = AeonTextStyles.Caption,
                    color = colors.textSecondary
                )
            }
        }
    }
}


// ----------------------------------------------------
// Life Score Ring
// ----------------------------------------------------

@Composable
private fun LifeScoreRing(
    score: Int,
    state: LifeScoreState,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = AeonThemeTokens.colors
    val progress = score.coerceIn(0, 100) / 100f
    val stateColor = lifeScoreStateColor(state)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val strokeWidth = if (compact) {
                7.dp.toPx()
            } else {
                AeonLifeScoreTokens.RingStrokeWidth.toPx()
            }

            val arcSize = Size(
                width = size.width - strokeWidth,
                height = size.height - strokeWidth
            )

            val topLeft = Offset(
                x = strokeWidth / 2,
                y = strokeWidth / 2
            )

            drawArc(
                color = colors.surfaceMuted,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        stateColor.copy(alpha = 0.88f),
                        colors.brand.copy(alpha = 0.92f),
                        stateColor.copy(alpha = 0.88f)
                    )
                ),
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = score.toString(),
                style = if (compact) {
                    AeonTextStyles.StatNumber
                } else {
                    AeonTextStyles.LifeScoreNumber
                },
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            if (!compact) {
                Text(
                    text = "/100",
                    style = AeonTextStyles.Caption,
                    color = colors.textTertiary
                )
            }
        }
    }
}


// ----------------------------------------------------
// Status Badge
// ----------------------------------------------------

@Composable
private fun LifeScoreStatusBadge(
    state: LifeScoreState
) {
    val colors = AeonThemeTokens.colors
    val stateColor = lifeScoreStateColor(state)

    Surface(
        shape = AeonComponentShapes.Badge,
        color = stateColor.copy(alpha = if (colors.isDark) 0.16f else 0.10f),
        contentColor = stateColor,
        border = BorderStroke(
            width = 1.dp,
            color = stateColor.copy(alpha = 0.22f)
        )
    ) {
        Text(
            text = lifeScoreStateLabel(state),
            style = AeonTextStyles.Micro,
            color = stateColor,
            modifier = Modifier.padding(
                horizontal = AeonSpacing.Medium,
                vertical = AeonSpacing.XSmall
            )
        )
    }
}


// ----------------------------------------------------
// Next Action
// ----------------------------------------------------

@Composable
private fun LifeScoreNextAction(
    text: String,
    state: LifeScoreState
) {
    val colors = AeonThemeTokens.colors
    val stateColor = lifeScoreStateColor(state)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AeonComponentShapes.InsightCard,
        color = stateColor.copy(alpha = if (colors.isDark) 0.12f else 0.08f),
        border = BorderStroke(
            width = 1.dp,
            color = stateColor.copy(alpha = 0.18f)
        )
    ) {
        Column(
            modifier = Modifier.padding(AeonSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
        ) {
            Text(
                text = "Next Best Action",
                style = AeonTextStyles.Micro,
                color = stateColor
            )

            Text(
                text = text,
                style = AeonTextStyles.CardTitle,
                color = colors.textPrimary
            )
        }
    }
}


// ----------------------------------------------------
// Breakdown
// ----------------------------------------------------

@Composable
private fun LifeScoreBreakdown(
    items: List<LifeScoreBreakdownItem>
) {
    AnimatedVisibility(
        visible = items.isNotEmpty(),
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
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
        ) {
            items.forEach { item ->
                LifeScoreBreakdownChip(item = item)
            }
        }
    }
}


@Composable
private fun LifeScoreBreakdownChip(
    item: LifeScoreBreakdownItem
) {
    val colors = AeonThemeTokens.colors
    val stateColor = lifeScoreStateColor(item.state)

    Surface(
        shape = AeonComponentShapes.Chip,
        color = colors.surfaceMuted.copy(alpha = if (colors.isDark) 0.72f else 1f),
        contentColor = colors.textPrimary,
        border = BorderStroke(
            width = 1.dp,
            color = stateColor.copy(alpha = 0.18f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AeonSpacing.Medium,
                vertical = AeonSpacing.Small
            ),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.icon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides stateColor
                ) {
                    Box(
                        modifier = Modifier.size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        item.icon.invoke()
                    }
                }
            }

            Text(
                text = item.label,
                style = AeonTextStyles.Micro,
                color = colors.textTertiary
            )

            Text(
                text = item.value,
                style = AeonTextStyles.Micro,
                color = stateColor
            )
        }
    }
}


// ----------------------------------------------------
// Brush
// ----------------------------------------------------

@Composable
private fun lifeScoreCardBrush(
    state: LifeScoreState
): Brush {
    val colors = AeonThemeTokens.colors
    val stateColor = lifeScoreStateColor(state)

    return Brush.linearGradient(
        colors = listOf(
            colors.surfaceElevated,
            stateColor.copy(alpha = if (colors.isDark) 0.16f else 0.10f),
            colors.surface
        )
    )
}


// ----------------------------------------------------
// State Resolvers
// ----------------------------------------------------

fun resolveLifeScoreState(score: Int): LifeScoreState {
    return when (score.coerceIn(0, 100)) {
        in 86..100 -> LifeScoreState.Excellent
        in 70..85 -> LifeScoreState.Good
        in 50..69 -> LifeScoreState.Balanced
        in 30..49 -> LifeScoreState.NeedsAttention
        else -> LifeScoreState.Critical
    }
}


@Composable
private fun lifeScoreStateColor(
    state: LifeScoreState
): androidx.compose.ui.graphics.Color {
    val colors = AeonThemeTokens.colors

    return when (state) {
        LifeScoreState.Excellent -> colors.premiumGold
        LifeScoreState.Good -> colors.success
        LifeScoreState.Balanced -> colors.focus
        LifeScoreState.NeedsAttention -> colors.warning
        LifeScoreState.Critical -> colors.error
    }
}


private fun lifeScoreStateLabel(
    state: LifeScoreState
): String {
    return when (state) {
        LifeScoreState.Excellent -> "Excellent"
        LifeScoreState.Good -> "Good balance"
        LifeScoreState.Balanced -> "Balanced"
        LifeScoreState.NeedsAttention -> "Needs attention"
        LifeScoreState.Critical -> "Critical"
    }
}
