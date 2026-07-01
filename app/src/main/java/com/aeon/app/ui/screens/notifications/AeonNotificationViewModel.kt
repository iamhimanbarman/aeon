package com.aeon.app.ui.screens.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aeon.app.core.notifications.AeonNotificationCenter
import com.aeon.app.core.notifications.AeonNotificationCenterInitResult
import com.aeon.app.core.notifications.AeonNotificationCenterState
import com.aeon.app.core.notifications.AeonNotificationChannelKey
import com.aeon.app.core.notifications.AeonNotificationEvaluationContext
import com.aeon.app.core.notifications.AeonNotificationPreferences
import com.aeon.app.core.notifications.AeonNotificationRecord
import com.aeon.app.core.notifications.AeonNotificationRule
import com.aeon.app.core.notifications.AeonNotificationRuleEngineResult
import com.aeon.app.core.notifications.AeonNotificationSourceCompletionState
import com.aeon.app.core.notifications.AeonQuietHoursPolicy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

/*
 * AEON NOTIFICATION VIEWMODEL
 *
 * Purpose:
 * Presentation-state manager for Aeon notification UI.
 *
 * Handles:
 * - Notification settings state
 * - Notification inbox state
 * - Notification preference state
 * - Permission/channel health refresh
 * - Preference mutations
 * - Rule enable/disable
 * - History actions
 * - One public bridge between UI and AeonNotificationCenter
 *
 * Senior Developer Rule:
 * Compose screens should not directly orchestrate notification business logic.
 * Screens should call this ViewModel.
 * This ViewModel talks only to AeonNotificationCenter.
 */


// ----------------------------------------------------
// UI State
// ----------------------------------------------------

data class AeonNotificationUiState(
    val loading: Boolean = true,
    val working: Boolean = false,
    val initialized: Boolean = false,
    val preferences: AeonNotificationPreferences = AeonNotificationPreferences(),
    val centerState: AeonNotificationCenterState? = null,
    val initResult: AeonNotificationCenterInitResult? = null,
    val records: List<AeonNotificationRecord> = emptyList(),
    val rules: List<AeonNotificationRule> = emptyList(),
    val selectedChannel: AeonNotificationChannelKey? = null,
    val selectedRecord: AeonNotificationRecord? = null,
    val message: String? = null,
    val error: String? = null
) {
    val isHealthy: Boolean
        get() = centerState?.isHealthy == true

    val primaryIssue: String?
        get() = centerState?.primaryIssue

    val hasNotifications: Boolean
        get() = records.isNotEmpty()

    val unreadCount: Int
        get() = records.count { it.status.name == "Delivered" }

    val scheduledCount: Int
        get() = records.count {
            it.status.name == "Pending" || it.status.name == "Scheduled"
        }

    val issueCount: Int
        get() = records.count {
            it.status.name == "Failed" || it.status.name == "Suppressed"
        }

    val enabledRuleCount: Int
        get() = rules.count { it.enabled }
}


// ----------------------------------------------------
// One-Shot Events
// ----------------------------------------------------

sealed interface AeonNotificationUiEvent {

    data class Toast(
        val message: String
    ) : AeonNotificationUiEvent

    data class NavigateToRoute(
        val route: String
    ) : AeonNotificationUiEvent

    data object OpenedSystemSettings : AeonNotificationUiEvent

    data object OpenedExactAlarmSettings : AeonNotificationUiEvent
}


// ----------------------------------------------------
// ViewModel
// ----------------------------------------------------

class AeonNotificationViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val notificationCenter: AeonNotificationCenter =
        AeonNotificationCenter.getInstance(application.applicationContext)

    private val localState = MutableStateFlow(
        AeonNotificationUiState()
    )

    private val eventsInternal = MutableSharedFlow<AeonNotificationUiEvent>(
        extraBufferCapacity = 16
    )

    val events = eventsInternal.asSharedFlow()

    val uiState: StateFlow<AeonNotificationUiState> =
        combine(
            localState,
            notificationCenter.observePreferences(),
            notificationCenter.observeRecentNotifications(limit = 150),
            notificationCenter.observeRules()
        ) { state, preferences, records, rules ->
            state.copy(
                preferences = preferences,
                records = records,
                rules = rules
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AeonNotificationUiState()
        )


    init {
        initialize()
    }


    // ----------------------------------------------------
    // Initialize / Refresh
    // ----------------------------------------------------

    fun initialize(
        saveDefaultRulesIfMissing: Boolean = true,
        scheduleEnabledRules: Boolean = false
    ) {
        launchOperation {
            val result = notificationCenter.initialize(
                saveDefaultRulesIfMissing = saveDefaultRulesIfMissing,
                scheduleEnabledRules = scheduleEnabledRules
            )

            val state = notificationCenter.state()

            localState.value = localState.value.copy(
                loading = false,
                initialized = true,
                initResult = result,
                centerState = state,
                message = result.toUiMessage(),
                error = null
            )
        }
    }


    fun refresh() {
        launchOperation(
            successMessage = "Notification system refreshed."
        ) {
            val state = notificationCenter.state()

            localState.value = localState.value.copy(
                loading = false,
                centerState = state,
                message = state.primaryIssue ?: "Notification system is ready.",
                error = null
            )
        }
    }


    fun clearMessage() {
        localState.value = localState.value.copy(
            message = null,
            error = null
        )
    }


    // ----------------------------------------------------
    // Preferences
    // ----------------------------------------------------

    fun updatePreferences(
        preferences: AeonNotificationPreferences
    ) {
        launchOperation(
            successMessage = "Notification preferences updated."
        ) {
            notificationCenter.updatePreferences(preferences)
            refreshStateOnly()
        }
    }


    fun setMasterEnabled(
        enabled: Boolean
    ) {
        val current = uiState.value.preferences

        updatePreferences(
            current.copy(masterEnabled = enabled)
        )
    }


    fun setCategoryEnabled(
        channel: AeonNotificationChannelKey,
        enabled: Boolean
    ) {
        val current = uiState.value.preferences

        updatePreferences(
            current.withCategoryEnabled(
                channel = channel,
                enabled = enabled
            )
        )
    }


    fun setQuietHoursEnabled(
        enabled: Boolean
    ) {
        val current = uiState.value.preferences
        val policy = current.quietHoursPolicy

        updatePreferences(
            current.copy(
                quietHoursPolicy = policy.copy(enabled = enabled)
            )
        )
    }


    fun updateQuietHoursPolicy(
        policy: AeonQuietHoursPolicy
    ) {
        val current = uiState.value.preferences

        updatePreferences(
            current.copy(
                quietHoursPolicy = policy
            )
        )
    }


    fun setQuietHoursStart(
        time: LocalTime
    ) {
        val current = uiState.value.preferences
        val policy = current.quietHoursPolicy

        updatePreferences(
            current.copy(
                quietHoursPolicy = policy.copy(start = time)
            )
        )
    }


    fun setQuietHoursEnd(
        time: LocalTime
    ) {
        val current = uiState.value.preferences
        val policy = current.quietHoursPolicy

        updatePreferences(
            current.copy(
                quietHoursPolicy = policy.copy(end = time)
            )
        )
    }


    fun setDigestEnabled(
        enabled: Boolean
    ) {
        val current = uiState.value.preferences

        updatePreferences(
            current.copy(digestEnabled = enabled)
        )
    }


    fun setDigestTime(
        time: LocalTime
    ) {
        val current = uiState.value.preferences

        updatePreferences(
            current.copy(digestTime = time)
        )
    }


    fun setDailyLimit(
        limit: Int
    ) {
        val current = uiState.value.preferences

        updatePreferences(
            current.copy(
                maxNotificationsPerDay = limit.coerceIn(1, 30)
            )
        )
    }


    fun increaseDailyLimit() {
        setDailyLimit(
            uiState.value.preferences.maxNotificationsPerDay + 1
        )
    }


    fun decreaseDailyLimit() {
        setDailyLimit(
            uiState.value.preferences.maxNotificationsPerDay - 1
        )
    }


    // ----------------------------------------------------
    // Rules
    // ----------------------------------------------------

    fun scheduleEnabledRules() {
        launchOperation(
            successMessage = "Enabled notification rules scheduled."
        ) {
            notificationCenter.scheduleEnabledRules()
            refreshStateOnly()
        }
    }


    fun scheduleDefaultRules() {
        launchOperation(
            successMessage = "Default Aeon notification rules scheduled."
        ) {
            notificationCenter.scheduleDefaultRules()
            refreshStateOnly()
        }
    }


    fun enableRule(
        ruleId: String
    ) {
        launchOperation(
            successMessage = "Notification rule enabled."
        ) {
            notificationCenter.enableRule(ruleId)
            refreshStateOnly()
        }
    }


    fun disableRule(
        ruleId: String
    ) {
        launchOperation(
            successMessage = "Notification rule disabled."
        ) {
            notificationCenter.disableRule(ruleId)
            refreshStateOnly()
        }
    }


    fun deleteRule(
        ruleId: String
    ) {
        launchOperation(
            successMessage = "Notification rule deleted."
        ) {
            notificationCenter.deleteRule(ruleId)
            refreshStateOnly()
        }
    }


    // ----------------------------------------------------
    // Feature Convenience
    // ----------------------------------------------------

    fun scheduleHabitReminder(
        habitId: String,
        habitName: String,
        completed: Boolean
    ) {
        launchOperation {
            val result = notificationCenter.scheduleHabitReminder(
                habitId = habitId,
                habitName = habitName,
                completed = completed
            )

            localState.value = localState.value.copy(
                message = result.toUiMessage(),
                error = null
            )

            refreshStateOnly()
        }
    }


    fun scheduleFocusReminder() {
        launchOperation {
            val result = notificationCenter.scheduleFocusReminder()

            localState.value = localState.value.copy(
                message = result.toUiMessage(),
                error = null
            )

            refreshStateOnly()
        }
    }


    fun scheduleMoodCheckIn() {
        launchOperation {
            val result = notificationCenter.scheduleMoodCheckIn()

            localState.value = localState.value.copy(
                message = result.toUiMessage(),
                error = null
            )

            refreshStateOnly()
        }
    }


    fun scheduleWeeklyReview() {
        launchOperation {
            val result = notificationCenter.scheduleWeeklyReview()

            localState.value = localState.value.copy(
                message = result.toUiMessage(),
                error = null
            )

            refreshStateOnly()
        }
    }


    fun scheduleRule(
        rule: AeonNotificationRule,
        context: AeonNotificationEvaluationContext = AeonNotificationEvaluationContext()
    ) {
        launchOperation {
            val result = notificationCenter.scheduleRule(
                rule = rule,
                context = context,
                saveRule = true
            )

            localState.value = localState.value.copy(
                message = result.toUiMessage(),
                error = null
            )

            refreshStateOnly()
        }
    }


    fun scheduleIncompleteHabitRule(
        rule: AeonNotificationRule,
        habitId: String,
        habitName: String
    ) {
        scheduleRule(
            rule = rule,
            context = AeonNotificationEvaluationContext(
                values = mapOf(
                    "habitId" to habitId,
                    "habitName" to habitName
                ),
                sourceId = habitId,
                sourceCompletionState = AeonNotificationSourceCompletionState.Incomplete
            )
        )
    }


    // ----------------------------------------------------
    // Inbox / History
    // ----------------------------------------------------

    fun selectRecord(
        record: AeonNotificationRecord?
    ) {
        localState.value = localState.value.copy(
            selectedRecord = record
        )
    }


    fun openRecord(
        record: AeonNotificationRecord
    ) {
        val route = record.deepLinkRoute

        if (route.isNullOrBlank()) {
            localState.value = localState.value.copy(
                message = "This notification has no route to open."
            )
            return
        }

        viewModelScope.launch {
            notificationCenter.markTapped(record.payloadId)

            eventsInternal.emit(
                AeonNotificationUiEvent.NavigateToRoute(route)
            )
        }
    }


    fun markTapped(
        payloadId: String
    ) {
        launchOperation(
            successMessage = "Notification marked as opened."
        ) {
            notificationCenter.markTapped(payloadId)
        }
    }


    fun markDismissed(
        payloadId: String
    ) {
        launchOperation(
            successMessage = "Notification dismissed."
        ) {
            notificationCenter.markDismissed(payloadId)
        }
    }


    fun clearHistory() {
        launchOperation(
            successMessage = "Notification history cleared."
        ) {
            notificationCenter.clearHistory()
        }
    }


    fun pruneHistoryBefore(
        beforeEpochMillis: Long
    ) {
        launchOperation(
            successMessage = "Old notification history removed."
        ) {
            notificationCenter.pruneOldHistory(beforeEpochMillis)
        }
    }


    // ----------------------------------------------------
    // Cancel
    // ----------------------------------------------------

    fun cancelPayload(
        payloadId: String
    ) {
        launchOperation(
            successMessage = "Notification cancelled."
        ) {
            notificationCenter.cancelPayload(payloadId)
            refreshStateOnly()
        }
    }


    fun cancelRule(
        ruleId: String
    ) {
        launchOperation {
            val result = notificationCenter.cancelRule(ruleId)

            localState.value = localState.value.copy(
                message = result.toUiMessage(),
                error = null
            )

            refreshStateOnly()
        }
    }


    fun cancelAllScheduled() {
        launchOperation {
            val result = notificationCenter.cancelAllScheduled()

            localState.value = localState.value.copy(
                message = result.toUiMessage(),
                error = null
            )

            refreshStateOnly()
        }
    }


    // ----------------------------------------------------
    // Permission / Settings
    // ----------------------------------------------------

    fun openAppNotificationSettings() {
        notificationCenter.openAppNotificationSettings()

        viewModelScope.launch {
            eventsInternal.emit(AeonNotificationUiEvent.OpenedSystemSettings)
        }
    }


    fun openChannelSettings(
        channel: AeonNotificationChannelKey
    ) {
        notificationCenter.openChannelSettings(channel)

        viewModelScope.launch {
            eventsInternal.emit(AeonNotificationUiEvent.OpenedSystemSettings)
        }
    }


    fun openExactAlarmSettings() {
        notificationCenter.openExactAlarmSettings()

        viewModelScope.launch {
            eventsInternal.emit(AeonNotificationUiEvent.OpenedExactAlarmSettings)
        }
    }


    fun selectChannel(
        channel: AeonNotificationChannelKey?
    ) {
        localState.value = localState.value.copy(
            selectedChannel = channel
        )
    }


    // ----------------------------------------------------
    // Internal Helpers
    // ----------------------------------------------------

    private suspend fun refreshStateOnly() {
        val state = notificationCenter.state()

        localState.value = localState.value.copy(
            loading = false,
            centerState = state
        )
    }


    private fun launchOperation(
        successMessage: String? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            localState.value = localState.value.copy(
                working = true,
                error = null
            )

            try {
                block()

                localState.value = localState.value.copy(
                    working = false,
                    loading = false,
                    message = successMessage ?: localState.value.message,
                    error = null
                )

                successMessage?.let { message ->
                    eventsInternal.emit(
                        AeonNotificationUiEvent.Toast(message)
                    )
                }
            } catch (throwable: Throwable) {
                val message = throwable.message
                    ?: "Notification operation failed."

                localState.value = localState.value.copy(
                    working = false,
                    loading = false,
                    error = message
                )

                eventsInternal.emit(
                    AeonNotificationUiEvent.Toast(message)
                )
            }
        }
    }
}


