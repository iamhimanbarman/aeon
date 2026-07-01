package com.aeon.app.core.notifications

import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import java.util.zip.CRC32

/*
 * AEON NOTIFICATION CONTRACT
 *
 * Purpose:
 * Core notification domain model for Aeon.
 *
 * This file is pure Kotlin.
 * It should not depend on Compose, Android UI, Room, WorkManager, or NotificationCompat.
 *
 * System responsibilities:
 * - Notification types
 * - Channels
 * - Priority
 * - Delivery modes
 * - Schedule contracts
 * - Quiet hours
 * - Payload model
 * - Rule model
 * - Notification record/history model
 * - Stable notification ID generation
 */


// ----------------------------------------------------
// Notification Type
// ----------------------------------------------------

enum class AeonNotificationType {
    DailyPlan,
    TaskReminder,
    HabitReminder,
    FocusSession,
    BreakReminder,
    MoodCheckIn,
    JournalReminder,
    HealthReminder,
    FinanceReminder,
    GoalReminder,
    WeeklyReview,
    AIInsight,
    DataBackup,
    SystemAlert
}


// ----------------------------------------------------
// Notification Channel
// ----------------------------------------------------

enum class AeonNotificationChannelKey(
    val channelId: String,
    val title: String,
    val description: String
) {
    DailyPlanning(
        channelId = "aeon_daily_planning",
        title = "Daily Planning",
        description = "Morning plans, daily reset, and personal planning reminders."
    ),

    Tasks(
        channelId = "aeon_tasks",
        title = "Tasks",
        description = "Task reminders and pending action alerts."
    ),

    Habits(
        channelId = "aeon_habits",
        title = "Habits",
        description = "Habit reminders, streak nudges, and consistency alerts."
    ),

    Focus(
        channelId = "aeon_focus",
        title = "Focus",
        description = "Focus sessions, breaks, and distraction control reminders."
    ),

    Mood(
        channelId = "aeon_mood",
        title = "Mood",
        description = "Mood check-ins and emotional reflection reminders."
    ),

    Health(
        channelId = "aeon_health",
        title = "Health",
        description = "Health, sleep, medicine, hydration, and wellness reminders."
    ),

    Finance(
        channelId = "aeon_finance",
        title = "Finance",
        description = "Budget, expense, bill, and spending awareness alerts."
    ),

    Goals(
        channelId = "aeon_goals",
        title = "Goals",
        description = "Goal milestones, reviews, and progress reminders."
    ),

    AIInsights(
        channelId = "aeon_ai_insights",
        title = "Aeon AI Insights",
        description = "Private AI insights, daily suggestions, and personal reviews."
    ),

    Backup(
        channelId = "aeon_backup",
        title = "Backup",
        description = "Data backup, restore, and export status notifications."
    ),

    System(
        channelId = "aeon_system",
        title = "System",
        description = "Important Aeon system messages."
    )
}


// ----------------------------------------------------
// Importance / Priority
// ----------------------------------------------------

enum class AeonNotificationImportance {
    Silent,
    Low,
    Default,
    High
}


enum class AeonNotificationPriority {
    Low,
    Normal,
    High,
    Urgent
}


// ----------------------------------------------------
// Delivery Mode
// ----------------------------------------------------

enum class AeonNotificationDeliveryMode {
    Silent,
    Standard,
    TimeSensitive,
    DigestOnly
}


// ----------------------------------------------------
// Notification Status
// ----------------------------------------------------

enum class AeonNotificationStatus {
    Draft,
    Pending,
    Scheduled,
    Delivered,
    Tapped,
    Dismissed,
    Cancelled,
    Suppressed,
    Failed
}


// ----------------------------------------------------
// Trigger Source
// ----------------------------------------------------

enum class AeonNotificationSource {
    Task,
    Habit,
    Focus,
    Mood,
    Journal,
    Health,
    Finance,
    Goal,
    Insight,
    AI,
    Backup,
    System,
    Manual
}


// ----------------------------------------------------
// Notification Action
// ----------------------------------------------------

