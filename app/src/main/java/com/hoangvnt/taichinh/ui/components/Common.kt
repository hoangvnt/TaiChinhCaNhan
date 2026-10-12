@file:OptIn(ExperimentalMaterial3Api::class)

package com.hoangvnt.taichinh.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hoangvnt.taichinh.util.formatDate
import com.hoangvnt.taichinh.util.formatMoney
import com.hoangvnt.taichinh.util.formatMonth
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@Composable
fun BackTopBar(title: String, nav: NavController, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
        },
        actions = { actions() },
    )
}

@Composable
fun MoneyText(
    amount: Long,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    prefix: String = "",
) {
    Text(prefix + formatMoney(amount), modifier = modifier, color = color, style = style, fontWeight = FontWeight.SemiBold, maxLines = 1)
}

/** Hiển thị số có dấu chấm phân cách hàng nghìn trong khi giá trị thực chỉ gồm chữ số. */
class ThousandsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val out = StringBuilder()
        val origToTrans = IntArray(digits.length + 1)
        for (i in digits.indices) {
            if (i > 0 && (digits.length - i) % 3 == 0) out.append('.')
            origToTrans[i] = out.length
            out.append(digits[i])
        }
        origToTrans[digits.length] = out.length
        val transToOrig = IntArray(out.length + 1)
        var count = 0
        for (t in 0..out.length) {
            transToOrig[t] = count
            if (t < out.length && out[t] != '.') count++
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = origToTrans[offset.coerceIn(0, digits.length)]
            override fun transformedToOriginal(offset: Int) = transToOrig[offset.coerceIn(0, out.length)]
        }
        return TransformedText(AnnotatedString(out.toString()), mapping)
    }
}

@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { s -> onValueChange(s.filter { it.isDigit() }.trimStart('0').take(15)) },
        label = { Text(label) },
        suffix = { Text("₫") },
        singleLine = true,
        visualTransformation = ThousandsTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.titleLarge,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Ô chọn ngày. Giá trị là epoch millis theo giờ địa phương. */
@Composable
fun DateField(
    label: String,
    millis: Long,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = if (millis > 0) formatDate(millis) else "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.CalendarMonth, null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Lớp phủ để bắt sự kiện bấm (TextField readOnly vẫn nuốt click)
        Box(
            Modifier
                .matchParentSize()
                .clickable { open = true }
        )
    }
    if (open) {
        val initialUtc = if (millis > 0) {
            Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } else null
        val state = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { utc ->
                        val d = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        val old = if (millis > 0) Instant.ofEpochMilli(millis).atZone(zone).toLocalTime() else java.time.LocalTime.NOON
                        onChange(d.atTime(old).atZone(zone).toInstant().toEpochMilli())
                    }
                    open = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Hủy") } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.let(optionLabel) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(optionLabel(opt)) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun MonthSelector(month: YearMonth, onChange: (YearMonth) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        IconButton(onClick = { onChange(month.minusMonths(1)) }) { Icon(Icons.Default.ChevronLeft, "Tháng trước") }
        Text(formatMonth(month), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(150.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onChange(month.plusMonths(1)) }) { Icon(Icons.Default.ChevronRight, "Tháng sau") }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val m = modifier.fillMaxWidth()
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(16.dp)) {
            if (title != null || action != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    action?.invoke()
                }
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = m, content = inner)
    } else {
        Card(modifier = m, content = inner)
    }
}

@Composable
fun EmojiBadge(emoji: String, color: Color = MaterialTheme.colorScheme.secondaryContainer, size: Int = 40) {
    Surface(shape = CircleShape, color = color, modifier = Modifier.size(size.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(emoji, style = if (size >= 40) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ProgressBar(ratio: Float, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { ratio.coerceIn(0f, 1f) },
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp),
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "Xóa",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
fun LabelValue(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

fun LocalDate.isTodayOrBefore(today: LocalDate) = !isAfter(today)
