package com.aeon.app.ui.components.feedback

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.aeon.app.ui.theme.AeonSkeletonTokens
import com.aeon.app.ui.theme.AeonThemeTokens

@Composable
fun AeonSkeleton(
    modifier: Modifier = Modifier,
    isCard: Boolean = false
) {
    val height = if (isCard) AeonSkeletonTokens.CardHeight else AeonSkeletonTokens.HeightMedium

    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(AeonSkeletonTokens.PulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(AeonSkeletonTokens.Shape)
            .background(AeonThemeTokens.colors.surfaceMuted.copy(alpha = alpha))
    )
}
