package com.aeon.app.ui.screens.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aeon.app.core.notifications.AeonNotificationCenterState
import com.aeon.app.core.notifications.AeonNotificationChannelKey
import com.aeon.app.core.notifications.AeonNotificationPreferences
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/*
 * NOTIFICATION SETTINGS SCREEN
 *
 * Purpose:
 * Production-connected notification settings screen for Aeon.
 *
 * Handles:
 * - Master notification switch
 * - Android permission/system health
 * - Category-level notification preferences
 * - Quiet hours
 * - Digest setting
 * - Daily notification limit
 * - Channel health and blocked channel shortcuts
 *
 * Senior Developer Rule:
 * This screen talks only to AeonNotificationCenter.
 * UI does not call Scheduler, Publisher, Repository, or RuleEngine directly.
 */


// ----------------------------------------------------
// Route
// ----------------------------------------------------

@Composable
fun NotificationSettingsRoute(
    viewModel: AeonNotificationViewModel = viewModel(),
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationSettingsScreen(
        state = state,
        onBack = onBack,
        onOpenAppSettings = {
            viewModel.openAppNotificationSettings()
        },
        onOpenExactAlarmSettings = {
            viewModel.openExactAlarmSettings()
        },
        onOpenChannelSettings = { channel ->
            viewModel.openChannelSettings(channel)
        },
        onRefresh = {
            viewModel.refresh()
        },
        onUpdatePreferences = { newPreferences ->
            viewModel.updatePreferences(newPreferences)
        },
        modifier = modifier
    )
}


// ----------------------------------------------------
// Screen
// ----------------------------------------------------

