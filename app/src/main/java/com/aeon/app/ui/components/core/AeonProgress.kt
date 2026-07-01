package com.aeon.app.ui.components.core

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.theme.AeonComponentShapes
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing
import com.aeon.app.ui.theme.AeonProgressTokens
import com.aeon.app.ui.theme.AeonSize
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM PROGRESS SYSTEM
 *
 * Purpose:
 * A premium reusable progress system for Aeon.
 *
 * Use for:
 * - Life score progress
 * - Habit completion
 * - Focus session progress
 * - Goal progress
 * - Health tracking
 * - Finance budget progress
 * - Onboarding progress
 * - Loading states
 *
 * Senior UI/UX Rule:
 * Progress should create clarity, not anxiety.
 * Use calm animation, soft tracks, rounded caps, and clear labels.
 */


// ----------------------------------------------------
// Progress Tone
// ----------------------------------------------------

enum class AeonProgressTone {
    Brand,
    Premium,
    Success,
    Warning,
    Danger,
    Info,
    Focus,
    Finance,
    Health,
    Mood,
    Neutral
}


// ----------------------------------------------------
// Progress Size
// ----------------------------------------------------

enum class AeonProgressSize {
    Small,
    Medium,
    Large
}


// ----------------------------------------------------
// Progress Token
// ----------------------------------------------------

@Immutable
private data class AeonProgressToken(
    val height: Dp,
    val circularSize: Dp,
    val strokeWidth: Dp
)


// ----------------------------------------------------
// Linear Progress
// ----------------------------------------------------

@Composable
fun AeonLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    tone: AeonProgressTone = AeonProgressTone.Brand,
    size: AeonProgressSize = AeonProgressSize.Medium,
    showLabel: Boolean = false,
    label: String? = null,
    percentageText: String? = null,
    animated: Boolean = true,
    trackColor: Color? = null,
    progressColor: Color? = null
) {
    val colors = AeonThemeTokens.colors
    val token = aeonProgressToken(size)

    val resolvedProgress = progress.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = resolvedProgress,
        animationSpec = if (animated) {
            tween(
                durationMillis = AeonDuration.Slow,
                easing = AeonEasing.Emphasized
            )
        } else {
            tween(durationMillis = 0)
        },
        label = "aeon_linear_progress"
    )

    val resolvedProgressColor = progressColor ?: aeonProgressToneColor(tone)
    val resolvedTrackColor = trackColor ?: colors.surfaceMuted

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
    ) {
        if (showLabel || label != null || percentageText != null) {
            AeonProgressHeader(
                label = label,
                percentageText = percentageText ?: "${(resolvedProgress * 100).toInt()}%"
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(token.height)
                .clip(AeonComponentShapes.Chip)
                .background(resolvedTrackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(AeonComponentShapes.Chip)
                    .background(resolvedProgressColor)
            )
        }
    }
}


// ----------------------------------------------------
// Circular Progress
// ----------------------------------------------------

@Composable
fun AeonCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    tone: AeonProgressTone = AeonProgressTone.Brand,
    size: AeonProgressSize = AeonProgressSize.Medium,
    animated: Boolean = true,
    showCenterText: Boolean = true,
    centerText: String? = null,
    trackColor: Color? = null,
    progressColor: Color? = null
) {
    val colors = AeonThemeTokens.colors
    val token = aeonProgressToken(size)

    val resolvedProgress = progress.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = resolvedProgress,
        animationSpec = if (animated) {
            tween(
                durationMillis = AeonDuration.Slow,
                easing = AeonEasing.Emphasized
            )
        } else {
            tween(durationMillis = 0)
        },
        label = "aeon_circular_progress"
    )

    val resolvedProgressColor = progressColor ?: aeonProgressToneColor(tone)
    val resolvedTrackColor = trackColor ?: colors.surfaceMuted

    Box(
        modifier = modifier.size(token.circularSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(token.circularSize)
        ) {
            val strokePx = token.strokeWidth.toPx()
            val canvasSize = this.size
            val arcSize = Size(
                width = canvasSize.width - strokePx,
                height = canvasSize.height - strokePx
            )

            val topLeft = Offset(
                x = strokePx / 2,
                y = strokePx / 2
            )

            drawArc(
                color = resolvedTrackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = resolvedProgressColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round
                )
            )
        }

        if (showCenterText) {
            Text(
                text = centerText ?: "${(resolvedProgress * 100).toInt()}%",
                style = when (size) {
                    AeonProgressSize.Small -> AeonTextStyles.Caption
                    AeonProgressSize.Medium -> AeonTextStyles.StatNumber
                    AeonProgressSize.Large -> AeonTextStyles.HeroMetric
                },
                color = colors.textPrimary
            )
        }
    }
}


// ----------------------------------------------------
// Indeterminate Linear Loading
// ----------------------------------------------------

