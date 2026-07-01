package com.aeon.app.data.task

import android.content.Context
import com.aeon.app.core.notifications.AeonNotificationAction
import com.aeon.app.core.notifications.AeonNotificationActionIds
import com.aeon.app.core.notifications.AeonNotificationChannelKey
import com.aeon.app.core.notifications.AeonNotificationImportance
import com.aeon.app.core.notifications.AeonNotificationPayload
import com.aeon.app.core.notifications.AeonNotificationPriority
import com.aeon.app.core.notifications.AeonNotificationSchedule
import com.aeon.app.core.notifications.AeonNotificationScheduleResult
import com.aeon.app.core.notifications.AeonNotificationScheduler
import com.aeon.app.core.notifications.AeonNotificationSource
import com.aeon.app.core.notifications.AeonNotificationType
import com.aeon.app.data.local.database.dao.TaskDao
import com.aeon.app.data.local.database.entities.TaskEntity
import com.aeon.app.data.local.database.entities.TaskReminderEntity
import com.aeon.app.data.local.database.entities.TaskReminderTypeStorage
import java.time.Instant

interface TaskReminderScheduler {
    suspend fun schedule(task: TaskEntity, preferExact: Boolean = true): TaskReminderScheduleOutcome
    suspend fun cancel(taskId: String)
    suspend fun reschedulePending()
}

sealed interface TaskReminderScheduleOutcome {
    data object NotRequired : TaskReminderScheduleOutcome
    data class Scheduled(val exact: Boolean) : TaskReminderScheduleOutcome
    data class Failed(val message: String) : TaskReminderScheduleOutcome
}

class AndroidTaskReminderScheduler(
    context: Context,
    private val taskDao: TaskDao,
    private val scheduler: AeonNotificationScheduler = AeonNotificationScheduler.create(context)
) : TaskReminderScheduler {

    override suspend fun schedule(
        task: TaskEntity,
        preferExact: Boolean
    ): TaskReminderScheduleOutcome {
        val reminderAt = task.reminderAt ?: return TaskReminderScheduleOutcome.NotRequired
        if (!reminderAt.isAfter(Instant.now())) return TaskReminderScheduleOutcome.NotRequired

        val payload = task.toReminderPayload()
        scheduler.cancel(payload.id)
        val exactResult = scheduler.schedule(
            payload = payload,
            schedule = AeonNotificationSchedule.OneTime(
                triggerAtEpochMillis = reminderAt.toEpochMilli(),
                exact = preferExact
            )
        )
        val finalResult = if (preferExact && exactResult is AeonNotificationScheduleResult.Blocked) {
            scheduler.schedule(
                payload = payload,
                schedule = AeonNotificationSchedule.OneTime(
                    triggerAtEpochMillis = reminderAt.toEpochMilli(),
                    exact = false
                )
            )
        } else {
            exactResult
        }

        val usedExact = preferExact && exactResult !is AeonNotificationScheduleResult.Blocked
        taskDao.upsertReminder(
            TaskReminderEntity(
                id = payload.id,
                taskId = task.id,
                reminderAt = reminderAt,
                type = if (usedExact) TaskReminderTypeStorage.Exact else TaskReminderTypeStorage.Flexible,
                updatedAt = Instant.now()
            )
        )

        return when (finalResult) {
            is AeonNotificationScheduleResult.Scheduled,
            is AeonNotificationScheduleResult.PublishedImmediately -> {
                TaskReminderScheduleOutcome.Scheduled(exact = usedExact)
            }
            is AeonNotificationScheduleResult.Blocked -> {
                TaskReminderScheduleOutcome.Failed(finalResult.message)
            }
            is AeonNotificationScheduleResult.Failed -> {
                TaskReminderScheduleOutcome.Failed(finalResult.reason)
            }
            is AeonNotificationScheduleResult.Cancelled -> {
                TaskReminderScheduleOutcome.Failed("Reminder was cancelled before it could be scheduled.")
            }
        }
    }

    override suspend fun cancel(taskId: String) {
        taskDao.getReminders(taskId).forEach { scheduler.cancel(it.id) }
        scheduler.cancel(payloadId(taskId))
        taskDao.deleteRemindersForTask(taskId)
    }

    override suspend fun reschedulePending() {
        taskDao.getPendingReminders().forEach { reminder ->
            val task = taskDao.getTaskById(reminder.taskId) ?: return@forEach
            schedule(
                task = task.copy(
                    reminderAt = reminder.snoozedUntil ?: reminder.reminderAt
                ),
                preferExact = reminder.type == TaskReminderTypeStorage.Exact
            )
        }
    }

    private fun TaskEntity.toReminderPayload(): AeonNotificationPayload =
        AeonNotificationPayload(
            id = payloadId(id),
            type = AeonNotificationType.TaskReminder,
            channel = AeonNotificationChannelKey.Tasks,
            title = "Task reminder",
            body = title,
            source = AeonNotificationSource.Task,
            sourceId = id,
            deepLinkRoute = "task_detail/$id",
            groupKey = "tasks",
            priority = when (priority) {
                "critical" -> AeonNotificationPriority.Urgent
                "high" -> AeonNotificationPriority.High
                "low" -> AeonNotificationPriority.Low
                else -> AeonNotificationPriority.Normal
            },
            importance = if (priority == "critical" || priority == "high") {
                AeonNotificationImportance.High
            } else {
                AeonNotificationImportance.Default
            },
            actions = listOf(
                AeonNotificationAction(AeonNotificationActionIds.COMPLETE_TASK, "Done"),
                AeonNotificationAction(AeonNotificationActionIds.SNOOZE_10, "Snooze 10m"),
                AeonNotificationAction(AeonNotificationActionIds.OPEN, "Open", "task_detail/$id")
            ),
            metadata = mapOf("task_id" to id)
        )

    private fun payloadId(taskId: String): String = "task_reminder_$taskId"
}

object NoOpTaskReminderScheduler : TaskReminderScheduler {
    override suspend fun schedule(task: TaskEntity, preferExact: Boolean) =
        TaskReminderScheduleOutcome.NotRequired

    override suspend fun cancel(taskId: String) = Unit

    override suspend fun reschedulePending() = Unit
}
