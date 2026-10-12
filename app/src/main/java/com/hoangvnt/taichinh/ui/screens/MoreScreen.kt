@file:OptIn(ExperimentalMaterial3Api::class)

package com.hoangvnt.taichinh.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hoangvnt.taichinh.BuildConfig
import com.hoangvnt.taichinh.TaiChinhApp
import com.hoangvnt.taichinh.ui.BottomBar
import com.hoangvnt.taichinh.ui.MainViewModel
import com.hoangvnt.taichinh.ui.Routes
import com.hoangvnt.taichinh.ui.components.ConfirmDialog
import kotlinx.coroutines.launch

@Composable
fun MoreScreen(vm: MainViewModel, nav: NavController) {
    val context = LocalContext.current
    val settings = (context.applicationContext as TaiChinhApp).container.settings
    val scope = rememberCoroutineScope()
    val lock by settings.lockEnabled.collectAsStateWithLifecycle(initialValue = false)
    val reminders by settings.remindersEnabled.collectAsStateWithLifecycle(initialValue = true)
    val user by vm.user.collectAsStateWithLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Khác") }) },
        bottomBar = { BottomBar(nav) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ListItem(
                headlineContent = { Text(user?.displayName ?: "Tài khoản") },
                supportingContent = { Text(user?.email ?: "") },
                leadingContent = { Icon(Icons.Default.Person, null) },
            )
            HorizontalDivider()
            NavItem(Icons.Default.AccountBalanceWallet, "Ví & tài khoản", "Tiền mặt, ngân hàng, thẻ, ví điện tử") { nav.navigate(Routes.WALLETS) }
            NavItem(Icons.Default.Category, "Danh mục", "Tùy chỉnh danh mục thu chi") { nav.navigate(Routes.CATEGORIES) }
            NavItem(Icons.Default.Repeat, "Giao dịch định kỳ", "Lương, tiền nhà, hóa đơn hằng tháng") { nav.navigate(Routes.RECURRING) }
            NavItem(Icons.Default.Backup, "Sao lưu & xuất dữ liệu", "JSON, CSV (Excel)") { nav.navigate(Routes.BACKUP) }
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Khóa ứng dụng") },
                supportingContent = { Text("Yêu cầu vân tay / khuôn mặt / mã PIN khi mở") },
                leadingContent = { Icon(Icons.Default.Fingerprint, null) },
                trailingContent = {
                    Switch(lock, { enable ->
                        val auth = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        if (enable && BiometricManager.from(context).canAuthenticate(auth) != BiometricManager.BIOMETRIC_SUCCESS) {
                            Toast.makeText(context, "Hãy cài đặt khóa màn hình hoặc vân tay cho thiết bị trước", Toast.LENGTH_LONG).show()
                        } else {
                            scope.launch { settings.setLockEnabled(enable) }
                        }
                    })
                },
            )
            ListItem(
                headlineContent = { Text("Thông báo nhắc nhở") },
                supportingContent = { Text("Định kỳ, vượt ngân sách, nợ sắp đến hạn") },
                leadingContent = { Icon(Icons.Default.Notifications, null) },
                trailingContent = { Switch(reminders, { v -> scope.launch { settings.setRemindersEnabled(v) } }) },
            )
            HorizontalDivider()
            NavItem(Icons.AutoMirrored.Filled.Logout, "Đăng xuất", null) { confirmLogout = true }
            Text(
                "Phiên bản ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "Đăng xuất?",
            text = "Dữ liệu đã được lưu trên đám mây, bạn có thể đăng nhập lại bất cứ lúc nào.",
            confirmLabel = "Đăng xuất",
            onConfirm = { vm.signOut() },
            onDismiss = { confirmLogout = false },
        )
    }
}

@Composable
private fun NavItem(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle) }
        } else null,
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
