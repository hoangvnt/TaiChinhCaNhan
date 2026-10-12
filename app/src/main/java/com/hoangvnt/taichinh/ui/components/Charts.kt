package com.hoangvnt.taichinh.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hoangvnt.taichinh.util.formatCompact

data class Slice(val value: Long, val color: Color)

/** Biểu đồ vành khuyên. */
@Composable
fun DonutChart(slices: List<Slice>, modifier: Modifier = Modifier, thickness: Dp = 28.dp, center: @Composable () -> Unit = {}) {
    val total = slices.sumOf { it.value }.toFloat()
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = thickness.toPx()
            val d = size.minDimension - stroke
            val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
            val arcSize = Size(d, d)
            if (total <= 0f) {
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                return@Canvas
            }
            var start = -90f
            slices.forEach { s ->
                val sweep = s.value / total * 360f
                drawArc(s.color, start, sweep, false, topLeft, arcSize, style = Stroke(stroke))
                start += sweep
            }
        }
        center()
    }
}

data class BarGroup(val label: String, val income: Long, val expense: Long)

/** Biểu đồ cột thu/chi theo tháng. */
@Composable
fun IncomeExpenseBars(
    groups: List<BarGroup>,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 160.dp,
) {
    val max = groups.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1) ?: 1
    Column(modifier.fillMaxWidth()) {
        Text(
            "Cao nhất: ${formatCompact(max)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(chartHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            groups.forEach { g ->
                Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.Bottom) {
                    Bar(g.income.toFloat() / max, incomeColor)
                    Spacer(Modifier.width(2.dp))
                    Bar(g.expense.toFloat() / max, expenseColor)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            groups.forEach { g ->
                Text(g.label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
            Legend(incomeColor, "Thu")
            Spacer(Modifier.width(16.dp))
            Legend(expenseColor, "Chi")
        }
    }
}

@Composable
private fun Bar(ratio: Float, color: Color) {
    Box(
        Modifier
            .width(12.dp)
            .fillMaxHeight(ratio.coerceIn(0.01f, 1f))
            .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
    )
}

@Composable
fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
