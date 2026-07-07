package com.aeon.app.data.sync

import android.content.Context
import com.aeon.app.data.auth.AuthRepository
import com.aeon.app.data.local.database.AeonDatabase
import com.aeon.app.data.local.database.entities.AeonSyncConflictEntity
import com.aeon.app.data.repository.AeonRepositories
import org.json.JSONObject

enum class AeonSyncConflictResolution(
    val wireValue: String
) {
    UseLocal("use_client"),
    UseServer("use_server"),
    Merged("merged")
}

class AeonSyncConflictResolveException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class AeonSyncConflictResolver(
    context: Context,
    private val database: AeonDatabase,
    private val repositories: AeonRepositories
) {
    private val appContext = context.applicationContext
    private val authRepository = AuthRepository(appContext)
    private val remoteClient = AeonSyncRemoteClient()

    suspend fun resolve(
        conflict: AeonSyncConflictEntity,
        resolution: AeonSyncConflictResolution,
        mergedPayloadJson: String? = null
    ) {
        val session = authRepository.getFreshAuthenticatedSession()
            ?: throw AeonSyncConflictResolveException("Sign in to resolve sync conflicts.")

        val payload = when (resolution) {
            AeonSyncConflictResolution.UseLocal -> conflict.localPayloadJson
            AeonSyncConflictResolution.UseServer -> null
            AeonSyncConflictResolution.Merged -> {
                val cleanPayload = mergedPayloadJson?.trim().orEmpty()
                if (cleanPayload.isBlank()) {
                    throw AeonSyncConflictResolveException("Merged payload is empty.")
                }
                validateJsonPayload(cleanPayload)
                cleanPayload
            }
        }

        val remoteResult = try {
            remoteClient.resolveConflict(
                accessToken = session.accessToken,
                clientId = AeonSyncDeviceId.getOrCreate(appContext),
                entityType = conflict.entityType,
                entityId = conflict.entityId,
                resolution = resolution.wireValue,
                payloadJson = payload,
                baseRevision = conflict.baseRevision
            )
        } catch (exception: AeonSyncRemoteException) {
            throw AeonSyncConflictResolveException(
                message = exception.message ?: "Unable to resolve conflict.",
                cause = exception
            )
        }

        if (resolution == AeonSyncConflictResolution.UseServer) {
            val applier = AeonSyncChangeApplier(database, repositories.sync)
            val serverDeletedAt = remoteResult.serverDeletedAt ?: conflict.serverDeletedAt?.toString()

            if (serverDeletedAt != null) {
                applier.applyResolvedServerDelete(
                    entityType = conflict.entityType,
                    entityId = conflict.entityId,
                    deletedAt = serverDeletedAt,
                    serverRevision = remoteResult.serverRevision ?: conflict.serverRevision,
                    userId = session.user.id
                )
            } else {
                applier.applyResolvedServerPayload(
                    entityType = conflict.entityType,
                    entityId = conflict.entityId,
                    payloadJson = remoteResult.serverPayloadJson ?: conflict.serverPayloadJson,
                    serverRevision = remoteResult.serverRevision ?: conflict.serverRevision,
                    userId = session.user.id
                )
            }
        } else {
            remoteResult.serverRevision?.let { revision ->
                repositories.sync.markPulled(
                    entityType = conflict.entityType,
                    entityId = conflict.entityId,
                    serverRevision = revision,
                    userId = session.user.id
                )
            }
        }

        repositories.sync.markConflictResolved(conflict.id)
    }

    private fun validateJsonPayload(payload: String) {
        runCatching {
            JSONObject(payload)
        }.getOrElse { throwable ->
            throw AeonSyncConflictResolveException("Merged payload must be valid JSON.", throwable)
        }
    }
}
