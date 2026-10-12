@file:OptIn(ExperimentalMaterial3Api::class)

package com.hoangvnt.taichinh.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hoangvnt.taichinh.data.model.TxType
import com.hoangvnt.taichinh.domain.Calc
import com.hoangvnt.taichinh.ui.BottomBar
import com.hoangvnt.taichinh.ui.MainViewModel
import com.hoangvnt.taichinh.ui.Routes
import com.hoangvnt.taichinh.ui.components.DropdownField
import com.hoangvnt.taichinh.ui.components.EmptyState
import com.hoangvnt.taichinh.ui.components.MonthSelector
import com.hoangvnt.taichinh.ui.components.MoneyText
import com.hoangvnt.taichinh.ui.theme.expenseColor
import com.hoangvnt.taichinh.ui.theme.incomeColor
import com.hoangvnt.taichinh.util.endMillis
import com.hoangvnt.taichinh.util.formatDayHeader
import com.hoangvnt.taichinh.util.formatSignedMoney
import com.hoangvnt.taichinh.util.startMillis
import com.hoangvnt.taichinh.util.toLocalDate
import com.hoangvnt.taichinh.util.zone
import java.time.YearMonth

@Composable
fun TransactionsScreen(vm: MainViewModel, nav: NavController) {
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()

    var month by rememberSaveable { mutableStateOf(YearMonth.now(zone)) }
    var walletFilter by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }

    val catMap = remember(categories) { categories.associateBy { it.id } }
    val walletMap = remember(wallets) { wallets.associateBy { it.id } }

    val filtered = remember(txs, month, walletFilter, typeFilter, query, catMap) {
        Calc.inRange(txs, month.startMillis(), month.endMillis()).filter { t ->
            (walletFilter.isEmpty() || t.walletId == walletFilter || t.toWalletId == walletFilter) &&
                (typeFilter.isEmpty() || t.type == typeFilter) &&
                (query.isBlank() || t.note.contains(query, true) || (catMap[t.categoryId]?.name?.contains(query, true) == true))
        }
    }
    val summary = remember(filtered) { Calc.summary(filtered) }
    val grouped = remember(filtered) { filtered.groupBy { it.date.toLocalDate() } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Giao dịch") }) },
        bottomBar = { BottomBar(nav) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(Routes.txEdit()) }) { Icon(Icons.Default.Add, "Thêm giao dịch") }
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    MonthSelector(month, { month = it })
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Tìm theo ghi chú, danh mục") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf("" to "Tất cả", TxType.EXPENSE to "Chi", TxType.INCOME to "Thu", TxType.TRANSFER to "Chuyển").forEach { (k, label) ->
                            FilterChip(selected = typeFilter == k, onClick = { typeFilter = k }, label = { Text(label) })
                        }
                    }
                    DropdownField(
                        label = "Ví",
                        options = listOf("") + wallets.map { it.id },
                        selected = walletFilter,
                        optionLabel = { id -> if (id.isEmpty()) "Tất cả ví" else walletMap[id]?.name ?: "?" },
                        onSelect = { walletFilter = it },
                    )
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Thu", style = MaterialTheme.typography.labelMedium)
                            MoneyText(summary.income, color = incomeColor())
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Chi", style = MaterialTheme.typography.labelMedium)
                            MoneyText(summary.expense, color = expenseColor())
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Chênh lệch", style = MaterialTheme.typography.labelMedium)
                            Text(formatSignedMoney(summary.net), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                HorizontalDivider()
            }
            if (filtered.isEmpty()) {
                item { EmptyState("Không có giao dịch trong tháng này") }
            }
            grouped.forEach { (day, list) ->
                item(key = "h$day") {
                    val dayNet = list.sumOf {
                        when (it.type) {
                            TxType.INCOME -> it.amount
                            TxType.EXPENSE -> -it.amount
                            else -> 0
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                    ) {
                        Text(formatDayHeader(day), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        Text(formatSignedMoney(dayNet), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(list, key = { it.id }) { t ->
                    TransactionRow(t, catMap, walletMap) { nav.navigate(Routes.txEdit(t.id)) }
                }
            }
        }
    }
}
