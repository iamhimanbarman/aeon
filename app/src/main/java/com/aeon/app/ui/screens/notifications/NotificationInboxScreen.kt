package com.aeon.app.ui.screens.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aeon.app.core.notifications.AeonNotificationRecord
import com.aeon.app.core.notifications.AeonNotificationSource
import com.aeon.app.core.notifications.AeonNotificationStatus
import com.aeon.app.core.notifications.AeonNotificationType
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.feedback.AeonDialog
import com.aeon.app.ui.components.feedback.AeonNoDataState
import com.aeon.app.ui.components.feedback.AeonToastDuration
import com.aeon.app.ui.components.feedback.LocalAeonToastHostState
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.components.layout.AeonScreenConfig
import com.aeon.app.ui.components.layout.aeonPremiumBackgroundBrush
import com.aeon.app.ui.theme.AeonComponentShapes
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NotificationInboxRoute(
    viewModel: AeonNotificationViewModel = viewModel(),
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenRoute: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastHostState = LocalAeonToastHostState.current
    val records = uiState.records

    var selectedFilterName by rememberSaveable {
        mutableStateOf(NotificationInboxFilter.All.name)
    }

    val selectedFilter = remember(selectedFilterName) {
        NotificationInboxFilter.valueOf(selectedFilterName)
    }

    LaunchedEffect(viewModel, onOpenRoute, toastHostState) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AeonNotificationUiEvent.NavigateToRoute -> onOpenRoute(event.route)
                is AeonNotificationUiEvent.Toast -> {
                    toastHostState.showNeutral(
                        title = event.message,
                        duration = AeonToastDuration.Short
                    )
                }

                else -> Unit
            }
        }
    }

    val filteredRecords = remember(records, selectedFilter) {
        records.filter(selectedFilter::matches)
    }

    val state = remember(records, filteredRecords, selectedFilter, uiState.working) {
        NotificationInboxUiState(
            records = records,
            filteredRecords = filteredRecords,
            selectedFilter = selectedFilter,
            working = uiState.working
        )
    }

    NotificationInboxScreen(
        state = state,
        onOpenSettings = onOpenSettings,
        onRefresh = viewModel::refresh,
        onMarkAllAsRead = viewModel::markAllAsRead,
        onFilterSelected = { filter ->
            selectedFilterName = filter.name
        },
        onMarkOpened = { record ->
            viewModel.markTapped(record.payloadId)
        },
        onClearHistory = viewModel::clearHistory,
        modifier = modifier
    )
}

@Immutable
private data class NotificationInboxUiState(
    val records: List<AeonNotificationRecord> = emptyList(),
    val filteredRecords: List<AeonNotificationRecord> = emptyList(),
    val selectedFilter: NotificationInboxFilter = NotificationInboxFilter.All,
    val working: Boolean = false
) {
    val hasUnread: Boolean
        get() = records.any { it.status == AeonNotificationStatus.Delivered }

    fun countFor(filter: NotificationInboxFilter): Int {
        return records.count(filter::matches)
    }
}

@Immutable
private data class NotificationDaySection(
    val date: LocalDate,
    val label: String,
    val records: List<AeonNotificationRecord>
)

private enum class NotificationInboxFilter(
    val title: String
) {
    All("All"),
    Unread("Unread"),
    Read("Read");

    fun matches(record: AeonNotificationRecord): Boolean {
        return when (this) {
            All -> true
            Unread -> record.status == AeonNotificationStatus.Delivered
            Read -> record.isReadState()
        }
    }
}

