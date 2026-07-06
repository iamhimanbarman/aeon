package com.aeon.app.ui.components.feedback

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.theme.AeonComponentShapes
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonEasing
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

private const val TOAST_DUPLICATE_GUARD_MS = 900L

enum class AeonToastType {
    Success,
    Error,
    Warning,
    Info,
    Neutral,
    Loading,
    Offline,
    Sync
}

enum class AeonToastDuration(
    val millis: Long?
) {
    Short(2_200L),
    Normal(3_200L),
    Long(5_000L),
    Indefinite(null)
}

enum class AeonToastResult {
    Dismissed,
    ActionPerformed
}

@Immutable
data class AeonToastVisuals(
    val title: String,
    val message: String? = null,
    val type: AeonToastType = AeonToastType.Info,
    val duration: AeonToastDuration = AeonToastDuration.Normal,
    val actionLabel: String? = null,
    val dismissLabel: String = "Dismiss",
    val withDismissAction: Boolean = true
)

@Stable
class AeonToastData internal constructor(
    val visuals: AeonToastVisuals,
    private val continuation: CancellableContinuation<AeonToastResult>
) {
    fun performAction() {
        if (continuation.isActive) {
            continuation.resume(AeonToastResult.ActionPerformed)
        }
    }

    fun dismiss() {
        if (continuation.isActive) {
            continuation.resume(AeonToastResult.Dismissed)
        }
    }
}

@Stable
class AeonToastHostState {

    private val mutex = Mutex()
    private var lastToastKey: String = ""
    private var lastToastAtMillis: Long = 0L

    var currentToastData by mutableStateOf<AeonToastData?>(null)
        private set

    suspend fun showToast(
        visuals: AeonToastVisuals
    ): AeonToastResult {
        val now = SystemClock.elapsedRealtime()
        val toastKey = visuals.dedupeKey()

        if (
            toastKey == lastToastKey &&
            now - lastToastAtMillis < TOAST_DUPLICATE_GUARD_MS
        ) {
            return AeonToastResult.Dismissed
        }

        lastToastKey = toastKey
        lastToastAtMillis = now

        return mutex.withLock {
            try {
                suspendCancellableCoroutine { continuation ->
                    currentToastData = AeonToastData(
                        visuals = visuals,
                        continuation = continuation
                    )

                    continuation.invokeOnCancellation {
                        currentToastData = null
                    }
                }
            } finally {
                currentToastData = null
            }
        }
    }

    suspend fun showSuccess(
        title: String,
        message: String? = null,
        duration: AeonToastDuration = AeonToastDuration.Normal
    ): AeonToastResult {
        return showToast(
            AeonToastVisuals(
                title = title,
                message = message,
                type = AeonToastType.Success,
                duration = duration
            )
        )
    }

    suspend fun showError(
        title: String,
        message: String? = null,
        actionLabel: String? = null,
        duration: AeonToastDuration = AeonToastDuration.Long
    ): AeonToastResult {
        return showToast(
            AeonToastVisuals(
                title = title,
                message = message,
                type = AeonToastType.Error,
                duration = duration,
                actionLabel = actionLabel
            )
        )
    }

    suspend fun showWarning(
        title: String,
        message: String? = null,
        duration: AeonToastDuration = AeonToastDuration.Long
    ): AeonToastResult {
        return showToast(
            AeonToastVisuals(
                title = title,
                message = message,
                type = AeonToastType.Warning,
                duration = duration
            )
        )
    }

    suspend fun showInfo(
        title: String,
        message: String? = null,
        duration: AeonToastDuration = AeonToastDuration.Normal
    ): AeonToastResult {
        return showToast(
            AeonToastVisuals(
                title = title,
                message = message,
                type = AeonToastType.Info,
                duration = duration
            )
        )
    }

    suspend fun showNeutral(
        title: String,
        message: String? = null,
        duration: AeonToastDuration = AeonToastDuration.Normal
    ): AeonToastResult {
        return showToast(
            AeonToastVisuals(
                title = title,
                message = message,
                type = AeonToastType.Neutral,
                duration = duration
            )
        )
    }