// ----------------------------------------------------
// Result Message Helpers
// ----------------------------------------------------

private fun AeonNotificationCenterInitResult.toUiMessage(): String {
    return when (this) {
        is AeonNotificationCenterInitResult.Ready -> {
            "Notification center ready. $scheduledRuleCount rules scheduled."
        }

        is AeonNotificationCenterInitResult.Partial -> {
            message
        }

        is AeonNotificationCenterInitResult.Failed -> {
            reason
        }
    }
}


private fun AeonNotificationRuleEngineResult.toUiMessage(): String {
    return when (this) {
        is AeonNotificationRuleEngineResult.Scheduled -> {
            "Notification rule scheduled."
        }

        is AeonNotificationRuleEngineResult.Delayed -> {
            "Notification delayed because quiet hours or safety rules are active."
        }

        is AeonNotificationRuleEngineResult.Skipped -> {
            reason
        }

        is AeonNotificationRuleEngineResult.Suppressed -> {
            reason
        }

        is AeonNotificationRuleEngineResult.Failed -> {
            reason
        }
    }
}


private fun com.aeon.app.core.notifications.AeonNotificationCenterResult.toUiMessage(): String {
    return when (this) {
        is com.aeon.app.core.notifications.AeonNotificationCenterResult.Success -> message
        is com.aeon.app.core.notifications.AeonNotificationCenterResult.Blocked -> message
        is com.aeon.app.core.notifications.AeonNotificationCenterResult.Failed -> message
    }
}


