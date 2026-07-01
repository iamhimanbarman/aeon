package com.aeon.app.ui.components.core

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aeon.app.ui.theme.AeonTextStyles
import com.aeon.app.ui.theme.AeonThemeTokens
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun AeonTimePickerDialog(
    initialHour: Int = 12,
    initialMinute: Int = 0,
    initialIsAm: Boolean = true,
    onDismissRequest: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int, isAm: Boolean) -> Unit
) {
    val colors = AeonThemeTokens.colors

    var selectedHour by remember {
        mutableIntStateOf(initialHour.coerceIn(1, 12))
    }

    var selectedMinute by remember {
        mutableIntStateOf(initialMinute.coerceIn(0, 59))
    }

    var isAm by remember {
        mutableStateOf(initialIsAm)
    }

    var mode by remember {
        mutableStateOf(AeonClockMode.Hour)
    }

    var visible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    val dialogAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 280,
            easing = FastOutSlowInEasing
        ),
        label = "time_picker_dialog_alpha"
    )

    val dialogScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.96f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = 420f
        ),
        label = "time_picker_dialog_scale"
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .graphicsLayer {
                    alpha = dialogAlpha
                    scaleX = dialogScale
                    scaleY = dialogScale
                }
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.surface,
                            colors.background.copy(alpha = 0.98f)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AeonTimePickerHeader()

                Spacer(modifier = Modifier.height(18.dp))

                AeonTimeDisplay(
                    hour = selectedHour,
                    minute = selectedMinute,
                    isAm = isAm,
                    mode = mode,
                    onModeChange = { mode = it },
                    onAmPmChange = { isAm = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Crossfade(
                    targetState = mode,
                    animationSpec = tween(
                        durationMillis = 260,
                        easing = FastOutSlowInEasing
                    ),
                    label = "aeon_time_picker_mode"
                ) { clockMode ->
                    AeonPremiumClockFace(
                        mode = clockMode,
                        selectedHour = selectedHour,
                        selectedMinute = selectedMinute,
                        onHourChange = { value ->
                            selectedHour = value
                        },
                        onMinuteChange = { value ->
                            selectedMinute = value
                        },
                        onSelectionCommitted = {
                            if (clockMode == AeonClockMode.Hour) {
                                mode = AeonClockMode.Minute
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                AeonTimePickerActions(
                    onCancel = onDismissRequest,
                    onSave = {
                        onTimeSelected(
                            selectedHour,
                            selectedMinute,
                            isAm
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AeonTimePickerHeader() {
    val colors = AeonThemeTokens.colors

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Select time",
            style = AeonTextStyles.CardTitle.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )

        Text(
            text = "Calm reminder timing with smooth precision.",
            style = AeonTextStyles.Caption.copy(
                color = colors.textSecondary
            )
        )
    }
}

@Composable
private fun AeonTimeDisplay(
    hour: Int,
    minute: Int,
    isAm: Boolean,
    mode: AeonClockMode,
    onModeChange: (AeonClockMode) -> Unit,
    onAmPmChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AeonTimeValuePill(
                text = hour.toString().padStart(2, '0'),
                selected = mode == AeonClockMode.Hour,
                onClick = {
                    onModeChange(AeonClockMode.Hour)
                },
                modifier = Modifier.weight(1f)
            )

            Text(
                text = ":",
                style = AeonTextStyles.HeroMetric.copy(
                    fontSize = 36.sp,
                    color = AeonThemeTokens.colors.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            AeonTimeValuePill(
                text = minute.toString().padStart(2, '0'),
                selected = mode == AeonClockMode.Minute,
                onClick = {
                    onModeChange(AeonClockMode.Minute)
                },
                modifier = Modifier.weight(1f)
            )
        }

        AeonAmPmSegment(
            isAm = isAm,
            onAmPmChange = onAmPmChange
        )
    }
}

@Composable
private fun AeonTimeValuePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeonThemeTokens.colors

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = 500f
        ),
        label = "time_value_pill_scale"
    )

    Box(
        modifier = modifier
            .height(70.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (selected) {
                    colors.brand.copy(alpha = 0.16f)
                } else {
                    colors.background.copy(alpha = 0.72f)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AeonTextStyles.HeroMetric.copy(
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) colors.brand else colors.textPrimary
            )
        )
    }
}

@Composable
private fun AeonAmPmSegment(
    isAm: Boolean,
    onAmPmChange: (Boolean) -> Unit
) {
    val colors = AeonThemeTokens.colors

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.background.copy(alpha = 0.72f))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AeonAmPmButton(
            text = "AM",
            selected = isAm,
            onClick = {
                onAmPmChange(true)
            }
        )

        AeonAmPmButton(
            text = "PM",
            selected = !isAm,
            onClick = {
                onAmPmChange(false)
            }
        )
    }
}

@Composable
private fun AeonAmPmButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    val alpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.62f,
        animationSpec = tween(180),
        label = "ampm_alpha"
    )

    Box(
        modifier = Modifier
            .width(56.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    colors.brand.copy(alpha = 0.18f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AeonTextStyles.ButtonMedium.copy(
                color = if (selected) colors.brand else colors.textSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha
            }
        )
    }
}

@Composable
private fun AeonPremiumClockFace(
    mode: AeonClockMode,
    selectedHour: Int,
    selectedMinute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onSelectionCommitted: () -> Unit
) {
    val selectedValue = when (mode) {
        AeonClockMode.Hour -> selectedHour
        AeonClockMode.Minute -> selectedMinute
    }

    val targetAngle = selectedValue.toClockAngle(mode)

    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = 360f
        ),
        label = "premium_clock_hand_angle"
    )

    val faceScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = 0.84f,
            stiffness = 380f
        ),
        label = "premium_clock_face_scale"
    )

    Box(
        modifier = Modifier
            .size(268.dp)
            .graphicsLayer {
                scaleX = faceScale
                scaleY = faceScale
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AeonThemeTokens.colors.surface,
                        AeonThemeTokens.colors.background.copy(alpha = 0.96f)
                    )
                )
            )
            .pointerInput(mode) {
                fun updateFromTouch(offset: Offset) {
                    val value = offset.toClockValue(
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                        mode = mode
                    )

                    when (mode) {
                        AeonClockMode.Hour -> onHourChange(value)
                        AeonClockMode.Minute -> onMinuteChange(value)
                    }
                }

                detectTapGestures(
                    onTap = { offset ->
                        updateFromTouch(offset)
                        onSelectionCommitted()
                    }
                )
            }
            .pointerInput(mode) {
                fun updateFromTouch(offset: Offset) {
                    val value = offset.toClockValue(
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                        mode = mode
                    )

                    when (mode) {
                        AeonClockMode.Hour -> onHourChange(value)
                        AeonClockMode.Minute -> onMinuteChange(value)
                    }
                }

                detectDragGestures(
                    onDrag = { change, _ ->
                        change.consume()
                        updateFromTouch(change.position)
                    },
                    onDragEnd = onSelectionCommitted
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AeonClockCanvas(
            mode = mode,
            animatedAngle = animatedAngle
        )

        AeonClockLabels(
            mode = mode,
            selectedValue = selectedValue
        )

        AeonClockCenterLabel(mode = mode)
    }
}

