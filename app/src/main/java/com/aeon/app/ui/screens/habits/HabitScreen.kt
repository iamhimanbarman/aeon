package com.aeon.app.ui.screens.habits

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * HABIT SCREEN
 *
 * Purpose:
 * Premium habit command center for Aeon.
 *
 * Responsibilities:
 * - Show habit consistency and streak health
 * - Help user check off habits quickly
 * - Identify weak habits, strong routines, and today’s remaining habits
 * - Keep habit-building calm, visual, and non-judgmental
 *
 * Senior Developer Rule:
 * This screen is pure UI-state.
 * Real habit storage, streak calculation, reminder scheduling, and AI analysis
 * should live in ViewModel/use-cases later.
 */


// ----------------------------------------------------
// Route
// ----------------------------------------------------

@Composable
fun AeonHabitRoute(
    onAddHabit: () -> Unit = {},
    onOpenHabit: (String) -> Unit = {},
    onCompleteHabit: (String) -> Unit = {},
    onSkipHabit: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state = rememberHabitUiState()

    HabitScreen(
        state = state,
        onAddHabit = onAddHabit,
        onOpenHabit = onOpenHabit,
        onCompleteHabit = onCompleteHabit,
        onSkipHabit = onSkipHabit,
        onOpenNotifications = onOpenNotifications,
        modifier = modifier
    )
}


// ----------------------------------------------------
// State
// ----------------------------------------------------

@Immutable
data class HabitUiState(
    val dateLabel: String,
    val consistencyScore: Int = 84,
    val consistencyLabel: String = "Strong",
    val consistencyMessage: String = "Your habit system is stable. Complete the two remaining habits before night to protect your streak rhythm.",
    val heroHabit: HabitItemUi = defaultHeroHabit(),
    val metrics: List<HabitMetricUi> = defaultHabitMetrics(),
    val habits: List<HabitItemUi> = defaultHabits(),
    val weeklyRhythm: List<HabitDayUi> = defaultHabitWeek(),
    val streaks: List<HabitStreakUi> = defaultHabitStreaks(),
    val reminders: List<HabitReminderUi> = defaultHabitReminders(),
    val insight: HabitInsightUi = HabitInsightUi()
)


@Immutable
data class HabitMetricUi(
    val label: String,
    val value: String,
    val caption: String,
    val tone: HabitTone
)


@Immutable
data class HabitItemUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val streakLabel: String,
    val progress: Float,
    val status: HabitStatus,
    val difficulty: HabitDifficulty,
    val tone: HabitTone
)


@Immutable
data class HabitDayUi(
    val label: String,
    val completion: Int,
    val completed: Boolean,
    val tone: HabitTone
)


@Immutable
data class HabitStreakUi(
    val id: String,
    val title: String,
    val streak: String,
    val subtitle: String,
    val tone: HabitTone
)


@Immutable
data class HabitReminderUi(
    val id: String,
    val title: String,
    val time: String,
    val enabled: Boolean,
    val tone: HabitTone
)


@Immutable
data class HabitInsightUi(
    val title: String = "Evening is your habit risk zone",
    val body: String = "Aeon noticed that unfinished habits after evening are more likely to be skipped. Use a gentle reminder instead of a strict alert.",
    val confidence: Int = 82
)


enum class HabitFilter {
    All,
    Today,
    Pending,
    Completed,
    Strong,
    AtRisk
}


enum class HabitStatus {
    Pending,
    Done,
    Skipped,
    AtRisk
}


enum class HabitDifficulty {
    Easy,
    Medium,
    Hard
}


enum class HabitTone {
    Habit,
    Health,
    Mood,
    Study,
    Focus,
    Finance,
    Growth,
    AI,
    Warning,
    Success,
    Neutral
}


// ----------------------------------------------------
// Remember State
// ----------------------------------------------------

@Composable
fun rememberHabitUiState(): HabitUiState {
    val dateLabel = remember {
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
    }

    return remember(dateLabel) {
        HabitUiState(dateLabel = dateLabel)
    }
}


// ----------------------------------------------------
// Screen
// ----------------------------------------------------

