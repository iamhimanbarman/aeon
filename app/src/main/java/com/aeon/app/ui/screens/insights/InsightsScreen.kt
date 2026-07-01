package com.aeon.app.ui.screens.insights

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
import com.aeon.app.ui.components.layout.AeonScreenConfig
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * INSIGHTS SCREEN
 *
 * Purpose:
 * Premium intelligence dashboard for Aeon.
 *
 * Responsibilities:
 * - Explain life patterns clearly
 * - Show domain-level insight
 * - Show risks, opportunities, predictions, and recommendations
 * - Convert raw tracking data into calm, actionable guidance
 *
 * Senior Developer Rule:
 * This screen is UI-state driven.
 * It must not call AI engine, database, repository, notification engine, or NavController directly.
 */


// ----------------------------------------------------
// Route
// ----------------------------------------------------

@Composable
fun AeonInsightsRoute(
    onOpenInsight: (String) -> Unit = {},
    onOpenDomain: (String) -> Unit = {},
    onOpenRecommendation: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state = rememberInsightsUiState()

    InsightsScreen(
        state = state,
        onOpenInsight = onOpenInsight,
        onOpenDomain = onOpenDomain,
        onOpenRecommendation = onOpenRecommendation,
        onOpenNotifications = onOpenNotifications,
        modifier = modifier
    )
}


// ----------------------------------------------------
// State
// ----------------------------------------------------

@Immutable
data class InsightsUiState(
    val dateLabel: String,
    val rangeLabel: String = "7-day intelligence",
    val intelligenceScore: Int = 84,
    val intelligenceLabel: String = "Clear pattern",
    val summary: String = "Your focus improves when habits are completed before evening. Health and recovery are the main constraints this week.",
    val confidence: Int = 88,
    val trendPoints: List<Int> = listOf(58, 64, 61, 72, 76, 81, 84),
    val metrics: List<InsightMetricUi> = defaultInsightMetrics(),
    val domains: List<InsightDomainUi> = defaultInsightDomains(),
    val recommendations: List<InsightRecommendationUi> = defaultRecommendations(),
    val patterns: List<InsightPatternUi> = defaultPatterns(),
    val forecasts: List<InsightForecastUi> = defaultForecasts(),
    val risks: List<InsightRiskUi> = defaultRisks()
)


@Immutable
data class InsightMetricUi(
    val label: String,
    val value: String,
    val caption: String,
    val tone: InsightTone,
    val trend: InsightTrend
)


@Immutable
data class InsightDomainUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val score: Int,
    val signal: String,
    val tone: InsightTone
)


@Immutable
data class InsightRecommendationUi(
    val id: String,
    val title: String,
    val body: String,
    val effort: String,
    val impact: String,
    val tone: InsightTone
)


@Immutable
data class InsightPatternUi(
    val id: String,
    val title: String,
    val body: String,
    val evidence: String,
    val tone: InsightTone
)


@Immutable
data class InsightForecastUi(
    val title: String,
    val value: String,
    val body: String,
    val tone: InsightTone
)


@Immutable
data class InsightRiskUi(
    val title: String,
    val body: String,
    val severity: InsightSeverity,
    val tone: InsightTone
)


enum class InsightTrend {
    Up,
    Down,
    Stable
}


enum class InsightSeverity {
    Low,
    Medium,
    High
}


enum class InsightTone {
    Intelligence,
    Focus,
    Habit,
    Health,
    Mood,
    Finance,
    Learning,
    Goal,
    Warning,
    Success,
    Neutral
}


// ----------------------------------------------------
// Remember State
// ----------------------------------------------------

@Composable
fun rememberInsightsUiState(): InsightsUiState {
    val dateLabel = remember {
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
    }

    return remember(dateLabel) {
        InsightsUiState(
            dateLabel = dateLabel
        )
    }
}


// ----------------------------------------------------
// Screen
// ----------------------------------------------------

