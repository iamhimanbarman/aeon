package com.aeon.app.ui.screens.tasks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.data.local.database.entities.TaskPriorityStorage
import com.aeon.app.data.local.database.entities.TaskProjectEntity
import com.aeon.app.data.local.database.entities.TaskRiskStorage
import com.aeon.app.data.local.database.entities.TaskStatusStorage
import com.aeon.app.di.aeonViewModel
import com.aeon.app.domain.task.TaskIntelligenceEngine
import com.aeon.app.presentation.viewmodel.AeonTaskViewModel
import com.aeon.app.presentation.viewmodel.TaskViewState
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
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun AeonTaskRoute(
    modifier: Modifier = Modifier,
    onOpenTask: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onStartFocus: () -> Unit = {}
) {
    val viewModel = aeonViewModel<AeonTaskViewModel>()
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = remember(viewState) { viewState.toTaskUiState() }
    var showEditor by rememberSaveable { mutableStateOf(false) }

    TaskScreen(
        state = state,
        onAddTask = { showEditor = true },
        onOpenTask = onOpenTask,
        onCompleteTask = viewModel::completeTask,
        onMarkPending = viewModel::markTaskPending,
        onSnoozeTask = { viewModel.snoozeTask(it, Instant.now().plus(Duration.ofHours(1))) },
        onStartFocus = { task ->
            viewModel.startFocus(task.id, task.estimatedMinutes.takeIf { it > 0 } ?: 25)
            onStartFocus()
        },
        onOpenNotifications = onOpenNotifications,
        modifier = modifier
    )

    if (showEditor) {
        TaskEditorSheet(
            projects = viewState.projects,
            onDismiss = { showEditor = false },
            onSave = { draft ->
                viewModel.createTask(draft)
                showEditor = false
            }
        )
    }
}

@Immutable
data class TaskAnalyticsUi(
    val pressureScore: Int,
    val pressureLabel: String,
    val momentumScore: Int,
    val dailyLoadScore: Int,
    val primaryInsight: String,
    val pendingCount: Int
)

@Immutable
data class TaskUiState(
    val isLoading: Boolean,
    val error: String?,
    val dateLabel: String,
    val analytics: TaskAnalyticsUi,
    val focusTask: TaskItemUi?,
    val allTasks: List<TaskItemUi>,
    val overdueTasks: List<TaskItemUi>,
    val dueTodayTasks: List<TaskItemUi>,
    val upcomingTasks: List<TaskItemUi>,
    val completedTodayTasks: List<TaskItemUi>
)

@Immutable
data class TaskItemUi(
    val entity: TaskEntity,
    val title: String,
    val description: String,
    val projectLabel: String,
    val dueLabel: String,
    val score: Int,
    val riskLevel: String,
    val progress: Float,
    val subtaskSummary: String?,
    val completed: Boolean
) {
    val id: String get() = entity.id
}

enum class TaskFilter(val label: String) {
    Today("Today"),
    Priority("Priority"),
    Pending("Pending"),
    Done("Done")
}

