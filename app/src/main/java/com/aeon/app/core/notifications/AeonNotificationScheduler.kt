package com.aeon.app.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import kotlin.math.max

/*
 * AEON NOTIFICATION SCHEDULER
 *
 * Purpose:
 * Accurate and efficient scheduling layer for Aeon notifications.
 *
 * Strategy:
 * - Immediate notifications -> publish directly.
 * - Exact one-time reminders -> AlarmManager exact alarm.
 * - Non-exact one-time reminders -> WorkManager.
 * - Daily / weekly reminders -> one-shot schedule, then receiver/worker reschedules.
 * - Repeating interval reminders -> PeriodicWorkRequest.
 *
 * Senior Developer Rule:
 * Use exact alarms only when the user expects exact timing.
 * Use WorkManager for battery-friendly, deferrable reminders.
 */


// ----------------------------------------------------
// Scheduler Intent Contract
// ----------------------------------------------------

object AeonNotificationSchedulerContract {
    const val ACTION_PUBLISH_SCHEDULED =
        "com.aeon.app.notifications.ACTION_PUBLISH_SCHEDULED"

    const val EXTRA_PAYLOAD_JSON = "extra_payload_json"
    const val EXTRA_SCHEDULE_JSON = "extra_schedule_json"
    const val EXTRA_RULE_ID = "extra_rule_id"
    const val EXTRA_TRIGGER_AT = "extra_trigger_at"
    const val EXTRA_RESCHEDULE_AFTER_DELIVERY = "extra_reschedule_after_delivery"

    const val WORK_DATA_PAYLOAD_JSON = "work_data_payload_json"
    const val WORK_DATA_SCHEDULE_JSON = "work_data_schedule_json"
    const val WORK_DATA_RULE_ID = "work_data_rule_id"
    const val WORK_DATA_TRIGGER_AT = "work_data_trigger_at"
    const val WORK_DATA_RESCHEDULE_AFTER_DELIVERY = "work_data_reschedule_after_delivery"

    const val WORK_TAG_ROOT = "aeon_notification"
}


// ----------------------------------------------------
// Scheduling Backend
// ----------------------------------------------------

enum class AeonNotificationScheduleBackend {
    Immediate,
    AlarmManagerExact,
    WorkManagerOneTime,
    WorkManagerPeriodic
}


// ----------------------------------------------------
// Schedule Result
// ----------------------------------------------------

sealed interface AeonNotificationScheduleResult {

    data class Scheduled(
        val payload: AeonNotificationPayload,
        val schedule: AeonNotificationSchedule,
        val triggerAtEpochMillis: Long?,
        val backend: AeonNotificationScheduleBackend,
        val uniqueName: String
    ) : AeonNotificationScheduleResult

    data class PublishedImmediately(
        val result: AeonNotificationPublishResult
    ) : AeonNotificationScheduleResult

    data class Blocked(
        val payload: AeonNotificationPayload,
        val schedule: AeonNotificationSchedule,
        val reason: AeonNotificationScheduleBlockedReason,
        val message: String
    ) : AeonNotificationScheduleResult

    data class Failed(
        val payload: AeonNotificationPayload,
        val schedule: AeonNotificationSchedule,
        val reason: String,
        val throwable: Throwable? = null
    ) : AeonNotificationScheduleResult

    data class Cancelled(
        val payloadId: String,
        val uniqueName: String
    ) : AeonNotificationScheduleResult
}


// ----------------------------------------------------
// Main Scheduler
// ----------------------------------------------------