@Composable
private fun NotificationInboxScreen(
    state: NotificationInboxUiState,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onFilterSelected: (NotificationInboxFilter) -> Unit,
    onMarkOpened: (AeonNotificationRecord) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedRecordId by rememberSaveable { mutableStateOf<String?>(null) }
    var actionsExpanded by rememberSaveable { mutableStateOf(false) }
    var showClearHistoryDialog by rememberSaveable { mutableStateOf(false) }

    val groupedSections = remember(state.filteredRecords) {
        state.filteredRecords.toDaySections()
    }

    LaunchedEffect(groupedSections, expandedRecordId) {
        if (expandedRecordId == null) return@LaunchedEffect

        val stillVisible = groupedSections
            .asSequence()
            .flatMap { section -> section.records.asSequence() }
            .any { record -> record.id == expandedRecordId }

        if (!stillVisible) {
            expandedRecordId = null
        }
    }

    if (showClearHistoryDialog) {
        AeonDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = "Clear notification history",
            body = "This removes the stored inbox history from the device.",
            actions = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AeonButton(
                        text = "Cancel",
                        onClick = { showClearHistoryDialog = false },
                        modifier = Modifier.weight(1f),
                        variant = AeonButtonVariant.Secondary,
                        size = AeonButtonSize.Small
                    )
                    AeonButton(
                        text = "Clear",
                        onClick = {
                            showClearHistoryDialog = false
                            onClearHistory()
                        },
                        modifier = Modifier.weight(1f),
                        variant = AeonButtonVariant.Danger,
                        size = AeonButtonSize.Small
                    )
                }
            }
        )
    }

    AeonScreen(
        modifier = modifier,
        config = AeonScreenConfig(scrollable = false),
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NotificationInboxTopBar(
            enabled = !state.working,
            onRefresh = {
                actionsExpanded = false
                onRefresh()
            },
            onMarkAllAsRead = {
                actionsExpanded = false
                onMarkAllAsRead()
            },
            onOpenSettings = {
                actionsExpanded = false
                onOpenSettings()
            },
            onShowClearHistory = {
                actionsExpanded = false
                showClearHistoryDialog = true
            },
            actionsExpanded = actionsExpanded,
            onActionsExpandedChange = { actionsExpanded = it },
            hasUnread = state.hasUnread,
            hasHistory = state.records.isNotEmpty()
        )

        NotificationInboxFilters(
            state = state,
            onFilterSelected = onFilterSelected
        )

        if (groupedSections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                NotificationInboxEmptyState(
                    selectedFilter = state.selectedFilter,
                    onResetFilter = {
                        onFilterSelected(NotificationInboxFilter.All)
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                groupedSections.forEach { section ->
                    item(key = "section_${section.date}") {
                        NotificationDateHeader(
                            label = section.label,
                            count = section.records.size
                        )
                    }

                    items(
                        items = section.records,
                        key = { record -> record.id }
                    ) { record ->
                        NotificationRecordBar(
                            record = record,
                            expanded = expandedRecordId == record.id,
                            onToggleExpanded = {
                                val willExpand = expandedRecordId != record.id
                                if (willExpand && record.status == AeonNotificationStatus.Delivered) {
                                    onMarkOpened(record)
                                }
                                expandedRecordId = if (expandedRecordId == record.id) {
                                    null
                                } else {
                                    record.id
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationInboxTopBar(
    enabled: Boolean,
    onRefresh: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowClearHistory: () -> Unit,
    actionsExpanded: Boolean,
    onActionsExpandedChange: (Boolean) -> Unit,
    hasUnread: Boolean,
    hasHistory: Boolean
) {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Notifications",
            style = AeonTextStyles.SectionTitle.copy(
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        )

        Box {
            Surface(
                shape = CircleShape,
                color = colors.surfaceElevated.copy(alpha = 0.94f),
                border = BorderStroke(
                    width = 1.dp,
                    color = colors.divider.copy(alpha = 0.72f)
                )
            ) {
                IconButton(
                    enabled = enabled,
                    onClick = {
                        onActionsExpandedChange(true)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Notification options",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = actionsExpanded,
                onDismissRequest = { onActionsExpandedChange(false) },
                containerColor = colors.surfaceElevated
            ) {
                DropdownMenuItem(
                    text = { Text("Refresh") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null
                        )
                    },
                    onClick = onRefresh,
                    enabled = enabled
                )
                DropdownMenuItem(
                    text = { Text("Mark all as read") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DoneAll,
                            contentDescription = null
                        )
                    },
                    onClick = onMarkAllAsRead,
                    enabled = enabled && hasUnread
                )
                DropdownMenuItem(
                    text = { Text("Notification settings") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null
                        )
                    },
                    onClick = onOpenSettings
                )
                DropdownMenuItem(
                    text = { Text("Clear history") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = null
                        )
                    },
                    onClick = onShowClearHistory,
                    enabled = enabled && hasHistory
                )
            }
        }
    }
}

@Composable
private fun NotificationInboxFilters(
    state: NotificationInboxUiState,
    onFilterSelected: (NotificationInboxFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(NotificationInboxFilter.entries) { filter ->
            AeonChip(
                text = "${filter.title} ${state.countFor(filter)}",
                variant = filter.filterChipVariant(
                    selected = filter == state.selectedFilter
                ),
                size = AeonChipSize.Compact,
                onClick = {
                    onFilterSelected(filter)
                }
            )
        }
    }
}

@Composable
private fun NotificationDateHeader(
    label: String,
    count: Int
) {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
        )
        Text(
            text = "$count",
            style = AeonTextStyles.Caption.copy(color = colors.textTertiary)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationRecordBar(
    record: AeonNotificationRecord,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    val colors = AeonThemeTokens.colors
    val accent = record.accentColor()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = AeonDuration.Fast,
                    easing = AeonEasing.Standard
                )
            ),
        onClick = onToggleExpanded,
        shape = AeonComponentShapes.CardCompact,
        color = record.containerColor(),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = if (record.status == AeonNotificationStatus.Delivered) 1.2.dp else 1.dp,
            color = accent.copy(
                alpha = if (record.status == AeonNotificationStatus.Delivered) 0.32f else 0.18f
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.14f),
                    contentColor = accent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = record.type.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = record.title,
                            modifier = Modifier.weight(1f),
                            style = AeonTextStyles.CardTitle.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (record.status == AeonNotificationStatus.Delivered) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(accent, CircleShape)
                            )
                        }
                    }

                    Text(
                        text = record.collapsedMetaLabel(),
                        style = AeonTextStyles.Caption.copy(color = colors.textSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = record.primaryMomentTimeLabel(),
                        style = AeonTextStyles.Micro.copy(color = colors.textTertiary),
                        maxLines = 1
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(
                        durationMillis = AeonDuration.Fast,
                        easing = AeonEasing.Decelerate
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = AeonDuration.Fast,
                        easing = AeonEasing.Decelerate
                    )
                ),
                exit = shrinkVertically(
                    animationSpec = tween(
                        durationMillis = AeonDuration.UltraFast,
                        easing = AeonEasing.Accelerate
                    )
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = AeonDuration.UltraFast,
                        easing = AeonEasing.Accelerate
                    )
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = colors.divider.copy(alpha = 0.5f))

                    Text(
                        text = record.body,
                        style = AeonTextStyles.CardSubtitle.copy(color = colors.textPrimary)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AeonChip(
                            text = record.statusDisplayLabel(),
                            variant = record.status.chipVariant(),
                            size = AeonChipSize.Compact
                        )
                        AeonChip(
                            text = record.type.uiLabel(),
                            variant = AeonChipVariant.Outline,
                            size = AeonChipSize.Compact
                        )
                        AeonChip(
                            text = record.source.uiLabel(),
                            variant = AeonChipVariant.Ghost,
                            size = AeonChipSize.Compact
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationInboxEmptyState(
    selectedFilter: NotificationInboxFilter,
    onResetFilter: () -> Unit
) {
    AeonNoDataState(
        title = "No notifications",
        message = selectedFilter.emptyMessage(),
        icon = {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        actionText = if (selectedFilter != NotificationInboxFilter.All) {
            "Show all"
        } else {
            null
        },
        onAction = if (selectedFilter != NotificationInboxFilter.All) {
            onResetFilter
        } else {
            null
        }
    )
}

private fun List<AeonNotificationRecord>.toDaySections(): List<NotificationDaySection> {
    if (isEmpty()) return emptyList()

    val today = LocalDate.now()

    return sortedByDescending { record ->
        record.primaryMomentEpochMillis()
    }.groupBy { record ->
        record.primaryMomentLocalDate()
    }.entries.sortedByDescending { entry ->
        entry.key
    }.map { entry ->
        NotificationDaySection(
            date = entry.key,
            label = entry.key.toNotificationSectionLabel(today),
            records = entry.value.sortedByDescending { record ->
                record.primaryMomentEpochMillis()
            }
        )
    }
}

private fun NotificationInboxFilter.emptyMessage(): String {
    return when (this) {
        NotificationInboxFilter.All ->
            "Aeon has not stored any notification history yet."

        NotificationInboxFilter.Unread ->
            "You do not have unread notifications right now."

        NotificationInboxFilter.Read ->
            "No read notifications were found."
    }
}

private fun AeonNotificationRecord.primaryMomentEpochMillis(): Long {
    return when (status) {
        AeonNotificationStatus.Tapped -> tappedAtEpochMillis
        AeonNotificationStatus.Dismissed,
        AeonNotificationStatus.Cancelled -> dismissedAtEpochMillis

        AeonNotificationStatus.Delivered -> deliveredAtEpochMillis

        AeonNotificationStatus.Pending,
        AeonNotificationStatus.Scheduled,
        AeonNotificationStatus.Draft -> scheduledAtEpochMillis

        AeonNotificationStatus.Suppressed,
        AeonNotificationStatus.Failed -> deliveredAtEpochMillis
    } ?: createdAtEpochMillis
}

private fun AeonNotificationRecord.primaryMomentLocalDate(): LocalDate {
    return Instant
        .ofEpochMilli(primaryMomentEpochMillis())
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun AeonNotificationRecord.primaryMomentTimeLabel(): String {
    return Instant
        .ofEpochMilli(primaryMomentEpochMillis())
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a"))
}

private fun AeonNotificationRecord.collapsedMetaLabel(): String {
    return "${statusDisplayLabel()} | ${type.uiLabel()} | ${source.uiLabel()}"
}

@Composable
private fun AeonNotificationRecord.containerColor(): Color {
    val colors = AeonThemeTokens.colors
    val accent = accentColor()

    val blendFraction = when (status) {
        AeonNotificationStatus.Delivered -> 0.14f
        AeonNotificationStatus.Tapped -> 0.035f
        AeonNotificationStatus.Pending,
        AeonNotificationStatus.Scheduled,
        AeonNotificationStatus.Draft -> 0.08f

        AeonNotificationStatus.Dismissed,
        AeonNotificationStatus.Cancelled -> 0.04f

        AeonNotificationStatus.Suppressed,
        AeonNotificationStatus.Failed -> 0.1f
    }

    return lerp(
        start = colors.surfaceElevated,
        stop = accent,
        fraction = blendFraction
    )
}

@Composable
private fun AeonNotificationRecord.accentColor(): Color {
    val colors = AeonThemeTokens.colors

    return when (status) {
        AeonNotificationStatus.Failed,
        AeonNotificationStatus.Suppressed -> colors.warning

        AeonNotificationStatus.Delivered -> colors.habit

        AeonNotificationStatus.Tapped -> colors.textSecondary

        AeonNotificationStatus.Pending,
        AeonNotificationStatus.Scheduled,
        AeonNotificationStatus.Draft -> colors.info

        AeonNotificationStatus.Dismissed,
        AeonNotificationStatus.Cancelled -> colors.textTertiary
    }
}

private fun AeonNotificationStatus.uiLabel(): String {
    return when (this) {
        AeonNotificationStatus.Draft -> "Draft"
        AeonNotificationStatus.Pending -> "Pending"
        AeonNotificationStatus.Scheduled -> "Scheduled"
        AeonNotificationStatus.Delivered -> "Unread"
        AeonNotificationStatus.Tapped -> "Opened"
        AeonNotificationStatus.Dismissed -> "Dismissed"
        AeonNotificationStatus.Cancelled -> "Cancelled"
        AeonNotificationStatus.Suppressed -> "Suppressed"
        AeonNotificationStatus.Failed -> "Failed"
    }
}

private fun AeonNotificationStatus.chipVariant(): AeonChipVariant {
    return when (this) {
        AeonNotificationStatus.Delivered -> AeonChipVariant.Success
        AeonNotificationStatus.Pending,
        AeonNotificationStatus.Scheduled,
        AeonNotificationStatus.Draft -> AeonChipVariant.Info

        AeonNotificationStatus.Tapped,
        AeonNotificationStatus.Dismissed,
        AeonNotificationStatus.Cancelled -> AeonChipVariant.Outline

        AeonNotificationStatus.Suppressed,
        AeonNotificationStatus.Failed -> AeonChipVariant.Danger
    }
}

private fun AeonNotificationRecord.isReadState(): Boolean {
    return status == AeonNotificationStatus.Tapped ||
        status == AeonNotificationStatus.Dismissed ||
        status == AeonNotificationStatus.Cancelled
}

private fun AeonNotificationRecord.statusDisplayLabel(): String {
    return when {
        status == AeonNotificationStatus.Delivered -> "Unread"
        isReadState() -> "Read"
        else -> status.uiLabel()
    }
}

private fun NotificationInboxFilter.filterChipVariant(
    selected: Boolean
): AeonChipVariant {
    return when {
        this == NotificationInboxFilter.Unread && selected -> AeonChipVariant.Success
        selected -> AeonChipVariant.Filled
        else -> AeonChipVariant.Outline
    }
}

private fun AeonNotificationType.uiLabel(): String {
    return name
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replaceFirstChar { char ->
            if (char.isLowerCase()) {
                char.titlecase(Locale.getDefault())
            } else {
                char.toString()
            }
        }
}

private fun AeonNotificationSource.uiLabel(): String {
    return name
        .replaceFirstChar { char ->
            if (char.isLowerCase()) {
                char.titlecase(Locale.getDefault())
            } else {
                char.toString()
            }
        }
}

private fun AeonNotificationType.icon(): ImageVector {
    return when (this) {
        AeonNotificationType.DailyPlan,
        AeonNotificationType.TaskReminder -> Icons.Outlined.Checklist

        AeonNotificationType.HabitReminder -> Icons.Outlined.CheckCircleOutline

        AeonNotificationType.FocusSession,
        AeonNotificationType.BreakReminder -> Icons.Outlined.CenterFocusStrong

        AeonNotificationType.MoodCheckIn,
        AeonNotificationType.JournalReminder -> Icons.Outlined.Mood

        AeonNotificationType.HealthReminder -> Icons.Outlined.HealthAndSafety

        AeonNotificationType.FinanceReminder -> Icons.Outlined.Paid

        AeonNotificationType.GoalReminder -> Icons.Outlined.Flag

        AeonNotificationType.WeeklyReview,
        AeonNotificationType.AIInsight -> Icons.Outlined.AutoAwesome

        AeonNotificationType.DataBackup -> Icons.Outlined.Settings

        AeonNotificationType.SystemAlert -> Icons.Outlined.WarningAmber
    }
}

private fun LocalDate.toNotificationSectionLabel(
    today: LocalDate
): String {
    return when (this) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
    }
}

private fun Long.asNotificationMoment(): String {
    return Instant
        .ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a"))
}
