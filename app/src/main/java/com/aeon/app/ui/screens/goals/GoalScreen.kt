package com.aeon.app.ui.screens.goals

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
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aeon.app.data.local.database.entities.GoalDomainStorage
import com.aeon.app.data.local.database.entities.GoalEntity
import com.aeon.app.data.local.database.entities.GoalMilestoneEntity
import com.aeon.app.data.local.database.entities.GoalMilestoneStatusStorage
import com.aeon.app.data.local.database.entities.GoalPriorityStorage
import com.aeon.app.data.local.database.entities.GoalStatusStorage
import com.aeon.app.di.aeonViewModel
import com.aeon.app.presentation.viewmodel.AeonGoalViewModel
import com.aeon.app.presentation.viewmodel.GoalViewState
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ----------------------------------------------------
// Route
// ----------------------------------------------------

@Composable
fun AeonGoalRoute(
    onAddGoal: () -> Unit = {},
    onOpenGoal: (String) -> Unit = {},
    onOpenMilestone: (String) -> Unit = {},
    onMarkMilestoneDone: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = aeonViewModel<AeonGoalViewModel>()
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()

    val uiState = remember(viewState) {
        viewState.toGoalUiState()
    }

    GoalScreen(
        state = uiState,
        onAddGoal = onAddGoal,
        onOpenGoal = onOpenGoal,
        onOpenMilestone = onOpenMilestone,
        onMarkMilestoneDone = { id ->
            viewModel.completeMilestone(id)
            onMarkMilestoneDone(id)
        },
        onOpenNotifications = onOpenNotifications,
        modifier = modifier
    )
}

// ----------------------------------------------------
// State Models
// ----------------------------------------------------

@Immutable
data class GoalUiState(
    val dateLabel: String,
    val goalScore: Int,
    val goalLabel: String,
    val goalMessage: String,
    val activeGoal: GoalItemUi?,
    val metrics: List<GoalMetricUi>,
    val goals: List<GoalItemUi>,
    val milestones: List<GoalMilestoneUi>,
    val roadmap: List<GoalRoadmapItemUi>,
    val domains: List<GoalDomainUi>,
    val insight: GoalInsightUi
)

@Immutable
data class GoalMetricUi(
    val label: String,
    val value: String,
    val caption: String,
    val tone: GoalTone
)

@Immutable
data class GoalItemUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val domain: String,
    val progress: Float,
    val dueLabel: String,
    val status: GoalStatus,
    val priority: GoalPriority,
    val tone: GoalTone
)

@Immutable
data class GoalMilestoneUi(
    val id: String,
    val goalId: String,
    val title: String,
    val subtitle: String,
    val dueLabel: String,
    val status: GoalMilestoneStatus,
    val tone: GoalTone
)

@Immutable
data class GoalRoadmapItemUi(
    val title: String,
    val subtitle: String,
    val timeLabel: String,
    val state: GoalRoadmapState,
    val tone: GoalTone
)

@Immutable
data class GoalDomainUi(
    val title: String,
    val subtitle: String,
    val activeGoals: Int,
    val progress: Float,
    val tone: GoalTone
)

@Immutable
data class GoalInsightUi(
    val title: String,
    val body: String,
    val confidence: Int
)

enum class GoalFilter { All, Active, Priority, Completed, AtRisk }
enum class GoalStatus { Active, Paused, Completed, AtRisk }
enum class GoalPriority { Low, Medium, High, LifeChanging }
enum class GoalMilestoneStatus { Pending, Active, Done, Blocked }
enum class GoalRoadmapState { Done, Current, Next, Later }
enum class GoalTone { Goal, Build, Study, Health, Finance, Career, Personal, AI, Warning, Success, Neutral }

// ----------------------------------------------------
// Screen
// ----------------------------------------------------

