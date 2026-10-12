package com.hoangvnt.taichinh

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.hoangvnt.taichinh.data.AuthRepository
import com.hoangvnt.taichinh.data.FinanceRepository
import com.hoangvnt.taichinh.data.SettingsRepository

/** DI thủ công đơn giản. */
class AppContainer(context: Context) {
    val firebaseReady: Boolean = FirebaseApp.getApps(context).isNotEmpty()
    val settings = SettingsRepository(context)
    val auth: AuthRepository by lazy { AuthRepository(context) }
    val finance: FinanceRepository by lazy { FinanceRepository(FirebaseFirestore.getInstance()) }
}