@Composable
fun HabitScreen(
    state: HabitUiState,
    onAddHabit: () -> Unit,
    onOpenHabit: (String) -> Unit,
    onCompleteHabit: (String) -> Unit,
    onSkipHabit: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by rememberSaveable {
        mutableStateOf(HabitFilter.All)
    }

    val filteredHabits = remember(selectedFilter, state.habits) {
        state.habits.filterBy(selectedFilter)
    }

    AeonScreen(
        modifier = modifier
    ) {
        HabitHeader(
            state = state,
            onOpenNotifications = onOpenNotifications
        )

        HabitConsistencyCard(state = state)

        HabitPrimaryActions(onAddHabit = onAddHabit)

        HabitHeroCard(
            habit = state.heroHabit,
            onOpenHabit = onOpenHabit,
            onCompleteHabit = onCompleteHabit,
            onSkipHabit = onSkipHabit
        )

        HabitMetricGrid(
            metrics = state.metrics
        )

        HabitWeeklyRhythmCard(
            days = state.weeklyRhythm
        )

        HabitFilterRow(
            selectedFilter = selectedFilter,
            onSelectFilter = {
                selectedFilter = it
            }
        )

        HabitListSection(
            title = selectedFilter.sectionTitle(),
            subtitle = selectedFilter.sectionSubtitle(),
            habits = filteredHabits,
            onOpenHabit = onOpenHabit,
            onCompleteHabit = onCompleteHabit,
            onSkipHabit = onSkipHabit
        )

        HabitStreakSection(
            streaks = state.streaks,
            onOpenHabit = onOpenHabit
        )

        HabitReminderSection(
            reminders = state.reminders,
            onOpenNotifications = onOpenNotifications
        )

        HabitInsightCard(insight = state.insight)

        HabitFooter()
    }
}


// ----------------------------------------------------
// Header
// ----------------------------------------------------

@Composable
private fun HabitHeader(
    state: HabitUiState,
    onOpenNotifications: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = state.dateLabel,
        title = "Habits",
        subtitle = "A calm system for consistency, streaks, routines, and long-term personal growth.",
        size = AeonSectionHeaderSize.Hero,
        tone = AeonSectionHeaderTone.Brand,
        action = {
            AeonChip(
                text = "Reminders",
                variant = AeonChipVariant.Premium,
                size = AeonChipSize.Compact,
                onClick = onOpenNotifications
            )
        }
    )
}


// ----------------------------------------------------
// Consistency Card
// ----------------------------------------------------

@Composable
private fun HabitConsistencyCard(state: HabitUiState) {
    AeonCard(
        variant = AeonCardVariant.Hero
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HabitScoreRing(
                score = state.consistencyScore,
                label = state.consistencyLabel
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Text(
                    text = "Habit consistency",
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = state.consistencyMessage,
                    style = AeonTextStyles.CardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AeonChip(
                        text = "Streak protected",
                        variant = AeonChipVariant.Success,
                        size = AeonChipSize.Compact
                    )

                    AeonChip(
                        text = "Locally reviewed",
                        variant = AeonChipVariant.Premium,
                        size = AeonChipSize.Compact
                    )
                }
            }
        }
    }
}


