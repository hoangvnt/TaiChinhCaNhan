package com.hoangvnt.taichinh.data

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    init {
        auth.addAuthStateListener { _user.value = it.currentUser }
    }

    /** Web client ID do plugin google-services sinh ra từ google-services.json. */
    private fun webClientId(): String? {
        @Suppress("DiscouragedApi")
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) context.getString(resId) else null
    }

    suspend fun signInWithGoogle(activity: Activity) {
        val clientId = webClientId()
            ?: error("Thiếu default_web_client_id. Hãy bật Google Sign-In trong Firebase và tải lại google-services.json.")
        val option = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(activity).getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        } else {
            error("Loại thông tin đăng nhập không được hỗ trợ")
        }
    }

    suspend fun signOut() {
        auth.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }
}
