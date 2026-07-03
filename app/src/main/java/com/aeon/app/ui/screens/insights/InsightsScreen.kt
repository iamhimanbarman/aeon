package com.aeon.app.ui.screens.insights

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.aeon.app.presentation.viewmodel.AeonTodayViewModel
import com.aeon.app.data.local.database.entities.MoodEntryEntity
import com.aeon.app.di.aeonViewModel
import com.aeon.app.presentation.viewmodel.AeonTrackViewModel
import com.aeon.app.presentation.viewmodel.TodayDashboardState
import com.aeon.app.presentation.viewmodel.TrackViewState
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
import com.aeon.app.ui.components.feedback.AeonErrorState
import com.aeon.app.ui.components.feedback.AeonLoading
import com.aeon.app.ui.components.feedback.AeonNoDataState
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.components.layout.AeonScreenConfig
import com.aeon.app.ui.components.layout.aeonPremiumBackgroundBrush
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AeonInsightsRoute(
    onOpenInsight: (String) -> Unit = {},
    onOpenDomain: (String) -> Unit = {},
    onOpenRecommendation: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val trackViewModel = aeonViewModel<AeonTrackViewModel>()
    val todayViewModel = aeonViewModel<AeonTodayViewModel>()
    val trackState by trackViewModel.uiState.collectAsStateWithLifecycle()
    val todayState by todayViewModel.uiState.collectAsStateWithLifecycle()
    val state = remember(trackState, todayState) {
        buildInsightsUiState(
            trackState = trackState,
            todayState = todayState
        )
    }

    InsightsScreen(
        state = state,
        onOpenInsight = onOpenInsight,
        onOpenNotifications = onOpenNotifications,
        modifier = modifier
    )
}

@Immutable
private data class InsightsUiState(
    val dateLabel: String,
    val rangeLabel: String,
    val intelligenceScore: Int,
    val intelligenceLabel: String,
    val summary: String,
    val confidence: Int,
    val trackedDomainCount: Int,
    val activeAlertCount: Int,
    val notificationCount: Int,
    val metrics: List<InsightMetricUi> = emptyList(),
    val domains: List<InsightDomainUi> = emptyList(),
    val trendPoints: List<InsightTrendPointUi> = emptyList(),
    val alerts: List<InsightAlertUi> = emptyList(),
    val recommendations: List<InsightRecommendationUi> = emptyList(),
    val nextAction: InsightRecommendationUi? = null,
    val hasData: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@Immutable
private data class InsightMetricUi(
    val label: String,
    val value: String,
    val caption: String,
    val tone: InsightTone,
    val trend: InsightDirection
)

@Immutable
private data class InsightDomainUi(
    val key: String,
    val title: String,
    val subtitle: String,
    val score: Int,
    val delta: String,
    val tone: InsightTone,
    val isTracked: Boolean
)

@Immutable
private data class InsightTrendPointUi(
    val label: String,
    val score: Int,
    val tracked: Boolean
)

@Immutable
private data class InsightAlertUi(
    val id: String,
    val title: String,
    val body: String,
    val domainLabel: String,
    val confidence: Int,
    val severity: InsightSeverityUi,
    val tone: InsightTone,
    val recommendation: String? = null
)

@Immutable
private data class InsightRecommendationUi(
    val id: String,
    val title: String,
    val body: String,
    val metaPrimary: String,
    val metaSecondary: String,
    val tone: InsightTone,
    val insightId: String? = null
)

private data class InsightDomainSnapshot(
    val key: String,
    val title: String,
    val subtitle: String,
    val score: Int,
    val previousScore: Int,
    val tone: InsightTone,
    val isTracked: Boolean,
    val hasComparisonData: Boolean
)

private enum class InsightDirection {
    Up,
    Down,
    Stable
}

private enum class InsightSeverityUi {
    Info,
    Positive,
    Warning,
    Critical
}

private enum class InsightTone {
    Intelligence,
    Focus,
    Habit,
    Health,
    Mood,
    Finance,
    Goal,
    Warning,
    Success,
    Neutral
}

@Composable
private fun InsightsScreen(
    state: InsightsUiState,
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
                InsightsLoadingState()
            }

            state.error != null -> {
                AeonErrorState(
                    title = "Unable to load insights",
                    message = state.error
                )
            }

            !state.hasData -> {
                AeonNoDataState(
                    title = "Insights will appear here",
                    message = "Log real focus sessions, habits, mood, health, goals, or finance activity to unlock weekly intelligence.",
                    actionText = "Insight alerts",
                    onAction = onOpenNotifications
                )
            }

            else -> {
                InsightHeroCard(state = state)
                InsightActionRow(
                    activeAlertCount = state.activeAlertCount,
                    onOpenNotifications = onOpenNotifications
                )
                InsightMetricGrid(metrics = state.metrics)
                InsightTrendBoard(
                    rangeLabel = state.rangeLabel,
                    points = state.trendPoints
                )
                InsightDomainBoard(domains = state.domains)
                InsightAlertSection(
                    alerts = state.alerts,
                    onOpenInsight = onOpenInsight
                )
                InsightRecommendationSection(
                    nextAction = state.nextAction,
                    recommendations = state.recommendations,
                    onOpenInsight = onOpenInsight
                )
                InsightFooter()
            }
        }
    }
}