class AeonNotificationScheduler(
    context: Context,
    private val config: AeonNotificationEngineConfig = AeonNotificationEngineConfig()
) {

    private val appContext: Context =
        context.applicationContext

    private val workManager: WorkManager =
        WorkManager.getInstance(appContext)

    private val alarmManager: AlarmManager =
        appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val permissionManager: AeonNotificationPermissionManager =
        AeonNotificationPermissionManager.create(appContext)

    private val publisher: AeonNotificationPublisher =
        AeonNotificationPublisher(
            context = appContext,
            config = config
        )


    // ----------------------------------------------------
    // Public Schedule API
    // ----------------------------------------------------

    fun schedule(
        payload: AeonNotificationPayload,
        schedule: AeonNotificationSchedule,
        ruleId: String? = null
    ): AeonNotificationScheduleResult {
        AeonNotificationChannels.ensureCreated(appContext)

        val permissionCheck = permissionManager.checkSchedulePermission(
            payload = payload,
            schedule = schedule
        )

        if (permissionCheck is AeonNotificationSchedulePermissionCheck.Blocked) {
            return AeonNotificationScheduleResult.Blocked(
                payload = payload,
                schedule = schedule,
                reason = permissionCheck.reason,
                message = permissionCheck.reason.userMessage()
            )
        }

        return try {
            when (schedule) {
                AeonNotificationSchedule.Immediate -> {
                    AeonNotificationScheduleResult.PublishedImmediately(
                        result = publisher.publish(payload)
                    )
                }

                is AeonNotificationSchedule.OneTime -> {
                    scheduleOneTime(
                        payload = payload,
                        schedule = schedule,
                        ruleId = ruleId,
                        rescheduleAfterDelivery = false
                    )
                }

                is AeonNotificationSchedule.RepeatingInterval -> {
                    scheduleRepeatingInterval(
                        payload = payload,
                        schedule = schedule,
                        ruleId = ruleId
                    )
                }

                is AeonNotificationSchedule.Daily -> {
                    val triggerAt = nextDailyTriggerMillis(schedule.localTime)

                    scheduleOneShotRecurring(
                        payload = payload,
                        schedule = schedule,
                        triggerAtEpochMillis = triggerAt,
                        ruleId = ruleId
                    )
                }

                is AeonNotificationSchedule.Weekly -> {
                    val triggerAt = nextWeeklyTriggerMillis(
                        days = schedule.days,
                        localTime = schedule.localTime
                    )

                    scheduleOneShotRecurring(
                        payload = payload,
                        schedule = schedule,
                        triggerAtEpochMillis = triggerAt,
                        ruleId = ruleId
                    )
                }
            }
        } catch (throwable: Throwable) {
            AeonNotificationScheduleResult.Failed(
                payload = payload,
                schedule = schedule,
                reason = throwable.message ?: "Failed to schedule notification.",
                throwable = throwable
            )
        }
    }


    fun scheduleRule(
        rule: AeonNotificationRule,
        values: Map<String, String> = emptyMap(),
        sourceId: String? = null
    ): AeonNotificationScheduleResult {
        if (!rule.enabled) {
            val payload = rule.buildPayload(
                values = values,
                sourceId = sourceId
            )

            return AeonNotificationScheduleResult.Failed(
                payload = payload,
                schedule = rule.schedule,
                reason = "Notification rule is disabled."
            )
        }

        val resolvedRuleId = rule.id.applyAeonLocalTemplate(values)

        return schedule(
            payload = rule.buildPayload(
                values = values,
                sourceId = sourceId
            ),
            schedule = rule.schedule,
            ruleId = resolvedRuleId
        )
    }


    fun scheduleMany(
        items: List<Pair<AeonNotificationPayload, AeonNotificationSchedule>>
    ): List<AeonNotificationScheduleResult> {
        return items.map { item ->
            schedule(
                payload = item.first,
                schedule = item.second
            )
        }
    }


    fun scheduleDefaultRules(): List<AeonNotificationScheduleResult> {
        return AeonDefaultNotificationRules
            .all()
            .map { rule ->
                scheduleRule(rule)
            }
    }


    // ----------------------------------------------------
    // Cancel API
    // ----------------------------------------------------

    fun cancel(
        payloadId: String
    ): AeonNotificationScheduleResult.Cancelled {
        val uniqueName = workUniqueName(payloadId)

        workManager.cancelUniqueWork(uniqueName)
        workManager.cancelAllWorkByTag(payloadTag(payloadId))

        cancelAlarm(payloadId)

        return AeonNotificationScheduleResult.Cancelled(
            payloadId = payloadId,
            uniqueName = uniqueName
        )
    }


    fun cancel(
        payload: AeonNotificationPayload
    ): AeonNotificationScheduleResult.Cancelled {
        publisher.cancel(payload)

        return cancel(payload.id)
    }


    fun cancelRule(
        ruleId: String
    ) {
        workManager.cancelAllWorkByTag(ruleTag(ruleId))
    }


    fun cancelAllScheduled() {
        workManager.cancelAllWorkByTag(AeonNotificationSchedulerContract.WORK_TAG_ROOT)
    }


    // ----------------------------------------------------
    // One-Time Schedule
    // ----------------------------------------------------

    private fun scheduleOneTime(
        payload: AeonNotificationPayload,
        schedule: AeonNotificationSchedule.OneTime,
        ruleId: String?,
        rescheduleAfterDelivery: Boolean
    ): AeonNotificationScheduleResult {
        val now = System.currentTimeMillis()
        val triggerAt = schedule.triggerAtEpochMillis

        if (triggerAt <= now) {
            return AeonNotificationScheduleResult.PublishedImmediately(
                result = publisher.publish(payload)
            )
        }

        return if (schedule.exact) {
            scheduleAlarmManagerExact(
                payload = payload,
                schedule = schedule,
                triggerAtEpochMillis = triggerAt,
                ruleId = ruleId,
                rescheduleAfterDelivery = rescheduleAfterDelivery
            )
        } else {
            scheduleWorkManagerOneTime(
                payload = payload,
                schedule = schedule,
                triggerAtEpochMillis = triggerAt,
                ruleId = ruleId,
                rescheduleAfterDelivery = rescheduleAfterDelivery
            )
        }
    }


    private fun scheduleOneShotRecurring(
        payload: AeonNotificationPayload,
        schedule: AeonNotificationSchedule,
        triggerAtEpochMillis: Long,
        ruleId: String?
    ): AeonNotificationScheduleResult {
        return if (schedule.requiresExactAlarm) {
            scheduleAlarmManagerExact(
                payload = payload,
                schedule = schedule,
                triggerAtEpochMillis = triggerAtEpochMillis,
                ruleId = ruleId,
                rescheduleAfterDelivery = true
            )
        } else {
            scheduleWorkManagerOneTime(
                payload = payload,
                schedule = schedule,
                triggerAtEpochMillis = triggerAtEpochMillis,
                ruleId = ruleId,
                rescheduleAfterDelivery = true
            )
        }
    }


    // ----------------------------------------------------
    // AlarmManager Exact
    // ----------------------------------------------------

    private fun scheduleAlarmManagerExact(
        payload: AeonNotificationPayload,
        schedule: AeonNotificationSchedule,
        triggerAtEpochMillis: Long,
        ruleId: String?,
        rescheduleAfterDelivery: Boolean
    ): AeonNotificationScheduleResult {
        if (!permissionManager.canScheduleExactAlarms()) {
            if (config.enableExactAlarmFallback) {
                return scheduleWorkManagerOneTime(
                    payload = payload,
                    schedule = schedule,
                    triggerAtEpochMillis = triggerAtEpochMillis,
                    ruleId = ruleId,
                    rescheduleAfterDelivery = rescheduleAfterDelivery
                )
            }

            return AeonNotificationScheduleResult.Blocked(
                payload = payload,
                schedule = schedule,
                reason = AeonNotificationScheduleBlockedReason.ExactAlarmAccessDenied,
                message = AeonNotificationScheduleBlockedReason.ExactAlarmAccessDenied.userMessage()
            )
        }

        val pendingIntent = buildAlarmPendingIntent(
            payload = payload,
            schedule = schedule,
            triggerAtEpochMillis = triggerAtEpochMillis,
            ruleId = ruleId,
            rescheduleAfterDelivery = rescheduleAfterDelivery
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtEpochMillis,
            pendingIntent
        )

        return AeonNotificationScheduleResult.Scheduled(
            payload = payload,
            schedule = schedule,
            triggerAtEpochMillis = triggerAtEpochMillis,
            backend = AeonNotificationScheduleBackend.AlarmManagerExact,
            uniqueName = alarmUniqueName(payload.id)
        )
    }


    private fun cancelAlarm(
        payloadId: String
    ) {
        val intent = Intent(
            appContext,
            AeonNotificationReceiver::class.java
        ).apply {
            action = AeonNotificationSchedulerContract.ACTION_PUBLISH_SCHEDULED
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            alarmRequestCode(payloadId),
            intent,
            pendingIntentFlags() or PendingIntent.FLAG_NO_CREATE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }


    private fun buildAlarmPendingIntent(
        payload: AeonNotificationPayload,
        schedule: AeonNotificationSchedule,
        triggerAtEpochMillis: Long,
        ruleId: String?,
        rescheduleAfterDelivery: Boolean
    ): PendingIntent {
        val intent = Intent(
            appContext,
            AeonNotificationReceiver::class.java
        ).apply {
            action = AeonNotificationSchedulerContract.ACTION_PUBLISH_SCHEDULED

            putExtra(
                AeonNotificationSchedulerContract.EXTRA_PAYLOAD_JSON,
                AeonNotificationPayloadCodec.encodePayload(payload)
            )

            putExtra(
                AeonNotificationSchedulerContract.EXTRA_SCHEDULE_JSON,
                AeonNotificationPayloadCodec.encodeSchedule(schedule)
            )

            putExtra(
                AeonNotificationSchedulerContract.EXTRA_RULE_ID,
                ruleId
            )

            putExtra(
                AeonNotificationSchedulerContract.EXTRA_TRIGGER_AT,
                triggerAtEpochMillis
            )

            putExtra(
                AeonNotificationSchedulerContract.EXTRA_RESCHEDULE_AFTER_DELIVERY,
                rescheduleAfterDelivery
            )
        }

        return PendingIntent.getBroadcast(
            appContext,
            alarmRequestCode(payload.id),
            intent,
            pendingIntentFlags()
        )
    }


    // ----------------------------------------------------
    // WorkManager One-Time
    // ----------------------------------------------------

    private fun scheduleWorkManagerOneTime(
        payload: AeonNotificationPayload,
        schedule: AeonNotificationSchedule,
        triggerAtEpochMillis: Long,
        ruleId: String?,
        rescheduleAfterDelivery: Boolean
    ): AeonNotificationScheduleResult {
        val delayMillis = max(
            0L,
            triggerAtEpochMillis - System.currentTimeMillis()
        )

        val request = OneTimeWorkRequestBuilder<AeonNotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .setInputData(
                buildWorkData(
                    payload = payload,
                    schedule = schedule,
                    triggerAtEpochMillis = triggerAtEpochMillis,
                    ruleId = ruleId,
                    rescheduleAfterDelivery = rescheduleAfterDelivery
                )
            )
            .addTag(AeonNotificationSchedulerContract.WORK_TAG_ROOT)
            .addTag(payloadTag(payload.id))
            .apply {
                if (!ruleId.isNullOrBlank()) {
                    addTag(ruleTag(ruleId))
                }
            }
            .build()

        val uniqueName = workUniqueName(payload.id)

        workManager.enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.REPLACE,
            request
        )

        return AeonNotificationScheduleResult.Scheduled(
            payload = payload,
            schedule = schedule,
            triggerAtEpochMillis = triggerAtEpochMillis,
            backend = AeonNotificationScheduleBackend.WorkManagerOneTime,
            uniqueName = uniqueName
        )
    }


    // ----------------------------------------------------
    // WorkManager Periodic
    // ----------------------------------------------------

    private fun scheduleRepeatingInterval(
        payload: AeonNotificationPayload,
        schedule: AeonNotificationSchedule.RepeatingInterval,
        ruleId: String?
    ): AeonNotificationScheduleResult {
        val repeatMinutes = schedule.repeatIntervalMinutes.coerceAtLeast(15L)
        val flexMinutes = schedule.flexMinutes.coerceIn(5L, repeatMinutes)

        val initialDelayMillis = max(
            0L,
            schedule.startAtEpochMillis - System.currentTimeMillis()
        )

        val request = PeriodicWorkRequestBuilder<AeonNotificationWorker>(
            repeatMinutes,
            TimeUnit.MINUTES,
            flexMinutes,
            TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .setInputData(
                buildWorkData(
                    payload = payload,
                    schedule = schedule,
                    triggerAtEpochMillis = schedule.startAtEpochMillis,
                    ruleId = ruleId,
                    rescheduleAfterDelivery = false
                )
            )
            .addTag(AeonNotificationSchedulerContract.WORK_TAG_ROOT)
            .addTag(payloadTag(payload.id))
            .apply {
                if (!ruleId.isNullOrBlank()) {
                    addTag(ruleTag(ruleId))
                }
            }
            .build()

        val uniqueName = workUniqueName(payload.id)

        workManager.enqueueUniquePeriodicWork(
            uniqueName,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )

        return AeonNotificationScheduleResult.Scheduled(
            payload = payload,
            schedule = schedule,
            triggerAtEpochMillis = schedule.startAtEpochMillis,
            backend = AeonNotificationScheduleBackend.WorkManagerPeriodic,
            uniqueName = uniqueName
        )
    }


    // ----------------------------------------------------
    // Work Data
    // ----------------------------------------------------

    private fun buildWorkData(
        payload: AeonNotificationPayload,
        schedule: AeonNotificationSchedule,
        triggerAtEpochMillis: Long,
        ruleId: String?,
        rescheduleAfterDelivery: Boolean
    ): Data {
        return Data.Builder()
            .putString(
                AeonNotificationSchedulerContract.WORK_DATA_PAYLOAD_JSON,
                AeonNotificationPayloadCodec.encodePayload(payload)
            )
            .putString(
                AeonNotificationSchedulerContract.WORK_DATA_SCHEDULE_JSON,
                AeonNotificationPayloadCodec.encodeSchedule(schedule)
            )
            .putString(
                AeonNotificationSchedulerContract.WORK_DATA_RULE_ID,
                ruleId.orEmpty()
            )
            .putLong(
                AeonNotificationSchedulerContract.WORK_DATA_TRIGGER_AT,
                triggerAtEpochMillis
            )
            .putBoolean(
                AeonNotificationSchedulerContract.WORK_DATA_RESCHEDULE_AFTER_DELIVERY,
                rescheduleAfterDelivery
            )
            .build()
    }


    // ----------------------------------------------------
    // Trigger Calculators
    // ----------------------------------------------------

    private fun nextDailyTriggerMillis(
        localTime: LocalTime,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val now = LocalDateTime.now(zoneId)

        var next = now
            .withHour(localTime.hour)
            .withMinute(localTime.minute)
            .withSecond(0)
            .withNano(0)

        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }

        return next
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }


    private fun nextWeeklyTriggerMillis(
        days: Set<DayOfWeek>,
        localTime: LocalTime,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val now = LocalDateTime.now(zoneId)

        return days
            .map { day ->
                var candidate = now
                    .with(TemporalAdjusters.nextOrSame(day))
                    .withHour(localTime.hour)
                    .withMinute(localTime.minute)
                    .withSecond(0)
                    .withNano(0)

                if (!candidate.isAfter(now)) {
                    candidate = candidate.plusWeeks(1)
                }

                candidate
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
            }
            .minOrNull()
            ?: nextDailyTriggerMillis(localTime, zoneId)
    }


    // ----------------------------------------------------
    // IDs / Tags / Flags
    // ----------------------------------------------------

    private fun workUniqueName(
        payloadId: String
    ): String {
        return "aeon_notification_work_$payloadId"
    }


    private fun alarmUniqueName(
        payloadId: String
    ): String {
        return "aeon_notification_alarm_$payloadId"
    }


    private fun payloadTag(
        payloadId: String
    ): String {
        return "aeon_payload_$payloadId"
    }


    private fun ruleTag(
        ruleId: String
    ): String {
        return "aeon_rule_$ruleId"
    }


    private fun alarmRequestCode(
        payloadId: String
    ): Int {
        return aeonStableNotificationId("alarm_$payloadId")
    }


    private fun pendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }


    companion object {

        fun create(
            context: Context
        ): AeonNotificationScheduler {
            return AeonNotificationScheduler(
                context = context.applicationContext
            )
        }
    }
}


