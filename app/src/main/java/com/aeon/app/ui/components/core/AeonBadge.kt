package com.aeon.app.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.aeon.app.ui.theme.AeonBadgeTokens
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun AeonBadge(
    count: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = AeonThemeTokens.colors.error
) {
    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = AeonBadgeTokens.MinWidth,
                minHeight = AeonBadgeTokens.MinHeight
            )
            .clip(AeonBadgeTokens.Shape)
            .background(containerColor)
            .padding(
                horizontal = AeonBadgeTokens.HorizontalPadding,
                vertical = AeonBadgeTokens.VerticalPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        val text = if (count > 99) "99+" else count.toString()
        Text(
            text = text,
            style = AeonTextStyles.Micro,
            color = Color.White
        )
    }
}
