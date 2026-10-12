package com.hoangvnt.taichinh.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val REPORTS = "reports"
    const val PLAN = "plan"
    const val MORE = "more"

    const val TX_EDIT = "tx_edit?id={id}&type={type}"
    fun txEdit(id: String? = null, type: String? = null) =
        "tx_edit?id=${id.orEmpty()}&type=${type.orEmpty()}"

    const val WALLETS = "wallets"
    const val CATEGORIES = "categories"
    const val RECURRING = "recurring"
    const val RECURRING_EDIT = "recurring_edit?id={id}"
    fun recurringEdit(id: String? = null) = "recurring_edit?id=${id.orEmpty()}"
    const val DEBT_EDIT = "debt_edit?id={id}"
    fun debtEdit(id: String? = null) = "debt_edit?id=${id.orEmpty()}"
    const val DEBT_DETAIL = "debt/{id}"
    fun debtDetail(id: String) = "debt/$id"
    const val BACKUP = "backup"
}

data class TopTab(val route: String, val label: String, val icon: ImageVector)

val topTabs = listOf(
    TopTab(Routes.HOME, "Tổng quan", Icons.Default.Home),
    TopTab(Routes.TRANSACTIONS, "Giao dịch", Icons.AutoMirrored.Filled.ReceiptLong),
    TopTab(Routes.REPORTS, "Báo cáo", Icons.Default.PieChart),
    TopTab(Routes.PLAN, "Kế hoạch", Icons.Default.Flag),
    TopTab(Routes.MORE, "Khác", Icons.Default.MoreHoriz),
)
