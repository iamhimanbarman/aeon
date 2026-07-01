package com.aeon.app.ui.screens.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.aeon.app.core.notifications.AeonNotificationChannelKey
import com.aeon.app.core.notifications.AeonNotificationPreferences
import com.aeon.app.core.notifications.AeonQuietHoursPolicy
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
 * NOTIFICATION PREFERENCE SCREEN
 *
 * Purpose:
 * Detailed preference screen for one notification category.
 *
 * Handles:
 * - Category enable / disable
 * - Android channel state
 * - Quiet-hour behavior
 * - Digest preference
 * - Daily limit safety
 * - System channel settings shortcut
 *
 * Senior Developer Rule:
 * This screen talks only to AeonNotificationCenter.
 * It does not directly access Scheduler, Publisher, Repository, Worker, or DAO.
 */


// ----------------------------------------------------
// Route
// ----------------------------------------------------

@Composable
fun NotificationPreferenceRoute(
    channelKey: AeonNotificationChannelKey,
    viewModel: AeonNotificationViewModel = viewModel(),
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationPreferenceScreen(
        channelKey = channelKey,
        state = state,
        onBack = onBack,
        onOpenSystemChannelSettings = {
            viewModel.openChannelSettings(channelKey)
        },
        onOpenAppNotificationSettings = {
            viewModel.openAppNotificationSettings()
        },
        onRefresh = {
            viewModel.refresh()
        },
        onUpdatePreferences = { updated ->
            viewModel.updatePreferences(updated)
        },
        modifier = modifier
    )
}


// ----------------------------------------------------
// Screen
// ----------------------------------------------------

