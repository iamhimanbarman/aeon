package com.aeon.app.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aeon.app.data.local.database.entities.FocusRoutineOccurrenceEntity
import com.aeon.app.data.local.database.entities.FocusRoutineStatusStorage
import com.aeon.app.di.aeonViewModel
import com.aeon.app.domain.focus.FocusRoutineResolver
import com.aeon.app.presentation.viewmodel.AeonFocusViewModel
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.components.layout.aeonPremiumBackgroundBrush
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AeonTodayRoute(
    onStartFocus: () -> Unit = {},
    onAddTask: () -> Unit = {},
    onLogMood: () -> Unit = {},
    onOpenTrack: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenHabit: (String) -> Unit = {},
    onOpenTask: (String) -> Unit = {},
    onOpenAiChat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = aeonViewModel<AeonFocusViewModel>()
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }

    LaunchedEffect(today) {
        viewModel.setRoutineDate(today)
    }

    val occurrences = remember(viewState.occurrences) {
        viewState.occurrences.sortedWith(
            compareBy<FocusRoutineOccurrenceEntity> { it.plannedStartAt ?: Instant.MAX }
                .thenBy { it.position }
        )
    }
    var now by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(60_000)
        }
    }
    val current = remember(occurrences, now) {
        FocusRoutineResolver.current(occurrences, now)
            ?: occurrences.firstOrNull {
                it.status == FocusRoutineStatusStorage.Current
            }
    }
    val future = remember(occurrences, now, current?.id) {
        occurrences
            .filter { occurrence ->
                occurrence.id != current?.id &&
                    occurrence.status == FocusRoutineStatusStorage.Upcoming &&
                    (occurrence.plannedStartAt ?: Instant.MAX).isAfter(now)
            }
            .take(2)
    }
    val previousOpen = remember(occurrences, now) {
        occurrences
            .filter { occurrence ->
                occurrence.status !in setOf(
                    FocusRoutineStatusStorage.Done,
                    FocusRoutineStatusStorage.Missed
                ) &&
                    occurrence.plannedEndAt?.isBefore(now) == true
            }
            .sortedByDescending { it.plannedEndAt }
    }

    TodayRoutineScreen(
        modifier = modifier,
        today = today,
        current = current,
        future = future,
        previousOpen = previousOpen,
        allCount = occurrences.size,
        onCreateRoutine = onStartFocus,
        onStart = viewModel::startRoutine,
        onDone = viewModel::completeRoutine,
        onMiss = viewModel::missRoutine,
        onOpenTask = onOpenTask,
        onOpenAiChat = onOpenAiChat
    )
}

