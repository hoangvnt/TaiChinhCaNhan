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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hoangvnt.taichinh.data.model.Budget
import com.hoangvnt.taichinh.data.model.Debt
import com.hoangvnt.taichinh.data.model.DebtKind
import com.hoangvnt.taichinh.data.model.Goal
import com.hoangvnt.taichinh.data.model.TxType
import com.hoangvnt.taichinh.domain.Calc
import com.hoangvnt.taichinh.ui.BottomBar
import com.hoangvnt.taichinh.ui.MainViewModel
import com.hoangvnt.taichinh.ui.Routes
import com.hoangvnt.taichinh.ui.components.AmountField
import com.hoangvnt.taichinh.ui.components.DateField
import com.hoangvnt.taichinh.ui.components.DropdownField
import com.hoangvnt.taichinh.ui.components.EmojiBadge
import com.hoangvnt.taichinh.ui.components.EmptyState
import com.hoangvnt.taichinh.ui.components.LabelValue
import com.hoangvnt.taichinh.ui.components.ProgressBar
import com.hoangvnt.taichinh.ui.components.SectionCard
import com.hoangvnt.taichinh.ui.theme.WarningColor
import com.hoangvnt.taichinh.ui.theme.expenseColor
import com.hoangvnt.taichinh.ui.theme.incomeColor
import com.hoangvnt.taichinh.util.endMillis
import com.hoangvnt.taichinh.util.formatDate
import com.hoangvnt.taichinh.util.formatMoney
import com.hoangvnt.taichinh.util.formatMonth
import com.hoangvnt.taichinh.util.startMillis
import com.hoangvnt.taichinh.util.toAmount
import com.hoangvnt.taichinh.util.zone
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun PlanScreen(vm: MainViewModel, nav: NavController) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var budgetDialog by remember { mutableStateOf<Budget?>(null) }
    var goalDialog by remember { mutableStateOf<Goal?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Kế hoạch tài chính") }) },
        bottomBar = { BottomBar(nav) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                when (tab) {
                    0 -> budgetDialog = Budget()
                    1 -> goalDialog = Goal()
                    else -> nav.navigate(Routes.debtEdit())
                }
            }) { Icon(Icons.Default.Add, "Thêm") }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = tab) {
                listOf("Ngân sách", "Mục tiêu", "Nợ / Vay").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
            when (tab) {
                0 -> BudgetsTab(vm) { budgetDialog = it }
                1 -> GoalsTab(vm) { goalDialog = it }
                else -> DebtsTab(vm, nav)
            }
        }
    }

    budgetDialog?.let { b -> BudgetDialog(vm, b) { budgetDialog = null } }
    goalDialog?.let { g -> GoalDialog(vm, g) { goalDialog = null } }
}

// ===================== Ngân sách =====================

