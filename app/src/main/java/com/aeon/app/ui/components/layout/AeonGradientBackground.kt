package com.aeon.app.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun AeonGradientBackground(
    modifier: Modifier = Modifier,
    topColor: Color = AeonThemeTokens.colors.brandSoft,
    bottomColor: Color = AeonThemeTokens.colors.background,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor)
                )
            )
    ) {
        content()
    }
}
