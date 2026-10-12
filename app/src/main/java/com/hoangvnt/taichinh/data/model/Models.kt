package com.hoangvnt.taichinh.data.model

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

/*
 * Các model lưu trên Firestore tại users/{uid}/{collection}/{id}.
 * Dùng `var` + giá trị mặc định để Firestore ánh xạ được (cần constructor rỗng và setter).
 * Số tiền lưu dạng Long (VNĐ), thời gian lưu dạng epoch millis.
 */

object TxType {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
    const val TRANSFER = "TRANSFER"
}

object WalletType {
    const val CASH = "CASH"
    const val BANK = "BANK"
    const val CREDIT = "CREDIT"
    const val EWALLET = "EWALLET"
    const val SAVING = "SAVING"
    const val OTHER = "OTHER"

    val all = listOf(CASH, BANK, CREDIT, EWALLET, SAVING, OTHER)

    fun label(type: String) = when (type) {
        CASH -> "Tiền mặt"
        BANK -> "Tài khoản ngân hàng"
        CREDIT -> "Thẻ tín dụng"
        EWALLET -> "Ví điện tử"
        SAVING -> "Tiết kiệm"
        else -> "Khác"
    }

    fun icon(type: String) = when (type) {
        CASH -> "💵"
        BANK -> "🏦"
        CREDIT -> "💳"
        EWALLET -> "📱"
        SAVING -> "🐷"
        else -> "👛"
    }
}

object Frequency {
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val YEARLY = "YEARLY"

    val all = listOf(DAILY, WEEKLY, MONTHLY, YEARLY)

    fun label(f: String) = when (f) {
        DAILY -> "Hằng ngày"
        WEEKLY -> "Hằng tuần"
        MONTHLY -> "Hằng tháng"
        else -> "Hằng năm"
    }
}

object DebtKind {
    /** Tôi đi vay (tôi nợ người khác) */
    const val BORROW = "BORROW"
    /** Tôi cho vay (người khác nợ tôi) */
    const val LEND = "LEND"
}

object Collections {
    const val WALLETS = "wallets"
    const val CATEGORIES = "categories"
    const val TRANSACTIONS = "transactions"
    const val BUDGETS = "budgets"
    const val GOALS = "goals"
    const val DEBTS = "debts"
    const val RECURRING = "recurring"
}

@Serializable
data class Wallet(
    @DocumentId var id: String = "",
    var name: String = "",
    var type: String = WalletType.CASH,
    var initialBalance: Long = 0,
    var icon: String = "💵",
    var includeInTotal: Boolean = true,
    var createdAt: Long = 0,
)

@Serializable
data class Category(
    @DocumentId var id: String = "",
    var name: String = "",
    var type: String = TxType.EXPENSE,
    var icon: String = "📦",
    var color: Long = 0xFF90A4AE,
    var order: Int = 0,
)

@Serializable
data class Transaction(
    @DocumentId var id: String = "",
    var type: String = TxType.EXPENSE,
    var amount: Long = 0,
    var walletId: String = "",
    /** Ví nhận (chỉ dùng cho chuyển tiền) */
    var toWalletId: String = "",
    var categoryId: String = "",
    var note: String = "",
    var date: Long = 0,
    var recurringId: String = "",
    var debtId: String = "",
    var createdAt: Long = 0,
)

/** Hạn mức chi tiêu hằng tháng cho một danh mục (áp dụng cho mọi tháng). */
@Serializable
data class Budget(
    @DocumentId var id: String = "",
    var categoryId: String = "",
    var limit: Long = 0,
)

@Serializable
data class Goal(
    @DocumentId var id: String = "",
    var name: String = "",
    var icon: String = "🎯",
    var target: Long = 0,
    var saved: Long = 0,
    /** 0 = không có hạn */
    var deadline: Long = 0,
    var createdAt: Long = 0,
)

@Serializable
data class DebtPayment(
    var amount: Long = 0,
    var date: Long = 0,
    var note: String = "",
)

@Serializable
data class Debt(
    @DocumentId var id: String = "",
    var kind: String = DebtKind.BORROW,
    var person: String = "",
    var amount: Long = 0,
    /** Lãi suất %/năm (lãi đơn), 0 = không lãi */
    var interestRate: Double = 0.0,
    var startDate: Long = 0,
    /** 0 = không có hạn */
    var dueDate: Long = 0,
    var note: String = "",
    var payments: List<DebtPayment> = emptyList(),
    var createdAt: Long = 0,
)

@Serializable
data class RecurringRule(
    @DocumentId var id: String = "",
    var name: String = "",
    var type: String = TxType.EXPENSE,
    var amount: Long = 0,
    var walletId: String = "",
    var categoryId: String = "",
    var frequency: String = Frequency.MONTHLY,
    var startDate: Long = 0,
    /** Số kỳ đã ghi nhận; kỳ kế tiếp = startDate + done * chu kỳ */
    var done: Int = 0,
    /** true: tự động ghi giao dịch khi đến hạn; false: chỉ nhắc nhở */
    var autoCreate: Boolean = true,
    var active: Boolean = true,
    var lastNotified: Long = 0,
)