@Composable
private fun HabitScoreRing(
    score: Int,
    label: String
) {
    val progress by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        label = "habit_score_progress"
    )

    val ringColor = HabitTone.Habit.color()
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    Box(
        modifier = Modifier.size(118.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(110.dp)
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


// ----------------------------------------------------
// Primary Actions
// ----------------------------------------------------

@Composable
private fun HabitPrimaryActions(onAddHabit: () -> Unit) {
    AeonButton(
        text = "+ Add habit",
        onClick = onAddHabit,
        variant = AeonButtonVariant.Primary,
        size = AeonButtonSize.Medium,
        modifier = Modifier.fillMaxWidth()
    )
}


// ----------------------------------------------------
// Hero Habit
// ----------------------------------------------------

@Composable
private fun HabitHeroCard(
    habit: HabitItemUi,
    onOpenHabit: (String) -> Unit,
    onCompleteHabit: (String) -> Unit,
    onSkipHabit: (String) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Today",
        title = "Habit to protect now",
        subtitle = "Aeon highlights the habit that matters most for today’s consistency.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Elevated,
        onClick = {
            onOpenHabit(habit.id)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.Top
        ) {
            HabitSymbolBadge(
                symbol = habit.status.symbol(),
                tone = habit.status.tone(habit.tone)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AeonChip(
                        text = habit.streakLabel,
                        variant = AeonChipVariant.Success,
                        size = AeonChipSize.Compact
                    )

                    AeonChip(
                        text = habit.difficulty.label(),
                        variant = habit.difficulty.variant(),
                        size = AeonChipSize.Compact
                    )
                }

                HabitProgressBar(
                    progress = habit.progress,
                    tone = habit.tone
                )

                AnimatedVisibility(
                    visible = habit.status != HabitStatus.Done
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
                    ) {
                        AeonButton(
                            text = "Mark done",
                            onClick = {
                                onCompleteHabit(habit.id)
                            },
                            variant = AeonButtonVariant.Primary,
                            size = AeonButtonSize.Small
                        )

                        AeonButton(
                            text = "Skip",
                            onClick = {
                                onSkipHabit(habit.id)
                            },
                            variant = AeonButtonVariant.Ghost,
                            size = AeonButtonSize.Small
                        )
                    }
                }
            }
        }
    }
}


// ----------------------------------------------------
// Metrics
// ----------------------------------------------------

