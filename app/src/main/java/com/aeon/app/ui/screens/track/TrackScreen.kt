package com.aeon.app.ui.screens.track

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.aeon.app.ui.components.core.AeonSectionHeader
import com.aeon.app.ui.components.core.AeonSectionHeaderSize
import com.aeon.app.ui.components.core.AeonSectionHeaderTone
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.components.layout.AeonScreenConfig
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * TRACK SCREEN
 *
 * Purpose:
 * Premium life-tracking dashboard for Aeon.
 *
 * Responsibilities:
 * - Show life-domain balance
 * - Track habits, focus, mood, health, finance, goals, and learning
 * - Give clear trend signals without overwhelming the user
 * - Surface what is improving, declining, and stable
 *
 * Senior Developer Rule:
 * This screen is pure UI-state.
 * It does not directly call database, repository, notification engine, AI engine, or NavController.
 */


// ----------------------------------------------------
// Route
// ----------------------------------------------------

@Composable
fun AeonTrackRoute(
    onAddEntry: () -> Unit = {},
    onOpenHabit: (String) -> Unit = {},
    onOpenGoal: (String) -> Unit = {},
    onOpenInsight: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state = rememberTrackUiState()

    TrackScreen(
        state = state,
        onAddEntry = onAddEntry,
        onOpenHabit = onOpenHabit,
        onOpenGoal = onOpenGoal,
        onOpenInsight = onOpenInsight,
        onOpenNotifications = onOpenNotifications,
        modifier = modifier
    )
}


// ----------------------------------------------------
// State
// ----------------------------------------------------

@Immutable
data class TrackUiState(
    val dateLabel: String,
    val rangeLabel: String = "Last 7 days",
    val lifeBalanceScore: Int = 81,
    val lifeBalanceLabel: String = "Strong balance",
    val lifeBalanceMessage: String = "Your habits and focus are improving. Health needs a little more consistency.",
    val metrics: List<TrackMetricUi> = defaultTrackMetrics(),
    val domains: List<TrackDomainUi> = defaultTrackDomains(),
    val weeklyRhythm: List<TrackDayUi> = defaultWeeklyRhythm(),
    val habits: List<TrackHabitUi> = defaultTrackHabits(),
    val goals: List<TrackGoalUi> = defaultTrackGoals(),
    val signals: List<TrackSignalUi> = defaultTrackSignals()
)


@Immutable
data class TrackMetricUi(
    val label: String,
    val value: String,
    val caption: String,
    val trend: TrackTrend,
    val tone: TrackTone
)


@Immutable
data class TrackDomainUi(
    val title: String,
    val subtitle: String,
    val score: Int,
    val delta: String,
    val tone: TrackTone
)


@Immutable
data class TrackDayUi(
    val label: String,
    val score: Int,
    val completed: Boolean,
    val tone: TrackTone
)


@Immutable
data class TrackHabitUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val progress: Float,
    val streak: String,
    val tone: TrackTone
)


@Immutable
data class TrackGoalUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val progress: Float,
    val dueLabel: String,
    val tone: TrackTone
)


@Immutable
data class TrackSignalUi(
    val id: String,
    val title: String,
    val body: String,
    val label: String,
    val tone: TrackTone
)


enum class TrackTrend {
    Up,
    Down,
    Stable
}


enum class TrackTone {
    Brand,
    Focus,
    Habit,
    Health,
    Mood,
    Finance,
    Learning,
    Goal,
    AI,
    Warning,
    Success,
    Neutral
}


// ----------------------------------------------------
// Remember State
// ----------------------------------------------------

@Composable
fun rememberTrackUiState(): TrackUiState {
    val dateLabel = remember {
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
    }

    return remember(dateLabel) {
        TrackUiState(
            dateLabel = dateLabel
        )
    }
}


// ----------------------------------------------------
// Screen
// ----------------------------------------------------

