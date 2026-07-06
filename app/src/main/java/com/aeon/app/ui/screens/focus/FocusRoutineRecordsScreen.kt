package com.aeon.app.ui.screens.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aeon.app.data.local.database.entities.FocusRoutineOccurrenceEntity
import com.aeon.app.data.local.database.entities.FocusRoutineStatusStorage
import com.aeon.app.di.aeonViewModel
import com.aeon.app.presentation.viewmodel.AeonFocusViewModel
import com.aeon.app.presentation.viewmodel.FocusViewState
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.feedback.AeonNoDataState
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.components.layout.AeonScreenConfig
import com.aeon.app.ui.components.layout.aeonPremiumBackgroundBrush
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AeonFocusRoutineRecordsRoute(
    monthKey: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = aeonViewModel<AeonFocusViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val initialMonth = remember(monthKey) { monthKey.toFocusYearMonth() }

    BackHandler(onBack = onBack)

    LaunchedEffect(initialMonth) {
        if (YearMonth.from(state.selectedDate) != initialMonth) {
            viewModel.viewRoutineDate(initialMonth.defaultRecordsDate())
        }
    }

    FocusRoutineRecordsScreen(
        state = state,
        modifier = modifier,
        onDateSelected = viewModel::viewRoutineDate
    )
}