data class AeonNotificationAction(
    val id: String,
    val label: String,
    val route: String? = null,
    val destructive: Boolean = false
) {
    init {
        require(id.isNotBlank()) {
            "Notification action id cannot be blank."
        }

        require(label.isNotBlank()) {
            "Notification action label cannot be blank."
        }
    }
}


// ----------------------------------------------------
// Notification Payload
// ----------------------------------------------------

data class AeonNotificationPayload(
    val id: String = aeonNewNotificationId(),
    val type: AeonNotificationType,
    val channel: AeonNotificationChannelKey,
    val title: String,
    val body: String,
    val source: AeonNotificationSource = AeonNotificationSource.System,
    val sourceId: String? = null,
    val deepLinkRoute: String? = null,
    val groupKey: String? = null,
    val priority: AeonNotificationPriority = AeonNotificationPriority.Normal,
    val importance: AeonNotificationImportance = AeonNotificationImportance.Default,
    val deliveryMode: AeonNotificationDeliveryMode = AeonNotificationDeliveryMode.Standard,
    val actions: List<AeonNotificationAction> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    val stableNotificationId: Int
        get() = aeonStableNotificationId(id)

    init {
        require(title.isNotBlank()) {
            "Notification title cannot be blank."
        }

        require(body.isNotBlank()) {
            "Notification body cannot be blank."
        }

        require(actions.size <= 3) {
            "Android notification actions should be limited to 3 for clean UX."
        }
    }
}


// ----------------------------------------------------
// Schedule Contract
// ----------------------------------------------------

sealed interface AeonNotificationSchedule {
    val scheduleId: String
    val requiresExactAlarm: Boolean

    data object Immediate : AeonNotificationSchedule {
        override val scheduleId: String = "immediate"
        override val requiresExactAlarm: Boolean = false
    }

    data class OneTime(
        val triggerAtEpochMillis: Long,
        val exact: Boolean = false
    ) : AeonNotificationSchedule {
        override val scheduleId: String = "one_time_$triggerAtEpochMillis"
        override val requiresExactAlarm: Boolean = exact

        init {
            require(triggerAtEpochMillis > 0L) {
                "One-time trigger time must be valid."
            }
        }
    }

    data class RepeatingInterval(
        val startAtEpochMillis: Long,
        val repeatIntervalMinutes: Long,
        val flexMinutes: Long = 15L
    ) : AeonNotificationSchedule {
        override val scheduleId: String =
            "repeating_${startAtEpochMillis}_${repeatIntervalMinutes}"

        override val requiresExactAlarm: Boolean = false

        init {
            require(startAtEpochMillis > 0L) {
                "Repeating schedule start time must be valid."
            }

            require(repeatIntervalMinutes >= 15L) {
                "Repeating interval should be at least 15 minutes."
            }

            require(flexMinutes >= 0L) {
                "Flex minutes cannot be negative."
            }
        }
    }

    data class Daily(
        val localTime: LocalTime,
        val exact: Boolean = false
    ) : AeonNotificationSchedule {
        override val scheduleId: String = "daily_${localTime.hour}_${localTime.minute}"
        override val requiresExactAlarm: Boolean = exact
    }

    data class Weekly(
        val days: Set<DayOfWeek>,
        val localTime: LocalTime,
        val exact: Boolean = false
    ) : AeonNotificationSchedule {
        override val scheduleId: String =
            "weekly_${days.joinToString("_")}_${localTime.hour}_${localTime.minute}"

        override val requiresExactAlarm: Boolean = exact

        init {
            require(days.isNotEmpty()) {
                "Weekly schedule must contain at least one day."
            }
        }
    }
}


// ----------------------------------------------------
// Quiet Hours
// ----------------------------------------------------

data class AeonQuietHoursPolicy(
    val enabled: Boolean = true,
    val start: LocalTime = LocalTime.of(22, 30),
    val end: LocalTime = LocalTime.of(7, 0),
    val bypassForUrgent: Boolean = true,
    val bypassForHealth: Boolean = true
) {
    fun isQuietAt(time: LocalTime): Boolean {
        if (!enabled) return false

        val sameDayWindow = !start.isAfter(end)

        return if (sameDayWindow) {
            !time.isBefore(start) && time.isBefore(end)
        } else {
            !time.isBefore(start) || time.isBefore(end)
        }
    }

    fun canBypass(
        priority: AeonNotificationPriority,
        type: AeonNotificationType
    ): Boolean {
        if (!enabled) return true

        if (bypassForUrgent && priority == AeonNotificationPriority.Urgent) {
            return true
        }

        if (
            bypassForHealth &&
            type == AeonNotificationType.HealthReminder
        ) {
            return true
        }

        return false
    }
}


