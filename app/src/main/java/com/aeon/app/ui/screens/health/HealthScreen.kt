package com.aeon.app.ui.screens.health

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
 * HEALTH SCREEN
 *
 * Purpose:
 * Premium health command center for Aeon.
 *
 * Responsibilities:
 * - Show health balance, sleep, movement, hydration, medicine, and body signals
 * - Help user log essential health actions quickly
 * - Surface upcoming medicine and health reminders
 * - Keep health tracking calm, safe, private, and non-diagnostic
 *
 * Senior Developer Rule:
 * This screen is UI-state driven.
 * Real health storage, prescription OCR, medication reminders, device sync,
 * and health insight generation should live in ViewModel/use-cases later.
 */


// ----------------------------------------------------
// Route
// ----------------------------------------------------

@Composable
fun AeonHealthRoute(
    onAddHealthEntry: () -> Unit = {},
    onLogWater: () -> Unit = {},
    onLogSleep: () -> Unit = {},
    onOpenMedicine: (String) -> Unit = {},
    onOpenHealthEntry: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state = rememberHealthUiState()

    HealthScreen(
        state = state,
        onAddHealthEntry = onAddHealthEntry,
        onLogWater = onLogWater,
        onLogSleep = onLogSleep,
        onOpenMedicine = onOpenMedicine,
        onOpenHealthEntry = onOpenHealthEntry,
        onOpenNotifications = onOpenNotifications,
        modifier = modifier
    )
}


// ----------------------------------------------------
// State
// ----------------------------------------------------

@Immutable
data class HealthUiState(
    val dateLabel: String,
    val healthScore: Int = 68,
    val healthLabel: String = "Needs care",
    val healthMessage: String = "Your focus demand is higher than your recovery. Hydration and a short walk can improve today’s balance.",
    val metrics: List<HealthMetricUi> = defaultHealthMetrics(),
    val essentials: List<HealthEssentialUi> = defaultHealthEssentials(),
    val medicines: List<HealthMedicineUi> = defaultHealthMedicines(),
    val activity: List<HealthActivityUi> = defaultHealthActivity(),
    val sleep: HealthSleepUi = HealthSleepUi(),
    val entries: List<HealthEntryUi> = defaultHealthEntries(),
    val reminders: List<HealthReminderUi> = defaultHealthReminders(),
    val insight: HealthInsightUi = HealthInsightUi()
)


@Immutable
data class HealthMetricUi(
    val label: String,
    val value: String,
    val caption: String,
    val tone: HealthTone
)


@Immutable
data class HealthEssentialUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val value: String,
    val progress: Float,
    val status: HealthStatus,
    val tone: HealthTone
)


@Immutable
data class HealthMedicineUi(
    val id: String,
    val name: String,
    val dosage: String,
    val time: String,
    val instruction: String,
    val status: MedicineStatus,
    val tone: HealthTone
)


@Immutable
data class HealthActivityUi(
    val title: String,
    val value: String,
    val target: String,
    val progress: Float,
    val tone: HealthTone
)


@Immutable
data class HealthSleepUi(
    val duration: String = "6h 20m",
    val score: Int = 64,
    val quality: String = "Light recovery",
    val message: String = "Sleep was slightly below your recovery target. Avoid late overload tonight."
)


@Immutable
data class HealthEntryUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val time: String,
    val type: HealthEntryType,
    val tone: HealthTone
)


@Immutable
data class HealthReminderUi(
    val id: String,
    val title: String,
    val time: String,
    val enabled: Boolean,
    val urgent: Boolean,
    val tone: HealthTone
)


@Immutable
data class HealthInsightUi(
    val title: String = "Recovery is your current bottleneck",
    val body: String = "Aeon noticed that health balance is lower than your task and focus pressure. A small walk, water, and earlier sleep can improve tomorrow’s focus.",
    val confidence: Int = 81
)


enum class HealthFilter {
    All,
    Medicine,
    Sleep,
    Hydration,
    Activity
}


enum class HealthStatus {
    Good,
    Pending,
    Low,
    Done
}


enum class MedicineStatus {
    Upcoming,
    Taken,
    Missed
}


enum class HealthEntryType {
    Medicine,
    Sleep,
    Hydration,
    Activity,
    Symptom
}


enum class HealthTone {
    Health,
    Water,
    Sleep,
    Medicine,
    Activity,
    Heart,
    Food,
    AI,
    Warning,
    Success,
    Neutral
}


