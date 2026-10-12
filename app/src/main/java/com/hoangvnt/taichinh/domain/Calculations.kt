package com.hoangvnt.taichinh.domain

import com.hoangvnt.taichinh.data.model.Budget
import com.hoangvnt.taichinh.data.model.Debt
import com.hoangvnt.taichinh.data.model.Frequency
import com.hoangvnt.taichinh.data.model.Goal
import com.hoangvnt.taichinh.data.model.RecurringRule
import com.hoangvnt.taichinh.data.model.Transaction
import com.hoangvnt.taichinh.data.model.TxType
import com.hoangvnt.taichinh.data.model.Wallet
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max

/** Các phép tính nghiệp vụ thuần Kotlin (không phụ thuộc Android) để dễ kiểm thử. */
object Calc {

    fun walletBalances(wallets: List<Wallet>, txs: List<Transaction>): Map<String, Long> {
        val map = wallets.associate { it.id to it.initialBalance }.toMutableMap()
        for (t in txs) {
            when (t.type) {
                TxType.INCOME -> map[t.walletId] = (map[t.walletId] ?: 0) + t.amount
                TxType.EXPENSE -> map[t.walletId] = (map[t.walletId] ?: 0) - t.amount
                TxType.TRANSFER -> {
                    map[t.walletId] = (map[t.walletId] ?: 0) - t.amount
                    if (t.toWalletId.isNotEmpty()) map[t.toWalletId] = (map[t.toWalletId] ?: 0) + t.amount
                }
            }
        }
        return map
    }

    fun inRange(txs: List<Transaction>, from: Long, to: Long) = txs.filter { it.date in from until to }

    data class Summary(val income: Long, val expense: Long) {
        val net get() = income - expense
    }

    fun summary(txs: List<Transaction>): Summary = Summary(
        income = txs.filter { it.type == TxType.INCOME }.sumOf { it.amount },
        expense = txs.filter { it.type == TxType.EXPENSE }.sumOf { it.amount },
    )

    /** Tổng tiền theo danh mục (giao dịch không có danh mục gom vào key ""). */
    fun byCategory(txs: List<Transaction>, type: String): Map<String, Long> =
        txs.filter { it.type == type }
            .groupBy { it.categoryId }
            .mapValues { (_, v) -> v.sumOf { it.amount } }

    data class BudgetStatus(val budget: Budget, val spent: Long) {
        val ratio: Float get() = if (budget.limit <= 0) 0f else spent.toFloat() / budget.limit
        val remaining: Long get() = budget.limit - spent
    }

    fun budgetStatuses(budgets: List<Budget>, monthTxs: List<Transaction>): List<BudgetStatus> {
        val spent = byCategory(monthTxs, TxType.EXPENSE)
        return budgets.map { BudgetStatus(it, spent[it.categoryId] ?: 0) }
    }

    /** Số tiền cần để dành mỗi tháng để đạt mục tiêu đúng hạn; null nếu không có hạn. */
    fun goalMonthlyNeeded(goal: Goal, today: LocalDate, zone: ZoneId): Long? {
        if (goal.deadline <= 0) return null
        val remaining = goal.target - goal.saved
        if (remaining <= 0) return 0
        val deadline = java.time.Instant.ofEpochMilli(goal.deadline).atZone(zone).toLocalDate()
        val months = max(1L, ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(deadline)) + 1)
        return (remaining + months - 1) / months
    }

    fun debtPaid(debt: Debt): Long = debt.payments.sumOf { it.amount }

    fun debtRemaining(debt: Debt): Long = max(0, debt.amount - debtPaid(debt))

    /** Lãi đơn ước tính trên số gốc còn lại, tính từ ngày bắt đầu đến `untilMillis`. */
    fun debtInterest(debt: Debt, untilMillis: Long): Long {
        if (debt.interestRate <= 0.0 || debt.startDate <= 0) return 0
        val days = max(0L, (untilMillis - debt.startDate) / 86_400_000L)
        return (debtRemaining(debt) * debt.interestRate / 100.0 * days / 365.0).toLong()
    }

    /** Ngày của kỳ thứ `index` (0-based) tính từ ngày bắt đầu, không bị trôi ngày khi qua tháng ngắn. */
    fun periodDate(start: LocalDate, frequency: String, index: Int): LocalDate = when (frequency) {
        Frequency.DAILY -> start.plusDays(index.toLong())
        Frequency.WEEKLY -> start.plusWeeks(index.toLong())
        Frequency.YEARLY -> start.plusYears(index.toLong())
        else -> start.plusMonths(index.toLong())
    }

    fun nextDate(rule: RecurringRule, zone: ZoneId): LocalDate {
        val start = java.time.Instant.ofEpochMilli(rule.startDate).atZone(zone).toLocalDate()
        return periodDate(start, rule.frequency, rule.done)
    }

    /** Danh sách ngày đến hạn (<= today) chưa được ghi, tối đa `limit` kỳ. */
    fun dueDates(rule: RecurringRule, today: LocalDate, zone: ZoneId, limit: Int = 400): List<LocalDate> {
        val start = java.time.Instant.ofEpochMilli(rule.startDate).atZone(zone).toLocalDate()
        val result = mutableListOf<LocalDate>()
        var i = rule.done
        while (result.size < limit) {
            val d = periodDate(start, rule.frequency, i)
            if (d.isAfter(today)) break
            result += d
            i++
        }
        return result
    }
}
