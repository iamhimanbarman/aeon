package com.aeon.app.ui.screens.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aeon.app.core.notifications.AeonNotificationActions
import com.aeon.app.core.notifications.AeonNotificationCenter
import com.aeon.app.core.notifications.AeonNotificationChannelKey
import com.aeon.app.core.notifications.AeonNotificationPayload
import com.aeon.app.core.notifications.AeonNotificationPriority
import com.aeon.app.core.notifications.AeonNotificationSchedule
import com.aeon.app.core.notifications.AeonNotificationSource
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
import java.time.LocalTime

/*
 * AEON NOTIFICATION TEST PANEL
 *
 * Purpose:
 * Developer-only screen to verify Aeon's notification engine.
 *
 * Tests:
 * - Runtime permission state
 * - Android channel health
 * - Immediate publish
 * - WorkManager one-time schedule
 * - Exact AlarmManager schedule
 * - Default rule scheduling
 * - Habit rule evaluation
 * - Mood check-in scheduling
 * - Weekly review scheduling
 *
 * Senior Developer Rule:
 * Do not expose this screen in production builds.
 * Add this route only for internal/debug builds.
 */


// ----------------------------------------------------
// Route
// ----------------------------------------------------

@Composable
fun AeonNotificationTestPanelRoute(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val notificationCenter = remember {
        AeonNotificationCenter.getInstance(context)
    }

    val records by notificationCenter
        .observeRecentNotifications(limit = 20)
        .collectAsState(initial = emptyList())

    var state by remember {
        mutableStateOf(AeonNotificationTestPanelState())
    }

    fun runAction(
        label: String,
        block: suspend () -> String
    ) {
        scope.launch {
            state = state.copy(
                working = true,
                message = "Running: $label",
                error = null
            )

            try {
                val result = block()

                state = state.copy(
                    working = false,
                    message = result,
                    error = null
                )
            } catch (throwable: Throwable) {
                state = state.copy(
                    working = false,
                    message = null,
                    error = throwable.message ?: "Notification test failed."
                )
            }
        }
    }

    AeonNotificationTestPanelScreen(
        state = state,
        recentCount = records.size,
        onBack = onBack,
        onOpenSettings = {
            notificationCenter.openAppNotificationSettings()
        },
        onOpenExactAlarmSettings = {
            notificationCenter.openExactAlarmSettings()
        },
        onInitialize = {
            runAction("Initialize") {
                val result = notificationCenter.initialize(
                    saveDefaultRulesIfMissing = true,
                    scheduleEnabledRules = false
                )

                "Initialized: ${result::class.simpleName}"
            }
        },
        onRefreshHealth = {
            runAction("Refresh health") {
                val health = notificationCenter.state()

                if (health.isHealthy) {
                    "Notification system is healthy."
                } else {
                    health.primaryIssue ?: "Notification system needs attention."
                }
            }
        },
        onPublishImmediate = {
            runAction("Immediate notification") {
                val payload = AeonNotificationPayload(
                    type = AeonNotificationType.SystemAlert,
                    channel = AeonNotificationChannelKey.System,
                    title = "Aeon notification test",
                    body = "Immediate notification delivery is working.",
                    source = AeonNotificationSource.System,
                    deepLinkRoute = "notification_inbox",
                    priority = AeonNotificationPriority.Normal,
                    actions = listOf(
                        AeonNotificationActions.open(
                            label = "Open",
                            route = "notification_inbox"
                        ),
                        AeonNotificationActions.dismiss()
                    )
                )

                val result = notificationCenter.publish(payload)

                "Immediate result: ${result::class.simpleName}"
            }
        },
        onScheduleWorkManager = {
            runAction("WorkManager schedule") {
                val triggerAt = System.currentTimeMillis() + 60_000L

                val payload = AeonNotificationPayload(
                    type = AeonNotificationType.TaskReminder,
                    channel = AeonNotificationChannelKey.Tasks,
                    title = "WorkManager reminder",
                    body = "This reminder was scheduled with WorkManager.",
                    source = AeonNotificationSource.Task,
                    sourceId = "test_task",
                    deepLinkRoute = "today",
                    actions = listOf(
                        AeonNotificationActions.open(
                            label = "Open",
                            route = "today"
                        ),
                        AeonNotificationActions.snooze(
                            minutes = 10
                        )
                    )
                )

                val result = notificationCenter.schedule(
                    payload = payload,
                    schedule = AeonNotificationSchedule.OneTime(
                        triggerAtEpochMillis = triggerAt,
                        exact = false
                    ),
                    ruleId = "test_workmanager_one_time"
                )

                "Scheduled with WorkManager: ${result::class.simpleName}"
            }
        },
        onScheduleExactAlarm = {
            runAction("Exact alarm schedule") {
                val triggerAt = System.currentTimeMillis() + 60_000L

                val payload = AeonNotificationPayload(
                    type = AeonNotificationType.FocusSession,
                    channel = AeonNotificationChannelKey.Focus,
                    title = "Exact focus reminder",
                    body = "This reminder requested exact alarm delivery.",
                    source = AeonNotificationSource.Focus,
                    sourceId = "test_focus",
                    deepLinkRoute = "focus",
                    priority = AeonNotificationPriority.High,
                    actions = listOf(
                        AeonNotificationActions.startFocus()
                    )
                )

                val result = notificationCenter.schedule(
                    payload = payload,
                    schedule = AeonNotificationSchedule.OneTime(
                        triggerAtEpochMillis = triggerAt,
                        exact = true
                    ),
                    ruleId = "test_exact_alarm"
                )

                "Exact alarm result: ${result::class.simpleName}"
            }
        },
        onScheduleDefaultRules = {
            runAction("Default rules") {
                val results = notificationCenter.scheduleDefaultRules()

                "Default rules processed: ${results.size}"
            }
        },
        onScheduleHabitRule = {
            runAction("Habit rule") {
                val result = notificationCenter.scheduleHabitReminder(
                    habitId = "habit_reading_test",
                    habitName = "Read for 20 minutes",
                    completed = false
                )

                "Habit rule result: ${result::class.simpleName}"
            }
        },
        onScheduleMoodRule = {
            runAction("Mood rule") {
                val result = notificationCenter.scheduleMoodCheckIn()

                "Mood check-in result: ${result::class.simpleName}"
            }
        },
        onScheduleWeeklyReview = {
            runAction("Weekly review") {
                val result = notificationCenter.scheduleWeeklyReview()

                "Weekly review result: ${result::class.simpleName}"
            }
        },
        onClearHistory = {
            runAction("Clear history") {
                notificationCenter.clearHistory()
                "Notification history cleared."
            }
        },
        modifier = modifier
    )
}