// ----------------------------------------------------
// Notification Template
// ----------------------------------------------------

data class AeonNotificationTemplate(
    val titleTemplate: String,
    val bodyTemplate: String,
    val deepLinkRouteTemplate: String? = null
) {
    init {
        require(titleTemplate.isNotBlank()) {
            "Notification title template cannot be blank."
        }

        require(bodyTemplate.isNotBlank()) {
            "Notification body template cannot be blank."
        }
    }

    fun render(
        values: Map<String, String> = emptyMap()
    ): AeonRenderedNotificationTemplate {
        return AeonRenderedNotificationTemplate(
            title = titleTemplate.applyAeonTemplate(values),
            body = bodyTemplate.applyAeonTemplate(values),
            deepLinkRoute = deepLinkRouteTemplate?.applyAeonTemplate(values)
        )
    }
}


data class AeonRenderedNotificationTemplate(
    val title: String,
    val body: String,
    val deepLinkRoute: String?
)


// ----------------------------------------------------
// Notification Rule
// ----------------------------------------------------

data class AeonNotificationRule(
    val id: String,
    val name: String,
    val type: AeonNotificationType,
    val channel: AeonNotificationChannelKey,
    val source: AeonNotificationSource,
    val schedule: AeonNotificationSchedule,
    val template: AeonNotificationTemplate,
    val enabled: Boolean = true,
    val priority: AeonNotificationPriority = AeonNotificationPriority.Normal,
    val importance: AeonNotificationImportance = AeonNotificationImportance.Default,
    val deliveryMode: AeonNotificationDeliveryMode = AeonNotificationDeliveryMode.Standard,
    val groupKey: String? = null,
    val quietHoursPolicy: AeonQuietHoursPolicy = AeonQuietHoursPolicy(),
    val conditions: List<AeonNotificationCondition> = emptyList(),
    val actions: List<AeonNotificationAction> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) {
            "Notification rule id cannot be blank."
        }

        require(name.isNotBlank()) {
            "Notification rule name cannot be blank."
        }
    }

    fun buildPayload(
        values: Map<String, String> = emptyMap(),
        sourceId: String? = null
    ): AeonNotificationPayload {
        val rendered = template.render(values)

        return AeonNotificationPayload(
            type = type,
            channel = channel,
            title = rendered.title,
            body = rendered.body,
            source = source,
            sourceId = sourceId,
            deepLinkRoute = rendered.deepLinkRoute,
            groupKey = groupKey,
            priority = priority,
            importance = importance,
            deliveryMode = deliveryMode,
            actions = actions,
            metadata = metadata + values
        )
    }
}


// ----------------------------------------------------
// Rule Conditions
// ----------------------------------------------------

sealed interface AeonNotificationCondition {
    val key: String

    data object OnlyIfIncomplete : AeonNotificationCondition {
        override val key: String = "only_if_incomplete"
    }

    data object OnlyIfNotDeliveredToday : AeonNotificationCondition {
        override val key: String = "only_if_not_delivered_today"
    }

    data object OnlyIfUserEnabledCategory : AeonNotificationCondition {
        override val key: String = "only_if_user_enabled_category"
    }

    data class MinMinutesSinceLastShown(
        val minutes: Long
    ) : AeonNotificationCondition {
        override val key: String = "min_minutes_since_last_shown"

        init {
            require(minutes > 0L) {
                "Minimum minutes since last shown must be greater than 0."
            }
        }
    }

    data class MaxPerDay(
        val count: Int
    ) : AeonNotificationCondition {
        override val key: String = "max_per_day"

        init {
            require(count > 0) {
                "Max per day must be greater than 0."
            }
        }
    }
}


