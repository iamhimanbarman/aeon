package com.aeon.app.ui.components.feature

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.scale
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
import com.aeon.app.ui.theme.AeonHabitTokens
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

/*
 * AEON PREMIUM HABIT CARD
 *
 * Purpose:
 * Reusable habit tracking card for Aeon.
 *
 * Use for:
 * - Today dashboard habit preview
 * - Habit tracking screen
 * - Habit detail summaries
 * - Weekly habit consistency
 *
 * Senior UI/UX Rule:
 * A habit card should create motivation without pressure.
 * It should show completion, streak, consistency, and next action clearly.
 */


// ----------------------------------------------------
// Habit Tone
// ----------------------------------------------------

enum class HabitTone {
    Discipline,
    Health,
    Focus,
    Learning,
    Finance,
    Mood,
    Relationship,
    General
}


// ----------------------------------------------------
// Habit Status
// ----------------------------------------------------

enum class HabitStatus {
    Completed,
    Pending,
    Missed,
    Paused
}


// ----------------------------------------------------
// Habit Day State
// ----------------------------------------------------

enum class HabitDayState {
    Done,
    Missed,
    Today,
    Future,
    Empty
}


// ----------------------------------------------------
// Weekly State
// ----------------------------------------------------

@Immutable
data class HabitWeekDay(
    val label: String,
    val state: HabitDayState
)


// ----------------------------------------------------
// Main Habit Card
// ----------------------------------------------------

@Composable
fun HabitCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    status: HabitStatus = HabitStatus.Pending,
    tone: HabitTone = HabitTone.General,
    streak: Int = 0,
    successRate: Float = 0f,
    completedToday: Boolean = false,
    weeklyProgress: List<HabitWeekDay> = emptyList(),
    category: String? = null,
    reminderText: String? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onToggleComplete: (() -> Unit)? = null,
    onViewDetails: (() -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors
    val toneColor = habitToneColor(tone)
    val statusColor = habitStatusColor(status)

    val animatedScale by animateFloatAsState(
        targetValue = if (completedToday || status == HabitStatus.Completed) 1.01f else 1f,
        animationSpec = tween(
            durationMillis = AeonDuration.Medium,
            easing = AeonEasing.Emphasized
        ),
        label = "habit_card_completion_scale"
    )

    AeonCard(
        modifier = modifier
            .defaultMinSize(minHeight = 176.dp)
            .scale(animatedScale),
        variant = AeonCardVariant.Default,
        onClick = onClick,
        backgroundBrush = habitCardBrush(tone = tone, status = status),
        borderColor = statusColor.copy(alpha = 0.24f),
        contentPadding = PaddingValues(AeonHabitTokens.CardPadding)
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
                HabitIconContainer(
                    tone = tone,
                    status = status,
                    icon = icon
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = AeonTextStyles.CardTitle,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        HabitStatusBadge(status = status)
                    }

                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = AeonTextStyles.CardSubtitle,
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (category != null) {
                            AeonChip(
                                text = category,
                                variant = AeonChipVariant.Ghost,
                                size = AeonChipSize.Compact
                            )
                        }

                        if (reminderText != null) {
                            AeonChip(
                                text = reminderText,
                                variant = AeonChipVariant.Tonal,
                                size = AeonChipSize.Compact
                            )
                        }
                    }
                }
            }

            HabitProgressSection(
                streak = streak,
                successRate = successRate,
                toneColor = toneColor
            )

            if (weeklyProgress.isNotEmpty()) {
                HabitWeekProgress(
                    days = weeklyProgress,
                    tone = tone
                )
            }

            if (onToggleComplete != null || onViewDetails != null) {
                HabitActions(
                    completedToday = completedToday,
                    status = status,
                    onToggleComplete = onToggleComplete,
                    onViewDetails = onViewDetails
                )
            }
        }
    }
}


// ----------------------------------------------------
// Compact Habit Card
// ----------------------------------------------------

@Composable
fun CompactHabitCard(
    title: String,
    modifier: Modifier = Modifier,
    status: HabitStatus = HabitStatus.Pending,
    tone: HabitTone = HabitTone.General,
    streak: Int = 0,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors
    val statusColor = habitStatusColor(status)

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        onClick = onClick,
        borderColor = statusColor.copy(alpha = 0.22f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HabitIconContainer(
                tone = tone,
                status = status,
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

                Text(
                    text = if (streak > 0) "$streak day streak" else habitStatusLabel(status),
                    style = AeonTextStyles.Caption,
                    color = colors.textSecondary
                )
            }

            HabitStatusBadge(status = status)
        }
    }
}