@Composable
private fun HabitMetricGrid(
    metrics: List<HabitMetricUi>
) {
    AeonSectionHeader(
        eyebrow = "Snapshot",
        title = "Habit pulse",
        subtitle = "Quick view of today's consistency and streak health.",
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
                    HabitMetricCard(
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
private fun HabitMetricCard(
    metric: HabitMetricUi,
    modifier: Modifier = Modifier
) {
    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact
    ) {
        Text(
            text = metric.value,
            style = AeonTextStyles.StatNumber,
            color = metric.tone.color()
        )

        Text(
            text = metric.label,
            style = AeonTextStyles.CardTitle,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = metric.caption,
            style = AeonTextStyles.Micro,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ----------------------------------------------------
// Weekly Rhythm
// ----------------------------------------------------

@Composable
private fun HabitWeeklyRhythmCard(
    days: List<HabitDayUi>
) {
    AeonSectionHeader(
        eyebrow = "Rhythm",
        title = "Weekly habit rhythm",
        subtitle = "Consistency should feel visible, not stressful.",
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
                HabitDayColumn(day)
            }
        }
    }
}


@Composable
private fun HabitDayColumn(
    day: HabitDayUi
) {
    val progress by animateFloatAsState(
        targetValue = day.completion.coerceIn(0, 100) / 100f,
        label = "habit_day_${day.label}"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
    ) {
        Box(
            modifier = Modifier
                .size(width = 18.dp, height = 76.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((76 * progress).dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(day.tone.color())
            )
        }

        Text(
            text = day.label,
            style = AeonTextStyles.Micro,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (day.completed) {
                        HabitTone.Success.color()
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                    }
                )
        )
    }
}


// ----------------------------------------------------
// Filters
// ----------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitFilterRow(
    selectedFilter: HabitFilter,
    onSelectFilter: (HabitFilter) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "View",
        title = "Habit filters",
        subtitle = "Switch between today, pending, completed, strong, and at-risk habits.",
        size = AeonSectionHeaderSize.Medium
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
    ) {
        HabitFilter.values().forEach { filter ->
            AeonChip(
                text = filter.label(),
                variant = if (filter == selectedFilter) {
                    AeonChipVariant.Filled
                } else {
                    AeonChipVariant.Outline
                },
                size = AeonChipSize.Medium,
                onClick = {
                    onSelectFilter(filter)
                }
            )
        }
    }
}


// ----------------------------------------------------
// Habit List
// ----------------------------------------------------

@Composable
private fun HabitListSection(
    title: String,
    subtitle: String,
    habits: List<HabitItemUi>,
    onOpenHabit: (String) -> Unit,
    onCompleteHabit: (String) -> Unit,
    onSkipHabit: (String) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "List",
        title = title,
        subtitle = subtitle,
        size = AeonSectionHeaderSize.Medium
    )

    if (habits.isEmpty()) {
        AeonCard(
            variant = AeonCardVariant.Glass
        ) {
            Text(
                text = "No habits in this view.",
                style = AeonTextStyles.EmptyStateTitle,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Change filter or create a new habit when you are ready.",
                style = AeonTextStyles.EmptyStateBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        habits.forEach { habit ->
            HabitItemCard(
                habit = habit,
                onOpenHabit = onOpenHabit,
                onCompleteHabit = onCompleteHabit,
                onSkipHabit = onSkipHabit
            )
        }
    }
}


@Composable
private fun HabitItemCard(
    habit: HabitItemUi,
    onOpenHabit: (String) -> Unit,
    onCompleteHabit: (String) -> Unit,
    onSkipHabit: (String) -> Unit
) {
    AeonCard(
        variant = if (habit.status == HabitStatus.AtRisk) {
            AeonCardVariant.Insight
        } else {
            AeonCardVariant.Default
        },
        onClick = {
            onOpenHabit(habit.id)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.Top
        ) {
            HabitSymbolBadge(
                symbol = habit.status.symbol(),
                tone = habit.status.tone(habit.tone)
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
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = habit.subtitle,
                            style = AeonTextStyles.CardSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AeonChip(
                        text = habit.status.label(),
                        variant = habit.status.variant(),
                        size = AeonChipSize.Compact
                    )
                }

                HabitProgressBar(
                    progress = habit.progress,
                    tone = habit.tone
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AeonChip(
                        text = habit.category,
                        variant = AeonChipVariant.Outline,
                        size = AeonChipSize.Compact
                    )

                    AeonChip(
                        text = habit.streakLabel,
                        variant = AeonChipVariant.Success,
                        size = AeonChipSize.Compact
                    )

                    AeonChip(
                        text = habit.difficulty.label(),
                        variant = habit.difficulty.variant(),
                        size = AeonChipSize.Compact
                    )
                }

                AnimatedVisibility(
                    visible = habit.status != HabitStatus.Done
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
                    ) {
                        AeonButton(
                            text = "Done",
                            onClick = {
                                onCompleteHabit(habit.id)
                            },
                            variant = AeonButtonVariant.Primary,
                            size = AeonButtonSize.Small
                        )

                        AeonButton(
                            text = "Skip",
                            onClick = {
                                onSkipHabit(habit.id)
                            },
                            variant = AeonButtonVariant.Ghost,
                            size = AeonButtonSize.Small
                        )
                    }
                }
            }
        }
    }
}


// ----------------------------------------------------
// Streaks
// ----------------------------------------------------

@Composable
private fun HabitStreakSection(
    streaks: List<HabitStreakUi>,
    onOpenHabit: (String) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Streaks",
        title = "Strong routines",
        subtitle = "Your strongest habits are the base layer of your personal system.",
        size = AeonSectionHeaderSize.Medium
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        streaks.forEach { streak ->
            AeonCard(
                variant = AeonCardVariant.Compact,
                onClick = {
                    onOpenHabit(streak.id)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HabitSymbolBadge(
                        symbol = "🔥",
                        tone = streak.tone
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
                    ) {
                        Text(
                            text = streak.title,
                            style = AeonTextStyles.CardTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = streak.subtitle,
                            style = AeonTextStyles.CardSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AeonChip(
                        text = streak.streak,
                        variant = AeonChipVariant.Success,
                        size = AeonChipSize.Compact
                    )
                }
            }
        }
    }
}


// ----------------------------------------------------
// Reminders
// ----------------------------------------------------

@Composable
private fun HabitReminderSection(
    reminders: List<HabitReminderUi>,
    onOpenNotifications: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Reminders",
        title = "Gentle habit nudges",
        subtitle = "Habit reminders should support consistency without becoming noise.",
        size = AeonSectionHeaderSize.Medium,
        action = {
            AeonChip(
                text = "Settings",
                variant = AeonChipVariant.Outline,
                size = AeonChipSize.Compact,
                onClick = onOpenNotifications
            )
        }
    )

    AeonCard(
        variant = AeonCardVariant.Default
    ) {
        reminders.forEachIndexed { index, reminder ->
            HabitReminderRow(
                reminder = reminder,
                onOpenNotifications = onOpenNotifications
            )

            if (index != reminders.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            }
        }
    }
}


