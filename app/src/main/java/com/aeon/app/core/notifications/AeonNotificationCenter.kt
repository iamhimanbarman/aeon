package com.aeon.app.core.notifications

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.flow.Flow

/*
 * AEON NOTIFICATION CENTER
 *
 * Purpose:
 * Single public entry point for Aeon's notification system.
 *
 * This class coordinates:
 * - Channels
 * - Permission state
 * - Repository
 * - Rule engine
 * - Scheduler
 * - Publisher
 * - Preferences
 * - Notification history
 *
 * Senior Developer Rule:
 * App features should not directly call Scheduler, Publisher, Repository,
 * PermissionManager, or RuleEngine separately.
 *
 * UI / ViewModel / Feature modules should talk to AeonNotificationCenter.
 */


// ----------------------------------------------------
// Center Initialization Result
// ----------------------------------------------------

sealed interface AeonNotificationCenterInitResult {

    data class Ready(
        val permissionSnapshot: AeonNotificationPermissionSnapshot,
        val savedDefaultRules: Boolean,
        val scheduledRuleCount: Int
    ) : AeonNotificationCenterInitResult

    data class Partial(
        val permissionSnapshot: AeonNotificationPermissionSnapshot,
        val message: String
    ) : AeonNotificationCenterInitResult

    data class Failed(
        val reason: String,
        val throwable: Throwable? = null
    ) : AeonNotificationCenterInitResult
}


// ----------------------------------------------------
// Center Operation Result
// ----------------------------------------------------

sealed interface AeonNotificationCenterResult {

    data class Success(
        val message: String
    ) : AeonNotificationCenterResult

    data class Blocked(
        val message: String
    ) : AeonNotificationCenterResult

    data class Failed(
        val message: String,
        val throwable: Throwable? = null
    ) : AeonNotificationCenterResult
}


// ----------------------------------------------------
// Notification Center State
// ----------------------------------------------------

data class AeonNotificationCenterState(
    val permissionSnapshot: AeonNotificationPermissionSnapshot,
    val preferences: AeonNotificationPreferences,
    val channelHealth: AeonNotificationChannelHealth
) {
    val isHealthy: Boolean
        get() = permissionSnapshot.canPostNotifications &&
            channelHealth.isHealthy

    val primaryIssue: String?
        get() = permissionSnapshot.primaryIssueMessage()
}


// ----------------------------------------------------
// Main Notification Center
// ----------------------------------------------------

