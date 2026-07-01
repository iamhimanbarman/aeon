package com.aeon.app.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aeon.app.ui.theme.AeonSize
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun AeonDivider(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AeonSize.DividerThickness)
            .background(AeonThemeTokens.colors.divider)
    )
}
