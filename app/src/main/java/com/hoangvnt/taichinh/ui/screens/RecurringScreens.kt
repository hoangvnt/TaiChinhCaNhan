@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.hoangvnt.taichinh.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.hoangvnt.taichinh.data.model.Frequency
import com.hoangvnt.taichinh.data.model.RecurringRule
import com.hoangvnt.taichinh.data.model.TxType
import com.hoangvnt.taichinh.domain.Calc
import com.hoangvnt.taichinh.ui.MainViewModel
import com.hoangvnt.taichinh.ui.Routes
import com.hoangvnt.taichinh.ui.components.AmountField
import com.hoangvnt.taichinh.ui.components.BackTopBar
import com.hoangvnt.taichinh.ui.components.ConfirmDialog
import com.hoangvnt.taichinh.ui.components.DateField
import com.hoangvnt.taichinh.ui.components.DropdownField
import com.hoangvnt.taichinh.ui.components.EmojiBadge
import com.hoangvnt.taichinh.ui.components.EmptyState
import com.hoangvnt.taichinh.ui.components.SectionCard
import com.hoangvnt.taichinh.ui.theme.expenseColor
import com.hoangvnt.taichinh.ui.theme.incomeColor
import com.hoangvnt.taichinh.util.formatMoney
import com.hoangvnt.taichinh.util.startMillis
import com.hoangvnt.taichinh.util.toAmount
import com.hoangvnt.taichinh.util.zone
import java.time.LocalDate

@Composable
fun RecurringScreen(vm: MainViewModel, nav: NavController) {
    val rules by vm.recurring.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val walletMap = remember(wallets) { wallets.associateBy { it.id } }
    val today = LocalDate.now(zone)
    val sorted = rules.sortedWith(compareBy({ !it.active }, { Calc.nextDate(it, zone) }))

    Scaffold(
        topBar = { BackTopBar("Giao dịch định kỳ", nav) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(Routes.recurringEdit()) }) { Icon(Icons.Default.Add, "Thêm") }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Lương, tiền nhà, hóa đơn... Chế độ \"Tự động\" sẽ tự ghi giao dịch khi đến hạn; chế độ \"Chỉ nhắc\" sẽ gửi thông báo để bạn xác nhận.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rules.isEmpty()) item { EmptyState("Chưa có giao dịch định kỳ") }
            items(sorted, key = { it.id }) { r ->
                val next = Calc.nextDate(r, zone)
                val due = !next.isAfter(today)
                val cat = catMap[r.categoryId]
                SectionCard(onClick = { nav.navigate(Routes.recurringEdit(r.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(cat?.icon ?: "🔁")
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.name.ifBlank { cat?.name ?: "Định kỳ" }, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${Frequency.label(r.frequency)} · ${if (r.autoCreate) "Tự động" else "Chỉ nhắc"} · ${walletMap[r.walletId]?.name ?: "?"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                if (!r.active) "Đang tạm dừng" else "Kỳ tới: ${com.hoangvnt.taichinh.util.formatDate(next.startMillis())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (due && r.active) expenseColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            (if (r.type == TxType.INCOME) "+" else "−") + formatMoney(r.amount),
                            color = if (r.type == TxType.INCOME) incomeColor() else expenseColor(),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (due && r.active && !r.autoCreate) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.confirmRecurring(r) }, modifier = Modifier.weight(1f)) { Text("Ghi nhận") }
                            OutlinedButton(onClick = { vm.skipRecurring(r) }, modifier = Modifier.weight(1f)) { Text("Bỏ qua kỳ này") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecurringEditScreen(vm: MainViewModel, nav: NavController, id: String) {
    val rules by vm.recurring.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val existing = rules.firstOrNull { it.id == id }

    var initialized by rememberSaveable { mutableStateOf(id.isEmpty()) }
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(TxType.EXPENSE) }
    var amount by rememberSaveable { mutableStateOf("") }
    var walletId by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var frequency by rememberSaveable { mutableStateOf(Frequency.MONTHLY) }
    var startDate by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var autoCreate by rememberSaveable { mutableStateOf(true) }
    var active by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (!initialized && existing != null) {
            name = existing.name
            type = existing.type
            amount = existing.amount.toString()
            walletId = existing.walletId
            categoryId = existing.categoryId
            frequency = existing.frequency
            startDate = existing.startDate
            autoCreate = existing.autoCreate
            active = existing.active
            initialized = true
        }
    }
    LaunchedEffect(wallets) { if (walletId.isEmpty()) walletId = wallets.firstOrNull()?.id.orEmpty() }

    Scaffold(
        topBar = {
            BackTopBar(if (id.isEmpty()) "Thêm định kỳ" else "Sửa định kỳ", nav) {
                if (id.isNotEmpty()) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Xóa") }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val types = listOf(TxType.EXPENSE to "Khoản chi", TxType.INCOME to "Khoản thu")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                types.forEachIndexed { i, (k, label) ->
                    SegmentedButton(
                        selected = type == k,
                        onClick = {
                            if (type != k) {
                                type = k
                                categoryId = ""
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, types.size),
                    ) { Text(label) }
                }
            }
            OutlinedTextField(name, { name = it }, label = { Text("Tên (vd: Lương, Tiền nhà, Internet)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            AmountField(amount, { amount = it }, "Số tiền mỗi kỳ")
            DropdownField("Ví", wallets, wallets.firstOrNull { it.id == walletId }, { "${it.icon} ${it.name}" }, { walletId = it.id })
            Text("Danh mục", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.filter { it.type == type }.forEach { c ->
                    FilterChip(selected = categoryId == c.id, onClick = { categoryId = c.id }, label = { Text("${c.icon} ${c.name}") })
                }
            }
            DropdownField("Lặp lại", Frequency.all, frequency, { Frequency.label(it) }, { frequency = it })
            DateField(if (id.isEmpty()) "Ngày bắt đầu (kỳ đầu tiên)" else "Ngày bắt đầu", startDate, { startDate = it })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tự động ghi giao dịch")
                    Text("Tắt để chỉ nhận thông báo nhắc", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(autoCreate, { autoCreate = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Đang hoạt động", modifier = Modifier.weight(1f))
                Switch(active, { active = it })
            }
            if (existing != null && existing.done > 0) {
                Text(
                    "Đã ghi ${existing.done} kỳ. Nếu đổi ngày bắt đầu/chu kỳ, kỳ tiếp theo sẽ được tính lại từ hôm nay.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    error = when {
                        amount.toAmount() <= 0 -> "Vui lòng nhập số tiền"
                        walletId.isEmpty() -> "Vui lòng chọn ví"
                        categoryId.isEmpty() -> "Vui lòng chọn danh mục"
                        else -> null
                    }
                    if (error == null) {
                        val base = existing ?: RecurringRule()
                        val scheduleChanged = existing != null && (existing.startDate != startDate || existing.frequency != frequency)
                        vm.saveRecurring(
                            base.copy(
                                name = name.trim(), type = type, amount = amount.toAmount(), walletId = walletId,
                                categoryId = categoryId, frequency = frequency, startDate = startDate,
                                autoCreate = autoCreate, active = active,
                                done = base.done,
                            ).let { r ->
                                // Đổi lịch: bắt đầu tính từ kỳ đầu tiên kể từ hôm nay để không ghi trùng kỳ cũ
                                if (scheduleChanged) r.copy(done = Calc.dueDates(r.copy(done = 0), LocalDate.now(zone).minusDays(1), zone).size) else r
                            }
                        )
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Lưu") }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Xóa giao dịch định kỳ?",
            text = "Các giao dịch đã được ghi trước đó vẫn được giữ lại.",
            onConfirm = {
                vm.deleteRecurring(id)
                nav.popBackStack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