@Composable
private fun HabitReminderRow(
    reminder: HabitReminderUi,
    onOpenNotifications: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HabitSymbolBadge(
            symbol = "◈",
            tone = reminder.tone
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
        ) {
            Text(
                text = reminder.title,
                style = AeonTextStyles.CardTitle,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = reminder.time,
                style = AeonTextStyles.CardSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AeonChip(
            text = if (reminder.enabled) "On" else "Off",
            variant = if (reminder.enabled) {
                AeonChipVariant.Success
            } else {
                AeonChipVariant.Outline
            },
            size = AeonChipSize.Compact,
            onClick = onOpenNotifications
        )
    }
}


// ----------------------------------------------------
// Insight
// ----------------------------------------------------

@Composable
private fun HabitInsightCard(insight: HabitInsightUi) {
    AeonSectionHeader(
        eyebrow = "Aeon intelligence",
        title = "Habit insight",
        subtitle = "A small recommendation to protect consistency.",
        size = AeonSectionHeaderSize.Medium,
        tone = AeonSectionHeaderTone.Premium
    )

    AeonCard(
        variant = AeonCardVariant.Insight
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.Top
        ) {
            HabitSymbolBadge(
                symbol = "✦",
                tone = HabitTone.AI
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Text(
                    text = insight.title,
                    style = AeonTextStyles.InsightTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = insight.body,
                    style = AeonTextStyles.InsightBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AeonChip(
                    text = "${insight.confidence}% confidence",
                    variant = AeonChipVariant.Premium,
                    size = AeonChipSize.Compact
                )
            }
        }
    }
}


// ----------------------------------------------------
// Footer
// ----------------------------------------------------

@Composable
private fun HabitFooter() {
    AeonCard(
        variant = AeonCardVariant.Glass
    ) {
        Text(
            text = "A habit is not discipline once. It is identity repeated quietly.",
            style = AeonTextStyles.Quote,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Keep it small. Keep it visible. Repeat.",
            style = AeonTextStyles.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ----------------------------------------------------
// Shared UI
// ----------------------------------------------------

@Composable
private fun HabitSymbolBadge(
    symbol: String,
    tone: HabitTone
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
private fun HabitProgressBar(
    progress: Float,
    tone: HabitTone,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "habit_progress"
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
// Helpers
// ----------------------------------------------------

@Composable
private fun HabitTone.color(): Color {
    return when (this) {
        HabitTone.Habit -> Color(0xFF34D399)
        HabitTone.Health -> Color(0xFF10B981)
        HabitTone.Mood -> Color(0xFF60A5FA)
        HabitTone.Study -> Color(0xFF38BDF8)
        HabitTone.Focus -> MaterialTheme.colorScheme.primary
        HabitTone.Finance -> Color(0xFFF5C542)
        HabitTone.Growth -> Color(0xFF8B5CF6)
        HabitTone.AI -> Color(0xFFA78BFA)
        HabitTone.Warning -> MaterialTheme.colorScheme.error
        HabitTone.Success -> Color(0xFF34D399)
        HabitTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}


private fun HabitFilter.label(): String {
    return when (this) {
        HabitFilter.All -> "All"
        HabitFilter.Today -> "Today"
        HabitFilter.Pending -> "Pending"
        HabitFilter.Completed -> "Done"
        HabitFilter.Strong -> "Strong"
        HabitFilter.AtRisk -> "At risk"
    }
}


private fun HabitFilter.sectionTitle(): String {
    return when (this) {
        HabitFilter.All -> "All habits"
        HabitFilter.Today -> "Today’s habits"
        HabitFilter.Pending -> "Pending habits"
        HabitFilter.Completed -> "Completed habits"
        HabitFilter.Strong -> "Strong habits"
        HabitFilter.AtRisk -> "At-risk habits"
    }
}


private fun HabitFilter.sectionSubtitle(): String {
    return when (this) {
        HabitFilter.All -> "Every visible routine in your habit system."
        HabitFilter.Today -> "Habits that matter for today’s rhythm."
        HabitFilter.Pending -> "Routines still waiting for action."
        HabitFilter.Completed -> "Habits already protected today."
        HabitFilter.Strong -> "Routines with stable progress and strong streaks."
        HabitFilter.AtRisk -> "Habits that need gentle attention before the day ends."
    }
}


private fun List<HabitItemUi>.filterBy(
    filter: HabitFilter
): List<HabitItemUi> {
    return when (filter) {
        HabitFilter.All -> this
        HabitFilter.Today -> this
        HabitFilter.Pending -> filter {
            it.status == HabitStatus.Pending || it.status == HabitStatus.AtRisk
        }

        HabitFilter.Completed -> filter {
            it.status == HabitStatus.Done
        }

        HabitFilter.Strong -> filter {
            it.progress >= 0.75f && it.status != HabitStatus.AtRisk
        }

        HabitFilter.AtRisk -> filter {
            it.status == HabitStatus.AtRisk
        }
    }
}


private fun HabitStatus.symbol(): String {
    return when (this) {
        HabitStatus.Pending -> "○"
        HabitStatus.Done -> "✓"
        HabitStatus.Skipped -> "–"
        HabitStatus.AtRisk -> "!"
    }
}


private fun HabitStatus.label(): String {
    return when (this) {
        HabitStatus.Pending -> "Pending"
        HabitStatus.Done -> "Done"
        HabitStatus.Skipped -> "Skipped"
        HabitStatus.AtRisk -> "At risk"
    }
}


private fun HabitStatus.variant(): AeonChipVariant {
    return when (this) {
        HabitStatus.Pending -> AeonChipVariant.Outline
        HabitStatus.Done -> AeonChipVariant.Success
        HabitStatus.Skipped -> AeonChipVariant.Warning
        HabitStatus.AtRisk -> AeonChipVariant.Warning
    }
}


private fun HabitStatus.tone(
    fallback: HabitTone
): HabitTone {
    return when (this) {
        HabitStatus.Pending -> fallback
        HabitStatus.Done -> HabitTone.Success
        HabitStatus.Skipped -> HabitTone.Warning
        HabitStatus.AtRisk -> HabitTone.Warning
    }
}


private fun HabitDifficulty.label(): String {
    return when (this) {
        HabitDifficulty.Easy -> "Easy"
        HabitDifficulty.Medium -> "Medium"
        HabitDifficulty.Hard -> "Hard"
    }
}


private fun HabitDifficulty.variant(): AeonChipVariant {
    return when (this) {
        HabitDifficulty.Easy -> AeonChipVariant.Success
        HabitDifficulty.Medium -> AeonChipVariant.Info
        HabitDifficulty.Hard -> AeonChipVariant.Warning
    }
}


// ----------------------------------------------------
// Dummy Data
// ----------------------------------------------------

private fun defaultHeroHabit(): HabitItemUi {
    return HabitItemUi(
        id = "habit_reading",
        title = "Read for 20 minutes",
        subtitle = "Protect this habit today. It has the strongest impact on your learning rhythm.",
        category = "Learning",
        streakLabel = "12-day streak",
        progress = 0.82f,
        status = HabitStatus.AtRisk,
        difficulty = HabitDifficulty.Easy,
        tone = HabitTone.Study
    )
}


private fun defaultHabitMetrics(): List<HabitMetricUi> {
    return listOf(
        HabitMetricUi(
            label = "Today",
            value = "4/6",
            caption = "habits done",
            tone = HabitTone.Habit
        ),
        HabitMetricUi(
            label = "Streak",
            value = "12d",
            caption = "best active",
            tone = HabitTone.Success
        ),
        HabitMetricUi(
            label = "Risk",
            value = "2",
            caption = "need attention",
            tone = HabitTone.Warning
        ),
        HabitMetricUi(
            label = "Weekly",
            value = "84%",
            caption = "completion",
            tone = HabitTone.AI
        )
    )
}


private fun defaultHabits(): List<HabitItemUi> {
    return listOf(
        defaultHeroHabit(),
        HabitItemUi(
            id = "habit_walk",
            title = "Evening walk",
            subtitle = "A short walk will support focus recovery and health balance.",
            category = "Health",
            streakLabel = "4-day streak",
            progress = 0.58f,
            status = HabitStatus.Pending,
            difficulty = HabitDifficulty.Easy,
            tone = HabitTone.Health
        ),
        HabitItemUi(
            id = "habit_journal",
            title = "Night journal",
            subtitle = "A small reflection improves tomorrow’s Aeon insights.",
            category = "Mood",
            streakLabel = "9-day streak",
            progress = 1f,
            status = HabitStatus.Done,
            difficulty = HabitDifficulty.Easy,
            tone = HabitTone.Mood
        ),
        HabitItemUi(
            id = "habit_focus",
            title = "One focus block",
            subtitle = "Complete one clean session before adding more work.",
            category = "Focus",
            streakLabel = "4-day streak",
            progress = 0.75f,
            status = HabitStatus.Done,
            difficulty = HabitDifficulty.Medium,
            tone = HabitTone.Focus
        ),
        HabitItemUi(
            id = "habit_budget",
            title = "Expense review",
            subtitle = "Quickly check if today’s spending is tagged.",
            category = "Finance",
            streakLabel = "2-day streak",
            progress = 0.35f,
            status = HabitStatus.Pending,
            difficulty = HabitDifficulty.Medium,
            tone = HabitTone.Finance
        ),
        HabitItemUi(
            id = "habit_hydration",
            title = "Hydration check",
            subtitle = "Small health action. Keep it easy and visible.",
            category = "Health",
            streakLabel = "Skipped once",
            progress = 0.28f,
            status = HabitStatus.AtRisk,
            difficulty = HabitDifficulty.Easy,
            tone = HabitTone.Warning
        )
    )
}


private fun defaultHabitWeek(): List<HabitDayUi> {
    return listOf(
        HabitDayUi("M", 72, true, HabitTone.Habit),
        HabitDayUi("T", 68, true, HabitTone.Health),
        HabitDayUi("W", 86, true, HabitTone.Success),
        HabitDayUi("T", 58, false, HabitTone.Warning),
        HabitDayUi("F", 84, true, HabitTone.Habit),
        HabitDayUi("S", 76, true, HabitTone.Mood),
        HabitDayUi("S", 90, true, HabitTone.AI)
    )
}


private fun defaultHabitStreaks(): List<HabitStreakUi> {
    return listOf(
        HabitStreakUi(
            id = "habit_reading",
            title = "Reading",
            streak = "12 days",
            subtitle = "Your strongest learning habit.",
            tone = HabitTone.Study
        ),
        HabitStreakUi(
            id = "habit_journal",
            title = "Night journal",
            streak = "9 days",
            subtitle = "Stable reflection routine.",
            tone = HabitTone.Mood
        ),
        HabitStreakUi(
            id = "habit_focus",
            title = "One focus block",
            streak = "4 days",
            subtitle = "Directly supports task completion.",
            tone = HabitTone.Focus
        )
    )
}


private fun defaultHabitReminders(): List<HabitReminderUi> {
    return listOf(
        HabitReminderUi(
            id = "reminder_reading",
            title = "Reading reminder",
            time = "8:30 PM",
            enabled = true,
            tone = HabitTone.Study
        ),
        HabitReminderUi(
            id = "reminder_walk",
            title = "Evening walk reminder",
            time = "7:00 PM",
            enabled = true,
            tone = HabitTone.Health
        ),
        HabitReminderUi(
            id = "reminder_journal",
            title = "Night journal reminder",
            time = "10:30 PM",
            enabled = false,
            tone = HabitTone.Mood
        )
    )
}
