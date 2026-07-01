package com.aeon.app.ui.screens.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aeon.app.core.notifications.AeonNotificationRecord
import com.aeon.app.core.notifications.AeonNotificationSource
import com.aeon.app.core.notifications.AeonNotificationStatus
import com.aeon.app.core.notifications.AeonNotificationType
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
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * NOTIFICATION INBOX SCREEN
 *
 * Purpose:
 * Notification history and inbox screen for Aeon.
 *
 * Handles:
 * - Recent notification history
 * - Delivered / opened / dismissed / scheduled / failed states
 * - Filtering
 * - Open deep-link routes
 * - Mark tapped/opened
 * - Mark dismissed
 * - Clear history
 *
 * Senior Developer Rule:
 * This screen talks only to AeonNotificationCenter.
 * It does not access Room DAO, Scheduler, Publisher, or RuleEngine directly.
 */


// ----------------------------------------------------
// Route
// ----------------------------------------------------

@Composable
fun NotificationInboxRoute(
    viewModel: AeonNotificationViewModel = viewModel(),
    onBack: () -> Unit = {},
    onOpenRoute: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val records = uiState.records

    var selectedFilterName by rememberSaveable {
        mutableStateOf(NotificationInboxFilter.All.name)
    }

    val selectedFilter = remember(selectedFilterName) {
        NotificationInboxFilter.valueOf(selectedFilterName)
    }

    val filteredRecords = remember(records, selectedFilter) {
        records.filter { record ->
            selectedFilter.matches(record)
        }
    }

    val state = remember(records, filteredRecords, selectedFilter, uiState.message) {
        NotificationInboxUiState(
            records = records,
            filteredRecords = filteredRecords,
            selectedFilter = selectedFilter,
            stats = NotificationInboxStats.from(records),
            message = uiState.message
        )
    }

    NotificationInboxScreen(
        state = state,
        onBack = onBack,
        onFilterSelected = { filter ->
            selectedFilterName = filter.name
        },
        onOpenRecord = { record ->
            viewModel.openRecord(record)
        },
        onMarkOpened = { record ->
            viewModel.markTapped(record.payloadId)
        },
        onDismissRecord = { record ->
            viewModel.markDismissed(record.payloadId)
        },
        onClearHistory = {
            viewModel.clearHistory()
        },
        modifier = modifier
    )
}


// ----------------------------------------------------
// State
// ----------------------------------------------------

@Immutable
data class NotificationInboxUiState(
    val records: List<AeonNotificationRecord> = emptyList(),
    val filteredRecords: List<AeonNotificationRecord> = emptyList(),
    val selectedFilter: NotificationInboxFilter = NotificationInboxFilter.All,
    val stats: NotificationInboxStats = NotificationInboxStats(),
    val message: String? = null
)


@Immutable
data class NotificationInboxStats(
    val total: Int = 0,
    val unread: Int = 0,
    val scheduled: Int = 0,
    val failed: Int = 0
) {
    companion object {
        fun from(records: List<AeonNotificationRecord>): NotificationInboxStats {
            return NotificationInboxStats(
                total = records.size,
                unread = records.count { it.status == AeonNotificationStatus.Delivered },
                scheduled = records.count {
                    it.status == AeonNotificationStatus.Pending ||
                        it.status == AeonNotificationStatus.Scheduled
                },
                failed = records.count {
                    it.status == AeonNotificationStatus.Failed ||
                        it.status == AeonNotificationStatus.Suppressed
                }
            )
        }
    }
}


// ----------------------------------------------------
// Filter
// ----------------------------------------------------

enum class NotificationInboxFilter(
    val title: String
) {
    All("All"),
    Unread("Unread"),
    Scheduled("Scheduled"),
    Opened("Opened"),
    Dismissed("Dismissed"),
    Failed("Failed");

    fun matches(record: AeonNotificationRecord): Boolean {
        return when (this) {
            All -> true

            Unread -> record.status == AeonNotificationStatus.Delivered

            Scheduled -> record.status == AeonNotificationStatus.Pending ||
                record.status == AeonNotificationStatus.Scheduled

            Opened -> record.status == AeonNotificationStatus.Tapped

            Dismissed -> record.status == AeonNotificationStatus.Dismissed ||
                record.status == AeonNotificationStatus.Cancelled

            Failed -> record.status == AeonNotificationStatus.Failed ||
                record.status == AeonNotificationStatus.Suppressed
        }
    }
}


// ----------------------------------------------------
// Screen
// ----------------------------------------------------

