package com.aeon.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.components.core.AeonChip
import com.aeon.app.ui.components.core.AeonChipVariant
import com.aeon.app.ui.components.core.AeonListItem
import com.aeon.app.ui.components.core.AeonSectionHeader
import com.aeon.app.ui.components.core.AeonSectionHeaderSize
import com.aeon.app.ui.components.layout.AeonScreen
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

// ----------------------------------------------------
// Route
// ----------------------------------------------------

@Composable
fun AeonSettingsRoute(
    onOpenNotificationSettings: () -> Unit = {},
    onOpenPrivacySettings: () -> Unit = {},
    onOpenAppearanceSettings: () -> Unit = {},
    onOpenBackupSettings: () -> Unit = {},
    onOpenDataSettings: () -> Unit = {},
    onOpenSecuritySettings: () -> Unit = {},
    onExportData: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenHelp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AeonScreen(modifier = modifier) {
        AeonSectionHeader(
            eyebrow = "System configuration",
            title = "Settings",
            subtitle = "Control your offline-first life OS.",
            size = AeonSectionHeaderSize.Hero
        )

        // ----------------------------------------------------
        // Profile Card
        // ----------------------------------------------------
        AeonCard(variant = AeonCardVariant.Glass) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AeonThemeTokens.colors.brand.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", style = AeonTextStyles.SectionTitle, color = AeonThemeTokens.colors.brand)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Aeon OS", style = AeonTextStyles.CardTitle)
                    Text("v1.0.0 · Offline-first", style = AeonTextStyles.CardSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                AeonChip(
                    text = "Protected",
                    variant = AeonChipVariant.Success
                )
            }
        }

        // ----------------------------------------------------
        // Preferences
        // ----------------------------------------------------
        SettingsGroup(title = "Preferences") {
            AeonListItem(
                title = "Appearance",
                subtitle = "Dark mode, typography, density",
                leadingIcon = rememberVectorPainter(Icons.Outlined.ColorLens),
                onClick = onOpenAppearanceSettings
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            AeonListItem(
                title = "Notifications",
                subtitle = "Quiet hours, reminders",
                leadingIcon = rememberVectorPainter(Icons.Outlined.Notifications),
                onClick = onOpenNotificationSettings
            )
        }

        // ----------------------------------------------------
        // Privacy & Data
        // ----------------------------------------------------
        SettingsGroup(title = "Privacy & Data") {
            AeonListItem(
                title = "Security & Privacy",
                subtitle = "App lock, local storage constraints",
                leadingIcon = rememberVectorPainter(Icons.Outlined.Lock),
                onClick = onOpenSecuritySettings
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            AeonListItem(
                title = "Data Management",
                subtitle = "Export, import, clear cache",
                leadingIcon = rememberVectorPainter(Icons.Outlined.Storage),
                onClick = onOpenDataSettings
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            AeonListItem(
                title = "Backup & Restore",
                subtitle = "Encrypted local backups",
                leadingIcon = rememberVectorPainter(Icons.Outlined.Backup),
                onClick = onOpenBackupSettings
            )
        }

        // ----------------------------------------------------
        // About
        // ----------------------------------------------------
        SettingsGroup(title = "About") {
            AeonListItem(
                title = "About Aeon",
                subtitle = "Version, Philosophy, Legal",
                leadingIcon = rememberVectorPainter(Icons.Outlined.Info),
                onClick = onOpenAbout
            )
        }

    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)) {
        Text(
            text = title,
            style = AeonTextStyles.SectionTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )
        AeonCard(
            variant = AeonCardVariant.Default,
            contentPadding = PaddingValues(0.dp)
        ) {
            Column {
                content()
            }
        }
    }
}
