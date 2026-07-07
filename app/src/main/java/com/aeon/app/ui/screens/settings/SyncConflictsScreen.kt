package com.aeon.app.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aeon.app.data.local.database.entities.AeonSyncConflictEntity
import com.aeon.app.data.sync.AeonSyncConflictResolution
import com.aeon.app.data.sync.AeonSyncConflictResolver
import com.aeon.app.di.currentAeonAppContainer
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.core.AeonTextField
import com.aeon.app.ui.components.core.AeonTextFieldVariant
import com.aeon.app.ui.components.feedback.AeonNoDataState
import com.aeon.app.ui.components.feedback.LocalAeonToastHostState
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.components.layout.aeonPremiumBackgroundBrush
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SyncConflictsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val container = currentAeonAppContainer()
    val context = LocalContext.current.applicationContext
    val conflicts by container.repositories.sync
        .observeUnresolvedConflicts()
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val toastHost = LocalAeonToastHostState.current
    val resolver = remember(context, container) {
        AeonSyncConflictResolver(
            context = context,
            database = container.database,
            repositories = container.repositories
        )
    }

    var resolvingConflictId by rememberSaveable { mutableStateOf<String?>(null) }

    AeonScreen(
        modifier = modifier,
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Sync Conflicts",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "Choose the trusted copy for records edited on multiple devices.",
                    style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                )
            }

            AeonChip(
                text = "${conflicts.size} open",
                variant = if (conflicts.isEmpty()) AeonChipVariant.Success else AeonChipVariant.Warning,
                size = AeonChipSize.Compact
            )
        }

        if (conflicts.isEmpty()) {
            AeonNoDataState(
                title = "No sync conflicts",
                message = "Aeon has no records waiting for manual merge.",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.CloudDone,
                        contentDescription = null
                    )
                }
            )
        } else {
            conflicts.forEach { conflict ->
                SyncConflictCard(
                    conflict = conflict,
                    resolving = resolvingConflictId == conflict.id,
                    onResolve = { resolution, mergedPayload ->
                        scope.launch {
                            resolvingConflictId = conflict.id
                            runCatching {
                                resolver.resolve(
                                    conflict = conflict,
                                    resolution = resolution,
                                    mergedPayloadJson = mergedPayload
                                )
                            }.onSuccess {
                                toastHost.showSuccess("Conflict resolved")
                            }.onFailure { throwable ->
                                toastHost.showError(
                                    title = "Resolve failed",
                                    message = throwable.message?.take(48)
                                )
                            }
                            resolvingConflictId = null
                        }
                    }
                )
            }
        }

        AeonButton(
            text = "Back",
            onClick = onBack,
            variant = AeonButtonVariant.Ghost,
            size = AeonButtonSize.Small
        )
    }
}

@Composable
private fun SyncConflictCard(
    conflict: AeonSyncConflictEntity,
    resolving: Boolean,
    onResolve: (AeonSyncConflictResolution, String?) -> Unit
) {
    val colors = AeonThemeTokens.colors
    var expanded by rememberSaveable(conflict.id) { mutableStateOf(false) }
    var mergeMode by rememberSaveable(conflict.id) { mutableStateOf(false) }
    var mergedPayload by rememberSaveable(conflict.id) {
        mutableStateOf(conflict.localPayloadJson.prettyJson())
    }

    AeonCard(
        variant = AeonCardVariant.Glass,
        borderColor = colors.warning.copy(alpha = 0.32f),
        contentPadding = PaddingValues(12.dp),
        onClick = { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.SyncProblem,
                contentDescription = null,
                tint = colors.warning
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = conflict.entityType.displayEntityType(),
                    style = AeonTextStyles.CardTitle.copy(color = colors.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${conflict.entityId.take(18)} · ${conflict.detectedAt.formatConflictTime()}",
                    style = AeonTextStyles.Micro.copy(color = colors.textTertiary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AeonChip(
                text = "Review",
                variant = AeonChipVariant.Warning,
                size = AeonChipSize.Compact
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PayloadPreviewCard(
                        title = "Local",
                        payload = conflict.localPayloadJson,
                        accent = colors.focus,
                        modifier = Modifier.weight(1f)
                    )
                    PayloadPreviewCard(
                        title = "Cloud",
                        payload = if (conflict.serverDeletedAt == null) {
                            conflict.serverPayloadJson
                        } else {
                            """{"status":"deleted","deletedAt":"${conflict.serverDeletedAt}"}"""
                        },
                        accent = colors.calm,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (mergeMode) {
                    AeonTextField(
                        value = mergedPayload,
                        onValueChange = { mergedPayload = it },
                        label = "Merged JSON",
                        placeholder = "Edit final payload",
                        variant = AeonTextFieldVariant.Glass,
                        singleLine = false,
                        minLines = 5,
                        maxLines = 9,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.CallMerge,
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AeonButton(
                        text = "Use cloud",
                        onClick = { onResolve(AeonSyncConflictResolution.UseServer, null) },
                        modifier = Modifier.weight(1f),
                        variant = AeonButtonVariant.Secondary,
                        size = AeonButtonSize.Small,
                        loading = resolving
                    )
                    AeonButton(
                        text = "Keep local",
                        onClick = { onResolve(AeonSyncConflictResolution.UseLocal, null) },
                        modifier = Modifier.weight(1f),
                        variant = AeonButtonVariant.Primary,
                        size = AeonButtonSize.Small,
                        loading = resolving
                    )
                }

                AeonButton(
                    text = if (mergeMode) "Send merged" else "Merge manually",
                    onClick = {
                        if (mergeMode) {
                            onResolve(AeonSyncConflictResolution.Merged, mergedPayload)
                        } else {
                            mergeMode = true
                        }
                    },
                    variant = AeonButtonVariant.Tonal,
                    size = AeonButtonSize.Small,
                    fullWidth = true,
                    loading = resolving,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Devices,
                            contentDescription = null,
                            tint = Color.Unspecified
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun PayloadPreviewCard(
    title: String,
    payload: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        containerColor = colors.surface.copy(alpha = 0.86f),
        borderColor = accent.copy(alpha = 0.26f)
    ) {
        Text(
            text = title,
            style = AeonTextStyles.Micro.copy(color = accent, fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = payload.compactJsonPreview(),
            style = AeonTextStyles.Caption.copy(color = colors.textSecondary),
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun String.prettyJson(): String {
    return runCatching {
        JSONObject(this).toString(2)
    }.getOrDefault(this)
}

private fun String.compactJsonPreview(): String {
    return runCatching {
        JSONObject(this).toString()
    }.getOrDefault(this)
}

private fun String.displayEntityType(): String {
    return split("_")
        .filter(String::isNotBlank)
        .joinToString(" ") { part -> part.replaceFirstChar(Char::uppercase) }
}

private fun java.time.Instant.formatConflictTime(): String {
    return DateTimeFormatter.ofPattern("d MMM, h:mm a")
        .withZone(ZoneId.systemDefault())
        .format(this)
}
