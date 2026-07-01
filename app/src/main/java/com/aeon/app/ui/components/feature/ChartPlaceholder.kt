package com.aeon.app.ui.components.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.aeon.app.ui.theme.AeonDirectionalShapes
import com.aeon.app.ui.theme.AeonInsightTokens
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun ChartPlaceholder(
    data: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    val height = if (isLarge) AeonInsightTokens.ChartHeightLarge else AeonInsightTokens.ChartHeightMedium

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(top = AeonInsightTokens.ChartTopPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { index, fraction ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(AeonSpacing.XXLarge)
                        .fillMaxHeight(fraction)
                        .clip(AeonDirectionalShapes.TopRoundedLarge)
                        .background(if (index == data.size - 1) AeonThemeTokens.colors.brand else AeonThemeTokens.colors.surfaceElevated)
                )
                Spacer(modifier = Modifier.height(AeonInsightTokens.ChartLabelGap))
                Text(
                    text = labels.getOrElse(index) { "" },
                    style = AeonTextStyles.Micro,
                    color = AeonThemeTokens.colors.textSecondary
                )
            }
        }
    }
}