@Composable
fun GoalScreen(
    state: GoalUiState,
    onAddGoal: () -> Unit,
    onOpenGoal: (String) -> Unit,
    onOpenMilestone: (String) -> Unit,
    onMarkMilestoneDone: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by rememberSaveable { mutableStateOf(GoalFilter.All) }

    val filteredGoals = remember(selectedFilter, state.goals) {
        state.goals.filterBy(selectedFilter)
    }

    AeonScreen(modifier = modifier) {
        Spacer(modifier = Modifier.height(AeonSpacing.Medium))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)) {
                Text(state.dateLabel.uppercase(), style = AeonTextStyles.Micro, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Goals", style = AeonTextStyles.HeroMetric, color = MaterialTheme.colorScheme.onSurface)
            }
            AeonButton("Reminders", onClick = onOpenNotifications, variant = AeonButtonVariant.Ghost, size = AeonButtonSize.Small)
        }
        
        Spacer(modifier = Modifier.height(AeonSpacing.Large))

        GoalClarityHero(state = state, onAddGoal = onAddGoal)

        Spacer(modifier = Modifier.height(AeonSpacing.XXLarge))

        if (state.activeGoal != null || state.metrics.isNotEmpty()) {
            AeonSectionHeader(
                eyebrow = "Trajectory", 
                title = "Current Focus", 
                subtitle = "The primary direction you are moving towards.", 
                size = AeonSectionHeaderSize.Medium
            )
            
            if (state.metrics.isNotEmpty()) {
                GoalMetricPulseGrid(metrics = state.metrics)
                Spacer(modifier = Modifier.height(AeonSpacing.Medium))
            }
            
            state.activeGoal?.let {
                ActiveGoalCard(goal = it, onOpenGoal = onOpenGoal)
            }
            Spacer(modifier = Modifier.height(AeonSpacing.XXLarge))
        }

        if (state.milestones.isNotEmpty()) {
            AeonSectionHeader(
                eyebrow = "Execution", 
                title = "Next Milestones", 
                subtitle = "Immediate actionable steps to keep momentum.", 
                size = AeonSectionHeaderSize.Medium
            )
            Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)) {
                state.milestones.take(4).forEach { milestone ->
                    GoalMilestoneCard(
                        milestone = milestone, 
                        onOpenMilestone = onOpenMilestone, 
                        onMarkMilestoneDone = onMarkMilestoneDone
                    )
                }
            }
            Spacer(modifier = Modifier.height(AeonSpacing.XXLarge))
        }

        AeonSectionHeader(
            eyebrow = "Portfolio", 
            title = "All Directions", 
            subtitle = "Manage, filter, and track all your ongoing goals.", 
            size = AeonSectionHeaderSize.Medium
        )
        GoalFilterRow(selectedFilter = selectedFilter, onSelectFilter = { selectedFilter = it })
        Spacer(modifier = Modifier.height(AeonSpacing.Medium))
        
        if (filteredGoals.isEmpty()) {
            AeonCard(variant = AeonCardVariant.Glass) {
                Text("No goals in this view.", style = AeonTextStyles.EmptyStateTitle, color = MaterialTheme.colorScheme.onSurface)
                Text("Change filter or create a goal when you are ready.", style = AeonTextStyles.EmptyStateBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)) {
                filteredGoals.forEach { goal -> GoalItemCard(goal = goal, onOpenGoal = onOpenGoal) }
            }
        }

        Spacer(modifier = Modifier.height(AeonSpacing.XXLarge))

        GoalInsightCard(insight = state.insight)

        Spacer(modifier = Modifier.height(AeonSpacing.XLarge))
        GoalFooter()
        Spacer(modifier = Modifier.height(AeonSpacing.Large))
    }
}

// ----------------------------------------------------
// Components
// ----------------------------------------------------

