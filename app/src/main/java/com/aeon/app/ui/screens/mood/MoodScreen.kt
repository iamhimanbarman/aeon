package com.aeon.app.ui.screens.mood

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
import androidx.compose.ui.graphics.Path
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
 * MOOD SCREEN
 *
 * Purpose:
 * Premium emotional check-in and mood intelligence screen for Aeon.
 *
 * Responsibilities:
 * - Help user log mood quickly
 * - Show mood stability, emotional trend, and recent check-ins
 * - Surface mood factors, journal prompts, and AI emotional insights
 * - Keep the experience calm, private, non-judgmental, and easy to use
 *
 * Senior Developer Rule:
 * This screen is UI-state driven.
 * Real mood entries, journal storage, sentiment analysis, reminders, and AI insight
 * generation should live in ViewModel/use-cases later.
 */


// ----------------------------------------------------
// Route
// ----------------------------------------------------

@Composable
fun AeonMoodRoute(
    onAddMoodEntry: () -> Unit = {},
    onSaveMood: (MoodOptionUi) -> Unit = {},
    onOpenMoodEntry: (String) -> Unit = {},
    onOpenJournalPrompt: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state = rememberMoodUiState()

    MoodScreen(
        state = state,
        onAddMoodEntry = onAddMoodEntry,
        onSaveMood = onSaveMood,
        onOpenMoodEntry = onOpenMoodEntry,
        onOpenJournalPrompt = onOpenJournalPrompt,
        onOpenNotifications = onOpenNotifications,
        modifier = modifier
    )
}


// ----------------------------------------------------
// State
// ----------------------------------------------------

@Immutable
data class MoodUiState(
    val dateLabel: String,
    val moodScore: Int = 76,
    val moodLabel: String = "Calm",
    val moodMessage: String = "Your mood is stable today. Keep the evening light and avoid task overload before sleep.",
    val selectedMood: MoodOptionUi = defaultMoodOptions().first(),
    val metrics: List<MoodMetricUi> = defaultMoodMetrics(),
    val moodOptions: List<MoodOptionUi> = defaultMoodOptions(),
    val weeklyTrend: List<MoodDayUi> = defaultMoodWeek(),
    val entries: List<MoodEntryUi> = defaultMoodEntries(),
    val factors: List<MoodFactorUi> = defaultMoodFactors(),
    val prompts: List<MoodJournalPromptUi> = defaultMoodPrompts(),
    val insight: MoodInsightUi = MoodInsightUi()
)


@Immutable
data class MoodMetricUi(
    val label: String,
    val value: String,
    val caption: String,
    val tone: MoodTone
)


@Immutable
data class MoodOptionUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val symbol: String,
    val intensity: Int,
    val tone: MoodTone
)


@Immutable
data class MoodDayUi(
    val label: String,
    val score: Int,
    val logged: Boolean,
    val tone: MoodTone
)


@Immutable
data class MoodEntryUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val time: String,
    val score: Int,
    val factors: List<String>,
    val tone: MoodTone
)


@Immutable
data class MoodFactorUi(
    val title: String,
    val subtitle: String,
    val impact: MoodImpact,
    val tone: MoodTone
)


@Immutable
data class MoodJournalPromptUi(
    val id: String,
    val title: String,
    val body: String,
    val label: String,
    val tone: MoodTone
)


@Immutable
data class MoodInsightUi(
    val title: String = "Your mood improves when the day closes gently",
    val body: String = "Aeon noticed better stability on days when evening tasks are reduced and reflection happens before sleep.",
    val confidence: Int = 84
)


enum class MoodFilter {
    All,
    Positive,
    Neutral,
    Low,
    Journaled
}


enum class MoodImpact {
    Positive,
    Negative,
    Neutral
}


enum class MoodTone {
    Calm,
    Happy,
    Focused,
    Tired,
    Stressed,
    Sad,
    Energy,
    Sleep,
    Health,
    AI,
    Warning,
    Success,
    Neutral
}


// ----------------------------------------------------
// Remember State
// ----------------------------------------------------

@Composable
fun rememberMoodUiState(): MoodUiState {
    val dateLabel = remember {
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
    }

    return remember(dateLabel) {
        MoodUiState(dateLabel = dateLabel)
    }
}


// ----------------------------------------------------
// Screen
// ----------------------------------------------------

