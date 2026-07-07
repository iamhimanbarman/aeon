package com.aeon.app.data.sync

import android.content.Context
import androidx.core.content.edit
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aeon.app.data.auth.AuthRepository
import com.aeon.app.data.local.database.AeonDatabase
import com.aeon.app.data.local.database.entities.AeonSyncOutboxEntity
import com.aeon.app.data.repository.AeonRepositories
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

class AeonSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val database = AeonDatabase.getInstance(applicationContext)
        val repositories = AeonRepositories(database)

        if (!repositories.settings.isCloudBackupEnabled()) {
            return Result.success()
        }

        val session = AuthRepository(applicationContext).getFreshAuthenticatedSession()
            ?: return Result.success()

        val pending = repositories.sync.getPendingBatch(BATCH_LIMIT)
        val remoteClient = AeonSyncRemoteClient()
        val clientId = AeonSyncDeviceId.getOrCreate(applicationContext)
        return try {
            if (pending.isNotEmpty()) {
                val response = remoteClient.pushOutbox(
                    accessToken = session.accessToken,
                    clientId = clientId,
                    entries = pending
                )
                applyAcknowledgements(
                    repositories = repositories,
                    pending = pending,
                    acknowledgements = response.acknowledgements,
                    userId = session.user.id
                )
            }
            pullAndApply(
                database = database,
                repositories = repositories,
                remoteClient = remoteClient,
                accessToken = session.accessToken,
                userId = session.user.id,
                clientId = clientId
            )
            Result.success()
        } catch (exception: AeonSyncRemoteException) {
            if (exception.authFailure) {
                Result.success()
            } else if (exception.retryable) {
                Result.retry()
            } else {
                pending.forEach { entry ->
                    repositories.sync.markFailed(entry, exception.message ?: "Sync rejected.")
                }
                Result.failure()
            }
        }
    }

    private suspend fun pullAndApply(
        database: AeonDatabase,
        repositories: AeonRepositories,
        remoteClient: AeonSyncRemoteClient,
        accessToken: String,
        userId: String,
        clientId: String
    ) {
        val cursorStore = AeonSyncCursorStore(applicationContext)
        val applier = AeonSyncChangeApplier(database, repositories.sync)
        var cursor = cursorStore.getCursor()
        var page = 0
        var hasMore: Boolean

        do {
            val response = remoteClient.pullChanges(
                accessToken = accessToken,
                cursor = cursor,
                limit = PULL_LIMIT,
                entityTypes = AeonSyncSupportedEntities.values
            )
            applier.applyChanges(
                changes = response.changes,
                userId = userId,
                currentClientId = clientId
            )
            cursor = response.cursor
            cursorStore.setCursor(cursor)
            hasMore = response.hasMore
            page += 1
        } while (hasMore && page < MAX_PULL_PAGES)
    }

    private suspend fun applyAcknowledgements(
        repositories: AeonRepositories,
        pending: List<AeonSyncOutboxEntity>,
        acknowledgements: List<AeonSyncAcknowledgement>,
        userId: String
    ) {
        val entriesByIdempotencyKey = pending.associateBy { it.idempotencyKey }

        acknowledgements.forEach { acknowledgement ->
            val entry = entriesByIdempotencyKey[acknowledgement.idempotencyKey] ?: return@forEach

            when (acknowledgement.status) {
                "applied", "duplicate" -> {
                    val revision = acknowledgement.serverRevision
                    if (revision == null) {
                        repositories.sync.markFailed(entry, "Sync response missed revision.")
                    } else {
                        repositories.sync.markSynced(
                            entry = entry,
                            serverRevision = revision,
                            userId = userId
                        )
                    }
                }

                "conflict" -> {
                    repositories.sync.markConflict(
                        entry = entry,
                        serverPayloadJson = acknowledgement.serverPayloadJson ?: "{}",
                        serverRevision = acknowledgement.serverRevision,
                        serverDeletedAt = acknowledgement.serverDeletedAt?.let { deletedAt ->
                            runCatching { Instant.parse(deletedAt) }.getOrNull()
                        }
                    )
                }

                else -> repositories.sync.markFailed(entry, "Unknown sync acknowledgement.")
            }
        }
    }

    private companion object {
        const val BATCH_LIMIT = 50
        const val PULL_LIMIT = 200
        const val MAX_PULL_PAGES = 5
    }
}

object AeonSyncScheduler {
    private const val PERIODIC_WORK_NAME = "aeon_periodic_sync"
    private const val ONE_TIME_WORK_NAME = "aeon_sync_now"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val request = PeriodicWorkRequestBuilder<AeonSyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(syncConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun requestNow(context: Context) {
        val appContext = context.applicationContext
        val request = OneTimeWorkRequestBuilder<AeonSyncWorker>()
            .setConstraints(syncConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun syncConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}

object AeonSyncDeviceId {
    private const val PREFERENCES = "aeon_sync_device"
    private const val CLIENT_ID = "client_id"

    fun getOrCreate(context: Context): String {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        preferences.getString(CLIENT_ID, null)?.let { clientId ->
            if (clientId.isNotBlank()) return clientId
        }

        val clientId = "android_${UUID.randomUUID().toString().replace("-", "")}_${Instant.now().epochSecond}"
        preferences.edit {
            putString(CLIENT_ID, clientId)
        }
        return clientId
    }
}

private class AeonSyncCursorStore(
    context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun getCursor(): Long {
        return preferences.getLong(CURSOR, 0L)
    }

    fun setCursor(cursor: Long) {
        preferences.edit {
            putLong(CURSOR, cursor.coerceAtLeast(0L))
        }
    }

    private companion object {
        const val PREFERENCES = "aeon_sync_state"
        const val CURSOR = "global_cursor"
    }
}

private suspend fun com.aeon.app.data.repository.AeonSettingsRepository.isCloudBackupEnabled(): Boolean {
    return getSetting("cloud_backup_enabled")
        ?.settingValue
        ?.trim()
        ?.equals("true", ignoreCase = true) == true
}
