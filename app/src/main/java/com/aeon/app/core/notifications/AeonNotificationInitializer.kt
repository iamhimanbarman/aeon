package com.aeon.app.core.notifications

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/*
 * AEON NOTIFICATION INITIALIZER
 *
 * Purpose:
 * Production startup/bootstrap layer for Aeon's notification system.
 *
 * Handles:
 * - Channel creation on app start
 * - Default preference creation
 * - Default rule creation
 * - Safe rule scheduling
 * - Notification history pruning
 * - Foreground refresh after user returns from Android settings
 * - Startup throttling to avoid unnecessary scheduling work
 *
 * Senior Developer Rule:
 * Application.onCreate() should not directly call Repository, Scheduler,
 * Publisher, RuleEngine, Worker, or DAO.
 *
 * Application should only call AeonNotificationInitializer.install(...).
 */


// ----------------------------------------------------
// Startup Policy
// ----------------------------------------------------

data class AeonNotificationStartupPolicy(
    val createChannelsOnStart: Boolean = true,
    val initializeCenterOnStart: Boolean = true,
    val saveDefaultRulesIfMissing: Boolean = true,
    val scheduleEnabledRulesOnStart: Boolean = true,
    val scheduleThrottleMillis: Long = 6 * 60 * 60 * 1000L,
    val pruneHistoryOnStart: Boolean = true,
    val historyRetentionMillis: Long = 90L * 24L * 60L * 60L * 1000L,
    val refreshOnAppForeground: Boolean = true,
    val rescheduleOnForegroundWhenNeeded: Boolean = false
)


// ----------------------------------------------------
// Initializer State
// ----------------------------------------------------

data class AeonNotificationInitializerState(
    val installed: Boolean = false,
    val initialized: Boolean = false,
    val lastInitAtEpochMillis: Long? = null,
    val lastScheduleAtEpochMillis: Long? = null,
    val lastForegroundRefreshAtEpochMillis: Long? = null,
    val initResult: AeonNotificationCenterInitResult? = null,
    val lastError: String? = null
)


// ----------------------------------------------------
// Initializer Result
// ----------------------------------------------------

sealed interface AeonNotificationInitializerResult {

    data class Installed(
        val state: AeonNotificationInitializerState
    ) : AeonNotificationInitializerResult

    data class AlreadyInstalled(
        val state: AeonNotificationInitializerState
    ) : AeonNotificationInitializerResult

    data class Initialized(
        val state: AeonNotificationInitializerState,
        val centerResult: AeonNotificationCenterInitResult
    ) : AeonNotificationInitializerResult

    data class Failed(
        val state: AeonNotificationInitializerState,
        val reason: String,
        val throwable: Throwable? = null
    ) : AeonNotificationInitializerResult
}


// ----------------------------------------------------
// Main Initializer
// ----------------------------------------------------