@Composable
fun MoodScreen(
    state: MoodUiState,
    onAddMoodEntry: () -> Unit,
    onSaveMood: (MoodOptionUi) -> Unit,
    onOpenMoodEntry: (String) -> Unit,
    onOpenJournalPrompt: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMood by rememberSaveable {
        mutableStateOf(state.selectedMood.id)
    }

    var selectedFilter by rememberSaveable {
        mutableStateOf(MoodFilter.All)
    }

    val selectedMoodItem = remember(selectedMood, state.moodOptions) {
        state.moodOptions.firstOrNull { it.id == selectedMood } ?: state.selectedMood
    }

    val filteredEntries = remember(selectedFilter, state.entries) {
        state.entries.filterBy(selectedFilter)
    }

    AeonScreen(
        modifier = modifier
    ) {
        MoodHeader(
            state = state,
            onOpenNotifications = onOpenNotifications
        )

        MoodStabilityCard(state = state)

        MoodPrimaryActions(onAddMoodEntry = onAddMoodEntry)

        MoodCheckInCard(
            selectedMood = selectedMoodItem,
            options = state.moodOptions,
            onSelectMood = { mood ->
                selectedMood = mood.id
            },
            onSaveMood = {
                onSaveMood(selectedMoodItem)
            }
        )

        MoodMetricGrid(
            metrics = state.metrics
        )

        MoodTrendCard(
            days = state.weeklyTrend
        )

        MoodFactorSection(
            factors = state.factors
        )

        MoodFilterRow(
            selectedFilter = selectedFilter,
            onSelectFilter = {
                selectedFilter = it
            }
        )

        MoodEntrySection(
            title = selectedFilter.sectionTitle(),
            subtitle = selectedFilter.sectionSubtitle(),
            entries = filteredEntries,
            onOpenMoodEntry = onOpenMoodEntry
        )

        MoodJournalPromptSection(
            prompts = state.prompts,
            onOpenJournalPrompt = onOpenJournalPrompt
        )

        MoodInsightCard(insight = state.insight)

        MoodFooter()
    }
}


// ----------------------------------------------------
// Header
// ----------------------------------------------------

@Composable
private fun MoodHeader(
    state: MoodUiState,
    onOpenNotifications: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = state.dateLabel,
        title = "Mood",
        subtitle = "A private space for emotional awareness, reflection, and calm self-understanding.",
        size = AeonSectionHeaderSize.Hero,
        tone = AeonSectionHeaderTone.Brand,
        action = {
            AeonChip(
                text = "Check-in reminder",
                variant = AeonChipVariant.Premium,
                size = AeonChipSize.Compact,
                onClick = onOpenNotifications
            )
        }
    )
}


// ----------------------------------------------------
// Stability Card
// ----------------------------------------------------

@Composable
private fun MoodStabilityCard(state: MoodUiState) {
    AeonCard(
        variant = AeonCardVariant.Hero
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoodScoreRing(
                score = state.moodScore,
                label = state.moodLabel
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Text(
                    text = "Mood stability",
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = state.moodMessage,
                    style = AeonTextStyles.CardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AeonChip(
                        text = "Stable",
                        variant = AeonChipVariant.Success,
                        size = AeonChipSize.Compact
                    )

                    AeonChip(
                        text = "Private",
                        variant = AeonChipVariant.Premium,
                        size = AeonChipSize.Compact
                    )
                }
            }
        }
    }
}


@Composable
private fun MoodScoreRing(
    score: Int,
    label: String
) {
    val progress by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        label = "mood_score_progress"
    )

    val ringColor = MoodTone.Calm.color()
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
private fun MoodPrimaryActions(onAddMoodEntry: () -> Unit) {
    AeonButton(
        text = "+ Add entry",
        onClick = onAddMoodEntry,
        variant = AeonButtonVariant.Primary,
        size = AeonButtonSize.Medium,
        modifier = Modifier.fillMaxWidth()
    )
}


