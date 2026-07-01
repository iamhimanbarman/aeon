package com.aeon.app.ui.screens.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.data.local.database.entities.TaskPriorityStorage
import com.aeon.app.data.local.database.entities.TaskProjectEntity
import com.aeon.app.domain.task.QuickTaskParser
import com.aeon.app.domain.task.TaskDraft
import com.aeon.app.domain.task.TaskRecurrenceCodec
import com.aeon.app.domain.task.TaskRecurrenceFrequency
import com.aeon.app.domain.task.TaskRecurrenceRule
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.core.AeonTextField
import com.aeon.app.ui.components.feedback.AeonBottomSheet
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class ReminderChoice(val label: String) {
    None("None"),
    AtDueTime("At due time"),
    OneHourBefore("1 hour before")
}

@Composable
fun TaskEditorSheet(
    projects: List<TaskProjectEntity>,
    onDismiss: () -> Unit,
    onSave: (TaskDraft) -> Unit,
    initialTask: TaskEntity? = null,
    initialSubtasks: List<String> = emptyList()
) {
    val context = LocalContext.current
    val zoneId = remember { ZoneId.systemDefault() }
    var title by remember(initialTask?.id) { mutableStateOf(initialTask?.title.orEmpty()) }
    var description by remember(initialTask?.id) { mutableStateOf(initialTask?.description.orEmpty()) }
    var priority by remember(initialTask?.id) {
        mutableStateOf(initialTask?.priority ?: TaskPriorityStorage.Medium)
    }
    var projectId by remember(initialTask?.id, projects) {
        mutableStateOf(initialTask?.projectId)
    }
    var dueAt by remember(initialTask?.id) { mutableStateOf(initialTask?.dueAt) }
    var reminderChoice by remember(initialTask?.id) {
        mutableStateOf(
            when {
                initialTask?.reminderAt == null -> ReminderChoice.None
                initialTask.reminderAt == initialTask.dueAt -> ReminderChoice.AtDueTime
                else -> ReminderChoice.OneHourBefore
            }
        )
    }
    var estimatedMinutes by remember(initialTask?.id) {
        mutableStateOf(initialTask?.estimatedMinutes?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var recurrence by remember(initialTask?.id) {
        mutableStateOf(TaskRecurrenceCodec.decode(initialTask?.recurrenceRule))
    }
    var advanced by remember(initialTask?.id) {
        mutableStateOf(
            initialTask != null || description.isNotBlank() || initialSubtasks.isNotEmpty() || recurrence != null
        )
    }
    var subtaskInput by remember { mutableStateOf("") }
    var subtasks by remember(initialTask?.id) { mutableStateOf(initialSubtasks) }
    var titleError by remember { mutableStateOf<String?>(null) }
    val parsed = remember(title) { QuickTaskParser.parse(title) }
    val selectedProject = projects.firstOrNull { it.id == projectId }

    fun chooseDate() {
        val initial = dueAt?.atZone(zoneId) ?: ZonedDateTime.now(zoneId).plusDays(1).withHour(18).withMinute(0)
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val time = dueAt?.atZone(zoneId)?.toLocalTime() ?: LocalTime.of(18, 0)
                dueAt = LocalDate.of(year, month + 1, day).atTime(time).atZone(zoneId).toInstant()
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).show()
    }

    fun chooseTime() {
        val initial = dueAt?.atZone(zoneId) ?: ZonedDateTime.now(zoneId).plusDays(1).withHour(18).withMinute(0)
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val date = dueAt?.atZone(zoneId)?.toLocalDate() ?: LocalDate.now(zoneId).plusDays(1)
                dueAt = date.atTime(hour, minute).atZone(zoneId).toInstant()
            },
            initial.hour,
            initial.minute,
            false
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AeonThemeTokens.colors.background)
            .safeDrawingPadding()
    ) {
        val colors = AeonThemeTokens.colors
        
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.IconButton(
                onClick = onDismiss,
                modifier = Modifier.background(colors.surfaceElevated, androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Cancel", tint = colors.textPrimary)
            }
            Text(
                text = if (initialTask == null) "New Task" else "Edit Task",
                style = AeonTextStyles.SectionTitle,
                color = colors.textPrimary
            )
            AeonButton(
                text = "Save",
                onClick = {
                    if (title.isBlank()) {
                        titleError = "A clear task title is required."
                        return@AeonButton
                    }
                    val resolvedDue = dueAt ?: parsed.dueAt
                    val reminderAt = when (reminderChoice) {
                        ReminderChoice.None -> null
                        ReminderChoice.AtDueTime -> resolvedDue
                        ReminderChoice.OneHourBefore -> resolvedDue?.minusSeconds(60 * 60)
                    }
                    val inferredProject = selectedProject
                        ?: projects.firstOrNull { it.name.equals(parsed.domain, ignoreCase = true) }
                        ?: projects.firstOrNull { it.isDefault }
                    onSave(
                        TaskDraft(
                            title = if (dueAt == null && parsed.dueAt != null) parsed.title else title.trim(),
                            description = description.trim().ifBlank { null },
                            priority = priority,
                            domain = if (initialTask != null && parsed.domain == "general") initialTask.domain else parsed.domain,
                            projectId = inferredProject?.id,
                            projectLabel = inferredProject?.name,
                            dueAt = resolvedDue,
                            reminderAt = reminderAt,
                            estimatedMinutes = estimatedMinutes.toIntOrNull()?.coerceIn(0, 999) ?: 0,
                            subtaskTitles = subtasks,
                            recurrenceRule = recurrence
                        )
                    )
                },
                size = AeonButtonSize.Small,
                variant = AeonButtonVariant.Premium
            )
        }

        // Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Hero Title Input
            com.aeon.app.ui.components.core.AeonCard(variant = com.aeon.app.ui.components.core.AeonCardVariant.Glass) {
                AeonTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = null
                    },
                    label = "What do you want to achieve?",
                    placeholder = "e.g., Submit assignment tomorrow at 10 AM",
                    helperText = parsed.dueAt?.let { "Aeon detected ${parsed.title} · ${it.toEditorDateTime(zoneId)}" },
                    errorText = titleError,
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4
                )
            }

            // Scheduling Card
            com.aeon.app.ui.components.core.AeonCard(variant = com.aeon.app.ui.components.core.AeonCardVariant.Elevated) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = colors.brand)
                            Text("Scheduling", style = AeonTextStyles.CardTitle)
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            DueChoiceChip("None", dueAt == null) { dueAt = null }
                            DueChoiceChip("Today", dueAt.isOnDate(LocalDate.now(zoneId), zoneId)) {
                                dueAt = LocalDate.now(zoneId).atTime(18, 0).atZone(zoneId).toInstant()
                            }
                            DueChoiceChip("Tomorrow", dueAt.isOnDate(LocalDate.now(zoneId).plusDays(1), zoneId)) {
                                dueAt = LocalDate.now(zoneId).plusDays(1).atTime(18, 0).atZone(zoneId).toInstant()
                            }
                            AeonChip(
                                text = dueAt?.toEditorDateTime(zoneId) ?: "Choose date",
                                variant = AeonChipVariant.Outline,
                                size = AeonChipSize.Compact,
                                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                                onClick = ::chooseDate
                            )
                            if (dueAt != null) {
                                AeonChip(
                                    text = dueAt?.atZone(zoneId)?.format(
                                        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
                                    ).orEmpty(),
                                    variant = AeonChipVariant.Outline,
                                    size = AeonChipSize.Compact,
                                    leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                                    onClick = ::chooseTime
                                )
                            }
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(color = colors.borderSoft)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Reminder", style = AeonTextStyles.CardSubtitle, color = colors.textSecondary)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            ReminderChoice.entries.forEach { choice ->
                                AeonChip(
                                    text = choice.label,
                                    selected = choice == reminderChoice,
                                    variant = if (choice == reminderChoice) AeonChipVariant.Filled else AeonChipVariant.Outline,
                                    size = AeonChipSize.Compact,
                                    enabled = choice == ReminderChoice.None || dueAt != null || parsed.dueAt != null,
                                    onClick = { reminderChoice = choice }
                                )
                            }
                        }
                    }
                }
            }

            // Priority Card
            com.aeon.app.ui.components.core.AeonCard(variant = com.aeon.app.ui.components.core.AeonCardVariant.Elevated) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Priority", style = AeonTextStyles.CardTitle)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf(
                            TaskPriorityStorage.Low,
                            TaskPriorityStorage.Medium,
                            TaskPriorityStorage.High,
                            TaskPriorityStorage.Critical
                        ).forEach { value ->
                            AeonChip(
                                text = value.replaceFirstChar(Char::uppercase),
                                selected = priority == value,
                                variant = if (priority == value) {
                                    if (value == TaskPriorityStorage.Critical || value == TaskPriorityStorage.High) AeonChipVariant.Warning
                                    else AeonChipVariant.Info
                                } else AeonChipVariant.Outline,
                                size = AeonChipSize.Compact,
                                onClick = { priority = value }
                            )
                        }
                    }
                }
            }

            AeonButton(
                text = if (advanced) "Hide details" else "Add details (Project, Subtasks...)",
                onClick = { advanced = !advanced },
                variant = AeonButtonVariant.Tonal,
                size = AeonButtonSize.Medium,
                fullWidth = true,
                leadingIcon = {
                    Icon(
                        if (advanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null
                    )
                }
            )

            AnimatedVisibility(advanced) {
                com.aeon.app.ui.components.core.AeonCard(variant = com.aeon.app.ui.components.core.AeonCardVariant.Glass) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        
                        AeonTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = "Description",
                            placeholder = "Context, notes, or the desired outcome",
                            singleLine = false,
                            minLines = 3,
                            maxLines = 5
                        )

                        if (projects.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Project", style = AeonTextStyles.CardTitle)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    projects.forEach { project ->
                                        AeonChip(
                                            text = project.name,
                                            selected = project.id == projectId,
                                            variant = if (project.id == projectId) AeonChipVariant.Filled else AeonChipVariant.Outline,
                                            size = AeonChipSize.Compact,
                                            onClick = { projectId = project.id }
                                        )
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                AeonTextField(
                                    value = estimatedMinutes,
                                    onValueChange = { estimatedMinutes = it.filter(Char::isDigit).take(3) },
                                    label = "Estimated minutes",
                                    placeholder = "25",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Repeat", style = AeonTextStyles.CardTitle)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                AeonChip(
                                    "None",
                                    selected = recurrence == null,
                                    variant = if (recurrence == null) AeonChipVariant.Filled else AeonChipVariant.Outline,
                                    size = AeonChipSize.Compact,
                                    onClick = { recurrence = null }
                                )
                                TaskRecurrenceFrequency.entries.forEach { frequency ->
                                    AeonChip(
                                        frequency.name,
                                        selected = recurrence?.frequency == frequency,
                                        variant = if (recurrence?.frequency == frequency) AeonChipVariant.Filled else AeonChipVariant.Outline,
                                        size = AeonChipSize.Compact,
                                        onClick = { recurrence = TaskRecurrenceRule(frequency) }
                                    )
                                }
                            }
                        }

                        androidx.compose.material3.HorizontalDivider(color = colors.borderSoft)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Subtasks", style = AeonTextStyles.CardTitle)
                            subtasks.forEachIndexed { index, subtask ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${index + 1}. $subtask", style = AeonTextStyles.CardSubtitle, modifier = Modifier.weight(1f))
                                    AeonChip(
                                        "Remove",
                                        variant = AeonChipVariant.Ghost,
                                        size = AeonChipSize.Compact,
                                        onClick = { subtasks = subtasks.toMutableList().also { it.removeAt(index) } }
                                    )
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    AeonTextField(
                                        value = subtaskInput,
                                        onValueChange = { subtaskInput = it },
                                        placeholder = "Add a subtask",
                                        singleLine = true
                                    )
                                }
                                AeonButton(
                                    text = "Add",
                                    onClick = {
                                        val value = subtaskInput.trim()
                                        if (value.isNotBlank()) {
                                            subtasks = subtasks + value
                                            subtaskInput = ""
                                        }
                                    },
                                    enabled = subtaskInput.isNotBlank(),
                                    size = AeonButtonSize.Medium,
                                    variant = AeonButtonVariant.Secondary,
                                    leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DueChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AeonChip(
        text = label,
        selected = selected,
        variant = if (selected) AeonChipVariant.Filled else AeonChipVariant.Outline,
        size = AeonChipSize.Compact,
        onClick = onClick
    )
}

private fun Instant?.isOnDate(date: LocalDate, zoneId: ZoneId): Boolean =
    this?.atZone(zoneId)?.toLocalDate() == date

private fun Instant.toEditorDateTime(zoneId: ZoneId): String =
    atZone(zoneId).format(DateTimeFormatter.ofPattern("d MMM · h:mm a", Locale.getDefault()))