@Composable
fun NotificationSettingsScreen(
    state: AeonNotificationUiState,
    onBack: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenChannelSettings: (AeonNotificationChannelKey) -> Unit,
    onRefresh: () -> Unit,
    onUpdatePreferences: (AeonNotificationPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    val preferences = state.preferences

    AeonScreen(
        modifier = modifier
    ) {
        NotificationSettingsHeader(
            state = state,
            onBack = onBack,
            onRefresh = onRefresh
        )

        NotificationSystemHealthCard(
            state = state,
            onOpenAppSettings = onOpenAppSettings,
            onOpenExactAlarmSettings = onOpenExactAlarmSettings
        )

        NotificationMasterCard(
            preferences = preferences,
            onUpdatePreferences = onUpdatePreferences
        )

        NotificationCategorySection(
            preferences = preferences,
            onUpdatePreferences = onUpdatePreferences
        )

        NotificationQuietHoursSection(
            preferences = preferences,
            onUpdatePreferences = onUpdatePreferences
        )

        NotificationDigestSection(
            preferences = preferences,
            onUpdatePreferences = onUpdatePreferences
        )

        NotificationDailyLimitSection(
            preferences = preferences,
            onUpdatePreferences = onUpdatePreferences
        )

        NotificationChannelHealthSection(
            state = state,
            onOpenChannelSettings = onOpenChannelSettings
        )
    }
}


// ----------------------------------------------------
// Header
// ----------------------------------------------------

@Composable
private fun NotificationSettingsHeader(
    state: AeonNotificationUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Aeon system",
        title = "Notifications",
        subtitle = "Control reminders, quiet hours, categories, and notification accuracy.",
        size = AeonSectionHeaderSize.Hero,
        tone = AeonSectionHeaderTone.Brand,
        action = {
            AeonChip(
                text = if (state.centerState?.isHealthy == true) {
                    "Healthy"
                } else {
                    "Check"
                },
                variant = if (state.centerState?.isHealthy == true) {
                    AeonChipVariant.Success
                } else {
                    AeonChipVariant.Warning
                },
                size = AeonChipSize.Compact,
                onClick = onRefresh
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
            text = "Refresh",
            onClick = onRefresh,
            variant = AeonButtonVariant.Secondary,
            size = AeonButtonSize.Small
        )
    }

    AnimatedVisibility(
        visible = !state.message.isNullOrBlank()
    ) {
        Text(
            text = state.message.orEmpty(),
            style = AeonTextStyles.Caption,
            color = MaterialTheme.colorScheme.primary
        )
    }
}


// ----------------------------------------------------
// System Health
// ----------------------------------------------------

@Composable
private fun NotificationSystemHealthCard(
    state: AeonNotificationUiState,
    onOpenAppSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit
) {
    val snapshot = state.centerState?.permissionSnapshot
    val issue = state.centerState?.primaryIssue

    AeonCard(
        variant = if (state.centerState?.isHealthy == true) {
            AeonCardVariant.Insight
        } else {
            AeonCardVariant.Elevated
        }
    ) {
        Text(
            text = if (state.centerState?.isHealthy == true) {
                "Notification system is ready"
            } else {
                "Notification system needs attention"
            },
            style = AeonTextStyles.CardTitle,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = issue ?: "Aeon can deliver reminders and system notifications correctly.",
            style = AeonTextStyles.CardSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
        ) {
            HealthLine(
                label = "Runtime permission",
                value = snapshot?.runtimePermissionStatus?.name ?: "Checking"
            )

            HealthLine(
                label = "App notifications",
                value = snapshot?.appNotificationState?.name ?: "Checking"
            )

            HealthLine(
                label = "Exact reminder access",
                value = snapshot?.exactAlarmAccessState?.name ?: "Checking"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
        ) {
            AeonButton(
                text = "System settings",
                onClick = onOpenAppSettings,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small
            )

            AeonButton(
                text = "Exact alarms",
                onClick = onOpenExactAlarmSettings,
                variant = AeonButtonVariant.Ghost,
                size = AeonButtonSize.Small
            )
        }
    }
}


@Composable
private fun HealthLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
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
// Master
// ----------------------------------------------------

@Composable
private fun NotificationMasterCard(
    preferences: AeonNotificationPreferences,
    onUpdatePreferences: (AeonNotificationPreferences) -> Unit
) {
    AeonCard(
        variant = AeonCardVariant.Hero
    ) {
        SettingSwitchRow(
            title = "Enable Aeon notifications",
            subtitle = "Master control for all reminders, nudges, reviews, and system alerts.",
            checked = preferences.masterEnabled,
            onCheckedChange = { enabled ->
                onUpdatePreferences(
                    preferences.copy(
                        masterEnabled = enabled
                    )
                )
            }
        )
    }
}


// ----------------------------------------------------
// Categories
// ----------------------------------------------------

@Composable
private fun NotificationCategorySection(
    preferences: AeonNotificationPreferences,
    onUpdatePreferences: (AeonNotificationPreferences) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Categories",
        title = "Reminder types",
        subtitle = "Choose exactly which parts of Aeon can notify you.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Default
    ) {
        SettingSwitchRow(
            title = "Daily planning",
            subtitle = "Morning plan, daily reset, and next-best-action reminders.",
            checked = preferences.dailyPlanningEnabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = {
                onUpdatePreferences(preferences.copy(dailyPlanningEnabled = it))
            }
        )

        SettingsDivider()

        SettingSwitchRow(
            title = "Tasks",
            subtitle = "Due tasks, pending actions, and important task reminders.",
            checked = preferences.taskRemindersEnabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = {
                onUpdatePreferences(preferences.copy(taskRemindersEnabled = it))
            }
        )

        SettingsDivider()

        SettingSwitchRow(
            title = "Habits",
            subtitle = "Habit reminders, streak protection, and consistency nudges.",
            checked = preferences.habitRemindersEnabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = {
                onUpdatePreferences(preferences.copy(habitRemindersEnabled = it))
            }
        )

        SettingsDivider()

        SettingSwitchRow(
            title = "Focus",
            subtitle = "Focus start reminders, break reminders, and deep-work nudges.",
            checked = preferences.focusRemindersEnabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = {
                onUpdatePreferences(preferences.copy(focusRemindersEnabled = it))
            }
        )

        SettingsDivider()

        SettingSwitchRow(
            title = "Mood check-ins",
            subtitle = "Private emotional check-ins and reflection reminders.",
            checked = preferences.moodCheckInsEnabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = {
                onUpdatePreferences(preferences.copy(moodCheckInsEnabled = it))
            }
        )

        SettingsDivider()

        SettingSwitchRow(
            title = "Health",
            subtitle = "Wellness, medicine, hydration, sleep, and health reminders.",
            checked = preferences.healthRemindersEnabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = {
                onUpdatePreferences(preferences.copy(healthRemindersEnabled = it))
            }
        )

        SettingsDivider()

        SettingSwitchRow(
            title = "Finance",
            subtitle = "Budget checks, bill reminders, and spending awareness.",
            checked = preferences.financeRemindersEnabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = {
                onUpdatePreferences(preferences.copy(financeRemindersEnabled = it))
            }
        )

        SettingsDivider()

        SettingSwitchRow(
            title = "Goals",
            subtitle = "Milestones, reviews, and long-term progress reminders.",
            checked = preferences.goalRemindersEnabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = {
                onUpdatePreferences(preferences.copy(goalRemindersEnabled = it))
            }
        )

        SettingsDivider()

        SettingSwitchRow(
            title = "Aeon AI insights",
            subtitle = "Private AI suggestions, weekly review, and personal intelligence.",
            checked = preferences.aiInsightsEnabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = {
                onUpdatePreferences(preferences.copy(aiInsightsEnabled = it))
            }
        )

        SettingsDivider()

        SettingSwitchRow(
            title = "Backup alerts",
            subtitle = "Backup, restore, export, and data safety notifications.",
            checked = preferences.backupNotificationsEnabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = {
                onUpdatePreferences(preferences.copy(backupNotificationsEnabled = it))
            }
        )
    }
}


// ----------------------------------------------------
// Quiet Hours
// ----------------------------------------------------

@Composable
private fun NotificationQuietHoursSection(
    preferences: AeonNotificationPreferences,
    onUpdatePreferences: (AeonNotificationPreferences) -> Unit
) {
    val policy = preferences.quietHoursPolicy

    AeonSectionHeader(
        eyebrow = "Calm mode",
        title = "Quiet hours",
        subtitle = "Delay non-urgent notifications during your rest window.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Glass
    ) {
        SettingSwitchRow(
            title = "Use quiet hours",
            subtitle = "${policy.start.asUiTime()} – ${policy.end.asUiTime()}",
            checked = policy.enabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = { enabled ->
                onUpdatePreferences(
                    preferences.copy(
                        quietHoursPolicy = policy.copy(
                            enabled = enabled
                        )
                    )
                )
            }
        )

        SettingsDivider()

        SettingSwitchRow(
            title = "Allow urgent reminders",
            subtitle = "Urgent reminders can bypass quiet hours.",
            checked = policy.bypassForUrgent,
            enabled = preferences.masterEnabled && policy.enabled,
            onCheckedChange = { enabled ->
                onUpdatePreferences(
                    preferences.copy(
                        quietHoursPolicy = policy.copy(
                            bypassForUrgent = enabled
                        )
                    )
                )
            }
        )

        SettingsDivider()

        SettingSwitchRow(
            title = "Allow health reminders",
            subtitle = "Medicine and important health reminders can bypass quiet hours.",
            checked = policy.bypassForHealth,
            enabled = preferences.masterEnabled && policy.enabled,
            onCheckedChange = { enabled ->
                onUpdatePreferences(
                    preferences.copy(
                        quietHoursPolicy = policy.copy(
                            bypassForHealth = enabled
                        )
                    )
                )
            }
        )
    }
}


// ----------------------------------------------------
// Digest
// ----------------------------------------------------

@Composable
private fun NotificationDigestSection(
    preferences: AeonNotificationPreferences,
    onUpdatePreferences: (AeonNotificationPreferences) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Summary",
        title = "Daily digest",
        subtitle = "Bundle low-priority updates into one calmer notification.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Default
    ) {
        SettingSwitchRow(
            title = "Enable daily digest",
            subtitle = "Digest time: ${preferences.digestTime.asUiTime()}",
            checked = preferences.digestEnabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = { enabled ->
                onUpdatePreferences(
                    preferences.copy(
                        digestEnabled = enabled
                    )
                )
            }
        )
    }
}