// ----------------------------------------------------
// Mood Check-in
// ----------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodCheckInCard(
    selectedMood: MoodOptionUi,
    options: List<MoodOptionUi>,
    onSelectMood: (MoodOptionUi) -> Unit,
    onSaveMood: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Check-in",
        title = "How are you feeling?",
        subtitle = "Keep it simple. One tap is enough for emotional awareness.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Elevated
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoodSymbolBadge(
                symbol = selectedMood.symbol,
                tone = selectedMood.tone,
                size = 58
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
            ) {
                Text(
                    text = selectedMood.title,
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = selectedMood.subtitle,
                    style = AeonTextStyles.CardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AeonChip(
                    text = "Intensity ${selectedMood.intensity}/10",
                    variant = AeonChipVariant.Info,
                    size = AeonChipSize.Compact
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
        ) {
            options.forEach { option ->
                AeonChip(
                    text = "${option.symbol} ${option.title}",
                    variant = if (option.id == selectedMood.id) {
                        AeonChipVariant.Filled
                    } else {
                        AeonChipVariant.Outline
                    },
                    size = AeonChipSize.Medium,
                    onClick = {
                        onSelectMood(option)
                    }
                )
            }
        }

        AeonButton(
            text = "Save mood check-in",
            onClick = onSaveMood,
            variant = AeonButtonVariant.Primary,
            size = AeonButtonSize.Medium,
            fullWidth = true
        )
    }
}


// ----------------------------------------------------
// Metrics
// ----------------------------------------------------

