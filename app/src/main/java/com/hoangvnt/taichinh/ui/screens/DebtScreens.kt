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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hoangvnt.taichinh.data.model.Debt
import com.hoangvnt.taichinh.data.model.DebtKind
import com.hoangvnt.taichinh.data.model.DebtPayment
import com.hoangvnt.taichinh.domain.Calc
import com.hoangvnt.taichinh.ui.MainViewModel
import com.hoangvnt.taichinh.ui.Routes
import com.hoangvnt.taichinh.ui.components.AmountField
import com.hoangvnt.taichinh.ui.components.BackTopBar
import com.hoangvnt.taichinh.ui.components.ConfirmDialog
import com.hoangvnt.taichinh.ui.components.DateField
import com.hoangvnt.taichinh.ui.components.DropdownField
import com.hoangvnt.taichinh.ui.components.EmptyState
import com.hoangvnt.taichinh.ui.components.LabelValue
import com.hoangvnt.taichinh.ui.components.ProgressBar
import com.hoangvnt.taichinh.ui.components.SectionCard
import com.hoangvnt.taichinh.ui.theme.expenseColor
import com.hoangvnt.taichinh.ui.theme.incomeColor
import com.hoangvnt.taichinh.util.formatDate
import com.hoangvnt.taichinh.util.formatMoney
import com.hoangvnt.taichinh.util.toAmount