@Composable
private fun AeonClockCanvas(
    mode: AeonClockMode,
    animatedAngle: Float
) {
    val colors = AeonThemeTokens.colors

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension / 2f - 14.dp.toPx()
        val numberRadius = size.minDimension / 2f - 38.dp.toPx()
        val handRadius = numberRadius - 18.dp.toPx()

        drawCircle(
            color = colors.brand.copy(alpha = 0.045f),
            radius = outerRadius,
            center = center
        )

        drawCircle(
            color = colors.textSecondary.copy(alpha = 0.10f),
            radius = outerRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        val divisions = if (mode == AeonClockMode.Hour) 12 else 60
        val majorStep = if (mode == AeonClockMode.Hour) 1 else 5

        for (index in 0 until divisions) {
            val isMajor = index % majorStep == 0
            val angle = ((index.toFloat() / divisions.toFloat()) * 360f - 90f).toRadians()

            val tickLength = if (isMajor) 8.dp.toPx() else 3.dp.toPx()
            val tickStartRadius = outerRadius - tickLength - 5.dp.toPx()
            val tickEndRadius = outerRadius - 5.dp.toPx()

            val start = Offset(
                x = center.x + tickStartRadius * cos(angle),
                y = center.y + tickStartRadius * sin(angle)
            )

            val end = Offset(
                x = center.x + tickEndRadius * cos(angle),
                y = center.y + tickEndRadius * sin(angle)
            )

            drawLine(
                color = if (isMajor) {
                    colors.textSecondary.copy(alpha = 0.34f)
                } else {
                    colors.textSecondary.copy(alpha = 0.16f)
                },
                start = start,
                end = end,
                strokeWidth = if (isMajor) 1.4.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        val handAngle = animatedAngle.toRadians()
        val handEnd = Offset(
            x = center.x + handRadius * cos(handAngle),
            y = center.y + handRadius * sin(handAngle)
        )

        drawLine(
            color = colors.brand.copy(alpha = 0.88f),
            start = center,
            end = handEnd,
            strokeWidth = 2.6.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawCircle(
            color = colors.brand.copy(alpha = 0.14f),
            radius = 26.dp.toPx(),
            center = handEnd
        )

        drawCircle(
            color = colors.brand,
            radius = 19.dp.toPx(),
            center = handEnd
        )

        drawCircle(
            color = colors.brand.copy(alpha = 0.18f),
            radius = 12.dp.toPx(),
            center = center
        )

        drawCircle(
            color = colors.brand,
            radius = 4.5.dp.toPx(),
            center = center
        )
    }
}

@Composable
private fun AeonClockLabels(
    mode: AeonClockMode,
    selectedValue: Int
) {
    val colors = AeonThemeTokens.colors

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val radiusDp = 268.dp / 2 - 38.dp

        val labels = when (mode) {
            AeonClockMode.Hour -> (1..12).toList()
            AeonClockMode.Minute -> (0 until 60 step 5).toList()
        }

        labels.forEach { value ->
            val angle = value.toClockAngle(mode).toRadians()
            val x = radiusDp.value * cos(angle)
            val y = radiusDp.value * sin(angle)
            val selected = value == selectedValue

            val scale by animateFloatAsState(
                targetValue = if (selected) 1.08f else 1f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = 520f
                ),
                label = "clock_label_scale_$value"
            )

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .offset {
                        IntOffset(
                            x = x.dp.roundToPx(),
                            y = y.dp.roundToPx()
                        )
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (mode) {
                        AeonClockMode.Hour -> value.toString()
                        AeonClockMode.Minute -> value.toString().padStart(2, '0')
                    },
                    style = AeonTextStyles.InputText.copy(
                        color = if (selected) Color.White else colors.textPrimary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (mode == AeonClockMode.Hour) 15.sp else 13.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

@Composable
private fun AeonClockCenterLabel(
    mode: AeonClockMode
) {
    val colors = AeonThemeTokens.colors

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.surface.copy(alpha = 0.74f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (mode) {
                AeonClockMode.Hour -> "Hour"
                AeonClockMode.Minute -> "Minute"
            },
            style = AeonTextStyles.Micro.copy(
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun AeonTimePickerActions(
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val colors = AeonThemeTokens.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Cancel",
            style = AeonTextStyles.ButtonMedium.copy(
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onCancel)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Save time",
            style = AeonTextStyles.ButtonMedium.copy(
                color = colors.brand,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(colors.brand.copy(alpha = 0.14f))
                .clickable(onClick = onSave)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

private enum class AeonClockMode {
    Hour,
    Minute
}

private fun Offset.toClockValue(
    width: Float,
    height: Float,
    mode: AeonClockMode
): Int {
    val center = Offset(width / 2f, height / 2f)
    val dx = x - center.x
    val dy = y - center.y

    var angle = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat() + 90f

    if (angle < 0f) {
        angle += 360f
    }

    return when (mode) {
        AeonClockMode.Hour -> {
            val raw = ((angle / 360f) * 12f).roundToInt()
            if (raw == 0 || raw == 12) 12 else raw.coerceIn(1, 11)
        }

        AeonClockMode.Minute -> {
            val raw = ((angle / 360f) * 60f).roundToInt()
            if (raw == 60) 0 else raw.coerceIn(0, 59)
        }
    }
}

private fun Int.toClockAngle(
    mode: AeonClockMode
): Float {
    return when (mode) {
        AeonClockMode.Hour -> {
            val normalized = if (this == 12) 0f else this / 12f
            normalized * 360f - 90f
        }

        AeonClockMode.Minute -> {
            this.coerceIn(0, 59) / 60f * 360f - 90f
        }
    }
}

private fun Float.toRadians(): Float {
    return (this * PI / 180.0).toFloat()
}