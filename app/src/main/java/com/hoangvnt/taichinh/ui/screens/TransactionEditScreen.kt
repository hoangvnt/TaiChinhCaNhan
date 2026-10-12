@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.hoangvnt.taichinh.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hoangvnt.taichinh.data.model.Transaction
import com.hoangvnt.taichinh.data.model.TxType
import com.hoangvnt.taichinh.ui.MainViewModel
import com.hoangvnt.taichinh.ui.components.AmountField
import com.hoangvnt.taichinh.ui.components.BackTopBar
import com.hoangvnt.taichinh.ui.components.ConfirmDialog
import com.hoangvnt.taichinh.ui.components.DateField
import com.hoangvnt.taichinh.ui.components.DropdownField
import com.hoangvnt.taichinh.util.formatMoney
import com.hoangvnt.taichinh.util.toAmount

@Composable
fun TransactionEditScreen(vm: MainViewModel, nav: NavController, id: String, initialType: String) {
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val balances by vm.balances.collectAsStateWithLifecycle()
    val existing = txs.firstOrNull { it.id == id }

    var initialized by rememberSaveable { mutableStateOf(id.isEmpty()) }
    var type by rememberSaveable { mutableStateOf(initialType.ifEmpty { TxType.EXPENSE }) }
    var amount by rememberSaveable { mutableStateOf("") }
    var walletId by rememberSaveable { mutableStateOf("") }
    var toWalletId by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (!initialized && existing != null) {
            type = existing.type
            amount = existing.amount.toString()
            walletId = existing.walletId
            toWalletId = existing.toWalletId
            categoryId = existing.categoryId
            note = existing.note
            date = existing.date
            initialized = true
        }
    }
    LaunchedEffect(wallets) {
        if (walletId.isEmpty()) walletId = wallets.firstOrNull()?.id.orEmpty()
    }

    val isDebtTx = existing?.debtId?.isNotEmpty() == true
    val catsForType = categories.filter { it.type == type }

    Scaffold(
        topBar = {
            BackTopBar(if (id.isEmpty()) "Thêm giao dịch" else "Sửa giao dịch", nav) {
                if (id.isNotEmpty()) {
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Xóa") }
                }
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
            if (!isDebtTx) {
                val types = listOf(TxType.EXPENSE to "Chi tiêu", TxType.INCOME to "Thu nhập", TxType.TRANSFER to "Chuyển tiền")
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
            } else {
                Text("Giao dịch gắn với khoản nợ/vay", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            AmountField(amount, { amount = it }, "Số tiền")

            DropdownField(
                label = if (type == TxType.TRANSFER) "Từ ví" else "Ví",
                options = wallets,
                selected = wallets.firstOrNull { it.id == walletId },
                optionLabel = { "${it.icon} ${it.name} (${formatMoney(balances[it.id] ?: 0)})" },
                onSelect = { walletId = it.id },
            )

            if (type == TxType.TRANSFER) {
                DropdownField(
                    label = "Đến ví",
                    options = wallets.filter { it.id != walletId },
                    selected = wallets.firstOrNull { it.id == toWalletId },
                    optionLabel = { "${it.icon} ${it.name}" },
                    onSelect = { toWalletId = it.id },
                )
            } else if (!isDebtTx) {
                Text("Danh mục", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    catsForType.forEach { c ->
                        FilterChip(
                            selected = categoryId == c.id,
                            onClick = { categoryId = c.id },
                            label = { Text("${c.icon} ${c.name}") },
                        )
                    }
                }
            }

            DateField("Ngày", date, { date = it })

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú") },
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    val value = amount.toAmount()
                    error = when {
                        value <= 0 -> "Vui lòng nhập số tiền"
                        walletId.isEmpty() -> "Vui lòng chọn ví (tạo ví trong mục Khác → Ví)"
                        type == TxType.TRANSFER && (toWalletId.isEmpty() || toWalletId == walletId) -> "Vui lòng chọn ví nhận khác ví nguồn"
                        type != TxType.TRANSFER && !isDebtTx && categoryId.isEmpty() -> "Vui lòng chọn danh mục"
                        else -> null
                    }
                    if (error == null) {
                        val base = existing ?: Transaction()
                        vm.saveTransaction(
                            base.copy(
                                type = type,
                                amount = value,
                                walletId = walletId,
                                toWalletId = if (type == TxType.TRANSFER) toWalletId else "",
                                categoryId = if (type == TxType.TRANSFER) "" else categoryId,
                                note = note.trim(),
                                date = date,
                            )
                        )
                        nav.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text("Lưu") }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Xóa giao dịch?",
            text = "Giao dịch sẽ bị xóa vĩnh viễn.",
            onConfirm = {
                vm.deleteTransaction(id)
                nav.popBackStack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
