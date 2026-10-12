package com.hoangvnt.taichinh.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val moneyFormat = DecimalFormat("#,##0", DecimalFormatSymbols().apply {
    groupingSeparator = '.'
    decimalSeparator = ','
})

fun formatNumber(value: Long): String = moneyFormat.format(value)

fun formatMoney(value: Long): String = "${moneyFormat.format(value)} ₫"

fun formatSignedMoney(value: Long): String =
    (if (value > 0) "+" else if (value < 0) "−" else "") + formatMoney(abs(value))

/** Dạng rút gọn: 1,2 tỷ / 3,5 tr / 250 k */
fun formatCompact(value: Long): String {
    val a = abs(value).toDouble()
    val sign = if (value < 0) "-" else ""
    fun f(x: Double) = if (x >= 100 || x % 1.0 == 0.0) "%.0f".format(x) else "%.1f".format(x).replace('.', ',')
    return when {
        a >= 1_000_000_000 -> "$sign${f(a / 1_000_000_000)} tỷ"
        a >= 1_000_000 -> "$sign${f(a / 1_000_000)} tr"
        a >= 1_000 -> "$sign${f(a / 1_000)} k"
        else -> "$sign${a.toLong()}"
    }
}

val zone: ZoneId get() = ZoneId.systemDefault()

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun LocalDate.startMillis(): Long = atStartOfDay(zone).toInstant().toEpochMilli()

fun YearMonth.startMillis(): Long = atDay(1).startMillis()

fun YearMonth.endMillis(): Long = plusMonths(1).atDay(1).startMillis()

fun Long.toYearMonth(): YearMonth = YearMonth.from(toLocalDate())

private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun formatDate(millis: Long): String = millis.toLocalDate().format(dateFmt)

fun formatMonth(ym: YearMonth): String = "Tháng ${ym.monthValue}/${ym.year}"

fun formatDayHeader(date: LocalDate): String {
    val today = LocalDate.now(zone)
    val dow = when (date.dayOfWeek.value) {
        1 -> "Thứ Hai"; 2 -> "Thứ Ba"; 3 -> "Thứ Tư"; 4 -> "Thứ Năm"
        5 -> "Thứ Sáu"; 6 -> "Thứ Bảy"; else -> "Chủ Nhật"
    }
    val label = when (date) {
        today -> "Hôm nay"
        today.minusDays(1) -> "Hôm qua"
        else -> dow
    }
    return "$label, ${date.format(dateFmt)}"
}

/** Chuyển chuỗi nhập (chỉ chữ số) sang Long an toàn. */
fun String.toAmount(): Long = filter { it.isDigit() }.take(15).toLongOrNull() ?: 0L
