package com.hoangvnt.taichinh.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoangvnt.taichinh.data.model.Category
import com.hoangvnt.taichinh.data.model.Transaction
import com.hoangvnt.taichinh.data.model.TxType
import com.hoangvnt.taichinh.data.model.Wallet
import com.hoangvnt.taichinh.ui.components.EmojiBadge
import com.hoangvnt.taichinh.ui.theme.expenseColor
import com.hoangvnt.taichinh.ui.theme.incomeColor
import com.hoangvnt.taichinh.util.formatMoney

fun txTitle(t: Transaction, cats: Map<String, Category>, wallets: Map<String, Wallet>): String = when {
    t.type == TxType.TRANSFER -> "Chuyển: ${wallets[t.walletId]?.name ?: "?"} → ${wallets[t.toWalletId]?.name ?: "?"}"
    t.debtId.isNotEmpty() -> "Nợ / vay"
    else -> cats[t.categoryId]?.name ?: "Chưa phân loại"
}

fun txIcon(t: Transaction, cats: Map<String, Category>): String = when {
    t.type == TxType.TRANSFER -> "🔁"
    t.debtId.isNotEmpty() -> "🤝"
    else -> cats[t.categoryId]?.icon ?: "❔"
}

@Composable
fun TransactionRow(
    t: Transaction,
    cats: Map<String, Category>,
    wallets: Map<String, Wallet>,
    onClick: () -> Unit,
) {
    val color = when (t.type) {
        TxType.INCOME -> incomeColor()
        TxType.EXPENSE -> expenseColor()
        else -> MaterialTheme.colorScheme.onSurface
    }
    val sign = when (t.type) {
        TxType.INCOME -> "+"
        TxType.EXPENSE -> "−"
        else -> ""
    }
    val catColor = cats[t.categoryId]?.color?.let { Color(it).copy(alpha = 0.25f) } ?: MaterialTheme.colorScheme.secondaryContainer
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiBadge(txIcon(t, cats), catColor)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(txTitle(t, cats, wallets), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOfNotNull(
                t.note.takeIf { it.isNotBlank() },
                if (t.type != TxType.TRANSFER) wallets[t.walletId]?.name else null,
            ).joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(sign + formatMoney(t.amount), color = color, fontWeight = FontWeight.SemiBold)
    }
}