@Composable
private fun BudgetsTab(vm: MainViewModel, onEdit: (Budget) -> Unit) {
    val budgets by vm.budgets.collectAsStateWithLifecycle()
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val month = remember { YearMonth.now(zone) }
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val monthTxs = remember(txs) { Calc.inRange(txs, month.startMillis(), month.endMillis()) }
    val statuses = remember(budgets, monthTxs) { Calc.budgetStatuses(budgets, monthTxs).sortedByDescending { it.ratio } }
    val totalLimit = statuses.sumOf { it.budget.limit }
    val totalSpent = statuses.sumOf { it.spent }
    val today = LocalDate.now(zone)
    val daysLeft = month.lengthOfMonth() - today.dayOfMonth + 1

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(title = "Ngân sách ${formatMonth(month)}") {
                ProgressBar(
                    if (totalLimit > 0) totalSpent.toFloat() / totalLimit else 0f,
                    if (totalSpent > totalLimit) expenseColor() else MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                LabelValue("Đã chi", formatMoney(totalSpent))
                LabelValue("Tổng ngân sách", formatMoney(totalLimit))
                LabelValue("Còn lại", formatMoney(totalLimit - totalSpent), if (totalLimit >= totalSpent) incomeColor() else expenseColor())
                if (totalLimit > totalSpent && daysLeft > 0) {
                    LabelValue("Có thể chi mỗi ngày", formatMoney((totalLimit - totalSpent) / daysLeft))
                }
            }
        }
        if (statuses.isEmpty()) {
            item { EmptyState("Chưa đặt ngân sách.\nBấm + để đặt hạn mức chi tiêu hằng tháng cho từng danh mục.") }
        }
        items(statuses, key = { it.budget.id }) { s ->
            val c = catMap[s.budget.categoryId]
            val color = when {
                s.ratio >= 1f -> expenseColor()
                s.ratio >= 0.8f -> WarningColor
                else -> MaterialTheme.colorScheme.primary
            }
            SectionCard(onClick = { onEdit(s.budget) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(c?.icon ?: "❔")
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c?.name ?: "Danh mục đã xóa", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${formatMoney(s.spent)} / ${formatMoney(s.budget.limit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("${(s.ratio * 100).toInt()}%", color = color, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                ProgressBar(s.ratio, color)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (s.remaining >= 0) "Còn ${formatMoney(s.remaining)}" else "Vượt ${formatMoney(-s.remaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (s.remaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else expenseColor(),
                )
            }
        }
    }
}

@Composable
private fun BudgetDialog(vm: MainViewModel, budget: Budget, onDismiss: () -> Unit) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val expenseCats = categories.filter { it.type == TxType.EXPENSE }
    var categoryId by remember { mutableStateOf(budget.categoryId) }
    var limit by remember { mutableStateOf(if (budget.limit > 0) budget.limit.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budget.id.isEmpty()) "Đặt ngân sách" else "Sửa ngân sách") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DropdownField(
                    label = "Danh mục chi",
                    options = expenseCats,
                    selected = expenseCats.firstOrNull { it.id == categoryId },
                    optionLabel = { "${it.icon} ${it.name}" },
                    onSelect = { categoryId = it.id },
                )
                AmountField(limit, { limit = it }, "Hạn mức mỗi tháng")
            }
        },
        confirmButton = {
            TextButton(
                enabled = categoryId.isNotEmpty() && limit.toAmount() > 0,
                onClick = {
                    vm.saveBudget(budget.copy(categoryId = categoryId, limit = limit.toAmount()))
                    onDismiss()
                },
            ) { Text("Lưu") }
        },
        dismissButton = {
            Row {
                if (budget.id.isNotEmpty()) {
                    TextButton(onClick = {
                        vm.deleteBudget(budget.id)
                        onDismiss()
                    }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Hủy") }
            }
        },
    )
}

// ===================== Mục tiêu =====================