@Composable
fun TaskScreen(
    state: TaskUiState,
    onAddTask: () -> Unit,
    onOpenTask: (String) -> Unit,
    onCompleteTask: (String) -> Unit,
    onMarkPending: (String) -> Unit,
    onSnoozeTask: (String) -> Unit,
    onStartFocus: (TaskEntity) -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by rememberSaveable { mutableStateOf(TaskFilter.Today) }
    val filteredTasks = remember(filter, state) {
        when (filter) {
            TaskFilter.Today -> (state.overdueTasks + state.dueTodayTasks + state.upcomingTasks)
                .distinctBy(TaskItemUi::id)
            TaskFilter.Priority -> state.allTasks.filter { !it.completed && it.score >= 55 }
            TaskFilter.Pending -> state.allTasks.filter { !it.completed }
            TaskFilter.Done -> state.allTasks.filter(TaskItemUi::completed)
        }
    }
    val colors = AeonThemeTokens.colors

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            TaskHeader(state.dateLabel, onOpenNotifications)
        }

        if (state.isLoading) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.brand)
                }
            }
        } else if (state.error != null) {
            item(key = "error") {
                TaskEmptyState(
                    title = "Tasks could not be loaded",
                    body = state.error
                )
            }
        } else {
            item(key = "analytics") {
                TaskAnalyticsBoard(state.analytics)
            }

            item(key = "quick_add") {
                TaskQuickAddBar(onAddTask)
            }

            item(key = "focus") {
                TaskFocusCard(
                    task = state.focusTask,
                    onOpenTask = onOpenTask,
                    onCompleteTask = onCompleteTask,
                    onSnoozeTask = onSnoozeTask,
                    onStartFocus = onStartFocus
                )
            }

            item(key = "filters") {
                TaskFilterChips(filter) { filter = it }
            }

            if (filteredTasks.isEmpty()) {
                item(key = "empty_${filter.name}") {
                    TaskEmptyState(
                        title = if (filter == TaskFilter.Done) "Nothing completed yet" else "No pressure right now",
                        body = if (filter == TaskFilter.Done) {
                            "Completed tasks will appear here in your daily record."
                        } else {
                            "Add a task when something needs attention."
                        }
                    )
                }
            } else {
                items(
                    items = filteredTasks,
                    key = TaskItemUi::id,
                    contentType = { "task" }
                ) { task ->
                    TaskItemCard(
                        task = task,
                        onOpenTask = onOpenTask,
                        onCompleteTask = onCompleteTask,
                        onMarkPending = onMarkPending,
                        onSnoozeTask = onSnoozeTask,
                        modifier = Modifier.animateItem()
                    )
                }
            }

        }
    }
}

@Composable
private fun TaskHeader(dateLabel: String, onOpenNotifications: () -> Unit) {
    AeonSectionHeader(
        title = "Tasks",
        size = AeonSectionHeaderSize.Hero,
        tone = AeonSectionHeaderTone.Brand,
        action = {
            AeonChip(
                text = "Reminders",
                variant = AeonChipVariant.Outline,
                size = AeonChipSize.Compact,
                leadingIcon = { Icon(Icons.Outlined.Alarm, contentDescription = null) },
                onClick = onOpenNotifications
            )
        }
    )
}

@Composable
private fun TaskAnalyticsBoard(analytics: TaskAnalyticsUi) {
    AeonCard(variant = AeonCardVariant.Elevated) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header / Insight
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(36.dp).background(AeonThemeTokens.colors.ai.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = AeonThemeTokens.colors.ai, modifier = Modifier.size(20.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Aeon Intelligence", style = AeonTextStyles.CardSubtitle, color = AeonThemeTokens.colors.ai)
                    Text(analytics.primaryInsight, style = AeonTextStyles.CardTitle, color = AeonThemeTokens.colors.textPrimary)
                }
            }
            
            androidx.compose.material3.HorizontalDivider(color = AeonThemeTokens.colors.borderSoft)
            
            // Metrics Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AnalyticsMetric("Pressure", "${analytics.pressureScore}%", pressureColor(analytics.pressureScore))
                AnalyticsMetric("Momentum", "${analytics.momentumScore}%", AeonThemeTokens.colors.success)
                AnalyticsMetric("Pending", "${analytics.pendingCount}", AeonThemeTokens.colors.textPrimary)
            }
        }
    }
}

@Composable
private fun AnalyticsMetric(label: String, value: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = AeonTextStyles.SectionTitle, color = color)
        Text(label, style = AeonTextStyles.Micro, color = AeonThemeTokens.colors.textSecondary)
    }
}

