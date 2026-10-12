package com.hoangvnt.taichinh.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.hoangvnt.taichinh.TaiChinhApp
import com.hoangvnt.taichinh.data.model.Budget
import com.hoangvnt.taichinh.data.model.Category
import com.hoangvnt.taichinh.data.model.Collections
import com.hoangvnt.taichinh.data.model.Debt
import com.hoangvnt.taichinh.data.model.DebtKind
import com.hoangvnt.taichinh.data.model.RecurringRule
import com.hoangvnt.taichinh.data.model.Transaction
import com.hoangvnt.taichinh.domain.Calc
import com.hoangvnt.taichinh.util.endMillis
import com.hoangvnt.taichinh.util.formatDate
import com.hoangvnt.taichinh.util.formatMoney
import com.hoangvnt.taichinh.util.startMillis
import com.hoangvnt.taichinh.util.zone
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

/**
 * Chạy định kỳ (~6 giờ/lần):
 *  - Tự động ghi giao dịch định kỳ đến hạn, nhắc các khoản chỉ-nhắc.
 *  - Cảnh báo ngân sách đạt 80% / 100%.
 *  - Nhắc khoản nợ sắp đến hạn (trong 3 ngày).
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as TaiChinhApp).container
        if (!container.firebaseReady) return Result.success()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val repo = container.finance
        val notify = container.settings.remindersEnabled.first()
        val prefs = applicationContext.getSharedPreferences("notified", Context.MODE_PRIVATE)

        return try {
            val today = LocalDate.now(zone)

            // 1. Giao dịch định kỳ
            val rules = repo.getAll(uid, Collections.RECURRING, RecurringRule::class.java)
            var autoCount = 0
            for (rule in rules.filter { it.active }) {
                if (rule.autoCreate) {
                    val due = Calc.dueDates(rule, today, zone)
                    if (due.isEmpty()) continue
                    val items = due.map { d ->
                        Triple(
                            Collections.TRANSACTIONS, "",
                            Transaction(
                                type = rule.type, amount = rule.amount, walletId = rule.walletId,
                                categoryId = rule.categoryId, note = rule.name, date = d.startMillis() + 12 * 3_600_000L,
                                recurringId = rule.id, createdAt = System.currentTimeMillis(),
                            ) as Any
                        )
                    }
                    repo.batchWrite(uid, items)
                    repo.update(uid, Collections.RECURRING, rule.id, mapOf("done" to rule.done + due.size))
                    autoCount += due.size
                } else {
                    val next = Calc.nextDate(rule, zone)
                    val nextMs = next.startMillis()
                    if (!next.isAfter(today.plusDays(1)) && rule.lastNotified < nextMs) {
                        if (notify) Notifications.show(
                            applicationContext, rule.id.hashCode(),
                            "Sắp đến hạn: ${rule.name}",
                            "${formatMoney(rule.amount)} — ngày ${formatDate(nextMs)}. Mở ứng dụng để ghi nhận.",
                        )
                        repo.update(uid, Collections.RECURRING, rule.id, mapOf("lastNotified" to nextMs))
                    }
                }
            }
            if (autoCount > 0 && notify) {
                Notifications.show(applicationContext, 1001, "Đã ghi giao dịch định kỳ", "Tự động ghi $autoCount giao dịch định kỳ.")
            }

            // 2. Ngân sách
            val ym = YearMonth.now(zone)
            val budgets = repo.getAll(uid, Collections.BUDGETS, Budget::class.java)
            if (budgets.isNotEmpty() && notify) {
                val txs = repo.getAll(uid, Collections.TRANSACTIONS, Transaction::class.java)
                val cats = repo.getAll(uid, Collections.CATEGORIES, Category::class.java).associateBy { it.id }
                val monthTxs = Calc.inRange(txs, ym.startMillis(), ym.endMillis())
                for (s in Calc.budgetStatuses(budgets, monthTxs)) {
                    val level = when {
                        s.ratio >= 1f -> 100
                        s.ratio >= 0.8f -> 80
                        else -> continue
                    }
                    val key = "budget:${s.budget.id}:$ym:$level"
                    if (prefs.getBoolean(key, false)) continue
                    val name = cats[s.budget.categoryId]?.name ?: "Danh mục"
                    val text = if (level == 100) "Đã vượt ngân sách $name: ${formatMoney(s.spent)} / ${formatMoney(s.budget.limit)}"
                    else "Đã dùng ${(s.ratio * 100).toInt()}% ngân sách $name (${formatMoney(s.spent)} / ${formatMoney(s.budget.limit)})"
                    Notifications.show(applicationContext, key.hashCode(), "Cảnh báo ngân sách", text)
                    prefs.edit().putBoolean(key, true).apply()
                }
            }

            // 3. Nợ sắp đến hạn
            if (notify) {
                val debts = repo.getAll(uid, Collections.DEBTS, Debt::class.java)
                val limit = today.plusDays(3).startMillis()
                for (d in debts) {
                    if (d.dueDate <= 0 || d.dueDate > limit || Calc.debtRemaining(d) <= 0) continue
                    val key = "debt:${d.id}:${d.dueDate}"
                    if (prefs.getBoolean(key, false)) continue
                    val text = if (d.kind == DebtKind.BORROW) "Bạn cần trả ${d.person} ${formatMoney(Calc.debtRemaining(d))} trước ${formatDate(d.dueDate)}"
                    else "${d.person} cần trả bạn ${formatMoney(Calc.debtRemaining(d))} trước ${formatDate(d.dueDate)}"
                    Notifications.show(applicationContext, key.hashCode(), "Khoản nợ sắp đến hạn", text)
                    prefs.edit().putBoolean(key, true).apply()
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.w("ReminderWorker", "Lỗi xử lý nhắc nhở", e)
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val wm = WorkManager.getInstance(context)
            wm.enqueueUniquePeriodicWork(
                "reminders",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderWorker>(6, TimeUnit.HOURS).build(),
            )
        }

        /** Chạy ngay một lần (khi mở ứng dụng) để ghi các giao dịch định kỳ đến hạn. */
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "reminders-now",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ReminderWorker>().build(),
            )
        }
    }
}
