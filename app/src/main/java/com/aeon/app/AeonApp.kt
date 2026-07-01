package com.aeon.app

import android.app.Application
import com.aeon.app.core.notifications.AeonNotificationInitializer
import com.aeon.app.core.notifications.AeonNotificationActionHandler
import com.aeon.app.core.notifications.AeonNotificationFeatureActionDelegate
import com.aeon.app.core.notifications.AeonNotificationStartupPolicy
import com.aeon.app.di.AeonAppContainer
import com.aeon.app.di.DefaultAeonAppContainer

class AeonApp : Application() {

    lateinit var container: AeonAppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        container = DefaultAeonAppContainer(this)

        AeonNotificationActionHandler.setFeatureActionDelegate(
            object : AeonNotificationFeatureActionDelegate {
                override suspend fun markTaskDone(taskId: String): Boolean {
                    if (container.repositories.tasks.getTask(taskId) == null) return false
                    container.useCases.completeTask(taskId, createPositiveInsight = false)
                    return true
                }

                override suspend fun startFocusSession(sourceId: String?): Boolean {
                    container.useCases.startFocusSession(taskId = sourceId)
                    return true
                }

                override suspend fun markFocusRoutineDone(occurrenceId: String): Boolean {
                    if (container.repositories.focusRoutines.getOccurrence(occurrenceId) == null) return false
                    container.useCases.focusRoutines.done(occurrenceId)
                    return true
                }

                override suspend fun startFocusRoutine(occurrenceId: String): Boolean {
                    if (container.repositories.focusRoutines.getOccurrence(occurrenceId) == null) return false
                    container.useCases.focusRoutines.start(occurrenceId)
                    return true
                }

                override suspend fun snoozeFocusRoutine(occurrenceId: String, minutes: Int): Boolean {
                    if (container.repositories.focusRoutines.getOccurrence(occurrenceId) == null) return false
                    container.useCases.focusRoutines.snooze(
                        occurrenceId,
                        java.time.Instant.now().plusSeconds(minutes * 60L)
                    )
                    return true
                }
            }
        )

        AeonNotificationInitializer.install(
            application = this,
            policy = AeonNotificationStartupPolicy(
                createChannelsOnStart = true,
                initializeCenterOnStart = true,
                saveDefaultRulesIfMissing = true,
                scheduleEnabledRulesOnStart = true,
                refreshOnAppForeground = true,
                rescheduleOnForegroundWhenNeeded = false
            )
        )
    }
}
