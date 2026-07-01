package com.aeon.app.ui.screens.focus

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.aeon.app.data.local.database.entities.FocusRepeatRuleStorage
import com.aeon.app.data.local.database.entities.FocusRoutineCategoryStorage
import com.aeon.app.data.local.database.entities.FocusRoutineItemEntity
import com.aeon.app.data.local.database.entities.FocusRoutineOccurrenceEntity
import com.aeon.app.data.local.database.entities.FocusRoutineTimeTypeStorage
import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.domain.focus.FocusRoutineDraft
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.core.AeonTextField
import com.aeon.app.ui.components.feedback.AeonBottomSheet
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class RoutineChoice(val value: String, val label: String)

private val timeTypeChoices = listOf(
    RoutineChoice(FocusRoutineTimeTypeStorage.ExactTime, "Exact time"),
    RoutineChoice(FocusRoutineTimeTypeStorage.TimeRange, "Time range"),
    RoutineChoice(FocusRoutineTimeTypeStorage.AnytimeToday, "Anytime today"),
    RoutineChoice(FocusRoutineTimeTypeStorage.AfterRoutine, "After another routine"),
    RoutineChoice(FocusRoutineTimeTypeStorage.BeforeRoutine, "Before a routine")
)

private val repeatChoices = listOf(
    RoutineChoice(FocusRepeatRuleStorage.Daily, "Daily"),
    RoutineChoice(FocusRepeatRuleStorage.Weekdays, "Weekdays"),
    RoutineChoice(FocusRepeatRuleStorage.Weekends, "Weekends")
)

private val categoryChoices = listOf(
    FocusRoutineCategoryStorage.Morning,
    FocusRoutineCategoryStorage.Study,
    FocusRoutineCategoryStorage.Work,
    FocusRoutineCategoryStorage.Health,
    FocusRoutineCategoryStorage.Recovery,
    FocusRoutineCategoryStorage.Reflection,
    FocusRoutineCategoryStorage.Sleep,
    FocusRoutineCategoryStorage.Personal
)