    suspend fun showLoading(
        title: String,
        message: String? = null
    ): AeonToastResult {
        return showToast(
            AeonToastVisuals(
                title = title,
                message = message,
                type = AeonToastType.Loading,
                duration = AeonToastDuration.Indefinite,
                withDismissAction = false
            )
        )
    }

    suspend fun showOffline(
        title: String = "You are offline",
        message: String? = "Aeon will continue working locally.",
        duration: AeonToastDuration = AeonToastDuration.Long
    ): AeonToastResult {
        return showToast(
            AeonToastVisuals(
                title = title,
                message = message,
                type = AeonToastType.Offline,
                duration = duration
            )
        )
    }

    suspend fun showSync(
        title: String = "Syncing",
        message: String? = "Your local changes are being prepared.",
        duration: AeonToastDuration = AeonToastDuration.Normal
    ): AeonToastResult {
        return showToast(
            AeonToastVisuals(
                title = title,
                message = message,
                type = AeonToastType.Sync,
                duration = duration
            )
        )
    }

    fun dismissCurrent() {
        currentToastData?.dismiss()
    }
}

val LocalAeonToastHostState = staticCompositionLocalOf<AeonToastHostState> {
    error("No AeonToastHostState provided.")
}

@Composable
fun rememberAeonToastHostState(): AeonToastHostState {
    return remember {
        AeonToastHostState()
    }
}

@Composable
fun AeonToastProvider(
    hostState: AeonToastHostState = rememberAeonToastHostState(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAeonToastHostState provides hostState,
        content = content
    )
}

@Composable
fun AeonToastHost(
    hostState: AeonToastHostState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    maxWidth: Dp = 520.dp
) {
    val toastData = hostState.currentToastData
    val accessibilityManager = LocalAccessibilityManager.current

    LaunchedEffect(toastData) {
        val current = toastData ?: return@LaunchedEffect
        val baseDuration = current.visuals.duration.millis ?: return@LaunchedEffect

        val adjustedDuration = accessibilityManager
            ?.calculateRecommendedTimeoutMillis(
                originalTimeoutMillis = baseDuration,
                containsIcons = true,
                containsText = true,
                containsControls = current.visuals.actionLabel != null
            )
            ?: baseDuration

        delay(adjustedDuration)
        current.dismiss()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = AeonSpacing.Small,
                end = AeonSpacing.Small,
                bottom = bottomPadding + AeonSpacing.Small
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = toastData != null,
            enter = aeonToastEnterTransition(),
            exit = aeonToastExitTransition()
        ) {
            toastData?.let { data ->
                AeonToastSurface(
                    data = data,
                    maxWidth = maxWidth
                )
            }
        }
    }
}

@Composable
private fun AeonToastSurface(
    data: AeonToastData,
    maxWidth: Dp
) {
    val colors = AeonThemeTokens.colors
    val token = aeonToastToken(data.visuals.type)

    Surface(
        modifier = Modifier
            .widthIn(max = maxWidth)
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                liveRegion = when (data.visuals.type) {
                    AeonToastType.Error,
                    AeonToastType.Warning,
                    AeonToastType.Offline -> LiveRegionMode.Assertive
                    else -> LiveRegionMode.Polite
                }
            },
        shape = AeonComponentShapes.CardHero,
        color = colors.surfaceElevated.copy(
            alpha = if (colors.isDark) 0.96f else 0.98f
        ),
        tonalElevation = 6.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(
            width = 1.dp,
            color = token.accentColor.copy(
                alpha = if (colors.isDark) 0.20f else 0.14f
            )
        )
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 46.dp)
                .padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AeonToastIcon(
                token = token,
                type = data.visuals.type
            )

            Text(
                text = data.visuals.compactText(),
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1_200
                    ),
                style = AeonTextStyles.ButtonMedium,
                color = colors.textPrimary,
                maxLines = 1
            )

            data.visuals.actionLabel?.takeIf { it.isNotBlank() }?.let { label ->
                TextButton(
                    onClick = data::performAction,
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = label,
                        style = AeonTextStyles.ButtonMedium,
                        color = token.accentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AeonToastIcon(
    token: AeonToastToken,
    type: AeonToastType
) {
    Surface(
        modifier = Modifier.size(28.dp),
        shape = AeonComponentShapes.IconButtonCircle,
        color = token.accentColor.copy(alpha = token.containerAlpha),
        contentColor = token.accentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (type == AeonToastType.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = token.accentColor
                )
            } else {
                Icon(
                    imageVector = token.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = token.accentColor
                )
            }
        }
    }
}

