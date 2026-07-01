package com.aeon.app.ui.components.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import com.aeon.app.ui.theme.AeonIconButtonTokens
import com.aeon.app.ui.theme.AeonSpring
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun AeonIconButton(
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AeonThemeTokens.colors.surfaceElevated,
    contentColor: Color = AeonThemeTokens.colors.iconPrimary,
    isCircular: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) AeonIconButtonTokens.PressedScale else 1f,
        animationSpec = AeonSpring.snappy(),
        label = "icon_button_scale"
    )

    val shape = if (isCircular) AeonIconButtonTokens.CircleShape else AeonIconButtonTokens.Shape

    Box(
        modifier = modifier
            .size(AeonIconButtonTokens.Size)
            .scale(scale)
            .clip(shape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(AeonIconButtonTokens.IconSize)
        )
    }
}