@Composable
fun NotificationPreferenceScreen(
    channelKey: AeonNotificationChannelKey,
    state: AeonNotificationUiState,
    onBack: () -> Unit,
    onOpenSystemChannelSettings: () -> Unit,
    onOpenAppNotificationSettings: () -> Unit,
    onRefresh: () -> Unit,
    onUpdatePreferences: (AeonNotificationPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    AeonScreen(
        modifier = modifier
    ) {
        NotificationPreferenceHeader(
            channelKey = channelKey,
            state = state,
            onBack = onBack,
            onRefresh = onRefresh
        )

        NotificationPreferenceHealthCard(
            channelKey = channelKey,
            state = state,
            onOpenSystemChannelSettings = onOpenSystemChannelSettings,
            onOpenAppNotificationSettings = onOpenAppNotificationSettings
        )

        NotificationPreferenceMasterCard(
            channelKey = channelKey,
            state = state,
            onUpdatePreferences = onUpdatePreferences
        )

        NotificationPreferenceBehaviorCard(
            state = state,
            onUpdatePreferences = onUpdatePreferences
        )

        NotificationPreferenceQuietHoursCard(
            state = state,
            onUpdatePreferences = onUpdatePreferences
        )

        NotificationPreferenceDailyLimitCard(
            state = state,
            onUpdatePreferences = onUpdatePreferences
        )

        NotificationPreferenceSystemCard(
            channelKey = channelKey,
            onOpenSystemChannelSettings = onOpenSystemChannelSettings
        )
    }
}


// ----------------------------------------------------
// Header
// ----------------------------------------------------

@Composable
private fun NotificationPreferenceHeader(
    channelKey: AeonNotificationChannelKey,
    state: AeonNotificationUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Notification preference",
        title = channelKey.title,
        subtitle = channelKey.description,
        size = AeonSectionHeaderSize.Hero,
        tone = AeonSectionHeaderTone.Brand,
        action = {
            AeonChip(
                text = if (state.preferences.isCategoryEnabled(channelKey)) {
                    "Enabled"
                } else {
                    "Off"
                },
                variant = if (state.preferences.isCategoryEnabled(channelKey)) {
                    AeonChipVariant.Success
                } else {
                    AeonChipVariant.Warning
                },
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
            text = "Refresh",
            onClick = onRefresh,
            variant = AeonButtonVariant.Secondary,
            size = AeonButtonSize.Small
        )
    }

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
}


// ----------------------------------------------------
// Health Card
// ----------------------------------------------------

@Composable
private fun NotificationPreferenceHealthCard(
    channelKey: AeonNotificationChannelKey,
    state: AeonNotificationUiState,
    onOpenSystemChannelSettings: () -> Unit,
    onOpenAppNotificationSettings: () -> Unit
) {
    val snapshot = state.centerState?.permissionSnapshot
    val blocked = snapshot?.blockedChannels?.contains(channelKey) == true
    val missing = snapshot?.missingChannels?.contains(channelKey) == true

    AeonCard(
        variant = if (!blocked && !missing) {
            AeonCardVariant.Elevated
        } else {
            AeonCardVariant.Insight
        }
    ) {
        Text(
            text = when {
                blocked -> "This category is blocked by Android"
                missing -> "This category channel is missing"
                else -> "This category is ready"
            },
            style = AeonTextStyles.CardTitle,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = when {
                blocked -> "Open Android channel settings and allow this notification category."
                missing -> "Refresh Aeon notification system to recreate this Android channel."
                else -> "Aeon can use this notification category when app notifications are allowed."
            },
            style = AeonTextStyles.CardSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PreferenceInfoLine(
            label = "Runtime permission",
            value = snapshot?.runtimePermissionStatus?.name ?: "Checking"
        )

        PreferenceInfoLine(
            label = "App notification state",
            value = snapshot?.appNotificationState?.name ?: "Checking"
        )

        PreferenceInfoLine(
            label = "Channel status",
            value = when {
                blocked -> "Blocked"
                missing -> "Missing"
                else -> "Available"
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
        ) {
            AeonButton(
                text = "Channel settings",
                onClick = onOpenSystemChannelSettings,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small
            )

            AeonButton(
                text = "App settings",
                onClick = onOpenAppNotificationSettings,
                variant = AeonButtonVariant.Ghost,
                size = AeonButtonSize.Small
            )
        }
    }
}


// ----------------------------------------------------
// Category Master
// ----------------------------------------------------

@Composable
private fun NotificationPreferenceMasterCard(
    channelKey: AeonNotificationChannelKey,
    state: AeonNotificationUiState,
    onUpdatePreferences: (AeonNotificationPreferences) -> Unit
) {
    val preferences = state.preferences
    val enabled = preferences.isCategoryEnabled(channelKey)

    AeonSectionHeader(
        eyebrow = "Category control",
        title = "Allow ${channelKey.title}",
        subtitle = "Turn this Aeon notification category on or off.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Hero
    ) {
        PreferenceSwitchRow(
            title = "Enable ${channelKey.title}",
            subtitle = "Aeon can send notifications from this category.",
            checked = enabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = { checked ->
                onUpdatePreferences(
                    preferences.withCategoryEnabled(
                        channel = channelKey,
                        enabled = checked
                    )
                )
            }
        )

        if (!preferences.masterEnabled) {
            Text(
                text = "Master notifications are disabled. Enable them from the main notification settings screen.",
                style = AeonTextStyles.Caption,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}


// ----------------------------------------------------
// Behavior
// ----------------------------------------------------

@Composable
private fun NotificationPreferenceBehaviorCard(
    state: AeonNotificationUiState,
    onUpdatePreferences: (AeonNotificationPreferences) -> Unit
) {
    val preferences = state.preferences

    AeonSectionHeader(
        eyebrow = "Behavior",
        title = "Delivery behavior",
        subtitle = "Control how Aeon groups and protects your attention.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Default
    ) {
        PreferenceSwitchRow(
            title = "Use daily digest",
            subtitle = "Low-priority updates can be bundled into one calmer notification.",
            checked = preferences.digestEnabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = { checked ->
                onUpdatePreferences(
                    preferences.copy(
                        digestEnabled = checked
                    )
                )
            }
        )

        PreferenceDivider()

        Text(
            text = "Digest time",
            style = AeonTextStyles.CardTitle,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = preferences.digestTime.asPreferenceTime(),
            style = AeonTextStyles.CardSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TimePresetRow(
            selectedTime = preferences.digestTime,
            presets = listOf(
                LocalTime.of(18, 0),
                LocalTime.of(20, 30),
                LocalTime.of(22, 0)
            ),
            enabled = preferences.masterEnabled && preferences.digestEnabled,
            onSelected = { time ->
                onUpdatePreferences(
                    preferences.copy(
                        digestTime = time
                    )
                )
            }
        )
    }
}


// ----------------------------------------------------
// Quiet Hours
// ----------------------------------------------------

@Composable
private fun NotificationPreferenceQuietHoursCard(
    state: AeonNotificationUiState,
    onUpdatePreferences: (AeonNotificationPreferences) -> Unit
) {
    val preferences = state.preferences
    val policy = preferences.quietHoursPolicy

    AeonSectionHeader(
        eyebrow = "Attention safety",
        title = "Quiet hours",
        subtitle = "Delay non-urgent reminders during your rest window.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Glass
    ) {
        PreferenceSwitchRow(
            title = "Respect quiet hours",
            subtitle = "${policy.start.asPreferenceTime()} – ${policy.end.asPreferenceTime()}",
            checked = policy.enabled,
            enabled = preferences.masterEnabled,
            onCheckedChange = { checked ->
                onUpdatePreferences(
                    preferences.copy(
                        quietHoursPolicy = policy.copy(
                            enabled = checked
                        )
                    )
                )
            }
        )

        PreferenceDivider()

        Text(
            text = "Start time",
            style = AeonTextStyles.CardTitle,
            color = MaterialTheme.colorScheme.onSurface
        )

        TimePresetRow(
            selectedTime = policy.start,
            presets = listOf(
                LocalTime.of(21, 0),
                LocalTime.of(22, 0),
                LocalTime.of(22, 30),
                LocalTime.of(23, 0)
            ),
            enabled = preferences.masterEnabled && policy.enabled,
            onSelected = { time ->
                onUpdatePreferences(
                    preferences.copy(
                        quietHoursPolicy = policy.copy(start = time)
                    )
                )
            }
        )

        PreferenceDivider()

        Text(
            text = "End time",
            style = AeonTextStyles.CardTitle,
            color = MaterialTheme.colorScheme.onSurface
        )

        TimePresetRow(
            selectedTime = policy.end,
            presets = listOf(
                LocalTime.of(6, 0),
                LocalTime.of(7, 0),
                LocalTime.of(8, 0),
                LocalTime.of(9, 0)
            ),
            enabled = preferences.masterEnabled && policy.enabled,
            onSelected = { time ->
                onUpdatePreferences(
                    preferences.copy(
                        quietHoursPolicy = policy.copy(end = time)
                    )
                )
            }
        )

        PreferenceDivider()

        PreferenceSwitchRow(
            title = "Urgent bypass",
            subtitle = "Urgent reminders can bypass quiet hours.",
            checked = policy.bypassForUrgent,
            enabled = preferences.masterEnabled && policy.enabled,
            onCheckedChange = { checked ->
                onUpdatePreferences(
                    preferences.copy(
                        quietHoursPolicy = policy.copy(
                            bypassForUrgent = checked
                        )
                    )
                )
            }
        )

        PreferenceDivider()

        PreferenceSwitchRow(
            title = "Health bypass",
            subtitle = "Important medicine or health reminders can bypass quiet hours.",
            checked = policy.bypassForHealth,
            enabled = preferences.masterEnabled && policy.enabled,
            onCheckedChange = { checked ->
                onUpdatePreferences(
                    preferences.copy(
                        quietHoursPolicy = policy.copy(
                            bypassForHealth = checked
                        )
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
private fun NotificationPreferenceDailyLimitCard(
    state: AeonNotificationUiState,
    onUpdatePreferences: (AeonNotificationPreferences) -> Unit
) {
    val preferences = state.preferences

    AeonSectionHeader(
        eyebrow = "Limit",
        title = "Daily notification limit",
        subtitle = "Protect your focus by limiting total notifications per day.",
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
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
            ) {
                Text(
                    text = "Maximum notifications",
                    style = AeonTextStyles.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Recommended range: 6–10 per day.",
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

        FlowControlRow(
            enabled = preferences.masterEnabled,
            onLow = {
                onUpdatePreferences(
                    preferences.copy(maxNotificationsPerDay = 4)
                )
            },
            onBalanced = {
                onUpdatePreferences(
                    preferences.copy(maxNotificationsPerDay = 8)
                )
            },
            onHigh = {
                onUpdatePreferences(
                    preferences.copy(maxNotificationsPerDay = 14)
                )
            }
        )

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
                enabled = preferences.masterEnabled && preferences.maxNotificationsPerDay > 1
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
                enabled = preferences.masterEnabled && preferences.maxNotificationsPerDay < 30
            )
        }
    }
}


// ----------------------------------------------------
// System Card
// ----------------------------------------------------

@Composable
private fun NotificationPreferenceSystemCard(
    channelKey: AeonNotificationChannelKey,
    onOpenSystemChannelSettings: () -> Unit
) {
    AeonSectionHeader(
        eyebrow = "Android system",
        title = "Channel control",
        subtitle = "Android controls final sound, vibration, and category-level blocking.",
        size = AeonSectionHeaderSize.Medium
    )

    AeonCard(
        variant = AeonCardVariant.Default
    ) {
        PreferenceInfoLine(
            label = "Channel name",
            value = channelKey.title
        )

        PreferenceInfoLine(
            label = "Channel ID",
            value = channelKey.channelId
        )

        PreferenceInfoLine(
            label = "Description",
            value = channelKey.description
        )

        AeonButton(
            text = "Open Android channel settings",
            onClick = onOpenSystemChannelSettings,
            variant = AeonButtonVariant.Secondary,
            size = AeonButtonSize.Medium,
            fullWidth = true
        )
    }
}


// ----------------------------------------------------
// Shared UI
// ----------------------------------------------------

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
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
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}


@Composable
private fun PreferenceInfoLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AeonSpacing.XXSmall),
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


@Composable
private fun PreferenceDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = AeonSpacing.XSmall),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimePresetRow(
    selectedTime: LocalTime,
    presets: List<LocalTime>,
    enabled: Boolean,
    onSelected: (LocalTime) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
    ) {
        presets.forEach { time ->
            AeonChip(
                text = time.asPreferenceTime(),
                variant = if (time == selectedTime) {
                    AeonChipVariant.Filled
                } else {
                    AeonChipVariant.Outline
                },
                size = AeonChipSize.Medium,
                enabled = enabled,
                onClick = {
                    onSelected(time)
                }
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowControlRow(
    enabled: Boolean,
    onLow: () -> Unit,
    onBalanced: () -> Unit,
    onHigh: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
    ) {
        AeonChip(
            text = "Low",
            variant = AeonChipVariant.Outline,
            size = AeonChipSize.Medium,
            enabled = enabled,
            onClick = onLow
        )

        AeonChip(
            text = "Balanced",
            variant = AeonChipVariant.Filled,
            size = AeonChipSize.Medium,
            enabled = enabled,
            onClick = onBalanced
        )

        AeonChip(
            text = "High",
            variant = AeonChipVariant.Outline,
            size = AeonChipSize.Medium,
            enabled = enabled,
            onClick = onHigh
        )
    }
}


private fun LocalTime.asPreferenceTime(): String {
    return format(DateTimeFormatter.ofPattern("h:mm a"))
}
