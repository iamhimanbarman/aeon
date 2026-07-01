package com.aeon.app.ui.components.layout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aeon.app.ui.theme.AeonCardTokens
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun AeonGlassContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        shape = AeonCardTokens.Glass.shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = AeonCardTokens.Glass.elevation
        ),
        border = BorderStroke(AeonCardTokens.Glass.borderWidth, AeonThemeTokens.colors.borderSoft),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(AeonThemeTokens.colors.surfaceGlass)
                .padding(AeonCardTokens.Glass.padding)
        ) {
            content()
        }
    }
}