// ----------------------------------------------------
// Payload + Schedule Codec
// Shared by Scheduler, Worker, Receiver.
// ----------------------------------------------------

object AeonNotificationPayloadCodec {

    fun encodePayload(
        payload: AeonNotificationPayload
    ): String {
        return JSONObject()
            .put("id", payload.id)
            .put("type", payload.type.name)
            .put("channel", payload.channel.name)
            .put("title", payload.title)
            .put("body", payload.body)
            .put("source", payload.source.name)
            .put("sourceId", payload.sourceId)
            .put("deepLinkRoute", payload.deepLinkRoute)
            .put("groupKey", payload.groupKey)
            .put("priority", payload.priority.name)
            .put("importance", payload.importance.name)
            .put("deliveryMode", payload.deliveryMode.name)
            .put("createdAtEpochMillis", payload.createdAtEpochMillis)
            .put("actions", JSONArray().apply {
                payload.actions.forEach { action ->
                    put(
                        JSONObject()
                            .put("id", action.id)
                            .put("label", action.label)
                            .put("route", action.route)
                            .put("destructive", action.destructive)
                    )
                }
            })
            .put("metadata", JSONObject().apply {
                payload.metadata.forEach { entry ->
                    put(entry.key, entry.value)
                }
            })
            .toString()
    }