@Composable
fun TrackScreen(
    state: TrackUiState,
    onAddEntry: () -> Unit,
    onOpenHabit: (String) -> Unit,
    onOpenGoal: (String) -> Unit,
    onOpenInsight: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    AeonScreen(
        modifier = modifier,
        config = AeonScreenConfig(safeDrawing = false)
    ) {
        TrackHeader(state = state)

        TrackBalanceCard(state = state)

        TrackPrimaryActions(
            onAddEntry = onAddEntry,
            onOpenNotifications = onOpenNotifications
        )

        TrackMetricGrid(
            metrics = state.metrics
        )

        TrackWeeklyRhythmCard(
            days = state.weeklyRhythm
        )

        TrackDomainSection(
            domains = state.domains
        )

        TrackHabitSection(
            habits = state.habits,
            onOpenHabit = onOpenHabit
        )

        TrackGoalSection(
            goals = state.goals,
            onOpenGoal = onOpenGoal
        )

        TrackSignalSection(
            signals = state.signals,
            onOpenInsight = onOpenInsight
        )

        TrackFooter()
    }
}


// ----------------------------------------------------
// Header
// ----------------------------------------------------

@Composable
private fun TrackHeader(state: TrackUiState) {
    AeonSectionHeader(
        eyebrow = state.dateLabel,
        title = "Track",
        subtitle = "Understand your life patterns across focus, habits, mood, health, finance, and goals.",
        size = AeonSectionHeaderSize.Hero,
        tone = AeonSectionHeaderTone.Brand,
        action = {
            AeonChip(
                text = state.rangeLabel,
                variant = AeonChipVariant.Premium,
                size = AeonChipSize.Compact
            )
        }
    )
}


// ----------------------------------------------------
// Balance Card
// ----------------------------------------------------

@Composable
private fun TrackBalanceCard(state: TrackUiState) {
    AeonCard(
        variant = AeonCardVariant.Hero
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackScoreRing(
                score = state.lifeBalanceScore,
                label = state.lifeBalanceLabel
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Text(
                    text = "Life balance",
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = state.lifeBalanceMessage,
                    style = AeonTextStyles.CardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowStatusChips()
            }
        }
    }
}


