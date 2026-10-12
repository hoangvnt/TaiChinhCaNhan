package com.hoangvnt.taichinh.data

import com.hoangvnt.taichinh.data.model.Budget
import com.hoangvnt.taichinh.data.model.Category
import com.hoangvnt.taichinh.data.model.Collections
import com.hoangvnt.taichinh.data.model.Debt
import com.hoangvnt.taichinh.data.model.Goal
import com.hoangvnt.taichinh.data.model.RecurringRule
import com.hoangvnt.taichinh.data.model.Transaction
import com.hoangvnt.taichinh.data.model.TxType
import com.hoangvnt.taichinh.data.model.Wallet
import com.hoangvnt.taichinh.util.formatDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = 0,
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val recurring: List<RecurringRule> = emptyList(),
)

object BackupManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toJson(data: BackupData): String = json.encodeToString(BackupData.serializer(), data)

    fun fromJson(text: String): BackupData = json.decodeFromString(BackupData.serializer(), text)

    /** Danh sách thao tác ghi để khôi phục — giữ nguyên id nên nhập lại nhiều lần không bị trùng. */
    fun toWrites(data: BackupData): List<Triple<String, String, Any>> =
        data.wallets.map { Triple(Collections.WALLETS, it.id, it as Any) } +
            data.categories.map { Triple(Collections.CATEGORIES, it.id, it as Any) } +
            data.transactions.map { Triple(Collections.TRANSACTIONS, it.id, it as Any) } +
            data.budgets.map { Triple(Collections.BUDGETS, it.id, it as Any) } +
            data.goals.map { Triple(Collections.GOALS, it.id, it as Any) } +
            data.debts.map { Triple(Collections.DEBTS, it.id, it as Any) } +
            data.recurring.map { Triple(Collections.RECURRING, it.id, it as Any) }

    fun toCsv(txs: List<Transaction>, wallets: List<Wallet>, categories: List<Category>): String {
        val w = wallets.associateBy { it.id }
        val c = categories.associateBy { it.id }
        fun esc(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' }) "\"${s.replace("\"", "\"\"")}\"" else s
        val sb = StringBuilder("﻿") // BOM để Excel đọc đúng tiếng Việt
        sb.append("Ngày,Loại,Số tiền,Ví,Ví nhận,Danh mục,Ghi chú\n")
        txs.sortedBy { it.date }.forEach { t ->
            val type = when (t.type) {
                TxType.INCOME -> "Thu"
                TxType.EXPENSE -> "Chi"
                else -> "Chuyển tiền"
            }
            sb.append(
                listOf(
                    formatDate(t.date), type, t.amount.toString(),
                    w[t.walletId]?.name.orEmpty(), w[t.toWalletId]?.name.orEmpty(),
                    c[t.categoryId]?.name.orEmpty(), t.note,
                ).joinToString(",") { esc(it) }
            ).append('\n')
        }
        return sb.toString()
    }
}