    fun decodePayload(
        json: String?
    ): AeonNotificationPayload? {
        if (json.isNullOrBlank()) return null

        return try {
            val objectJson = JSONObject(json)

            val actionsJson = objectJson.optJSONArray("actions") ?: JSONArray()
            val metadataJson = objectJson.optJSONObject("metadata") ?: JSONObject()

            val actions = buildList {
                for (index in 0 until actionsJson.length()) {
                    val actionJson = actionsJson.getJSONObject(index)

                    add(
                        AeonNotificationAction(
                            id = actionJson.getString("id"),
                            label = actionJson.getString("label"),
                            route = actionJson.optNullableString("route"),
                            destructive = actionJson.optBoolean("destructive", false)
                        )
                    )
                }
            }

            val metadata = buildMap {
                val keys = metadataJson.keys()

                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, metadataJson.optString(key))
                }
            }

            AeonNotificationPayload(
                id = objectJson.getString("id"),
                type = enumValueOf(objectJson.getString("type")),
                channel = enumValueOf(objectJson.getString("channel")),
                title = objectJson.getString("title"),
                body = objectJson.getString("body"),
                source = enumValueOf(
                    objectJson.optString(
                        "source",
                        AeonNotificationSource.System.name
                    )
                ),
                sourceId = objectJson.optNullableString("sourceId"),
                deepLinkRoute = objectJson.optNullableString("deepLinkRoute"),
                groupKey = objectJson.optNullableString("groupKey"),
                priority = enumValueOf(
                    objectJson.optString(
                        "priority",
                        AeonNotificationPriority.Normal.name
                    )
                ),
                importance = enumValueOf(
                    objectJson.optString(
                        "importance",
                        AeonNotificationImportance.Default.name
                    )
                ),
                deliveryMode = enumValueOf(
                    objectJson.optString(
                        "deliveryMode",
                        AeonNotificationDeliveryMode.Standard.name
                    )
                ),
                actions = actions,
                metadata = metadata,
                createdAtEpochMillis = objectJson.optLong(
                    "createdAtEpochMillis",
                    System.currentTimeMillis()
                )
            )
        } catch (_: Throwable) {
            null
        }
    }


    fun encodeSchedule(
        schedule: AeonNotificationSchedule
    ): String {
        return when (schedule) {
            AeonNotificationSchedule.Immediate -> {
                JSONObject()
                    .put("kind", "Immediate")
                    .toString()
            }

            is AeonNotificationSchedule.OneTime -> {
                JSONObject()
                    .put("kind", "OneTime")
                    .put("triggerAtEpochMillis", schedule.triggerAtEpochMillis)
                    .put("exact", schedule.exact)
                    .toString()
            }

            is AeonNotificationSchedule.RepeatingInterval -> {
                JSONObject()
                    .put("kind", "RepeatingInterval")
                    .put("startAtEpochMillis", schedule.startAtEpochMillis)
                    .put("repeatIntervalMinutes", schedule.repeatIntervalMinutes)
                    .put("flexMinutes", schedule.flexMinutes)
                    .toString()
            }

            is AeonNotificationSchedule.Daily -> {
                JSONObject()
                    .put("kind", "Daily")
                    .put("hour", schedule.localTime.hour)
                    .put("minute", schedule.localTime.minute)
                    .put("exact", schedule.exact)
                    .toString()
            }

            is AeonNotificationSchedule.Weekly -> {
                JSONObject()
                    .put("kind", "Weekly")
                    .put("days", JSONArray().apply {
                        schedule.days.forEach { day ->
                            put(day.name)
                        }
                    })
                    .put("hour", schedule.localTime.hour)
                    .put("minute", schedule.localTime.minute)
                    .put("exact", schedule.exact)
                    .toString()
            }
        }
    }


    fun decodeSchedule(
        json: String?
    ): AeonNotificationSchedule? {
        if (json.isNullOrBlank()) return null

        return try {
            val objectJson = JSONObject(json)

            when (objectJson.getString("kind")) {
                "Immediate" -> {
                    AeonNotificationSchedule.Immediate
                }

                "OneTime" -> {
                    AeonNotificationSchedule.OneTime(
                        triggerAtEpochMillis = objectJson.getLong("triggerAtEpochMillis"),
                        exact = objectJson.optBoolean("exact", false)
                    )
                }

                "RepeatingInterval" -> {
                    AeonNotificationSchedule.RepeatingInterval(
                        startAtEpochMillis = objectJson.getLong("startAtEpochMillis"),
                        repeatIntervalMinutes = objectJson.getLong("repeatIntervalMinutes"),
                        flexMinutes = objectJson.optLong("flexMinutes", 15L)
                    )
                }

                "Daily" -> {
                    AeonNotificationSchedule.Daily(
                        localTime = LocalTime.of(
                            objectJson.getInt("hour"),
                            objectJson.getInt("minute")
                        ),
                        exact = objectJson.optBoolean("exact", false)
                    )
                }

                "Weekly" -> {
                    val daysJson = objectJson.getJSONArray("days")

                    val days = buildSet {
                        for (index in 0 until daysJson.length()) {
                            add(DayOfWeek.valueOf(daysJson.getString(index)))
                        }
                    }

                    AeonNotificationSchedule.Weekly(
                        days = days,
                        localTime = LocalTime.of(
                            objectJson.getInt("hour"),
                            objectJson.getInt("minute")
                        ),
                        exact = objectJson.optBoolean("exact", false)
                    )
                }

                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }
}


// ----------------------------------------------------
// Local Helpers
// ----------------------------------------------------

private fun JSONObject.optNullableString(
    key: String
): String? {
    if (!has(key) || isNull(key)) return null

    return optString(key)
        .takeIf { value -> value.isNotBlank() }
}


private fun String.applyAeonLocalTemplate(
    values: Map<String, String>
): String {
    return values.entries.fold(this) { result, entry ->
        result.replace("{${entry.key}}", entry.value)
    }
}