@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onOpenInsight: (String) -> Unit,
    onOpenDomain: (String) -> Unit,
    onOpenRecommendation: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    AeonScreen(
        modifier = modifier,
        config = AeonScreenConfig(safeDrawing = false)
    ) {
        InsightsHeader(state = state)

        IntelligenceScoreCard(state = state)

        InsightsPrimaryActions(onOpenNotifications = onOpenNotifications)

        InsightMetricGrid(
            metrics = state.metrics
        )

        IntelligenceTrendCard(
            points = state.trendPoints
        )

        InsightDomainSection(
            domains = state.domains,
            onOpenDomain = onOpenDomain
        )

        RecommendationSection(
            recommendations = state.recommendations,
            onOpenRecommendation = onOpenRecommendation
        )

        PatternSection(
            patterns = state.patterns,
            onOpenInsight = onOpenInsight
        )

        ForecastSection(
            forecasts = state.forecasts
        )

        RiskSection(
            risks = state.risks
        )

        InsightsFooter()
    }
}


// ----------------------------------------------------
// Header
// ----------------------------------------------------

@Composable
private fun InsightsHeader(state: InsightsUiState) {
    AeonSectionHeader(
        eyebrow = state.dateLabel,
        title = "Insights",
        subtitle = "Aeon turns your tracked life data into clear patterns, risks, and next actions.",
        size = AeonSectionHeaderSize.Hero,
        tone = AeonSectionHeaderTone.Premium,
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
// Intelligence Score
// ----------------------------------------------------

@Composable
private fun IntelligenceScoreCard(state: InsightsUiState) {
    AeonCard(
        variant = AeonCardVariant.Hero
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InsightScoreRing(
                score = state.intelligenceScore,
                label = state.intelligenceLabel
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Text(
                    text = "Aeon intelligence",
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = state.summary,
                    style = AeonTextStyles.CardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AeonChip(
                        text = "${state.confidence}% confidence",
                        variant = AeonChipVariant.Premium,
                        size = AeonChipSize.Compact
                    )

                    AeonChip(
                        text = "Actionable",
                        variant = AeonChipVariant.Success,
                        size = AeonChipSize.Compact
                    )
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
    val progress by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        label = "insight_score_progress"
    )

    val ringColor = InsightTone.Intelligence.color()
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
private fun InsightsPrimaryActions(onOpenNotifications: () -> Unit) {
        AeonButton(
            text = "Insight alerts",
            onClick = onOpenNotifications,
            variant = AeonButtonVariant.Secondary,
            size = AeonButtonSize.Medium,
            modifier = Modifier.fillMaxWidth()
        )
}


// ----------------------------------------------------
// Metrics
// ----------------------------------------------------

@Composable
private fun InsightMetricGrid(
    metrics: List<InsightMetricUi>
) {
    AeonSectionHeader(
        eyebrow = "Snapshot",
        title = "Key signals",
        subtitle = "The most important movement in your recent data.",
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
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = metric.caption,
                    style = AeonTextStyles.Micro,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AeonChip(
                text = metric.trend.label(),
                variant = metric.trend.variant(),
                size = AeonChipSize.Compact
            )
        }
    }
}


// ----------------------------------------------------
// Trend
// ----------------------------------------------------

@Composable
private fun IntelligenceTrendCard(
    points: List<Int>
) {
    AeonSectionHeader(
        eyebrow = "Trajectory",
        title = "Insight trend",
        subtitle = "A smooth view of how your overall pattern clarity is moving.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Default
    ) {
        InsightLineChart(
            points = points,
            tone = InsightTone.Intelligence
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { index, value ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = listOf("M", "T", "W", "T", "F", "S", "S").getOrElse(index) { "" },
                        style = AeonTextStyles.Micro,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = value.toString(),
                        style = AeonTextStyles.Micro,
                        color = if (index == points.lastIndex) {
                            InsightTone.Intelligence.color()
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun InsightLineChart(
    points: List<Int>,
    tone: InsightTone
) {
    val color = tone.color()
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
    ) {
        if (points.size < 2) return@Canvas

        val min = points.minOrNull() ?: 0
        val max = points.maxOrNull() ?: 100
        val range = (max - min).takeIf { it > 0 } ?: 1

        val stepX = size.width / (points.lastIndex).coerceAtLeast(1)

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


// ----------------------------------------------------
// Domains
// ----------------------------------------------------

@Composable
private fun InsightDomainSection(
    domains: List<InsightDomainUi>,
    onOpenDomain: (String) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Domain intelligence",
        title = "Where your life is moving",
        subtitle = "Aeon compares focus, habits, mood, health, finance, and learning.",
        size = AeonSectionHeaderSize.Medium
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        domains.forEach { domain ->
            InsightDomainCard(
                domain = domain,
                onOpenDomain = onOpenDomain
            )
        }
    }
}


@Composable
private fun InsightDomainCard(
    domain: InsightDomainUi,
    onOpenDomain: (String) -> Unit
) {
    AeonCard(
        variant = AeonCardVariant.Elevated,
        onClick = {
            onOpenDomain(domain.id)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InsightMiniRing(
                score = domain.score,
                tone = domain.tone
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

                    AeonChip(
                        text = domain.signal,
                        variant = if (domain.score >= 75) {
                            AeonChipVariant.Success
                        } else {
                            AeonChipVariant.Warning
                        },
                        size = AeonChipSize.Compact
                    )
                }

                InsightProgressBar(
                    progress = domain.score / 100f,
                    tone = domain.tone
                )
            }
        }
    }
}


@Composable
private fun InsightMiniRing(
    score: Int,
    tone: InsightTone
) {
    val progress by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        label = "insight_domain_ring"
    )

    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val toneColor = tone.color()

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
                color = toneColor,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(
                    width = 6.dp.toPx(),
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
// Recommendations
// ----------------------------------------------------

@Composable
private fun RecommendationSection(
    recommendations: List<InsightRecommendationUi>,
    onOpenRecommendation: (String) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Next actions",
        title = "Recommended moves",
        subtitle = "Small actions with high leverage for the next 24 hours.",
        size = AeonSectionHeaderSize.Medium,
        tone = AeonSectionHeaderTone.Premium
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        recommendations.forEach { recommendation ->
            RecommendationCard(
                recommendation = recommendation,
                onOpenRecommendation = onOpenRecommendation
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecommendationCard(
    recommendation: InsightRecommendationUi,
    onOpenRecommendation: (String) -> Unit
) {
    AeonCard(
        variant = AeonCardVariant.Insight,
        onClick = {
            onOpenRecommendation(recommendation.id)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.Top
        ) {
            InsightSymbolBadge(
                symbol = "✦",
                tone = recommendation.tone
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Text(
                    text = recommendation.title,
                    style = AeonTextStyles.InsightTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = recommendation.body,
                    style = AeonTextStyles.InsightBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall),
                    verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
                ) {
                    AeonChip(
                        text = "Impact: ${recommendation.impact}",
                        variant = AeonChipVariant.Success,
                        size = AeonChipSize.Compact
                    )

                    AeonChip(
                        text = "Effort: ${recommendation.effort}",
                        variant = AeonChipVariant.Outline,
                        size = AeonChipSize.Compact
                    )
                }
            }
        }
    }
}


// ----------------------------------------------------
// Patterns
// ----------------------------------------------------

@Composable
private fun PatternSection(
    patterns: List<InsightPatternUi>,
    onOpenInsight: (String) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Detected patterns",
        title = "What Aeon noticed",
        subtitle = "Patterns become useful when they are understandable and repeatable.",
        size = AeonSectionHeaderSize.Medium
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        patterns.forEach { pattern ->
            PatternCard(
                pattern = pattern,
                onOpenInsight = onOpenInsight
            )
        }
    }
}


@Composable
private fun PatternCard(
    pattern: InsightPatternUi,
    onOpenInsight: (String) -> Unit
) {
    AeonCard(
        variant = AeonCardVariant.Default,
        onClick = {
            onOpenInsight(pattern.id)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.Top
        ) {
            InsightSymbolBadge(
                symbol = "◎",
                tone = pattern.tone
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
            ) {
                Text(
                    text = pattern.title,
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = pattern.body,
                    style = AeonTextStyles.CardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AeonChip(
                    text = pattern.evidence,
                    variant = AeonChipVariant.Info,
                    size = AeonChipSize.Compact
                )
            }
        }
    }
}


// ----------------------------------------------------
// Forecasts
// ----------------------------------------------------

@Composable
private fun ForecastSection(
    forecasts: List<InsightForecastUi>
) {
    AeonSectionHeader(
        eyebrow = "Forecast",
        title = "Tomorrow's prediction",
        subtitle = "Aeon estimates what may happen if your current rhythm continues.",
        size = AeonSectionHeaderSize.Medium
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        forecasts.forEach { forecast ->
            AeonCard(
                variant = AeonCardVariant.Compact
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InsightSymbolBadge(
                        symbol = "↗",
                        tone = forecast.tone
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
                    ) {
                        Text(
                            text = forecast.title,
                            style = AeonTextStyles.CardTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = forecast.body,
                            style = AeonTextStyles.CardSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = forecast.value,
                        style = AeonTextStyles.StatNumber,
                        color = forecast.tone.color()
                    )
                }
            }
        }
    }
}


// ----------------------------------------------------
// Risks
// ----------------------------------------------------

@Composable
private fun RiskSection(
    risks: List<InsightRiskUi>
) {
    AeonSectionHeader(
        eyebrow = "Attention risks",
        title = "What can break the day",
        subtitle = "Useful insights should also warn you before patterns decline.",
        size = AeonSectionHeaderSize.Medium
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
    ) {
        risks.forEach { risk ->
            AeonCard(
                variant = if (risk.severity == InsightSeverity.High) {
                    AeonCardVariant.Insight
                } else {
                    AeonCardVariant.Default
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
                    verticalAlignment = Alignment.Top
                ) {
                    InsightSymbolBadge(
                        symbol = "!",
                        tone = risk.tone
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
                                text = risk.title,
                                style = AeonTextStyles.CardTitle,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            AeonChip(
                                text = risk.severity.label(),
                                variant = risk.severity.variant(),
                                size = AeonChipSize.Compact
                            )
                        }

                        Text(
                            text = risk.body,
                            style = AeonTextStyles.CardSubtitle,
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
private fun InsightsFooter() {
    AeonCard(
        variant = AeonCardVariant.Glass
    ) {
        Text(
            text = "Insight is useful only when it makes the next action clearer.",
            style = AeonTextStyles.Quote,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Read patterns calmly. Act on one thing.",
            style = AeonTextStyles.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ----------------------------------------------------
// Shared UI
// ----------------------------------------------------

@Composable
private fun InsightSymbolBadge(
    symbol: String,
    tone: InsightTone
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
private fun InsightProgressBar(
    progress: Float,
    tone: InsightTone,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "insight_progress"
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
private fun InsightTone.color(): Color {
    return when (this) {
        InsightTone.Intelligence -> Color(0xFFA78BFA)
        InsightTone.Focus -> MaterialTheme.colorScheme.primary
        InsightTone.Habit -> Color(0xFF34D399)
        InsightTone.Health -> Color(0xFF10B981)
        InsightTone.Mood -> Color(0xFF60A5FA)
        InsightTone.Finance -> Color(0xFFF5C542)
        InsightTone.Learning -> Color(0xFF38BDF8)
        InsightTone.Goal -> Color(0xFF8B5CF6)
        InsightTone.Warning -> MaterialTheme.colorScheme.error
        InsightTone.Success -> Color(0xFF34D399)
        InsightTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}


private fun InsightTrend.label(): String {
    return when (this) {
        InsightTrend.Up -> "Up"
        InsightTrend.Down -> "Down"
        InsightTrend.Stable -> "Stable"
    }
}


private fun InsightTrend.variant(): AeonChipVariant {
    return when (this) {
        InsightTrend.Up -> AeonChipVariant.Success
        InsightTrend.Down -> AeonChipVariant.Warning
        InsightTrend.Stable -> AeonChipVariant.Outline
    }
}


private fun InsightSeverity.label(): String {
    return when (this) {
        InsightSeverity.Low -> "Low"
        InsightSeverity.Medium -> "Medium"
        InsightSeverity.High -> "High"
    }
}


private fun InsightSeverity.variant(): AeonChipVariant {
    return when (this) {
        InsightSeverity.Low -> AeonChipVariant.Outline
        InsightSeverity.Medium -> AeonChipVariant.Warning
        InsightSeverity.High -> AeonChipVariant.Warning
    }
}


// ----------------------------------------------------
// Dummy Data
// ----------------------------------------------------

private fun defaultInsightMetrics(): List<InsightMetricUi> {
    return listOf(
        InsightMetricUi(
            label = "Clarity",
            value = "84",
            caption = "pattern score",
            tone = InsightTone.Intelligence,
            trend = InsightTrend.Up
        ),
        InsightMetricUi(
            label = "Focus",
            value = "+18%",
            caption = "weekly change",
            tone = InsightTone.Focus,
            trend = InsightTrend.Up
        ),
        InsightMetricUi(
            label = "Health",
            value = "-6%",
            caption = "consistency drop",
            tone = InsightTone.Health,
            trend = InsightTrend.Down
        ),
        InsightMetricUi(
            label = "Mood",
            value = "Stable",
            caption = "low volatility",
            tone = InsightTone.Mood,
            trend = InsightTrend.Stable
        )
    )
}


private fun defaultInsightDomains(): List<InsightDomainUi> {
    return listOf(
        InsightDomainUi(
            id = "domain_focus",
            title = "Focus is your strongest active domain",
            subtitle = "Your best days include one protected deep-work block before late evening.",
            score = 86,
            signal = "Strong",
            tone = InsightTone.Focus
        ),
        InsightDomainUi(
            id = "domain_habits",
            title = "Habits are carrying your momentum",
            subtitle = "Small routines are keeping your life score stable even on busy days.",
            score = 82,
            signal = "Rising",
            tone = InsightTone.Habit
        ),
        InsightDomainUi(
            id = "domain_health",
            title = "Health is the constraint",
            subtitle = "Low movement and hydration can reduce focus quality tomorrow.",
            score = 64,
            signal = "Watch",
            tone = InsightTone.Health
        )
    )
}


private fun defaultRecommendations(): List<InsightRecommendationUi> {
    return listOf(
        InsightRecommendationUi(
            id = "recommend_focus_before_evening",
            title = "Do one focus block before adding new tasks",
            body = "Aeon predicts better completion if you protect a 25-minute block before expanding your task list.",
            effort = "Low",
            impact = "High",
            tone = InsightTone.Focus
        ),
        InsightRecommendationUi(
            id = "recommend_evening_walk",
            title = "Add a short evening walk reminder",
            body = "Health is the weakest signal. A simple walk can improve mood and focus recovery.",
            effort = "Low",
            impact = "Medium",
            tone = InsightTone.Health
        )
    )
}


private fun defaultPatterns(): List<InsightPatternUi> {
    return listOf(
        InsightPatternUi(
            id = "pattern_habit_focus",
            title = "Habit completion improves focus quality",
            body = "On days when habits cross 70%, focus quality also improves.",
            evidence = "5 of 7 days",
            tone = InsightTone.Habit
        ),
        InsightPatternUi(
            id = "pattern_late_overload",
            title = "Late task overload reduces clarity",
            body = "When new tasks are added at night, next-day planning becomes less stable.",
            evidence = "Repeated twice",
            tone = InsightTone.Warning
        )
    )
}


private fun defaultForecasts(): List<InsightForecastUi> {
    return listOf(
        InsightForecastUi(
            title = "Tomorrow focus potential",
            value = "78%",
            body = "Likely good if sleep and health remain stable.",
            tone = InsightTone.Focus
        ),
        InsightForecastUi(
            title = "Habit completion chance",
            value = "84%",
            body = "High chance if reminder stays gentle.",
            tone = InsightTone.Habit
        )
    )
}


private fun defaultRisks(): List<InsightRiskUi> {
    return listOf(
        InsightRiskUi(
            title = "Health drift risk",
            body = "Movement and hydration are lower than your focus demand. Recovery may become the bottleneck.",
            severity = InsightSeverity.Medium,
            tone = InsightTone.Health
        ),
        InsightRiskUi(
            title = "Task expansion risk",
            body = "Adding more tasks before closing the current priority may reduce completion rate.",
            severity = InsightSeverity.Low,
            tone = InsightTone.Warning
        )
    )
}