@Composable
private fun TodayRoutineScreen(
    today: LocalDate,
    current: FocusRoutineOccurrenceEntity?,
    future: List<FocusRoutineOccurrenceEntity>,
    previousOpen: List<FocusRoutineOccurrenceEntity>,
    allCount: Int,
    modifier: Modifier = Modifier,
    onCreateRoutine: () -> Unit,
    onStart: (String) -> Unit,
    onDone: (String) -> Unit,
    onMiss: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenAiChat: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AeonScreen(
            modifier = modifier,
            backgroundBrush = aeonPremiumBackgroundBrush(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
        TodaySetupBar(
            date = today,
            allCount = allCount,
            onClick = onCreateRoutine
        )

        Spacer(modifier = Modifier.height(18.dp))

        CurrentWorkCard(
            occurrence = current,
            onCreateRoutine = onCreateRoutine,
            onStart = onStart,
            onDone = onDone,
            onMiss = onMiss,
            onOpenTask = onOpenTask
        )

        Spacer(modifier = Modifier.height(18.dp))

        RoutineSectionTitle("Focus Tools", "Quick utilities")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            com.aeon.app.ui.components.widgets.AeonStopwatchWidget(modifier = Modifier.weight(1f))
            com.aeon.app.ui.components.widgets.AeonTimerWidget(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(18.dp))

        RoutineSectionTitle("Next work", "Future two blocks")
        Spacer(modifier = Modifier.height(10.dp))
        if (future.isEmpty()) {
            CompactEmptyCard("No upcoming routine block is set.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                future.forEach { occurrence ->
                    CompactRoutineCard(
                        occurrence = occurrence,
                        onStart = { onStart(occurrence.id) },
                        onDone = { onDone(occurrence.id) },
                        onMiss = { onMiss(occurrence.id) },
                        onOpenTask = { occurrence.linkedTaskId?.let(onOpenTask) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        RoutineSectionTitle("Previous open work", "Not complete or missed")
        Spacer(modifier = Modifier.height(10.dp))
        if (previousOpen.isEmpty()) {
            CompactEmptyCard("No previous open work is waiting.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                previousOpen.forEach { occurrence ->
                    CompactRoutineCard(
                        occurrence = occurrence,
                        onStart = { onStart(occurrence.id) },
                        onDone = { onDone(occurrence.id) },
                        onMiss = { onMiss(occurrence.id) },
                        onOpenTask = { occurrence.linkedTaskId?.let(onOpenTask) }
                    )
                }
            }
        }
    }
    
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 24.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(AeonThemeTokens.colors.brand)
                .clickable { onOpenAiChat() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Face,
                contentDescription = "AI Chat",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun TodaySetupBar(
    date: LocalDate,
    allCount: Int,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Hero,
        onClick = onClick,
        backgroundBrush = Brush.linearGradient(
            listOf(
                colors.brand.copy(alpha = 0.20f),
                colors.intelligence.copy(alpha = 0.12f),
                colors.surfaceElevated
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())),
                    style = AeonTextStyles.SectionTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (allCount == 0) {
                        "Tap to create today’s routine. Day starts at 12:00 AM."
                    } else {
                        "$allCount routine blocks planned. Tap to edit today’s routine."
                    },
                    style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.brand.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = colors.brand
                )
            }
        }
    }
}

@Composable
private fun CurrentWorkCard(
    occurrence: FocusRoutineOccurrenceEntity?,
    onCreateRoutine: () -> Unit,
    onStart: (String) -> Unit,
    onDone: (String) -> Unit,
    onMiss: (String) -> Unit,
    onOpenTask: (String) -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(variant = AeonCardVariant.Hero) {
        Text(
            text = "Present work",
            style = AeonTextStyles.Caption.copy(
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (occurrence == null) {
            Text(
                text = "No active work right now",
                style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create today’s routine from the bar above. Aeon will surface work here when its time window begins.",
                style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
            )
            Spacer(modifier = Modifier.height(16.dp))
            AeonButton(
                text = "Create today routine",
                onClick = onCreateRoutine,
                variant = AeonButtonVariant.Primary,
                size = AeonButtonSize.Pill,
                leadingIcon = { Icon(Icons.Outlined.Schedule, null) }
            )
        } else {
            Text(
                text = occurrence.title,
                style = AeonTextStyles.SectionTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = occurrence.timeRangeLabel(),
                style = AeonTextStyles.Caption.copy(color = colors.brand)
            )
            if (!occurrence.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = occurrence.description,
                    style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            RoutineActions(
                occurrence = occurrence,
                onStart = { onStart(occurrence.id) },
                onDone = { onDone(occurrence.id) },
                onMiss = { onMiss(occurrence.id) },
                onOpenTask = { occurrence.linkedTaskId?.let(onOpenTask) }
            )
        }
    }
}

@Composable
private fun RoutineSectionTitle(
    title: String,
    subtitle: String
) {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
        )
        Text(
            text = subtitle,
            style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
        )
    }
}

@Composable
private fun CompactRoutineCard(
    occurrence: FocusRoutineOccurrenceEntity,
    onStart: () -> Unit,
    onDone: () -> Unit,
    onMiss: () -> Unit,
    onOpenTask: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Default,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = occurrence.title,
                    style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = occurrence.timeRangeLabel(),
                    style = AeonTextStyles.Caption.copy(color = colors.brand)
                )
            }
            StatusChip(status = occurrence.status)
        }

        if (!occurrence.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = occurrence.description,
                style = AeonTextStyles.Caption.copy(color = colors.textSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        RoutineActions(
            occurrence = occurrence,
            onStart = onStart,
            onDone = onDone,
            onMiss = onMiss,
            onOpenTask = onOpenTask
        )
    }
}

@Composable
private fun RoutineActions(
    occurrence: FocusRoutineOccurrenceEntity,
    onStart: () -> Unit,
    onDone: () -> Unit,
    onMiss: () -> Unit,
    onOpenTask: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (occurrence.status == FocusRoutineStatusStorage.Upcoming ||
            occurrence.status == FocusRoutineStatusStorage.Snoozed
        ) {
            AeonButton(
                text = "Start",
                onClick = onStart,
                variant = AeonButtonVariant.Tonal,
                size = AeonButtonSize.Small,
                leadingIcon = { Icon(Icons.Rounded.PlayCircle, null) }
            )
        }
        if (occurrence.status != FocusRoutineStatusStorage.Done) {
            AeonButton(
                text = "Done",
                onClick = onDone,
                variant = AeonButtonVariant.Success,
                size = AeonButtonSize.Small,
                leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) }
            )
        }
        if (occurrence.status !in setOf(FocusRoutineStatusStorage.Done, FocusRoutineStatusStorage.Missed)) {
            AeonButton(
                text = "Missed",
                onClick = onMiss,
                variant = AeonButtonVariant.Ghost,
                size = AeonButtonSize.Small
            )
        }
        if (occurrence.linkedTaskId != null) {
            AeonButton(
                text = "Task",
                onClick = onOpenTask,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small
            )
        }
    }
}

@Composable
private fun CompactEmptyCard(
    message: String
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Glass,
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = message,
            style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
        )
    }
}

@Composable
private fun StatusChip(status: String) {
    val variant = when (status) {
        FocusRoutineStatusStorage.Done -> AeonChipVariant.Success
        FocusRoutineStatusStorage.Missed -> AeonChipVariant.Danger
        FocusRoutineStatusStorage.Current -> AeonChipVariant.Premium
        FocusRoutineStatusStorage.Snoozed -> AeonChipVariant.Warning
        FocusRoutineStatusStorage.Skipped -> AeonChipVariant.Ghost
        else -> AeonChipVariant.Info
    }

    AeonChip(
        text = status.replaceFirstChar(Char::uppercaseChar),
        variant = variant,
        size = AeonChipSize.Compact
    )
}

private fun FocusRoutineOccurrenceEntity.timeRangeLabel(): String {
    val start = plannedStartAt?.toMinutesOfDay()
    val end = plannedEndAt?.toMinutesOfDay()

    return when {
        start != null && end != null -> "${start.toTimeLabel()} – ${end.toTimeLabel()}"
        start != null -> start.toTimeLabel()
        else -> "Anytime today"
    }
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
