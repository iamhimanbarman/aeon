package com.aeon.app.core.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/*
 * AEON NOTIFICATION DEEP LINK HANDLER
 *
 * Purpose:
 * Converts notification tap/action intents into clean navigation events.
 *
 * Handles:
 * - Notification tap
 * - Notification action button click
 * - Deep-link route extraction
 * - Payload/action metadata extraction
 * - Notification history update
 * - Safe route normalization
 *
 * Senior Developer Rule:
 * Core notification code should not directly own NavController.
 * It should emit route events, and UI/navigation layer should collect them.
 */


// ----------------------------------------------------
// Deep Link Origin
// ----------------------------------------------------

enum class AeonNotificationDeepLinkOrigin {
    NotificationTap,
    NotificationAction,
    LauncherIntent,
    Unknown
}


// ----------------------------------------------------
// Deep Link Event
// ----------------------------------------------------

data class AeonNotificationDeepLinkEvent(
    val eventId: String,
    val origin: AeonNotificationDeepLinkOrigin,
    val payloadId: String?,
    val notificationId: Int?,
    val type: AeonNotificationType?,
    val source: AeonNotificationSource?,
    val sourceId: String?,
    val route: String?,
    val actionId: String?,
    val groupKey: String?,
    val receivedAtEpochMillis: Long = System.currentTimeMillis()
) {
    val canNavigate: Boolean
        get() = !route.isNullOrBlank()

    val hasPayload: Boolean
        get() = !payloadId.isNullOrBlank()
}


// ----------------------------------------------------
// Deep Link Handling Result
// ----------------------------------------------------

sealed interface AeonNotificationDeepLinkResult {

    data class Handled(
        val event: AeonNotificationDeepLinkEvent
    ) : AeonNotificationDeepLinkResult

    data class Ignored(
        val reason: String
    ) : AeonNotificationDeepLinkResult

    data class Failed(
        val reason: String,
        val throwable: Throwable? = null
    ) : AeonNotificationDeepLinkResult
}


// ----------------------------------------------------
// Main Handler
// ----------------------------------------------------

class AeonNotificationDeepLinkHandler private constructor(
    private val repository: AeonNotificationRepository?
) {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private val _events = MutableSharedFlow<AeonNotificationDeepLinkEvent>(
        replay = 1,
        extraBufferCapacity = 32
    )

    val events: SharedFlow<AeonNotificationDeepLinkEvent> =
        _events.asSharedFlow()


    // ----------------------------------------------------
    // Handle Intent
    // ----------------------------------------------------

    fun handleIntent(
        intent: Intent?
    ): AeonNotificationDeepLinkResult {
        if (intent == null) {
            return AeonNotificationDeepLinkResult.Ignored(
                reason = "Intent is null."
            )
        }

        return try {
            val event = intent.toAeonNotificationDeepLinkEvent()
                ?: return AeonNotificationDeepLinkResult.Ignored(
                    reason = "Intent does not contain Aeon notification navigation data."
                )

            _events.tryEmit(event)

            persistEvent(event)

            AeonNotificationDeepLinkResult.Handled(event)
        } catch (throwable: Throwable) {
            AeonNotificationDeepLinkResult.Failed(
                reason = throwable.message ?: "Failed to handle notification deep link.",
                throwable = throwable
            )
        }
    }


    // ----------------------------------------------------
    // Persist Event
    // ----------------------------------------------------

    private fun persistEvent(
        event: AeonNotificationDeepLinkEvent
    ) {
        val payloadId = event.payloadId
            ?: return

        val repo = repository
            ?: return

        scope.launch {
            when (event.origin) {
                AeonNotificationDeepLinkOrigin.NotificationTap,
                AeonNotificationDeepLinkOrigin.NotificationAction -> {
                    repo.markTapped(payloadId)
                }

                else -> Unit
            }
        }
    }


    companion object {

        @Volatile
        private var INSTANCE: AeonNotificationDeepLinkHandler? = null


        fun getInstance(
            context: Context
        ): AeonNotificationDeepLinkHandler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AeonNotificationDeepLinkHandler(
                    repository = RoomAeonNotificationRepository.create(context)
                ).also { handler ->
                    INSTANCE = handler
                }
            }
        }


        fun createWithoutRepository(): AeonNotificationDeepLinkHandler {
            return AeonNotificationDeepLinkHandler(
                repository = null
            )
        }


        fun create(
            repository: AeonNotificationRepository
        ): AeonNotificationDeepLinkHandler {
            return AeonNotificationDeepLinkHandler(
                repository = repository
            )
        }
    }
}


// ----------------------------------------------------
// Intent Parser
// ----------------------------------------------------

