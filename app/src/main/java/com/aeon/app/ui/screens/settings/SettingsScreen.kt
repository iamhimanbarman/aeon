package com.aeon.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aeon.app.BuildConfig
import com.aeon.app.data.auth.AuthSessionState
import com.aeon.app.di.currentAeonAppContainer
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipSize
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.core.AeonListItem
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.components.layout.aeonPremiumBackgroundBrush
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import kotlinx.coroutines.launch

@Composable
fun AeonSettingsRoute(
    onOpenHomeControl: () -> Unit = {},
    onOpenTrackControl: () -> Unit = {},
    onOpenFocusControl: () -> Unit = {},
    onOpenInsightsControl: () -> Unit = {},
    onOpenFinanceControl: () -> Unit = {},
    onOpenAiControl: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    onOpenPrivacySettings: () -> Unit = {},
    onOpenAppearanceSettings: () -> Unit = {},
    onOpenDataBackupSettings: () -> Unit = {},
    onOpenSyncConflicts: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors
    val container = currentAeonAppContainer()
    val authState by container.authRepository.sessionState.collectAsState()
    val scope = rememberCoroutineScope()
    var authActionLoading by rememberSaveable { mutableStateOf(false) }

    val showLoginButton = authState is AuthSessionState.Guest
    val authenticatedSession = (authState as? AuthSessionState.Authenticated)?.session
    val accountTitle = when {
        authenticatedSession?.user?.displayName?.isNotBlank() == true -> authenticatedSession.user.displayName.orEmpty()
        authenticatedSession != null -> authenticatedSession.user.email
        else -> "Guest workspace"
    }
    val accountSubtitle = when {
        authenticatedSession != null -> "Signed in as ${authenticatedSession.user.email}"
        else -> "Local-only mode. Your private data stays on this device until you sign in."
    }
    val identityLabel = if (authenticatedSession != null) "Connected" else "Guest"
    val syncLabel = if (authenticatedSession != null) "Cloud-ready" else "Device only"
    val authOriginLabel = if (BuildConfig.AUTH_BASE_URL.isBlank()) "No backend" else "Render auth"

    fun openLogin() {
        scope.launch {
            authActionLoading = true
            runCatching { container.authRepository.signOut() }
            authActionLoading = false
        }
    }

    fun signOut() {
        scope.launch {
            authActionLoading = true
            runCatching { container.authRepository.signOut() }
            authActionLoading = false
        }
    }

    val hubCards = remember(
        onOpenHomeControl,
        onOpenTrackControl,
        onOpenFocusControl,
        onOpenInsightsControl,
        onOpenFinanceControl,
        onOpenAiControl
    ) {
        listOf(
            SettingsHubCard(
                title = "Home",
                subtitle = "Daily command center and personal flow.",
                icon = Icons.Outlined.Home,
                accent = colors.brand,
                onClick = onOpenHomeControl
            ),
            SettingsHubCard(
                title = "Track",
                subtitle = "Habits, health, mood, and personal signals.",
                icon = Icons.Outlined.Timeline,
                accent = colors.health,
                onClick = onOpenTrackControl
            ),
            SettingsHubCard(
                title = "Focus",
                subtitle = "Sessions, routines, notifications, and timing.",
                icon = Icons.Outlined.CenterFocusStrong,
                accent = colors.focus,
                onClick = onOpenFocusControl
            ),
            SettingsHubCard(
                title = "Insights",
                subtitle = "Daily brief, trends, and reflection surfaces.",
                icon = Icons.Outlined.AutoGraph,
                accent = colors.intelligence,
                onClick = onOpenInsightsControl
            ),
            SettingsHubCard(
                title = "Finance",
                subtitle = "Budgets, categories, entry behavior, and import flow.",
                icon = Icons.Outlined.AccountBalanceWallet,
                accent = colors.finance,
                onClick = onOpenFinanceControl
            ),
            SettingsHubCard(
                title = "AI",
                subtitle = "Assistant behavior, model access, and connection state.",
                icon = Icons.Outlined.SmartToy,
                accent = colors.ai,
                onClick = onOpenAiControl
            )
        )
    }

    AeonScreen(
        modifier = modifier,
        backgroundBrush = aeonPremiumBackgroundBrush(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsHeaderRow(
            showLoginButton = showLoginButton,
            loginLoading = authActionLoading,
            onLoginClick = ::openLogin
        )

        AeonCard(
            variant = AeonCardVariant.Hero,
            backgroundBrush = Brush.linearGradient(
                colors = listOf(
                    colors.brand.copy(alpha = 0.22f),
                    colors.intelligence.copy(alpha = 0.18f),
                    colors.finance.copy(alpha = 0.20f)
                )
            ),
            borderColor = colors.border
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            authenticatedSession?.user?.displayName?.isNullOrBlank() == false ->
                                authenticatedSession.user.displayName.trim().first().uppercase()
                            authenticatedSession != null -> authenticatedSession.user.email.first().uppercase()
                            else -> "G"
                        },
                        style = AeonTextStyles.SectionTitle.copy(fontWeight = FontWeight.SemiBold),
                        color = if (authenticatedSession != null) colors.brand else colors.warning
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = accountTitle,
                        style = AeonTextStyles.SectionTitle.copy(color = colors.textPrimary)
                    )
                    Text(
                        text = accountSubtitle,
                        style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
                    )
                }

                AeonChip(
                    text = identityLabel,
                    variant = if (authenticatedSession != null) {
                        AeonChipVariant.Success
                    } else {
                        AeonChipVariant.Warning
                    },
                    size = AeonChipSize.Compact
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsSignalCard(
                    modifier = Modifier.weight(1f),
                    label = "Identity",
                    value = identityLabel,
                    accent = if (authenticatedSession != null) colors.success else colors.warning
                )
                SettingsSignalCard(
                    modifier = Modifier.weight(1f),
                    label = "Storage",
                    value = syncLabel,
                    accent = colors.calm
                )
                SettingsSignalCard(
                    modifier = Modifier.weight(1f),
                    label = "Auth",
                    value = authOriginLabel,
                    accent = colors.intelligence
                )
            }

            if (authenticatedSession != null) {
                Spacer(modifier = Modifier.height(12.dp))

                AeonButton(
                    text = "Sign out",
                    onClick = ::signOut,
                    loading = authActionLoading,
                    variant = AeonButtonVariant.Secondary,
                    size = AeonButtonSize.Small
                )
            }
        }

        SettingsSectionCard(
            title = "Central Control",
            subtitle = "Jump into the pages that drive Aeon's daily operating system."
        ) {
            hubCards.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        SettingsHubTile(
                            card = item,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        SettingsSectionCard(
            title = "System Preferences",
            subtitle = "Control cross-app behavior, visual tone, privacy, and recovery."
        ) {
            AeonListItem(
                title = "Notifications",
                subtitle = "Quiet hours, reminders, focus alerts, and delivery rules.",
                leadingIcon = rememberVectorPainter(Icons.Outlined.Notifications),
                onClick = onOpenNotificationSettings
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
            AeonListItem(
                title = "Appearance",
                subtitle = "Density, visual polish, readability, and theme behavior.",
                leadingIcon = rememberVectorPainter(Icons.Outlined.ColorLens),
                onClick = onOpenAppearanceSettings
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
            AeonListItem(
                title = "Privacy & Security",
                subtitle = "Device protection, storage policy, and session trust.",
                leadingIcon = rememberVectorPainter(Icons.Outlined.Lock),
                onClick = onOpenPrivacySettings
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
            AeonListItem(
                title = "Data & Backup",
                subtitle = "Exports, restores, local resilience, and backup workflow.",
                leadingIcon = rememberVectorPainter(Icons.Outlined.Backup),
                onClick = onOpenDataBackupSettings
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
            AeonListItem(
                title = "Sync Conflicts",
                subtitle = "Review records that changed on multiple devices.",
                leadingIcon = rememberVectorPainter(Icons.Outlined.SyncProblem),
                onClick = onOpenSyncConflicts
            )
        }

        SettingsSectionCard(
            title = "Platform",
            subtitle = "Version, product state, and overall system context."
        ) {
            AeonListItem(
                title = "About Aeon",
                subtitle = "Version ${BuildConfig.VERSION_NAME} · product details and legal context.",
                leadingIcon = rememberVectorPainter(Icons.Outlined.Info),
                onClick = onOpenAbout
            )
        }
    }
}

@Composable
private fun SettingsHeaderRow(
    showLoginButton: Boolean,
    loginLoading: Boolean,
    onLoginClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            )
            Text(
                text = "Central control for account, privacy, modules, and device behavior.",
                style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
            )
        }

        if (showLoginButton) {
            AeonButton(
                text = "Log in",
                onClick = onLoginClick,
                loading = loginLoading,
                variant = AeonButtonVariant.Secondary,
                size = AeonButtonSize.Small,
                        leadingIcon = {
                            Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Login,
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AeonThemeTokens.colors

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier = Modifier.padding(start = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = AeonTextStyles.SectionTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = subtitle,
                style = AeonTextStyles.CardSubtitle.copy(color = colors.textSecondary)
            )
        }

        AeonCard(
            variant = AeonCardVariant.Glass,
            contentPadding = PaddingValues(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

@Composable
private fun SettingsSignalCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        containerColor = colors.surface.copy(alpha = 0.88f),
        borderColor = accent.copy(alpha = 0.26f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = AeonTextStyles.Micro.copy(color = colors.textTertiary)
            )
            Text(
                text = value,
                style = AeonTextStyles.CardTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class SettingsHubCard(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit
)

@Composable
private fun SettingsHubTile(
    card: SettingsHubCard,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        modifier = modifier,
        variant = AeonCardVariant.Compact,
        onClick = card.onClick,
        containerColor = colors.surfaceElevated,
        borderColor = card.accent.copy(alpha = 0.18f)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(card.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = card.icon,
                    contentDescription = null,
                    tint = card.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = card.title,
                    style = AeonTextStyles.CardTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = card.subtitle,
                    style = AeonTextStyles.Micro.copy(color = colors.textSecondary),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AeonChip(
                text = "Open",
                variant = AeonChipVariant.Outline,
                size = AeonChipSize.Compact
            )
        }
    }
}
