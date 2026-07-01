package com.aeon.app.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aeon.app.data.local.database.entities.TaskStatusStorage
import com.aeon.app.di.aeonViewModel
import com.aeon.app.domain.task.TaskRecurrenceCodec
import com.aeon.app.presentation.viewmodel.AeonTaskViewModel
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.core.AeonHeroSectionHeader
import com.aeon.app.ui.components.core.AeonSectionHeader
import com.aeon.app.ui.components.core.AeonSectionHeaderSize
import com.aeon.app.ui.components.core.aeonBrandCardBrush
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AeonTaskDetailRoute(
    taskId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onStartFocus: () -> Unit = {}
) {
    val viewModel = aeonViewModel<AeonTaskViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val task = state.activeTasks.firstOrNull { it.id == taskId }
    val taskSubtasks = state.subtasks.filter { it.taskId == taskId }
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val colors = AeonThemeTokens.colors

    if (state.isLoading) {
        Box(
            modifier = modifier.fillMaxSize().background(colors.background).safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colors.brand)
        }
        return
    }

    if (task == null) {
        Box(
            modifier = modifier.fillMaxSize().background(colors.background).safeDrawingPadding().padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            AeonCard(variant = AeonCardVariant.Glass) {
                Text("Task not found", style = AeonTextStyles.EmptyStateTitle)
                Text(
                    "It may have been deleted or archived.",
                    style = AeonTextStyles.EmptyStateBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AeonButton("Go back", onBack)
            }
        }
        return
    }

    val isCompleted = task.status == TaskStatusStorage.Completed

    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.background).safeDrawingPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item("detail_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.background(colors.surfaceElevated, CircleShape)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (task.isRecurring) {
                        AeonChip("Repeating", variant = AeonChipVariant.Premium, size = AeonChipSize.Compact)
                    }
                    IconButton(onClick = { editing = true }, modifier = Modifier.background(colors.surfaceElevated, CircleShape)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit task", tint = colors.textPrimary)
                    }
                    IconButton(onClick = { confirmDelete = true }, modifier = Modifier.background(colors.surfaceElevated, CircleShape)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete task", tint = colors.error)
                    }
                }
            }
        }

        item("task_summary") {
            AeonHeroSectionHeader(
                title = task.title,
                eyebrow = task.projectLabel ?: task.domain.replaceFirstChar(Char::uppercase),
                subtitle = task.description?.takeIf { it.isNotBlank() }
            )
        }

        item("status_cards") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                AeonCard(variant = AeonCardVariant.Glass, modifier = Modifier.weight(1f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Priority", style = AeonTextStyles.Micro, color = colors.textSecondary)
                        Text(
                            task.priority.replaceFirstChar(Char::uppercase), 
                            style = AeonTextStyles.CardTitle, 
                            color = if (task.priority == "high" || task.priority == "critical") colors.warning else colors.textPrimary
                        )
                    }
                }
                AeonCard(variant = AeonCardVariant.Glass, modifier = Modifier.weight(1f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Status", style = AeonTextStyles.Micro, color = colors.textSecondary)
                        Text(
                            if (isCompleted) "Completed" else task.status.replaceFirstChar(Char::uppercase), 
                            style = AeonTextStyles.CardTitle, 
                            color = if (isCompleted) colors.success else colors.textPrimary
                        )
                    }
                }
            }
        }

        if (task.dueAt != null || task.reminderAt != null) {
            item("dates") {
                AeonCard(variant = AeonCardVariant.Elevated) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        task.dueAt?.let {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(modifier = Modifier.size(40.dp).background(colors.brandSoft, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Alarm, contentDescription = null, tint = colors.brand, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("Due Date", style = AeonTextStyles.Micro, color = colors.textSecondary)
                                    Text(it.toDetailDateTime(), style = AeonTextStyles.CardTitle)
                                }
                            }
                        }
                        if (task.dueAt != null && task.reminderAt != null) {
                            HorizontalDivider(color = colors.borderSoft)
                        }
                        task.reminderAt?.let {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(modifier = Modifier.size(40.dp).background(colors.surfaceGlass, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Alarm, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("Reminder", style = AeonTextStyles.Micro, color = colors.textSecondary)
                                    Text(it.toDetailDateTime(), style = AeonTextStyles.CardTitle)
                                }
                            }
                        }
                    }
                }
            }
        }

        item("actions") {
            if (isCompleted) {
                AeonCard(variant = AeonCardVariant.Glass) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).background(colors.success.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.success)
                            }
                            Text("Task Completed", style = AeonTextStyles.CardTitle, color = colors.success)
                        }
                        AeonButton("Mark pending", { viewModel.markTaskPending(task.id) }, size = AeonButtonSize.Small, variant = AeonButtonVariant.Ghost)
                    }
                }
            } else {
                AeonCard(variant = AeonCardVariant.Elevated) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Ready to focus?", style = AeonTextStyles.SectionTitle, color = colors.textPrimary)
                            Text("Start a focus session or mark this task as done.", style = AeonTextStyles.CardSubtitle, color = colors.textSecondary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AeonButton(
                                text = "Focus Now",
                                onClick = {
                                    viewModel.startFocus(task.id, task.estimatedMinutes.takeIf { it > 0 } ?: 25)
                                    onStartFocus()
                                },
                                variant = AeonButtonVariant.Premium,
                                size = AeonButtonSize.Small,
                                leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
                                modifier = Modifier.weight(1f)
                            )
                            AeonButton(
                                text = "Complete",
                                onClick = { viewModel.completeTask(task.id) },
                                variant = AeonButtonVariant.Secondary,
                                size = AeonButtonSize.Small,
                                leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (taskSubtasks.isNotEmpty()) {
            item("subtask_header") {
                AeonSectionHeader(
                    title = "Subtasks",
                    subtitle = "Progress is calculated from completed subtasks.",
                    size = AeonSectionHeaderSize.Medium
                )
            }
            items(taskSubtasks, key = { it.id }) { subtask ->
                AeonCard(variant = AeonCardVariant.Compact) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                viewModel.setSubtaskCompleted(task.id, subtask.id, !subtask.isCompleted)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                if (subtask.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = if (subtask.isCompleted) {
                                    "Mark ${subtask.title} pending"
                                } else {
                                    "Complete ${subtask.title}"
                                },
                                tint = if (subtask.isCompleted) colors.success else colors.iconSecondary
                            )
                        }
                        Text(
                            subtask.title,
                            style = AeonTextStyles.CardTitle,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (editing) {
        TaskEditorSheet(
            projects = state.projects,
            initialTask = task,
            initialSubtasks = taskSubtasks.map { it.title },
            onDismiss = { editing = false },
            onSave = { draft ->
                viewModel.updateTask(
                    task.copy(
                        title = draft.title,
                        description = draft.description,
                        priority = draft.priority,
                        domain = draft.domain,
                        projectId = draft.projectId,
                        projectLabel = draft.projectLabel,
                        dueAt = draft.dueAt,
                        reminderAt = draft.reminderAt,
                        estimatedMinutes = draft.estimatedMinutes,
                        isRecurring = draft.recurrenceRule != null,
                        recurrenceRule = draft.recurrenceRule?.let(TaskRecurrenceCodec::encode)
                    ),
                    subtaskTitles = draft.subtaskTitles
                )
                editing = false
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete task?") },
            text = { Text("This removes the task from active views while preserving safe database history.") },
            confirmButton = {
                AeonButton(
                    text = "Delete",
                    onClick = {
                        viewModel.deleteTask(task.id)
                        confirmDelete = false
                        onBack()
                    },
                    variant = AeonButtonVariant.Danger,
                    size = AeonButtonSize.Small
                )
            },
            dismissButton = {
                AeonButton(
                    text = "Cancel",
                    onClick = { confirmDelete = false },
                    variant = AeonButtonVariant.Ghost,
                    size = AeonButtonSize.Small
                )
            },
            containerColor = colors.surfaceElevated
        )
    }
}

@Composable
fun AeonStandaloneAddTaskRoute(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = aeonViewModel<AeonTaskViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Box(modifier.fillMaxSize().background(AeonThemeTokens.colors.background))
    TaskEditorSheet(
        projects = state.projects,
        onDismiss = onDismiss,
        onSave = {
            viewModel.createTask(it)
            onDismiss()
        }
    )
}

private fun Instant.toDetailDateTime(): String = atZone(ZoneId.systemDefault()).format(
    DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", Locale.getDefault())
)
