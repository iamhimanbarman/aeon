package com.aeon.app.ui.components.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonLinearProgress
import com.aeon.app.ui.theme.AeonGoalTokens
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun GoalProgressCard(
    goalTitle: String,
    progressFraction: Float,
    statusText: String,
    modifier: Modifier = Modifier
) {
    AeonCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Active Goal",
            style = AeonTextStyles.Micro,
            color = AeonThemeTokens.colors.goal
        )
        Spacer(modifier = Modifier.height(AeonSpacing.XSmall))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = goalTitle,
                style = AeonTextStyles.CardTitle,
                color = AeonThemeTokens.colors.textPrimary
            )
            Text(
                text = "${(progressFraction * 100).toInt()}%",
                style = AeonTextStyles.CardSubtitle,
                color = AeonThemeTokens.colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.height(AeonSpacing.Medium))
        AeonLinearProgress(
            progress = progressFraction,
            progressColor = AeonThemeTokens.colors.goal
        )
        Spacer(modifier = Modifier.height(AeonSpacing.Small))
        Text(
            text = statusText,
            style = AeonTextStyles.Micro,
            color = AeonThemeTokens.colors.textTertiary
        )
    }
}
