package com.aeon.app.ui.screens.track

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aeon.app.data.local.database.entities.AeonInsightEntity
import com.aeon.app.data.local.database.entities.FinanceTransactionEntity
import com.aeon.app.data.local.database.entities.FinanceTransactionTypeStorage
import com.aeon.app.data.local.database.entities.GoalDomainStorage
import com.aeon.app.data.local.database.entities.HabitEntity
import com.aeon.app.data.local.database.entities.HabitFrequencyStorage
import com.aeon.app.data.local.database.entities.HabitLogEntity
import com.aeon.app.data.local.database.entities.HabitLogStatusStorage
import com.aeon.app.data.local.database.entities.HealthEntryEntity
import com.aeon.app.data.local.database.entities.InsightSeverityStorage
import com.aeon.app.data.local.database.entities.MedicineDoseLogEntity
import com.aeon.app.data.local.database.entities.MedicineEntity
import com.aeon.app.di.aeonViewModel
import com.aeon.app.presentation.viewmodel.AeonTrackViewModel
import com.aeon.app.presentation.viewmodel.TrackViewState
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.core.AeonCompactSectionHeader
import com.aeon.app.ui.components.core.AeonSectionHeader
import com.aeon.app.ui.components.core.AeonSectionHeaderSize
import com.aeon.app.ui.components.core.AeonSectionHeaderTone
import com.aeon.app.ui.components.feedback.AeonErrorState
import com.aeon.app.ui.components.feedback.AeonLoading
import com.aeon.app.ui.components.feedback.AeonNoDataState
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.components.layout.AeonScreenConfig
import com.aeon.app.ui.components.layout.aeonPremiumBackgroundBrush
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AeonTrackRoute(
    onAddEntry: () -> Unit = {},
    onOpenHabit: (String) -> Unit = {},
    onOpenGoal: (String) -> Unit = {},
    onOpenInsight: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = aeonViewModel<AeonTrackViewModel>()
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = remember(viewState) { viewState.toTrackUiState() }

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

@Immutable
data class TrackUiState(
    val dateLabel: String,
    val rangeLabel: String,
    val lifeBalanceScore: Int = 0,
    val lifeBalanceLabel: String = "Waiting for data",
    val lifeBalanceMessage: String = "Start logging real activity across the app and Aeon will build your weekly track board here.",
    val metrics: List<TrackMetricUi> = emptyList(),
    val domains: List<TrackDomainUi> = emptyList(),
    val weeklyRhythm: List<TrackDayUi> = emptyList(),
    val habits: List<TrackHabitUi> = emptyList(),
    val goals: List<TrackGoalUi> = emptyList(),
    val signals: List<TrackSignalUi> = emptyList(),
    val hasTrackedData: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@Immutable
data class TrackMetricUi(
    val label: String,
    val value: String,
    val caption: String,
    val trend: TrackTrend,
    val tone: TrackTone,
    val isTracked: Boolean
)

@Immutable
data class TrackDomainUi(
    val title: String,
    val subtitle: String,
    val score: Int,
    val delta: String,
    val tone: TrackTone,
    val isTracked: Boolean
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
        config = AeonScreenConfig(safeDrawing = false),
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when {
            state.isLoading -> {
                TrackLoadingBoard()
            }

            state.error != null -> {
                AeonErrorState(
                    title = "Unable to load track",
                    message = state.error
                )
            }

            else -> {
                TrackHeroBoard(state = state)

                TrackPrimaryActions(
                    onAddEntry = onAddEntry,
                    onOpenNotifications = onOpenNotifications
                )

                TrackMetricBoard(metrics = state.metrics)

                TrackWeeklyMapBoard(
                    days = state.weeklyRhythm,
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
    }
}

@Composable
private fun TrackHeader(
    state: TrackUiState,
    onOpenNotifications: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = state.dateLabel,
        title = "Track",
        subtitle = "Read the shape of your week across focus, habits, mood, health, finance, and goals.",
        size = AeonSectionHeaderSize.Hero,
        tone = AeonSectionHeaderTone.Brand,
        action = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AeonChip(
                    text = state.rangeLabel,
                    variant = AeonChipVariant.Premium,
                    size = AeonChipSize.Compact
                )
                AeonChip(
                    text = "Alerts",
                    variant = AeonChipVariant.Outline,
                    size = AeonChipSize.Compact,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    onClick = onOpenNotifications
                )
            }
        }
    )
}

@Composable
private fun TrackLoadingBoard() {
    AeonCard(variant = AeonCardVariant.Hero) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AeonLoading(isLarge = true)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Building your live track board",
                    style = AeonTextStyles.CardTitle.copy(
                        color = AeonThemeTokens.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "Aeon is reading real focus, habit, mood, health, goal, and finance records.",
                    style = AeonTextStyles.CardSubtitle.copy(
                        color = AeonThemeTokens.colors.textSecondary
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackHeroBoard(state: TrackUiState) {
    val colors = AeonThemeTokens.colors
    val trackedDomains = remember(state.domains) {
        state.domains.filter(TrackDomainUi::isTracked)
    }
    val strongestDomain = remember(trackedDomains) {
        trackedDomains.maxByOrNull(TrackDomainUi::score)
    }
    val weakestDomain = remember(trackedDomains) {
        trackedDomains.minByOrNull(TrackDomainUi::score)
    }
    val risingCount = remember(trackedDomains) {
        trackedDomains.count { domain -> domain.delta.startsWith("+") }
    }
    val featuredDomains = remember(trackedDomains, state.domains) {
        trackedDomains
            .ifEmpty { state.domains }
            .sortedByDescending(TrackDomainUi::score)
            .take(4)
    }

    AeonCard(
        variant = AeonCardVariant.Hero,
        containerColor = colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackScoreRing(
                score = state.lifeBalanceScore,
                label = state.lifeBalanceLabel
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Life pulse",
                    style = AeonTextStyles.Micro.copy(color = colors.brand)
                )
                Text(
                    text = state.lifeBalanceLabel,
                    style = AeonTextStyles.SectionTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = state.lifeBalanceMessage,
                    style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrackHeroStat(
                label = "Best",
                value = strongestDomain?.title ?: "--",
                tone = strongestDomain?.tone ?: TrackTone.Neutral,
                modifier = Modifier.weight(1f)
            )
            TrackHeroStat(
                label = "Watch",
                value = weakestDomain?.title ?: "--",
                tone = weakestDomain?.tone ?: TrackTone.Warning,
                modifier = Modifier.weight(1f)
            )
            TrackHeroStat(
                label = "Rising",
                value = "$risingCount domains",
                tone = TrackTone.Success,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            featuredDomains.forEach { domain ->
                AeonChip(
                    text = "${domain.title} ${domain.score}%",
                    variant = AeonChipVariant.Ghost,
                    size = AeonChipSize.Compact,
                    leadingIcon = {
                        Icon(
                            imageVector = domain.tone.icon(),
                            contentDescription = null,
                            tint = domain.tone.color(),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun TrackHeroStat(
    label: String,
    value: String,
    tone: TrackTone,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface.copy(alpha = 0.78f))
            .height(76.dp)
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = tone.icon(),
                contentDescription = null,
                tint = tone.color(),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = value,
                style = AeonTextStyles.CardTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrackScoreRing(
    score: Int,
    label: String
) {
    val colors = AeonThemeTokens.colors
    val progress by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        label = "track_life_pulse_ring"
    )

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(108.dp)
        ) {
            drawArc(
                color = colors.borderSoft.copy(alpha = 0.72f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(
                    width = 9.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
            drawArc(
                color = colors.brand,
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = score.toString(),
                style = AeonTextStyles.LifeScoreNumber.copy(color = colors.textPrimary)
            )
            Text(
                text = label,
                style = AeonTextStyles.Micro.copy(color = colors.textSecondary),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TrackPrimaryActions(
    onAddEntry: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AeonButton(
            text = "Add entry",
            onClick = onAddEntry,
            variant = AeonButtonVariant.Primary,
            size = AeonButtonSize.Medium,
            fullWidth = true,
            modifier = Modifier.weight(1f),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        AeonButton(
            text = "Reminders",
            onClick = onOpenNotifications,
            variant = AeonButtonVariant.Secondary,
            size = AeonButtonSize.Medium,
            fullWidth = true,
            modifier = Modifier.weight(1f),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

@Composable
private fun TrackMetricBoard(
    metrics: List<TrackMetricUi>
) {
    val liveMetrics = remember(metrics) { metrics.count(TrackMetricUi::isTracked) }

    AeonCompactSectionHeader(
        title = "Current pulse",
        subtitle = "Four quick readings that describe how the week feels right now.",
        action = {
            AeonChip(
                text = "$liveMetrics live",
                variant = AeonChipVariant.Info,
                size = AeonChipSize.Compact
            )
        }
    )

    AeonCard(variant = AeonCardVariant.Elevated) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            metrics.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { metric ->
                        TrackMetricTile(
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
}

@Composable
private fun TrackMetricTile(
    metric: TrackMetricUi,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface.copy(alpha = 0.72f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackIconBadge(
                icon = metric.tone.icon(),
                tone = metric.tone,
                size = 36.dp
            )

            AeonChip(
                text = metric.trend.label(),
                variant = metric.trend.chipVariant(),
                size = AeonChipSize.Compact
            )
        }

        Text(
            text = metric.value,
            style = AeonTextStyles.StatNumber.copy(color = metric.tone.color())
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = metric.label,
                style = AeonTextStyles.CardTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metric.caption,
                style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
            )
        }
    }
}

@Composable
private fun TrackWeeklyMapBoard(
    days: List<TrackDayUi>,
    domains: List<TrackDomainUi>
) {
    val alignedDays = remember(days) { days.count(TrackDayUi::completed) }
    val averageScore = remember(days) {
        if (days.isEmpty()) 0 else days.sumOf(TrackDayUi::score) / days.size
    }
    val strongestDay = remember(days) { days.maxByOrNull(TrackDayUi::score) }

    AeonCompactSectionHeader(
        title = "Weekly map",
        subtitle = "Consistency and domain balance in one compact view.",
        action = {
            AeonChip(
                text = "$alignedDays/${days.size} aligned",
                variant = AeonChipVariant.Success,
                size = AeonChipSize.Compact
            )
        }
    )

    AeonCard(variant = AeonCardVariant.Elevated) {
        TrackBoardHeader(
            title = "Rhythm",
            subtitle = "Your week improves when the middle stays steady, not only when one day spikes."
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrackMiniSummaryBlock(
                label = "Average",
                value = "$averageScore%",
                tone = TrackTone.Focus,
                modifier = Modifier.weight(1f)
            )
            TrackMiniSummaryBlock(
                label = "Peak",
                value = strongestDay?.label ?: "--",
                tone = strongestDay?.tone ?: TrackTone.Success,
                modifier = Modifier.weight(1f)
            )
            TrackMiniSummaryBlock(
                label = "Stable",
                value = "$alignedDays days",
                tone = TrackTone.Habit,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { day ->
                TrackDayColumn(day = day)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = AeonThemeTokens.colors.borderSoft.copy(alpha = 0.72f))
        Spacer(modifier = Modifier.height(14.dp))

        TrackBoardHeader(
            title = "Domain balance",
            subtitle = "Which areas are carrying the week and which ones need support."
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            domains.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { domain ->
                        TrackDomainTile(
                            domain = domain,
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
}

@Composable
private fun TrackMiniSummaryBlock(
    label: String,
    value: String,
    tone: TrackTone,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface.copy(alpha = 0.72f))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
        )
        Text(
            text = value,
            style = AeonTextStyles.CardTitle.copy(
                color = tone.color(),
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TrackDayColumn(
    day: TrackDayUi
) {
    val colors = AeonThemeTokens.colors
    val progress by animateFloatAsState(
        targetValue = day.score.coerceIn(0, 100) / 100f,
        label = "track_day_${day.label}"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = day.score.toString(),
            style = AeonTextStyles.Micro.copy(color = day.tone.color())
        )
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(84.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.borderSoft.copy(alpha = 0.56f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((84 * progress).dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(day.tone.color())
            )
        }
        Text(
            text = day.label,
            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (day.completed) {
                        day.tone.color()
                    } else {
                        colors.borderSoft
                    }
                )
        )
    }
}

@Composable
private fun TrackDomainTile(
    domain: TrackDomainUi,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface.copy(alpha = 0.72f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackIconBadge(
                icon = domain.tone.icon(),
                tone = domain.tone,
                size = 34.dp
            )
            TrackDeltaChip(delta = domain.delta)
        }

        Text(
            text = domain.title,
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = domain.subtitle,
            style = AeonTextStyles.Micro.copy(color = colors.textSecondary),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        TrackProgressBar(
            progress = domain.score / 100f,
            tone = domain.tone
        )
        Text(
            text = "${domain.score}% strength",
            style = AeonTextStyles.Micro.copy(color = domain.tone.color())
        )
    }
}

@Composable
private fun TrackHabitSection(
    habits: List<TrackHabitUi>,
    onOpenHabit: (String) -> Unit
) {
    AeonCompactSectionHeader(
        title = "Habit engine",
        subtitle = "Small repeated actions are still the strongest lever in this page.",
        action = {
            AeonChip(
                text = "${habits.size} active",
                variant = AeonChipVariant.Outline,
                size = AeonChipSize.Compact
            )
        }
    )

    if (habits.isEmpty()) {
        AeonNoDataState(
            title = "No habit data yet",
            message = "Add habits and start checking them off. Real consistency signals will appear here."
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            habits.forEach { habit ->
                TrackHabitCard(
                    habit = habit,
                    onOpenHabit = onOpenHabit
                )
            }
        }
    }
}

@Composable
private fun TrackHabitCard(
    habit: TrackHabitUi,
    onOpenHabit: (String) -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Compact,
        onClick = { onOpenHabit(habit.id) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackIconBadge(
                icon = habit.tone.icon(),
                tone = habit.tone
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = habit.title,
                            style = AeonTextStyles.CardTitle.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = habit.subtitle,
                            style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(habit.progress * 100).toInt()}% weekly completion",
                        style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = colors.iconSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackGoalSection(
    goals: List<TrackGoalUi>,
    onOpenGoal: (String) -> Unit
) {
    AeonCompactSectionHeader(
        title = "Goal runway",
        subtitle = "Long-term work stays clear when progress and timing stay visible.",
        action = {
            AeonChip(
                text = "${goals.size} goals",
                variant = AeonChipVariant.Info,
                size = AeonChipSize.Compact
            )
        }
    )

    if (goals.isEmpty()) {
        AeonNoDataState(
            title = "No active goals yet",
            message = "Create a goal or milestone and Aeon will surface progress, timing, and runway here."
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            goals.forEach { goal ->
                TrackGoalCard(
                    goal = goal,
                    onOpenGoal = onOpenGoal
                )
            }
        }
    }
}

@Composable
private fun TrackGoalCard(
    goal: TrackGoalUi,
    onOpenGoal: (String) -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Elevated,
        onClick = { onOpenGoal(goal.id) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackMiniRing(
                progress = goal.progress,
                tone = goal.tone
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = goal.title,
                            style = AeonTextStyles.CardTitle.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = goal.subtitle,
                            style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AeonChip(
                        text = goal.dueLabel,
                        variant = AeonChipVariant.Outline,
                        size = AeonChipSize.Compact
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(goal.progress * 100).toInt()}% complete",
                        style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = colors.iconSecondary,
                        modifier = Modifier.size(18.dp)
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
    val colors = AeonThemeTokens.colors
    val toneColor = tone.color()
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "track_goal_ring"
    )

    Box(
        modifier = Modifier.size(58.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(54.dp)) {
            drawArc(
                color = colors.borderSoft.copy(alpha = 0.72f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(
                    width = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
            drawArc(
                color = toneColor,
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
            style = AeonTextStyles.Micro.copy(color = colors.textPrimary)
        )
    }
}

@Composable
private fun TrackSignalSection(
    signals: List<TrackSignalUi>,
    onOpenInsight: (String) -> Unit
) {
    AeonCompactSectionHeader(
        title = "Aeon intelligence",
        subtitle = "Readable signals extracted from the patterns above.",
        action = {
            AeonChip(
                text = "Insights",
                variant = AeonChipVariant.Premium,
                size = AeonChipSize.Compact,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }
    )

    if (signals.isEmpty()) {
        AeonNoDataState(
            title = "No fresh insights yet",
            message = "As real patterns accumulate across focus, habits, mood, health, and finance, Aeon will surface them here."
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            signals.forEach { signal ->
                AeonCard(
                    variant = AeonCardVariant.Insight,
                    onClick = { onOpenInsight(signal.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        TrackIconBadge(
                            icon = signal.tone.icon(),
                            tone = signal.tone
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = signal.title,
                                    style = AeonTextStyles.InsightTitle.copy(
                                        color = AeonThemeTokens.colors.textPrimary
                                    ),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                AeonChip(
                                    text = signal.label,
                                    variant = AeonChipVariant.Premium,
                                    size = AeonChipSize.Compact
                                )
                            }

                            Text(
                                text = signal.body,
                                style = AeonTextStyles.InsightBody.copy(
                                    color = AeonThemeTokens.colors.textSecondary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackFooter() {
    val colors = AeonThemeTokens.colors

    AeonCard(variant = AeonCardVariant.Glass) {
        Text(
            text = "Tracking is for awareness, not pressure.",
            style = AeonTextStyles.Quote.copy(color = colors.textPrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Measure gently. Improve consistently.",
            style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
        )
    }
}

@Composable
private fun TrackBoardHeader(
    title: String,
    subtitle: String
) {
    val colors = AeonThemeTokens.colors

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )
        Text(
            text = subtitle,
            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
        )
    }
}

@Composable
private fun TrackIconBadge(
    icon: ImageVector,
    tone: TrackTone,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 42.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tone.color().copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tone.color(),
            modifier = Modifier.size(size * 0.45f)
        )
    }
}

@Composable
private fun TrackDeltaChip(delta: String) {
    val variant = when {
        delta.startsWith("+") -> AeonChipVariant.Success
        delta.startsWith("-") -> AeonChipVariant.Warning
        else -> AeonChipVariant.Outline
    }

    AeonChip(
        text = delta,
        variant = variant,
        size = AeonChipSize.Compact
    )
}

@Composable
private fun TrackProgressBar(
    progress: Float,
    tone: TrackTone,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "track_progress_bar"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(colors.borderSoft.copy(alpha = 0.62f))
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

@Composable
private fun TrackTone.color(): Color {
    val colors = AeonThemeTokens.colors
    return when (this) {
        TrackTone.Brand -> colors.brand
        TrackTone.Focus -> colors.focus
        TrackTone.Habit -> colors.habit
        TrackTone.Health -> colors.health
        TrackTone.Mood -> colors.mood
        TrackTone.Finance -> colors.finance
        TrackTone.Learning -> colors.learning
        TrackTone.Goal -> colors.goal
        TrackTone.AI -> colors.ai
        TrackTone.Warning -> colors.warning
        TrackTone.Success -> colors.success
        TrackTone.Neutral -> colors.textSecondary
    }
}

private fun TrackTone.icon(): ImageVector {
    return when (this) {
        TrackTone.Brand -> Icons.Outlined.Timeline
        TrackTone.Focus -> Icons.Outlined.CenterFocusStrong
        TrackTone.Habit -> Icons.Outlined.Repeat
        TrackTone.Health -> Icons.Outlined.HealthAndSafety
        TrackTone.Mood -> Icons.Outlined.SentimentSatisfiedAlt
        TrackTone.Finance -> Icons.Outlined.Paid
        TrackTone.Learning -> Icons.Outlined.School
        TrackTone.Goal -> Icons.Outlined.Flag
        TrackTone.AI -> Icons.Outlined.AutoAwesome
        TrackTone.Warning -> Icons.Outlined.WarningAmber
        TrackTone.Success -> Icons.Outlined.CheckCircleOutline
        TrackTone.Neutral -> Icons.Outlined.HorizontalRule
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

private data class TrackDomainSnapshot(
    val title: String,
    val subtitle: String,
    val score: Int,
    val delta: String,
    val tone: TrackTone,
    val isTracked: Boolean
)

private fun TrackViewState.toTrackUiState(): TrackUiState {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())
    val rangeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val habitLogsByHabit = currentHabitLogs.groupBy(HabitLogEntity::habitId)
    val previousHabitLogsByHabit = previousHabitLogs.groupBy(HabitLogEntity::habitId)
    val currency = budgets.firstOrNull()?.currency
        ?: currentMonthTransactions.firstOrNull()?.currency
        ?: currentTransactions.firstOrNull()?.currency
        ?: "INR"
    val currentMonthBudgets = budgets.filter { budget ->
        today >= budget.periodStart && today <= budget.periodEnd && budget.isActive
    }

    val focusScore = currentFocusSessions.focusScore()
    val previousFocusScore = previousFocusSessions.focusScore()
    val habitScore = activeHabits.habitScore(habitLogsByHabit)
    val previousHabitScore = activeHabits.habitScore(previousHabitLogsByHabit)
    val moodScore = currentMoodEntries.averageMoodScore() ?: 0
    val previousMoodScore = previousMoodEntries.averageMoodScore() ?: 0
    val healthScore = currentHealthEntries.healthScore(currentDoseLogs)
    val previousHealthScore = previousHealthEntries.healthScore(previousDoseLogs)
    val financeScore = currentMonthTransactions.financeScore(
        currentWeekTransactions = currentTransactions,
        budgets = currentMonthBudgets
    )
    val previousFinanceScore = previousTransactions.previousFinanceScore()
    val goalScore = activeGoals.goalScore()

    val focusTracked = currentFocusSessions.isNotEmpty()
    val habitTracked = activeHabits.isNotEmpty()
    val moodTracked = currentMoodEntries.isNotEmpty()
    val healthTracked = currentHealthEntries.isNotEmpty() || currentDoseLogs.isNotEmpty() || activeMedicines.isNotEmpty()
    val financeTracked = currentTransactions.isNotEmpty() || currentMonthTransactions.isNotEmpty() || currentMonthBudgets.isNotEmpty()
    val goalTracked = activeGoals.isNotEmpty()

    val domainSnapshots = listOf(
        TrackDomainSnapshot(
            title = "Focus",
            subtitle = if (focusTracked) {
                buildString {
                    append("${currentFocusSessions.focusMinutes().formatMinutes()} across ${currentFocusSessions.size} sessions")
                    currentFocusSessions.averageQualityScore()?.let { quality ->
                        append(" · $quality quality")
                    }
                }
            } else {
                "No focus sessions logged in the last 7 days."
            },
            score = focusScore,
            delta = formatScoreDelta(focusScore, previousFocusScore, focusTracked || previousFocusSessions.isNotEmpty()),
            tone = TrackTone.Focus,
            isTracked = focusTracked
        ),
        TrackDomainSnapshot(
            title = "Habits",
            subtitle = if (habitTracked) {
                "${habitLogsByHabit.values.flatten().doneCount()} completions across ${activeHabits.size} active habits"
            } else {
                "No active habits yet."
            },
            score = habitScore,
            delta = formatScoreDelta(habitScore, previousHabitScore, habitTracked || previousHabitLogs.isNotEmpty()),
            tone = TrackTone.Habit,
            isTracked = habitTracked
        ),
        TrackDomainSnapshot(
            title = "Mood",
            subtitle = if (moodTracked) {
                "${currentMoodEntries.size} mood check-ins this week"
            } else {
                "No mood entries logged in the last 7 days."
            },
            score = moodScore,
            delta = formatScoreDelta(moodScore, previousMoodScore, moodTracked || previousMoodEntries.isNotEmpty()),
            tone = TrackTone.Mood,
            isTracked = moodTracked
        ),
        TrackDomainSnapshot(
            title = "Health",
            subtitle = healthSubtitle(
                entries = currentHealthEntries,
                doseLogs = currentDoseLogs,
                medicines = activeMedicines
            ),
            score = healthScore,
            delta = formatScoreDelta(healthScore, previousHealthScore, healthTracked || previousHealthEntries.isNotEmpty() || previousDoseLogs.isNotEmpty()),
            tone = TrackTone.Health,
            isTracked = healthTracked
        ),
        TrackDomainSnapshot(
            title = "Finance",
            subtitle = financeSubtitle(
                currency = currency,
                currentWeekTransactions = currentTransactions,
                currentMonthTransactions = currentMonthTransactions,
                budgets = currentMonthBudgets
            ),
            score = financeScore,
            delta = financeDelta(
                currentWeekTransactions = currentTransactions,
                previousWeekTransactions = previousTransactions
            ),
            tone = TrackTone.Finance,
            isTracked = financeTracked
        ),
        TrackDomainSnapshot(
            title = "Goals",
            subtitle = if (goalTracked) {
                "${goalScore}% average progress across ${activeGoals.size} active goals"
            } else {
                "No active goals yet."
            },
            score = goalScore,
            delta = if (goalTracked) "${activeGoals.size} live" else "steady",
            tone = TrackTone.Goal,
            isTracked = goalTracked
        )
    )

    val trackedDomains = domainSnapshots.filter(TrackDomainSnapshot::isTracked)
    val lifeBalanceScore = trackedDomains
        .map(TrackDomainSnapshot::score)
        .takeIf(List<Int>::isNotEmpty)
        ?.average()
        ?.roundToInt()
        ?: 0
    val lifeBalanceLabel = when {
        trackedDomains.isEmpty() -> "Waiting for data"
        lifeBalanceScore >= 85 -> "Excellent balance"
        lifeBalanceScore >= 70 -> "Strong balance"
        lifeBalanceScore >= 55 -> "Steady balance"
        lifeBalanceScore >= 35 -> "Needs attention"
        else -> "Fragile balance"
    }
    val strongestDomain = trackedDomains.maxByOrNull(TrackDomainSnapshot::score)
    val weakestDomain = trackedDomains.minByOrNull(TrackDomainSnapshot::score)
    val lifeBalanceMessage = when {
        trackedDomains.isEmpty() -> {
            "Start logging real activity across focus, habits, mood, health, finance, and goals to build this board."
        }

        trackedDomains.size == 1 && strongestDomain != null -> {
            "${strongestDomain.title} is active. Add more tracked areas to make the balance score more meaningful."
        }

        strongestDomain != null && weakestDomain != null && abs(strongestDomain.score - weakestDomain.score) <= 8 -> {
            "Your tracked areas are moving in a fairly even rhythm this week. Keep the consistency steady."
        }

        strongestDomain != null && weakestDomain != null -> {
            "${strongestDomain.title} is carrying the week. ${weakestDomain.title} has the clearest room to improve."
        }

        else -> {
            "Keep logging consistently and Aeon will sharpen these weekly signals."
        }
    }

    val metrics = listOf(
        TrackMetricUi(
            label = "Focus",
            value = currentFocusSessions.focusMinutes().formatMinutes(),
            caption = if (focusTracked) {
                "${currentFocusSessions.size} sessions logged in the last 7 days"
            } else {
                "No focus sessions in the last 7 days"
            },
            trend = trendFromScore(focusScore, previousFocusScore),
            tone = TrackTone.Focus,
            isTracked = focusTracked
        ),
        TrackMetricUi(
            label = "Habits",
            value = "$habitScore%",
            caption = if (habitTracked) {
                "${habitLogsByHabit.values.flatten().doneCount()} check-ins completed this week"
            } else {
                "No active habits yet"
            },
            trend = trendFromScore(habitScore, previousHabitScore),
            tone = TrackTone.Habit,
            isTracked = habitTracked
        ),
        TrackMetricUi(
            label = "Mood",
            value = currentMoodEntries.averageMoodScore().toMoodLabel(),
            caption = if (moodTracked) {
                "${currentMoodEntries.size} entries with $moodScore% average mood"
            } else {
                "No mood entries in the last 7 days"
            },
            trend = trendFromScore(moodScore, previousMoodScore),
            tone = TrackTone.Mood,
            isTracked = moodTracked
        ),
        TrackMetricUi(
            label = "Spend",
            value = currentTransactions.sumAmount(FinanceTransactionTypeStorage.Expense).formatMoney(currency),
            caption = if (financeTracked) {
                "${currentTransactions.size} finance entries tracked this week"
            } else {
                "No finance entries in the last 7 days"
            },
            trend = spendTrend(currentTransactions, previousTransactions),
            tone = TrackTone.Finance,
            isTracked = financeTracked
        )
    )

    val habits = activeHabits
        .sortedWith(
            compareByDescending<HabitEntity> { it.isPinned }
                .thenByDescending { habit ->
                    habit.weeklyProgress(habitLogsByHabit[habit.id].orEmpty())
                }
                .thenByDescending(HabitEntity::currentStreak)
        )
        .take(3)
        .map { habit ->
            val habitLogs = habitLogsByHabit[habit.id].orEmpty()
            val progress = habit.weeklyProgress(habitLogs)
            TrackHabitUi(
                id = habit.id,
                title = habit.title,
                subtitle = when {
                    habitLogs.isNotEmpty() && habit.frequencyType == HabitFrequencyStorage.Daily -> {
                        "${habitLogs.doneCount()} of 7 days completed"
                    }

                    habitLogs.isNotEmpty() -> {
                        "${(progress * 100).roundToInt()}% completed on current cycle"
                    }

                    else -> {
                        "${(habit.completionRate * 100).roundToInt()}% completion so far"
                    }
                },
                progress = progress,
                streak = habit.currentStreak.streakLabel(),
                tone = habit.trackTone()
            )
        }

    val goals = activeGoals
        .sortedWith(
            compareByDescending<com.aeon.app.data.local.database.entities.GoalEntity> { it.isPinned }
                .thenBy { it.dueAt ?: java.time.Instant.MAX }
                .thenByDescending { it.progress }
        )
        .take(3)
        .map { goal ->
            TrackGoalUi(
                id = goal.id,
                title = goal.title,
                subtitle = goal.description
                    ?.takeIf(String::isNotBlank)
                    ?: "${goal.domain.prettyGoalDomain()} goal",
                progress = goal.progress.coerceIn(0f, 1f),
                dueLabel = goal.dueLabel(today),
                tone = goal.trackTone()
            )
        }

    val signals = insights
        .sortedWith(
            compareByDescending<AeonInsightEntity> { it.confidence }
                .thenByDescending { it.createdAt }
        )
        .take(3)
        .map { insight ->
            TrackSignalUi(
                id = insight.id,
                title = insight.title,
                body = insight.recommendation
                    ?.takeIf(String::isNotBlank)
                    ?: insight.body,
                label = insight.domain.prettyInsightDomain(),
                tone = insight.trackTone()
            )
        }

    return TrackUiState(
        dateLabel = today.format(dateFormatter),
        rangeLabel = "${rangeStart.format(rangeFormatter)} - ${rangeEnd.format(rangeFormatter)}",
        lifeBalanceScore = lifeBalanceScore,
        lifeBalanceLabel = lifeBalanceLabel,
        lifeBalanceMessage = lifeBalanceMessage,
        metrics = metrics,
        domains = domainSnapshots.map { domain ->
            TrackDomainUi(
                title = domain.title,
                subtitle = domain.subtitle,
                score = domain.score,
                delta = domain.delta,
                tone = domain.tone,
                isTracked = domain.isTracked
            )
        },
        weeklyRhythm = buildWeeklyRhythm(habitLogsByHabit = habitLogsByHabit),
        habits = habits,
        goals = goals,
        signals = signals,
        hasTrackedData = trackedDomains.isNotEmpty(),
        isLoading = isLoading,
        error = error
    )
}

private fun TrackViewState.buildWeeklyRhythm(
    habitLogsByHabit: Map<String, List<HabitLogEntity>>
): List<TrackDayUi> {
    val zone = ZoneId.systemDefault()
    val focusByDate = currentFocusSessions.groupBy { session ->
        session.startedAt.atZone(zone).toLocalDate()
    }
    val moodByDate = currentMoodEntries.groupBy { entry -> entry.logDate }
    val healthByDate = currentHealthEntries.groupBy { entry -> entry.logDate }
    val doseByDate = currentDoseLogs.groupBy { log ->
        log.scheduledAt.atZone(zone).toLocalDate()
    }
    val habitByDate = habitLogsByHabit.values.flatten().groupBy(HabitLogEntity::logDate)

    return (0L..6L).map { offset ->
        val date = rangeStart.plusDays(offset)
        val candidates = mutableListOf<Int>()
        val dayFocus = focusByDate[date].orEmpty()
        val dayHabits = habitByDate[date].orEmpty()
        val dayMood = moodByDate[date].orEmpty()
        val dayHealthEntries = healthByDate[date].orEmpty()
        val dayDoseLogs = doseByDate[date].orEmpty()

        if (dayFocus.isNotEmpty()) {
            candidates += dayFocus.focusScore()
        }

        if (activeHabits.isNotEmpty()) {
            val completedHabits = dayHabits.doneCount()
            val habitScore = ((completedHabits.toFloat() / activeHabits.size) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
            candidates += habitScore
        }

        dayMood.averageMoodScore()?.let(candidates::add)

        if (dayHealthEntries.isNotEmpty() || dayDoseLogs.isNotEmpty()) {
            candidates += dayHealthEntries.healthScore(dayDoseLogs)
        }

        val score = candidates
            .takeIf(List<Int>::isNotEmpty)
            ?.average()
            ?.roundToInt()
            ?: 0
        val tracked = candidates.isNotEmpty()

        TrackDayUi(
            label = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
            score = score,
            completed = tracked && score >= 60,
            tone = dayTone(score, tracked)
        )
    }
}

private fun List<com.aeon.app.data.local.database.entities.FocusSessionEntity>.focusMinutes(): Int {
    return sumOf { session -> session.actualMinutes.coerceAtLeast(0) }
}

private fun List<com.aeon.app.data.local.database.entities.FocusSessionEntity>.averageQualityScore(): Int? {
    return mapNotNull { session -> session.qualityScore }
        .takeIf(List<Int>::isNotEmpty)
        ?.average()
        ?.roundToInt()
}

private fun List<com.aeon.app.data.local.database.entities.FocusSessionEntity>.focusScore(): Int {
    if (isEmpty()) return 0

    val candidates = mutableListOf<Int>()
    candidates += ((focusMinutes() / 300f) * 100f).roundToInt().coerceIn(0, 100)
    averageQualityScore()?.let(candidates::add)
    return candidates.average().roundToInt().coerceIn(0, 100)
}

private fun List<HabitEntity>.habitScore(
    logsByHabit: Map<String, List<HabitLogEntity>>
): Int {
    if (isEmpty()) return 0
    return map { habit ->
        (habit.weeklyProgress(logsByHabit[habit.id].orEmpty()) * 100f).roundToInt()
    }.average().roundToInt().coerceIn(0, 100)
}

private fun HabitEntity.weeklyProgress(logs: List<HabitLogEntity>): Float {
    val doneCount = logs.doneCount()
    return when (frequencyType) {
        HabitFrequencyStorage.Daily -> (doneCount / 7f).coerceIn(0f, 1f)
        else -> completionRate.coerceIn(0f, 1f)
    }
}

private fun List<HabitLogEntity>.doneCount(): Int {
    return count { log -> log.status == HabitLogStatusStorage.Done }
}

private fun List<com.aeon.app.data.local.database.entities.MoodEntryEntity>.averageMoodScore(): Int? {
    return takeIf { it.isNotEmpty() }
        ?.map { entry -> entry.moodScore }
        ?.average()
        ?.roundToInt()
}

private fun Int?.toMoodLabel(): String {
    val score = this ?: return "--"
    return when {
        score >= 80 -> "Bright"
        score >= 65 -> "Calm"
        score >= 50 -> "Steady"
        score >= 35 -> "Low"
        else -> "Heavy"
    }
}

private fun List<HealthEntryEntity>.healthScore(
    doseLogs: List<MedicineDoseLogEntity>
): Int {
    if (isEmpty() && doseLogs.isEmpty()) return 0

    val candidates = mutableListOf<Int>()
    mapNotNull { entry -> entry.score }
        .takeIf(List<Int>::isNotEmpty)
        ?.average()
        ?.roundToInt()
        ?.let(candidates::add)
    doseLogs.doseAdherenceScore()?.let(candidates::add)
    if (candidates.isEmpty() && isNotEmpty()) {
        candidates += (size * 20).coerceAtMost(100)
    }
    return candidates.average().roundToInt().coerceIn(0, 100)
}

private fun List<MedicineDoseLogEntity>.doseAdherenceScore(): Int? {
    if (isEmpty()) return null
    val takenCount = count { log -> log.takenAt != null }
    return ((takenCount.toFloat() / size) * 100f).roundToInt().coerceIn(0, 100)
}

private fun List<com.aeon.app.data.local.database.entities.GoalEntity>.goalScore(): Int {
    return takeIf { it.isNotEmpty() }
        ?.map { goal -> goal.progress.coerceIn(0f, 1f) * 100f }
        ?.average()
        ?.roundToInt()
        ?: 0
}

private fun List<FinanceTransactionEntity>.financeScore(
    currentWeekTransactions: List<FinanceTransactionEntity>,
    budgets: List<com.aeon.app.data.local.database.entities.BudgetEntity>
): Int {
    val totalBudget = budgets.sumOfMoney { budget -> budget.budgetLimit }
    val monthExpense = sumAmount(FinanceTransactionTypeStorage.Expense)
    val monthIncome = sumAmount(FinanceTransactionTypeStorage.Income)

    return when {
        totalBudget > BigDecimal.ZERO -> {
            ((totalBudget.subtract(monthExpense))
                .coerceAtLeast(BigDecimal.ZERO)
                .divide(totalBudget, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100")))
                .toInt()
                .coerceIn(0, 100)
        }

        monthIncome > BigDecimal.ZERO -> {
            ((monthIncome.subtract(monthExpense))
                .coerceAtLeast(BigDecimal.ZERO)
                .divide(monthIncome, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100")))
                .toInt()
                .coerceIn(0, 100)
        }

        currentWeekTransactions.isNotEmpty() -> {
            (currentWeekTransactions.size * 18).coerceAtMost(100)
        }

        else -> 0
    }
}

private fun List<FinanceTransactionEntity>.previousFinanceScore(): Int {
    val expense = sumAmount(FinanceTransactionTypeStorage.Expense)
    if (expense <= BigDecimal.ZERO) return 0
    return (100 - (expense.toDouble() / 100.0).roundToInt()).coerceIn(0, 100)
}

private fun List<FinanceTransactionEntity>.sumAmount(
    type: String
): BigDecimal {
    return filter { transaction -> transaction.transactionType == type }
        .fold(BigDecimal.ZERO) { total, transaction ->
            total.add(transaction.amount)
        }
}

private fun <T> Iterable<T>.sumOfMoney(
    selector: (T) -> BigDecimal
): BigDecimal {
    return fold(BigDecimal.ZERO) { total, item ->
        total.add(selector(item))
    }
}

private fun financeSubtitle(
    currency: String,
    currentWeekTransactions: List<FinanceTransactionEntity>,
    currentMonthTransactions: List<FinanceTransactionEntity>,
    budgets: List<com.aeon.app.data.local.database.entities.BudgetEntity>
): String {
    val totalBudget = budgets.sumOfMoney { budget -> budget.budgetLimit }
    val monthExpense = currentMonthTransactions.sumAmount(FinanceTransactionTypeStorage.Expense)
    val monthIncome = currentMonthTransactions.sumAmount(FinanceTransactionTypeStorage.Income)

    return when {
        totalBudget > BigDecimal.ZERO -> {
            "${totalBudget.subtract(monthExpense).coerceAtLeast(BigDecimal.ZERO).formatMoney(currency)} left of ${totalBudget.formatMoney(currency)} this month"
        }

        monthIncome > BigDecimal.ZERO -> {
            "${monthIncome.subtract(monthExpense).formatMoney(currency)} net this month"
        }

        currentWeekTransactions.isNotEmpty() -> {
            "${currentWeekTransactions.size} finance entries captured this week"
        }

        else -> {
            "No finance entries logged in the last 7 days."
        }
    }
}

private fun financeDelta(
    currentWeekTransactions: List<FinanceTransactionEntity>,
    previousWeekTransactions: List<FinanceTransactionEntity>
): String {
    val currentExpense = currentWeekTransactions.sumAmount(FinanceTransactionTypeStorage.Expense)
    val previousExpense = previousWeekTransactions.sumAmount(FinanceTransactionTypeStorage.Expense)
    if (currentExpense <= BigDecimal.ZERO && previousExpense <= BigDecimal.ZERO) return "steady"
    if (previousExpense <= BigDecimal.ZERO) return "tracked"
    val change = currentExpense.subtract(previousExpense)
    val percent = change.abs()
        .divide(previousExpense, 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal("100"))
        .toInt()
    return if (change <= BigDecimal.ZERO) {
        "+$percent%"
    } else {
        "-$percent%"
    }
}

private fun spendTrend(
    currentWeekTransactions: List<FinanceTransactionEntity>,
    previousWeekTransactions: List<FinanceTransactionEntity>
): TrackTrend {
    val currentExpense = currentWeekTransactions.sumAmount(FinanceTransactionTypeStorage.Expense)
    val previousExpense = previousWeekTransactions.sumAmount(FinanceTransactionTypeStorage.Expense)
    return when {
        currentExpense <= BigDecimal.ZERO && previousExpense <= BigDecimal.ZERO -> TrackTrend.Stable
        previousExpense <= BigDecimal.ZERO -> TrackTrend.Stable
        currentExpense <= previousExpense -> TrackTrend.Up
        else -> TrackTrend.Down
    }
}

private fun healthSubtitle(
    entries: List<HealthEntryEntity>,
    doseLogs: List<MedicineDoseLogEntity>,
    medicines: List<MedicineEntity>
): String {
    return when {
        doseLogs.isNotEmpty() -> {
            "${doseLogs.count { log -> log.takenAt != null }} of ${doseLogs.size} doses taken this week"
        }

        entries.isNotEmpty() -> {
            "${entries.size} health logs captured this week"
        }

        medicines.isNotEmpty() -> {
            "${medicines.size} active medicines with no dose logs yet"
        }

        else -> {
            "No health activity logged in the last 7 days."
        }
    }
}

private fun formatScoreDelta(
    current: Int,
    previous: Int,
    hasComparisonData: Boolean
): String {
    if (!hasComparisonData) return "steady"
    val change = current - previous
    if (abs(change) < 3) return "steady"
    return if (change > 0) {
        "+$change pts"
    } else {
        "$change pts"
    }
}

private fun trendFromScore(current: Int, previous: Int): TrackTrend {
    return when {
        abs(current - previous) < 3 -> TrackTrend.Stable
        current > previous -> TrackTrend.Up
        else -> TrackTrend.Down
    }
}

private fun dayTone(
    score: Int,
    tracked: Boolean
): TrackTone {
    if (!tracked) return TrackTone.Neutral
    return when {
        score >= 80 -> TrackTone.Success
        score >= 65 -> TrackTone.Focus
        score >= 45 -> TrackTone.Mood
        else -> TrackTone.Warning
    }
}

private fun Int.formatMinutes(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

private fun BigDecimal.formatMoney(currencyCode: String): String {
    return runCatching {
        val formatter = NumberFormat.getCurrencyInstance(
            if (currencyCode.equals("INR", ignoreCase = true)) {
                Locale("en", "IN")
            } else {
                Locale.getDefault()
            }
        )
        formatter.currency = java.util.Currency.getInstance(currencyCode.uppercase(Locale.ROOT))
        formatter.maximumFractionDigits = 0
        formatter.minimumFractionDigits = 0
        formatter.format(this)
    }.getOrElse {
        "${currencyCode.uppercase(Locale.ROOT)} ${setScale(0, RoundingMode.HALF_UP)}"
    }
}

private fun Int.streakLabel(): String {
    return if (this == 1) "1 day" else "${coerceAtLeast(0)} days"
}

private fun HabitEntity.trackTone(): TrackTone {
    return when (category.lowercase(Locale.ROOT)) {
        "health", "fitness", "sleep", "hydration", "walk" -> TrackTone.Health
        "journal", "mind", "mood", "reflection" -> TrackTone.Mood
        "study", "learning", "reading" -> TrackTone.Learning
        else -> TrackTone.Habit
    }
}

private fun com.aeon.app.data.local.database.entities.GoalEntity.trackTone(): TrackTone {
    return when (domain) {
        GoalDomainStorage.Study -> TrackTone.Learning
        GoalDomainStorage.Health -> TrackTone.Health
        GoalDomainStorage.Finance -> TrackTone.Finance
        else -> TrackTone.Goal
    }
}

private fun String.prettyGoalDomain(): String {
    return split("_", "-", " ")
        .filter(String::isNotBlank)
        .joinToString(" ") { token ->
            token.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }
        }
        .ifBlank { "Personal" }
}

private fun com.aeon.app.data.local.database.entities.GoalEntity.dueLabel(
    today: LocalDate
): String {
    val dueDate = dueAt?.atZone(ZoneId.systemDefault())?.toLocalDate()
        ?: return "No deadline"
    val days = ChronoUnit.DAYS.between(today, dueDate)
    return when {
        days < 0 -> "Overdue"
        days == 0L -> "Today"
        days == 1L -> "1 day"
        days < 7L -> "$days days"
        dueDate.month == today.month && dueDate.year == today.year -> "This month"
        else -> dueDate.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }
}

private fun String.prettyInsightDomain(): String {
    return split("_", "-", " ")
        .filter(String::isNotBlank)
        .joinToString(" ") { token ->
            token.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }
        }
        .ifBlank { "Insight" }
}

private fun AeonInsightEntity.trackTone(): TrackTone {
    return when (domain.lowercase(Locale.ROOT)) {
        "focus" -> TrackTone.Focus
        "habit", "habits" -> TrackTone.Habit
        "mood" -> TrackTone.Mood
        "health" -> TrackTone.Health
        "finance" -> TrackTone.Finance
        "goal", "goals" -> TrackTone.Goal
        "learning", "study" -> TrackTone.Learning
        else -> when (severity) {
            InsightSeverityStorage.Warning,
            InsightSeverityStorage.Critical -> TrackTone.Warning

            else -> TrackTone.AI
        }
    }
}