// ----------------------------------------------------
// Daily Limit
// ----------------------------------------------------

@Composable
private fun NotificationDailyLimitSection(
    preferences: AeonNotificationPreferences,
    onUpdatePreferences: (AeonNotificationPreferences) -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Attention safety",
        title = "Daily notification limit",
        subtitle = "Protect focus by limiting how many notifications Aeon can send.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Elevated
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Maximum per day",
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Recommended: 6–10 notifications per day.",
                    style = AeonTextStyles.CardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = preferences.maxNotificationsPerDay.toString(),
                style = AeonTextStyles.HeroMetric,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
        ) {
            AeonButton(
                text = "Decrease",
                onClick = {
                    onUpdatePreferences(
                        preferences.copy(
                            maxNotificationsPerDay =
                                (preferences.maxNotificationsPerDay - 1).coerceIn(1, 30)
                        )
                    )
                },
                variant = AeonButtonVariant.Ghost,
                size = AeonButtonSize.Small,
                enabled = preferences.maxNotificationsPerDay > 1
            )

            AeonButton(
                text = "Increase",
                onClick = {
                    onUpdatePreferences(
                        preferences.copy(
                            maxNotificationsPerDay =
                                (preferences.maxNotificationsPerDay + 1).coerceIn(1, 30)
                        )
                    )
                },
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small,
                enabled = preferences.maxNotificationsPerDay < 30
            )
        }
    }
}


