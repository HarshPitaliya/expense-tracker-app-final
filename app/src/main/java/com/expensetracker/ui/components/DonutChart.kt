package com.expensetracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun DonutChart(
    data: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 60f, // ✅ Increased from 40f to 60f for fuller appearance
    startAngle: Float = -90f
) {
    Box(
        modifier = modifier.aspectRatio(1f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val total = data.sum()
            if (total <= 0) return@Canvas

            val canvasSize = min(size.width, size.height)
            val radius = (canvasSize / 2) - strokeWidth
            val center = Offset(size.width / 2, size.height / 2)

            var currentAngle = startAngle

            data.forEachIndexed { index, value ->
                val sweepAngle = (value / total * 360)
                val color = colors.getOrElse(index) { colors.lastOrNull() ?: Color.Gray }

                drawArc(
                    color = color,
                    startAngle = currentAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )

                currentAngle += sweepAngle
            }
        }
    }
}

// Enhanced version with value formatting
@Composable
fun CategoryDonutChartWithValues(
    categoryData: List<Pair<String, Double>>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 100f, // ✅ Increased default stroke width
    currencyFormatter: java.text.NumberFormat? = null
) {
    val data = categoryData.map { it.second.toFloat() }
    val total = data.sum()

    if (data.isEmpty() || total <= 0) {
        Box(
            modifier = modifier.aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(18.dp), // ✅ Increased spacing
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donut Chart
        DonutChart(
            data = data,
            colors = colors,
            strokeWidth = strokeWidth,
            modifier = Modifier.weight(0.55f)
        )

        // Enhanced Legend with values
        Column(
            modifier = Modifier.weight(0.45f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            categoryData.forEachIndexed { index, (label, value) ->
                val percentage = ((value / total) * 100).toInt()
                val color = colors.getOrElse(index) { colors.lastOrNull() ?: Color.Gray }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Color indicator
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(color)
                    )

                    // Label, percentage, and value
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (currencyFormatter != null) {
                                Text(
                                    text = currencyFormatter.format(value),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Keep compatibility with Overview screen
@Composable
fun SimpleDonutChart(
    data: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 60f // ✅ Increased default for Overview screen consistency
) {
    DonutChart(
        data = data,
        colors = colors,
        modifier = modifier,
        strokeWidth = strokeWidth
    )
}