@Composable
private fun TaskQuickAddBar(onAddTask: () -> Unit) {
    val hints = remember {
        val hour = LocalTime.now().hour
        when (hour) {
            in 5..11 -> listOf(
                "What's the main focus today?",
                "Capture a morning thought...",
                "Start the day right...",
                "What needs to be done?"
            )
            in 12..16 -> listOf(
                "Crush the afternoon...",
                "Keep the momentum going...",
                "Any quick wins?",
                "What needs to be done?"
            )
            in 17..21 -> listOf(
                "Plan for tomorrow...",
                "Wrap up the day...",
                "Empty your mind...",
                "What needs to be done?"
            )
            else -> listOf(
                "Late night thoughts?",
                "Set yourself up for tomorrow...",
                "Any midnight ideas?",
                "What needs to be done?"
            )
        }
    }
    var currentHintIndex by remember { mutableStateOf(0) }

    LaunchedEffect(hints) {
        while (true) {
            delay(5000L)
            currentHintIndex = (currentHintIndex + 1) % hints.size
        }
    }

    AeonCard(
        variant = AeonCardVariant.Elevated,
        onClick = onAddTask,
        modifier = Modifier.semantics {
            role = Role.Button
            contentDescription = "Add a new task"
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = AeonThemeTokens.colors.brand)
            AnimatedContent(
                targetState = hints[currentHintIndex],
                transitionSpec = {
                    (slideInVertically(animationSpec = tween(600)) { height -> height } + fadeIn(animationSpec = tween(600)))
                        .togetherWith(slideOutVertically(animationSpec = tween(600)) { height -> -height } + fadeOut(animationSpec = tween(600)))
                },
                modifier = Modifier.weight(1f),
                label = "hint_animation"
            ) { hint ->
                Text(
                    text = hint,
                    style = AeonTextStyles.CardTitle,
                    color = AeonThemeTokens.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun TaskFocusCard(
    task: TaskItemUi?,
    onOpenTask: (String) -> Unit,
    onCompleteTask: (String) -> Unit,
    onSnoozeTask: (String) -> Unit,
    onStartFocus: (TaskEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AeonSectionHeader(
            title = "Focus now",
            size = AeonSectionHeaderSize.Medium
        )
        if (task == null) {
            TaskEmptyState(
                title = "No pressure right now",
                body = "Add a task when something needs attention."
            )
            return@Column
        }
        AeonCard(variant = AeonCardVariant.Elevated, onClick = { onOpenTask(task.id) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        task.title,
                        style = AeonTextStyles.CardTitle,
                        color = AeonThemeTokens.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TaskPriorityBadge(task.entity.priority)
                        AeonChip(task.dueLabel, variant = AeonChipVariant.Outline, size = AeonChipSize.Compact)
                    }
                }
                AeonButton(
                    text = "Focus",
                    onClick = { onStartFocus(task.entity) },
                    variant = AeonButtonVariant.Premium,
                    size = AeonButtonSize.Small,
                    leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
private fun TaskFilterChips(selected: TaskFilter, onSelect: (TaskFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskFilter.entries.forEach { filter ->
            AeonChip(
                text = filter.label,
                selected = filter == selected,
                variant = if (filter == selected) AeonChipVariant.Filled else AeonChipVariant.Outline,
                size = AeonChipSize.Compact,
                onClick = { onSelect(filter) }
            )
        }
    }
}

@Composable
private fun TaskItemCard(
    task: TaskItemUi,
    onOpenTask: (String) -> Unit,
    onCompleteTask: (String) -> Unit,
    onMarkPending: (String) -> Unit,
    onSnoozeTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AeonCard(
        modifier = modifier,
        variant = if (task.score >= 65 && !task.completed) AeonCardVariant.Elevated else AeonCardVariant.Glass,
        onClick = { onOpenTask(task.id) }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    task.title,
                    style = AeonTextStyles.CardTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (task.completed) AeonThemeTokens.colors.textSecondary else AeonThemeTokens.colors.textPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!task.completed) TaskPriorityBadge(task.entity.priority)
                    AeonChip(task.dueLabel, variant = AeonChipVariant.Outline, size = AeonChipSize.Compact)
                }
            }
            IconButton(
                onClick = { if (task.completed) onMarkPending(task.id) else onCompleteTask(task.id) },
                modifier = Modifier.size(32.dp).semantics {
                    contentDescription = if (task.completed) "Mark ${task.title} pending" else "Complete ${task.title}"
                }
            ) {
                Icon(
                    if (task.completed) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (task.completed) AeonThemeTokens.colors.success else AeonThemeTokens.colors.iconSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun TaskEmptyState(title: String, body: String) {
    AeonCard(variant = AeonCardVariant.Glass) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = AeonThemeTokens.colors.brand)
            Column(Modifier.weight(1f)) {
                Text(title, style = AeonTextStyles.EmptyStateTitle)
                Text(body, style = AeonTextStyles.EmptyStateBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TaskProgressBar(progress: Float) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(7.dp),
        color = AeonThemeTokens.colors.brand,
        trackColor = AeonThemeTokens.colors.brandSoft
    )
}

@Composable
private fun TaskPriorityBadge(priority: String) {
    AeonChip(
        text = priority.replaceFirstChar(Char::uppercase),
        variant = when (priority) {
            TaskPriorityStorage.Critical, TaskPriorityStorage.High -> AeonChipVariant.Warning
            TaskPriorityStorage.Medium -> AeonChipVariant.Info
            else -> AeonChipVariant.Outline
        },
        size = AeonChipSize.Compact
    )
}

@Composable
private fun TaskRiskBadge(risk: String) {
    AeonChip(
        text = risk.replaceFirstChar(Char::uppercase),
        variant = when (risk) {
            TaskRiskStorage.Critical -> AeonChipVariant.Danger
            TaskRiskStorage.High -> AeonChipVariant.Warning
            TaskRiskStorage.Medium -> AeonChipVariant.Info
            else -> AeonChipVariant.Outline
        },
        size = AeonChipSize.Compact
    )
}

@Composable
private fun pressureColor(score: Int): Color = when {
    score >= 85 -> MaterialTheme.colorScheme.error
    score >= 65 -> AeonThemeTokens.colors.warning
    score >= 40 -> AeonThemeTokens.colors.focus
    else -> AeonThemeTokens.colors.success
}

private fun dueVariant(label: String): AeonChipVariant = when {
    label.startsWith("Overdue") -> AeonChipVariant.Danger
    label.startsWith("Due today") -> AeonChipVariant.Warning
    else -> AeonChipVariant.Info
}

private fun TaskViewState.toTaskUiState(
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): TaskUiState {
    val today = now.atZone(zoneId).toLocalDate()
    val projectsById = projects.associateBy(TaskProjectEntity::id)
    val subtasksByTask = subtasks.groupBy { it.taskId }
    val items = activeTasks.map { task ->
        val taskSubtasks = subtasksByTask[task.id].orEmpty()
        val completedCount = taskSubtasks.count { it.isCompleted }
        val calculated = TaskIntelligenceEngine.evaluate(task, now, zoneId)
        TaskItemUi(
            entity = task,
            title = task.title,
            description = task.description.orEmpty(),
            projectLabel = projectsById[task.projectId]?.name
                ?: task.projectLabel
                ?: task.domain.replaceFirstChar(Char::uppercase),
            dueLabel = task.dueAt.toDueLabel(task.completedAt, now, zoneId),
            score = calculated.score,
            riskLevel = calculated.riskLevel,
            progress = if (taskSubtasks.isEmpty()) task.progress else completedCount.toFloat() / taskSubtasks.size,
            subtaskSummary = taskSubtasks.takeIf { it.isNotEmpty() }
                ?.let { "$completedCount of ${it.size} subtasks complete" },
            completed = task.status == TaskStatusStorage.Completed
        )
    }.sortedWith(compareByDescending<TaskItemUi> { !it.completed }.thenByDescending { it.score })

    val open = items.filterNot(TaskItemUi::completed)
    val overdue = open.filter { task ->
        task.entity.dueAt?.atZone(zoneId)?.toLocalDate()?.isBefore(today) == true
    }
    val dueToday = open.filter { it.entity.dueAt?.atZone(zoneId)?.toLocalDate() == today }
    val upcoming = open.filter { task ->
        val date = task.entity.dueAt?.atZone(zoneId)?.toLocalDate()
        date == null || date.isAfter(today)
    }
    val completedToday = items.filter { task ->
        task.entity.completedAt?.atZone(zoneId)?.toLocalDate() == today
    }
    val focus = open
        .filterNot { it.entity.snoozedUntil?.isAfter(now) == true }
        .maxByOrNull(TaskItemUi::score)
    val criticalOverdue = overdue.count { it.entity.priority == TaskPriorityStorage.Critical || it.entity.priority == TaskPriorityStorage.High }
    
    val basePressure = if (open.isEmpty()) 0 else {
        val topTasksAverage = open.sortedByDescending { it.score }.take(5).map { it.score }.average().coerceAtLeast(0.0)
        val overduePenalty = (overdue.size * 5) + (criticalOverdue * 10)
        val todayPenalty = dueToday.size * 3
        (topTasksAverage + overduePenalty + todayPenalty).toInt()
    }
    val finalPressure = basePressure.coerceIn(0, 100)
    
    val pressureLabel = when {
        finalPressure >= 85 -> "Critical Load"
        finalPressure >= 65 -> "High Pressure"
        finalPressure >= 40 -> "Manageable"
        finalPressure > 0 -> "Light Day"
        else -> "All Clear"
    }

    val todayTotal = dueToday.size + completedToday.size
    val momentumScore = if (todayTotal == 0) {
        if (completedToday.isNotEmpty()) 100 else 0
    } else {
        ((completedToday.size.toFloat() / todayTotal) * 100).toInt().coerceIn(0, 100)
    }

    val primaryInsight = when {
        criticalOverdue > 0 -> "High priority tasks are overdue. Tackle them immediately."
        overdue.isNotEmpty() -> "You have ${overdue.size} overdue tasks accumulating. Clear the backlog."
        finalPressure >= 85 -> "Your daily load is peaking. Recommend snoozing low priority tasks."
        momentumScore >= 80 && todayTotal > 0 -> "Incredible momentum today. You're almost done."
        dueToday.size >= 5 -> "Busy day ahead. Try focusing on one task at a time."
        dueToday.isEmpty() && open.isNotEmpty() -> "No deadlines today. Great time to get ahead."
        open.isEmpty() -> "Zero active tasks. Enjoy your free time."
        else -> "Steady pace. Keep chipping away at your tasks."
    }

    val analytics = TaskAnalyticsUi(
        pressureScore = finalPressure,
        pressureLabel = pressureLabel,
        momentumScore = momentumScore,
        dailyLoadScore = (dueToday.size * 10).coerceIn(0, 100),
        primaryInsight = primaryInsight,
        pendingCount = dueToday.size + overdue.size
    )

    return TaskUiState(
        isLoading = isLoading,
        error = error,
        dateLabel = today.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())),
        analytics = analytics,
        focusTask = focus,
        allTasks = items,
        overdueTasks = overdue,
        dueTodayTasks = dueToday,
        upcomingTasks = upcoming,
        completedTodayTasks = completedToday
    )
}

private fun Instant?.toDueLabel(
    completedAt: Instant?,
    now: Instant,
    zoneId: ZoneId
): String {
    if (completedAt != null) {
        return if (completedAt.atZone(zoneId).toLocalDate() == now.atZone(zoneId).toLocalDate()) {
            "Completed today"
        } else {
            "Completed"
        }
    }
    val due = this ?: return "No deadline"
    val dueDateTime = due.atZone(zoneId)
    val today = now.atZone(zoneId).toLocalDate()
    val date = dueDateTime.toLocalDate()
    val time = dueDateTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    return when {
        date.isBefore(today) -> {
            val days = java.time.temporal.ChronoUnit.DAYS.between(date, today)
            "Overdue by $days ${if (days == 1L) "day" else "days"}"
        }
        date == today -> "Due today · $time"
        date == today.plusDays(1) -> "Due tomorrow · $time"
        else -> "Due ${date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))}"
    }
}