// ----------------------------------------------------
// Channel Health
// ----------------------------------------------------

@Composable
private fun NotificationChannelHealthSection(
    state: AeonNotificationUiState,
    onOpenChannelSettings: (AeonNotificationChannelKey) -> Unit
) {
    val snapshot = state.centerState?.permissionSnapshot

    val blocked = snapshot?.blockedChannels.orEmpty()
    val missing = snapshot?.missingChannels.orEmpty()

    AeonSectionHeader(
        eyebrow = "Android channels",
        title = "System categories",
        subtitle = "Android can block individual notification channels from system settings.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Default
    ) {
        if (blocked.isEmpty() && missing.isEmpty()) {
            Text(
                text = "All notification channels are active.",
                style = AeonTextStyles.CardTitle,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Aeon can use all required Android notification channels.",
                style = AeonTextStyles.CardSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            if (blocked.isNotEmpty()) {
                Text(
                    text = "Blocked channels",
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                blocked.forEach { channel ->
                    ChannelIssueRow(
                        channel = channel,
                        issue = "Blocked in Android settings",
                        onOpenChannelSettings = onOpenChannelSettings
                    )
                }
            }

            if (missing.isNotEmpty()) {
                Text(
                    text = "Missing channels",
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                missing.forEach { channel ->
                    ChannelIssueRow(
                        channel = channel,
                        issue = "Missing channel. Refresh the notification system.",
                        onOpenChannelSettings = onOpenChannelSettings
                    )
                }
            }
        }
    }
}


@Composable
private fun ChannelIssueRow(
    channel: AeonNotificationChannelKey,
    issue: String,
    onOpenChannelSettings: (AeonNotificationChannelKey) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AeonSpacing.XSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
        ) {
            Text(
                text = channel.title,
                style = AeonTextStyles.CardTitle,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = issue,
                style = AeonTextStyles.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AeonButton(
            text = "Open",
            onClick = {
                onOpenChannelSettings(channel)
            },
            variant = AeonButtonVariant.Ghost,
            size = AeonButtonSize.Small
        )
    }
}


// ----------------------------------------------------
// Shared Setting Row
// ----------------------------------------------------

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AeonSpacing.XSmall),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
        ) {
            Text(
                text = title,
                style = AeonTextStyles.CardTitle,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = subtitle,
                style = AeonTextStyles.CardSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}


@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = AeonSpacing.XSmall),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
}


// ----------------------------------------------------
// Time Helper
// ----------------------------------------------------

private fun LocalTime.asUiTime(): String {
    return format(
        DateTimeFormatter.ofPattern("h:mm a")
    )
}
