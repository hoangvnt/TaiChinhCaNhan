package com.hoangvnt.taichinh.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hoangvnt.taichinh.TaiChinhApp
import com.hoangvnt.taichinh.data.BackupData
import com.hoangvnt.taichinh.data.BackupManager
import com.hoangvnt.taichinh.data.model.Budget
import com.hoangvnt.taichinh.data.model.Category
import com.hoangvnt.taichinh.data.model.Collections
import com.hoangvnt.taichinh.data.model.Debt
import com.hoangvnt.taichinh.data.model.DebtKind
import com.hoangvnt.taichinh.data.model.DebtPayment
import com.hoangvnt.taichinh.data.model.Goal
import com.hoangvnt.taichinh.data.model.RecurringRule
import com.hoangvnt.taichinh.data.model.Transaction
import com.hoangvnt.taichinh.data.model.TxType
import com.hoangvnt.taichinh.data.model.Wallet
import com.hoangvnt.taichinh.domain.Calc
import com.hoangvnt.taichinh.util.startMillis
import com.hoangvnt.taichinh.util.zone
import com.hoangvnt.taichinh.work.ReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as TaiChinhApp).container
    private val repo = container.finance
    private val auth = container.auth

    val user = auth.user
    private val uidFlow = auth.user.map { it?.uid }.distinctUntilChanged()
    private val uid: String? get() = auth.user.value?.uid

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    private fun <T : Any> listFlow(name: String, clazz: Class<T>): StateFlow<List<T>> =
        uidFlow.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.observe(id, name, clazz) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val wallets = listFlow(Collections.WALLETS, Wallet::class.java)
        .let { f -> f.map { l -> l.sortedBy { it.createdAt } }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()) }
    val categories = listFlow(Collections.CATEGORIES, Category::class.java)
        .let { f -> f.map { l -> l.sortedWith(compareBy({ it.order }, { it.name })) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()) }
    val transactions = listFlow(Collections.TRANSACTIONS, Transaction::class.java)
        .let { f -> f.map { l -> l.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt }) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()) }
    val budgets = listFlow(Collections.BUDGETS, Budget::class.java)
    val goals = listFlow(Collections.GOALS, Goal::class.java)
        .let { f -> f.map { l -> l.sortedBy { it.createdAt } }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()) }
    val debts = listFlow(Collections.DEBTS, Debt::class.java)
        .let { f -> f.map { l -> l.sortedByDescending { it.startDate } }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()) }
    val recurring = listFlow(Collections.RECURRING, RecurringRule::class.java)

    val balances: StateFlow<Map<String, Long>> = combine(wallets, transactions) { w, t -> Calc.walletBalances(w, t) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    init {
        viewModelScope.launch {
            uidFlow.collect { id ->
                if (id != null) {
                    repo.ensureSeeded(id)
                    ReminderWorker.runNow(getApplication())
                }
            }
        }
    }

    private fun toast(msg: String) {
        _messages.tryEmit(msg)
    }

    // ---------- Giao dịch ----------
    fun saveTransaction(tx: Transaction) {
        val id = uid ?: return
        if (tx.createdAt == 0L) tx.createdAt = System.currentTimeMillis()
        repo.save(id, Collections.TRANSACTIONS, tx.id, tx.copy())
    }

    fun deleteTransaction(txId: String) {
        val id = uid ?: return
        repo.delete(id, Collections.TRANSACTIONS, txId)
    }

    // ---------- Ví ----------
    fun saveWallet(w: Wallet) {
        val id = uid ?: return
        if (w.createdAt == 0L) w.createdAt = System.currentTimeMillis()
        repo.save(id, Collections.WALLETS, w.id, w.copy())
    }

    /** Trả về false nếu ví còn giao dịch. */
    fun deleteWallet(walletId: String): Boolean {
        val id = uid ?: return false
        val used = transactions.value.any { it.walletId == walletId || it.toWalletId == walletId } ||
            recurring.value.any { it.walletId == walletId }
        if (used) {
            toast("Không thể xóa: ví còn giao dịch hoặc giao dịch định kỳ")
            return false
        }
        repo.delete(id, Collections.WALLETS, walletId)
        return true
    }

    // ---------- Danh mục ----------
    fun saveCategory(c: Category) {
        val id = uid ?: return
        if (c.id.isEmpty()) c.order = (categories.value.filter { it.type == c.type }.maxOfOrNull { it.order } ?: 0) + 1
        repo.save(id, Collections.CATEGORIES, c.id, c.copy())
    }

    fun deleteCategory(catId: String) {
        val id = uid ?: return
        repo.delete(id, Collections.CATEGORIES, catId)
        budgets.value.filter { it.categoryId == catId }.forEach { repo.delete(id, Collections.BUDGETS, it.id) }
    }

    // ---------- Ngân sách ----------
    fun saveBudget(b: Budget) {
        val id = uid ?: return
        // Mỗi danh mục chỉ có một ngân sách
        val existing = budgets.value.firstOrNull { it.categoryId == b.categoryId && it.id != b.id }
        val target = if (existing != null && b.id.isEmpty()) b.copy(id = existing.id) else b
        repo.save(id, Collections.BUDGETS, target.id, target.copy())
    }

    fun deleteBudget(budgetId: String) {
        val id = uid ?: return
        repo.delete(id, Collections.BUDGETS, budgetId)
    }

    // ---------- Mục tiêu ----------
    fun saveGoal(g: Goal) {
        val id = uid ?: return
        if (g.createdAt == 0L) g.createdAt = System.currentTimeMillis()
        repo.save(id, Collections.GOALS, g.id, g.copy())
    }

    fun addToGoal(goal: Goal, delta: Long) {
        val id = uid ?: return
        val newSaved = (goal.saved + delta).coerceAtLeast(0)
        repo.update(id, Collections.GOALS, goal.id, mapOf("saved" to newSaved))
        if (newSaved >= goal.target && goal.saved < goal.target) toast("🎉 Chúc mừng! Bạn đã đạt mục tiêu \"${goal.name}\"")
    }

    fun deleteGoal(goalId: String) {
        val id = uid ?: return
        repo.delete(id, Collections.GOALS, goalId)
    }

    // ---------- Nợ / vay ----------
    /**
     * Lưu khoản nợ. Nếu `walletId` khác rỗng và là khoản mới, ghi thêm giao dịch:
     * đi vay = tiền vào ví, cho vay = tiền ra khỏi ví.
     */
    fun saveDebt(d: Debt, walletId: String) {
        val id = uid ?: return
        val isNew = d.id.isEmpty()
        if (d.createdAt == 0L) d.createdAt = System.currentTimeMillis()
        val debtId = if (isNew) repo.newId(id, Collections.DEBTS) else d.id
        repo.save(id, Collections.DEBTS, debtId, d.copy())
        if (isNew && walletId.isNotEmpty()) {
            saveTransaction(
                Transaction(
                    type = if (d.kind == DebtKind.BORROW) TxType.INCOME else TxType.EXPENSE,
                    amount = d.amount, walletId = walletId, date = d.startDate, debtId = debtId,
                    note = if (d.kind == DebtKind.BORROW) "Vay của ${d.person}" else "Cho ${d.person} vay",
                )
            )
        }
    }

    fun addDebtPayment(d: Debt, payment: DebtPayment, walletId: String) {
        val id = uid ?: return
        repo.update(id, Collections.DEBTS, d.id, mapOf("payments" to (d.payments + payment).map {
            mapOf("amount" to it.amount, "date" to it.date, "note" to it.note)
        }))
        if (walletId.isNotEmpty()) {
            saveTransaction(
                Transaction(
                    type = if (d.kind == DebtKind.BORROW) TxType.EXPENSE else TxType.INCOME,
                    amount = payment.amount, walletId = walletId, date = payment.date, debtId = d.id,
                    note = if (d.kind == DebtKind.BORROW) "Trả nợ ${d.person}" else "${d.person} trả nợ",
                )
            )
        }
    }

    fun removeDebtPayment(d: Debt, index: Int) {
        val id = uid ?: return
        val list = d.payments.toMutableList().apply { if (index in indices) removeAt(index) }
        repo.update(id, Collections.DEBTS, d.id, mapOf("payments" to list.map {
            mapOf("amount" to it.amount, "date" to it.date, "note" to it.note)
        }))
    }

    fun deleteDebt(debtId: String) {
        val id = uid ?: return
        repo.delete(id, Collections.DEBTS, debtId)
    }

    // ---------- Định kỳ ----------
    fun saveRecurring(r: RecurringRule) {
        val id = uid ?: return
        repo.save(id, Collections.RECURRING, r.id, r.copy())
        ReminderWorker.runNow(getApplication())
    }

    /** Ghi nhận kỳ kế tiếp của khoản chỉ-nhắc (tạo giao dịch + tăng số kỳ). */
    fun confirmRecurring(r: RecurringRule) {
        val id = uid ?: return
        val date = Calc.nextDate(r, zone)
        saveTransaction(
            Transaction(
                type = r.type, amount = r.amount, walletId = r.walletId, categoryId = r.categoryId,
                note = r.name, date = date.startMillis() + 12 * 3_600_000L, recurringId = r.id,
            )
        )
        repo.update(id, Collections.RECURRING, r.id, mapOf("done" to r.done + 1))
        toast("Đã ghi nhận \"${r.name}\"")
    }

    fun skipRecurring(r: RecurringRule) {
        val id = uid ?: return
        repo.update(id, Collections.RECURRING, r.id, mapOf("done" to r.done + 1))
    }

    fun deleteRecurring(ruleId: String) {
        val id = uid ?: return
        repo.delete(id, Collections.RECURRING, ruleId)
    }

    // ---------- Sao lưu ----------
    fun currentBackup() = BackupData(
        exportedAt = System.currentTimeMillis(),
        wallets = wallets.value, categories = categories.value, transactions = transactions.value,
        budgets = budgets.value, goals = goals.value, debts = debts.value, recurring = recurring.value,
    )

    fun exportJson(uri: Uri) = writeToUri(uri) { BackupManager.toJson(currentBackup()) }

    fun exportCsv(uri: Uri) = writeToUri(uri) {
        BackupManager.toCsv(transactions.value, wallets.value, categories.value)
    }

    private fun writeToUri(uri: Uri, content: () -> String) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                        it.write(content().toByteArray(Charsets.UTF_8))
                    } ?: error("Không mở được tệp")
                }
            }
            toast(if (ok.isSuccess) "Đã xuất dữ liệu" else "Lỗi xuất dữ liệu: ${ok.exceptionOrNull()?.message}")
        }
    }

    fun importJson(uri: Uri) {
        val id = uid ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Không đọc được tệp")
                    BackupManager.fromJson(text)
                }
            }
            result.onSuccess { data ->
                repo.batchWrite(id, BackupManager.toWrites(data))
                toast("Đã khôi phục ${data.transactions.size} giao dịch, ${data.wallets.size} ví")
            }.onFailure { toast("Tệp sao lưu không hợp lệ: ${it.message}") }
        }
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }

    fun today(): LocalDate = LocalDate.now(zone)
}