// ----------------------------------------------------
// Remember State
// ----------------------------------------------------

@Composable
fun rememberHealthUiState(): HealthUiState {
    val dateLabel = remember {
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
    }

    return remember(dateLabel) {
        HealthUiState(dateLabel = dateLabel)
    }
}


// ----------------------------------------------------
// Screen
// ----------------------------------------------------

@Composable
fun HealthScreen(
    state: HealthUiState,
    onAddHealthEntry: () -> Unit,
    onLogWater: () -> Unit,
    onLogSleep: () -> Unit,
    onOpenMedicine: (String) -> Unit,
    onOpenHealthEntry: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by rememberSaveable {
        mutableStateOf(HealthFilter.All)
    }

    val filteredEntries = remember(selectedFilter, state.entries) {
        state.entries.filterBy(selectedFilter)
    }

    AeonScreen(
        modifier = modifier
    ) {
        HealthHeader(
            state = state,
            onOpenNotifications = onOpenNotifications
        )

        HealthBalanceCard(state = state)

        HealthPrimaryActions(
            onAddHealthEntry = onAddHealthEntry,
            onLogWater = onLogWater,
            onLogSleep = onLogSleep
        )

        HealthMetricGrid(
            metrics = state.metrics
        )

        HealthEssentialSection(
            essentials = state.essentials,
            onLogWater = onLogWater,
            onLogSleep = onLogSleep
        )

        MedicineSection(
            medicines = state.medicines,
            onOpenMedicine = onOpenMedicine
        )

        HealthActivitySection(
            activity = state.activity
        )

        HealthSleepCard(
            sleep = state.sleep,
            onLogSleep = onLogSleep
        )

        HealthReminderSection(
            reminders = state.reminders,
            onOpenNotifications = onOpenNotifications
        )

        HealthFilterRow(
            selectedFilter = selectedFilter,
            onSelectFilter = {
                selectedFilter = it
            }
        )

        HealthEntrySection(
            title = selectedFilter.sectionTitle(),
            subtitle = selectedFilter.sectionSubtitle(),
            entries = filteredEntries,
            onOpenHealthEntry = onOpenHealthEntry
        )

        HealthInsightCard(insight = state.insight)

        HealthFooter()
    }
}


// ----------------------------------------------------
// Header
// ----------------------------------------------------

@Composable
private fun HealthHeader(
    state: HealthUiState,
    onOpenNotifications: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = state.dateLabel,
        title = "Health",
        subtitle = "A private place for sleep, hydration, medicine, movement, and body awareness.",
        size = AeonSectionHeaderSize.Hero,
        tone = AeonSectionHeaderTone.Brand,
        action = {
            AeonChip(
                text = "Health reminders",
                variant = AeonChipVariant.Premium,
                size = AeonChipSize.Compact,
                onClick = onOpenNotifications
            )
        }
    )
}


// ----------------------------------------------------
// Balance Card
// ----------------------------------------------------

