package com.aeon.app.ui.components.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import kotlinx.coroutines.delay

@Composable
fun AeonStopwatchWidget(modifier: Modifier = Modifier) {
    val colors = AeonThemeTokens.colors
    var timeMillis by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(10)
            timeMillis += 10
        }
    }

    val minutes = (timeMillis / 1000) / 60
    val seconds = (timeMillis / 1000) % 60
    val milliseconds = (timeMillis % 1000) / 10

    AeonCard(
        modifier = modifier.aspectRatio(1f),
        variant = AeonCardVariant.Default
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Stopwatch",
                style = AeonTextStyles.Caption.copy(
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Text(
                text = String.format("%02d:%02d.%02d", minutes, seconds, milliseconds),
                style = AeonTextStyles.SectionTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WidgetIconButton(
                    icon = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    onClick = { isRunning = !isRunning },
                    backgroundColor = colors.brand.copy(alpha = 0.15f),
                    iconColor = colors.brand
                )
                WidgetIconButton(
                    icon = Icons.Rounded.Refresh,
                    onClick = {
                        isRunning = false
                        timeMillis = 0L
                    },
                    backgroundColor = colors.surfaceElevated,
                    iconColor = colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun AeonTimerWidget(
    modifier: Modifier = Modifier,
    initialMinutes: Int = 25
) {
    val colors = AeonThemeTokens.colors
    val initialMillis = initialMinutes * 60 * 1000L
    var timeMillis by remember { mutableLongStateOf(initialMillis) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, timeMillis) {
        if (isRunning && timeMillis > 0) {
            delay(100)
            timeMillis -= 100
        } else if (timeMillis <= 0) {
            isRunning = false
        }
    }

    val minutes = (timeMillis / 1000) / 60
    val seconds = (timeMillis / 1000) % 60

    AeonCard(
        modifier = modifier.aspectRatio(1f),
        variant = AeonCardVariant.Default
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Timer",
                style = AeonTextStyles.Caption.copy(
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = AeonTextStyles.SectionTitle.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WidgetIconButton(
                    icon = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    onClick = { if (timeMillis > 0) isRunning = !isRunning },
                    backgroundColor = colors.brand.copy(alpha = 0.15f),
                    iconColor = colors.brand
                )
                WidgetIconButton(
                    icon = Icons.Rounded.Refresh,
                    onClick = {
                        isRunning = false
                        timeMillis = initialMillis
                    },
                    backgroundColor = colors.surfaceElevated,
                    iconColor = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun WidgetIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    backgroundColor: androidx.compose.ui.graphics.Color,
    iconColor: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor
            )
        }
    }
}
