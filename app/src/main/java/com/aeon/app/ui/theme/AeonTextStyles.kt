package com.aeon.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AeonTextStyles {

    // ----------------------------------------------------
    // Hero / Dashboard Numbers
    // ----------------------------------------------------

    val LifeScoreNumber = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1.6).sp
    )

    val HeroMetric = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp
    )

    val StatNumber = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp
    )


    // ----------------------------------------------------
    // Finance / Numeric Styles
    // ----------------------------------------------------

    val MoneyLarge = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.7).sp
    )

    val MoneyMedium = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    )

    val MoneySmall = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )


    // ----------------------------------------------------
    // Section / Card Styles
    // ----------------------------------------------------

    val SectionTitle = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    )

    val CardTitle = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    )

    val CardSubtitle = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    )


    // ----------------------------------------------------
    // Button / Input Styles
    // ----------------------------------------------------

    val ButtonLarge = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    )

    val ButtonMedium = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    val InputText = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )

    val InputLabel = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    )


    // ----------------------------------------------------
    // Navigation / Tabs
    // ----------------------------------------------------

    val BottomNavSelected = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )

    val BottomNavUnselected = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )

    val TabSelected = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    val TabUnselected = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )


    // ----------------------------------------------------
    // Captions / Metadata
    // ----------------------------------------------------

    val Caption = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.1.sp
    )

    val Micro = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    )

    val Timestamp = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.2.sp
    )


    // ----------------------------------------------------
    // AI Assistant / Insight Styles
    // ----------------------------------------------------

    val AIMessage = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )

    val InsightTitle = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    )

    val InsightBody = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )


    // ----------------------------------------------------
    // Special Text
    // ----------------------------------------------------

    val Quote = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    )

    val EmptyStateTitle = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    )

    val EmptyStateBody = TextStyle(
        fontFamily = AeonFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )
}