@Composable
fun AeonIndeterminateLinearProgress(
    modifier: Modifier = Modifier,
    tone: AeonProgressTone = AeonProgressTone.Brand,
    size: AeonProgressSize = AeonProgressSize.Medium,
    trackColor: Color? = null,
    progressColor: Color? = null
) {
    val colors = AeonThemeTokens.colors
    val token = aeonProgressToken(size)

    val resolvedProgressColor = progressColor ?: aeonProgressToneColor(tone)
    val resolvedTrackColor = trackColor ?: colors.surfaceMuted

    val infiniteTransition = rememberInfiniteTransition(
        label = "aeon_indeterminate_linear_transition"
    )

    val start by infiniteTransition.animateFloat(
        initialValue = -0.45f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "aeon_indeterminate_start"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(token.height)
            .clip(AeonComponentShapes.Chip)
            .background(resolvedTrackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    translationX = this.size.width * start
                }
                .clip(AeonComponentShapes.Chip)
                .background(resolvedProgressColor)
        )
    }
}


// ----------------------------------------------------
// Segmented Progress
// Good for onboarding, weekly habits, steps, milestones.
// ----------------------------------------------------

@Composable
fun AeonSegmentedProgress(
    totalSegments: Int,
    completedSegments: Int,
    modifier: Modifier = Modifier,
    tone: AeonProgressTone = AeonProgressTone.Brand,
    size: AeonProgressSize = AeonProgressSize.Medium,
    gap: Dp = AeonSpacing.Small,
    animated: Boolean = true
) {
    val colors = AeonThemeTokens.colors
    val token = aeonProgressToken(size)
    val activeColor = aeonProgressToneColor(tone)
    val inactiveColor = colors.surfaceMuted

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSegments.coerceAtLeast(1)) { index ->
            val isCompleted = index < completedSegments

            val alpha by animateFloatAsState(
                targetValue = if (isCompleted) 1f else 0.38f,
                animationSpec = if (animated) {
                    tween(
                        durationMillis = AeonDuration.Normal,
                        easing = AeonEasing.Standard
                    )
                } else {
                    tween(durationMillis = 0)
                },
                label = "aeon_segment_alpha"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(token.height)
                    .clip(AeonComponentShapes.Chip)
                    .background(
                        if (isCompleted) {
                            activeColor.copy(alpha = alpha)
                        } else {
                            inactiveColor
                        }
                    )
            )
        }
    }
}


// ----------------------------------------------------
// Progress With Label Row
// ----------------------------------------------------

@Composable
fun AeonProgressWithLabel(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    tone: AeonProgressTone = AeonProgressTone.Brand,
    size: AeonProgressSize = AeonProgressSize.Medium,
    valueText: String = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%"
) {
    AeonLinearProgress(
        progress = progress,
        modifier = modifier,
        tone = tone,
        size = size,
        showLabel = true,
        label = label,
        percentageText = valueText
    )
}


// ----------------------------------------------------
// Header
// ----------------------------------------------------

@Composable
private fun AeonProgressHeader(
    label: String?,
    percentageText: String
) {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label != null) {
            Text(
                text = label,
                style = AeonTextStyles.Caption,
                color = colors.textSecondary
            )
        } else {
            Spacer(modifier = Modifier.width(AeonSpacing.None))
        }

        Text(
            text = percentageText,
            style = AeonTextStyles.Micro,
            color = colors.textTertiary
        )
    }
}


// ----------------------------------------------------
// Token Resolver
// ----------------------------------------------------

private fun aeonProgressToken(
    size: AeonProgressSize
): AeonProgressToken {
    return when (size) {
        AeonProgressSize.Small -> AeonProgressToken(
            height = 6.dp,
            circularSize = AeonProgressTokens.CircularSizeSmall,
            strokeWidth = AeonProgressTokens.StrokeWidthSmall
        )

        AeonProgressSize.Medium -> AeonProgressToken(
            height = AeonSize.ProgressBarHeight,
            circularSize = AeonProgressTokens.CircularSizeMedium,
            strokeWidth = AeonProgressTokens.StrokeWidthMedium
        )

        AeonProgressSize.Large -> AeonProgressToken(
            height = AeonSize.ProgressBarHeightLarge,
            circularSize = AeonProgressTokens.CircularSizeLarge,
            strokeWidth = AeonProgressTokens.StrokeWidthLarge
        )
    }
}


// ----------------------------------------------------
// Tone Resolver
// ----------------------------------------------------

@Composable
private fun aeonProgressToneColor(
    tone: AeonProgressTone
): Color {
    val colors = AeonThemeTokens.colors

    return when (tone) {
        AeonProgressTone.Brand -> colors.brand
        AeonProgressTone.Premium -> colors.premiumGold
        AeonProgressTone.Success -> colors.success
        AeonProgressTone.Warning -> colors.warning
        AeonProgressTone.Danger -> colors.error
        AeonProgressTone.Info -> colors.info
        AeonProgressTone.Focus -> colors.focus
        AeonProgressTone.Finance -> colors.finance
        AeonProgressTone.Health -> colors.health
        AeonProgressTone.Mood -> colors.mood
        AeonProgressTone.Neutral -> colors.textTertiary
    }
}