@Composable
private fun HealthBalanceCard(state: HealthUiState) {
    AeonCard(
        variant = AeonCardVariant.Hero
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HealthScoreRing(
                score = state.healthScore,
                label = state.healthLabel
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Text(
                    text = "Health balance",
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = state.healthMessage,
                    style = AeonTextStyles.CardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AeonChip(
                        text = "Recovery watch",
                        variant = AeonChipVariant.Warning,
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
private fun HealthScoreRing(
    score: Int,
    label: String
) {
    val progress by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        label = "health_score_progress"
    )

    val ringColor = HealthTone.Health.color()
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
private fun HealthPrimaryActions(
    onAddHealthEntry: () -> Unit,
    onLogWater: () -> Unit,
    onLogSleep: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
    ) {
        AeonButton(
            text = "+ Health log",
            onClick = onAddHealthEntry,
            variant = AeonButtonVariant.Primary,
            size = AeonButtonSize.Medium,
            modifier = Modifier.weight(1f)
        )

        AeonButton(
            text = "Water",
            onClick = onLogWater,
            variant = AeonButtonVariant.Secondary,
            size = AeonButtonSize.Medium,
            modifier = Modifier.weight(1f)
        )

        AeonButton(
            text = "Sleep",
            onClick = onLogSleep,
            variant = AeonButtonVariant.Ghost,
            size = AeonButtonSize.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}


// ----------------------------------------------------
// Metrics
// ----------------------------------------------------

@Composable
private fun HealthMetricGrid(
    metrics: List<HealthMetricUi>
) {
    AeonSectionHeader(
        eyebrow = "Snapshot",
        title = "Health pulse",
        subtitle = "A compact view of body signals and recovery.",
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
                    HealthMetricCard(
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
private fun HealthMetricCard(
    metric: HealthMetricUi,
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
// Essentials
// ----------------------------------------------------

@Composable
private fun HealthEssentialSection(
    essentials: List<HealthEssentialUi>,
    onLogWater: () -> Unit,
    onLogSleep: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Essentials",
        title = "Today’s body basics",
        subtitle = "Small health actions that support energy, mood, and focus.",
        size = AeonSectionHeaderSize.Medium
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        essentials.forEach { essential ->
            HealthEssentialCard(
                essential = essential,
                onClick = {
                    when (essential.id) {
                        "hydration" -> onLogWater()
                        "sleep" -> onLogSleep()
                    }
                }
            )
        }
    }
}


@Composable
private fun HealthEssentialCard(
    essential: HealthEssentialUi,
    onClick: () -> Unit
) {
    AeonCard(
        variant = if (essential.status == HealthStatus.Low) {
            AeonCardVariant.Insight
        } else {
            AeonCardVariant.Default
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.Top
        ) {
            HealthSymbolBadge(
                symbol = essential.status.symbol(),
                tone = essential.status.tone(essential.tone)
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
                            text = essential.title,
                            style = AeonTextStyles.CardTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = essential.subtitle,
                            style = AeonTextStyles.CardSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = essential.value,
                        style = AeonTextStyles.StatNumber,
                        color = essential.tone.color()
                    )
                }

                HealthProgressBar(
                    progress = essential.progress,
                    tone = essential.tone
                )

                AeonChip(
                    text = essential.status.label(),
                    variant = essential.status.variant(),
                    size = AeonChipSize.Compact
                )
            }
        }
    }
}


// ----------------------------------------------------
// Medicine
// ----------------------------------------------------

@Composable
private fun MedicineSection(
    medicines: List<HealthMedicineUi>,
    onOpenMedicine: (String) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Medicine",
        title = "Medicine schedule",
        subtitle = "Aeon keeps medicine reminders clear, visible, and careful.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Default
    ) {
        medicines.forEachIndexed { index, medicine ->
            MedicineRow(
                medicine = medicine,
                onOpenMedicine = onOpenMedicine
            )

            if (index != medicines.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            }
        }
    }
}


@Composable
private fun MedicineRow(
    medicine: HealthMedicineUi,
    onOpenMedicine: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HealthSymbolBadge(
            symbol = medicine.status.symbol(),
            tone = medicine.status.tone(medicine.tone)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
        ) {
            Text(
                text = medicine.name,
                style = AeonTextStyles.CardTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${medicine.dosage} · ${medicine.instruction}",
                style = AeonTextStyles.CardSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = medicine.time,
                style = AeonTextStyles.Micro,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AeonChip(
            text = medicine.status.label(),
            variant = medicine.status.variant(),
            size = AeonChipSize.Compact,
            onClick = {
                onOpenMedicine(medicine.id)
            }
        )
    }
}


// ----------------------------------------------------
// Activity
// ----------------------------------------------------

@Composable
private fun HealthActivitySection(
    activity: List<HealthActivityUi>
) {
    AeonSectionHeader(
        eyebrow = "Movement",
        title = "Activity signals",
        subtitle = "Gentle movement supports focus recovery and mood stability.",
        size = AeonSectionHeaderSize.Medium
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        activity.forEach { item ->
            AeonCard(
                variant = AeonCardVariant.Compact
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HealthMiniRing(
                        progress = item.progress,
                        tone = item.tone
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
                    ) {
                        Text(
                            text = item.title,
                            style = AeonTextStyles.CardTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "${item.value} / ${item.target}",
                            style = AeonTextStyles.CardSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HealthProgressBar(
                            progress = item.progress,
                            tone = item.tone
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun HealthMiniRing(
    progress: Float,
    tone: HealthTone
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "health_mini_ring"
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
                sweepAngle = animated * 360f,
                useCenter = false,
                style = Stroke(
                    width = 5.dp.toPx(),
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
// Sleep
// ----------------------------------------------------

@Composable
private fun HealthSleepCard(
    sleep: HealthSleepUi,
    onLogSleep: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Recovery",
        title = "Sleep recovery",
        subtitle = "Sleep quality has direct impact on focus, mood, and health balance.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Elevated,
        onClick = onLogSleep
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HealthMiniRing(
                progress = sleep.score / 100f,
                tone = HealthTone.Sleep
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Text(
                    text = sleep.duration,
                    style = AeonTextStyles.StatNumber,
                    color = HealthTone.Sleep.color()
                )

                Text(
                    text = sleep.quality,
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = sleep.message,
                    style = AeonTextStyles.CardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AeonChip(
                text = "${sleep.score}",
                variant = if (sleep.score >= 75) {
                    AeonChipVariant.Success
                } else {
                    AeonChipVariant.Warning
                },
                size = AeonChipSize.Compact
            )
        }
    }
}


// ----------------------------------------------------
// Reminders
// ----------------------------------------------------

@Composable
private fun HealthReminderSection(
    reminders: List<HealthReminderUi>,
    onOpenNotifications: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Reminders",
        title = "Health reminders",
        subtitle = "Important health reminders can bypass quiet hours only when you allow it.",
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
            HealthReminderRow(
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
private fun HealthReminderRow(
    reminder: HealthReminderUi,
    onOpenNotifications: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HealthSymbolBadge(
            symbol = if (reminder.urgent) "!" else "◈",
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
// Filter
// ----------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthFilterRow(
    selectedFilter: HealthFilter,
    onSelectFilter: (HealthFilter) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "History",
        title = "Health log filters",
        subtitle = "Review medicine, sleep, hydration, activity, and health notes.",
        size = AeonSectionHeaderSize.Medium
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
    ) {
        HealthFilter.values().forEach { filter ->
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
private fun HealthEntrySection(
    title: String,
    subtitle: String,
    entries: List<HealthEntryUi>,
    onOpenHealthEntry: (String) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Entries",
        title = title,
        subtitle = subtitle,
        size = AeonSectionHeaderSize.Medium
    )

    if (entries.isEmpty()) {
        AeonCard(
            variant = AeonCardVariant.Glass
        ) {
            Text(
                text = "No health entries in this view.",
                style = AeonTextStyles.EmptyStateTitle,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Change filter or add a health log when you are ready.",
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
            AeonCard(
                variant = AeonCardVariant.Default,
                onClick = {
                    onOpenHealthEntry(entry.id)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HealthSymbolBadge(
                        symbol = entry.type.symbol(),
                        tone = entry.tone
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
                    ) {
                        Text(
                            text = entry.title,
                            style = AeonTextStyles.CardTitle,
                            color = MaterialTheme.colorScheme.onSurface
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
            }
        }
    }
}


// ----------------------------------------------------
// Insight
// ----------------------------------------------------

@Composable
private fun HealthInsightCard(insight: HealthInsightUi) {
    AeonSectionHeader(
        eyebrow = "Aeon intelligence",
        title = "Health insight",
        subtitle = "A private, non-diagnostic observation from your health rhythm.",
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
            HealthSymbolBadge(
                symbol = "✦",
                tone = HealthTone.AI
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
private fun HealthFooter() {
    AeonCard(
        variant = AeonCardVariant.Glass
    ) {
        Text(
            text = "Health tracking should support awareness, not anxiety.",
            style = AeonTextStyles.Quote,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Aeon is not a medical diagnosis tool. For serious symptoms, consult a qualified professional.",
            style = AeonTextStyles.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ----------------------------------------------------
// Shared UI
// ----------------------------------------------------

@Composable
private fun HealthSymbolBadge(
    symbol: String,
    tone: HealthTone
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
private fun HealthProgressBar(
    progress: Float,
    tone: HealthTone,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "health_progress"
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
private fun HealthTone.color(): Color {
    return when (this) {
        HealthTone.Health -> Color(0xFF10B981)
        HealthTone.Water -> Color(0xFF38BDF8)
        HealthTone.Sleep -> Color(0xFF818CF8)
        HealthTone.Medicine -> Color(0xFF34D399)
        HealthTone.Activity -> MaterialTheme.colorScheme.primary
        HealthTone.Heart -> Color(0xFFF472B6)
        HealthTone.Food -> Color(0xFFF5C542)
        HealthTone.AI -> Color(0xFFA78BFA)
        HealthTone.Warning -> MaterialTheme.colorScheme.error
        HealthTone.Success -> Color(0xFF34D399)
        HealthTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}


private fun HealthStatus.symbol(): String {
    return when (this) {
        HealthStatus.Good -> "✓"
        HealthStatus.Pending -> "○"
        HealthStatus.Low -> "!"
        HealthStatus.Done -> "✓"
    }
}


private fun HealthStatus.label(): String {
    return when (this) {
        HealthStatus.Good -> "Good"
        HealthStatus.Pending -> "Pending"
        HealthStatus.Low -> "Low"
        HealthStatus.Done -> "Done"
    }
}


private fun HealthStatus.variant(): AeonChipVariant {
    return when (this) {
        HealthStatus.Good -> AeonChipVariant.Success
        HealthStatus.Pending -> AeonChipVariant.Outline
        HealthStatus.Low -> AeonChipVariant.Warning
        HealthStatus.Done -> AeonChipVariant.Success
    }
}


private fun HealthStatus.tone(
    fallback: HealthTone
): HealthTone {
    return when (this) {
        HealthStatus.Good -> HealthTone.Success
        HealthStatus.Pending -> fallback
        HealthStatus.Low -> HealthTone.Warning
        HealthStatus.Done -> HealthTone.Success
    }
}


private fun MedicineStatus.symbol(): String {
    return when (this) {
        MedicineStatus.Upcoming -> "○"
        MedicineStatus.Taken -> "✓"
        MedicineStatus.Missed -> "!"
    }
}


private fun MedicineStatus.label(): String {
    return when (this) {
        MedicineStatus.Upcoming -> "Upcoming"
        MedicineStatus.Taken -> "Taken"
        MedicineStatus.Missed -> "Missed"
    }
}


private fun MedicineStatus.variant(): AeonChipVariant {
    return when (this) {
        MedicineStatus.Upcoming -> AeonChipVariant.Info
        MedicineStatus.Taken -> AeonChipVariant.Success
        MedicineStatus.Missed -> AeonChipVariant.Warning
    }
}


private fun MedicineStatus.tone(
    fallback: HealthTone
): HealthTone {
    return when (this) {
        MedicineStatus.Upcoming -> fallback
        MedicineStatus.Taken -> HealthTone.Success
        MedicineStatus.Missed -> HealthTone.Warning
    }
}


private fun HealthEntryType.symbol(): String {
    return when (this) {
        HealthEntryType.Medicine -> "+"
        HealthEntryType.Sleep -> "☾"
        HealthEntryType.Hydration -> "≋"
        HealthEntryType.Activity -> "◎"
        HealthEntryType.Symptom -> "!"
    }
}


private fun HealthFilter.label(): String {
    return when (this) {
        HealthFilter.All -> "All"
        HealthFilter.Medicine -> "Medicine"
        HealthFilter.Sleep -> "Sleep"
        HealthFilter.Hydration -> "Water"
        HealthFilter.Activity -> "Activity"
    }
}


private fun HealthFilter.sectionTitle(): String {
    return when (this) {
        HealthFilter.All -> "Health history"
        HealthFilter.Medicine -> "Medicine entries"
        HealthFilter.Sleep -> "Sleep entries"
        HealthFilter.Hydration -> "Hydration entries"
        HealthFilter.Activity -> "Activity entries"
    }
}


private fun HealthFilter.sectionSubtitle(): String {
    return when (this) {
        HealthFilter.All -> "Recent health logs, body signals, and care actions."
        HealthFilter.Medicine -> "Medicine-related logs and reminder outcomes."
        HealthFilter.Sleep -> "Sleep and recovery logs."
        HealthFilter.Hydration -> "Water and hydration records."
        HealthFilter.Activity -> "Movement, steps, and exercise logs."
    }
}


private fun List<HealthEntryUi>.filterBy(
    filter: HealthFilter
): List<HealthEntryUi> {
    return when (filter) {
        HealthFilter.All -> this
        HealthFilter.Medicine -> filter { it.type == HealthEntryType.Medicine }
        HealthFilter.Sleep -> filter { it.type == HealthEntryType.Sleep }
        HealthFilter.Hydration -> filter { it.type == HealthEntryType.Hydration }
        HealthFilter.Activity -> filter { it.type == HealthEntryType.Activity }
    }
}


// ----------------------------------------------------
// Dummy Data
// ----------------------------------------------------

private fun defaultHealthMetrics(): List<HealthMetricUi> {
    return listOf(
        HealthMetricUi(
            label = "Sleep",
            value = "6h 20m",
            caption = "last night",
            tone = HealthTone.Sleep
        ),
        HealthMetricUi(
            label = "Water",
            value = "4/8",
            caption = "glasses today",
            tone = HealthTone.Water
        ),
        HealthMetricUi(
            label = "Steps",
            value = "4.8k",
            caption = "today",
            tone = HealthTone.Activity
        ),
        HealthMetricUi(
            label = "Medicine",
            value = "1 due",
            caption = "upcoming",
            tone = HealthTone.Medicine
        )
    )
}


private fun defaultHealthEssentials(): List<HealthEssentialUi> {
    return listOf(
        HealthEssentialUi(
            id = "hydration",
            title = "Hydration",
            subtitle = "Drink water regularly to support energy and focus.",
            value = "50%",
            progress = 0.50f,
            status = HealthStatus.Low,
            tone = HealthTone.Water
        ),
        HealthEssentialUi(
            id = "sleep",
            title = "Sleep recovery",
            subtitle = "Recovery is below target. Keep tonight lighter.",
            value = "64",
            progress = 0.64f,
            status = HealthStatus.Pending,
            tone = HealthTone.Sleep
        ),
        HealthEssentialUi(
            id = "movement",
            title = "Movement",
            subtitle = "A short walk can improve health balance today.",
            value = "60%",
            progress = 0.60f,
            status = HealthStatus.Pending,
            tone = HealthTone.Activity
        )
    )
}


private fun defaultHealthMedicines(): List<HealthMedicineUi> {
    return listOf(
        HealthMedicineUi(
            id = "medicine_vitamin_d",
            name = "Vitamin D",
            dosage = "1 tablet",
            time = "8:30 PM",
            instruction = "After food",
            status = MedicineStatus.Upcoming,
            tone = HealthTone.Medicine
        ),
        HealthMedicineUi(
            id = "medicine_multivitamin",
            name = "Multivitamin",
            dosage = "1 capsule",
            time = "Morning",
            instruction = "Taken after breakfast",
            status = MedicineStatus.Taken,
            tone = HealthTone.Success
        )
    )
}


private fun defaultHealthActivity(): List<HealthActivityUi> {
    return listOf(
        HealthActivityUi(
            title = "Steps",
            value = "4,820",
            target = "8,000",
            progress = 0.60f,
            tone = HealthTone.Activity
        ),
        HealthActivityUi(
            title = "Walk",
            value = "12 min",
            target = "25 min",
            progress = 0.48f,
            tone = HealthTone.Health
        )
    )
}


private fun defaultHealthEntries(): List<HealthEntryUi> {
    return listOf(
        HealthEntryUi(
            id = "entry_water_evening",
            title = "Hydration logged",
            subtitle = "Added one glass of water.",
            time = "Today · 7:40 PM",
            type = HealthEntryType.Hydration,
            tone = HealthTone.Water
        ),
        HealthEntryUi(
            id = "entry_sleep_today",
            title = "Sleep recorded",
            subtitle = "6h 20m sleep with light recovery score.",
            time = "Today · Morning",
            type = HealthEntryType.Sleep,
            tone = HealthTone.Sleep
        ),
        HealthEntryUi(
            id = "entry_medicine_morning",
            title = "Medicine taken",
            subtitle = "Morning multivitamin marked as taken.",
            time = "Today · 9:10 AM",
            type = HealthEntryType.Medicine,
            tone = HealthTone.Medicine
        ),
        HealthEntryUi(
            id = "entry_walk",
            title = "Short walk",
            subtitle = "12 minutes of walking recorded.",
            time = "Yesterday",
            type = HealthEntryType.Activity,
            tone = HealthTone.Activity
        )
    )
}


private fun defaultHealthReminders(): List<HealthReminderUi> {
    return listOf(
        HealthReminderUi(
            id = "reminder_vitamin_d",
            title = "Vitamin D reminder",
            time = "Today · 8:30 PM",
            enabled = true,
            urgent = true,
            tone = HealthTone.Medicine
        ),
        HealthReminderUi(
            id = "reminder_water",
            title = "Hydration reminder",
            time = "Every 2 hours",
            enabled = true,
            urgent = false,
            tone = HealthTone.Water
        ),
        HealthReminderUi(
            id = "reminder_sleep",
            title = "Sleep wind-down",
            time = "10:30 PM",
            enabled = false,
            urgent = false,
            tone = HealthTone.Sleep
        )
    )
}