@Composable
fun NotificationInboxScreen(
    state: NotificationInboxUiState,
    onBack: () -> Unit,
    onFilterSelected: (NotificationInboxFilter) -> Unit,
    onOpenRecord: (AeonNotificationRecord) -> Unit,
    onMarkOpened: (AeonNotificationRecord) -> Unit,
    onDismissRecord: (AeonNotificationRecord) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    AeonScreen(
        modifier = modifier
    ) {
        NotificationInboxHeader(
            state = state,
            onBack = onBack,
            onClearHistory = onClearHistory
        )

        NotificationInboxStatsCard(
            stats = state.stats
        )

        NotificationInboxFilters(
            selectedFilter = state.selectedFilter,
            onFilterSelected = onFilterSelected
        )

        AnimatedVisibility(
            visible = !state.message.isNullOrBlank()
        ) {
            AeonCard(
                variant = AeonCardVariant.Glass
            ) {
                Text(
                    text = state.message.orEmpty(),
                    style = AeonTextStyles.Caption,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (state.filteredRecords.isEmpty()) {
            NotificationInboxEmptyState(
                selectedFilter = state.selectedFilter
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
            ) {
                state.filteredRecords.forEach { record ->
                    NotificationRecordCard(
                        record = record,
                        onOpenRecord = onOpenRecord,
                        onMarkOpened = onMarkOpened,
                        onDismissRecord = onDismissRecord
                    )
                }
            }
        }
    }
}


// ----------------------------------------------------
// Header
// ----------------------------------------------------

@Composable
private fun NotificationInboxHeader(
    state: NotificationInboxUiState,
    onBack: () -> Unit,
    onClearHistory: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Aeon center",
        title = "Notification inbox",
        subtitle = "Review reminders, system alerts, AI insights, and notification history.",
        size = AeonSectionHeaderSize.Hero,
        tone = AeonSectionHeaderTone.Brand,
        action = {
            AeonChip(
                text = "${state.stats.total} total",
                variant = AeonChipVariant.Premium,
                size = AeonChipSize.Compact
            )
        }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
    ) {
        AeonButton(
            text = "Back",
            onClick = onBack,
            variant = AeonButtonVariant.Ghost,
            size = AeonButtonSize.Small
        )

        AeonButton(
            text = "Clear history",
            onClick = onClearHistory,
            variant = AeonButtonVariant.Danger,
            size = AeonButtonSize.Small,
            enabled = state.records.isNotEmpty()
        )
    }
}


// ----------------------------------------------------
// Stats
// ----------------------------------------------------

@Composable
private fun NotificationInboxStatsCard(
    stats: NotificationInboxStats
) {
    AeonCard(
        variant = AeonCardVariant.Hero
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
        ) {
            NotificationStatItem(
                label = "Unread",
                value = stats.unread.toString(),
                modifier = Modifier.weight(1f)
            )

            NotificationStatItem(
                label = "Scheduled",
                value = stats.scheduled.toString(),
                modifier = Modifier.weight(1f)
            )

            NotificationStatItem(
                label = "Issues",
                value = stats.failed.toString(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
private fun NotificationStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = value,
            style = AeonTextStyles.StatNumber,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = label,
            style = AeonTextStyles.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ----------------------------------------------------
// Filters
// ----------------------------------------------------

@Composable
private fun NotificationInboxFilters(
    selectedFilter: NotificationInboxFilter,
    onFilterSelected: (NotificationInboxFilter) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Filter",
        title = "History view",
        subtitle = "Choose which notification state you want to inspect.",
        size = AeonSectionHeaderSize.Medium
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
    ) {
        NotificationInboxFilter.entries.forEach { filter ->
            AeonChip(
                text = filter.title,
                variant = if (filter == selectedFilter) {
                    AeonChipVariant.Filled
                } else {
                    AeonChipVariant.Outline
                },
                size = AeonChipSize.Medium,
                onClick = {
                    onFilterSelected(filter)
                }
            )
        }
    }
}


// ----------------------------------------------------
// Record Card
// ----------------------------------------------------

@Composable
private fun NotificationRecordCard(
    record: AeonNotificationRecord,
    onOpenRecord: (AeonNotificationRecord) -> Unit,
    onMarkOpened: (AeonNotificationRecord) -> Unit,
    onDismissRecord: (AeonNotificationRecord) -> Unit
) {
    val canOpen = !record.deepLinkRoute.isNullOrBlank()
    val canDismiss = record.status == AeonNotificationStatus.Delivered ||
        record.status == AeonNotificationStatus.Scheduled ||
        record.status == AeonNotificationStatus.Pending

    AeonCard(
        variant = record.cardVariant(),
        onClick = if (canOpen) {
            { onOpenRecord(record) }
        } else {
            null
        }
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
                    text = record.title,
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = record.body,
                    style = AeonTextStyles.CardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AeonChip(
                text = record.status.uiLabel(),
                variant = record.status.chipVariant(),
                size = AeonChipSize.Compact
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = AeonSpacing.XSmall),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )

        NotificationRecordMeta(
            record = record
        )

        NotificationRecordActions(
            record = record,
            canOpen = canOpen,
            canDismiss = canDismiss,
            onOpenRecord = onOpenRecord,
            onMarkOpened = onMarkOpened,
            onDismissRecord = onDismissRecord
        )
    }
}


// ----------------------------------------------------
// Record Meta
// ----------------------------------------------------

@Composable
private fun NotificationRecordMeta(
    record: AeonNotificationRecord
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
    ) {
        NotificationMetaLine(
            label = "Type",
            value = record.type.uiLabel()
        )

        NotificationMetaLine(
            label = "Source",
            value = record.source.uiLabel()
        )

        NotificationMetaLine(
            label = "Created",
            value = record.createdAtEpochMillis.asNotificationTime()
        )

        if (record.deliveredAtEpochMillis != null) {
            NotificationMetaLine(
                label = "Delivered",
                value = record.deliveredAtEpochMillis.asNotificationTime()
            )
        }

        if (!record.failureReason.isNullOrBlank()) {
            NotificationMetaLine(
                label = "Issue",
                value = record.failureReason
            )
        }
    }
}


@Composable
private fun NotificationMetaLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = AeonTextStyles.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = AeonTextStyles.Caption,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


// ----------------------------------------------------
// Record Actions
// ----------------------------------------------------

@Composable
private fun NotificationRecordActions(
    record: AeonNotificationRecord,
    canOpen: Boolean,
    canDismiss: Boolean,
    onOpenRecord: (AeonNotificationRecord) -> Unit,
    onMarkOpened: (AeonNotificationRecord) -> Unit,
    onDismissRecord: (AeonNotificationRecord) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
    ) {
        AeonButton(
            text = "Open",
            onClick = {
                onOpenRecord(record)
            },
            variant = AeonButtonVariant.Secondary,
            size = AeonButtonSize.Small,
            enabled = canOpen
        )

        AeonButton(
            text = "Mark opened",
            onClick = {
                onMarkOpened(record)
            },
            variant = AeonButtonVariant.Ghost,
            size = AeonButtonSize.Small,
            enabled = record.status != AeonNotificationStatus.Tapped
        )

        AeonButton(
            text = "Dismiss",
            onClick = {
                onDismissRecord(record)
            },
            variant = AeonButtonVariant.Ghost,
            size = AeonButtonSize.Small,
            enabled = canDismiss
        )
    }
}


// ----------------------------------------------------
// Empty State
// ----------------------------------------------------

@Composable
private fun NotificationInboxEmptyState(
    selectedFilter: NotificationInboxFilter
) {
    AeonCard(
        variant = AeonCardVariant.Glass
    ) {
        Text(
            text = "No notifications found",
            style = AeonTextStyles.EmptyStateTitle,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = when (selectedFilter) {
                NotificationInboxFilter.All ->
                    "Aeon has not stored any notification history yet."

                NotificationInboxFilter.Unread ->
                    "You have no unread delivered notifications."

                NotificationInboxFilter.Scheduled ->
                    "No notifications are currently visible as scheduled in history."

                NotificationInboxFilter.Opened ->
                    "You have not opened any notifications yet."

                NotificationInboxFilter.Dismissed ->
                    "No dismissed or cancelled notifications found."

                NotificationInboxFilter.Failed ->
                    "No failed or suppressed notifications found."
            },
            style = AeonTextStyles.EmptyStateBody,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ----------------------------------------------------
// UI Helpers
// ----------------------------------------------------

private fun AeonNotificationRecord.cardVariant(): AeonCardVariant {
    return when (status) {
        AeonNotificationStatus.Delivered -> AeonCardVariant.Elevated
        AeonNotificationStatus.Tapped -> AeonCardVariant.Default
        AeonNotificationStatus.Dismissed,
        AeonNotificationStatus.Cancelled -> AeonCardVariant.Compact

        AeonNotificationStatus.Failed,
        AeonNotificationStatus.Suppressed -> AeonCardVariant.Insight

        AeonNotificationStatus.Pending,
        AeonNotificationStatus.Scheduled,
        AeonNotificationStatus.Draft -> AeonCardVariant.Glass
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

        AeonNotificationStatus.Tapped -> AeonChipVariant.Filled

        AeonNotificationStatus.Dismissed,
        AeonNotificationStatus.Cancelled -> AeonChipVariant.Outline

        AeonNotificationStatus.Suppressed,
        AeonNotificationStatus.Failed -> AeonChipVariant.Danger
    }
}


private fun AeonNotificationType.uiLabel(): String {
    return name
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
        }
}


private fun AeonNotificationSource.uiLabel(): String {
    return name
        .replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
        }
}


private fun Long.asNotificationTime(): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM, h:mm a")

    return Instant
        .ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