@Composable
private fun GoalClarityHero(state: GoalUiState, onAddGoal: () -> Unit) {
    AeonCard(variant = AeonCardVariant.Hero) {
        Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.Large)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Large),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GoalScoreRing(score = state.goalScore, label = "Clarity")
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
                ) {
                    Text(state.goalLabel, style = AeonTextStyles.CardTitle, color = MaterialTheme.colorScheme.onSurface)
                    Text(state.goalMessage, style = AeonTextStyles.CardSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AeonChip("Progress evaluated", variant = AeonChipVariant.Premium, size = AeonChipSize.Compact)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            AeonButton("+ New Direction", onClick = onAddGoal, variant = AeonButtonVariant.Primary, size = AeonButtonSize.Medium, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun GoalScoreRing(score: Int, label: String) {
    val progress by animateFloatAsState(targetValue = score.coerceIn(0, 100) / 100f, label = "goal_score_progress")
    val ringColor = GoalTone.Goal.color()
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    Box(modifier = Modifier.size(118.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(110.dp)) {
            drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color = ringColor, startAngle = -90f, sweepAngle = progress * 360f, useCenter = false, style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score.toString(), style = AeonTextStyles.LifeScoreNumber, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = AeonTextStyles.Micro, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalMetricPulseGrid(metrics: List<GoalMetricUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)) {
        metrics.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small)) {
                row.forEach { metric ->
                    AeonCard(modifier = Modifier.weight(1f), variant = AeonCardVariant.Glass) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween, 
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)) {
                                Text(metric.label, style = AeonTextStyles.CardSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(metric.caption, style = AeonTextStyles.Micro, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                            Text(metric.value, style = AeonTextStyles.StatNumber, color = metric.tone.color())
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActiveGoalCard(goal: GoalItemUi, onOpenGoal: (String) -> Unit) {
    AeonCard(variant = AeonCardVariant.Elevated, onClick = { onOpenGoal(goal.id) }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium), verticalAlignment = Alignment.CenterVertically) {
            GoalMiniRing(progress = goal.progress, tone = goal.tone)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)) {
                Text(goal.title, style = AeonTextStyles.CardTitle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(goal.subtitle, style = AeonTextStyles.CardSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall), verticalAlignment = Alignment.CenterVertically) {
                    AeonChip(goal.priority.label(), variant = goal.priority.variant(), size = AeonChipSize.Compact)
                    AeonChip(goal.dueLabel, variant = AeonChipVariant.Info, size = AeonChipSize.Compact)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalFilterRow(selectedFilter: GoalFilter, onSelectFilter: (GoalFilter) -> Unit) {
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small), verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)) {
        GoalFilter.values().forEach { filter ->
            AeonChip(
                text = filter.label(), 
                variant = if (filter == selectedFilter) AeonChipVariant.Filled else AeonChipVariant.Ghost, 
                size = AeonChipSize.Medium, 
                onClick = { onSelectFilter(filter) }
            )
        }
    }
}

@Composable
private fun GoalItemCard(goal: GoalItemUi, onOpenGoal: (String) -> Unit) {
    AeonCard(variant = if (goal.status == GoalStatus.AtRisk) AeonCardVariant.Insight else AeonCardVariant.Default, onClick = { onOpenGoal(goal.id) }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium), verticalAlignment = Alignment.CenterVertically) {
            GoalMiniRing(progress = goal.progress, tone = goal.status.tone(goal.tone))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)) {
                        Text(goal.title, style = AeonTextStyles.CardTitle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(goal.subtitle, style = AeonTextStyles.CardSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall), verticalAlignment = Alignment.CenterVertically) {
                    AeonChip(goal.status.label(), variant = goal.status.variant(), size = AeonChipSize.Compact)
                    AeonChip(goal.domain, variant = AeonChipVariant.Ghost, size = AeonChipSize.Compact)
                    AeonChip(goal.priority.label(), variant = goal.priority.variant(), size = AeonChipSize.Compact)
                    AeonChip(goal.dueLabel, variant = AeonChipVariant.Info, size = AeonChipSize.Compact)
                }
            }
        }
    }
}

@Composable
private fun GoalMilestoneCard(milestone: GoalMilestoneUi, onOpenMilestone: (String) -> Unit, onMarkMilestoneDone: (String) -> Unit) {
    AeonCard(variant = if (milestone.status == GoalMilestoneStatus.Active) AeonCardVariant.Elevated else AeonCardVariant.Default, onClick = { onOpenMilestone(milestone.id) }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium), verticalAlignment = Alignment.CenterVertically) {
            GoalSymbolBadge(symbol = milestone.status.symbol(), tone = milestone.status.tone(milestone.tone))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)) {
                Text(milestone.title, style = AeonTextStyles.CardTitle, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall), verticalAlignment = Alignment.CenterVertically) {
                    Text(milestone.dueLabel, style = AeonTextStyles.Micro, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AeonChip(milestone.status.label(), variant = milestone.status.variant(), size = AeonChipSize.Compact)
                }
            }
            AnimatedVisibility(visible = milestone.status != GoalMilestoneStatus.Done) {
                AeonButton("Done", onClick = { onMarkMilestoneDone(milestone.id) }, variant = AeonButtonVariant.Tonal, size = AeonButtonSize.Small)
            }
        }
    }
}