@Composable
private fun MoodMetricGrid(
    metrics: List<MoodMetricUi>
) {
    AeonSectionHeader(
        eyebrow = "Snapshot",
        title = "Mood pulse",
        subtitle = "A compact emotional overview without pressure.",
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
                    MoodMetricCard(
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
private fun MoodMetricCard(
    metric: MoodMetricUi,
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
// Trend
// ----------------------------------------------------

@Composable
private fun MoodTrendCard(
    days: List<MoodDayUi>
) {
    AeonSectionHeader(
        eyebrow = "Trend",
        title = "Weekly mood rhythm",
        subtitle = "Aeon shows emotional direction without judging good or bad days.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Default
    ) {
        MoodLineChart(
            points = days.map { it.score },
            tone = MoodTone.Calm
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { day ->
                MoodDayColumn(day)
            }
        }
    }
}


@Composable
private fun MoodLineChart(
    points: List<Int>,
    tone: MoodTone
) {
    val color = tone.color()
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        if (points.size < 2) return@Canvas

        val min = 0
        val max = 100
        val range = max - min
        val stepX = size.width / points.lastIndex.coerceAtLeast(1)

        repeat(4) { index ->
            val y = size.height * (index + 1) / 5f

            drawLine(
                color = trackColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val path = Path()

        points.forEachIndexed { index, value ->
            val x = stepX * index
            val normalized = (value - min).toFloat() / range.toFloat()
            val y = size.height - (normalized * size.height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        points.forEachIndexed { index, value ->
            val x = stepX * index
            val normalized = (value - min).toFloat() / range.toFloat()
            val y = size.height - (normalized * size.height)

            drawCircle(
                color = color,
                radius = if (index == points.lastIndex) 5.dp.toPx() else 3.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}


@Composable
private fun MoodDayColumn(
    day: MoodDayUi
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
    ) {
        Text(
            text = day.label,
            style = AeonTextStyles.Micro,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = day.score.toString(),
            style = AeonTextStyles.Micro,
            color = day.tone.color()
        )

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (day.logged) {
                        day.tone.color()
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                    }
                )
        )
    }
}


// ----------------------------------------------------
// Factors
// ----------------------------------------------------

@Composable
private fun MoodFactorSection(
    factors: List<MoodFactorUi>
) {
    AeonSectionHeader(
        eyebrow = "Factors",
        title = "What affects mood",
        subtitle = "Small signals that may explain emotional movement.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Default
    ) {
        factors.forEachIndexed { index, factor ->
            MoodFactorRow(factor)

            if (index != factors.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            }
        }
    }
}


@Composable
private fun MoodFactorRow(
    factor: MoodFactorUi
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MoodSymbolBadge(
            symbol = factor.impact.symbol(),
            tone = factor.tone
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
        ) {
            Text(
                text = factor.title,
                style = AeonTextStyles.CardTitle,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = factor.subtitle,
                style = AeonTextStyles.CardSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AeonChip(
            text = factor.impact.label(),
            variant = factor.impact.variant(),
            size = AeonChipSize.Compact
        )
    }
}


// ----------------------------------------------------
// Filter
// ----------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodFilterRow(
    selectedFilter: MoodFilter,
    onSelectFilter: (MoodFilter) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "View",
        title = "Mood filters",
        subtitle = "Review entries by emotional state and journal activity.",
        size = AeonSectionHeaderSize.Medium
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
    ) {
        MoodFilter.values().forEach { filter ->
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
// Entries
// ----------------------------------------------------

@Composable
private fun MoodEntrySection(
    title: String,
    subtitle: String,
    entries: List<MoodEntryUi>,
    onOpenMoodEntry: (String) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "History",
        title = title,
        subtitle = subtitle,
        size = AeonSectionHeaderSize.Medium
    )

    if (entries.isEmpty()) {
        AeonCard(
            variant = AeonCardVariant.Glass
        ) {
            Text(
                text = "No mood entries in this view.",
                style = AeonTextStyles.EmptyStateTitle,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Change filter or create a new check-in when you are ready.",
                style = AeonTextStyles.EmptyStateBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        entries.forEach { entry ->
            MoodEntryCard(
                entry = entry,
                onOpenMoodEntry = onOpenMoodEntry
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodEntryCard(
    entry: MoodEntryUi,
    onOpenMoodEntry: (String) -> Unit
) {
    AeonCard(
        variant = AeonCardVariant.Default,
        onClick = {
            onOpenMoodEntry(entry.id)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.Top
        ) {
            MoodMiniRing(
                score = entry.score,
                tone = entry.tone
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
                            text = entry.title,
                            style = AeonTextStyles.CardTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = entry.subtitle,
                            style = AeonTextStyles.CardSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = entry.time,
                        style = AeonTextStyles.Micro,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall),
                    verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
                ) {
                    entry.factors.forEach { factor ->
                        AeonChip(
                            text = factor,
                            variant = AeonChipVariant.Outline,
                            size = AeonChipSize.Compact
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun MoodMiniRing(
    score: Int,
    tone: MoodTone
) {
    val progress by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        label = "mood_entry_ring"
    )

    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val toneColor = tone.color()

    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(52.dp)
        ) {
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(
                    width = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = toneColor,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(
                    width = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        Text(
            text = score.toString(),
            style = AeonTextStyles.Micro,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


// ----------------------------------------------------
// Journal Prompts
// ----------------------------------------------------

@Composable
private fun MoodJournalPromptSection(
    prompts: List<MoodJournalPromptUi>,
    onOpenJournalPrompt: (String) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Journal",
        title = "Reflection prompts",
        subtitle = "Short prompts that help convert emotions into clarity.",
        size = AeonSectionHeaderSize.Medium
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        prompts.forEach { prompt ->
            AeonCard(
                variant = AeonCardVariant.Compact,
                onClick = {
                    onOpenJournalPrompt(prompt.id)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
                    verticalAlignment = Alignment.Top
                ) {
                    MoodSymbolBadge(
                        symbol = "✎",
                        tone = prompt.tone
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
                    ) {
                        Text(
                            text = prompt.title,
                            style = AeonTextStyles.CardTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = prompt.body,
                            style = AeonTextStyles.CardSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AeonChip(
                            text = prompt.label,
                            variant = AeonChipVariant.Info,
                            size = AeonChipSize.Compact
                        )
                    }
                }
            }
        }
    }
}


// ----------------------------------------------------
// Insight
// ----------------------------------------------------

@Composable
private fun MoodInsightCard(insight: MoodInsightUi) {
    AeonSectionHeader(
        eyebrow = "Aeon intelligence",
        title = "Mood insight",
        subtitle = "A private observation from your emotional rhythm.",
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
            MoodSymbolBadge(
                symbol = "✦",
                tone = MoodTone.AI
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
private fun MoodFooter() {
    AeonCard(
        variant = AeonCardVariant.Glass
    ) {
        Text(
            text = "Mood tracking is not about fixing every feeling. It is about noticing yourself clearly.",
            style = AeonTextStyles.Quote,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Observe gently. Reflect honestly. Continue calmly.",
            style = AeonTextStyles.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ----------------------------------------------------
// Shared UI
// ----------------------------------------------------

@Composable
private fun MoodSymbolBadge(
    symbol: String,
    tone: MoodTone,
    size: Int = 42
) {
    Box(
        modifier = Modifier
            .size(size.dp)
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


// ----------------------------------------------------
// Helpers
// ----------------------------------------------------

@Composable
private fun MoodTone.color(): Color {
    return when (this) {
        MoodTone.Calm -> Color(0xFF2DD4BF)
        MoodTone.Happy -> Color(0xFF34D399)
        MoodTone.Focused -> MaterialTheme.colorScheme.primary
        MoodTone.Tired -> Color(0xFF94A3B8)
        MoodTone.Stressed -> Color(0xFFF97316)
        MoodTone.Sad -> Color(0xFF60A5FA)
        MoodTone.Energy -> Color(0xFFF5C542)
        MoodTone.Sleep -> Color(0xFF818CF8)
        MoodTone.Health -> Color(0xFF10B981)
        MoodTone.AI -> Color(0xFFA78BFA)
        MoodTone.Warning -> MaterialTheme.colorScheme.error
        MoodTone.Success -> Color(0xFF34D399)
        MoodTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}


private fun MoodFilter.label(): String {
    return when (this) {
        MoodFilter.All -> "All"
        MoodFilter.Positive -> "Positive"
        MoodFilter.Neutral -> "Neutral"
        MoodFilter.Low -> "Low"
        MoodFilter.Journaled -> "Journaled"
    }
}


private fun MoodFilter.sectionTitle(): String {
    return when (this) {
        MoodFilter.All -> "Mood history"
        MoodFilter.Positive -> "Positive entries"
        MoodFilter.Neutral -> "Neutral entries"
        MoodFilter.Low -> "Low mood entries"
        MoodFilter.Journaled -> "Journaled entries"
    }
}


private fun MoodFilter.sectionSubtitle(): String {
    return when (this) {
        MoodFilter.All -> "Recent emotional check-ins and reflection notes."
        MoodFilter.Positive -> "Entries with calm, happy, focused, or stable mood."
        MoodFilter.Neutral -> "Balanced entries without strong emotional movement."
        MoodFilter.Low -> "Entries that may need gentler attention."
        MoodFilter.Journaled -> "Entries connected with reflection and writing."
    }
}


private fun List<MoodEntryUi>.filterBy(
    filter: MoodFilter
): List<MoodEntryUi> {
    return when (filter) {
        MoodFilter.All -> this
        MoodFilter.Positive -> filter { it.score >= 75 }
        MoodFilter.Neutral -> filter { it.score in 55..74 }
        MoodFilter.Low -> filter { it.score < 55 }
        MoodFilter.Journaled -> filter {
            it.factors.any { factor ->
                factor.contains("Journal", ignoreCase = true) ||
                    factor.contains("Reflection", ignoreCase = true)
            }
        }
    }
}


private fun MoodImpact.label(): String {
    return when (this) {
        MoodImpact.Positive -> "Positive"
        MoodImpact.Negative -> "Negative"
        MoodImpact.Neutral -> "Neutral"
    }
}


private fun MoodImpact.symbol(): String {
    return when (this) {
        MoodImpact.Positive -> "+"
        MoodImpact.Negative -> "!"
        MoodImpact.Neutral -> "○"
    }
}


private fun MoodImpact.variant(): AeonChipVariant {
    return when (this) {
        MoodImpact.Positive -> AeonChipVariant.Success
        MoodImpact.Negative -> AeonChipVariant.Warning
        MoodImpact.Neutral -> AeonChipVariant.Outline
    }
}


// ----------------------------------------------------
// Dummy Data
// ----------------------------------------------------

private fun defaultMoodOptions(): List<MoodOptionUi> {
    return listOf(
        MoodOptionUi(
            id = "calm",
            title = "Calm",
            subtitle = "Stable and clear enough to continue gently.",
            symbol = "◌",
            intensity = 7,
            tone = MoodTone.Calm
        ),
        MoodOptionUi(
            id = "happy",
            title = "Happy",
            subtitle = "Positive, light, and emotionally open.",
            symbol = "☺",
            intensity = 8,
            tone = MoodTone.Happy
        ),
        MoodOptionUi(
            id = "focused",
            title = "Focused",
            subtitle = "Mentally directed and ready for deep work.",
            symbol = "◎",
            intensity = 8,
            tone = MoodTone.Focused
        ),
        MoodOptionUi(
            id = "tired",
            title = "Tired",
            subtitle = "Low energy. Recovery may matter more than output.",
            symbol = "☾",
            intensity = 5,
            tone = MoodTone.Tired
        ),
        MoodOptionUi(
            id = "stressed",
            title = "Stressed",
            subtitle = "Pressure is high. Reduce noise and simplify.",
            symbol = "!",
            intensity = 6,
            tone = MoodTone.Stressed
        ),
        MoodOptionUi(
            id = "sad",
            title = "Low",
            subtitle = "Emotionally heavy. Be softer with yourself.",
            symbol = "◇",
            intensity = 4,
            tone = MoodTone.Sad
        )
    )
}


private fun defaultMoodMetrics(): List<MoodMetricUi> {
    return listOf(
        MoodMetricUi(
            label = "Current",
            value = "Calm",
            caption = "last check-in",
            tone = MoodTone.Calm
        ),
        MoodMetricUi(
            label = "Stability",
            value = "76",
            caption = "mood score",
            tone = MoodTone.Success
        ),
        MoodMetricUi(
            label = "Entries",
            value = "5",
            caption = "this week",
            tone = MoodTone.AI
        ),
        MoodMetricUi(
            label = "Risk",
            value = "Low",
            caption = "today",
            tone = MoodTone.Health
        )
    )
}


private fun defaultMoodWeek(): List<MoodDayUi> {
    return listOf(
        MoodDayUi("M", 68, true, MoodTone.Calm),
        MoodDayUi("T", 72, true, MoodTone.Focused),
        MoodDayUi("W", 58, true, MoodTone.Tired),
        MoodDayUi("T", 63, false, MoodTone.Neutral),
        MoodDayUi("F", 78, true, MoodTone.Happy),
        MoodDayUi("S", 74, true, MoodTone.Calm),
        MoodDayUi("S", 76, true, MoodTone.Success)
    )
}


private fun defaultMoodEntries(): List<MoodEntryUi> {
    return listOf(
        MoodEntryUi(
            id = "mood_today_evening",
            title = "Calm evening",
            subtitle = "Stable mood with slight tiredness. Better after planning got simpler.",
            time = "Today · 8:20 PM",
            score = 76,
            factors = listOf("Focus", "Less noise", "Reflection"),
            tone = MoodTone.Calm
        ),
        MoodEntryUi(
            id = "mood_today_afternoon",
            title = "Focused block",
            subtitle = "Good mental clarity during build work.",
            time = "Today · 4:10 PM",
            score = 82,
            factors = listOf("Deep work", "Progress"),
            tone = MoodTone.Focused
        ),
        MoodEntryUi(
            id = "mood_yesterday_night",
            title = "Tired but steady",
            subtitle = "Energy was low, but journal helped close the day.",
            time = "Yesterday · 10:30 PM",
            score = 61,
            factors = listOf("Journal", "Sleep", "Low energy"),
            tone = MoodTone.Tired
        ),
        MoodEntryUi(
            id = "mood_week_low",
            title = "Pressure spike",
            subtitle = "Task overload made the evening heavier.",
            time = "Wednesday",
            score = 48,
            factors = listOf("Task pressure", "Late work"),
            tone = MoodTone.Stressed
        )
    )
}


private fun defaultMoodFactors(): List<MoodFactorUi> {
    return listOf(
        MoodFactorUi(
            title = "Focus completion",
            subtitle = "Mood improves when one deep-work block is completed.",
            impact = MoodImpact.Positive,
            tone = MoodTone.Focused
        ),
        MoodFactorUi(
            title = "Late task overload",
            subtitle = "Adding new work late at night lowers emotional clarity.",
            impact = MoodImpact.Negative,
            tone = MoodTone.Stressed
        ),
        MoodFactorUi(
            title = "Reflection",
            subtitle = "Journal entries appear to stabilize next-day planning.",
            impact = MoodImpact.Positive,
            tone = MoodTone.AI
        )
    )
}


private fun defaultMoodPrompts(): List<MoodJournalPromptUi> {
    return listOf(
        MoodJournalPromptUi(
            id = "prompt_reduce_noise",
            title = "What can I remove tonight?",
            body = "Write one thing you can stop thinking about until tomorrow.",
            label = "Calm",
            tone = MoodTone.Calm
        ),
        MoodJournalPromptUi(
            id = "prompt_pressure",
            title = "Where is the pressure coming from?",
            body = "Name the real source of pressure, not the surface-level task.",
            label = "Clarity",
            tone = MoodTone.Focused
        ),
        MoodJournalPromptUi(
            id = "prompt_gratitude",
            title = "What went quietly well today?",
            body = "Capture one small win that deserves recognition.",
            label = "Positive",
            tone = MoodTone.Happy
        )
    )
}
