package com.aeon.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeon.app.R
import com.aeon.app.ui.components.core.AeonButton
import com.aeon.app.ui.components.core.AeonButtonSize
import com.aeon.app.ui.components.core.AeonButtonVariant
import com.aeon.app.ui.components.core.AeonCard
import com.aeon.app.ui.components.core.AeonCardVariant
import com.aeon.app.ui.theme.AeonBrand
import com.aeon.app.ui.theme.AeonCalm
import com.aeon.app.ui.theme.AeonComponentShapes
import com.aeon.app.ui.theme.AeonDuration
import com.aeon.app.ui.theme.AeonGradientBrandEnd
import com.aeon.app.ui.theme.AeonGradientBrandMiddle
import com.aeon.app.ui.theme.AeonGradientBrandStart
import com.aeon.app.ui.theme.AeonPremiumGold
import com.aeon.app.ui.theme.AeonScreenMotion
import com.aeon.app.ui.theme.AeonScreenSpacing
import com.aeon.app.ui.theme.AeonSectionSpacing
import com.aeon.app.ui.theme.AeonSpacing
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens

private data class AeonOnboardingPage(
    val eyebrow: String,
    val title: String,
    val body: String,
    val statOne: String,
    val statTwo: String,
    val showHeroLogo: Boolean = false,
    val optionsTitle: String? = null,
    val options: List<String> = emptyList()
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingFlow(onFinish: () -> Unit) {
    val pages = remember {
        listOf(
            AeonOnboardingPage(
                eyebrow = "Personal Life OS",
                title = "Run your day with calm precision.",
                body = "Aeon brings tasks, habits, focus, health, finance, mood, goals, and daily insight into one private control center.",
                statOne = "Private-first",
                statTwo = "Offline-ready",
                showHeroLogo = true
            ),
            AeonOnboardingPage(
                eyebrow = "Intent",
                title = "What should Aeon optimize first?",
                body = "Choose the primary outcome you want the app to emphasize during setup.",
                statOne = "Adaptive setup",
                statTwo = "No noise",
                optionsTitle = "Primary focus",
                options = listOf(
                    "Focus and time control",
                    "Habit consistency",
                    "Health and energy",
                    "Money clarity",
                    "Life goals"
                )
            ),
            AeonOnboardingPage(
                eyebrow = "Operating rhythm",
                title = "Pick the rhythm that fits your life.",
                body = "Aeon uses your rhythm to make the opening dashboard feel relevant from the first session.",
                statOne = "Daily planning",
                statTwo = "Weekly review",
                optionsTitle = "Preferred rhythm",
                options = listOf(
                    "Fast daily command center",
                    "Deep weekly planning",
                    "Balanced morning and evening checks",
                    "Quiet background tracking"
                )
            ),
            AeonOnboardingPage(
                eyebrow = "Premium workspace",
                title = "Select the modules you care about.",
                body = "Start clean. Aeon can expand later, but the first impression should match what matters now.",
                statOne = "Modular",
                statTwo = "Focused",
                optionsTitle = "Initial modules",
                options = listOf(
                    "Tasks",
                    "Focus",
                    "Habits",
                    "Health",
                    "Finance",
                    "Mood",
                    "Goals",
                    "Daily brief"
                )
            ),
            AeonOnboardingPage(
                eyebrow = "Ready",
                title = "Your Aeon workspace is prepared.",
                body = "A calm, premium dashboard is ready with the essentials surfaced first and the rest kept one tap away.",
                statOne = "Secure",
                statTwo = "Intentional"
            )
        )
    }

    var currentStep by remember { mutableIntStateOf(0) }
    var primaryFocus by remember { mutableStateOf(pages[1].options.first()) }
    var rhythm by remember { mutableStateOf(pages[2].options.first()) }
    var module by remember { mutableStateOf(pages[3].options.first()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AeonThemeTokens.colors.background,
                        AeonThemeTokens.colors.backgroundAlt,
                        AeonThemeTokens.colors.surfaceElevated
                    )
                )
            )
    ) {
        AeonOnboardingAmbientLayer()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = AeonScreenSpacing.PremiumHorizontal)
                .padding(top = AeonSpacing.Large, bottom = AeonSpacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.Large)
        ) {
            AeonOnboardingTopBar(
                currentStep = currentStep,
                totalSteps = pages.size
            )

            AeonProgressTrack(
                currentStep = currentStep,
                totalSteps = pages.size
            )

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = AeonScreenMotion.enterSpec()
                    ) { width -> width } + fadeIn(
                        animationSpec = AeonScreenMotion.enterSpec()
                    ) togetherWith slideOutHorizontally(
                        animationSpec = AeonScreenMotion.exitSpec()
                    ) { width -> -width } + fadeOut(
                        animationSpec = AeonScreenMotion.exitSpec()
                    )
                },
                label = "aeon_onboarding_page",
                modifier = Modifier.weight(1f)
            ) { step ->
                val page = pages[step]
                val selectedOption = when (step) {
                    1 -> primaryFocus
                    2 -> rhythm
                    3 -> module
                    else -> null
                }

                AeonOnboardingPageContent(
                    page = page,
                    selectedOption = selectedOption,
                    onOptionSelected = { option ->
                        when (step) {
                            1 -> primaryFocus = option
                            2 -> rhythm = option
                            3 -> module = option
                        }
                    }
                )
            }

            AeonOnboardingActions(
                currentStep = currentStep,
                totalSteps = pages.size,
                onBack = {
                    if (currentStep > 0) currentStep--
                },
                onNext = {
                    if (currentStep < pages.lastIndex) {
                        currentStep++
                    } else {
                        onFinish()
                    }
                }
            )
        }
    }
}

@Composable
private fun AeonOnboardingTopBar(
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "Aeon logo",
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(CircleShape)
                )
            }

            Column {
                Text(
                    text = "Aeon",
                    style = MaterialTheme.typography.titleMedium,
                    color = AeonThemeTokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Premium setup",
                    style = AeonTextStyles.Caption,
                    color = AeonThemeTokens.colors.textTertiary
                )
            }
        }

        AeonStepPill(
            label = "${currentStep + 1}/$totalSteps"
        )
    }
}

@Composable
private fun AeonProgressTrack(
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.XSmall)
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(AeonComponentShapes.Chip)
                    .background(
                        if (index <= currentStep) {
                            Brush.horizontalGradient(
                                listOf(
                                    AeonGradientBrandStart,
                                    AeonGradientBrandMiddle,
                                    AeonGradientBrandEnd
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    AeonThemeTokens.colors.surfaceMuted,
                                    AeonThemeTokens.colors.surfaceMuted
                                )
                            )
                        }
                    )
            )
        }
    }
}

@Composable
private fun AeonOnboardingPageContent(
    page: AeonOnboardingPage,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        AeonCard(
            variant = AeonCardVariant.Hero,
            backgroundBrush = Brush.linearGradient(
                colors = listOf(
                    AeonBrand.copy(alpha = 0.95f),
                    AeonGradientBrandMiddle.copy(alpha = 0.82f),
                    AeonCalm.copy(alpha = 0.72f)
                )
            ),
            borderColor = Color.White.copy(alpha = 0.22f)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.Large)
            ) {
                if (page.showHeroLogo) {
                    AeonWelcomeLogo()
                }

                AeonStepPill(
                    label = page.eyebrow,
                    darkText = true
                )

                Text(
                    text = page.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 36.sp
                )

                Text(
                    text = page.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.78f)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Small),
                    verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
                ) {
                    AeonStepPill(label = page.statOne, darkText = true)
                    AeonStepPill(label = page.statTwo, darkText = true)
                }
            }
        }

        if (page.options.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AeonSectionSpacing.BetweenSections))

            Text(
                text = page.optionsTitle.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = AeonThemeTokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(AeonSpacing.Medium))

            Column(
                verticalArrangement = Arrangement.spacedBy(AeonSpacing.Small)
            ) {
                page.options.forEach { option ->
                    AeonSelectableSetupCard(
                        text = option,
                        selected = option == selectedOption,
                        onClick = { onOptionSelected(option) }
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(AeonSectionSpacing.BetweenSections))

            AeonReadinessCard()
        }
    }
}

@Composable
private fun AeonWelcomeLogo() {
    Surface(
        modifier = Modifier.size(156.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
        shadowElevation = 24.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            AeonGradientBrandStart.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "Aeon logo",
                modifier = Modifier.size(126.dp)
            )
        }
    }
}

@Composable
private fun AeonSelectableSetupCard(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    AeonCard(
        variant = if (selected) AeonCardVariant.Elevated else AeonCardVariant.Glass,
        onClick = onClick,
        borderColor = if (selected) AeonPremiumGold.copy(alpha = 0.72f) else colors.borderSoft,
        backgroundBrush = if (selected) {
            Brush.linearGradient(
                colors = listOf(
                    colors.surfaceElevated,
                    colors.brandSoft.copy(alpha = 0.34f),
                    colors.surfaceElevated
                )
            )
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            Brush.linearGradient(listOf(AeonPremiumGold, AeonGradientBrandStart))
                        } else {
                            Brush.linearGradient(listOf(colors.surfaceMuted, colors.surfaceMuted))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = AeonTextStyles.CardTitle,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun AeonReadinessCard() {
    AeonCard(
        variant = AeonCardVariant.Glass,
        borderColor = AeonThemeTokens.colors.borderSoft
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AeonSpacing.Medium)
        ) {
            Text(
                text = "What opens next",
                style = MaterialTheme.typography.titleMedium,
                color = AeonThemeTokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "A polished command center with daily priorities, focus state, reminders, and private insights ready for your first session.",
                style = MaterialTheme.typography.bodyMedium,
                color = AeonThemeTokens.colors.textSecondary
            )
        }
    }
}

@Composable
private fun AeonOnboardingActions(
    currentStep: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentStep > 0) {
            AeonButton(
                text = "Back",
                onClick = onBack,
                variant = AeonButtonVariant.Ghost,
                size = AeonButtonSize.Large,
                modifier = Modifier.weight(0.42f)
            )
        }

        AeonButton(
            text = if (currentStep == totalSteps - 1) "Enter Aeon" else "Continue",
            onClick = onNext,
            variant = if (currentStep == totalSteps - 1) {
                AeonButtonVariant.Premium
            } else {
                AeonButtonVariant.Primary
            },
            size = AeonButtonSize.Large,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AeonStepPill(
    label: String,
    darkText: Boolean = false
) {
    Surface(
        shape = AeonComponentShapes.Chip,
        color = if (darkText) {
            Color.White.copy(alpha = 0.16f)
        } else {
            AeonThemeTokens.colors.surfaceGlass
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (darkText) Color.White.copy(alpha = 0.22f) else AeonThemeTokens.colors.borderSoft
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = AeonTextStyles.Micro,
            color = if (darkText) Color.White.copy(alpha = 0.90f) else AeonThemeTokens.colors.textSecondary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AeonOnboardingAmbientLayer() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AeonGradientBrandStart.copy(alpha = 0.24f),
                        Color.Transparent
                    ),
                    radius = 760f
                )
            )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AeonGradientBrandMiddle.copy(alpha = 0.14f),
                        Color.Transparent
                    )
                )
            )
    )
}
