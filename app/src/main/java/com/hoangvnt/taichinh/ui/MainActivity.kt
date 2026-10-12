package com.hoangvnt.taichinh.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.hoangvnt.taichinh.TaiChinhApp
import com.hoangvnt.taichinh.ui.theme.TaiChinhTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    private val locked = mutableStateOf(false)
    private var lockEnabled = false
    private var backgroundAt = 0L

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as TaiChinhApp).container.settings
        // Đọc nhanh trạng thái khóa trước khi vẽ giao diện để không lộ dữ liệu
        lockEnabled = runBlocking { settings.lockEnabled.first() }
        if (savedInstanceState == null) locked.value = lockEnabled
        lifecycleScope.launch { settings.lockEnabled.collect { lockEnabled = it } }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            TaiChinhTheme {
                AppRoot(locked = locked.value, onUnlock = ::authenticate)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (lockEnabled && backgroundAt > 0 && System.currentTimeMillis() - backgroundAt > LOCK_AFTER_MS) {
            locked.value = true
        }
    }

    private fun authenticate() {
        val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Thiết bị chưa cài khóa màn hình — bỏ qua khóa ứng dụng", Toast.LENGTH_LONG).show()
            locked.value = false
            return
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                locked.value = false
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Mở khóa Tài Chính Cá Nhân")
                .setSubtitle("Dùng vân tay, khuôn mặt hoặc mã PIN của máy")
                .setAllowedAuthenticators(authenticators)
                .build()
        )
    }

    companion object {
        private const val LOCK_AFTER_MS = 30_000L
    }
}