class AeonNotificationCenter private constructor(
    private val context: Context,
    private val config: AeonNotificationEngineConfig,
    private val repository: AeonNotificationRepository,
    private val channels: AeonNotificationChannels,
    private val permissionManager: AeonNotificationPermissionManager,
    private val scheduler: AeonNotificationScheduler,
    private val publisher: AeonNotificationPublisher,
    private val ruleEngine: AeonNotificationRuleEngine
) {

    // ----------------------------------------------------
    // Initialize
    // ----------------------------------------------------

    suspend fun initialize(
        saveDefaultRulesIfMissing: Boolean = true,
        scheduleEnabledRules: Boolean = true
    ): AeonNotificationCenterInitResult {
        return try {
            channels.createAllChannels()

            val preferences = repository.getPreferences()
            repository.savePreferences(preferences)

            var savedDefaultRules = false

            val existingRules = repository.enabledRules()

            if (saveDefaultRulesIfMissing && existingRules.isEmpty()) {
                val defaultRules = AeonDefaultNotificationRules.all()
                repository.saveRules(defaultRules)
                savedDefaultRules = true
            }

            val scheduleResults = if (scheduleEnabledRules) {
                ruleEngine.evaluateAndScheduleEnabledRules()
            } else {
                emptyList()
            }

            val snapshot = permissionManager.snapshot()

            if (!snapshot.canPostNotifications) {
                AeonNotificationCenterInitResult.Partial(
                    permissionSnapshot = snapshot,
                    message = snapshot.primaryIssueMessage()
                        ?: "Notifications are not fully enabled."
                )
            } else {
                AeonNotificationCenterInitResult.Ready(
                    permissionSnapshot = snapshot,
                    savedDefaultRules = savedDefaultRules,
                    scheduledRuleCount = scheduleResults.count {
                        it is AeonNotificationRuleEngineResult.Scheduled ||
                            it is AeonNotificationRuleEngineResult.Delayed
                    }
                )
            }
        } catch (throwable: Throwable) {
            AeonNotificationCenterInitResult.Failed(
                reason = throwable.message ?: "Failed to initialize notification center.",
                throwable = throwable
            )
        }
    }


    // ----------------------------------------------------
    // State
    // ----------------------------------------------------

    suspend fun state(): AeonNotificationCenterState {
        return AeonNotificationCenterState(
            permissionSnapshot = permissionManager.snapshot(),
            preferences = repository.getPreferences(),
            channelHealth = channels.checkHealth()
        )
    }


    fun observeRecentNotifications(
        limit: Int = 100
    ): Flow<List<AeonNotificationRecord>> {
        return repository.observeRecentRecords(limit)
    }


    fun observeRules(): Flow<List<AeonNotificationRule>> {
        return repository.observeRules()
    }


    fun observePreferences(): Flow<AeonNotificationPreferences> {
        return repository.observePreferences()
    }


    // ----------------------------------------------------
    // Direct Publish
    // ----------------------------------------------------

    suspend fun publish(
        payload: AeonNotificationPayload,
        ruleId: String? = null
    ): AeonNotificationPublishResult {
        repository.saveRecord(
            payload.toPendingRecord(ruleId = ruleId)
        )

        val result = publisher.publish(payload)

        persistPublishResult(
            result = result,
            ruleId = ruleId
        )

        return result
    }


    // ----------------------------------------------------
    // Schedule
    // ----------------------------------------------------

    suspend fun schedule(
        payload: AeonNotificationPayload,
        schedule: AeonNotificationSchedule,
        ruleId: String? = null
    ): AeonNotificationScheduleResult {
        repository.saveRecord(
            payload.toPendingRecord(
                ruleId = ruleId,
                scheduledAtEpochMillis = schedule.initialScheduledAtOrNull()
            )
        )

        val result = scheduler.schedule(
            payload = payload,
            schedule = schedule,
            ruleId = ruleId
        )

        persistScheduleResult(
            result = result,
            ruleId = ruleId
        )

        return result
    }


    suspend fun scheduleRule(
        rule: AeonNotificationRule,
        context: AeonNotificationEvaluationContext = AeonNotificationEvaluationContext(),
        saveRule: Boolean = true
    ): AeonNotificationRuleEngineResult {
        if (saveRule) {
            repository.saveRule(rule)
        }

        val result = ruleEngine.evaluateAndSchedule(
            rule = rule,
            context = context
        )

        persistRuleEngineResult(result)

        return result
    }


    suspend fun scheduleEnabledRules(): List<AeonNotificationRuleEngineResult> {
        val results = ruleEngine.evaluateAndScheduleEnabledRules()

        results.forEach { result ->
            persistRuleEngineResult(result)
        }

        return results
    }


    suspend fun scheduleDefaultRules(): List<AeonNotificationRuleEngineResult> {
        val rules = AeonDefaultNotificationRules.all()

        repository.saveRules(rules)

        return rules.map { rule ->
            scheduleRule(
                rule = rule,
                saveRule = false
            )
        }
    }


    // ----------------------------------------------------
    // Feature Convenience APIs
    // ----------------------------------------------------

    suspend fun scheduleHabitReminder(
        habitId: String,
        habitName: String,
        completed: Boolean
    ): AeonNotificationRuleEngineResult {
        return scheduleRule(
            rule = AeonDefaultNotificationRules.habitReminderRule(),
            context = AeonNotificationEvaluationContext(
                values = mapOf(
                    "habitId" to habitId,
                    "habitName" to habitName
                ),
                sourceId = habitId,
                sourceCompletionState = if (completed) {
                    AeonNotificationSourceCompletionState.Complete
                } else {
                    AeonNotificationSourceCompletionState.Incomplete
                }
            )
        )
    }


    suspend fun scheduleFocusReminder(): AeonNotificationRuleEngineResult {
        return scheduleRule(
            rule = AeonDefaultNotificationRules.focusReminderRule(),
            context = AeonNotificationEvaluationContext()
        )
    }


    suspend fun scheduleMoodCheckIn(): AeonNotificationRuleEngineResult {
        return scheduleRule(
            rule = AeonDefaultNotificationRules.moodCheckInRule(),
            context = AeonNotificationEvaluationContext()
        )
    }


    suspend fun scheduleWeeklyReview(): AeonNotificationRuleEngineResult {
        return scheduleRule(
            rule = AeonDefaultNotificationRules.weeklyReviewRule(),
            context = AeonNotificationEvaluationContext()
        )
    }


    // ----------------------------------------------------
    // Cancel
    // ----------------------------------------------------

    suspend fun cancelPayload(
        payloadId: String
    ): AeonNotificationScheduleResult.Cancelled {
        val result = scheduler.cancel(payloadId)

        repository.markCancelled(payloadId)

        return result
    }


    suspend fun cancelPayload(
        payload: AeonNotificationPayload
    ): AeonNotificationScheduleResult.Cancelled {
        publisher.cancel(payload)

        return cancelPayload(payload.id)
    }


    suspend fun cancelRule(
        ruleId: String
    ): AeonNotificationCenterResult {
        return try {
            scheduler.cancelRule(ruleId)
            repository.setRuleEnabled(ruleId, false)

            AeonNotificationCenterResult.Success(
                message = "Notification rule cancelled."
            )
        } catch (throwable: Throwable) {
            AeonNotificationCenterResult.Failed(
                message = throwable.message ?: "Failed to cancel notification rule.",
                throwable = throwable
            )
        }
    }


    suspend fun cancelAllScheduled(): AeonNotificationCenterResult {
        return try {
            scheduler.cancelAllScheduled()

            AeonNotificationCenterResult.Success(
                message = "All scheduled notifications cancelled."
            )
        } catch (throwable: Throwable) {
            AeonNotificationCenterResult.Failed(
                message = throwable.message ?: "Failed to cancel scheduled notifications.",
                throwable = throwable
            )
        }
    }


    // ----------------------------------------------------
    // Records / History
    // ----------------------------------------------------

    suspend fun recentNotifications(
        limit: Int = 100
    ): List<AeonNotificationRecord> {
        return repository.recentRecords(limit)
    }


    suspend fun markTapped(
        payloadId: String
    ) {
        repository.markTapped(payloadId)
    }


    suspend fun markDismissed(
        payloadId: String
    ) {
        repository.markDismissed(payloadId)
    }


    suspend fun clearHistory() {
        repository.clearHistory()
    }


    suspend fun pruneOldHistory(
        beforeEpochMillis: Long
    ) {
        repository.pruneRecordsBefore(beforeEpochMillis)
    }


    // ----------------------------------------------------
    // Preferences
    // ----------------------------------------------------

    suspend fun preferences(): AeonNotificationPreferences {
        return repository.getPreferences()
    }


    suspend fun updatePreferences(
        preferences: AeonNotificationPreferences
    ) {
        repository.savePreferences(preferences)

        if (!preferences.masterEnabled) {
            scheduler.cancelAllScheduled()
        } else {
            scheduleEnabledRules()
        }
    }


    suspend fun setMasterEnabled(
        enabled: Boolean
    ) {
        val current = repository.getPreferences()

        updatePreferences(
            current.copy(
                masterEnabled = enabled
            )
        )
    }


    suspend fun setHabitRemindersEnabled(
        enabled: Boolean
    ) {
        val current = repository.getPreferences()

        updatePreferences(
            current.copy(
                habitRemindersEnabled = enabled
            )
        )
    }


    suspend fun setFocusRemindersEnabled(
        enabled: Boolean
    ) {
        val current = repository.getPreferences()

        updatePreferences(
            current.copy(
                focusRemindersEnabled = enabled
            )
        )
    }


    suspend fun setMoodCheckInsEnabled(
        enabled: Boolean
    ) {
        val current = repository.getPreferences()

        updatePreferences(
            current.copy(
                moodCheckInsEnabled = enabled
            )
        )
    }


    // ----------------------------------------------------
    // Rules
    // ----------------------------------------------------

    suspend fun saveRule(
        rule: AeonNotificationRule
    ) {
        repository.saveRule(rule)
    }


    suspend fun saveRules(
        rules: List<AeonNotificationRule>
    ) {
        repository.saveRules(rules)
    }


    suspend fun enableRule(
        ruleId: String
    ) {
        repository.setRuleEnabled(
            ruleId = ruleId,
            enabled = true
        )

        val rule = repository.getRule(ruleId)

        if (rule != null) {
            scheduleRule(
                rule = rule,
                saveRule = false
            )
        }
    }


    suspend fun disableRule(
        ruleId: String
    ) {
        repository.setRuleEnabled(
            ruleId = ruleId,
            enabled = false
        )

        scheduler.cancelRule(ruleId)
    }


    suspend fun deleteRule(
        ruleId: String
    ) {
        scheduler.cancelRule(ruleId)
        repository.deleteRule(ruleId)
    }


    // ----------------------------------------------------
    // Permissions / Settings
    // ----------------------------------------------------

    fun permissionSnapshot(): AeonNotificationPermissionSnapshot {
        return permissionManager.snapshot()
    }


    fun canPostNotifications(): Boolean {
        return permissionManager.canPostNotifications()
    }


    fun shouldRequestRuntimePermission(): Boolean {
        return permissionManager.shouldRequestRuntimeNotificationPermission()
    }


    fun openAppNotificationSettings() {
        permissionManager.openAppNotificationSettings()
    }


    fun openChannelSettings(
        channelKey: AeonNotificationChannelKey
    ) {
        permissionManager.openChannelNotificationSettings(channelKey)
    }


    fun openExactAlarmSettings() {
        permissionManager.openExactAlarmSettings()
    }


    // ----------------------------------------------------
    // Internal Persistence Helpers
    // ----------------------------------------------------

    private suspend fun persistPublishResult(
        result: AeonNotificationPublishResult,
        ruleId: String?
    ) {
        when (result) {
            is AeonNotificationPublishResult.Published -> {
                repository.saveRecord(
                    result.record.copy(
                        ruleId = ruleId ?: result.record.ruleId
                    )
                )
            }

            is AeonNotificationPublishResult.Blocked -> {
                repository.saveRecord(
                    result.payload.toPendingRecord(ruleId = ruleId).copy(
                        status = AeonNotificationStatus.Suppressed,
                        failureReason = result.message
                    )
                )
            }

            is AeonNotificationPublishResult.Failed -> {
                repository.saveRecord(
                    result.payload.toPendingRecord(ruleId = ruleId).copy(
                        status = AeonNotificationStatus.Failed,
                        failureReason = result.reason
                    )
                )
            }
        }
    }


    private suspend fun persistScheduleResult(
        result: AeonNotificationScheduleResult,
        ruleId: String?
    ) {
        when (result) {
            is AeonNotificationScheduleResult.Scheduled -> {
                repository.saveRecord(
                    result.payload.toPendingRecord(
                        ruleId = ruleId,
                        scheduledAtEpochMillis = result.triggerAtEpochMillis
                    )
                )
            }

            is AeonNotificationScheduleResult.PublishedImmediately -> {
                persistPublishResult(
                    result = result.result,
                    ruleId = ruleId
                )
            }

            is AeonNotificationScheduleResult.Blocked -> {
                repository.saveRecord(
                    result.payload.toPendingRecord(ruleId = ruleId).copy(
                        status = AeonNotificationStatus.Suppressed,
                        failureReason = result.message
                    )
                )
            }

            is AeonNotificationScheduleResult.Failed -> {
                repository.saveRecord(
                    result.payload.toPendingRecord(ruleId = ruleId).copy(
                        status = AeonNotificationStatus.Failed,
                        failureReason = result.reason
                    )
                )
            }

            is AeonNotificationScheduleResult.Cancelled -> {
                repository.markCancelled(result.payloadId)
            }
        }
    }


    private suspend fun persistRuleEngineResult(
        result: AeonNotificationRuleEngineResult
    ) {
        when (result) {
            is AeonNotificationRuleEngineResult.Scheduled -> {
                persistScheduleResult(
                    result = result.scheduleResult,
                    ruleId = result.ruleId
                )
            }

            is AeonNotificationRuleEngineResult.Delayed -> {
                persistScheduleResult(
                    result = result.scheduleResult,
                    ruleId = result.ruleId
                )
            }

            is AeonNotificationRuleEngineResult.Skipped -> Unit

            is AeonNotificationRuleEngineResult.Suppressed -> Unit

            is AeonNotificationRuleEngineResult.Failed -> Unit
        }
    }


    // ----------------------------------------------------
    // Factory
    // ----------------------------------------------------

    companion object {

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: AeonNotificationCenter? = null


        fun getInstance(
            context: Context,
            config: AeonNotificationEngineConfig = AeonNotificationEngineConfig()
        ): AeonNotificationCenter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: createInternal(
                    context = context.applicationContext,
                    config = config
                ).also { center ->
                    INSTANCE = center
                }
            }
        }


        fun create(
            context: Context,
            config: AeonNotificationEngineConfig = AeonNotificationEngineConfig()
        ): AeonNotificationCenter {
            return createInternal(
                context = context.applicationContext,
                config = config
            )
        }


        private fun createInternal(
            context: Context,
            config: AeonNotificationEngineConfig
        ): AeonNotificationCenter {
            val repository = RoomAeonNotificationRepository.create(context)
            val channels = AeonNotificationChannels.create(context)
            val permissionManager = AeonNotificationPermissionManager.create(context)
            val scheduler = AeonNotificationScheduler(
                context = context,
                config = config
            )
            val publisher = AeonNotificationPublisher(
                context = context,
                config = config
            )
            val ruleEngine = AeonNotificationRuleEngine(
                repository = repository,
                scheduler = scheduler
            )

            return AeonNotificationCenter(
                context = context,
                config = config,
                repository = repository,
                channels = channels,
                permissionManager = permissionManager,
                scheduler = scheduler,
                publisher = publisher,
                ruleEngine = ruleEngine
            )
        }
    }
}


// ----------------------------------------------------
// Schedule Helper
// ----------------------------------------------------

private fun AeonNotificationSchedule.initialScheduledAtOrNull(): Long? {
    return when (this) {
        AeonNotificationSchedule.Immediate -> null
        is AeonNotificationSchedule.OneTime -> triggerAtEpochMillis
        is AeonNotificationSchedule.RepeatingInterval -> startAtEpochMillis
        is AeonNotificationSchedule.Daily -> null
        is AeonNotificationSchedule.Weekly -> null
    }
}