class AeonNotificationInitializer private constructor(
    private val application: Application,
    private val policy: AeonNotificationStartupPolicy,
    private val config: AeonNotificationEngineConfig,
    private val appScope: CoroutineScope
) {

    private val appContext: Context =
        application.applicationContext

    private val installed = AtomicBoolean(false)

    private val preferences by lazy {
        appContext.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )
    }

    private val notificationCenter by lazy {
        AeonNotificationCenter.getInstance(
            context = appContext,
            config = config
        )
    }


    // ----------------------------------------------------
    // Install
    // ----------------------------------------------------

    fun install(): AeonNotificationInitializerResult {
        if (!installed.compareAndSet(false, true)) {
            return AeonNotificationInitializerResult.AlreadyInstalled(
                state = snapshot().copy(installed = true)
            )
        }

        if (policy.createChannelsOnStart) {
            AeonNotificationChannels.ensureCreated(appContext)
        }

        if (policy.refreshOnAppForeground) {
            registerForegroundObserver()
        }

        if (policy.initializeCenterOnStart) {
            appScope.launch {
                initializeInternal()
            }
        }

        return AeonNotificationInitializerResult.Installed(
            state = snapshot().copy(installed = true)
        )
    }


    // ----------------------------------------------------
    // Force Initialize
    // ----------------------------------------------------

    fun initializeNow() {
        appScope.launch {
            initializeInternal(force = true)
        }
    }


    fun refreshAfterPermissionChange() {
        appScope.launch {
            refreshInternal(
                rescheduleIfNeeded = policy.rescheduleOnForegroundWhenNeeded
            )
        }
    }


    // ----------------------------------------------------
    // Internal Init
    // ----------------------------------------------------

    private suspend fun initializeInternal(
        force: Boolean = false
    ): AeonNotificationInitializerResult {
        return withContext(Dispatchers.IO) {
            try {
                AeonNotificationChannels.ensureCreated(appContext)

                val shouldSchedule = force || shouldScheduleNow()

                val result = notificationCenter.initialize(
                    saveDefaultRulesIfMissing = policy.saveDefaultRulesIfMissing,
                    scheduleEnabledRules = policy.scheduleEnabledRulesOnStart && shouldSchedule
                )

                val now = System.currentTimeMillis()

                preferences.edit()
                    .putLong(KEY_LAST_INIT_AT, now)
                    .putString(KEY_LAST_INIT_RESULT, result.simpleName())
                    .apply()

                if (shouldSchedule) {
                    preferences.edit()
                        .putLong(KEY_LAST_SCHEDULE_AT, now)
                        .apply()
                }

                if (policy.pruneHistoryOnStart) {
                    pruneOldHistory()
                }

                AeonNotificationInitializerResult.Initialized(
                    state = snapshot().copy(
                        installed = true,
                        initialized = true,
                        initResult = result,
                        lastError = null
                    ),
                    centerResult = result
                )
            } catch (throwable: Throwable) {
                val message = throwable.message
                    ?: "Notification initializer failed."

                preferences.edit()
                    .putString(KEY_LAST_ERROR, message)
                    .apply()

                AeonNotificationInitializerResult.Failed(
                    state = snapshot().copy(
                        installed = true,
                        initialized = false,
                        lastError = message
                    ),
                    reason = message,
                    throwable = throwable
                )
            }
        }
    }


    // ----------------------------------------------------
    // Foreground Refresh
    // ----------------------------------------------------

    private fun registerForegroundObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {

                override fun onStart(owner: LifecycleOwner) {
                    appScope.launch {
                        refreshInternal(
                            rescheduleIfNeeded = policy.rescheduleOnForegroundWhenNeeded
                        )
                    }
                }
            }
        )
    }


    private suspend fun refreshInternal(
        rescheduleIfNeeded: Boolean
    ) {
        withContext(Dispatchers.IO) {
            try {
                AeonNotificationChannels.ensureCreated(appContext)

                val state = notificationCenter.state()

                if (rescheduleIfNeeded && state.preferences.masterEnabled && shouldScheduleNow()) {
                    notificationCenter.scheduleEnabledRules()

                    preferences.edit()
                        .putLong(KEY_LAST_SCHEDULE_AT, System.currentTimeMillis())
                        .apply()
                }

                preferences.edit()
                    .putLong(KEY_LAST_FOREGROUND_REFRESH_AT, System.currentTimeMillis())
                    .remove(KEY_LAST_ERROR)
                    .apply()
            } catch (throwable: Throwable) {
                preferences.edit()
                    .putString(
                        KEY_LAST_ERROR,
                        throwable.message ?: "Notification foreground refresh failed."
                    )
                    .apply()
            }
        }
    }


    // ----------------------------------------------------
    // History Pruning
    // ----------------------------------------------------

    private suspend fun pruneOldHistory() {
        val cutoff = System.currentTimeMillis() - policy.historyRetentionMillis

        notificationCenter.pruneOldHistory(
            beforeEpochMillis = cutoff
        )
    }


    // ----------------------------------------------------
    // Scheduling Throttle
    // ----------------------------------------------------

    private fun shouldScheduleNow(): Boolean {
        val lastScheduleAt = preferences.getLong(
            KEY_LAST_SCHEDULE_AT,
            0L
        )

        if (lastScheduleAt <= 0L) return true

        val elapsed = System.currentTimeMillis() - lastScheduleAt

        return elapsed >= policy.scheduleThrottleMillis
    }


    // ----------------------------------------------------
    // State
    // ----------------------------------------------------

    fun snapshot(): AeonNotificationInitializerState {
        return AeonNotificationInitializerState(
            installed = installed.get(),
            initialized = preferences.getLong(KEY_LAST_INIT_AT, 0L) > 0L,
            lastInitAtEpochMillis = preferences
                .getLong(KEY_LAST_INIT_AT, 0L)
                .takeIf { it > 0L },
            lastScheduleAtEpochMillis = preferences
                .getLong(KEY_LAST_SCHEDULE_AT, 0L)
                .takeIf { it > 0L },
            lastForegroundRefreshAtEpochMillis = preferences
                .getLong(KEY_LAST_FOREGROUND_REFRESH_AT, 0L)
                .takeIf { it > 0L },
            initResult = null,
            lastError = preferences.getString(KEY_LAST_ERROR, null)
        )
    }


    companion object {

        private const val PREF_NAME = "aeon_notification_initializer"

        private const val KEY_LAST_INIT_AT = "last_init_at"
        private const val KEY_LAST_SCHEDULE_AT = "last_schedule_at"
        private const val KEY_LAST_FOREGROUND_REFRESH_AT = "last_foreground_refresh_at"
        private const val KEY_LAST_INIT_RESULT = "last_init_result"
        private const val KEY_LAST_ERROR = "last_error"

        @Volatile
        private var INSTANCE: AeonNotificationInitializer? = null


        fun install(
            application: Application,
            policy: AeonNotificationStartupPolicy = AeonNotificationStartupPolicy(),
            config: AeonNotificationEngineConfig = AeonNotificationEngineConfig(),
            appScope: CoroutineScope = CoroutineScope(
                SupervisorJob() + Dispatchers.Default
            )
        ): AeonNotificationInitializerResult {
            val initializer = getInstance(
                application = application,
                policy = policy,
                config = config,
                appScope = appScope
            )

            return initializer.install()
        }


        fun getInstance(
            application: Application,
            policy: AeonNotificationStartupPolicy = AeonNotificationStartupPolicy(),
            config: AeonNotificationEngineConfig = AeonNotificationEngineConfig(),
            appScope: CoroutineScope = CoroutineScope(
                SupervisorJob() + Dispatchers.Default
            )
        ): AeonNotificationInitializer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AeonNotificationInitializer(
                    application = application,
                    policy = policy,
                    config = config,
                    appScope = appScope
                ).also { initializer ->
                    INSTANCE = initializer
                }
            }
        }
    }
}


// ----------------------------------------------------
// Result Helper
// ----------------------------------------------------

private fun AeonNotificationCenterInitResult.simpleName(): String {
    return when (this) {
        is AeonNotificationCenterInitResult.Ready -> "Ready"
        is AeonNotificationCenterInitResult.Partial -> "Partial"
        is AeonNotificationCenterInitResult.Failed -> "Failed"
    }
}
