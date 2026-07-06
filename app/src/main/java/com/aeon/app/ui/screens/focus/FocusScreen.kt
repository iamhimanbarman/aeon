package com.aeon.app.ui.screens.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aeon.app.data.local.database.entities.FocusRoutineOccurrenceEntity
import com.aeon.app.data.local.database.entities.FocusRoutineStatusStorage
import com.aeon.app.data.local.database.entities.FocusRoutineTimeTypeStorage
import com.aeon.app.di.aeonViewModel
import com.aeon.app.domain.focus.FocusRoutineDraft
import com.aeon.app.domain.focus.FocusRoutineScheduleRules
import com.aeon.app.domain.focus.FocusRoutineTextLimits
import com.aeon.app.presentation.viewmodel.AeonFocusViewModel
import com.aeon.app.presentation.viewmodel.FocusViewState
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.core.AeonTextField
import com.aeon.app.ui.components.core.AeonTextFieldVariant
import com.aeon.app.ui.components.core.AeonTimePickerDialog
import com.aeon.app.ui.components.feedback.AeonToastDuration
import com.aeon.app.ui.components.feedback.AeonDialog
import com.aeon.app.ui.components.feedback.LocalAeonToastHostState
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.components.layout.AeonScreenConfig
import com.aeon.app.ui.components.layout.aeonPremiumBackgroundBrush
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Contextual actions rendered by the application shell only while Focus is active.
 * This keeps the date selector at the app-bar level without coupling the shell to
 * the Focus ViewModel's navigation-scoped lifetime.
 */
data class FocusTopBarConfig(
    val selectedDate: LocalDate,
    val onDateClick: () -> Unit,
    val onDateSelected: (LocalDate) -> Unit,
    val onRecordsClick: () -> Unit,
    val onRefreshClick: () -> Unit,
    val onJumpToToday: () -> Unit
)