@Composable
private fun TrackScoreRing(
    score: Int,
    label: String
) {
    val progress by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        label = "track_balance_progress"
    )

    val ringColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    Box(
        modifier = Modifier.size(116.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(108.dp)
        ) {
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(
                    width = 9.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(
                    width = 9.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = score.toString(),
                style = AeonTextStyles.LifeScoreNumber,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = label,
                style = AeonTextStyles.Micro,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun FlowStatusChips() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AeonChip(
            text = "7-day view",
            variant = AeonChipVariant.Info,
            size = AeonChipSize.Compact
        )

        AeonChip(
            text = "AI ready",
            variant = AeonChipVariant.Premium,
            size = AeonChipSize.Compact
        )
    }
}


// ----------------------------------------------------
// Primary Actions
// ----------------------------------------------------

@Composable
private fun TrackPrimaryActions(
    onAddEntry: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
    ) {
        AeonButton(
            text = "+ Add entry",
            onClick = onAddEntry,
            variant = AeonButtonVariant.Primary,
            size = AeonButtonSize.Medium,
            modifier = Modifier.weight(1f)
        )

        AeonButton(
            text = "Reminders",
            onClick = onOpenNotifications,
            variant = AeonButtonVariant.Secondary,
            size = AeonButtonSize.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}


// ----------------------------------------------------
// Metrics
// ----------------------------------------------------

@Composable
private fun TrackMetricGrid(
    metrics: List<TrackMetricUi>
) {
    AeonSectionHeader(
        eyebrow = "Overview",
        title = "Current signals",
        subtitle = "A quick reading of your personal operating system.",
        size = AeonSectionHeaderSize.Medium
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        metrics.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
            ) {
                row.forEach { metric ->
                    TrackMetricCard(
                        metric = metric,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


@Composable
private fun TrackMetricCard(
    metric: TrackMetricUi,
    modifier: Modifier = Modifier
) {
    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
            ) {
                Text(
                    text = metric.value,
                    style = AeonTextStyles.StatNumber,
                    color = metric.tone.color()
                )

                Text(
                    text = metric.label,
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = metric.caption,
                    style = AeonTextStyles.Micro,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AeonChip(
                text = metric.trend.label(),
                variant = metric.trend.chipVariant(),
                size = AeonChipSize.Compact
            )
        }
    }
}


// ----------------------------------------------------
// Weekly Rhythm
// ----------------------------------------------------

@Composable
private fun TrackWeeklyRhythmCard(
    days: List<TrackDayUi>
) {
    AeonSectionHeader(
        eyebrow = "Rhythm",
        title = "Weekly consistency",
        subtitle = "Your best progress comes from stable repetition, not intensity spikes.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Default
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { day ->
                TrackDayColumn(day)
            }
        }
    }
}


@Composable
private fun TrackDayColumn(
    day: TrackDayUi
) {
    val progress by animateFloatAsState(
        targetValue = day.score.coerceIn(0, 100) / 100f,
        label = "track_day_${day.label}"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
    ) {
        Box(
            modifier = Modifier
                .height(72.dp)
                .size(width = 18.dp, height = 72.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((72 * progress).dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(day.tone.color())
            )
        }

        Text(
            text = day.label,
            style = AeonTextStyles.Micro,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TrackCompletionDot(
            completed = day.completed,
            tone = day.tone
        )
    }
}


@Composable
private fun TrackCompletionDot(
    completed: Boolean,
    tone: TrackTone
) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(
                if (completed) {
                    tone.color()
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                }
            )
    )
}


// ----------------------------------------------------
// Domains
// ----------------------------------------------------

@Composable
private fun TrackDomainSection(
    domains: List<TrackDomainUi>
) {
    AeonSectionHeader(
        eyebrow = "Domains",
        title = "Life balance map",
        subtitle = "Each domain should be strong enough to support the others.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Default
    ) {
        domains.forEachIndexed { index, domain ->
            TrackDomainRow(domain)

            if (index != domains.lastIndex) {
                Spacer(modifier = Modifier.height(AeonSpacing.Medium))
            }
        }
    }
}


@Composable
private fun TrackDomainRow(
    domain: TrackDomainUi
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
            ) {
                Text(
                    text = domain.title,
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = domain.subtitle,
                    style = AeonTextStyles.CardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${domain.score}%",
                    style = AeonTextStyles.Caption,
                    color = domain.tone.color()
                )

                Text(
                    text = domain.delta,
                    style = AeonTextStyles.Micro,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        TrackProgressBar(
            progress = domain.score / 100f,
            tone = domain.tone
        )
    }
}


// ----------------------------------------------------
// Habits
// ----------------------------------------------------

@Composable
private fun TrackHabitSection(
    habits: List<TrackHabitUi>,
    onOpenHabit: (String) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Habits",
        title = "Consistency engine",
        subtitle = "Track the small routines that shape your long-term direction.",
        size = AeonSectionHeaderSize.Medium
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        habits.forEach { habit ->
            TrackHabitCard(
                habit = habit,
                onOpenHabit = onOpenHabit
            )
        }
    }
}


@Composable
private fun TrackHabitCard(
    habit: TrackHabitUi,
    onOpenHabit: (String) -> Unit
) {
    AeonCard(
        variant = AeonCardVariant.Compact,
        onClick = {
            onOpenHabit(habit.id)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackSymbolBadge(
                symbol = "◎",
                tone = habit.tone
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
                    ) {
                        Text(
                            text = habit.title,
                            style = AeonTextStyles.CardTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = habit.subtitle,
                            style = AeonTextStyles.CardSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AeonChip(
                        text = habit.streak,
                        variant = AeonChipVariant.Success,
                        size = AeonChipSize.Compact
                    )
                }

                TrackProgressBar(
                    progress = habit.progress,
                    tone = habit.tone
                )
            }
        }
    }
}


// ----------------------------------------------------
// Goals
// ----------------------------------------------------

@Composable
private fun TrackGoalSection(
    goals: List<TrackGoalUi>,
    onOpenGoal: (String) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Goals",
        title = "Long-term progress",
        subtitle = "Goals stay useful when progress is visible and measurable.",
        size = AeonSectionHeaderSize.Medium
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        goals.forEach { goal ->
            TrackGoalCard(
                goal = goal,
                onOpenGoal = onOpenGoal
            )
        }
    }
}


@Composable
private fun TrackGoalCard(
    goal: TrackGoalUi,
    onOpenGoal: (String) -> Unit
) {
    AeonCard(
        variant = AeonCardVariant.Elevated,
        onClick = {
            onOpenGoal(goal.id)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackMiniRing(
                progress = goal.progress,
                tone = goal.tone
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Text(
                    text = goal.title,
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = goal.subtitle,
                    style = AeonTextStyles.CardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AeonChip(
                        text = "${(goal.progress * 100).toInt()}%",
                        variant = AeonChipVariant.Info,
                        size = AeonChipSize.Compact
                    )

                    AeonChip(
                        text = goal.dueLabel,
                        variant = AeonChipVariant.Outline,
                        size = AeonChipSize.Compact
                    )
                }
            }
        }
    }
}


@Composable
private fun TrackMiniRing(
    progress: Float,
    tone: TrackTone
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "track_goal_ring"
    )

    val ringColor = tone.color()
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    Box(
        modifier = Modifier.size(58.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(54.dp)
        ) {
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(
                    width = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = animated * 360f,
                useCenter = false,
                style = Stroke(
                    width = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        Text(
            text = "${(animated * 100).toInt()}",
            style = AeonTextStyles.Micro,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


// ----------------------------------------------------
// Signals
// ----------------------------------------------------

@Composable
private fun TrackSignalSection(
    signals: List<TrackSignalUi>,
    onOpenInsight: (String) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Aeon intelligence",
        title = "Tracking insights",
        subtitle = "Readable signals from your recent patterns.",
        size = AeonSectionHeaderSize.Medium,
        tone = AeonSectionHeaderTone.Premium
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        signals.forEach { signal ->
            AeonCard(
                variant = AeonCardVariant.Insight,
                onClick = {
                    onOpenInsight(signal.id)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
                    verticalAlignment = Alignment.Top
                ) {
                    TrackSymbolBadge(
                        symbol = "✦",
                        tone = signal.tone
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = signal.title,
                                style = AeonTextStyles.InsightTitle,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            AeonChip(
                                text = signal.label,
                                variant = AeonChipVariant.Premium,
                                size = AeonChipSize.Compact
                            )
                        }

                        Text(
                            text = signal.body,
                            style = AeonTextStyles.InsightBody,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


// ----------------------------------------------------
// Footer
// ----------------------------------------------------

@Composable
private fun TrackFooter() {
    AeonCard(
        variant = AeonCardVariant.Glass
    ) {
        Text(
            text = "Tracking is not for pressure. It is for awareness.",
            style = AeonTextStyles.Quote,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Measure gently. Improve consistently.",
            style = AeonTextStyles.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ----------------------------------------------------
// Shared UI
// ----------------------------------------------------

@Composable
private fun TrackSymbolBadge(
    symbol: String,
    tone: TrackTone
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(tone.color().copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = AeonTextStyles.CardTitle,
            color = tone.color()
        )
    }
}


@Composable
private fun TrackProgressBar(
    progress: Float,
    tone: TrackTone,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "track_progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(tone.color())
        )
    }
}


// ----------------------------------------------------
// UI Helpers
// ----------------------------------------------------

@Composable
private fun TrackTone.color(): Color {
    return when (this) {
        TrackTone.Brand -> MaterialTheme.colorScheme.primary
        TrackTone.Focus -> MaterialTheme.colorScheme.tertiary
        TrackTone.Habit -> Color(0xFF34D399)
        TrackTone.Health -> Color(0xFF10B981)
        TrackTone.Mood -> Color(0xFF60A5FA)
        TrackTone.Finance -> Color(0xFFF5C542)
        TrackTone.Learning -> Color(0xFF38BDF8)
        TrackTone.Goal -> Color(0xFF8B5CF6)
        TrackTone.AI -> Color(0xFFA78BFA)
        TrackTone.Warning -> MaterialTheme.colorScheme.error
        TrackTone.Success -> Color(0xFF34D399)
        TrackTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}


private fun TrackTrend.label(): String {
    return when (this) {
        TrackTrend.Up -> "Up"
        TrackTrend.Down -> "Down"
        TrackTrend.Stable -> "Stable"
    }
}


private fun TrackTrend.chipVariant(): AeonChipVariant {
    return when (this) {
        TrackTrend.Up -> AeonChipVariant.Success
        TrackTrend.Down -> AeonChipVariant.Warning
        TrackTrend.Stable -> AeonChipVariant.Outline
    }
}


// ----------------------------------------------------
// Dummy Data
// ----------------------------------------------------

private fun defaultTrackMetrics(): List<TrackMetricUi> {
    return listOf(
        TrackMetricUi(
            label = "Focus",
            value = "4h 20m",
            caption = "this week",
            trend = TrackTrend.Up,
            tone = TrackTone.Focus
        ),
        TrackMetricUi(
            label = "Habits",
            value = "84%",
            caption = "completion",
            trend = TrackTrend.Up,
            tone = TrackTone.Habit
        ),
        TrackMetricUi(
            label = "Mood",
            value = "Calm",
            caption = "average state",
            trend = TrackTrend.Stable,
            tone = TrackTone.Mood
        ),
        TrackMetricUi(
            label = "Spend",
            value = "₹820",
            caption = "tracked today",
            trend = TrackTrend.Down,
            tone = TrackTone.Finance
        )
    )
}


private fun defaultTrackDomains(): List<TrackDomainUi> {
    return listOf(
        TrackDomainUi(
            title = "Focus",
            subtitle = "Deep work, study, attention, and distraction control.",
            score = 78,
            delta = "+12%",
            tone = TrackTone.Focus
        ),
        TrackDomainUi(
            title = "Habits",
            subtitle = "Consistency across repeatable routines and personal discipline.",
            score = 84,
            delta = "+8%",
            tone = TrackTone.Habit
        ),
        TrackDomainUi(
            title = "Health",
            subtitle = "Sleep, movement, hydration, medicine, and body awareness.",
            score = 66,
            delta = "-4%",
            tone = TrackTone.Health
        ),
        TrackDomainUi(
            title = "Mood",
            subtitle = "Emotional state, reflection, journaling, and mental clarity.",
            score = 76,
            delta = "stable",
            tone = TrackTone.Mood
        ),
        TrackDomainUi(
            title = "Finance",
            subtitle = "Expense tracking, budget attention, and spending awareness.",
            score = 71,
            delta = "+3%",
            tone = TrackTone.Finance
        )
    )
}


private fun defaultWeeklyRhythm(): List<TrackDayUi> {
    return listOf(
        TrackDayUi("M", 72, true, TrackTone.Focus),
        TrackDayUi("T", 64, true, TrackTone.Habit),
        TrackDayUi("W", 81, true, TrackTone.Success),
        TrackDayUi("T", 58, false, TrackTone.Warning),
        TrackDayUi("F", 77, true, TrackTone.Focus),
        TrackDayUi("S", 69, true, TrackTone.Mood),
        TrackDayUi("S", 84, true, TrackTone.AI)
    )
}


private fun defaultTrackHabits(): List<TrackHabitUi> {
    return listOf(
        TrackHabitUi(
            id = "habit_reading",
            title = "Reading",
            subtitle = "5 of 7 days completed",
            progress = 0.71f,
            streak = "12 days",
            tone = TrackTone.Habit
        ),
        TrackHabitUi(
            id = "habit_walk",
            title = "Evening walk",
            subtitle = "4 of 7 days completed",
            progress = 0.57f,
            streak = "4 days",
            tone = TrackTone.Health
        ),
        TrackHabitUi(
            id = "habit_journal",
            title = "Night journal",
            subtitle = "6 of 7 days completed",
            progress = 0.86f,
            streak = "9 days",
            tone = TrackTone.Mood
        )
    )
}


private fun defaultTrackGoals(): List<TrackGoalUi> {
    return listOf(
        TrackGoalUi(
            id = "goal_aeon_mvp",
            title = "Build Aeon MVP",
            subtitle = "Core architecture, UI system, and notification engine.",
            progress = 0.62f,
            dueLabel = "This month",
            tone = TrackTone.Goal
        ),
        TrackGoalUi(
            id = "goal_exam_revision",
            title = "Exam preparation",
            subtitle = "Keep revision consistent with focused blocks.",
            progress = 0.48f,
            dueLabel = "Ongoing",
            tone = TrackTone.Learning
        )
    )
}


private fun defaultTrackSignals(): List<TrackSignalUi> {
    return listOf(
        TrackSignalUi(
            id = "insight_focus_window",
            title = "Focus improves when habits are completed early",
            body = "Your strongest days happen when habit completion crosses 70% before evening.",
            label = "Pattern",
            tone = TrackTone.AI
        ),
        TrackSignalUi(
            id = "insight_health_balance",
            title = "Health is the weakest active domain",
            body = "A small walk and hydration reminder may lift your overall balance score tomorrow.",
            label = "Health",
            tone = TrackTone.Health
        )
    )
}
