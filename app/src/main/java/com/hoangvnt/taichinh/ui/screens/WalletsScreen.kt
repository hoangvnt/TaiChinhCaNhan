@file:OptIn(ExperimentalMaterial3Api::class)

package com.hoangvnt.taichinh.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hoangvnt.taichinh.data.model.TxType
import com.hoangvnt.taichinh.data.model.Wallet
import com.hoangvnt.taichinh.data.model.WalletType
import com.hoangvnt.taichinh.ui.MainViewModel
import com.hoangvnt.taichinh.ui.Routes
import com.hoangvnt.taichinh.ui.components.AmountField
import com.hoangvnt.taichinh.ui.components.BackTopBar
import com.hoangvnt.taichinh.ui.components.DropdownField
import com.hoangvnt.taichinh.ui.components.EmojiBadge
import com.hoangvnt.taichinh.ui.components.LabelValue
import com.hoangvnt.taichinh.ui.components.MoneyText
import com.hoangvnt.taichinh.ui.components.SectionCard
import com.hoangvnt.taichinh.ui.theme.expenseColor
import com.hoangvnt.taichinh.util.formatMoney
import com.hoangvnt.taichinh.util.toAmount

@Composable
fun WalletsScreen(vm: MainViewModel, nav: NavController) {
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val balances by vm.balances.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Wallet?>(null) }

    val total = wallets.filter { it.includeInTotal }.sumOf { balances[it.id] ?: 0L }
    val debt = wallets.sumOf { (balances[it.id] ?: 0L).coerceAtMost(0L) }

    Scaffold(
        topBar = {
            BackTopBar("Ví & tài khoản", nav) {
                IconButton(onClick = { nav.navigate(Routes.txEdit(type = TxType.TRANSFER)) }) {
                    Icon(Icons.Default.SwapHoriz, "Chuyển tiền giữa các ví")
                }
            }
        },
        floatingActionButton = { FloatingActionButton(onClick = { editing = Wallet() }) { Icon(Icons.Default.Add, "Thêm ví") } },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Text("Tổng tài sản trong ví", style = MaterialTheme.typography.labelLarge)
                    MoneyText(total, style = MaterialTheme.typography.headlineSmall)
                    if (debt < 0) LabelValue("Dư nợ (thẻ tín dụng, ví âm)", formatMoney(debt), expenseColor())
                }
            }
            items(wallets, key = { it.id }) { w ->
                SectionCard(onClick = { editing = w }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(w.icon)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                WalletType.label(w.type) + if (!w.includeInTotal) " · không tính vào tổng" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val b = balances[w.id] ?: 0L
                        MoneyText(b, color = if (b < 0) expenseColor() else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }

    editing?.let { w -> WalletDialog(vm, w) { editing = null } }
}

@Composable
private fun WalletDialog(vm: MainViewModel, wallet: Wallet, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(wallet.name) }
    var type by remember { mutableStateOf(wallet.type) }
    var icon by remember { mutableStateOf(wallet.icon) }
    var initial by remember { mutableStateOf(if (wallet.initialBalance != 0L) kotlin.math.abs(wallet.initialBalance).toString() else "") }
    var negative by remember { mutableStateOf(wallet.initialBalance < 0) }
    var include by remember { mutableStateOf(wallet.includeInTotal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (wallet.id.isEmpty()) "Thêm ví" else "Sửa ví") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(icon, { icon = it.take(4) }, label = { Text("Biểu tượng") }, singleLine = true, modifier = Modifier.width(96.dp))
                    OutlinedTextField(name, { name = it }, label = { Text("Tên ví") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                DropdownField("Loại", WalletType.all, type, { "${WalletType.icon(it)} ${WalletType.label(it)}" }, {
                    if (icon == WalletType.icon(type) || icon.isBlank()) icon = WalletType.icon(it)
                    type = it
                })
                AmountField(initial, { initial = it }, "Số dư ban đầu")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Số dư âm (đang nợ thẻ)", modifier = Modifier.weight(1f))
                    Switch(negative, { negative = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tính vào tổng số dư", modifier = Modifier.weight(1f))
                    Switch(include, { include = it })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                val v = initial.toAmount()
                vm.saveWallet(
                    wallet.copy(
                        name = name.trim(), type = type, icon = icon.ifBlank { WalletType.icon(type) },
                        initialBalance = if (negative) -v else v, includeInTotal = include,
                    )
                )
                onDismiss()
            }) { Text("Lưu") }
        },
        dismissButton = {
            Row {
                if (wallet.id.isNotEmpty()) {
                    TextButton(onClick = { if (vm.deleteWallet(wallet.id)) onDismiss() }) {
                        Text("Xóa", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Hủy") }
            }
        },
    )
}
