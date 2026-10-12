package com.hoangvnt.taichinh.ui

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hoangvnt.taichinh.TaiChinhApp
import kotlinx.coroutines.launch

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val auth = (app as TaiChinhApp).container.auth

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun signIn(activity: Activity) {
        if (loading) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                auth.signInWithGoogle(activity)
            } catch (e: GetCredentialCancellationException) {
                // Người dùng hủy — bỏ qua
            } catch (e: NoCredentialException) {
                error = "Không tìm thấy tài khoản Google trên thiết bị. Hãy thêm tài khoản trong Cài đặt."
            } catch (e: Exception) {
                error = e.message ?: "Đăng nhập thất bại"
            } finally {
                loading = false
            }
        }
    }
}