// ----------------------------------------------------
// Rule Engine Decision
// ----------------------------------------------------

sealed interface AeonNotificationDecision {
    data class Deliver(
        val payload: AeonNotificationPayload,
        val schedule: AeonNotificationSchedule,
        val reason: String
    ) : AeonNotificationDecision

    data class Delay(
        val payload: AeonNotificationPayload,
        val newTriggerAtEpochMillis: Long,
        val reason: String
    ) : AeonNotificationDecision

    data class Skip(
        val reason: String
    ) : AeonNotificationDecision

    data class Suppress(
        val reason: String
    ) : AeonNotificationDecision
}


// ----------------------------------------------------
// Notification Record / History
// ----------------------------------------------------

data class AeonNotificationRecord(
    val id: String = aeonNewNotificationId(prefix = "record"),
    val payloadId: String,
    val notificationId: Int,
    val ruleId: String? = null,
    val type: AeonNotificationType,
    val channelId: String,
    val title: String,
    val body: String,
    val deepLinkRoute: String? = null,
    val source: AeonNotificationSource,
    val sourceId: String? = null,
    val status: AeonNotificationStatus,
    val scheduledAtEpochMillis: Long? = null,
    val deliveredAtEpochMillis: Long? = null,
    val tappedAtEpochMillis: Long? = null,
    val dismissedAtEpochMillis: Long? = null,
    val failureReason: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    val isDelivered: Boolean
        get() = status == AeonNotificationStatus.Delivered ||
            status == AeonNotificationStatus.Tapped ||
            status == AeonNotificationStatus.Dismissed

    val isFinal: Boolean
        get() = status == AeonNotificationStatus.Tapped ||
            status == AeonNotificationStatus.Dismissed ||
            status == AeonNotificationStatus.Cancelled ||
            status == AeonNotificationStatus.Suppressed ||
            status == AeonNotificationStatus.Failed
}


// ----------------------------------------------------
// Notification Preferences
// ----------------------------------------------------

data class AeonNotificationPreferences(
    val masterEnabled: Boolean = true,
    val dailyPlanningEnabled: Boolean = true,
    val taskRemindersEnabled: Boolean = true,
    val habitRemindersEnabled: Boolean = true,
    val focusRemindersEnabled: Boolean = true,
    val moodCheckInsEnabled: Boolean = true,
    val healthRemindersEnabled: Boolean = true,
    val financeRemindersEnabled: Boolean = true,
    val goalRemindersEnabled: Boolean = true,
    val aiInsightsEnabled: Boolean = true,
    val backupNotificationsEnabled: Boolean = true,
    val quietHoursPolicy: AeonQuietHoursPolicy = AeonQuietHoursPolicy(),
    val maxNotificationsPerDay: Int = 8,
    val digestEnabled: Boolean = true,
    val digestTime: LocalTime = LocalTime.of(20, 30)
) {
    init {
        require(maxNotificationsPerDay in 1..30) {
            "Max notifications per day should be between 1 and 30."
        }
    }

    fun isChannelEnabled(
        channel: AeonNotificationChannelKey
    ): Boolean {
        if (!masterEnabled) return false

        return when (channel) {
            AeonNotificationChannelKey.DailyPlanning -> dailyPlanningEnabled
            AeonNotificationChannelKey.Tasks -> taskRemindersEnabled
            AeonNotificationChannelKey.Habits -> habitRemindersEnabled
            AeonNotificationChannelKey.Focus -> focusRemindersEnabled
            AeonNotificationChannelKey.Mood -> moodCheckInsEnabled
            AeonNotificationChannelKey.Health -> healthRemindersEnabled
            AeonNotificationChannelKey.Finance -> financeRemindersEnabled
            AeonNotificationChannelKey.Goals -> goalRemindersEnabled
            AeonNotificationChannelKey.AIInsights -> aiInsightsEnabled
            AeonNotificationChannelKey.Backup -> backupNotificationsEnabled
            AeonNotificationChannelKey.System -> true
        }
    }
}


// ----------------------------------------------------
// Engine Config
// ----------------------------------------------------

