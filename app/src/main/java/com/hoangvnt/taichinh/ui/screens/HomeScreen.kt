@file:OptIn(ExperimentalMaterial3Api::class)

package com.hoangvnt.taichinh.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hoangvnt.taichinh.data.model.DebtKind
import com.hoangvnt.taichinh.domain.Calc
import com.hoangvnt.taichinh.ui.BottomBar
import com.hoangvnt.taichinh.ui.MainViewModel
import com.hoangvnt.taichinh.ui.Routes
import com.hoangvnt.taichinh.ui.components.EmojiBadge
import com.hoangvnt.taichinh.ui.components.EmptyState
import com.hoangvnt.taichinh.ui.components.LabelValue
import com.hoangvnt.taichinh.ui.components.MoneyText
import com.hoangvnt.taichinh.ui.components.ProgressBar
import com.hoangvnt.taichinh.ui.components.SectionCard
import com.hoangvnt.taichinh.ui.theme.WarningColor
import com.hoangvnt.taichinh.ui.theme.expenseColor
import com.hoangvnt.taichinh.ui.theme.incomeColor
import com.hoangvnt.taichinh.util.endMillis
import com.hoangvnt.taichinh.util.formatMoney
import com.hoangvnt.taichinh.util.formatMonth
import com.hoangvnt.taichinh.util.formatSignedMoney
import com.hoangvnt.taichinh.util.startMillis
import com.hoangvnt.taichinh.util.zone
import java.time.YearMonth

@Composable
fun HomeScreen(vm: MainViewModel, nav: NavController) {
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val balances by vm.balances.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val budgets by vm.budgets.collectAsStateWithLifecycle()
    val goals by vm.goals.collectAsStateWithLifecycle()
    val debts by vm.debts.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()

    val month = remember { YearMonth.now(zone) }
    val monthTxs = remember(txs) { Calc.inRange(txs, month.startMillis(), month.endMillis()) }
    val summary = remember(monthTxs) { Calc.summary(monthTxs) }
    val total = wallets.filter { it.includeInTotal }.sumOf { balances[it.id] ?: 0L }
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val walletMap = remember(wallets) { wallets.associateBy { it.id } }
    val budgetAlerts = remember(budgets, monthTxs) {
        Calc.budgetStatuses(budgets, monthTxs).filter { it.ratio >= 0.8f }.sortedByDescending { it.ratio }
    }
    val owe = debts.filter { it.kind == DebtKind.BORROW }.sumOf { Calc.debtRemaining(it) }
    val lent = debts.filter { it.kind == DebtKind.LEND }.sumOf { Calc.debtRemaining(it) }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Xin chào${user?.displayName?.let { ", ${it.substringAfterLast(' ')}" } ?: ""} 👋", style = MaterialTheme.typography.titleLarge)
                    Text(formatMonth(month), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            })
        },
        bottomBar = { BottomBar(nav) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.txEdit()) },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Thêm") },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Tổng số dư", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        MoneyText(total, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(12.dp))
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text("Thu tháng này", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                MoneyText(summary.income, color = incomeColor())
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Chi tháng này", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                MoneyText(summary.expense, color = expenseColor())
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Chênh lệch: ${formatSignedMoney(summary.net)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ví của tôi", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { nav.navigate(Routes.WALLETS) }) { Text("Quản lý") }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(wallets, key = { it.id }) { w ->
                        Card(onClick = { nav.navigate(Routes.WALLETS) }) {
                            Column(Modifier.padding(12.dp).width(140.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    EmojiBadge(w.icon, size = 28)
                                    Spacer(Modifier.width(8.dp))
                                    Text(w.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                }
                                Spacer(Modifier.height(8.dp))
                                MoneyText(balances[w.id] ?: 0, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            if (budgetAlerts.isNotEmpty()) {
                item {
                    SectionCard(title = "⚠️ Ngân sách cần chú ý", onClick = { nav.navigate(Routes.PLAN) }) {
                        budgetAlerts.take(4).forEach { s ->
                            val cat = catMap[s.budget.categoryId]
                            Text("${cat?.icon ?: ""} ${cat?.name ?: "Danh mục"}", style = MaterialTheme.typography.bodyMedium)
                            ProgressBar(s.ratio, if (s.ratio >= 1f) expenseColor() else WarningColor)
                            Text(
                                "${formatMoney(s.spent)} / ${formatMoney(s.budget.limit)} (${(s.ratio * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            if (goals.isNotEmpty() || owe > 0 || lent > 0) {
                item {
                    SectionCard(title = "Kế hoạch", onClick = { nav.navigate(Routes.PLAN) }) {
                        goals.take(3).forEach { g ->
                            Text("${g.icon} ${g.name}", style = MaterialTheme.typography.bodyMedium)
                            ProgressBar(if (g.target > 0) g.saved.toFloat() / g.target else 0f)
                            Text(
                                "${formatMoney(g.saved)} / ${formatMoney(g.target)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        if (owe > 0) LabelValue("Tôi đang nợ", formatMoney(owe), expenseColor())
                        if (lent > 0) LabelValue("Người khác nợ tôi", formatMoney(lent), incomeColor())
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Giao dịch gần đây", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { nav.navigate(Routes.TRANSACTIONS) }) { Text("Xem tất cả") }
                }
            }
            if (txs.isEmpty()) {
                item { EmptyState("Chưa có giao dịch nào.\nBấm \"Thêm\" để ghi khoản thu/chi đầu tiên.") }
            } else {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        txs.take(6).forEach { t ->
                            TransactionRow(t, catMap, walletMap) { nav.navigate(Routes.txEdit(t.id)) }
                        }
                    }
                }
            }
        }
    }
}
