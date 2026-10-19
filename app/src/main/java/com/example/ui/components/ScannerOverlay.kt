package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ScannerCornerColor
import com.example.ui.theme.ScannerLaserColor
import com.example.ui.theme.ScannerOverlayDim

@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier,
    laserActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserTransition")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserPosition"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate viewfinder size: square box roughly 70% of min dimension, capped at 320dp
            val boxSize = (minOf(canvasWidth, canvasHeight) * 0.72f).coerceIn(240.dp.toPx(), 320.dp.toPx())
            val left = (canvasWidth - boxSize) / 2f
            val top = (canvasHeight - boxSize) / 2.3f // slightly above center
            val right = left + boxSize
            val bottom = top + boxSize
            val cornerRadius = 24.dp.toPx()

            val boxRect = Rect(left, top, right, bottom)
            val boxPath = Path().apply {
                addRoundRect(RoundRect(boxRect, CornerRadius(cornerRadius, cornerRadius)))
            }

            // Dim surrounding screen
            clipPath(boxPath, clipOp = ClipOp.Difference) {
                drawRect(color = ScannerOverlayDim)
            }

            // Draw corner brackets
            val bracketLength = 36.dp.toPx()
            val bracketStroke = 4.5.dp.toPx()
            val cornerColor = ScannerCornerColor

            // Top-Left
            drawLine(
                color = cornerColor,
                start = Offset(left, top + bracketLength),
                end = Offset(left, top + cornerRadius),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )
            drawArc(
                color = cornerColor,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(left, top),
                size = Size(cornerRadius * 2, cornerRadius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(bracketStroke, cap = StrokeCap.Round)
            )
            drawLine(
                color = cornerColor,
                start = Offset(left + cornerRadius, top),
                end = Offset(left + bracketLength, top),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )

            // Top-Right
            drawLine(
                color = cornerColor,
                start = Offset(right - bracketLength, top),
                end = Offset(right - cornerRadius, top),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )
            drawArc(
                color = cornerColor,
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(right - cornerRadius * 2, top),
                size = Size(cornerRadius * 2, cornerRadius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(bracketStroke, cap = StrokeCap.Round)
            )
            drawLine(
                color = cornerColor,
                start = Offset(right, top + cornerRadius),
                end = Offset(right, top + bracketLength),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )

            // Bottom-Left
            drawLine(
                color = cornerColor,
                start = Offset(left, bottom - bracketLength),
                end = Offset(left, bottom - cornerRadius),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )
            drawArc(
                color = cornerColor,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(left, bottom - cornerRadius * 2),
                size = Size(cornerRadius * 2, cornerRadius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(bracketStroke, cap = StrokeCap.Round)
            )
            drawLine(
                color = cornerColor,
                start = Offset(left + cornerRadius, bottom),
                end = Offset(left + bracketLength, bottom),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )

            // Bottom-Right
            drawLine(
                color = cornerColor,
                start = Offset(right - bracketLength, bottom),
                end = Offset(right - cornerRadius, bottom),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )
            drawArc(
                color = cornerColor,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(right - cornerRadius * 2, bottom - cornerRadius * 2),
                size = Size(cornerRadius * 2, cornerRadius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(bracketStroke, cap = StrokeCap.Round)
            )
            drawLine(
                color = cornerColor,
                start = Offset(right, bottom - cornerRadius),
                end = Offset(right, bottom - bracketLength),
                strokeWidth = bracketStroke,
                cap = StrokeCap.Round
            )

            // Draw laser line if active
            if (laserActive) {
                val laserY = top + (bottom - top) * laserPosition
                val laserGlowBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        ScannerLaserColor.copy(alpha = 0.8f),
                        ScannerLaserColor,
                        ScannerLaserColor.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    startX = left + 12.dp.toPx(),
                    endX = right - 12.dp.toPx()
                )

                // Laser glow line
                drawLine(
                    brush = laserGlowBrush,
                    start = Offset(left + 14.dp.toPx(), laserY),
                    end = Offset(right - 14.dp.toPx(), laserY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