data class AeonNotificationEngineConfig(
    val appName: String = "Aeon",
    val notificationSmallIconName: String = "ic_stat_aeon",
    val defaultChannel: AeonNotificationChannelKey = AeonNotificationChannelKey.System,
    val enableHistory: Boolean = true,
    val enableGrouping: Boolean = true,
    val enableQuietHours: Boolean = true,
    val enableExactAlarmFallback: Boolean = true,
    val defaultPreferences: AeonNotificationPreferences = AeonNotificationPreferences()
)


// ----------------------------------------------------
// Payload Helpers
// ----------------------------------------------------

fun AeonNotificationPayload.toPendingRecord(
    ruleId: String? = null,
    scheduledAtEpochMillis: Long? = null
): AeonNotificationRecord {
    return AeonNotificationRecord(
        payloadId = id,
        notificationId = stableNotificationId,
        ruleId = ruleId,
        type = type,
        channelId = channel.channelId,
        title = title,
        body = body,
        deepLinkRoute = deepLinkRoute,
        source = source,
        sourceId = sourceId,
        status = if (scheduledAtEpochMillis == null) {
            AeonNotificationStatus.Pending
        } else {
            AeonNotificationStatus.Scheduled
        },
        scheduledAtEpochMillis = scheduledAtEpochMillis
    )
}


fun AeonNotificationRecord.markDelivered(
    deliveredAtEpochMillis: Long = System.currentTimeMillis()
): AeonNotificationRecord {
    return copy(
        status = AeonNotificationStatus.Delivered,
        deliveredAtEpochMillis = deliveredAtEpochMillis
    )
}


fun AeonNotificationRecord.markTapped(
    tappedAtEpochMillis: Long = System.currentTimeMillis()
): AeonNotificationRecord {
    return copy(
        status = AeonNotificationStatus.Tapped,
        tappedAtEpochMillis = tappedAtEpochMillis
    )
}


fun AeonNotificationRecord.markDismissed(
    dismissedAtEpochMillis: Long = System.currentTimeMillis()
): AeonNotificationRecord {
    return copy(
        status = AeonNotificationStatus.Dismissed,
        dismissedAtEpochMillis = dismissedAtEpochMillis
    )
}


fun AeonNotificationRecord.markFailed(
    reason: String,
    failedAtEpochMillis: Long = System.currentTimeMillis()
): AeonNotificationRecord {
    return copy(
        status = AeonNotificationStatus.Failed,
        deliveredAtEpochMillis = deliveredAtEpochMillis ?: failedAtEpochMillis,
        failureReason = reason
    )
}


// ----------------------------------------------------
// Default Aeon Rules
// ----------------------------------------------------

object AeonDefaultNotificationRules {

    fun dailyPlanRule(): AeonNotificationRule {
        return AeonNotificationRule(
            id = "rule_daily_plan",
            name = "Daily Planning Reminder",
            type = AeonNotificationType.DailyPlan,
            channel = AeonNotificationChannelKey.DailyPlanning,
            source = AeonNotificationSource.System,
            schedule = AeonNotificationSchedule.Daily(
                localTime = LocalTime.of(8, 0),
                exact = false
            ),
            template = AeonNotificationTemplate(
                titleTemplate = "Plan your day with Aeon",
                bodyTemplate = "Take one minute to choose your focus, habits, and next best action.",
                deepLinkRouteTemplate = "today"
            ),
            priority = AeonNotificationPriority.Normal,
            importance = AeonNotificationImportance.Default,
            conditions = listOf(
                AeonNotificationCondition.OnlyIfNotDeliveredToday,
                AeonNotificationCondition.MaxPerDay(1)
            )
        )
    }


    fun habitReminderRule(): AeonNotificationRule {
        return AeonNotificationRule(
            id = "rule_habit_reminder_{habitId}",
            name = "Habit Reminder",
            type = AeonNotificationType.HabitReminder,
            channel = AeonNotificationChannelKey.Habits,
            source = AeonNotificationSource.Habit,
            schedule = AeonNotificationSchedule.Daily(
                localTime = LocalTime.of(21, 30),
                exact = false
            ),
            template = AeonNotificationTemplate(
                titleTemplate = "{habitName}",
                bodyTemplate = "A small repeat today protects your long-term progress.",
                deepLinkRouteTemplate = "habit_detail/{habitId}"
            ),
            priority = AeonNotificationPriority.Normal,
            importance = AeonNotificationImportance.Default,
            conditions = listOf(
                AeonNotificationCondition.OnlyIfIncomplete,
                AeonNotificationCondition.MaxPerDay(1)
            )
        )
    }


