package com.aeon.app.ui.components.feature

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.theme.AeonComponentShapes
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing
import com.aeon.app.ui.theme.AeonFeatureMotion
import com.aeon.app.ui.theme.AeonFocusTokens
import com.aeon.app.ui.theme.AeonMotionValue
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM FOCUS TIMER
 *
 * Purpose:
 * Premium focus timer component for Aeon's Focus screen and Today dashboard.
 *
 * Use for:
 * - Deep work sessions
 * - Study sessions
 * - Reading sessions
 * - Meditation/focus reset
 * - Break timers
 *
 * Senior UI/UX Rule:
 * Focus UI should feel calm, controlled, distraction-free, and intentional.
 * The timer must be visually dominant, but not aggressive.
 */


// ----------------------------------------------------
// Timer State
// ----------------------------------------------------

enum class FocusTimerState {
    Idle,
    Running,
    Paused,
    Completed
}


// ----------------------------------------------------
// Focus Mode Tone
// ----------------------------------------------------

enum class FocusTimerTone {
    DeepWork,
    Study,
    Reading,
    Meditation,
    Break,
    Custom
}


// ----------------------------------------------------
// Focus Stat
// ----------------------------------------------------

@Immutable
data class FocusTimerStat(
    val label: String,
    val value: String,
    val tone: FocusTimerTone = FocusTimerTone.Custom
)


// ----------------------------------------------------
// Main Focus Timer Card
// ----------------------------------------------------

@Composable
fun FocusTimer(
    totalSeconds: Int,
    remainingSeconds: Int,
    modifier: Modifier = Modifier,
    state: FocusTimerState = FocusTimerState.Idle,
    tone: FocusTimerTone = FocusTimerTone.DeepWork,
    title: String = "Deep Focus",
    subtitle: String = "Protect this session from distractions",
    modeLabel: String = "Focus Mode",
    stats: List<FocusTimerStat> = emptyList(),
    distractionCount: Int? = null,
    showControls: Boolean = true,
    compact: Boolean = false,
    onStart: (() -> Unit)? = null,
    onPause: (() -> Unit)? = null,
    onResume: (() -> Unit)? = null,
    onStop: (() -> Unit)? = null,
    onReset: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors
    val accentColor = focusTimerToneColor(tone)

    val safeTotal = totalSeconds.coerceAtLeast(1)
    val safeRemaining = remainingSeconds.coerceIn(0, safeTotal)
    val progress = 1f - (safeRemaining.toFloat() / safeTotal.toFloat())

    AeonCard(
        modifier = modifier.defaultMinSize(
            minHeight = if (compact) 260.dp else 420.dp
        ),
        variant = AeonCardVariant.Hero,
        onClick = onClick,
        backgroundBrush = focusTimerBrush(tone = tone, state = state),
        borderColor = accentColor.copy(alpha = 0.26f),
        contentPadding = PaddingValues(
            if (compact) AeonSpacing.Large else AeonSpacing.XLarge
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(
                if (compact) AeonSpacing.Large else AeonSpacing.XLarge
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FocusTimerHeader(
                title = title,
                subtitle = subtitle,
                modeLabel = modeLabel,
                tone = tone,
                state = state,
                distractionCount = distractionCount
            )

            FocusTimerRing(
                remainingSeconds = safeRemaining,
                progress = progress,
                state = state,
                tone = tone,
                compact = compact
            )

            if (stats.isNotEmpty()) {
                FocusTimerStats(
                    stats = stats.take(3)
                )
            }

            if (showControls) {
                FocusTimerControls(
                    state = state,
                    onStart = onStart,
                    onPause = onPause,
                    onResume = onResume,
                    onStop = onStop,
                    onReset = onReset
                )
            }
        }
    }
}


// ----------------------------------------------------
// Compact Focus Timer Card
// ----------------------------------------------------

@Composable
fun CompactFocusTimer(
    totalSeconds: Int,
    remainingSeconds: Int,
    modifier: Modifier = Modifier,
    state: FocusTimerState = FocusTimerState.Idle,
    tone: FocusTimerTone = FocusTimerTone.DeepWork,
    title: String = "Focus Session",
    onClick: (() -> Unit)? = null
) {
    val accentColor = focusTimerToneColor(tone)
    val safeTotal = totalSeconds.coerceAtLeast(1)
    val safeRemaining = remainingSeconds.coerceIn(0, safeTotal)
    val progress = 1f - (safeRemaining.toFloat() / safeTotal.toFloat())

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        onClick = onClick,
        borderColor = accentColor.copy(alpha = 0.24f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FocusTimerRing(
                remainingSeconds = safeRemaining,
                progress = progress,
                state = state,
                tone = tone,
                compact = true,
                ringSize = 78.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Text(
                    text = title,
                    style = AeonTextStyles.CardTitle,
                    color = AeonThemeTokens.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = focusTimerStateLabel(state),
                    style = AeonTextStyles.Caption,
                    color = accentColor
                )
            }
        }
    }
}