fun Intent.toAeonNotificationDeepLinkEvent(): AeonNotificationDeepLinkEvent? {
    val payloadId = getStringExtra(
        AeonNotificationIntentContract.EXTRA_PAYLOAD_ID
    )?.takeIf { it.isNotBlank() }

    val rawDeepLinkRoute = getStringExtra(
        AeonNotificationIntentContract.EXTRA_DEEP_LINK_ROUTE
    )

    val rawActionRoute = getStringExtra(
        AeonNotificationIntentContract.EXTRA_ACTION_ROUTE
    )

    val actionId = getStringExtra(
        AeonNotificationIntentContract.EXTRA_ACTION_ID
    )?.takeIf { it.isNotBlank() }

    val origin = resolveAeonNotificationDeepLinkOrigin()

    val route = when (origin) {
        AeonNotificationDeepLinkOrigin.NotificationAction -> {
            rawActionRoute.normalizedAeonRoute()
                ?: rawDeepLinkRoute.normalizedAeonRoute()
        }

        else -> rawDeepLinkRoute.normalizedAeonRoute()
    }

    if (payloadId.isNullOrBlank() && route.isNullOrBlank()) {
        return null
    }

    val notificationId = if (
        hasExtra(AeonNotificationIntentContract.EXTRA_NOTIFICATION_ID)
    ) {
        getIntExtra(
            AeonNotificationIntentContract.EXTRA_NOTIFICATION_ID,
            0
        ).takeIf { it != 0 }
    } else {
        null
    }

    val type = enumValueOrNull<AeonNotificationType>(
        getStringExtra(
            AeonNotificationIntentContract.EXTRA_NOTIFICATION_TYPE
        )
    )

    val source = enumValueOrNull<AeonNotificationSource>(
        getStringExtra(
            AeonNotificationIntentContract.EXTRA_SOURCE
        )
    )

    val sourceId = getStringExtra(
        AeonNotificationIntentContract.EXTRA_SOURCE_ID
    )?.takeIf { it.isNotBlank() }

    val groupKey = getStringExtra(
        AeonNotificationIntentContract.EXTRA_GROUP_KEY
    )?.takeIf { it.isNotBlank() }

    return AeonNotificationDeepLinkEvent(
        eventId = buildAeonDeepLinkEventId(
            payloadId = payloadId,
            route = route,
            actionId = actionId
        ),
        origin = origin,
        payloadId = payloadId,
        notificationId = notificationId,
        type = type,
        source = source,
        sourceId = sourceId,
        route = route,
        actionId = actionId,
        groupKey = groupKey
    )
}


// ----------------------------------------------------
// Origin Resolver
// ----------------------------------------------------

private fun Intent.resolveAeonNotificationDeepLinkOrigin(): AeonNotificationDeepLinkOrigin {
    return when (action) {
        AeonNotificationIntentContract.ACTION_NOTIFICATION_TAP -> {
            AeonNotificationDeepLinkOrigin.NotificationTap
        }

        AeonNotificationIntentContract.ACTION_NOTIFICATION_ACTION -> {
            AeonNotificationDeepLinkOrigin.NotificationAction
        }

        Intent.ACTION_MAIN -> {
            if (
                hasExtra(AeonNotificationIntentContract.EXTRA_DEEP_LINK_ROUTE) ||
                hasExtra(AeonNotificationIntentContract.EXTRA_PAYLOAD_ID)
            ) {
                AeonNotificationDeepLinkOrigin.LauncherIntent
            } else {
                AeonNotificationDeepLinkOrigin.Unknown
            }
        }

        else -> {
            if (
                hasExtra(AeonNotificationIntentContract.EXTRA_DEEP_LINK_ROUTE) ||
                hasExtra(AeonNotificationIntentContract.EXTRA_PAYLOAD_ID)
            ) {
                AeonNotificationDeepLinkOrigin.LauncherIntent
            } else {
                AeonNotificationDeepLinkOrigin.Unknown
            }
        }
    }
}


// ----------------------------------------------------
// Route Normalizer
// ----------------------------------------------------

private fun String?.normalizedAeonRoute(): String? {
    val raw = this
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null

    return when {
        raw.startsWith("aeon://", ignoreCase = true) -> {
            val uri = Uri.parse(raw)

            val path = uri.encodedPath
                ?.trim('/')
                .orEmpty()

            val query = uri.encodedQuery

            when {
                path.isBlank() -> null
                query.isNullOrBlank() -> path
                else -> "$path?$query"
            }
        }

        raw.startsWith("/") -> {
            raw.trimStart('/')
        }

        else -> raw
    }
}


// ----------------------------------------------------
// Event ID
// ----------------------------------------------------

private fun buildAeonDeepLinkEventId(
    payloadId: String?,
    route: String?,
    actionId: String?
): String {
    val seed = listOfNotNull(
        payloadId,
        route,
        actionId,
        System.currentTimeMillis().toString()
    ).joinToString("_")

    return "notification_event_${aeonStableNotificationId(seed)}"
}


// ----------------------------------------------------
// Enum Helper
// ----------------------------------------------------

private inline fun <reified T : Enum<T>> enumValueOrNull(
    value: String?
): T? {
    return try {
        if (value.isNullOrBlank()) null else enumValueOf<T>(value)
    } catch (_: Throwable) {
        null
    }
}