@Composable
fun DebtEditScreen(vm: MainViewModel, nav: NavController, id: String) {
    val debts by vm.debts.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val existing = debts.firstOrNull { it.id == id }

    var initialized by rememberSaveable { mutableStateOf(id.isEmpty()) }
    var kind by rememberSaveable { mutableStateOf(DebtKind.BORROW) }
    var person by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var dueDate by rememberSaveable { mutableStateOf(0L) }
    var note by rememberSaveable { mutableStateOf("") }
    var recordToWallet by rememberSaveable { mutableStateOf(true) }
    var walletId by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(existing) {
        if (!initialized && existing != null) {
            kind = existing.kind
            person = existing.person
            amount = existing.amount.toString()
            rate = if (existing.interestRate > 0) existing.interestRate.toString() else ""
            startDate = existing.startDate
            dueDate = existing.dueDate
            note = existing.note
            initialized = true
        }
    }
    LaunchedEffect(wallets) { if (walletId.isEmpty()) walletId = wallets.firstOrNull()?.id.orEmpty() }

    Scaffold(topBar = { BackTopBar(if (id.isEmpty()) "Thêm khoản vay" else "Sửa khoản vay", nav) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val kinds = listOf(DebtKind.BORROW to "Tôi đi vay", DebtKind.LEND to "Tôi cho vay")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                kinds.forEachIndexed { i, (k, label) ->
                    SegmentedButton(selected = kind == k, onClick = { kind = k }, shape = SegmentedButtonDefaults.itemShape(i, kinds.size)) { Text(label) }
                }
            }
            OutlinedTextField(
                person, { person = it },
                label = { Text(if (kind == DebtKind.BORROW) "Vay của ai" else "Cho ai vay") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            AmountField(amount, { amount = it }, "Số tiền gốc")
            OutlinedTextField(
                rate, { s -> rate = s.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.') },
                label = { Text("Lãi suất %/năm (tùy chọn)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            DateField("Ngày bắt đầu", startDate, { startDate = it })
            DateField("Hạn trả (tùy chọn)", dueDate, { dueDate = it })
            if (dueDate > 0) TextButton(onClick = { dueDate = 0 }) { Text("Bỏ hạn trả") }
            OutlinedTextField(note, { note = it }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth())

            if (id.isEmpty() && wallets.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(recordToWallet, { recordToWallet = it })
                    Text(if (kind == DebtKind.BORROW) "Ghi tiền vay vào ví" else "Trừ tiền cho vay khỏi ví")
                }
                if (recordToWallet) {
                    DropdownField("Ví", wallets, wallets.firstOrNull { it.id == walletId }, { "${it.icon} ${it.name}" }, { walletId = it.id })
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    error = when {
                        person.isBlank() -> "Vui lòng nhập tên người"
                        amount.toAmount() <= 0 -> "Vui lòng nhập số tiền"
                        else -> null
                    }
                    if (error == null) {
                        val base = existing ?: Debt()
                        vm.saveDebt(
                            base.copy(
                                kind = kind, person = person.trim(), amount = amount.toAmount(),
                                interestRate = rate.toDoubleOrNull() ?: 0.0, startDate = startDate,
                                dueDate = dueDate, note = note.trim(),
                            ),
                            walletId = if (id.isEmpty() && recordToWallet) walletId else "",
                        )
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Lưu") }
        }
    }
}

@Composable
fun DebtDetailScreen(vm: MainViewModel, nav: NavController, id: String) {
    val debts by vm.debts.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val d = debts.firstOrNull { it.id == id }
    var payDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BackTopBar(d?.person ?: "Khoản vay", nav) {
                IconButton(onClick = { nav.navigate(Routes.debtEdit(id)) }) { Icon(Icons.Default.Edit, "Sửa") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Xóa") }
            }
        },
    ) { padding ->
        if (d == null) {
            EmptyState("Không tìm thấy khoản vay", Modifier.padding(padding))
            return@Scaffold
        }
        val remaining = Calc.debtRemaining(d)
        val interest = Calc.debtInterest(d, if (d.dueDate > 0) d.dueDate else System.currentTimeMillis())
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(title = if (d.kind == DebtKind.BORROW) "📥 Tôi vay của ${d.person}" else "📤 Tôi cho ${d.person} vay") {
                    ProgressBar(if (d.amount > 0) Calc.debtPaid(d).toFloat() / d.amount else 0f, incomeColor())
                    Spacer(Modifier.height(8.dp))
                    LabelValue("Số tiền gốc", formatMoney(d.amount))
                    LabelValue("Đã trả", formatMoney(Calc.debtPaid(d)), incomeColor())
                    LabelValue("Còn lại", formatMoney(remaining), if (remaining > 0) expenseColor() else incomeColor())
                    if (d.interestRate > 0) {
                        LabelValue("Lãi suất", "${d.interestRate}%/năm")
                        LabelValue(if (d.dueDate > 0) "Lãi ước tính đến hạn" else "Lãi ước tính đến nay", formatMoney(interest))
                    }
                    LabelValue("Ngày bắt đầu", formatDate(d.startDate))
                    LabelValue("Hạn trả", if (d.dueDate > 0) formatDate(d.dueDate) else "Không có")
                    if (d.note.isNotBlank()) LabelValue("Ghi chú", d.note)
                }
            }
            item {
                Button(onClick = { payDialog = true }, enabled = remaining > 0, modifier = Modifier.fillMaxWidth()) {
                    Text(if (d.kind == DebtKind.BORROW) "Ghi nhận trả nợ" else "Ghi nhận thu nợ")
                }
            }
            item { Text("Lịch sử trả", style = MaterialTheme.typography.titleMedium) }
            if (d.payments.isEmpty()) item { EmptyState("Chưa có lần trả nào") }
            itemsIndexed(d.payments) { index, p ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(formatMoney(p.amount), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            listOf(formatDate(p.date), p.note).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { vm.removeDebtPayment(d, index) }) { Icon(Icons.Default.Close, "Xóa lần trả") }
                }
            }
        }
        if (payDialog) {
            var amount by remember { mutableStateOf(remaining.toString()) }
            var date by remember { mutableStateOf(System.currentTimeMillis()) }
            var note by remember { mutableStateOf("") }
            var toWallet by remember { mutableStateOf(wallets.isNotEmpty()) }
            var walletId by remember { mutableStateOf(wallets.firstOrNull()?.id.orEmpty()) }
            AlertDialog(
                onDismissRequest = { payDialog = false },
                title = { Text(if (d.kind == DebtKind.BORROW) "Trả nợ" else "Thu nợ") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AmountField(amount, { amount = it }, "Số tiền")
                        DateField("Ngày", date, { date = it })
                        OutlinedTextField(note, { note = it }, label = { Text("Ghi chú") }, singleLine = true)
                        if (wallets.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(toWallet, { toWallet = it })
                                Text("Ghi giao dịch vào ví")
                            }
                            if (toWallet) DropdownField("Ví", wallets, wallets.firstOrNull { it.id == walletId }, { "${it.icon} ${it.name}" }, { walletId = it.id })
                        }
                    }
                },
                confirmButton = {
                    TextButton(enabled = amount.toAmount() > 0, onClick = {
                        vm.addDebtPayment(d, DebtPayment(amount.toAmount(), date, note.trim()), if (toWallet) walletId else "")
                        payDialog = false
                    }) { Text("Lưu") }
                },
                dismissButton = { TextButton(onClick = { payDialog = false }) { Text("Hủy") } },
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Xóa khoản vay?",
            text = "Khoản vay và lịch sử trả sẽ bị xóa. Các giao dịch đã ghi vào ví vẫn được giữ.",
            onConfirm = {
                vm.deleteDebt(id)
                nav.popBackStack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
