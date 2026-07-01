package com.aeon.app.ui.components.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun HealthSummaryCard(
    waterText: String,
    sleepText: String,
    caloriesText: String,
    modifier: Modifier = Modifier
) {
    AeonCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💧", style = AeonTextStyles.SectionTitle)
                Text(waterText, style = AeonTextStyles.CardTitle, color = AeonThemeTokens.colors.textPrimary)
                Text("Water", style = AeonTextStyles.Micro, color = AeonThemeTokens.colors.textSecondary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💤", style = AeonTextStyles.SectionTitle)
                Text(sleepText, style = AeonTextStyles.CardTitle, color = AeonThemeTokens.colors.textPrimary)
                Text("Sleep", style = AeonTextStyles.Micro, color = AeonThemeTokens.colors.textSecondary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥", style = AeonTextStyles.SectionTitle)
                Text(caloriesText, style = AeonTextStyles.CardTitle, color = AeonThemeTokens.colors.textPrimary)
                Text("Kcal", style = AeonTextStyles.Micro, color = AeonThemeTokens.colors.textSecondary)
            }
        }
    }
}
