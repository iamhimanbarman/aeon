package com.aeon.app.ui.components.layout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeon.app.ui.theme.AeonAppBarTokens
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

enum class AeonTopBarMenuDestination(
    val label: String,
    val icon: ImageVector
) {
    Settings("Settings", Icons.Outlined.Settings),
    DailyBrief("Daily brief", Icons.Outlined.Newspaper),
    Goals("Goals", Icons.Outlined.Flag),
    Health("Health", Icons.Outlined.HealthAndSafety),
    Journal("Journal", Icons.AutoMirrored.Outlined.MenuBook),
    Mood("Mood", Icons.Outlined.Mood),
    Tasks("Tasks", Icons.Outlined.Checklist)
}

@Composable
fun AeonTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    isElevated: Boolean = false
) {
    val shadowElevation = if (isElevated) AeonAppBarTokens.Elevation else 0.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AeonAppBarTokens.Height)
            .shadow(shadowElevation)
            .background(AeonThemeTokens.colors.surface)
            .padding(horizontal = AeonAppBarTokens.HorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (navigationIcon != null) {
                navigationIcon()
            }
            Box(modifier = Modifier.padding(start = if (navigationIcon != null) AeonAppBarTokens.TitleStartPadding else 0.dp)) {
                Text(
                    text = title,
                    style = AeonTextStyles.SectionTitle,
                    color = AeonThemeTokens.colors.textPrimary
                )
            }
        }
        if (actions != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                actions()
            }
        }
    }
}


@Composable
fun AeonBrandTopAppBar(
    onNotificationsClick: () -> Unit,
    onMenuDestinationClick: (AeonTopBarMenuDestination) -> Unit,
    modifier: Modifier = Modifier,
    unreadNotificationCount: Int = 0,
    titleOverride: String? = null,
    actionsOverride: (@Composable () -> Unit)? = null
) {
    val colors = AeonThemeTokens.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        colors.surface.copy(alpha = 0.98f),
                        colors.surfaceElevated.copy(alpha = 0.96f),
                        colors.surface.copy(alpha = 0.98f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = AeonAppBarTokens.HorizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand mark or Page Title
            if (titleOverride != null) {
                Text(
                    text = titleOverride,
                    style = AeonTextStyles.SectionTitle.copy(
                        letterSpacing = 1.0.sp,
                        fontSize = 18.sp
                    ),
                    color = colors.textPrimary,
                    maxLines = 1
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(colors.brand, colors.ai)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Aeon",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "AEON",
                        style = AeonTextStyles.SectionTitle.copy(
                            letterSpacing = 1.6.sp,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                }
            }

            // Actions - compact or Overrides
            if (actionsOverride != null) {
                actionsOverride()
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AeonTopBarAction(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsNone,
                                contentDescription = "Notifications",
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        onClick = onNotificationsClick,
                        badgeCount = unreadNotificationCount
                    )

                    AeonTopBarOverflowMenu(
                        onDestinationClick = onMenuDestinationClick
                    )
                }
            }
        }
    }
}


@Composable
private fun AeonTopBarOverflowMenu(
    onDestinationClick: (AeonTopBarMenuDestination) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = AeonThemeTokens.colors

    Box {
        AeonTopBarAction(
            icon = {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More options",
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(220.dp),
            containerColor = colors.surfaceElevated,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, colors.borderSoft.copy(alpha = 0.64f))
        ) {
            AeonTopBarMenuDestination.entries.forEach { destination ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = destination.label,
                            style = AeonTextStyles.CardTitle,
                            color = colors.textPrimary
                        )
                    },
                    onClick = {
                        expanded = false
                        onDestinationClick(destination)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                            tint = colors.iconSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }
    }
}


@Composable
private fun AeonTopBarAction(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    badgeCount: Int = 0
) {
    val colors = AeonThemeTokens.colors

    Box {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(36.dp)
                .defaultMinSize(
                    minWidth = 36.dp,
                    minHeight = 36.dp
                ),
            shape = CircleShape,
            color = colors.surfaceElevated.copy(alpha = 0.60f),
            contentColor = colors.iconPrimary,
            border = BorderStroke(0.5.dp, colors.borderSoft.copy(alpha = 0.50f)),
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }

        if (badgeCount > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(15.dp),
                shape = CircleShape,
                color = colors.error,
                contentColor = Color.White,
                border = BorderStroke(1.dp, colors.surface)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                        style = AeonTextStyles.Micro,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
