package com.aeon.app.ui.screens.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.core.AeonSectionHeader
import com.aeon.app.ui.components.core.AeonSectionHeaderSize
import com.aeon.app.ui.components.core.AeonSectionHeaderTone
import com.aeon.app.ui.components.feedback.AeonNoDataState
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.components.layout.aeonPremiumBackgroundBrush
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import java.time.Instant
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
    val records = uiState.records

    var selectedFilterName by rememberSaveable {
        mutableStateOf(NotificationInboxFilter.All.name)
    }

    val selectedFilter = remember(selectedFilterName) {
        NotificationInboxFilter.valueOf(selectedFilterName)
    }

    LaunchedEffect(viewModel, onOpenRoute) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AeonNotificationUiEvent.NavigateToRoute -> onOpenRoute(event.route)
                else -> Unit
            }
        }
    }

    val filteredRecords = remember(records, selectedFilter) {
        records.filter(selectedFilter::matches)
    }

    val state = remember(
        records,
        filteredRecords,
        selectedFilter,
        uiState.message,
        uiState.isHealthy,
        uiState.primaryIssue,
        uiState.enabledRuleCount
    ) {
        NotificationInboxUiState(
            records = records,
            filteredRecords = filteredRecords,
            selectedFilter = selectedFilter,
            stats = NotificationInboxStats.from(records),
            message = uiState.message,
            isHealthy = uiState.isHealthy,
            primaryIssue = uiState.primaryIssue,
            enabledRuleCount = uiState.enabledRuleCount
        )
    }

    NotificationInboxScreen(
        state = state,
        onBack = onBack,
        onOpenSettings = onOpenSettings,
        onFilterSelected = { filter ->
            selectedFilterName = filter.name
        },
        onOpenRecord = viewModel::openRecord,
        onMarkOpened = { record ->
            viewModel.markTapped(record.payloadId)
        },
        onDismissRecord = { record ->
            viewModel.markDismissed(record.payloadId)
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
    val stats: NotificationInboxStats = NotificationInboxStats(),
    val message: String? = null,
    val isHealthy: Boolean = true,
    val primaryIssue: String? = null,
    val enabledRuleCount: Int = 0
) {
    fun countFor(filter: NotificationInboxFilter): Int {
        return records.count(filter::matches)
    }
}

@Immutable
private data class NotificationInboxStats(
    val total: Int = 0,
    val unread: Int = 0,
    val scheduled: Int = 0,
    val opened: Int = 0,
    val issues: Int = 0
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
                opened = records.count { it.status == AeonNotificationStatus.Tapped },
                issues = records.count {
                    it.status == AeonNotificationStatus.Failed ||
                        it.status == AeonNotificationStatus.Suppressed
                }
            )
        }
    }
}