@Composable
private fun FocusRoutineRecordsScreen(
    state: FocusViewState,
    modifier: Modifier = Modifier,
    onDateSelected: (LocalDate) -> Unit
) {
    val selectedDate = state.selectedDate
    val selectedMonth = remember(selectedDate) { YearMonth.from(selectedDate) }
    val monthDates = remember(selectedMonth) {
        (1..selectedMonth.lengthOfMonth()).map(selectedMonth::atDay)
    }
    val monthlyRecordsByDate = remember(state.monthlyOccurrences) {
        state.monthlyOccurrences
            .sortedWith(
                compareBy<FocusRoutineOccurrenceEntity> { it.date }
                    .thenBy { it.plannedStartAt ?: Instant.MAX }
                    .thenBy { it.position }
            )
            .groupBy(FocusRoutineOccurrenceEntity::date)
    }
    val selectedDateRecords = remember(selectedDate, monthlyRecordsByDate) {
        monthlyRecordsByDate[selectedDate].orEmpty()
    }
    val dateRailState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedDate.dayOfMonth - 1).coerceAtLeast(0)
    )
    var showMonthPicker by remember { mutableStateOf(false) }
    val colors = AeonThemeTokens.colors

    LaunchedEffect(selectedDate, selectedMonth) {
        dateRailState.animateScrollToItem((selectedDate.dayOfMonth - 1).coerceAtLeast(0))
    }

    AeonScreen(
        modifier = modifier,
        config = AeonScreenConfig(
            safeDrawing = true,
            scrollable = false
        ),
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Routine records",
                    modifier = Modifier.weight(1f),
                    style = AeonTextStyles.SectionTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                AeonChip(
                    text = selectedMonth.toFocusMonthLabel(),
                    variant = AeonChipVariant.Premium,
                    size = AeonChipSize.Compact,
                    onClick = { showMonthPicker = true },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyColumn(
                    state = dateRailState,
                    modifier = Modifier
                        .width(54.dp)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = monthDates,
                        key = { date -> date.toEpochDay() }
                    ) { date ->
                        FocusRecordsDateRailItem(
                            date = date,
                            isSelected = date == selectedDate,
                            onClick = { onDateSelected(date) }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FocusRecordsDayHeader(
                        date = selectedDate,
                        routines = selectedDateRecords
                    )

                    if (selectedDateRecords.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AeonNoDataState(
                                title = if (state.monthlyOccurrences.isEmpty()) {
                                    "No routines saved in ${selectedMonth.month.displayName()} yet"
                                } else {
                                    "No routines on ${selectedDate.recordsHeaderLabel()}"
                                },
                                message = if (state.monthlyOccurrences.isEmpty()) {
                                    "Create focus blocks from the Focus page and they will appear here month by month."
                                } else {
                                    "Choose another date from the left rail or add a new block for this day."
                                }
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = selectedDateRecords,
                                key = { record -> record.id }
                            ) { record ->
                                FocusRoutineRecordBar(record = record)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMonthPicker) {
        FocusRoutineMonthPickerDialog(
            selectedMonth = selectedMonth,
            onDismiss = { showMonthPicker = false },
            onSelected = { month ->
                val nextDay = selectedDate.dayOfMonth.coerceAtMost(month.lengthOfMonth())
                onDateSelected(month.atDay(nextDay))
                showMonthPicker = false
            }
        )
    }
}

@Composable
private fun FocusRecordsDateRailItem(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val primaryColor = if (isSelected) colors.brand else colors.textSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = AeonTextStyles.ButtonMedium.copy(
                color = primaryColor,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun FocusRecordsDayHeader(
    date: LocalDate,
    routines: List<FocusRoutineOccurrenceEntity>
) {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date.recordsHeaderLabel(),
            modifier = Modifier.weight(1f),
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        )

        if (routines.isNotEmpty()) {
            AeonChip(
                text = "${routines.size} blocks",
                variant = AeonChipVariant.Outline,
                size = AeonChipSize.Compact
            )
        }
    }
}

@Composable
private fun FocusRoutineRecordBar(
    record: FocusRoutineOccurrenceEntity
) {
    var expanded by remember(record.id) { mutableStateOf(false) }
    val colors = AeonThemeTokens.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.78f))
            .clickable { expanded = !expanded }
            .animateContentSize(
                animationSpec = tween(durationMillis = 140)
            )
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = record.timeRangeLabel(),
                style = AeonTextStyles.Micro.copy(
                    color = colors.brand,
                    fontWeight = FontWeight.SemiBold
                )
            )
            AeonChip(
                text = record.recordStatusLabel(),
                variant = record.status.recordStatusVariant(),
                size = AeonChipSize.Compact
            )
        }

        Text(
            text = record.title,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    repeatDelayMillis = 1_500
                ),
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(140)) + fadeIn(animationSpec = tween(120)),
            exit = shrinkVertically(animationSpec = tween(120)) + fadeOut(animationSpec = tween(90))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                record.description?.takeIf(String::isNotBlank)?.let { description ->
                    Text(
                        text = description,
                        style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
                    )
                }

                Text(
                    text = "Date: ${record.date.recordsHeaderLabel()}",
                    style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
                )
                Text(
                    text = "Category: ${record.category.toReadableRoutineMeta()}",
                    style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
                )
                Text(
                    text = "Type: ${record.timeType.toReadableRoutineMeta()}",
                    style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
                )
                Text(
                    text = "Status: ${record.recordStatusLabel()}",
                    style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
                )

                record.snoozedUntil?.let { snoozedUntil ->
                    Text(
                        text = "Snoozed until: ${snoozedUntil.toRoutineDateTimeLabel()}",
                        style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
                    )
                }

                record.skipReason?.takeIf(String::isNotBlank)?.let { skipReason ->
                    Text(
                        text = "Skip reason: $skipReason",
                        style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
                    )
                }

                record.completionNote?.takeIf(String::isNotBlank)?.let { completionNote ->
                    Text(
                        text = "Completion note: $completionNote",
                        style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusRoutineMonthPickerDialog(
    selectedMonth: YearMonth,
    onDismiss: () -> Unit,
    onSelected: (YearMonth) -> Unit
) {
    var visibleYear by remember(selectedMonth) { mutableIntStateOf(selectedMonth.year) }
    val locale = remember { Locale.getDefault() }
    val colors = AeonThemeTokens.colors
    val monthRows = remember {
        (1..12).chunked(3)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = colors.surface.copy(alpha = 0.98f),
                border = BorderStroke(1.dp, colors.brand.copy(alpha = 0.22f)),
                shadowElevation = 28.dp
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Choose month",
                            style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AeonChip(
                                text = "Prev",
                                variant = AeonChipVariant.Outline,
                                size = AeonChipSize.Compact,
                                onClick = { visibleYear -= 1 }
                            )
                            AeonChip(
                                text = "Next",
                                variant = AeonChipVariant.Outline,
                                size = AeonChipSize.Compact,
                                onClick = { visibleYear += 1 }
                            )
                        }
                    }

                    Text(
                        text = visibleYear.toString(),
                        style = AeonTextStyles.CardTitle.copy(
                            color = colors.brand,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    monthRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { monthValue ->
                                val candidate = YearMonth.of(visibleYear, monthValue)
                                val isSelected = candidate == selectedMonth
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp),
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isSelected) {
                                        colors.brand.copy(alpha = 0.92f)
                                    } else {
                                        colors.surfaceElevated.copy(alpha = 0.82f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) {
                                            Color.Transparent
                                        } else {
                                            colors.borderSoft.copy(alpha = 0.48f)
                                        }
                                    ),
                                    onClick = { onSelected(candidate) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = Month.of(monthValue)
                                                .getDisplayName(TextStyle.SHORT, locale),
                                            style = AeonTextStyles.ButtonMedium.copy(
                                                color = if (isSelected) Color.White else colors.textPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String.toFocusYearMonth(): YearMonth {
    return runCatching { YearMonth.parse(this) }
        .getOrElse { YearMonth.now() }
}

private fun YearMonth.defaultRecordsDate(): LocalDate {
    val today = LocalDate.now()
    return if (this == YearMonth.from(today)) today else atDay(1)
}

private fun YearMonth.toFocusMonthLabel(): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    return atDay(1).format(formatter)
}

private fun Month.displayName(): String {
    return getDisplayName(TextStyle.FULL, Locale.getDefault())
}

private fun LocalDate.recordsHeaderLabel(): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
    return format(formatter)
}

private fun FocusRoutineOccurrenceEntity.startTimeLabel(): String =
    plannedStartAt?.toMinutesOfDay()?.toTimeLabel() ?: "Anytime"

private fun FocusRoutineOccurrenceEntity.endTimeLabel(): String =
    plannedEndAt?.toMinutesOfDay()?.toTimeLabel() ?: "Today"

private fun FocusRoutineOccurrenceEntity.timeRangeLabel(): String =
    "${startTimeLabel()} - ${endTimeLabel()}"

private fun FocusRoutineOccurrenceEntity.recordStatusLabel(): String = when (status) {
    FocusRoutineStatusStorage.Done -> "Done"
    FocusRoutineStatusStorage.Missed -> "Missed"
    FocusRoutineStatusStorage.Current -> "Current"
    FocusRoutineStatusStorage.Snoozed -> "Snoozed"
    FocusRoutineStatusStorage.Skipped -> "Skipped"
    else -> "Open"
}

private fun String.recordStatusVariant(): AeonChipVariant = when (this) {
    FocusRoutineStatusStorage.Done -> AeonChipVariant.Success
    FocusRoutineStatusStorage.Missed -> AeonChipVariant.Danger
    FocusRoutineStatusStorage.Current -> AeonChipVariant.Premium
    FocusRoutineStatusStorage.Snoozed -> AeonChipVariant.Warning
    FocusRoutineStatusStorage.Skipped -> AeonChipVariant.Ghost
    else -> AeonChipVariant.Info
}

private fun Instant.toMinutesOfDay(): Int {
    val localTime = atZone(ZoneId.systemDefault()).toLocalTime()
    return localTime.hour * 60 + localTime.minute
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

private fun Instant.toRoutineDateTimeLabel(): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault())
    return atZone(ZoneId.systemDefault()).format(formatter)
}

private fun String.toReadableRoutineMeta(): String =
    split('_', '-', ' ')
        .filter(String::isNotBlank)
        .joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }
