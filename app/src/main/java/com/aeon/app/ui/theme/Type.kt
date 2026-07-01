package com.aeon.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * AEON PREMIUM TYPOGRAPHY SYSTEM
 *
 * Product feel:
 * Calm • Premium • Focused • Intelligent • Personal Life OS
 *
 * Recommended font:
 * Manrope
 *
 * Design rule:
 * Use typography to create clarity, not decoration.
 * Keep large text elegant and small text readable.
 */

// Replace FontFamily.Default with your custom Manrope font family
// after adding the font files inside res/font.
val AeonFontFamily = FontFamily.Default

val AeonTypography = Typography(

    // ----------------------------------------------------
    // Display Styles
    // Use for onboarding hero text, life score number,
    // premium dashboard hero numbers.
    // ----------------------------------------------------

    displayLarge = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1.2).sp
    ),

    displayMedium = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.0).sp
    ),

    displaySmall = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.7).sp
    ),


    // ----------------------------------------------------
    // Headline Styles
    // Use for screen titles and major dashboard sections.
    // ----------------------------------------------------

    headlineLarge = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),

    headlineMedium = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),

    headlineSmall = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),


    // ----------------------------------------------------
    // Title Styles
    // Use for cards, sections, dialogs, list headers.
    // ----------------------------------------------------

    titleLarge = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),

    titleMedium = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.1).sp
    ),

    titleSmall = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),


    // ----------------------------------------------------
    // Body Styles
    // Use for normal readable app content.
    // ----------------------------------------------------

    bodyLarge = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),

    bodySmall = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),


    // ----------------------------------------------------
    // Label Styles
    // Use for buttons, chips, bottom nav, tabs, badges.
    // ----------------------------------------------------

    labelLarge = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    labelMedium = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),

    labelSmall = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    )
)