private enum class NotificationInboxFilter(
    val title: String
) {
    All("All"),
    Unread("Unread"),
    Scheduled("Scheduled"),
    Opened("Opened"),
    Dismissed("Dismissed"),
    Failed("Issues");

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

@Composable
private fun NotificationInboxScreen(
    state: NotificationInboxUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onFilterSelected: (NotificationInboxFilter) -> Unit,
    onOpenRecord: (AeonNotificationRecord) -> Unit,
    onMarkOpened: (AeonNotificationRecord) -> Unit,
    onDismissRecord: (AeonNotificationRecord) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    AeonScreen(
        modifier = modifier,
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NotificationInboxHeader(
            state = state,
            onBack = onBack,
            onOpenSettings = onOpenSettings,
            onClearHistory = onClearHistory
        )

        AnimatedVisibility(visible = !state.message.isNullOrBlank()) {
            NotificationInboxMessageCard(message = state.message.orEmpty())
        }

        NotificationInboxSummaryCard(state = state)

        NotificationInboxFilters(
            state = state,
            onFilterSelected = onFilterSelected
        )

        NotificationHistoryHeader(state = state)

        if (state.filteredRecords.isEmpty()) {
            NotificationInboxEmptyState(
                selectedFilter = state.selectedFilter,
                onResetFilter = {
                    onFilterSelected(NotificationInboxFilter.All)
                }
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationInboxHeader(
    state: NotificationInboxUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearHistory: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = if (state.isHealthy) {
            "Aeon system"
        } else {
            "Needs attention"
        },
        title = "Notification center",
        subtitle = state.primaryIssue
            ?: "Review reminders, AI signals, private alerts, and the full notification history.",
        size = AeonSectionHeaderSize.Hero,
        tone = if (state.isHealthy) {
            AeonSectionHeaderTone.Brand
        } else {
            AeonSectionHeaderTone.Warning
        },
        action = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AeonChip(
                    text = if (state.isHealthy) "Healthy" else "Check",
                    variant = if (state.isHealthy) {
                        AeonChipVariant.Success
                    } else {
                        AeonChipVariant.Warning
                    },
                    size = AeonChipSize.Compact
                )
                AeonChip(
                    text = "${state.stats.total} total",
                    variant = AeonChipVariant.Premium,
                    size = AeonChipSize.Compact
                )
            }
        }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AeonButton(
            text = "Back",
            onClick = onBack,
            variant = AeonButtonVariant.Ghost,
            size = AeonButtonSize.Small
        )
        AeonButton(
            text = "Settings",
            onClick = onOpenSettings,
            variant = AeonButtonVariant.Secondary,
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

@Composable
private fun NotificationInboxMessageCard(
    message: String
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Glass,
        containerColor = colors.surfaceElevated.copy(alpha = 0.82f)
    ) {
        Text(
            text = message,
            style = AeonTextStyles.Caption.copy(color = colors.brand)
        )
    }
}

@Composable
private fun NotificationInboxSummaryCard(
    state: NotificationInboxUiState
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = AeonCardVariant.Hero,
        containerColor = colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (state.isHealthy) "Delivery is stable" else "Delivery needs review",
                    style = AeonTextStyles.CardTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = state.primaryIssue
                        ?: "Channels, permissions, and active rules look ready. Aeon is storing real notification history here.",
                    style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                )
            }

            AeonChip(
                text = "${state.enabledRuleCount} rules",
                variant = AeonChipVariant.Info,
                size = AeonChipSize.Compact
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotificationStatTile(
                    label = "Unread",
                    value = state.stats.unread.toString(),
                    accent = colors.brand,
                    modifier = Modifier.weight(1f)
                )
                NotificationStatTile(
                    label = "Scheduled",
                    value = state.stats.scheduled.toString(),
                    accent = colors.info,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotificationStatTile(
                    label = "Opened",
                    value = state.stats.opened.toString(),
                    accent = colors.success,
                    modifier = Modifier.weight(1f)
                )
                NotificationStatTile(
                    label = "Issues",
                    value = state.stats.issues.toString(),
                    accent = colors.warning,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NotificationStatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        containerColor = colors.surface.copy(alpha = 0.82f),
        borderColor = accent.copy(alpha = 0.24f)
    ) {
        Text(
            text = value,
            style = AeonTextStyles.StatNumber.copy(color = accent)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = AeonTextStyles.Caption.copy(color = colors.textSecondary)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationInboxFilters(
    state: NotificationInboxUiState,
    onFilterSelected: (NotificationInboxFilter) -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonSectionHeader(
        eyebrow = "Filter",
        title = "Inbox view",
        subtitle = "Move between unread, scheduled, opened, and issue states without leaving the page.",
        size = AeonSectionHeaderSize.Medium,
        tone = AeonSectionHeaderTone.Subtle
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NotificationInboxFilter.entries.forEach { filter ->
            val count = state.countFor(filter)

            AeonChip(
                text = "${filter.title} $count",
                variant = if (filter == state.selectedFilter) {
                    AeonChipVariant.Filled
                } else {
                    AeonChipVariant.Outline
                },
                size = AeonChipSize.Medium,
                onClick = {
                    onFilterSelected(filter)
                },
                leadingIcon = if (filter == state.selectedFilter) {
                    {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.brand)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun NotificationHistoryHeader(
    state: NotificationInboxUiState
) {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = state.selectedFilter.sectionTitle(),
                style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary)
            )
            Text(
                text = "${state.filteredRecords.size} notifications in this view",
                style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
            )
        }

        AeonChip(
            text = if (state.selectedFilter == NotificationInboxFilter.All) {
                "Live history"
            } else {
                state.selectedFilter.title
            },
            variant = AeonChipVariant.Ghost,
            size = AeonChipSize.Compact
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationRecordCard(
    record: AeonNotificationRecord,
    onOpenRecord: (AeonNotificationRecord) -> Unit,
    onMarkOpened: (AeonNotificationRecord) -> Unit,
    onDismissRecord: (AeonNotificationRecord) -> Unit
) {
    val colors = AeonThemeTokens.colors
    val accent = record.accentColor()
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
        },
        containerColor = colors.surfaceElevated,
        borderColor = accent.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = record.type.icon(),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = record.title,
                            style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = record.body,
                            style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    AeonChip(
                        text = record.status.uiLabel(),
                        variant = record.status.chipVariant(),
                        size = AeonChipSize.Compact
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                    AeonChip(
                        text = record.primaryMomentLabel(),
                        variant = AeonChipVariant.Info,
                        size = AeonChipSize.Compact,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                    if (canOpen) {
                        AeonChip(
                            text = "Opens app",
                            variant = AeonChipVariant.Premium,
                            size = AeonChipSize.Compact
                        )
                    }
                }

                if (!record.failureReason.isNullOrBlank()) {
                    Text(
                        text = record.failureReason,
                        style = AeonTextStyles.Micro.copy(color = colors.warning)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            color = colors.divider.copy(alpha = 0.54f)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AeonButton(
                text = "Open",
                onClick = { onOpenRecord(record) },
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small,
                enabled = canOpen
            )
            AeonButton(
                text = "Mark opened",
                onClick = { onMarkOpened(record) },
                variant = AeonButtonVariant.Ghost,
                size = AeonButtonSize.Small,
                enabled = record.status != AeonNotificationStatus.Tapped
            )
            AeonButton(
                text = "Dismiss",
                onClick = { onDismissRecord(record) },
                variant = AeonButtonVariant.Danger,
                size = AeonButtonSize.Small,
                enabled = canDismiss
            )
        }
    }
}

@Composable
private fun NotificationInboxEmptyState(
    selectedFilter: NotificationInboxFilter,
    onResetFilter: () -> Unit
) {
    AeonNoDataState(
        title = "No notifications found",
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

@Composable
private fun AeonNotificationRecord.accentColor(): Color {
    val colors = AeonThemeTokens.colors

    return when (status) {
        AeonNotificationStatus.Failed,
        AeonNotificationStatus.Suppressed -> colors.warning
        AeonNotificationStatus.Delivered -> colors.brand
        AeonNotificationStatus.Tapped -> colors.success
        AeonNotificationStatus.Pending,
        AeonNotificationStatus.Scheduled,
        AeonNotificationStatus.Draft -> colors.info
        AeonNotificationStatus.Dismissed,
        AeonNotificationStatus.Cancelled -> colors.textTertiary
    }
}

private fun NotificationInboxFilter.sectionTitle(): String {
    return when (this) {
        NotificationInboxFilter.All -> "Recent notification history"
        NotificationInboxFilter.Unread -> "Unread notifications"
        NotificationInboxFilter.Scheduled -> "Scheduled notifications"
        NotificationInboxFilter.Opened -> "Opened notifications"
        NotificationInboxFilter.Dismissed -> "Dismissed notifications"
        NotificationInboxFilter.Failed -> "Issue notifications"
    }
}

private fun NotificationInboxFilter.emptyMessage(): String {
    return when (this) {
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
    }
}

private fun AeonNotificationRecord.primaryMomentLabel(): String {
    val timestamp = when {
        tappedAtEpochMillis != null -> tappedAtEpochMillis
        deliveredAtEpochMillis != null -> deliveredAtEpochMillis
        scheduledAtEpochMillis != null -> scheduledAtEpochMillis
        dismissedAtEpochMillis != null -> dismissedAtEpochMillis
        else -> createdAtEpochMillis
    }

    val prefix = when {
        tappedAtEpochMillis != null -> "Opened"
        deliveredAtEpochMillis != null -> "Delivered"
        scheduledAtEpochMillis != null -> "Scheduled"
        dismissedAtEpochMillis != null -> "Dismissed"
        else -> "Created"
    }

    return "$prefix ${timestamp.asNotificationTime()}"
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

private fun Long.asNotificationTime(): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM, h:mm a")

    return Instant
        .ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