// ----------------------------------------------------
// Habit Icon Container
// ----------------------------------------------------

@Composable
private fun HabitIconContainer(
    tone: HabitTone,
    status: HabitStatus,
    icon: (@Composable () -> Unit)?,
    compact: Boolean = false
) {
    val colors = AeonThemeTokens.colors
    val toneColor = habitToneColor(tone)
    val statusColor = habitStatusColor(status)

    val finalColor by animateColorAsState(
        targetValue = if (status == HabitStatus.Completed) statusColor else toneColor,
        animationSpec = tween(
            durationMillis = AeonDuration.Normal,
            easing = AeonEasing.Standard
        ),
        label = "habit_icon_color"
    )

    val size = if (compact) 42.dp else AeonHabitTokens.CheckContainerSize
    val iconSize = if (compact) 20.dp else AeonHabitTokens.CheckSize

    Surface(
        modifier = Modifier.size(size),
        shape = AeonComponentShapes.IconButton,
        color = finalColor.copy(alpha = if (colors.isDark) 0.16f else 0.10f),
        contentColor = finalColor,
        border = BorderStroke(
            width = 1.dp,
            color = finalColor.copy(alpha = 0.22f)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides finalColor
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
                    text = habitToneSymbol(tone, status),
                    style = AeonTextStyles.StatNumber,
                    color = finalColor
                )
            }
        }
    }
}


// ----------------------------------------------------
// Progress Section
// ----------------------------------------------------

@Composable
private fun HabitProgressSection(
    streak: Int,
    successRate: Float,
    toneColor: Color
) {
    val colors = AeonThemeTokens.colors
    val safeProgress = successRate.coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (streak > 0) "$streak days" else "No streak yet",
                    style = AeonTextStyles.StatNumber,
                    color = if (streak > 0) toneColor else colors.textPrimary
                )

                Text(
                    text = "Current streak",
                    style = AeonTextStyles.Caption,
                    color = colors.textSecondary
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${(safeProgress * 100).toInt()}%",
                    style = AeonTextStyles.StatNumber,
                    color = toneColor
                )

                Text(
                    text = "Success rate",
                    style = AeonTextStyles.Caption,
                    color = colors.textSecondary
                )
            }
        }

        AeonLinearProgress(
            progress = safeProgress,
            tone = habitProgressTone(toneColor),
            size = AeonProgressSize.Medium,
            progressColor = toneColor,
            showLabel = false
        )
    }
}


// ----------------------------------------------------
// Weekly Progress
// ----------------------------------------------------

@Composable
private fun HabitWeekProgress(
    days: List<HabitWeekDay>,
    tone: HabitTone
) {
    val colors = AeonThemeTokens.colors
    val toneColor = habitToneColor(tone)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
    ) {
        Text(
            text = "This week",
            style = AeonTextStyles.Micro,
            color = colors.textTertiary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            days.take(7).forEach { day ->
                HabitDayDot(
                    day = day,
                    toneColor = toneColor
                )
            }
        }
    }
}


@Composable
private fun HabitDayDot(
    day: HabitWeekDay,
    toneColor: Color
) {
    val colors = AeonThemeTokens.colors

    val dotColor = when (day.state) {
        HabitDayState.Done -> toneColor
        HabitDayState.Missed -> colors.error
        HabitDayState.Today -> colors.brand
        HabitDayState.Future -> colors.surfaceMuted
        HabitDayState.Empty -> colors.surfaceMuted
    }

    val textColor = when (day.state) {
        HabitDayState.Done,
        HabitDayState.Missed,
        HabitDayState.Today -> dotColor

        HabitDayState.Future,
        HabitDayState.Empty -> colors.textTertiary
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = AeonComponentShapes.IconButtonCircle,
            color = dotColor.copy(
                alpha = when (day.state) {
                    HabitDayState.Done -> 0.22f
                    HabitDayState.Missed -> 0.18f
                    HabitDayState.Today -> 0.20f
                    else -> if (colors.isDark) 0.50f else 1f
                }
            ),
            border = BorderStroke(
                width = 1.dp,
                color = dotColor.copy(alpha = 0.28f)
            )
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (day.state) {
                        HabitDayState.Done -> "✓"
                        HabitDayState.Missed -> "×"
                        HabitDayState.Today -> "•"
                        HabitDayState.Future -> ""
                        HabitDayState.Empty -> ""
                    },
                    style = AeonTextStyles.Micro,
                    color = textColor
                )
            }
        }

        Text(
            text = day.label,
            style = AeonTextStyles.Micro,
            color = colors.textTertiary
        )
    }
}


