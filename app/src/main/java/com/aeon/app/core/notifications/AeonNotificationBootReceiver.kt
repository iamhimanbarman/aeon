package com.aeon.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aeon.app.data.local.database.AeonDatabase
import com.aeon.app.data.task.AndroidTaskReminderScheduler
import com.aeon.app.data.focus.AndroidFocusRoutineReminderScheduler
import com.aeon.app.data.repository.FocusRoutineRepository
import java.util.concurrent.TimeUnit

/*
 * AEON NOTIFICATION BOOT RECEIVER
 *
 * Purpose:
 * Restores Aeon notification infrastructure after:
 * - Device reboot
 * - App update
 * - Time changed
 * - Time zone changed
 *
 * Why this exists:
 * - AlarmManager alarms are cleared after reboot.
 * - Notification channels should be recreated after updates.
 * - Time/timezone changes can make scheduled reminders inaccurate.
 * - WorkManager usually survives reboot, but exact alarms do not.
 *
 * Senior Developer Rule:
 * BroadcastReceiver must stay lightweight.
 * It should enqueue background work and exit quickly.
 */


// ----------------------------------------------------
// Boot Contract
// ----------------------------------------------------

object AeonNotificationBootContract {
    const val UNIQUE_RESCHEDULE_WORK = "aeon_notification_boot_reschedule_work"

    const val WORK_REASON = "work_reason"

    const val REASON_BOOT_COMPLETED = "boot_completed"
    const val REASON_LOCKED_BOOT_COMPLETED = "locked_boot_completed"
    const val REASON_PACKAGE_REPLACED = "package_replaced"
    const val REASON_TIME_CHANGED = "time_changed"
    const val REASON_TIMEZONE_CHANGED = "timezone_changed"
    const val REASON_UNKNOWN = "unknown"
}


// ----------------------------------------------------
// Boot Receiver
// ----------------------------------------------------

class AeonNotificationBootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {
        if (intent == null) return

        val reason = intent.action.toAeonBootReason()

        /*
         * Always recreate channels immediately.
         * This is cheap and idempotent.
         */
        AeonNotificationChannels.ensureCreated(context)

        /*
         * Heavy rescheduling is delegated to WorkManager.
         */
        enqueueRescheduleWork(
            context = context.applicationContext,
            reason = reason
        )
    }


    private fun enqueueRescheduleWork(
        context: Context,
        reason: String
    ) {
        val request = OneTimeWorkRequestBuilder<AeonNotificationRescheduleWorker>()
            .setInitialDelay(2, TimeUnit.SECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15,
                TimeUnit.SECONDS
            )
            .setInputData(
                androidx.work.Data.Builder()
                    .putString(
                        AeonNotificationBootContract.WORK_REASON,
                        reason
                    )
                    .build()
            )
            .addTag(AeonNotificationSchedulerContract.WORK_TAG_ROOT)
            .addTag("aeon_notification_reschedule")
            .build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                AeonNotificationBootContract.UNIQUE_RESCHEDULE_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }
}


// ----------------------------------------------------
// Reschedule Worker
// ----------------------------------------------------

class AeonNotificationRescheduleWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
) {

    override suspend fun doWork(): Result {
        val reason = inputData.getString(
            AeonNotificationBootContract.WORK_REASON
        ) ?: AeonNotificationBootContract.REASON_UNKNOWN

        return try {
            /*
             * 1. Recreate channels after app update / boot.
             */
            AeonNotificationChannels.ensureCreated(applicationContext)

            /*
             * 2. Schedule default Aeon system rules.
             *
             * Later, after AeonNotificationRepository.kt is connected,
             * this worker should load all user-enabled rules from Room:
             *
             * - habit reminders
             * - task reminders
             * - medicine reminders
             * - finance reminders
             * - custom focus alarms
             * - weekly reviews
             *
             * For now this keeps the engine functional and safe.
             */
            val repository = RoomAeonNotificationRepository.create(applicationContext)

            val savedRules = repository.enabledRules()

            if (savedRules.isEmpty()) {
                val defaultRules = AeonDefaultNotificationRules.all()
                repository.saveRules(defaultRules)
            }

            val engine = AeonNotificationRuleEngine.create(applicationContext)
            engine.evaluateAndScheduleEnabledRules()

            AndroidTaskReminderScheduler(
                context = applicationContext,
                taskDao = AeonDatabase.getInstance(applicationContext).taskDao()
            ).reschedulePending()

            val focusDatabase = AeonDatabase.getInstance(applicationContext)
            val focusRoutines = FocusRoutineRepository(focusDatabase.focusRoutineDao())
            focusRoutines.ensureTemplates()
            focusRoutines.generate()
            if (reason == AeonNotificationBootContract.REASON_TIMEZONE_CHANGED ||
                reason == AeonNotificationBootContract.REASON_TIME_CHANGED
            ) {
                focusRoutines.regenerateUpcomingForClockChange()
            }
            focusRoutines.refreshStatuses()

            AndroidFocusRoutineReminderScheduler(
                context = applicationContext,
                dao = focusDatabase.focusRoutineDao()
            ).reschedulePending()

            Result.success(
                androidx.work.Data.Builder()
                    .putString("status", "rescheduled")
                    .putString("reason", reason)
                    .build()
            )
        } catch (throwable: Throwable) {
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure(
                    androidx.work.Data.Builder()
                        .putString("status", "failed")
                        .putString("reason", reason)
                        .putString(
                            "error",
                            throwable.message ?: "Unknown reschedule failure."
                        )
                        .build()
                )
            }
        }
    }


    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}


// ----------------------------------------------------
// Intent Action Resolver
// ----------------------------------------------------

private fun String?.toAeonBootReason(): String {
    return when (this) {
        Intent.ACTION_BOOT_COMPLETED ->
            AeonNotificationBootContract.REASON_BOOT_COMPLETED

        Intent.ACTION_LOCKED_BOOT_COMPLETED ->
            AeonNotificationBootContract.REASON_LOCKED_BOOT_COMPLETED

        Intent.ACTION_MY_PACKAGE_REPLACED ->
            AeonNotificationBootContract.REASON_PACKAGE_REPLACED

        Intent.ACTION_TIME_CHANGED ->
            AeonNotificationBootContract.REASON_TIME_CHANGED

        Intent.ACTION_TIMEZONE_CHANGED ->
            AeonNotificationBootContract.REASON_TIMEZONE_CHANGED

        else ->
            AeonNotificationBootContract.REASON_UNKNOWN
    }
}
