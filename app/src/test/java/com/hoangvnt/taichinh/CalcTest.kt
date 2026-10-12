package com.hoangvnt.taichinh

import com.hoangvnt.taichinh.data.model.Budget
import com.hoangvnt.taichinh.data.model.Debt
import com.hoangvnt.taichinh.data.model.DebtPayment
import com.hoangvnt.taichinh.data.model.Frequency
import com.hoangvnt.taichinh.data.model.Goal
import com.hoangvnt.taichinh.data.model.RecurringRule
import com.hoangvnt.taichinh.data.model.Transaction
import com.hoangvnt.taichinh.data.model.TxType
import com.hoangvnt.taichinh.data.model.Wallet
import com.hoangvnt.taichinh.domain.Calc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class CalcTest {
    private val zone = ZoneOffset.UTC
    private fun ms(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun walletBalances_includesIncomeExpenseAndTransfers() {
        val wallets = listOf(Wallet(id = "a", initialBalance = 1_000_000), Wallet(id = "b", initialBalance = 0))
        val txs = listOf(
            Transaction(type = TxType.INCOME, amount = 500_000, walletId = "a"),
            Transaction(type = TxType.EXPENSE, amount = 200_000, walletId = "a"),
            Transaction(type = TxType.TRANSFER, amount = 300_000, walletId = "a", toWalletId = "b"),
        )
        val b = Calc.walletBalances(wallets, txs)
        assertEquals(1_000_000L, b["a"])
        assertEquals(300_000L, b["b"])
    }

    @Test
    fun summary_ignoresTransfers() {
        val s = Calc.summary(
            listOf(
                Transaction(type = TxType.INCOME, amount = 10),
                Transaction(type = TxType.EXPENSE, amount = 4),
                Transaction(type = TxType.TRANSFER, amount = 100),
            )
        )
        assertEquals(10L, s.income)
        assertEquals(4L, s.expense)
        assertEquals(6L, s.net)
    }

    @Test
    fun budgetStatus_computesSpentPerCategory() {
        val st = Calc.budgetStatuses(
            listOf(Budget(id = "b1", categoryId = "food", limit = 1000)),
            listOf(
                Transaction(type = TxType.EXPENSE, amount = 600, categoryId = "food"),
                Transaction(type = TxType.EXPENSE, amount = 300, categoryId = "food"),
                Transaction(type = TxType.EXPENSE, amount = 999, categoryId = "other"),
            ),
        ).single()
        assertEquals(900L, st.spent)
        assertEquals(0.9f, st.ratio, 0.0001f)
        assertEquals(100L, st.remaining)
    }

    @Test
    fun goalMonthlyNeeded_splitsRemainingOverMonthsInclusive() {
        val today = LocalDate.of(2026, 1, 15)
        val g = Goal(target = 12_000_000, saved = 0, deadline = ms(LocalDate.of(2026, 12, 31)))
        assertEquals(1_000_000L, Calc.goalMonthlyNeeded(g, today, zone))
        assertNull(Calc.goalMonthlyNeeded(g.copy(deadline = 0), today, zone))
        assertEquals(0L, Calc.goalMonthlyNeeded(g.copy(saved = 12_000_000), today, zone))
    }

    @Test
    fun debt_remainingAndInterest() {
        val start = LocalDate.of(2026, 1, 1)
        val d = Debt(
            amount = 10_000_000, interestRate = 12.0, startDate = ms(start),
            payments = listOf(DebtPayment(amount = 4_000_000)),
        )
        assertEquals(4_000_000L, Calc.debtPaid(d))
        assertEquals(6_000_000L, Calc.debtRemaining(d))
        // 6tr * 12% * 365/365 = 720k
        assertEquals(720_000L, Calc.debtInterest(d, ms(start.plusDays(365))))
    }

    @Test
    fun periodDate_monthlyDoesNotDrift() {
        val start = LocalDate.of(2026, 1, 31)
        assertEquals(LocalDate.of(2026, 2, 28), Calc.periodDate(start, Frequency.MONTHLY, 1))
        assertEquals(LocalDate.of(2026, 3, 31), Calc.periodDate(start, Frequency.MONTHLY, 2))
        assertEquals(LocalDate.of(2026, 2, 14), Calc.periodDate(start, Frequency.WEEKLY, 2))
    }

    @Test
    fun dueDates_returnsUnrecordedPastPeriods() {
        val rule = RecurringRule(frequency = Frequency.MONTHLY, startDate = ms(LocalDate.of(2026, 1, 5)), done = 1)
        val due = Calc.dueDates(rule, LocalDate.of(2026, 4, 5), zone)
        assertEquals(listOf(LocalDate.of(2026, 2, 5), LocalDate.of(2026, 3, 5), LocalDate.of(2026, 4, 5)), due)
        assertEquals(LocalDate.of(2026, 2, 5), Calc.nextDate(rule, zone))
    }
}