// ----------------------------------------------------
// Header
// ----------------------------------------------------

@Composable
private fun FocusTimerHeader(
    title: String,
    subtitle: String,
    modeLabel: String,
    tone: FocusTimerTone,
    state: FocusTimerState,
    distractionCount: Int?
) {
    val colors = AeonThemeTokens.colors
    val accentColor = focusTimerToneColor(tone)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AeonChip(
                text = modeLabel,
                variant = focusTimerChipVariant(tone),
                size = AeonChipSize.Compact
            )

            AeonChip(
                text = focusTimerStateLabel(state),
                variant = focusTimerStateChipVariant(state),
                size = AeonChipSize.Compact
            )

            Spacer(modifier = Modifier.weight(1f))

            if (distractionCount != null) {
                AeonChip(
                    text = "$distractionCount distractions",
                    variant = if (distractionCount == 0) {
                        AeonChipVariant.Success
                    } else {
                        AeonChipVariant.Warning
                    },
                    size = AeonChipSize.Compact
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
        ) {
            Text(
                text = title,
                style = AeonTextStyles.InsightTitle,
                color = colors.textPrimary
            )

            Text(
                text = subtitle,
                style = AeonTextStyles.InsightBody,
                color = colors.textSecondary
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AeonComponentShapes.InsightCard,
            color = accentColor.copy(alpha = if (colors.isDark) 0.12f else 0.08f),
            border = BorderStroke(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.18f)
            )
        ) {
            Text(
                text = focusTimerGuidance(state = state, tone = tone),
                style = AeonTextStyles.Caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(AeonSpacing.Medium)
            )
        }
    }
}


// ----------------------------------------------------
// Timer Ring
// ----------------------------------------------------

