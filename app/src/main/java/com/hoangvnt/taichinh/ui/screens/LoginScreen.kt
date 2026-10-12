package com.hoangvnt.taichinh.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoangvnt.taichinh.ui.AuthViewModel

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun LoginScreen(vm: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("💰", fontSize = 72.sp)
        Spacer(Modifier.height(16.dp))
        Text("Tài Chính Cá Nhân", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Quản lý thu chi, ngân sách, mục tiêu tiết kiệm và khoản vay.\nDữ liệu được đồng bộ an toàn qua tài khoản Google của bạn.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = { context.findActivity()?.let(vm::signIn) },
            enabled = !vm.loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (vm.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("Đăng nhập với Google")
        }
        vm.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}