@Composable
private fun InsightsLoadingState() {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Compact,
        containerColor = colors.surfaceElevated
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AeonLoading(isLarge = true)
            Text(
                text = "Building your intelligence board",
                style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
            )
            Text(
                text = "Aeon is reading real activity from focus, habits, mood, health, goals, and finance.",
                style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightsHeader(
    state: InsightsUiState,
    onOpenNotifications: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = state.dateLabel,
        title = "Insights",
        subtitle = "Aeon turns your tracked week into clearer patterns, sharper alerts, and calmer next steps.",
        size = AeonSectionHeaderSize.Hero,
        tone = AeonSectionHeaderTone.Premium,
        action = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AeonChip(
                    text = "7-day intelligence",
                    variant = AeonChipVariant.Premium,
                    size = AeonChipSize.Compact
                )
                AeonChip(
                    text = "${state.activeAlertCount} alerts",
                    variant = if (state.activeAlertCount > 0) {
                        AeonChipVariant.Warning
                    } else {
                        AeonChipVariant.Outline
                    },
                    size = AeonChipSize.Compact
                )
                AeonChip(
                    text = "${state.notificationCount} inbox",
                    variant = AeonChipVariant.Outline,
                    size = AeonChipSize.Compact,
                    onClick = onOpenNotifications,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightHeroCard(
    state: InsightsUiState
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Hero,
        containerColor = colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InsightScoreRing(
                score = state.intelligenceScore,
                label = state.intelligenceLabel
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Aeon intelligence",
                    style = AeonTextStyles.CardTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Text(
                    text = state.summary,
                    style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AeonChip(
                        text = "${state.confidence}% confidence",
                        variant = AeonChipVariant.Premium,
                        size = AeonChipSize.Compact
                    )
                    AeonChip(
                        text = "${state.trackedDomainCount} live domains",
                        variant = AeonChipVariant.Info,
                        size = AeonChipSize.Compact
                    )
                }
            }
        }

        state.nextAction?.let { nextAction ->
            Spacer(modifier = Modifier.height(10.dp))
            AeonCard(
                variant = AeonCardVariant.Compact,
                containerColor = colors.surface.copy(alpha = 0.88f),
                borderColor = colors.border.copy(alpha = 0.42f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.brand.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = colors.brand,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Next best move",
                            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                        )
                        Text(
                            text = nextAction.title,
                            style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
                        )
                        Text(
                            text = nextAction.body,
                            style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightScoreRing(
    score: Int,
    label: String
) {
    val colors = AeonThemeTokens.colors
    val progress by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        label = "insight_score_progress"
    )

    Box(
        modifier = Modifier.size(122.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(114.dp)) {
            drawArc(
                color = colors.surface.copy(alpha = 0.92f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )

            drawArc(
                color = colors.brand,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                style = AeonTextStyles.LifeScoreNumber.copy(color = colors.textPrimary)
            )
            Text(
                text = label,
                style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
            )
        }
    }
}

@Composable
private fun InsightActionRow(
    activeAlertCount: Int,
    onOpenNotifications: () -> Unit
) {
    AeonButton(
        text = if (activeAlertCount > 0) {
            "Insight alerts ($activeAlertCount)"
        } else {
            "Insight alerts"
        },
        onClick = onOpenNotifications,
        variant = AeonButtonVariant.Secondary,
        size = AeonButtonSize.Medium,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InsightMetricGrid(
    metrics: List<InsightMetricUi>
) {
    val colors = AeonThemeTokens.colors

    Text(
        text = "Key signals",
        style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { metric ->
                    InsightMetricCard(
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
private fun InsightMetricCard(
    metric: InsightMetricUi,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        containerColor = colors.surfaceElevated
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
                    text = metric.value,
                    style = AeonTextStyles.StatNumber.copy(color = metric.tone.color())
                )
                Text(
                    text = metric.label,
                    style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
                )
                Text(
                    text = metric.caption,
                    style = AeonTextStyles.Micro.copy(color = colors.textSecondary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AeonChip(
                text = metric.trend.label,
                variant = metric.trend.variant,
                size = AeonChipSize.Compact
            )
        }
    }
}

@Composable
private fun InsightTrendBoard(
    rangeLabel: String,
    points: List<InsightTrendPointUi>
) {
    val colors = AeonThemeTokens.colors

    Text(
        text = "Weekly intelligence",
        style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
    )

    if (points.none(InsightTrendPointUi::tracked)) {
        EmptyInsightSectionCard(
            message = "Aeon needs activity across the week before it can plot a reliable trend."
        )
        return
    }

    AeonCard(
        variant = AeonCardVariant.Default,
        containerColor = colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rangeLabel,
                style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
            )
            AeonChip(
                text = "${points.count(InsightTrendPointUi::tracked)} tracked days",
                variant = AeonChipVariant.Outline,
                size = AeonChipSize.Compact
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        InsightLineChart(points = points)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { point ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = point.label,
                        style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
                    )
                    Text(
                        text = if (point.tracked) point.score.toString() else "--",
                        style = AeonTextStyles.Micro.copy(
                            color = if (point.tracked) colors.textPrimary else colors.textTertiary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightLineChart(
    points: List<InsightTrendPointUi>
) {
    val colors = AeonThemeTokens.colors
    val chartPoints = points.map { point -> if (point.tracked) point.score else 0 }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
    ) {
        if (points.count(InsightTrendPointUi::tracked) < 2) return@Canvas

        val visiblePoints = points.filter(InsightTrendPointUi::tracked)
        val min = visiblePoints.minOf { point -> point.score }
        val max = visiblePoints.maxOf { point -> point.score }
        val range = (max - min).coerceAtLeast(1)
        val stepX = size.width / (points.lastIndex).coerceAtLeast(1)

        repeat(4) { index ->
            val y = size.height * (index + 1) / 5f
            drawLine(
                color = colors.divider.copy(alpha = 0.58f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val path = Path()
        var hasStarted = false

        points.forEachIndexed { index, point ->
            if (!point.tracked) return@forEachIndexed
            val normalized = (point.score - min).toFloat() / range.toFloat()
            val x = stepX * index
            val y = size.height - (normalized * size.height)
            if (!hasStarted) {
                path.moveTo(x, y)
                hasStarted = true
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = colors.brand,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        points.forEachIndexed { index, point ->
            if (!point.tracked) return@forEachIndexed
            val normalized = (point.score - min).toFloat() / range.toFloat()
            val x = stepX * index
            val y = size.height - (normalized * size.height)
            drawCircle(
                color = colors.brand,
                radius = if (index == points.indexOfLast { it.tracked }) 5.dp.toPx() else 3.5.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}

@Composable
private fun InsightDomainBoard(
    domains: List<InsightDomainUi>
) {
    val colors = AeonThemeTokens.colors

    Text(
        text = "Domain intelligence",
        style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        domains.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { domain ->
                    InsightDomainCard(
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

@Composable
private fun InsightDomainCard(
    domain: InsightDomainUi,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        containerColor = colors.surfaceElevated,
        borderColor = if (domain.isTracked) {
            domain.tone.color().copy(alpha = 0.30f)
        } else {
            colors.border.copy(alpha = 0.40f)
        }
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
                    text = domain.title,
                    style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
                )
                Text(
                    text = domain.subtitle,
                    style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AeonChip(
                text = if (domain.isTracked) domain.delta else "Idle",
                variant = if (domain.isTracked) AeonChipVariant.Info else AeonChipVariant.Outline,
                size = AeonChipSize.Compact
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        InsightProgressBar(
            progress = domain.score / 100f,
            tone = domain.tone
        )
        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (domain.isTracked) "${domain.score}% signal strength" else "No recent records yet",
            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
        )
    }
}

@Composable
private fun InsightAlertSection(
    alerts: List<InsightAlertUi>,
    onOpenInsight: (String) -> Unit
) {
    val colors = AeonThemeTokens.colors

    Text(
        text = "Insight alerts",
        style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
    )

    if (alerts.isEmpty()) {
        EmptyInsightSectionCard(
            message = "No live alerts right now. Aeon will surface new patterns here as real data changes."
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        alerts.forEach { alert ->
            AeonCard(
                variant = if (alert.severity == InsightSeverityUi.Warning || alert.severity == InsightSeverityUi.Critical) {
                    AeonCardVariant.Insight
                } else {
                    AeonCardVariant.Default
                },
                onClick = { onOpenInsight(alert.id) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(alert.tone.color().copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = alert.severity.icon,
                            contentDescription = null,
                            tint = alert.tone.color(),
                            modifier = Modifier.size(18.dp)
                        )
                    }

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
                                text = alert.title,
                                modifier = Modifier.weight(1f),
                                style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            AeonChip(
                                text = alert.severity.label,
                                variant = alert.severity.variant,
                                size = AeonChipSize.Compact
                            )
                        }

                        Text(
                            text = alert.body,
                            style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                        )

                        alert.recommendation?.takeIf(String::isNotBlank)?.let { recommendation ->
                            Text(
                                text = recommendation,
                                style = AeonTextStyles.Micro.copy(color = colors.textPrimary)
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AeonChip(
                                text = alert.domainLabel,
                                variant = AeonChipVariant.Outline,
                                size = AeonChipSize.Compact
                            )
                            AeonChip(
                                text = "${alert.confidence}% confidence",
                                variant = AeonChipVariant.Info,
                                size = AeonChipSize.Compact
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightRecommendationSection(
    nextAction: InsightRecommendationUi?,
    recommendations: List<InsightRecommendationUi>,
    onOpenInsight: (String) -> Unit
) {
    val colors = AeonThemeTokens.colors

    Text(
        text = "Recommended moves",
        style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
    )

    val items = buildList {
        nextAction?.let(::add)
        addAll(recommendations)
    }

    if (items.isEmpty()) {
        EmptyInsightSectionCard(
            message = "Action cards will appear when Aeon detects a pattern worth acting on."
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { recommendation ->
            AeonCard(
                variant = AeonCardVariant.Compact,
                onClick = recommendation.insightId?.let { insightId ->
                    { onOpenInsight(insightId) }
                },
                containerColor = colors.surfaceElevated
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(recommendation.tone.color().copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = recommendation.tone.color(),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = recommendation.title,
                            style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
                        )
                        Text(
                            text = recommendation.body,
                            style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AeonChip(
                                text = recommendation.metaPrimary,
                                variant = AeonChipVariant.Success,
                                size = AeonChipSize.Compact
                            )
                            AeonChip(
                                text = recommendation.metaSecondary,
                                variant = AeonChipVariant.Outline,
                                size = AeonChipSize.Compact
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightFooter() {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Glass
    ) {
        Text(
            text = "Real intelligence comes from steady logging, not sample data.",
            style = AeonTextStyles.Quote.copy(color = colors.textPrimary)
        )
        Text(
            text = "Track consistently and Aeon will keep this board sharper each week.",
            style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
        )
    }
}

@Composable
private fun EmptyInsightSectionCard(
    message: String
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Compact,
        containerColor = colors.surfaceElevated
    ) {
        Text(
            text = message,
            style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
        )
    }
}

@Composable
private fun InsightProgressBar(
    progress: Float,
    tone: InsightTone
) {
    val colors = AeonThemeTokens.colors
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "insight_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(colors.surface.copy(alpha = 0.82f))
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

private fun buildInsightsUiState(
    trackState: TrackViewState,
    todayState: TodayDashboardState
): InsightsUiState {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())
    val rangeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val habitLogsByHabit = trackState.currentHabitLogs.groupBy(HabitLogEntity::habitId)
    val previousHabitLogsByHabit = trackState.previousHabitLogs.groupBy(HabitLogEntity::habitId)
    val currentMonthBudgets = trackState.budgets.filter { budget ->
        trackState.today >= budget.periodStart && trackState.today <= budget.periodEnd && budget.isActive
    }
    val mergedInsights = (trackState.insights + (todayState.commandCenter?.newInsights ?: emptyList()))
        .distinctBy(AeonInsightEntity::id)
    val currency = trackState.budgets.firstOrNull()?.currency
        ?: trackState.currentMonthTransactions.firstOrNull()?.currency
        ?: trackState.currentTransactions.firstOrNull()?.currency
        ?: "INR"

    val focusScore = trackState.currentFocusSessions.focusScore()
    val previousFocusScore = trackState.previousFocusSessions.focusScore()
    val habitScore = trackState.activeHabits.habitScore(habitLogsByHabit)
    val previousHabitScore = trackState.activeHabits.habitScore(previousHabitLogsByHabit)
    val moodScore = trackState.currentMoodEntries.averageMoodScore() ?: 0
    val previousMoodScore = trackState.previousMoodEntries.averageMoodScore() ?: 0
    val healthScore = trackState.currentHealthEntries.healthScore(trackState.currentDoseLogs)
    val previousHealthScore = trackState.previousHealthEntries.healthScore(trackState.previousDoseLogs)
    val financeScore = trackState.currentMonthTransactions.financeScore(
        currentWeekTransactions = trackState.currentTransactions,
        budgets = currentMonthBudgets
    )
    val previousFinanceScore = trackState.previousTransactions.previousFinanceScore()
    val goalScore = trackState.activeGoals.goalScore()

    val domainSnapshots = listOf(
        InsightDomainSnapshot(
            key = "focus",
            title = "Focus",
            subtitle = if (trackState.currentFocusSessions.isNotEmpty()) {
                buildString {
                    append(trackState.currentFocusSessions.focusMinutes().formatMinutes())
                    append(" across ${trackState.currentFocusSessions.size} sessions")
                    trackState.currentFocusSessions.averageQualityScore()?.let { quality ->
                        append(" and $quality quality")
                    }
                }
            } else {
                "No focus sessions logged in the last 7 days."
            },
            score = focusScore,
            previousScore = previousFocusScore,
            tone = InsightTone.Focus,
            isTracked = trackState.currentFocusSessions.isNotEmpty(),
            hasComparisonData = trackState.currentFocusSessions.isNotEmpty() || trackState.previousFocusSessions.isNotEmpty()
        ),
        InsightDomainSnapshot(
            key = "habits",
            title = "Habits",
            subtitle = if (trackState.activeHabits.isNotEmpty()) {
                "${habitLogsByHabit.values.flatten().doneCount()} completions across ${trackState.activeHabits.size} active habits"
            } else {
                "No active habits yet."
            },
            score = habitScore,
            previousScore = previousHabitScore,
            tone = InsightTone.Habit,
            isTracked = trackState.activeHabits.isNotEmpty(),
            hasComparisonData = trackState.activeHabits.isNotEmpty() || trackState.previousHabitLogs.isNotEmpty()
        ),
        InsightDomainSnapshot(
            key = "mood",
            title = "Mood",
            subtitle = if (trackState.currentMoodEntries.isNotEmpty()) {
                "${trackState.currentMoodEntries.size} mood check-ins recorded this week"
            } else {
                "No mood entries logged in the last 7 days."
            },
            score = moodScore,
            previousScore = previousMoodScore,
            tone = InsightTone.Mood,
            isTracked = trackState.currentMoodEntries.isNotEmpty(),
            hasComparisonData = trackState.currentMoodEntries.isNotEmpty() || trackState.previousMoodEntries.isNotEmpty()
        ),
        InsightDomainSnapshot(
            key = "health",
            title = "Health",
            subtitle = healthSubtitle(
                entries = trackState.currentHealthEntries,
                doseLogs = trackState.currentDoseLogs
            ),
            score = healthScore,
            previousScore = previousHealthScore,
            tone = InsightTone.Health,
            isTracked = trackState.currentHealthEntries.isNotEmpty() || trackState.currentDoseLogs.isNotEmpty(),
            hasComparisonData = trackState.currentHealthEntries.isNotEmpty() ||
                trackState.currentDoseLogs.isNotEmpty() ||
                trackState.previousHealthEntries.isNotEmpty() ||
                trackState.previousDoseLogs.isNotEmpty()
        ),
        InsightDomainSnapshot(
            key = "finance",
            title = "Finance",
            subtitle = financeSubtitle(
                currency = currency,
                currentWeekTransactions = trackState.currentTransactions,
                currentMonthTransactions = trackState.currentMonthTransactions,
                budgets = currentMonthBudgets
            ),
            score = financeScore,
            previousScore = previousFinanceScore,
            tone = InsightTone.Finance,
            isTracked = trackState.currentTransactions.isNotEmpty() ||
                trackState.currentMonthTransactions.isNotEmpty() ||
                currentMonthBudgets.isNotEmpty(),
            hasComparisonData = trackState.currentTransactions.isNotEmpty() || trackState.previousTransactions.isNotEmpty()
        ),
        InsightDomainSnapshot(
            key = "goals",
            title = "Goals",
            subtitle = if (trackState.activeGoals.isNotEmpty()) {
                "${goalScore}% average progress across ${trackState.activeGoals.size} active goals"
            } else {
                "No active goals yet."
            },
            score = goalScore,
            previousScore = goalScore,
            tone = InsightTone.Goal,
            isTracked = trackState.activeGoals.isNotEmpty(),
            hasComparisonData = trackState.activeGoals.isNotEmpty()
        )
    )

    val trackedDomains = domainSnapshots.filter(InsightDomainSnapshot::isTracked)
    val trendPoints = trackState.toInsightTrendPoints(habitLogsByHabit)
    val trackedDayCount = trendPoints.count(InsightTrendPointUi::tracked)
    val averageInsightConfidence = mergedInsights
        .map(AeonInsightEntity::confidence)
        .takeIf(List<Int>::isNotEmpty)
        ?.average()
        ?.roundToInt()
    val domainAverage = trackedDomains
        .map(InsightDomainSnapshot::score)
        .takeIf(List<Int>::isNotEmpty)
        ?.average()
        ?.roundToInt()
        ?: 0
    val coverageScore = if (trackedDomains.isEmpty()) {
        0
    } else {
        ((trackedDomains.size / 6f) * 100f).roundToInt().coerceIn(0, 100)
    }
    val intelligenceScore = listOfNotNull(
        todayState.lifeScore?.score,
        domainAverage.takeIf { trackedDomains.isNotEmpty() },
        averageInsightConfidence,
        coverageScore.takeIf { trackedDomains.isNotEmpty() }
    ).takeIf(List<Int>::isNotEmpty)
        ?.average()
        ?.roundToInt()
        ?.coerceIn(0, 100)
        ?: 0
    val confidence = listOfNotNull(
        averageInsightConfidence,
        todayState.lifeScore?.score,
        coverageScore.takeIf { trackedDomains.isNotEmpty() },
        ((trackedDayCount / 7f) * 100f).roundToInt().takeIf { trackedDayCount > 0 }
    ).takeIf(List<Int>::isNotEmpty)
        ?.average()
        ?.roundToInt()
        ?.coerceIn(18, 97)
        ?: 0
    val strongestDomain = trackedDomains.maxByOrNull(InsightDomainSnapshot::score)
    val weakestDomain = trackedDomains.minByOrNull(InsightDomainSnapshot::score)
    val topAlert = mergedInsights
        .sortedWith(
            compareByDescending<AeonInsightEntity> { it.severity.insightSeverityRank() }
                .thenByDescending(AeonInsightEntity::confidence)
                .thenByDescending(AeonInsightEntity::createdAt)
        )
        .firstOrNull()

    val summary = when {
        topAlert?.recommendation?.isNotBlank() == true -> {
            topAlert.recommendation
        }

        topAlert != null -> {
            topAlert.body
        }

        strongestDomain != null && weakestDomain != null && strongestDomain.key == weakestDomain.key -> {
            "${strongestDomain.title} is the only domain with recent signal. Add more logs to make weekly intelligence more reliable."
        }

        strongestDomain != null && weakestDomain != null && abs(strongestDomain.score - weakestDomain.score) <= 8 -> {
            "Your tracked areas are moving with a fairly even rhythm this week. The next gain comes from steady consistency."
        }

        strongestDomain != null && weakestDomain != null -> {
            "${strongestDomain.title} is leading this week, while ${weakestDomain.title.lowercase(Locale.getDefault())} needs the most support."
        }

        else -> {
            "Log real activity across the app and Aeon will convert it into patterns, risks, and next actions."
        }
    }

    val alerts = mergedInsights
        .sortedWith(
            compareByDescending<AeonInsightEntity> { it.severity.insightSeverityRank() }
                .thenByDescending(AeonInsightEntity::confidence)
                .thenByDescending(AeonInsightEntity::createdAt)
        )
        .map { insight ->
            InsightAlertUi(
                id = insight.id,
                title = insight.title,
                body = insight.body,
                domainLabel = insight.domain.prettyInsightDomain(),
                confidence = insight.confidence.coerceIn(0, 100),
                severity = insight.severity.toInsightSeverityUi(),
                tone = insight.toInsightTone(),
                recommendation = insight.recommendation
            )
        }

    val baseRecommendations = mergedInsights
        .mapNotNull { insight ->
            insight.recommendation
                ?.takeIf(String::isNotBlank)
                ?.let { recommendation ->
                    InsightRecommendationUi(
                        id = "insight_recommendation_${insight.id}",
                        title = insight.title,
                        body = recommendation,
                        metaPrimary = "${insight.confidence}% confidence",
                        metaSecondary = insight.domain.prettyInsightDomain(),
                        tone = insight.toInsightTone(),
                        insightId = insight.id
                    )
                }
        }

    val nextAction = todayState.commandCenter?.nextBestAction?.let { action ->
        InsightRecommendationUi(
            id = "next_best_action",
            title = action.title,
            body = action.body,
            metaPrimary = action.priority.name.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.getDefault()) },
            metaSecondary = "Command center",
            tone = when (action.priority.name.lowercase(Locale.ROOT)) {
                "critical", "high" -> InsightTone.Warning
                else -> InsightTone.Intelligence
            }
        )
    }

    val heuristicRecommendations = trackedDomains
        .sortedBy(InsightDomainSnapshot::score)
        .take(2)
        .mapNotNull { weakest ->
            weakest.heuristicRecommendation()
        }

    val recommendations = (baseRecommendations + heuristicRecommendations)
        .distinctBy { recommendation ->
            recommendation.title.lowercase(Locale.ROOT)
        }
        .take(4)

    val metrics = listOf(
        InsightMetricUi(
            label = "Clarity",
            value = if (trackedDomains.isNotEmpty()) intelligenceScore.toString() else "--",
            caption = if (trackedDomains.isNotEmpty()) {
                "${confidence}% confidence across ${trackedDomains.size} live domains"
            } else {
                "No weekly signal yet"
            },
            tone = InsightTone.Intelligence,
            trend = trackedDomains.insightDirection(
                current = intelligenceScore,
                previous = domainAverage
            )
        ),
        InsightMetricUi(
            label = "Focus",
            value = if (trackState.currentFocusSessions.isNotEmpty()) {
                focusScore.toString()
            } else {
                "--"
            },
            caption = if (trackState.currentFocusSessions.isNotEmpty()) {
                "${trackState.currentFocusSessions.focusMinutes().formatMinutes()} across ${trackState.currentFocusSessions.size} sessions"
            } else {
                "No focus sessions this week"
            },
            tone = InsightTone.Focus,
            trend = insightDirectionFromScores(
                current = focusScore,
                previous = previousFocusScore
            )
        ),
        InsightMetricUi(
            label = "Health",
            value = if (trackState.currentHealthEntries.isNotEmpty() || trackState.currentDoseLogs.isNotEmpty()) {
                healthScore.toString()
            } else {
                "--"
            },
            caption = healthSubtitle(
                entries = trackState.currentHealthEntries,
                doseLogs = trackState.currentDoseLogs
            ),
            tone = InsightTone.Health,
            trend = insightDirectionFromScores(
                current = healthScore,
                previous = previousHealthScore
            )
        ),
        InsightMetricUi(
            label = "Mood",
            value = trackState.currentMoodEntries.averageMoodScore().toMoodLabel(),
            caption = if (trackState.currentMoodEntries.isNotEmpty()) {
                "${trackState.currentMoodEntries.size} entries with $moodScore% average mood"
            } else {
                "No mood check-ins this week"
            },
            tone = InsightTone.Mood,
            trend = insightDirectionFromScores(
                current = moodScore,
                previous = previousMoodScore
            )
        )
    )

    return InsightsUiState(
        dateLabel = trackState.today.format(dateFormatter),
        rangeLabel = "${trackState.rangeStart.format(rangeFormatter)} - ${trackState.rangeEnd.format(rangeFormatter)}",
        intelligenceScore = intelligenceScore,
        intelligenceLabel = intelligenceScore.labelForIntelligence(),
        summary = summary,
        confidence = confidence,
        trackedDomainCount = trackedDomains.size,
        activeAlertCount = alerts.size,
        notificationCount = todayState.commandCenter?.unreadNotificationCount
            ?: todayState.lifeScore?.unreadNotifications
            ?: 0,
        metrics = metrics,
        domains = domainSnapshots.map { snapshot ->
            InsightDomainUi(
                key = snapshot.key,
                title = snapshot.title,
                subtitle = snapshot.subtitle,
                score = snapshot.score,
                delta = formatInsightDelta(
                    current = snapshot.score,
                    previous = snapshot.previousScore,
                    hasComparisonData = snapshot.hasComparisonData
                ),
                tone = snapshot.tone,
                isTracked = snapshot.isTracked
            )
        },
        trendPoints = trendPoints,
        alerts = alerts,
        recommendations = recommendations,
        nextAction = nextAction,
        hasData = trackedDomains.isNotEmpty() || alerts.isNotEmpty() || recommendations.isNotEmpty(),
        isLoading = trackState.isLoading || todayState.isLoading,
        error = trackState.error ?: todayState.error
    )
}

private fun TrackViewState.toInsightTrendPoints(
    habitLogsByHabit: Map<String, List<HabitLogEntity>>
): List<InsightTrendPointUi> {
    val zone = ZoneId.systemDefault()
    val focusByDate = currentFocusSessions.groupBy { session ->
        session.startedAt.atZone(zone).toLocalDate()
    }
    val moodByDate = currentMoodEntries.groupBy(MoodEntryEntity::logDate)
    val healthByDate = currentHealthEntries.groupBy(HealthEntryEntity::logDate)
    val doseByDate = currentDoseLogs.groupBy { log ->
        log.scheduledAt.atZone(zone).toLocalDate()
    }
    val habitByDate = habitLogsByHabit.values.flatten().groupBy(HabitLogEntity::logDate)

    return (0L..6L).map { offset ->
        val date = rangeStart.plusDays(offset)
        val candidates = mutableListOf<Int>()

        focusByDate[date]
            .orEmpty()
            .takeIf(List<Any>::isNotEmpty)
            ?.focusScore()
            ?.takeIf { it > 0 }
            ?.let(candidates::add)

        if (activeHabits.isNotEmpty()) {
            val completedHabits = habitByDate[date].orEmpty().doneCount()
            val habitScore = ((completedHabits.toFloat() / activeHabits.size) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
            candidates += habitScore
        }

        moodByDate[date].orEmpty().averageMoodScore()?.let(candidates::add)

        if (healthByDate[date].orEmpty().isNotEmpty() || doseByDate[date].orEmpty().isNotEmpty()) {
            candidates += healthByDate[date].orEmpty().healthScore(doseByDate[date].orEmpty())
        }

        val tracked = candidates.isNotEmpty()
        val score = candidates
            .takeIf(List<Int>::isNotEmpty)
            ?.average()
            ?.roundToInt()
            ?: 0

        InsightTrendPointUi(
            label = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
            score = score,
            tracked = tracked
        )
    }
}

private val InsightDirection.label: String
    get() = when (this) {
        InsightDirection.Up -> "Up"
        InsightDirection.Down -> "Down"
        InsightDirection.Stable -> "Stable"
    }

private val InsightDirection.variant: AeonChipVariant
    get() = when (this) {
        InsightDirection.Up -> AeonChipVariant.Success
        InsightDirection.Down -> AeonChipVariant.Warning
        InsightDirection.Stable -> AeonChipVariant.Outline
    }

private val InsightSeverityUi.label: String
    get() = when (this) {
        InsightSeverityUi.Info -> "Info"
        InsightSeverityUi.Positive -> "Positive"
        InsightSeverityUi.Warning -> "Warning"
        InsightSeverityUi.Critical -> "Critical"
    }

private val InsightSeverityUi.variant: AeonChipVariant
    get() = when (this) {
        InsightSeverityUi.Info -> AeonChipVariant.Info
        InsightSeverityUi.Positive -> AeonChipVariant.Success
        InsightSeverityUi.Warning -> AeonChipVariant.Warning
        InsightSeverityUi.Critical -> AeonChipVariant.Danger
    }

private val InsightSeverityUi.icon: ImageVector
    get() = when (this) {
        InsightSeverityUi.Info -> Icons.Outlined.Timeline
        InsightSeverityUi.Positive -> Icons.Outlined.CheckCircleOutline
        InsightSeverityUi.Warning -> Icons.Outlined.WarningAmber
        InsightSeverityUi.Critical -> Icons.Outlined.WarningAmber
    }

@Composable
private fun InsightTone.color(): Color {
    val colors = AeonThemeTokens.colors
    return when (this) {
        InsightTone.Intelligence -> colors.brand
        InsightTone.Focus -> colors.focus
        InsightTone.Habit -> colors.habit
        InsightTone.Health -> colors.health
        InsightTone.Mood -> colors.mood
        InsightTone.Finance -> colors.finance
        InsightTone.Goal -> colors.goal
        InsightTone.Warning -> colors.warning
        InsightTone.Success -> colors.success
        InsightTone.Neutral -> colors.textTertiary
    }
}

private fun String.insightSeverityRank(): Int {
    return when (lowercase(Locale.ROOT)) {
        InsightSeverityStorage.Critical -> 4
        InsightSeverityStorage.Warning -> 3
        InsightSeverityStorage.Positive -> 2
        else -> 1
    }
}

private fun String.toInsightSeverityUi(): InsightSeverityUi {
    return when (lowercase(Locale.ROOT)) {
        InsightSeverityStorage.Critical -> InsightSeverityUi.Critical
        InsightSeverityStorage.Warning -> InsightSeverityUi.Warning
        InsightSeverityStorage.Positive -> InsightSeverityUi.Positive
        else -> InsightSeverityUi.Info
    }
}

private fun AeonInsightEntity.toInsightTone(): InsightTone {
    return when (domain.lowercase(Locale.ROOT)) {
        "focus" -> InsightTone.Focus
        "habit", "habits" -> InsightTone.Habit
        "mood" -> InsightTone.Mood
        "health" -> InsightTone.Health
        "finance" -> InsightTone.Finance
        "goal", "goals" -> InsightTone.Goal
        else -> when (severity.lowercase(Locale.ROOT)) {
            InsightSeverityStorage.Warning,
            InsightSeverityStorage.Critical -> InsightTone.Warning
            InsightSeverityStorage.Positive -> InsightTone.Success
            else -> InsightTone.Intelligence
        }
    }
}

private fun String.prettyInsightDomain(): String {
    return split("_", "-", " ")
        .filter(String::isNotBlank)
        .joinToString(" ") { token ->
            token.replaceFirstChar { character -> character.titlecase(Locale.getDefault()) }
        }
        .ifBlank { "Insight" }
}

private fun Int.labelForIntelligence(): String {
    return when {
        this >= 85 -> "Clear pattern"
        this >= 70 -> "Strong signal"
        this >= 55 -> "Useful signal"
        this >= 35 -> "Early signal"
        this > 0 -> "Weak signal"
        else -> "No signal"
    }
}

private fun formatInsightDelta(
    current: Int,
    previous: Int,
    hasComparisonData: Boolean
): String {
    if (!hasComparisonData) return "New"
    val change = current - previous
    if (abs(change) < 3) return "Steady"
    return if (change > 0) {
        "+$change pts"
    } else {
        "$change pts"
    }
}

private fun insightDirectionFromScores(
    current: Int,
    previous: Int
): InsightDirection {
    return when {
        abs(current - previous) < 3 -> InsightDirection.Stable
        current > previous -> InsightDirection.Up
        else -> InsightDirection.Down
    }
}

private fun List<InsightDomainSnapshot>.insightDirection(
    current: Int,
    previous: Int?
): InsightDirection {
    return previous?.let { prior ->
        insightDirectionFromScores(current = current, previous = prior)
    } ?: when {
        isEmpty() -> InsightDirection.Stable
        else -> InsightDirection.Up
    }
}

private fun InsightDomainSnapshot.heuristicRecommendation(): InsightRecommendationUi? {
    if (!isTracked) return null

    return when (key) {
        "focus" -> InsightRecommendationUi(
            id = "heuristic_focus",
            title = "Protect one deep-work block",
            body = "Focus is the weak point this week. Reserve one uninterrupted 25-minute block before adding new work.",
            metaPrimary = "High leverage",
            metaSecondary = "Low effort",
            tone = InsightTone.Focus
        )

        "habits" -> InsightRecommendationUi(
            id = "heuristic_habits",
            title = "Finish one easy habit early",
            body = "An early completion helps the rest of the day stabilize and improves pattern confidence for Aeon.",
            metaPrimary = "Quick win",
            metaSecondary = "Habit support",
            tone = InsightTone.Habit
        )

        "mood" -> InsightRecommendationUi(
            id = "heuristic_mood",
            title = "Log one calm check-in",
            body = "A simple mood check-in gives Aeon a cleaner emotional baseline and makes the next insight less noisy.",
            metaPrimary = "Gentle reset",
            metaSecondary = "Mood support",
            tone = InsightTone.Mood
        )

        "health" -> InsightRecommendationUi(
            id = "heuristic_health",
            title = "Capture one recovery signal",
            body = "Log water, sleep, activity, or medicine so health stops being the blind spot in your weekly pattern.",
            metaPrimary = "Higher confidence",
            metaSecondary = "Health support",
            tone = InsightTone.Health
        )

        "finance" -> InsightRecommendationUi(
            id = "heuristic_finance",
            title = "Review this month's expense flow",
            body = "Finance is under pressure. A quick look at this month's expense ledger can reduce drift before it compounds.",
            metaPrimary = "Prevent drift",
            metaSecondary = "Finance support",
            tone = InsightTone.Finance
        )

        "goals" -> InsightRecommendationUi(
            id = "heuristic_goals",
            title = "Advance one active goal",
            body = "Move one milestone forward so your weekly momentum is not carried by routine work alone.",
            metaPrimary = "Progress boost",
            metaSecondary = "Goal support",
            tone = InsightTone.Goal
        )

        else -> null
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

private fun List<MoodEntryEntity>.averageMoodScore(): Int? {
    return takeIf(List<MoodEntryEntity>::isNotEmpty)
        ?.map(MoodEntryEntity::moodScore)
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
    mapNotNull(HealthEntryEntity::score)
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
    return takeIf(List<com.aeon.app.data.local.database.entities.GoalEntity>::isNotEmpty)
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
            "No finance activity in the last 7 days."
        }
    }
}

private fun healthSubtitle(
    entries: List<HealthEntryEntity>,
    doseLogs: List<MedicineDoseLogEntity>
): String {
    return when {
        doseLogs.isNotEmpty() -> {
            "${doseLogs.count { log -> log.takenAt != null }} of ${doseLogs.size} doses taken this week"
        }

        entries.isNotEmpty() -> {
            "${entries.size} health logs captured this week"
        }

        else -> {
            "No health activity in the last 7 days."
        }
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
        formatter.currency = Currency.getInstance(currencyCode.uppercase(Locale.ROOT))
        formatter.maximumFractionDigits = 0
        formatter.minimumFractionDigits = 0
        formatter.format(this)
    }.getOrElse {
        "${currencyCode.uppercase(Locale.ROOT)} ${setScale(0, RoundingMode.HALF_UP)}"
    }
}