@Composable
private fun GoalsTab(vm: MainViewModel, onEdit: (Goal) -> Unit) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    var depositFor by remember { mutableStateOf<Goal?>(null) }
    val today = LocalDate.now(zone)
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (goals.isEmpty()) {
            item { EmptyState("Chưa có mục tiêu tiết kiệm.\nBấm + để tạo mục tiêu (mua nhà, du lịch, quỹ dự phòng...).") }
        } else {
            item {
                val saved = goals.sumOf { it.saved }
                val target = goals.sumOf { it.target }
                SectionCard(title = "Tổng tiết kiệm") {
                    ProgressBar(if (target > 0) saved.toFloat() / target else 0f, incomeColor())
                    Spacer(Modifier.height(8.dp))
                    LabelValue("Đã tích lũy", formatMoney(saved), incomeColor())
                    LabelValue("Tổng mục tiêu", formatMoney(target))
                    val monthly = goals.sumOf { Calc.goalMonthlyNeeded(it, today, zone) ?: 0L }
                    if (monthly > 0) LabelValue("Cần để dành mỗi tháng", formatMoney(monthly))
                }
            }
        }
        items(goals, key = { it.id }) { g ->
            val ratio = if (g.target > 0) g.saved.toFloat() / g.target else 0f
            val needed = Calc.goalMonthlyNeeded(g, today, zone)
            SectionCard(onClick = { onEdit(g) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(g.icon)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.name, style = MaterialTheme.typography.titleSmall)
                        if (g.deadline > 0) {
                            Text("Hạn: ${formatDate(g.deadline)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("${(ratio * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                ProgressBar(ratio, incomeColor())
                Spacer(Modifier.height(4.dp))
                LabelValue("Đã có", "${formatMoney(g.saved)} / ${formatMoney(g.target)}")
                if (g.saved >= g.target) {
                    Text("🎉 Đã hoàn thành mục tiêu!", color = incomeColor(), style = MaterialTheme.typography.bodyMedium)
                } else if (needed != null) {
                    LabelValue("Cần để dành/tháng", formatMoney(needed))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { depositFor = g }, modifier = Modifier.fillMaxWidth()) { Text("Nạp / rút tiền") }
            }
        }
    }
    depositFor?.let { g -> GoalDepositDialog(vm, g) { depositFor = null } }
}

@Composable
private fun GoalDialog(vm: MainViewModel, goal: Goal, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(goal.name) }
    var icon by remember { mutableStateOf(goal.icon) }
    var target by remember { mutableStateOf(if (goal.target > 0) goal.target.toString() else "") }
    var saved by remember { mutableStateOf(if (goal.saved > 0) goal.saved.toString() else "") }
    var deadline by remember { mutableStateOf(goal.deadline) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal.id.isEmpty()) "Mục tiêu mới" else "Sửa mục tiêu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(icon, { icon = it.take(4) }, label = { Text("Biểu tượng") }, singleLine = true, modifier = Modifier.width(96.dp))
                    OutlinedTextField(name, { name = it }, label = { Text("Tên mục tiêu") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                AmountField(target, { target = it }, "Số tiền mục tiêu")
                AmountField(saved, { saved = it }, "Đã có")
                DateField("Hạn hoàn thành (tùy chọn)", deadline, { deadline = it })
                if (deadline > 0) TextButton(onClick = { deadline = 0 }) { Text("Bỏ hạn") }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && target.toAmount() > 0,
                onClick = {
                    vm.saveGoal(goal.copy(name = name.trim(), icon = icon.ifBlank { "🎯" }, target = target.toAmount(), saved = saved.toAmount(), deadline = deadline))
                    onDismiss()
                },
            ) { Text("Lưu") }
        },
        dismissButton = {
            Row {
                if (goal.id.isNotEmpty()) {
                    TextButton(onClick = {
                        vm.deleteGoal(goal.id)
                        onDismiss()
                    }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Hủy") }
            }
        },
    )
}

@Composable
private fun GoalDepositDialog(vm: MainViewModel, goal: Goal, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var withdraw by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${goal.icon} ${goal.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val opts = listOf(false to "Nạp thêm", true to "Rút ra")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    opts.forEachIndexed { i, (k, label) ->
                        SegmentedButton(selected = withdraw == k, onClick = { withdraw = k }, shape = SegmentedButtonDefaults.itemShape(i, opts.size)) { Text(label) }
                    }
                }
                AmountField(amount, { amount = it }, "Số tiền")
                Text("Hiện có: ${formatMoney(goal.saved)}", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = amount.toAmount() > 0, onClick = {
                val v = amount.toAmount()
                vm.addToGoal(goal, if (withdraw) -v else v)
                onDismiss()
            }) { Text("Xác nhận") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

// ===================== Nợ / Vay =====================

@Composable
private fun DebtsTab(vm: MainViewModel, nav: NavController) {
    val debts by vm.debts.collectAsStateWithLifecycle()
    val borrow = debts.filter { it.kind == DebtKind.BORROW }
    val lend = debts.filter { it.kind == DebtKind.LEND }
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(title = "Tổng quan") {
                LabelValue("Tôi đang nợ", formatMoney(borrow.sumOf { Calc.debtRemaining(it) }), expenseColor())
                LabelValue("Người khác nợ tôi", formatMoney(lend.sumOf { Calc.debtRemaining(it) }), incomeColor())
            }
        }
        if (debts.isEmpty()) {
            item { EmptyState("Chưa có khoản vay nào.\nBấm + để ghi khoản đi vay hoặc cho vay.") }
        }
        if (borrow.isNotEmpty()) {
            item { Text("Tôi đi vay", style = MaterialTheme.typography.titleMedium) }
            items(borrow, key = { it.id }) { d -> DebtCard(d) { nav.navigate(Routes.debtDetail(d.id)) } }
        }
        if (lend.isNotEmpty()) {
            item { Text("Tôi cho vay", style = MaterialTheme.typography.titleMedium) }
            items(lend, key = { it.id }) { d -> DebtCard(d) { nav.navigate(Routes.debtDetail(d.id)) } }
        }
    }
}

@Composable
private fun DebtCard(d: Debt, onClick: () -> Unit) {
    val remaining = Calc.debtRemaining(d)
    val ratio = if (d.amount > 0) Calc.debtPaid(d).toFloat() / d.amount else 0f
    val overdue = d.dueDate in 1 until System.currentTimeMillis() && remaining > 0
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(if (d.kind == DebtKind.BORROW) "📥" else "📤")
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(d.person, style = MaterialTheme.typography.titleSmall)
                val due = if (d.dueDate > 0) "Hạn: ${formatDate(d.dueDate)}" else "Không có hạn"
                Text(
                    if (overdue) "$due · QUÁ HẠN" else due,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) expenseColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (remaining == 0L) "Đã xong ✅" else formatMoney(remaining), fontWeight = FontWeight.Bold)
                Text("/ ${formatMoney(d.amount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(ratio, incomeColor())
    }
}
