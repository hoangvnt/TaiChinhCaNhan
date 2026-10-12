package com.hoangvnt.taichinh.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.hoangvnt.taichinh.data.model.Collections
import com.hoangvnt.taichinh.data.model.Wallet
import com.hoangvnt.taichinh.data.model.WalletType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Truy cập Firestore. Firestore trên Android bật bộ nhớ đệm offline mặc định:
 * mọi thao tác ghi được áp dụng ngay vào cache cục bộ và tự đồng bộ khi có mạng.
 * Vì vậy KHÔNG await các thao tác ghi (await sẽ treo khi offline).
 */
class FinanceRepository(private val db: FirebaseFirestore) {

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun col(uid: String, name: String) = userDoc(uid).collection(name)

    fun <T : Any> observe(uid: String, name: String, clazz: Class<T>): Flow<List<T>> = callbackFlow {
        val reg = col(uid, name).addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "Lỗi đọc $name", err)
                return@addSnapshotListener
            }
            if (snap != null) trySend(snap.documents.mapNotNull { it.toObject(clazz) })
        }
        awaitClose { reg.remove() }
    }

    suspend fun <T : Any> getAll(uid: String, name: String, clazz: Class<T>): List<T> =
        col(uid, name).get().await().documents.mapNotNull { it.toObject(clazz) }

    /** Tạo mới (id rỗng) hoặc ghi đè. Trả về id của tài liệu. */
    fun save(uid: String, name: String, id: String, data: Any): String {
        val ref = if (id.isEmpty()) col(uid, name).document() else col(uid, name).document(id)
        ref.set(data).addOnFailureListener { Log.w(TAG, "Lỗi ghi $name/${ref.id}", it) }
        return ref.id
    }

    fun newId(uid: String, name: String): String = col(uid, name).document().id

    fun update(uid: String, name: String, id: String, fields: Map<String, Any>) {
        col(uid, name).document(id).update(fields)
            .addOnFailureListener { Log.w(TAG, "Lỗi cập nhật $name/$id", it) }
    }

    fun delete(uid: String, name: String, id: String) {
        col(uid, name).document(id).delete()
            .addOnFailureListener { Log.w(TAG, "Lỗi xóa $name/$id", it) }
    }

    /** Ghi hàng loạt (ví dụ khi khôi phục sao lưu). Mỗi phần tử: (collection, id, data). */
    fun batchWrite(uid: String, items: List<Triple<String, String, Any>>) {
        items.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (name, id, data) ->
                val ref = if (id.isEmpty()) col(uid, name).document() else col(uid, name).document(id)
                batch.set(ref, data)
            }
            batch.commit().addOnFailureListener { Log.w(TAG, "Lỗi ghi hàng loạt", it) }
        }
    }

    /**
     * Tạo dữ liệu mặc định (danh mục + ví tiền mặt) cho người dùng mới.
     * Chỉ kiểm tra với máy chủ để tránh tạo trùng khi đang offline.
     */
    suspend fun ensureSeeded(uid: String) {
        val snap = try {
            userDoc(uid).get(Source.SERVER).await()
        } catch (e: Exception) {
            Log.i(TAG, "Bỏ qua khởi tạo dữ liệu (offline?)", e)
            return
        }
        if (snap.getBoolean("seeded") == true) return
        val now = System.currentTimeMillis()
        val batch = db.batch()
        DefaultData.categories().forEach { batch.set(col(uid, Collections.CATEGORIES).document(), it) }
        batch.set(
            col(uid, Collections.WALLETS).document(),
            Wallet(name = "Tiền mặt", type = WalletType.CASH, icon = WalletType.icon(WalletType.CASH), createdAt = now)
        )
        batch.set(userDoc(uid), mapOf("seeded" to true, "createdAt" to now), SetOptions.merge())
        batch.commit().addOnFailureListener { Log.w(TAG, "Lỗi khởi tạo dữ liệu", it) }
    }

    companion object {
        private const val TAG = "FinanceRepository"
    }
}