@Composable
fun FocusTopBarActions(config: FocusTopBarConfig) {
    var menuExpanded by remember { mutableStateOf(false) }
    var dateMenuExpanded by remember { mutableStateOf(false) }
    val colors = AeonThemeTokens.colors

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AeonChip(
                text = config.selectedDate.smartDateLabel(),
                variant = AeonChipVariant.Premium,
                size = AeonChipSize.Compact,
                onClick = { dateMenuExpanded = true },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )

            DropdownMenu(
                expanded = dateMenuExpanded,
                onDismissRequest = { dateMenuExpanded = false },
                containerColor = colors.surfaceElevated
            ) {
                DropdownMenuItem(
                    text = { Text("Today") },
                    onClick = {
                        dateMenuExpanded = false
                        config.onDateSelected(LocalDate.now())
                    }
                )
                DropdownMenuItem(
                    text = { Text("Tomorrow") },
                    onClick = {
                        dateMenuExpanded = false
                        config.onDateSelected(LocalDate.now().plusDays(1))
                    }
                )
                DropdownMenuItem(
                    text = { Text("Choose a future date") },
                    onClick = {
                        dateMenuExpanded = false
                        config.onDateClick()
                    }
                )
            }
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Focus options",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = colors.surfaceElevated
            ) {
                DropdownMenuItem(
                    text = { Text("Routine records") },
                    leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        config.onRecordsClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Refresh schedule") },
                    leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        config.onRefreshClick()
                    }
                )
                if (config.selectedDate != LocalDate.now()) {
                    DropdownMenuItem(
                        text = { Text("Back to today") },
                        leadingIcon = { Icon(Icons.Outlined.Today, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            config.onJumpToToday()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AeonFocusRoute(
    modifier: Modifier = Modifier,
    onOpenRoutineRecords: (YearMonth) -> Unit = {},
    onTopBarConfigChanged: (FocusTopBarConfig) -> Unit = {}
) {
    val viewModel = aeonViewModel<AeonFocusViewModel>()
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()

    FocusScreen(
        modifier = modifier,
        state = viewState,
        onDateSelected = viewModel::setRoutineDate,
        onRefresh = viewModel::refreshRoutines,
        onAddRoutine = viewModel::addRoutine,
        onDeleteRoutine = viewModel::deleteRoutine,
        onOpenRoutineRecords = onOpenRoutineRecords,
        onTopBarConfigChanged = onTopBarConfigChanged
    )
}

@Composable
private fun FocusScreen(
    state: FocusViewState,
    modifier: Modifier = Modifier,
    onDateSelected: (LocalDate) -> Unit,
    onRefresh: () -> Unit,
    onAddRoutine: (FocusRoutineDraft) -> Unit,
    onDeleteRoutine: (String) -> Unit,
    onOpenRoutineRecords: (YearMonth) -> Unit,
    onTopBarConfigChanged: (FocusTopBarConfig) -> Unit
) {
    val selectedDate = state.selectedDate
    val sortedOccurrences = remember(state.occurrences) {
        state.occurrences.sortedWith(
            compareBy<FocusRoutineOccurrenceEntity> { it.plannedStartAt ?: Instant.MAX }
                .thenBy { it.position }
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<FocusRoutineOccurrenceEntity?>(null) }

    val topBarConfig = remember(selectedDate) {
        FocusTopBarConfig(
            selectedDate = selectedDate,
            onDateClick = { showDatePicker = true },
            onDateSelected = onDateSelected,
            onRecordsClick = { onOpenRoutineRecords(YearMonth.from(selectedDate)) },
            onRefreshClick = onRefresh,
            onJumpToToday = { onDateSelected(LocalDate.now()) }
        )
    }
    LaunchedEffect(topBarConfig) {
        onTopBarConfigChanged(topBarConfig)
    }

    AeonScreen(
        modifier = modifier.imePadding(),
        config = AeonScreenConfig(safeDrawing = false),
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        sortedOccurrences.forEach { occurrence ->
            RoutineBar(
                occurrence = occurrence,
                onDelete = { deleteTarget = occurrence }
            )
        }

        QuickRoutineComposer(
            selectedDate = selectedDate,
            existingOccurrences = sortedOccurrences,
            onAddRoutine = onAddRoutine
        )
    }

    if (showDatePicker) {
        FutureDateCalendar(
            selectedDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                onDateSelected(date)
                showDatePicker = false
            }
        )
    }

    deleteTarget?.let { occurrence ->
        DeleteRoutineDialog(
            title = occurrence.title,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                onDeleteRoutine(occurrence.routineItemId)
                deleteTarget = null
            }
        )
    }
}

@Composable
private fun QuickRoutineComposer(
    selectedDate: LocalDate,
    existingOccurrences: List<FocusRoutineOccurrenceEntity>,
    onAddRoutine: (FocusRoutineDraft) -> Unit
) {
    val toastHostState = LocalAeonToastHostState.current
    val initialRange = remember(selectedDate) {
        defaultRoutineTimeRange(
            date = selectedDate,
            existingOccurrences = existingOccurrences
        )
    }
    var title by remember(selectedDate) { mutableStateOf("") }
    var details by remember(selectedDate) { mutableStateOf("") }
    var startMinutes by remember(selectedDate) { mutableIntStateOf(initialRange.first) }
    var endMinutes by remember(selectedDate) { mutableIntStateOf(initialRange.last) }
    var showOptions by remember(selectedDate) { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    val titleWords = title.wordCount()
    val detailWords = details.wordCount()
    val pendingDraft = remember(
        title,
        details,
        startMinutes,
        endMinutes
    ) {
        FocusRoutineDraft(
            title = FocusRoutineTextLimits.enforceTitle(title.ifBlank { "Routine" }),
            description = FocusRoutineTextLimits.enforceDetails(details),
            category = "personal",
            timeType = FocusRoutineTimeTypeStorage.ExactTime,
            startTimeMinutes = startMinutes,
            endTimeMinutes = endMinutes,
            durationMinutes = endMinutes - startMinutes,
            reminderMinutesBefore = 5
        )
    }
    val scheduleError = remember(
        pendingDraft,
        existingOccurrences
    ) {
        FocusRoutineScheduleRules.validateNewDraft(
            draft = pendingDraft,
            existingOccurrences = existingOccurrences
        )
    }
    val canAdd = title.isNotBlank() &&
        titleWords <= FocusRoutineTextLimits.TitleWords &&
        detailWords <= FocusRoutineTextLimits.DetailWords &&
        endMinutes > startMinutes &&
        scheduleError == null
    val colors = AeonThemeTokens.colors
    val composerBringIntoViewRequester = remember { BringIntoViewRequester() }
    val titleInteractionSource = remember { MutableInteractionSource() }
    val detailsInteractionSource = remember { MutableInteractionSource() }
    val titleFocused by titleInteractionSource.collectIsFocusedAsState()
    val detailsFocused by detailsInteractionSource.collectIsFocusedAsState()
    val suggestedRange = remember(selectedDate, existingOccurrences) {
        defaultRoutineTimeRange(
            date = selectedDate,
            existingOccurrences = existingOccurrences
        )
    }
    var lastScheduleErrorToast by remember(selectedDate) { mutableStateOf<String?>(null) }

    LaunchedEffect(showOptions, titleFocused, detailsFocused) {
        if (showOptions || titleFocused || detailsFocused) {
            delay(120)
            composerBringIntoViewRequester.bringIntoView()
        }
    }

    LaunchedEffect(selectedDate, existingOccurrences) {
        if (title.isBlank() && details.isBlank()) {
            startMinutes = suggestedRange.first
            endMinutes = suggestedRange.last
        }
    }

    LaunchedEffect(scheduleError, showOptions, title) {
        val activeScheduleError = scheduleError?.takeIf {
            showOptions && title.isNotBlank()
        }

        if (activeScheduleError == null) {
            lastScheduleErrorToast = null
            return@LaunchedEffect
        }

        if (activeScheduleError == lastScheduleErrorToast) {
            return@LaunchedEffect
        }

        lastScheduleErrorToast = activeScheduleError
        toastHostState.showError(
            title = activeScheduleError.toFocusScheduleToastText(),
            duration = AeonToastDuration.Short
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(composerBringIntoViewRequester),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .animateContentSize()
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AeonTextField(
                    value = title,
                    onValueChange = { title = it.limitWords(FocusRoutineTextLimits.TitleWords) },
                    modifier = Modifier.weight(1f),
                    placeholder = "Add work for ${selectedDate.smartDateLabel().lowercase(Locale.getDefault())}",
                    variant = AeonTextFieldVariant.Tonal,
                    interactionSource = titleInteractionSource,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    trailingIcon = if (title.isNotBlank()) {
                        {
                            IconButton(onClick = { showOptions = !showOptions }) {
                                Icon(
                                    imageVector = if (showOptions) {
                                        Icons.Outlined.ExpandLess
                                    } else {
                                        Icons.Outlined.ExpandMore
                                    },
                                    contentDescription = if (showOptions) {
                                        "Hide routine details"
                                    } else {
                                        "Add time and details"
                                    },
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    } else {
                        null
                    }
                )
            }

            AnimatedVisibility(visible = showOptions && title.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompactTimeField(
                            label = "From",
                            minutes = startMinutes,
                            modifier = Modifier.weight(1f),
                            onClick = { pickingStart = true }
                        )
                        CompactTimeField(
                            label = "To",
                            minutes = endMinutes,
                            modifier = Modifier.weight(1f),
                            onClick = { pickingEnd = true }
                        )
                    }

                    AeonTextField(
                        value = details,
                        onValueChange = { details = it.limitWords(FocusRoutineTextLimits.DetailWords) },
                        label = "Details",
                        placeholder = "Optional context, outcome, or location",
                        helperText = "$detailWords/${FocusRoutineTextLimits.DetailWords} words | reminder 5 min before",
                        singleLine = false,
                        minLines = 2,
                        maxLines = 3,
                        variant = AeonTextFieldVariant.Tonal,
                        interactionSource = detailsInteractionSource,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = true,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        )
                    )

                    Text(
                        text = "$titleWords/${FocusRoutineTextLimits.TitleWords} title words",
                        style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
                    )

                    if (existingOccurrences.isNotEmpty()) {
                        Text(
                            text = "Each new routine starts after the last saved block ends.",
                            style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
                        )
                    }
                }
            }
        }

        IconButton(
            enabled = canAdd,
            modifier = Modifier
                .padding(top = 4.dp)
                .size(40.dp),
            onClick = {
                if (scheduleError != null) return@IconButton
                onAddRoutine(
                    FocusRoutineDraft(
                        title = FocusRoutineTextLimits.enforceTitle(title),
                        description = FocusRoutineTextLimits.enforceDetails(details),
                        category = "personal",
                        timeType = FocusRoutineTimeTypeStorage.ExactTime,
                        startTimeMinutes = startMinutes,
                        endTimeMinutes = endMinutes,
                        durationMinutes = endMinutes - startMinutes,
                        reminderMinutesBefore = 5
                    )
                )
                val resetRange = nextSequentialRoutineTimeRange(
                    selectedDate = selectedDate,
                    latestEndMinutes = endMinutes
                )
                title = ""
                details = ""
                startMinutes = resetRange.first
                endMinutes = resetRange.last
                showOptions = false
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Save routine work",
                tint = if (canAdd) colors.brand else colors.textDisabled,
                modifier = Modifier.size(22.dp)
            )
        }
    }

    if (pickingStart) {
        SmartTimePicker(
            minutes = startMinutes,
            onDismiss = { pickingStart = false },
            onSelected = { selected ->
                val safeStart = selected.coerceAtMost(23 * 60 + 58)
                startMinutes = safeStart
                if (endMinutes <= safeStart) {
                    endMinutes = (safeStart + 60).coerceAtMost(23 * 60 + 59)
                }
                pickingStart = false
            }
        )
    }

    if (pickingEnd) {
        SmartTimePicker(
            minutes = endMinutes,
            onDismiss = { pickingEnd = false },
            onSelected = { selected ->
                endMinutes = selected
                pickingEnd = false
            }
        )
    }
}

@Composable
private fun CompactTimeField(
    label: String,
    minutes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = AeonTextStyles.Micro.copy(color = colors.textSecondary)
        )
        Text(
            text = minutes.toTimeLabel(),
            style = AeonTextStyles.Caption.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun RoutineBar(
    occurrence: FocusRoutineOccurrenceEntity,
    onDelete: () -> Unit
) {
    var expanded by remember(occurrence.id) { mutableStateOf(false) }
    val colors = AeonThemeTokens.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (expanded) colors.surfaceElevated else colors.surface.copy(alpha = 0.78f)
                )
                .clickable { expanded = !expanded }
                .animateContentSize()
                .padding(horizontal = 11.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(58.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = occurrence.startTimeLabel(),
                        style = AeonTextStyles.Micro.copy(color = colors.brand),
                        maxLines = 1
                    )

                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(14.dp)
                    )

                    Text(
                        text = occurrence.endTimeLabel(),
                        style = AeonTextStyles.Micro.copy(color = colors.textSecondary),
                        maxLines = 1
                    )
                }

                Text(
                    text = occurrence.title,
                    modifier = Modifier.weight(1f),
                    style = AeonTextStyles.CardTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = if (expanded) 3 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 68.dp, end = 4.dp, top = 3.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = occurrence.description?.takeIf(String::isNotBlank)
                            ?: "No additional details.",
                        style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
                    )
                    Text(
                        text = "${occurrence.timeRangeLabel()} | reminder five minutes before",
                        style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
                    )
                }
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Delete ${occurrence.title}",
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FutureDateCalendar(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val safeSelectedDate = remember(selectedDate, today) { selectedDate.coerceAtLeast(today) }
    var pendingDate by remember(safeSelectedDate) { mutableStateOf(safeSelectedDate) }
    var visibleMonth by remember(safeSelectedDate) { mutableStateOf(YearMonth.from(safeSelectedDate)) }
    val currentMonth = remember(today) { YearMonth.from(today) }
    val locale = remember { Locale.getDefault() }
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE, d MMM", locale) }
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val weekDays = remember(firstDayOfWeek) { orderedWeekdays(firstDayOfWeek) }
    val monthGrid = remember(visibleMonth, today, firstDayOfWeek) {
        buildFutureCalendarMonth(
            month = visibleMonth,
            minimumDate = today,
            firstDayOfWeek = firstDayOfWeek
        )
    }
    val colors = AeonThemeTokens.colors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
                shape = RoundedCornerShape(28.dp),
                color = colors.surface.copy(alpha = 0.98f),
                contentColor = colors.textPrimary,
                border = BorderStroke(1.dp, colors.brand.copy(alpha = 0.34f)),
                shadowElevation = 26.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    colors.surfaceElevated.copy(alpha = 0.96f),
                                    colors.surface.copy(alpha = 0.98f)
                                )
                            )
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Choose a focus date",
                            style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
                        )
                        Text(
                            text = pendingDate.format(dateFormatter),
                            style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surfaceElevated.copy(alpha = 0.84f),
                        border = BorderStroke(1.dp, colors.borderSoft.copy(alpha = 0.56f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                enabled = visibleMonth > currentMonth,
                                onClick = { visibleMonth = visibleMonth.minusMonths(1) }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowLeft,
                                    contentDescription = "Previous month",
                                    tint = if (visibleMonth > currentMonth) {
                                        colors.textPrimary
                                    } else {
                                        colors.textDisabled
                                    }
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = visibleMonth.atDay(1).format(monthFormatter),
                                    style = AeonTextStyles.CardTitle.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = if (visibleMonth == currentMonth) {
                                        "Current month"
                                    } else {
                                        "Future planning"
                                    },
                                    style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
                                )
                            }

                            IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowRight,
                                    contentDescription = "Next month",
                                    tint = colors.textPrimary
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            weekDays.forEach { dayOfWeek ->
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayOfWeek.compactWeekdayLabel(locale),
                                        style = AeonTextStyles.Micro.copy(
                                            color = colors.textTertiary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }

                        monthGrid.forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                week.forEach { day ->
                                    CompactCalendarDayCell(
                                        day = day,
                                        selectedDate = pendingDate,
                                        today = today,
                                        modifier = Modifier.weight(1f),
                                        onDateSelected = { date ->
                                            pendingDate = date
                                            visibleMonth = YearMonth.from(date)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AeonButton(
                            text = "Cancel",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            variant = AeonButtonVariant.Ghost,
                            size = AeonButtonSize.Small
                        )
                        AeonButton(
                            text = "Use date",
                            onClick = { onDateSelected(pendingDate) },
                            modifier = Modifier.weight(1f),
                            variant = AeonButtonVariant.Premium,
                            size = AeonButtonSize.Small
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCalendarDayCell(
    day: FocusCalendarDay,
    selectedDate: LocalDate,
    today: LocalDate,
    modifier: Modifier = Modifier,
    onDateSelected: (LocalDate) -> Unit
) {
    val colors = AeonThemeTokens.colors
    val isSelected = day.date == selectedDate
    val isToday = day.date == today
    val containerColor = when {
        isSelected -> colors.brand
        isToday -> colors.brandSoft.copy(alpha = 0.34f)
        day.inCurrentMonth -> colors.surface.copy(alpha = if (day.enabled) 0.44f else 0.24f)
        else -> Color.Transparent
    }
    val contentColor = when {
        isSelected -> Color.White
        !day.enabled -> colors.textDisabled
        day.inCurrentMonth -> colors.textPrimary
        else -> colors.textTertiary
    }

    Surface(
        onClick = { onDateSelected(day.date) },
        enabled = day.enabled,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = if (isSelected) 8.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = AeonTextStyles.ButtonMedium.copy(
                    color = contentColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun DeleteRoutineDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AeonDialog(
        onDismissRequest = onDismiss,
        title = "Delete routine work?",
        body = "\"$title\" will be removed from this routine and its upcoming reminder will be cancelled.",
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AeonButton(
                    text = "Keep",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    variant = AeonButtonVariant.Ghost,
                    size = AeonButtonSize.Medium
                )
                AeonButton(
                    text = "Delete",
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    variant = AeonButtonVariant.Danger,
                    size = AeonButtonSize.Medium
                )
            }
        }
    )
}

@Composable
private fun SmartTimePicker(
    minutes: Int,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit
) {
    AeonTimePickerDialog(
        initialHour = minutes.toClockHour12(),
        initialMinute = minutes % 60,
        initialIsAm = minutes < 12 * 60,
        onDismissRequest = onDismiss,
        onTimeSelected = { hour, minute, isAm ->
            onSelected(clockToMinutes(hour, minute, isAm))
        }
    )
}

private fun defaultRoutineTimeRange(
    date: LocalDate,
    existingOccurrences: List<FocusRoutineOccurrenceEntity> = emptyList()
): IntRange {
    return FocusRoutineScheduleRules.suggestedTimedRange(
        date = date,
        existingOccurrences = existingOccurrences,
        now = LocalTime.now()
    )
}

private fun nextSequentialRoutineTimeRange(
    selectedDate: LocalDate,
    latestEndMinutes: Int
): IntRange {
    val safeStart = latestEndMinutes.coerceAtMost(23 * 60 + 58)
    val safeEnd = (safeStart + 60).coerceAtMost(23 * 60 + 59)
    return if (selectedDate == LocalDate.now() && safeStart < defaultRoutineTimeRange(selectedDate).first) {
        defaultRoutineTimeRange(selectedDate)
    } else {
        safeStart..safeEnd.coerceAtLeast(safeStart + 1)
    }
}

private fun FocusRoutineOccurrenceEntity.startTimeLabel(): String =
    plannedStartAt?.toMinutesOfDay()?.toTimeLabel() ?: "Anytime"

private fun FocusRoutineOccurrenceEntity.endTimeLabel(): String =
    plannedEndAt?.toMinutesOfDay()?.toTimeLabel() ?: "Today"

private fun FocusRoutineOccurrenceEntity.timeRangeLabel(): String =
    "${startTimeLabel()} - ${endTimeLabel()}"

private fun Instant.toMinutesOfDay(): Int {
    val localTime = atZone(ZoneId.systemDefault()).toLocalTime()
    return localTime.hour * 60 + localTime.minute
}

private fun LocalDate.smartDateLabel(): String {
    val today = LocalDate.now()
    return when (this) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    }
}

private fun Int.toTimeLabel(): String {
    val hour24 = (this / 60).coerceIn(0, 23)
    val minute = (this % 60).coerceIn(0, 59)
    val suffix = if (hour24 < 12) "AM" else "PM"
    val hour12 = when (val raw = hour24 % 12) {
        0 -> 12
        else -> raw
    }
    return "%d:%02d %s".format(hour12, minute, suffix)
}

private fun Int.toClockHour12(): Int {
    val hour24 = (this / 60).coerceIn(0, 23)
    return (hour24 % 12).let { if (it == 0) 12 else it }
}

private fun clockToMinutes(hour: Int, minute: Int, isAm: Boolean): Int {
    val normalizedHour = when {
        isAm && hour == 12 -> 0
        !isAm && hour != 12 -> hour + 12
        else -> hour
    }
    return (normalizedHour * 60 + minute).coerceIn(0, 23 * 60 + 59)
}

internal data class FocusCalendarDay(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val enabled: Boolean
)

internal fun orderedWeekdays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> =
    List(7) { offset -> firstDayOfWeek.plus(offset.toLong()) }

internal fun buildFutureCalendarMonth(
    month: YearMonth,
    minimumDate: LocalDate,
    firstDayOfWeek: DayOfWeek
): List<List<FocusCalendarDay>> {
    val firstOfMonth = month.atDay(1)
    val leadingDays = ((firstOfMonth.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
    val gridStart = firstOfMonth.minusDays(leadingDays.toLong())

    return List(6) { weekIndex ->
        List(7) { dayIndex ->
            val date = gridStart.plusDays((weekIndex * 7L) + dayIndex.toLong())
            FocusCalendarDay(
                date = date,
                inCurrentMonth = YearMonth.from(date) == month,
                enabled = !date.isBefore(minimumDate)
            )
        }
    }
}

private fun DayOfWeek.compactWeekdayLabel(locale: Locale): String =
    getDisplayName(TextStyle.SHORT, locale)
        .replace(".", "")
        .replace(" ", "")
        .take(2)
        .uppercase(locale)

private fun String.wordCount(): Int = trim()
    .split(Regex("\\s+"))
    .count(String::isNotBlank)

private fun String.limitWords(maxWords: Int): String {
    val matches = Regex("\\S+").findAll(this).toList()
    if (matches.size <= maxWords) return this
    return substring(0, matches[maxWords - 1].range.last + 1)
}

private fun String.recordStatusLabel(): String = when (this) {
    FocusRoutineStatusStorage.Done -> "Done"
    FocusRoutineStatusStorage.Missed -> "Missed"
    FocusRoutineStatusStorage.Current -> "Current"
    FocusRoutineStatusStorage.Snoozed -> "Snoozed"
    FocusRoutineStatusStorage.Skipped -> "Skipped"
    else -> "Open"
}

private fun String.toFocusScheduleToastText(): String = when {
    contains("End time must be after start time", ignoreCase = true) -> {
        "End after start"
    }
    contains("start after the previous one ends", ignoreCase = true) -> {
        "Pick a later start"
    }
    contains("overlaps with", ignoreCase = true) -> {
        "Time overlaps"
    }
    else -> "Fix routine time"
}