// ----------------------------------------------------
// Status Badge
// ----------------------------------------------------

@Composable
private fun HabitStatusBadge(
    status: HabitStatus
) {
    val colors = AeonThemeTokens.colors
    val statusColor = habitStatusColor(status)

    Surface(
        shape = AeonComponentShapes.Badge,
        color = statusColor.copy(alpha = if (colors.isDark) 0.16f else 0.10f),
        contentColor = statusColor,
        border = BorderStroke(
            width = 1.dp,
            color = statusColor.copy(alpha = 0.22f)
        )
    ) {
        Text(
            text = habitStatusLabel(status),
            style = AeonTextStyles.Micro,
            color = statusColor,
            modifier = Modifier.padding(
                horizontal = AeonSpacing.Small,
                vertical = AeonSpacing.XSmall
            )
        )
    }
}


// ----------------------------------------------------
// Actions
// ----------------------------------------------------

@Composable
private fun HabitActions(
    completedToday: Boolean,
    status: HabitStatus,
    onToggleComplete: (() -> Unit)?,
    onViewDetails: (() -> Unit)?
) {
    AnimatedVisibility(
        visible = onToggleComplete != null || onViewDetails != null,
        enter = fadeIn(
            animationSpec = tween(AeonDuration.Medium)
        ) + scaleIn(
            initialScale = 0.98f,
            animationSpec = tween(
                durationMillis = AeonDuration.Medium,
                easing = AeonEasing.Emphasized
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onViewDetails != null) {
                AeonButton(
                    text = "Details",
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    variant = AeonButtonVariant.Secondary,
                    size = AeonButtonSize.Medium
                )
            }

            if (onToggleComplete != null) {
                AeonButton(
                    text = if (completedToday || status == HabitStatus.Completed) {
                        "Completed"
                    } else {
                        "Mark done"
                    },
                    onClick = onToggleComplete,
                    modifier = Modifier.weight(1f),
                    variant = if (completedToday || status == HabitStatus.Completed) {
                        AeonButtonVariant.Success
                    } else {
                        AeonButtonVariant.Primary
                    },
                    size = AeonButtonSize.Medium
                )
            }
        }
    }
}


// ----------------------------------------------------
// Brush
// ----------------------------------------------------

@Composable
private fun habitCardBrush(
    tone: HabitTone,
    status: HabitStatus
): Brush {
    val colors = AeonThemeTokens.colors
    val toneColor = habitToneColor(tone)
    val statusColor = habitStatusColor(status)

    val accent = if (status == HabitStatus.Completed) statusColor else toneColor

    return Brush.linearGradient(
        colors = listOf(
            colors.surface,
            accent.copy(alpha = if (colors.isDark) 0.12f else 0.07f),
            colors.surfaceElevated
        )
    )
}


// ----------------------------------------------------
// Resolvers
// ----------------------------------------------------

@Composable
private fun habitToneColor(
    tone: HabitTone
): Color {
    val colors = AeonThemeTokens.colors

    return when (tone) {
        HabitTone.Discipline -> colors.brand
        HabitTone.Health -> colors.health
        HabitTone.Focus -> colors.focus
        HabitTone.Learning -> colors.learning
        HabitTone.Finance -> colors.finance
        HabitTone.Mood -> colors.mood
        HabitTone.Relationship -> colors.relationship
        HabitTone.General -> colors.habit
    }
}


@Composable
private fun habitStatusColor(
    status: HabitStatus
): Color {
    val colors = AeonThemeTokens.colors

    return when (status) {
        HabitStatus.Completed -> colors.success
        HabitStatus.Pending -> colors.info
        HabitStatus.Missed -> colors.error
        HabitStatus.Paused -> colors.textTertiary
    }
}


private fun habitStatusLabel(
    status: HabitStatus
): String {
    return when (status) {
        HabitStatus.Completed -> "Done"
        HabitStatus.Pending -> "Pending"
        HabitStatus.Missed -> "Missed"
        HabitStatus.Paused -> "Paused"
    }
}


private fun habitToneSymbol(
    tone: HabitTone,
    status: HabitStatus
): String {
    if (status == HabitStatus.Completed) return "✓"

    return when (tone) {
        HabitTone.Discipline -> "◆"
        HabitTone.Health -> "+"
        HabitTone.Focus -> "◉"
        HabitTone.Learning -> "⌁"
        HabitTone.Finance -> "₹"
        HabitTone.Mood -> "○"
        HabitTone.Relationship -> "♡"
        HabitTone.General -> "✓"
    }
}


private fun habitProgressTone(
    toneColor: Color
): AeonProgressTone {
    return AeonProgressTone.Success
}