    fun focusReminderRule(): AeonNotificationRule {
        return AeonNotificationRule(
            id = "rule_focus_start",
            name = "Focus Session Reminder",
            type = AeonNotificationType.FocusSession,
            channel = AeonNotificationChannelKey.Focus,
            source = AeonNotificationSource.Focus,
            schedule = AeonNotificationSchedule.Daily(
                localTime = LocalTime.of(10, 0),
                exact = false
            ),
            template = AeonNotificationTemplate(
                titleTemplate = "Protect one focus session",
                bodyTemplate = "Start a 25-minute deep work block before distractions increase.",
                deepLinkRouteTemplate = "focus"
            ),
            priority = AeonNotificationPriority.High,
            importance = AeonNotificationImportance.Default,
            conditions = listOf(
                AeonNotificationCondition.OnlyIfNotDeliveredToday,
                AeonNotificationCondition.MaxPerDay(1)
            )
        )
    }


    fun moodCheckInRule(): AeonNotificationRule {
        return AeonNotificationRule(
            id = "rule_mood_check_in",
            name = "Mood Check-in",
            type = AeonNotificationType.MoodCheckIn,
            channel = AeonNotificationChannelKey.Mood,
            source = AeonNotificationSource.Mood,
            schedule = AeonNotificationSchedule.Daily(
                localTime = LocalTime.of(20, 0),
                exact = false
            ),
            template = AeonNotificationTemplate(
                titleTemplate = "How are you feeling?",
                bodyTemplate = "A quick private check-in helps Aeon understand your day better.",
                deepLinkRouteTemplate = "add_mood_entry"
            ),
            priority = AeonNotificationPriority.Low,
            importance = AeonNotificationImportance.Low,
            conditions = listOf(
                AeonNotificationCondition.OnlyIfNotDeliveredToday,
                AeonNotificationCondition.MaxPerDay(1)
            )
        )
    }


    fun weeklyReviewRule(): AeonNotificationRule {
        return AeonNotificationRule(
            id = "rule_weekly_review",
            name = "Weekly Review",
            type = AeonNotificationType.WeeklyReview,
            channel = AeonNotificationChannelKey.AIInsights,
            source = AeonNotificationSource.AI,
            schedule = AeonNotificationSchedule.Weekly(
                days = setOf(DayOfWeek.SUNDAY),
                localTime = LocalTime.of(19, 0),
                exact = false
            ),
            template = AeonNotificationTemplate(
                titleTemplate = "Your weekly Aeon review is ready",
                bodyTemplate = "Review habits, focus, mood, and progress from this week.",
                deepLinkRouteTemplate = "insights"
            ),
            priority = AeonNotificationPriority.Normal,
            importance = AeonNotificationImportance.Default,
            conditions = listOf(
                AeonNotificationCondition.MaxPerDay(1)
            )
        )
    }


    fun all(): List<AeonNotificationRule> {
        return listOf(
            dailyPlanRule(),
            habitReminderRule(),
            focusReminderRule(),
            moodCheckInRule(),
            weeklyReviewRule()
        )
    }
}


// ----------------------------------------------------
// Utility Functions
// ----------------------------------------------------

fun aeonNewNotificationId(
    prefix: String = "notification"
): String {
    return "${prefix}_${UUID.randomUUID()}"
}


fun aeonStableNotificationId(
    seed: String
): Int {
    val crc = CRC32()
    crc.update(seed.toByteArray(Charsets.UTF_8))

    return (crc.value and 0x7FFFFFFF).toInt()
}


private fun String.applyAeonTemplate(
    values: Map<String, String>
): String {
    return values.entries.fold(this) { result, entry ->
        result.replace("{${entry.key}}", entry.value)
    }
}