// ----------------------------------------------------
// Preference Helpers
// ----------------------------------------------------

fun AeonNotificationPreferences.isCategoryEnabled(
    channel: AeonNotificationChannelKey
): Boolean {
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


fun AeonNotificationPreferences.withCategoryEnabled(
    channel: AeonNotificationChannelKey,
    enabled: Boolean
): AeonNotificationPreferences {
    return when (channel) {
        AeonNotificationChannelKey.DailyPlanning ->
            copy(dailyPlanningEnabled = enabled)

        AeonNotificationChannelKey.Tasks ->
            copy(taskRemindersEnabled = enabled)

        AeonNotificationChannelKey.Habits ->
            copy(habitRemindersEnabled = enabled)

        AeonNotificationChannelKey.Focus ->
            copy(focusRemindersEnabled = enabled)

        AeonNotificationChannelKey.Mood ->
            copy(moodCheckInsEnabled = enabled)

        AeonNotificationChannelKey.Health ->
            copy(healthRemindersEnabled = enabled)

        AeonNotificationChannelKey.Finance ->
            copy(financeRemindersEnabled = enabled)

        AeonNotificationChannelKey.Goals ->
            copy(goalRemindersEnabled = enabled)

        AeonNotificationChannelKey.AIInsights ->
            copy(aiInsightsEnabled = enabled)

        AeonNotificationChannelKey.Backup ->
            copy(backupNotificationsEnabled = enabled)

        AeonNotificationChannelKey.System ->
            this
    }
}
