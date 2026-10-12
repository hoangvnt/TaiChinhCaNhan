@file:OptIn(ExperimentalMaterial3Api::class)

package com.hoangvnt.taichinh.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hoangvnt.taichinh.ui.MainViewModel
import com.hoangvnt.taichinh.ui.components.BackTopBar
import com.hoangvnt.taichinh.ui.components.ConfirmDialog
import com.hoangvnt.taichinh.ui.components.SectionCard
import java.time.LocalDate

@Composable
fun BackupScreen(vm: MainViewModel, nav: NavController) {
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportJson)
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(vm::exportCsv)
    }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingImport = uri
    }
    val stamp = LocalDate.now().toString()

    Scaffold(topBar = { BackTopBar("Sao lưu & xuất dữ liệu", nav) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(title = "☁️ Đồng bộ đám mây") {
                Text(
                    "Dữ liệu của bạn được tự động đồng bộ lên Firebase theo tài khoản Google. " +
                        "Khi mất mạng, ứng dụng vẫn hoạt động bình thường và sẽ đồng bộ lại khi có mạng. " +
                        "Đăng nhập cùng tài khoản trên máy khác để xem dữ liệu.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SectionCard(title = "Sao lưu thủ công (JSON)") {
                Text(
                    "Tệp JSON chứa toàn bộ dữ liệu: ví, danh mục, giao dịch, ngân sách, mục tiêu, nợ, định kỳ.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = { exportJson.launch("taichinh-saoluu-$stamp.json") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Icon(Icons.Default.Download, null)
                    Text("  Xuất tệp sao lưu")
                }
                OutlinedButton(onClick = { importJson.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Upload, null)
                    Text("  Khôi phục từ tệp")
                }
            }
            SectionCard(title = "Xuất Excel (CSV)") {
                Text("Xuất danh sách giao dịch ra tệp CSV để mở bằng Excel / Google Sheets.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { exportCsv.launch("taichinh-giaodich-$stamp.csv") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Icon(Icons.Default.TableChart, null)
                    Text("  Xuất CSV")
                }
            }
        }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = "Khôi phục dữ liệu?",
            text = "Dữ liệu trong tệp sẽ được ghi vào tài khoản hiện tại. Các mục trùng mã sẽ bị ghi đè, các mục khác được giữ nguyên.",
            confirmLabel = "Khôi phục",
            onConfirm = { vm.importJson(uri) },
            onDismiss = { pendingImport = null },
        )
    }
}