@Composable
fun FocusRoutineEditorPage(
    availableTasks: List<TaskEntity>,
    initialItem: FocusRoutineItemEntity?,
    onDismiss: () -> Unit,
    onSave: (FocusRoutineDraft) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var title by remember(initialItem?.id) { mutableStateOf(initialItem?.title.orEmpty()) }
    var description by remember(initialItem?.id) { mutableStateOf(initialItem?.description.orEmpty()) }
    var category by remember(initialItem?.id) {
        mutableStateOf(initialItem?.category ?: FocusRoutineCategoryStorage.Personal)
    }
    var timeType by remember(initialItem?.id) {
        mutableStateOf(initialItem?.timeType ?: FocusRoutineTimeTypeStorage.ExactTime)
    }
    var startMinutes by remember(initialItem?.id) { mutableStateOf(initialItem?.startTimeMinutes ?: 8 * 60) }
    var endMinutes by remember(initialItem?.id) { mutableStateOf(initialItem?.endTimeMinutes ?: 9 * 60) }
    var duration by remember(initialItem?.id) {
        mutableStateOf((initialItem?.durationMinutes ?: 30).toString())
    }
    var repeatRule by remember(initialItem?.id) {
        mutableStateOf(initialItem?.repeatRule ?: FocusRepeatRuleStorage.Daily)
    }
    var reminderBefore by remember(initialItem?.id) {
        mutableStateOf(initialItem?.reminderMinutesBefore)
    }
    var linkedTaskId by remember(initialItem?.id) { mutableStateOf(initialItem?.linkedTaskId) }
    var priority by remember(initialItem?.id) { mutableStateOf(initialItem?.priority ?: 0) }
    var error by remember { mutableStateOf<String?>(null) }

    fun pickTime(current: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            current / 60,
            current % 60,
            false
        ).show()
    }

    // Intercept back presses to dismiss the screen gracefully
    BackHandler(onBack = onDismiss)

    // Using AeonScreen for a full complete premium page experience
    AeonScreen {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = AeonSpacing.Medium, bottom = AeonSpacing.Large),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                if (initialItem == null) "New Routine" else "Edit Routine", 
                style = AeonTextStyles.SectionTitle, 
                color = MaterialTheme.colorScheme.onSurface
            )
            // Empty spacer to center title
            Spacer(modifier = Modifier.size(48.dp))
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XLarge)
        ) {
            // Identity Section
            Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)) {
                AeonTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = "Routine name",
                    placeholder = "Morning planning, Study block, Evening walk",
                    errorText = error
                )
                AeonTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description (Optional)",
                    placeholder = "What does a successful block look like?",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4
                )
            }

            // Rhythm & Timing Section
            AeonCard(variant = AeonCardVariant.Glass) {
                Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.Large)) {
                    Text("Rhythm & Timing", style = AeonTextStyles.CardTitle)
                    
                    RoutineChoiceRow("When should this happen?", timeTypeChoices, timeType) { timeType = it }

                    AnimatedVisibility(timeType == FocusRoutineTimeTypeStorage.ExactTime || timeType == FocusRoutineTimeTypeStorage.TimeRange) {
                        Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)) {
                            Text("Time window", style = AeonTextStyles.CardSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (timeType == FocusRoutineTimeTypeStorage.ExactTime) {
                                TimeChip("Starts at", startMinutes) { pickTime(startMinutes) { startMinutes = it } }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TimeChip("From", startMinutes, Modifier.weight(1f)) { pickTime(startMinutes) { startMinutes = it } }
                                    TimeChip("Until", endMinutes, Modifier.weight(1f)) { pickTime(endMinutes) { endMinutes = it } }
                                }
                            }
                        }
                    }

                    RoutineChoiceRow("Repeats", repeatChoices, repeatRule) { repeatRule = it }
                    
                    AeonTextField(
                        value = duration,
                        onValueChange = { duration = it.filter(Char::isDigit).take(3) },
                        label = "Estimated duration (minutes)",
                        placeholder = "30"
                    )
                }
            }

            // Categorization
            AeonCard(variant = AeonCardVariant.Glass) {
                Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)) {
                    Text("Categorization", style = AeonTextStyles.CardTitle)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryChoices.forEach { choice ->
                            SelectionChip(choice.replaceFirstChar(Char::uppercase), category == choice) { category = choice }
                        }
                    }
                }
            }

            // Advanced Rules & Connections
            AeonCard(variant = AeonCardVariant.Glass) {
                Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.Large)) {
                    Text("Rules & Connections", style = AeonTextStyles.CardTitle)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)) {
                        Text("Reminder", style = AeonTextStyles.CardSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(null to "Off", 0 to "At time", 5 to "5 min before", 15 to "15 min before").forEach { (value, label) ->
                                SelectionChip(label, reminderBefore == value) { reminderBefore = value }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)) {
                        Text("Priority", style = AeonTextStyles.CardSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0 to "Normal", 1 to "Important", 2 to "Critical").forEach { (value, label) ->
                                SelectionChip(label, priority == value) { priority = value }
                            }
                        }
                    }

                    if (availableTasks.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)) {
                            Text("Linked task", style = AeonTextStyles.CardSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SelectionChip("None", linkedTaskId == null) { linkedTaskId = null }
                                availableTasks.take(12).forEach { task ->
                                    AeonChip(
                                        text = task.title,
                                        selected = linkedTaskId == task.id,
                                        variant = if (linkedTaskId == task.id) AeonChipVariant.Info else AeonChipVariant.Outline,
                                        size = AeonChipSize.Compact,
                                        leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                                        onClick = { linkedTaskId = task.id }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(AeonSpacing.XXLarge))
        }

        // Bottom Fixed Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AeonSpacing.Medium, bottom = AeonSpacing.Large),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            onDelete?.let {
                AeonButton(
                    text = "Delete",
                    onClick = it,
                    variant = AeonButtonVariant.Danger,
                    size = AeonButtonSize.Large,
                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                    modifier = Modifier.weight(0.4f)
                )
            }
            AeonButton(
                text = if (initialItem == null) "Create Routine" else "Save Changes",
                onClick = {
                    error = when {
                        title.isBlank() -> "Enter a routine name."
                        timeType == FocusRoutineTimeTypeStorage.TimeRange && endMinutes <= startMinutes -> "End time must be after start time."
                        else -> null
                    }
                    if (error != null) return@AeonButton
                    onSave(
                        FocusRoutineDraft(
                            title = title.trim(),
                            description = description.trim().ifBlank { null },
                            category = category,
                            timeType = timeType,
                            startTimeMinutes = startMinutes.takeIf {
                                timeType == FocusRoutineTimeTypeStorage.ExactTime || timeType == FocusRoutineTimeTypeStorage.TimeRange
                            },
                            endTimeMinutes = endMinutes.takeIf { timeType == FocusRoutineTimeTypeStorage.TimeRange },
                            durationMinutes = duration.toIntOrNull()?.coerceIn(5, 720),
                            repeatRule = repeatRule,
                            priority = priority,
                            linkedTaskId = linkedTaskId,
                            reminderMinutesBefore = reminderBefore
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                size = AeonButtonSize.Large,
                variant = AeonButtonVariant.Premium
            )
        }
    }
}



@Composable
fun FocusCalendarSheet(
    occurrences: List<FocusRoutineOccurrenceEntity>,
    onDismiss: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()) }
    AeonBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Your week", style = AeonTextStyles.SectionTitle)
            Text(
                "A calm preview of what is planned and what is already complete.",
                style = AeonTextStyles.CardSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            occurrences.groupBy { it.date }.toSortedMap().forEach { (date, items) ->
                val completed = items.count { it.status == "done" }
                AeonCard(variant = AeonCardVariant.Compact) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(date.format(formatter), style = AeonTextStyles.CardTitle)
                        Text("$completed/${items.size} done", style = AeonTextStyles.Micro)
                    }
                    items.sortedBy { it.plannedStartAt }.take(4).forEach { item ->
                        Text("${item.calendarTime()}  ${item.title}", style = AeonTextStyles.CardSubtitle)
                    }
                }
            }
            if (occurrences.isEmpty()) {
                Text("No routine blocks are scheduled this week.", style = AeonTextStyles.CardSubtitle)
            }
        }
    }
}

@Composable
private fun RoutineChoiceRow(
    label: String,
    choices: List<RoutineChoice>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)) {
        Text(label, style = AeonTextStyles.CardSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            choices.forEach { choice ->
                SelectionChip(choice.label, selected == choice.value) { onSelected(choice.value) }
            }
        }
    }
}

@Composable
private fun SelectionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AeonChip(
        text = label,
        selected = selected,
        variant = if (selected) AeonChipVariant.Filled else AeonChipVariant.Outline,
        size = AeonChipSize.Medium,
        onClick = onClick
    )
}

@Composable
private fun TimeChip(label: String, minutes: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val hour = minutes / 60
    val minute = minutes % 60
    val text = java.time.LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    AeonChip(
        text = "$label · $text",
        modifier = modifier,
        variant = AeonChipVariant.Outline,
        leadingIcon = { Icon(Icons.Outlined.AccessTime, contentDescription = null) },
        onClick = onClick
    )
}

private fun FocusRoutineOccurrenceEntity.calendarTime(): String {
    val start = plannedStartAt ?: return "Flexible"
    return start.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
}
