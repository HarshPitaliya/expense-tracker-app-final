package com.expensetracker.ui.components

import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.graphics.withSave

@Composable
fun BarChart(
    data: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
    maxBarHeightFraction: Float = 0.8f, // fraction of height bars can use
    onBarClick: ((index: Int) -> Unit)? = null
) {
    require(data.size == labels.size) { "Data and labels must have the same size" }
    require(data.isNotEmpty()) { "Data list cannot be empty" }
    require(barColors.isNotEmpty()) { "Bar colors list cannot be empty" }

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                onBarClick?.let { action ->
                    // Placeholder for future bar hit testing if needed
                }
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBarChart(
                data = data,
                labels = labels,
                barColors = barColors,
                maxBarHeightFraction = maxBarHeightFraction,
                drawScope = this
            )
        }
    }
}

private fun drawBarChart(
    data: List<Float>,
    labels: List<String>,
    barColors: List<Color>,
    maxBarHeightFraction: Float,
    drawScope: DrawScope
) {
    val canvasWidth = drawScope.size.width
    val canvasHeight = drawScope.size.height

    val maxDataValue = data.maxOrNull() ?: 1f
    val normalizedMaxValue = if (maxDataValue == 0f) 1f else maxDataValue

    val maxBarHeight = canvasHeight * maxBarHeightFraction

    val slots = data.size
    val totalGapFraction = 0.3f // Fraction of width for gaps
    val totalGapWidth = canvasWidth * totalGapFraction
    val gapWidth = if (slots > 1) totalGapWidth / (slots - 1) else 0f
    val usableBarWidth = canvasWidth - totalGapWidth
    val dynamicBarWidth = usableBarWidth / slots

    val spaceBetweenBars = gapWidth
    val startX = 0f

    val axisY = canvasHeight - 20f
    drawScope.drawLine(
        color = Color.DarkGray,
        start = Offset(0f, axisY),
        end = Offset(canvasWidth, axisY),
        strokeWidth = 2f
    )

    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.BLACK
        textSize = 26f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    var currentX = startX
    for (i in data.indices) {
        val value = data[i]
        val barHeight = (value / normalizedMaxValue) * maxBarHeight
        val top = axisY - barHeight
        val color = barColors[i % barColors.size]

        // Draw bar fill
        drawScope.drawRect(color = color, topLeft = Offset(currentX, top), size = Size(dynamicBarWidth, barHeight))
        // Draw bar border
        drawScope.drawRect(
            color = color.copy(alpha = 0.8f),
            topLeft = Offset(currentX, top),
            size = Size(dynamicBarWidth, barHeight),
            style = Stroke(width = 3f)
        )

        // Draw value text above bar
        drawScope.drawContext.canvas.nativeCanvas.drawText(
            value.toInt().toString(),
            currentX + dynamicBarWidth / 2,
            top - 10f,
            textPaint
        )

        // Draw label below - for this simplified approach, labels correspond to month only once every two bars
        if (i % 2 == 0) {
            val label = labels[i].split("\n")[0] // Month name without suffix e.g. "Exp"
            val maxLabelWidth = dynamicBarWidth * 2
            drawScope.drawContext.canvas.nativeCanvas.apply {
                val staticLayout = StaticLayout.Builder.obtain(label, 0, label.length, textPaint, maxLabelWidth.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setMaxLines(1)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()

                withSave {
                    // Shift labelX position to dynamicBarWidth + half gap (between bars)
                    val labelX =
                        currentX + dynamicBarWidth + spaceBetweenBars + dynamicBarWidth / 2 - maxLabelWidth / 2
                    translate(labelX, axisY + 8f)
                    staticLayout.draw(this)
                }
            }
        }


        currentX += dynamicBarWidth + spaceBetweenBars
    }
}