@Composable
private fun GoalInsightCard(insight: GoalInsightUi) {
    AeonSectionHeader(eyebrow = "Aeon Intelligence", title = "Goal Insight", subtitle = "A calm recommendation to convert direction into action.", size = AeonSectionHeaderSize.Medium, tone = AeonSectionHeaderTone.Premium)
    AeonCard(variant = AeonCardVariant.Insight) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium), verticalAlignment = Alignment.Top) {
            GoalSymbolBadge(symbol = "✦", tone = GoalTone.AI)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)) {
                Text(insight.title, style = AeonTextStyles.InsightTitle, color = MaterialTheme.colorScheme.onSurface)
                Text(insight.body, style = AeonTextStyles.InsightBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AeonChip("${insight.confidence}% confidence", variant = AeonChipVariant.Premium, size = AeonChipSize.Compact)
            }
        }
    }
}

@Composable
private fun GoalFooter() {
    AeonCard(variant = AeonCardVariant.Glass) {
        Text("A goal without a next action becomes imagination. A small step makes it real.", style = AeonTextStyles.Quote, color = MaterialTheme.colorScheme.onSurface)
        Text("Choose direction. Reduce noise. Move one milestone.", style = AeonTextStyles.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GoalSymbolBadge(symbol: String, tone: GoalTone) {
    Box(
        modifier = Modifier.size(42.dp).clip(CircleShape).background(tone.color().copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, style = AeonTextStyles.CardTitle, color = tone.color())
    }
}

@Composable
private fun GoalMiniRing(progress: Float, tone: GoalTone) {
    val animated by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "goal_mini_ring")
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val toneColor = tone.color()

    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(44.dp)) {
            drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color = toneColor, startAngle = -90f, sweepAngle = animated * 360f, useCenter = false, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
        }
        Text("${(animated * 100).toInt()}", style = AeonTextStyles.Micro, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ----------------------------------------------------
// Formatting & Colors
// ----------------------------------------------------

@Composable
private fun GoalTone.color(): Color = when (this) {
    GoalTone.Goal -> Color(0xFF8B5CF6)
    GoalTone.Build -> MaterialTheme.colorScheme.primary
    GoalTone.Study -> Color(0xFF38BDF8)
    GoalTone.Health -> Color(0xFF10B981)
    GoalTone.Finance -> Color(0xFFF5C542)
    GoalTone.Career -> Color(0xFFA78BFA)
    GoalTone.Personal -> Color(0xFF2DD4BF)
    GoalTone.AI -> Color(0xFFA78BFA)
    GoalTone.Warning -> MaterialTheme.colorScheme.error
    GoalTone.Success -> Color(0xFF34D399)
    GoalTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun GoalTone.symbol(): String = when (this) {
    GoalTone.Goal -> "◆"
    GoalTone.Build -> "▣"
    GoalTone.Study -> "◇"
    GoalTone.Health -> "+"
    GoalTone.Finance -> "₹"
    GoalTone.Career -> "↗"
    GoalTone.Personal -> "◌"
    GoalTone.AI -> "✦"
    GoalTone.Warning -> "!"
    GoalTone.Success -> "✓"
    GoalTone.Neutral -> "○"
}

private fun GoalFilter.label(): String = when (this) {
    GoalFilter.All -> "All"
    GoalFilter.Active -> "Active"
    GoalFilter.Priority -> "Priority"
    GoalFilter.Completed -> "Done"
    GoalFilter.AtRisk -> "At risk"
}

private fun List<GoalItemUi>.filterBy(filter: GoalFilter): List<GoalItemUi> = when (filter) {
    GoalFilter.All -> this
    GoalFilter.Active -> filter { it.status == GoalStatus.Active }
    GoalFilter.Priority -> filter { it.priority == GoalPriority.High || it.priority == GoalPriority.LifeChanging }
    GoalFilter.Completed -> filter { it.status == GoalStatus.Completed }
    GoalFilter.AtRisk -> filter { it.status == GoalStatus.AtRisk }
}

private fun GoalStatus.label(): String = when (this) {
    GoalStatus.Active -> "Active"
    GoalStatus.Paused -> "Paused"
    GoalStatus.Completed -> "Done"
    GoalStatus.AtRisk -> "At risk"
}

private fun GoalStatus.variant(): AeonChipVariant = when (this) {
    GoalStatus.Active -> AeonChipVariant.Info
    GoalStatus.Paused -> AeonChipVariant.Outline
    GoalStatus.Completed -> AeonChipVariant.Success
    GoalStatus.AtRisk -> AeonChipVariant.Warning
}

private fun GoalStatus.tone(fallback: GoalTone): GoalTone = when (this) {
    GoalStatus.Active -> fallback
    GoalStatus.Paused -> GoalTone.Neutral
    GoalStatus.Completed -> GoalTone.Success
    GoalStatus.AtRisk -> GoalTone.Warning
}

private fun GoalPriority.label(): String = when (this) {
    GoalPriority.Low -> "Low"
    GoalPriority.Medium -> "Medium"
    GoalPriority.High -> "High"
    GoalPriority.LifeChanging -> "Life-changing"
}

private fun GoalPriority.variant(): AeonChipVariant = when (this) {
    GoalPriority.Low -> AeonChipVariant.Outline
    GoalPriority.Medium -> AeonChipVariant.Info
    GoalPriority.High -> AeonChipVariant.Warning
    GoalPriority.LifeChanging -> AeonChipVariant.Premium
}

private fun GoalMilestoneStatus.symbol(): String = when (this) {
    GoalMilestoneStatus.Pending -> "○"
    GoalMilestoneStatus.Active -> "●"
    GoalMilestoneStatus.Done -> "✓"
    GoalMilestoneStatus.Blocked -> "!"
}

private fun GoalMilestoneStatus.label(): String = when (this) {
    GoalMilestoneStatus.Pending -> "Pending"
    GoalMilestoneStatus.Active -> "Active"
    GoalMilestoneStatus.Done -> "Done"
    GoalMilestoneStatus.Blocked -> "Blocked"
}

private fun GoalMilestoneStatus.variant(): AeonChipVariant = when (this) {
    GoalMilestoneStatus.Pending -> AeonChipVariant.Outline
    GoalMilestoneStatus.Active -> AeonChipVariant.Info
    GoalMilestoneStatus.Done -> AeonChipVariant.Success
    GoalMilestoneStatus.Blocked -> AeonChipVariant.Warning
}

private fun GoalMilestoneStatus.tone(fallback: GoalTone): GoalTone = when (this) {
    GoalMilestoneStatus.Pending -> fallback
    GoalMilestoneStatus.Active -> fallback
    GoalMilestoneStatus.Done -> GoalTone.Success
    GoalMilestoneStatus.Blocked -> GoalTone.Warning
}

private fun GoalRoadmapState.symbol(): String = when (this) {
    GoalRoadmapState.Done -> "✓"
    GoalRoadmapState.Current -> "●"
    GoalRoadmapState.Next -> "○"
    GoalRoadmapState.Later -> "◌"
}

private fun GoalRoadmapState.label(): String = when (this) {
    GoalRoadmapState.Done -> "Done"
    GoalRoadmapState.Current -> "Now"
    GoalRoadmapState.Next -> "Next"
    GoalRoadmapState.Later -> "Later"
}

private fun GoalRoadmapState.variant(): AeonChipVariant = when (this) {
    GoalRoadmapState.Done -> AeonChipVariant.Success
    GoalRoadmapState.Current -> AeonChipVariant.Info
    GoalRoadmapState.Next -> AeonChipVariant.Outline
    GoalRoadmapState.Later -> AeonChipVariant.Outline
}

private fun GoalRoadmapState.tone(fallback: GoalTone): GoalTone = when (this) {
    GoalRoadmapState.Done -> GoalTone.Success
    GoalRoadmapState.Current -> fallback
    GoalRoadmapState.Next -> fallback
    GoalRoadmapState.Later -> GoalTone.Neutral
}

// ----------------------------------------------------
// UI State Mapper
// ----------------------------------------------------

private fun GoalViewState.toGoalUiState(): GoalUiState {
    val zoneId = ZoneId.systemDefault()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val dateLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))

    val allGoals = goals.filter { it.deletedAt == null }
    val mappedGoals = allGoals.map { it.toUiModel(dateFormatter, zoneId) }
    
    val activeList = mappedGoals.filter { it.status == GoalStatus.Active }
    val completedList = mappedGoals.filter { it.status == GoalStatus.Completed }
    val atRiskList = mappedGoals.filter { it.status == GoalStatus.AtRisk }

    val activeCount = activeList.size
    val totalProgress = if (allGoals.isNotEmpty()) allGoals.map { it.progress }.average().toFloat() * 100 else 0f
    
    val milestonesCompleted = upcomingMilestones.count { it.status == GoalMilestoneStatusStorage.Done }
    val totalMilestones = upcomingMilestones.size

    val activeGoal = mappedGoals.maxByOrNull { it.priority.ordinal } ?: activeList.firstOrNull()

    val clarityScore = when {
        allGoals.isEmpty() -> 0
        activeCount == 0 -> 20
        atRiskList.isNotEmpty() -> 50
        totalProgress > 75 -> 95
        totalProgress > 50 -> 80
        else -> 65
    }

    val clarityLabel = when {
        clarityScore >= 80 -> "On track"
        clarityScore >= 50 -> "Needs attention"
        else -> "At risk"
    }

    val clarityMessage = when {
        allGoals.isEmpty() -> "You have no goals set. Start by defining one long-term direction."
        atRiskList.isNotEmpty() -> "Some of your goals are at risk. Focus on recovery before expanding."
        activeGoal != null -> "Your main focus is ${activeGoal.title}. Protect time for it today."
        else -> "Your goals are defined, but none are actively moving right now."
    }

    val mappedMetrics = listOf(
        GoalMetricUi("Active Focus", activeCount.toString(), "goals in motion", GoalTone.Goal),
        GoalMetricUi("Completion", "${totalProgress.toInt()}%", "overall progress", GoalTone.Build),
        GoalMetricUi("Execution", "$milestonesCompleted/$totalMilestones", "milestones done", GoalTone.Success),
        GoalMetricUi("Attention", atRiskList.size.toString(), "goals at risk", if (atRiskList.isEmpty()) GoalTone.Success else GoalTone.Warning)
    )

    val mappedMilestones = upcomingMilestones.map { it.toUiModel(dateFormatter, zoneId, mappedGoals) }

    val roadmap = generateRoadmap(mappedMilestones, mappedGoals)
    val domains = generateDomains(mappedGoals)

    val insight = GoalInsightUi(
        title = if (atRiskList.isNotEmpty()) "Goals at risk" else "Maintain momentum",
        body = if (atRiskList.isNotEmpty()) "You have ${atRiskList.size} goal(s) that need immediate attention to prevent stagnation." else "Your goal distribution looks balanced. Keep executing on the active milestones.",
        confidence = if (atRiskList.isNotEmpty()) 95 else 85
    )

    return GoalUiState(
        dateLabel = dateLabel,
        goalScore = clarityScore,
        goalLabel = clarityLabel,
        goalMessage = clarityMessage,
        activeGoal = activeGoal,
        metrics = mappedMetrics,
        goals = mappedGoals,
        milestones = mappedMilestones,
        roadmap = roadmap,
        domains = domains,
        insight = insight
    )
}

private fun GoalEntity.toUiModel(formatter: DateTimeFormatter, zoneId: ZoneId): GoalItemUi {
    val statusEnum = when (status) {
        GoalStatusStorage.Active -> GoalStatus.Active
        GoalStatusStorage.Paused -> GoalStatus.Paused
        GoalStatusStorage.Completed -> GoalStatus.Completed
        GoalStatusStorage.AtRisk -> GoalStatus.AtRisk
        else -> GoalStatus.Active
    }

    val priorityEnum = when (priority) {
        GoalPriorityStorage.Low -> GoalPriority.Low
        GoalPriorityStorage.Medium -> GoalPriority.Medium
        GoalPriorityStorage.High -> GoalPriority.High
        GoalPriorityStorage.LifeChanging -> GoalPriority.LifeChanging
        else -> GoalPriority.Medium
    }

    val toneEnum = when (domain) {
        GoalDomainStorage.Build -> GoalTone.Build
        GoalDomainStorage.Study -> GoalTone.Study
        GoalDomainStorage.Health -> GoalTone.Health
        GoalDomainStorage.Finance -> GoalTone.Finance
        GoalDomainStorage.Career -> GoalTone.Career
        GoalDomainStorage.Personal -> GoalTone.Personal
        else -> GoalTone.Goal
    }

    val dueStr = dueAt?.atZone(zoneId)?.toLocalDate()?.format(formatter) ?: "Ongoing"

    return GoalItemUi(
        id = id,
        title = title,
        subtitle = description ?: "No description provided.",
        domain = domain.replaceFirstChar { it.uppercase() },
        progress = progress,
        dueLabel = dueStr,
        status = statusEnum,
        priority = priorityEnum,
        tone = toneEnum
    )
}

private fun GoalMilestoneEntity.toUiModel(formatter: DateTimeFormatter, zoneId: ZoneId, goals: List<GoalItemUi>): GoalMilestoneUi {
    val statusEnum = when (status) {
        GoalMilestoneStatusStorage.Pending -> GoalMilestoneStatus.Pending
        GoalMilestoneStatusStorage.Active -> GoalMilestoneStatus.Active
        GoalMilestoneStatusStorage.Done -> GoalMilestoneStatus.Done
        GoalMilestoneStatusStorage.Blocked -> GoalMilestoneStatus.Blocked
        else -> GoalMilestoneStatus.Pending
    }

    val parentGoal = goals.find { it.id == goalId }
    val dueStr = dueAt?.atZone(zoneId)?.toLocalDate()?.format(formatter) ?: "No date"

    return GoalMilestoneUi(
        id = id,
        goalId = goalId,
        title = title,
        subtitle = description ?: parentGoal?.title ?: "No description",
        dueLabel = dueStr,
        status = statusEnum,
        tone = parentGoal?.tone ?: GoalTone.Goal
    )
}

private fun generateRoadmap(milestones: List<GoalMilestoneUi>, goals: List<GoalItemUi>): List<GoalRoadmapItemUi> {
    if (milestones.isEmpty()) return emptyList()

    return milestones.sortedBy { it.status.ordinal }.take(4).map { milestone ->
        val state = when (milestone.status) {
            GoalMilestoneStatus.Done -> GoalRoadmapState.Done
            GoalMilestoneStatus.Active -> GoalRoadmapState.Current
            GoalMilestoneStatus.Pending -> GoalRoadmapState.Next
            GoalMilestoneStatus.Blocked -> GoalRoadmapState.Later
        }
        GoalRoadmapItemUi(
            title = milestone.title,
            subtitle = milestone.subtitle,
            timeLabel = milestone.dueLabel,
            state = state,
            tone = milestone.tone
        )
    }
}

private fun generateDomains(goals: List<GoalItemUi>): List<GoalDomainUi> {
    val domainGroups = goals.groupBy { it.domain }
    return domainGroups.map { (domainName, domainGoals) ->
        val active = domainGoals.count { it.status == GoalStatus.Active }
        val avgProgress = domainGoals.map { it.progress }.average().toFloat()
        val tone = domainGoals.firstOrNull()?.tone ?: GoalTone.Goal
        
        GoalDomainUi(
            title = domainName,
            subtitle = "${domainGoals.size} total goals",
            activeGoals = active,
            progress = avgProgress,
            tone = tone
        )
    }
}
