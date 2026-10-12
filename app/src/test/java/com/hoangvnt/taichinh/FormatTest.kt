package com.hoangvnt.taichinh

import com.hoangvnt.taichinh.data.BackupData
import com.hoangvnt.taichinh.data.BackupManager
import com.hoangvnt.taichinh.data.model.Category
import com.hoangvnt.taichinh.data.model.Debt
import com.hoangvnt.taichinh.data.model.DebtPayment
import com.hoangvnt.taichinh.data.model.Transaction
import com.hoangvnt.taichinh.data.model.TxType
import com.hoangvnt.taichinh.data.model.Wallet
import com.hoangvnt.taichinh.util.formatCompact
import com.hoangvnt.taichinh.util.formatMoney
import com.hoangvnt.taichinh.util.toAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {
    @Test
    fun formatMoney_usesDotGrouping() {
        assertEquals("1.234.567 ₫", formatMoney(1_234_567))
        assertEquals("0 ₫", formatMoney(0))
        assertEquals("-50.000 ₫", formatMoney(-50_000))
    }

    @Test
    fun formatCompact_units() {
        assertEquals("1,5 tr", formatCompact(1_500_000))
        assertEquals("2 tỷ", formatCompact(2_000_000_000))
        assertEquals("250 k", formatCompact(250_000))
    }

    @Test
    fun toAmount_stripsNonDigits() {
        assertEquals(1_200_000L, "1.200.000".toAmount())
        assertEquals(0L, "".toAmount())
    }

    @Test
    fun backup_roundTrip() {
        val data = BackupData(
            exportedAt = 1,
            wallets = listOf(Wallet(id = "w", name = "Ví", initialBalance = 5)),
            categories = listOf(Category(id = "c", name = "Ăn uống")),
            transactions = listOf(Transaction(id = "t", type = TxType.EXPENSE, amount = 3, walletId = "w", categoryId = "c", note = "phở, \"ngon\"")),
            debts = listOf(Debt(id = "d", person = "An", amount = 10, payments = listOf(DebtPayment(4, 1, "")))),
        )
        val back = BackupManager.fromJson(BackupManager.toJson(data))
        assertEquals(data, back)
        assertEquals(4, BackupManager.toWrites(data).size)
    }

    @Test
    fun csv_escapesQuotesAndCommas() {
        val csv = BackupManager.toCsv(
            listOf(Transaction(type = TxType.EXPENSE, amount = 3, walletId = "w", categoryId = "c", note = "phở, \"ngon\"")),
            listOf(Wallet(id = "w", name = "Ví")),
            listOf(Category(id = "c", name = "Ăn uống")),
        )
        assertTrue(csv.contains("\"phở, \"\"ngon\"\"\""))
        assertTrue(csv.contains("Chi,3,Ví,,Ăn uống"))
    }
}