@Immutable
private data class AeonToastToken(
    val accentColor: Color,
    val icon: ImageVector,
    val containerAlpha: Float
)

@Composable
private fun aeonToastToken(
    type: AeonToastType
): AeonToastToken {
    val colors = AeonThemeTokens.colors

    return when (type) {
        AeonToastType.Success -> AeonToastToken(
            accentColor = colors.habit,
            icon = Icons.Rounded.CheckCircle,
            containerAlpha = if (colors.isDark) 0.20f else 0.14f
        )

        AeonToastType.Error -> AeonToastToken(
            accentColor = colors.error,
            icon = Icons.Rounded.Error,
            containerAlpha = if (colors.isDark) 0.22f else 0.14f
        )

        AeonToastType.Warning -> AeonToastToken(
            accentColor = colors.premiumGold,
            icon = Icons.Rounded.Warning,
            containerAlpha = if (colors.isDark) 0.22f else 0.15f
        )

        AeonToastType.Info -> AeonToastToken(
            accentColor = colors.brand,
            icon = Icons.Rounded.Info,
            containerAlpha = if (colors.isDark) 0.20f else 0.13f
        )

        AeonToastType.Neutral -> AeonToastToken(
            accentColor = colors.textSecondary,
            icon = Icons.Rounded.Info,
            containerAlpha = if (colors.isDark) 0.18f else 0.10f
        )

        AeonToastType.Loading -> AeonToastToken(
            accentColor = colors.focus,
            icon = Icons.Rounded.Sync,
            containerAlpha = if (colors.isDark) 0.20f else 0.13f
        )

        AeonToastType.Offline -> AeonToastToken(
            accentColor = colors.premiumGold,
            icon = Icons.Rounded.CloudOff,
            containerAlpha = if (colors.isDark) 0.22f else 0.15f
        )

        AeonToastType.Sync -> AeonToastToken(
            accentColor = colors.focus,
            icon = Icons.Rounded.Sync,
            containerAlpha = if (colors.isDark) 0.20f else 0.13f
        )
    }
}

private fun aeonToastEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Decelerate
        )
    ) + slideInVertically(
        initialOffsetY = { height -> height / 3 },
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Decelerate
        )
    ) + scaleIn(
        initialScale = 0.975f,
        animationSpec = tween(
            durationMillis = AeonDuration.Fast,
            easing = AeonEasing.Decelerate
        )
    )
}

private fun aeonToastExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = AeonDuration.UltraFast,
            easing = AeonEasing.Accelerate
        )
    ) + slideOutVertically(
        targetOffsetY = { height -> height / 4 },
        animationSpec = tween(
            durationMillis = AeonDuration.UltraFast,
            easing = AeonEasing.Accelerate
        )
    ) + scaleOut(
        targetScale = 0.98f,
        animationSpec = tween(
            durationMillis = AeonDuration.UltraFast,
            easing = AeonEasing.Accelerate
        )
    )
}

private fun AeonToastVisuals.compactText(): String =
    title
        .toSingleLineToastText()
        .ifBlank { message.orEmpty().toSingleLineToastText() }

private fun AeonToastVisuals.dedupeKey(): String =
    listOf(
        type.name,
        title.toSingleLineToastText(),
        message.orEmpty().toSingleLineToastText(),
        actionLabel.orEmpty().toSingleLineToastText()
    ).joinToString("|")

private fun String.toSingleLineToastText(): String =
    replace(Regex("\\s+"), " ").trim()