// ----------------------------------------------------
// State
// ----------------------------------------------------

@Immutable
data class AeonNotificationTestPanelState(
    val working: Boolean = false,
    val message: String? = null,
    val error: String? = null
)


// ----------------------------------------------------
// Screen
// ----------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AeonNotificationTestPanelScreen(
    state: AeonNotificationTestPanelState,
    recentCount: Int,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onInitialize: () -> Unit,
    onRefreshHealth: () -> Unit,
    onPublishImmediate: () -> Unit,
    onScheduleWorkManager: () -> Unit,
    onScheduleExactAlarm: () -> Unit,
    onScheduleDefaultRules: () -> Unit,
    onScheduleHabitRule: () -> Unit,
    onScheduleMoodRule: () -> Unit,
    onScheduleWeeklyReview: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    AeonScreen(
        modifier = modifier
    ) {
        AeonSectionHeader(
            eyebrow = "Developer tools",
            title = "Notification test panel",
            subtitle = "Verify channels, permissions, immediate delivery, scheduled reminders, rules, and notification history.",
            size = AeonSectionHeaderSize.Hero,
            tone = AeonSectionHeaderTone.Brand,
            action = {
                AeonChip(
                    text = "$recentCount records",
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
                text = "Settings",
                onClick = onOpenSettings,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small
            )
        }

        AnimatedVisibility(
            visible = state.working || !state.message.isNullOrBlank() || !state.error.isNullOrBlank()
        ) {
            AeonCard(
                variant = if (state.error == null) {
                    AeonCardVariant.Glass
                } else {
                    AeonCardVariant.Insight
                }
            ) {
                Text(
                    text = when {
                        state.working -> "Working..."
                        !state.error.isNullOrBlank() -> state.error
                        else -> state.message.orEmpty()
                    },
                    style = AeonTextStyles.CardSubtitle,
                    color = if (state.error == null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }

        AeonNotificationTestSection(
            title = "System health",
            subtitle = "Use these first before testing delivery."
        ) {
            AeonButton(
                text = "Initialize",
                onClick = onInitialize,
                variant = AeonButtonVariant.Primary,
                size = AeonButtonSize.Medium,
                enabled = !state.working
            )

            AeonButton(
                text = "Refresh health",
                onClick = onRefreshHealth,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Medium,
                enabled = !state.working
            )

            AeonButton(
                text = "Exact alarm settings",
                onClick = onOpenExactAlarmSettings,
                variant = AeonButtonVariant.Ghost,
                size = AeonButtonSize.Medium,
                enabled = !state.working
            )
        }

        AeonNotificationTestSection(
            title = "Delivery tests",
            subtitle = "Test actual notification delivery paths."
        ) {
            AeonButton(
                text = "Publish now",
                onClick = onPublishImmediate,
                variant = AeonButtonVariant.Primary,
                size = AeonButtonSize.Medium,
                enabled = !state.working
            )

            AeonButton(
                text = "Schedule 1 min",
                onClick = onScheduleWorkManager,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Medium,
                enabled = !state.working
            )

            AeonButton(
                text = "Exact 1 min",
                onClick = onScheduleExactAlarm,
                variant = AeonButtonVariant.Premium,
                size = AeonButtonSize.Medium,
                enabled = !state.working
            )
        }

        AeonNotificationTestSection(
            title = "Rule engine tests",
            subtitle = "Test Aeon's rule evaluation layer."
        ) {
            AeonButton(
                text = "Default rules",
                onClick = onScheduleDefaultRules,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Medium,
                enabled = !state.working
            )

            AeonButton(
                text = "Habit rule",
                onClick = onScheduleHabitRule,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Medium,
                enabled = !state.working
            )

            AeonButton(
                text = "Mood rule",
                onClick = onScheduleMoodRule,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Medium,
                enabled = !state.working
            )

            AeonButton(
                text = "Weekly review",
                onClick = onScheduleWeeklyReview,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Medium,
                enabled = !state.working
            )
        }

        AeonNotificationTestSection(
            title = "Maintenance",
            subtitle = "Reset local notification history when testing."
        ) {
            AeonButton(
                text = "Clear history",
                onClick = onClearHistory,
                variant = AeonButtonVariant.Danger,
                size = AeonButtonSize.Medium,
                enabled = !state.working
            )
        }
    }
}


// ----------------------------------------------------
// Section
// ----------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AeonNotificationTestSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    AeonCard(
        variant = AeonCardVariant.Default
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.XXSmall)
        ) {
            Text(
                text = title,
                style = AeonTextStyles.CardTitle,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = AeonTextStyles.CardSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
        ) {
            content()
        }
    }
}
