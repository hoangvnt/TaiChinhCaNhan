@file:OptIn(ExperimentalMaterial3Api::class)

package com.hoangvnt.taichinh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hoangvnt.taichinh.data.model.TxType
import com.hoangvnt.taichinh.domain.Calc
import com.hoangvnt.taichinh.ui.BottomBar
import com.hoangvnt.taichinh.ui.MainViewModel
import com.hoangvnt.taichinh.ui.components.BarGroup
import com.hoangvnt.taichinh.ui.components.DonutChart
import com.hoangvnt.taichinh.ui.components.EmptyState
import com.hoangvnt.taichinh.ui.components.IncomeExpenseBars
import com.hoangvnt.taichinh.ui.components.LabelValue
import com.hoangvnt.taichinh.ui.components.MonthSelector
import com.hoangvnt.taichinh.ui.components.SectionCard
import com.hoangvnt.taichinh.ui.components.Slice
import com.hoangvnt.taichinh.ui.theme.expenseColor
import com.hoangvnt.taichinh.ui.theme.incomeColor
import com.hoangvnt.taichinh.util.endMillis
import com.hoangvnt.taichinh.util.formatCompact
import com.hoangvnt.taichinh.util.formatMoney
import com.hoangvnt.taichinh.util.formatSignedMoney
import com.hoangvnt.taichinh.util.startMillis
import com.hoangvnt.taichinh.util.zone
import java.time.YearMonth

@Composable
fun ReportsScreen(vm: MainViewModel, nav: NavController) {
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    var month by rememberSaveable { mutableStateOf(YearMonth.now(zone)) }
    var type by rememberSaveable { mutableStateOf(TxType.EXPENSE) }

    val catMap = remember(categories) { categories.associateBy { it.id } }
    val monthTxs = remember(txs, month) { Calc.inRange(txs, month.startMillis(), month.endMillis()) }
    val summary = remember(monthTxs) { Calc.summary(monthTxs) }
    val byCat = remember(monthTxs, type) {
        Calc.byCategory(monthTxs.filter { it.debtId.isEmpty() }, type).entries.sortedByDescending { it.value }
    }
    val total = byCat.sumOf { it.value }
    val bars = remember(txs, month) {
        (5 downTo 0).map { i ->
            val m = month.minusMonths(i.toLong())
            val s = Calc.summary(Calc.inRange(txs, m.startMillis(), m.endMillis()))
            BarGroup("T${m.monthValue}", s.income, s.expense)
        }
    }
    val yearSummary = remember(txs, month) {
        val y = YearMonth.of(month.year, 1)
        Calc.summary(Calc.inRange(txs, y.startMillis(), y.plusMonths(12).startMillis()))
    }
    val days = month.lengthOfMonth()
    val fallback = MaterialTheme.colorScheme.outline

    Scaffold(
        topBar = { TopAppBar(title = { Text("Báo cáo") }) },
        bottomBar = { BottomBar(nav) },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MonthSelector(month, { month = it }) }
            item {
                SectionCard(title = "Tổng quan tháng") {
                    LabelValue("Tổng thu", formatMoney(summary.income), incomeColor())
                    LabelValue("Tổng chi", formatMoney(summary.expense), expenseColor())
                    LabelValue("Chênh lệch", formatSignedMoney(summary.net))
                    LabelValue("Chi trung bình/ngày", formatMoney(summary.expense / days))
                    if (summary.income > 0) {
                        val rate = (summary.net * 100 / summary.income)
                        LabelValue("Tỷ lệ tiết kiệm", "$rate%")
                    }
                }
            }
            item {
                SectionCard(title = "Theo danh mục") {
                    val types = listOf(TxType.EXPENSE to "Chi tiêu", TxType.INCOME to "Thu nhập")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        types.forEachIndexed { i, (k, label) ->
                            SegmentedButton(
                                selected = type == k,
                                onClick = { type = k },
                                shape = SegmentedButtonDefaults.itemShape(i, types.size),
                            ) { Text(label) }
                        }
                    }
                    Spacer(Modifier.size(16.dp))
                    if (byCat.isEmpty()) {
                        EmptyState("Chưa có dữ liệu")
                    } else {
                        val slices = byCat.map { (cid, v) -> Slice(v, catMap[cid]?.color?.let { Color(it) } ?: fallback) }
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            DonutChart(slices, Modifier.size(200.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Tổng", style = MaterialTheme.typography.labelMedium)
                                    Text(formatCompact(total), style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                        Spacer(Modifier.size(16.dp))
                        byCat.forEach { (cid, v) ->
                            val c = catMap[cid]
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(12.dp).background(c?.color?.let { Color(it) } ?: fallback, CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Text("${c?.icon ?: "❔"} ${c?.name ?: "Chưa phân loại"}", modifier = Modifier.weight(1f), maxLines = 1)
                                Text("${v * 100 / total.coerceAtLeast(1)}%  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatMoney(v))
                            }
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Thu chi 6 tháng") {
                    IncomeExpenseBars(bars, incomeColor(), expenseColor())
                }
            }
            item {
                SectionCard(title = "Năm ${month.year}") {
                    LabelValue("Tổng thu", formatMoney(yearSummary.income), incomeColor())
                    LabelValue("Tổng chi", formatMoney(yearSummary.expense), expenseColor())
                    LabelValue("Tích lũy", formatSignedMoney(yearSummary.net))
                }
            }
        }
    }
}
