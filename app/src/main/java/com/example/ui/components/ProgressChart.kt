package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProgressChart(
    dailyXpValues: List<Int>, // Last 7 days, e.g. [20, 50, 0, 100, 30, 40, 60]
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(dailyXpValues) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Activity Progress (Last 7 Days)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val width = size.width
            val height = size.height
            val paddingLeft = 35.dp.toPx()
            val paddingBottom = 20.dp.toPx()
            val paddingTop = 10.dp.toPx()
            val paddingRight = 10.dp.toPx()

            val chartWidth = width - paddingLeft - paddingRight
            val chartHeight = height - paddingTop - paddingBottom

            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val maxVal = (dailyXpValues.maxOrNull() ?: 100).coerceAtLeast(100)

            // 1. Draw horizontal grid lines & Y-axis labels
            val gridLines = 3
            for (i in 0..gridLines) {
                val fraction = i.toFloat() / gridLines
                val y = paddingTop + chartHeight * (1 - fraction)
                // Grid line
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(width - paddingRight, y),
                    strokeWidth = 1.dp.toPx()
                )
                // Y Label
                val xpLabel = (maxVal * fraction).toInt().toString()
                val measuredText = textMeasurer.measure(
                    text = xpLabel,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 9.sp,
                        color = labelColor
                    )
                )
                drawText(
                    textLayoutResult = measuredText,
                    topLeft = Offset(paddingLeft - measuredText.size.width - 5.dp.toPx(), y - measuredText.size.height / 2f)
                )
            }

            if (dailyXpValues.isNotEmpty()) {
                val points = mutableListOf<Offset>()
                val stepX = chartWidth / (dailyXpValues.size - 1).coerceAtLeast(1)

                for (i in dailyXpValues.indices) {
                    val x = paddingLeft + i * stepX
                    val xpVal = dailyXpValues[i]
                    val fraction = xpVal.toFloat() / maxVal
                    // Invert for Canvas coordinates (0 is at top)
                    val y = paddingTop + chartHeight * (1 - fraction * animatedProgress.value)
                    points.add(Offset(x, y))
                }

                // 2. Draw smooth cubic Bézier spline path
                if (points.size > 1) {
                    val strokePath = Path()
                    val fillPath = Path()

                    strokePath.moveTo(points[0].x, points[0].y)
                    fillPath.moveTo(points[0].x, paddingTop + chartHeight)
                    fillPath.lineTo(points[0].x, points[0].y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val conX1 = p0.x + stepX / 2f
                        val conY1 = p0.y
                        val conX2 = p1.x - stepX / 2f
                        val conY2 = p1.y

                        strokePath.cubicTo(conX1, conY1, conX2, conY2, p1.x, p1.y)
                        fillPath.cubicTo(conX1, conY1, conX2, conY2, p1.x, p1.y)
                    }

                    fillPath.lineTo(points.last().x, paddingTop + chartHeight)
                    fillPath.close()

                    // Draw gradient fill
                    val fillBrush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent),
                        startY = paddingTop,
                        endY = paddingTop + chartHeight
                    )
                    drawPath(path = fillPath, brush = fillBrush)

                    // Draw path outline
                    drawPath(
                        path = strokePath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // 3. Draw dots & Day labels
                for (i in points.indices) {
                    val pt = points[i]
                    // Outer pulse highlight for active days
                    if (dailyXpValues[i] > 0) {
                        drawCircle(
                            color = secondaryColor.copy(alpha = 0.25f),
                            radius = 6.dp.toPx(),
                            center = pt
                        )
                    }
                    // Inner solid point
                    drawCircle(
                        color = if (dailyXpValues[i] > 0) primaryColor else labelColor.copy(alpha = 0.4f),
                        radius = 4.dp.toPx(),
                        center = pt
                    )

                    // Draw day text labels underneath
                    if (i < days.size) {
                        val label = days[i]
                        val measuredText = textMeasurer.measure(
                            text = label,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 10.sp,
                                color = labelColor
                            )
                        )
                        drawText(
                            textLayoutResult = measuredText,
                            topLeft = Offset(pt.x - measuredText.size.width / 2f, paddingTop + chartHeight + 4.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