@Composable
private fun FocusTimerRing(
    remainingSeconds: Int,
    progress: Float,
    state: FocusTimerState,
    tone: FocusTimerTone,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    ringSize: Dp = if (compact) {
        AeonFocusTokens.CompactTimerSize
    } else {
        AeonFocusTokens.TimerSize
    }
) {
    val colors = AeonThemeTokens.colors
    val accentColor = focusTimerToneColor(tone)
    val safeProgress = progress.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = safeProgress,
        animationSpec = tween(
            durationMillis = AeonFeatureMotion.FocusSessionStartDuration,
            easing = AeonEasing.Emphasized
        ),
        label = "focus_timer_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(
        label = "focus_timer_pulse_transition"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = AeonMotionValue.FocusPulseMinScale,
        targetValue = AeonMotionValue.FocusPulseMaxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AeonFocusTokens.PulseDuration,
                easing = AeonEasing.Standard
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "focus_timer_pulse_scale"
    )

    val appliedScale = if (state == FocusTimerState.Running && !compact) {
        pulseScale
    } else {
        1f
    }

    Box(
        modifier = modifier
            .size(ringSize)
            .graphicsLayer {
                scaleX = appliedScale
                scaleY = appliedScale
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(ringSize)
        ) {
            val strokeWidth = if (compact) {
                8.dp.toPx()
            } else {
                AeonFocusTokens.TimerStrokeWidth.toPx()
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
                        accentColor.copy(alpha = 0.95f),
                        colors.brand.copy(alpha = 0.92f),
                        colors.calm.copy(alpha = 0.86f),
                        accentColor.copy(alpha = 0.95f)
                    )
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }

        Surface(
            modifier = Modifier.size(
                if (compact) ringSize * 0.68f else ringSize * 0.70f
            ),
            shape = AeonComponentShapes.FocusTimerContainer,
            color = colors.surfaceElevated.copy(alpha = if (colors.isDark) 0.92f else 0.96f),
            border = BorderStroke(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.18f)
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    if (compact) AeonSpacing.Small else AeonSpacing.Medium
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatFocusTime(remainingSeconds),
                    style = if (compact) {
                        AeonTextStyles.StatNumber
                    } else {
                        AeonTextStyles.HeroMetric
                    },
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                if (!compact) {
                    Spacer(modifier = Modifier.height(AeonSpacing.XSmall))

                    Text(
                        text = focusTimerStateLabel(state),
                        style = AeonTextStyles.Caption,
                        color = accentColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}


// ----------------------------------------------------
// Stats
// ----------------------------------------------------

@Composable
private fun FocusTimerStats(
    stats: List<FocusTimerStat>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stats.forEach { stat ->
            FocusTimerStatTile(
                stat = stat,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
private fun FocusTimerStatTile(
    stat: FocusTimerStat,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val accentColor = focusTimerToneColor(stat.tone)

    Surface(
        modifier = modifier,
        shape = AeonComponentShapes.CardSmall,
        color = colors.surfaceMuted.copy(alpha = if (colors.isDark) 0.70f else 1f),
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
                text = stat.value,
                style = AeonTextStyles.StatNumber,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = stat.label,
                style = AeonTextStyles.Micro,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


// ----------------------------------------------------
// Controls
// ----------------------------------------------------

@Composable
private fun FocusTimerControls(
    state: FocusTimerState,
    onStart: (() -> Unit)?,
    onPause: (() -> Unit)?,
    onResume: (() -> Unit)?,
    onStop: (() -> Unit)?,
    onReset: (() -> Unit)?
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(
            animationSpec = tween(AeonDuration.Medium)
        ),
        exit = fadeOut(
            animationSpec = tween(AeonDuration.Fast)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
        ) {
            when (state) {
                FocusTimerState.Idle -> {
                    if (onStart != null) {
                        AeonButton(
                            text = "Start Focus",
                            onClick = onStart,
                            variant = AeonButtonVariant.Primary,
                            size = AeonButtonSize.Large,
                            fullWidth = true
                        )
                    }
                }

                FocusTimerState.Running -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onPause != null) {
                            AeonButton(
                                text = "Pause",
                                onClick = onPause,
                                modifier = Modifier.weight(1f),
                                variant = AeonButtonVariant.Secondary,
                                size = AeonButtonSize.Medium
                            )
                        }

                        if (onStop != null) {
                            AeonButton(
                                text = "Stop",
                                onClick = onStop,
                                modifier = Modifier.weight(1f),
                                variant = AeonButtonVariant.Danger,
                                size = AeonButtonSize.Medium
                            )
                        }
                    }
                }

                FocusTimerState.Paused -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onResume != null) {
                            AeonButton(
                                text = "Resume",
                                onClick = onResume,
                                modifier = Modifier.weight(1f),
                                variant = AeonButtonVariant.Primary,
                                size = AeonButtonSize.Medium
                            )
                        }

                        if (onStop != null) {
                            AeonButton(
                                text = "End",
                                onClick = onStop,
                                modifier = Modifier.weight(1f),
                                variant = AeonButtonVariant.Secondary,
                                size = AeonButtonSize.Medium
                            )
                        }
                    }
                }

                FocusTimerState.Completed -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onReset != null) {
                            AeonButton(
                                text = "Reset",
                                onClick = onReset,
                                modifier = Modifier.weight(1f),
                                variant = AeonButtonVariant.Secondary,
                                size = AeonButtonSize.Medium
                            )
                        }

                        if (onStart != null) {
                            AeonButton(
                                text = "Start Again",
                                onClick = onStart,
                                modifier = Modifier.weight(1f),
                                variant = AeonButtonVariant.Success,
                                size = AeonButtonSize.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}


// ----------------------------------------------------
// Background Brush
// ----------------------------------------------------

@Composable
private fun focusTimerBrush(
    tone: FocusTimerTone,
    state: FocusTimerState
): Brush {
    val colors = AeonThemeTokens.colors
    val accentColor = focusTimerToneColor(tone)
    val stateColor = focusTimerStateColor(state)

    return Brush.radialGradient(
        colors = listOf(
            accentColor.copy(alpha = if (colors.isDark) 0.18f else 0.10f),
            stateColor.copy(alpha = if (colors.isDark) 0.10f else 0.06f),
            colors.surface,
            colors.surfaceElevated
        )
    )
}


// ----------------------------------------------------
// Resolvers
// ----------------------------------------------------

@Composable
private fun focusTimerToneColor(
    tone: FocusTimerTone
): Color {
    val colors = AeonThemeTokens.colors

    return when (tone) {
        FocusTimerTone.DeepWork -> colors.focus
        FocusTimerTone.Study -> colors.learning
        FocusTimerTone.Reading -> colors.brand
        FocusTimerTone.Meditation -> colors.calm
        FocusTimerTone.Break -> colors.health
        FocusTimerTone.Custom -> colors.focus
    }
}


@Composable
private fun focusTimerStateColor(
    state: FocusTimerState
): Color {
    val colors = AeonThemeTokens.colors

    return when (state) {
        FocusTimerState.Idle -> colors.textTertiary
        FocusTimerState.Running -> colors.focus
        FocusTimerState.Paused -> colors.warning
        FocusTimerState.Completed -> colors.success
    }
}


private fun focusTimerStateLabel(
    state: FocusTimerState
): String {
    return when (state) {
        FocusTimerState.Idle -> "Ready"
        FocusTimerState.Running -> "Running"
        FocusTimerState.Paused -> "Paused"
        FocusTimerState.Completed -> "Completed"
    }
}


private fun focusTimerGuidance(
    state: FocusTimerState,
    tone: FocusTimerTone
): String {
    return when (state) {
        FocusTimerState.Idle -> when (tone) {
            FocusTimerTone.DeepWork -> "Choose one important task. Keep your phone away before starting."
            FocusTimerTone.Study -> "Prepare your study material first. Start only when your desk is clear."
            FocusTimerTone.Reading -> "Keep the session calm. Read slowly and avoid switching apps."
            FocusTimerTone.Meditation -> "Sit comfortably. Let the session begin without pressure."
            FocusTimerTone.Break -> "Use this break to recover, not to fall into scrolling."
            FocusTimerTone.Custom -> "Start with intention. One clear session is enough."
        }

        FocusTimerState.Running -> "Stay with this one session. Do not switch context unless it is truly necessary."
        FocusTimerState.Paused -> "You paused the session. Resume only when you are ready to focus again."
        FocusTimerState.Completed -> "Good work. Record how it felt before starting another session."
    }
}


private fun focusTimerChipVariant(
    tone: FocusTimerTone
): AeonChipVariant {
    return when (tone) {
        FocusTimerTone.DeepWork -> AeonChipVariant.Info
        FocusTimerTone.Study -> AeonChipVariant.Tonal
        FocusTimerTone.Reading -> AeonChipVariant.Ghost
        FocusTimerTone.Meditation -> AeonChipVariant.Success
        FocusTimerTone.Break -> AeonChipVariant.Success
        FocusTimerTone.Custom -> AeonChipVariant.Tonal
    }
}


private fun focusTimerStateChipVariant(
    state: FocusTimerState
): AeonChipVariant {
    return when (state) {
        FocusTimerState.Idle -> AeonChipVariant.Ghost
        FocusTimerState.Running -> AeonChipVariant.Info
        FocusTimerState.Paused -> AeonChipVariant.Warning
        FocusTimerState.Completed -> AeonChipVariant.Success
    }
}


private fun formatFocusTime(
    totalSeconds: Int
): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60

    return "%02d:%02d".format(minutes, seconds)
}
