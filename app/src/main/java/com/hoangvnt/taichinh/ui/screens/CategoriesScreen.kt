@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.hoangvnt.taichinh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hoangvnt.taichinh.data.DefaultData
import com.hoangvnt.taichinh.data.model.Category
import com.hoangvnt.taichinh.data.model.TxType
import com.hoangvnt.taichinh.ui.MainViewModel
import com.hoangvnt.taichinh.ui.components.BackTopBar
import com.hoangvnt.taichinh.ui.components.ConfirmDialog
import com.hoangvnt.taichinh.ui.components.EmojiBadge

@Composable
fun CategoriesScreen(vm: MainViewModel, nav: NavController) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    var editing by remember { mutableStateOf<Category?>(null) }
    val type = if (tab == 0) TxType.EXPENSE else TxType.INCOME

    Scaffold(
        topBar = { BackTopBar("Danh mục", nav) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = Category(type = type, color = DefaultData.palette.random()) }) {
                Icon(Icons.Default.Add, "Thêm danh mục")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Chi tiêu") })
                Tab(tab == 1, { tab = 1 }, text = { Text("Thu nhập") })
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                items(categories.filter { it.type == type }, key = { it.id }) { c ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { editing = c }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EmojiBadge(c.icon, Color(c.color).copy(alpha = 0.25f))
                        Spacer(Modifier.width(12.dp))
                        Text(c.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    editing?.let { c -> CategoryDialog(vm, c) { editing = null } }
}

@Composable
private fun CategoryDialog(vm: MainViewModel, category: Category, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(category.name) }
    var icon by remember { mutableStateOf(category.icon) }
    var color by remember { mutableStateOf(category.color) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category.id.isEmpty()) "Thêm danh mục" else "Sửa danh mục") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(icon, { icon = it.take(8) }, label = { Text("Emoji") }, singleLine = true, modifier = Modifier.width(96.dp))
                    OutlinedTextField(name, { name = it }, label = { Text("Tên") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("Màu", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DefaultData.palette.forEach { c ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .background(Color(c), CircleShape)
                                .then(if (c == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { color = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                vm.saveCategory(category.copy(name = name.trim(), icon = icon.ifBlank { "📦" }, color = color))
                onDismiss()
            }) { Text("Lưu") }
        },
        dismissButton = {
            Row {
                if (category.id.isNotEmpty()) {
                    TextButton(onClick = { confirmDelete = true }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Hủy") }
            }
        },
    )
    if (confirmDelete) {
        ConfirmDialog(
            title = "Xóa danh mục?",
            text = "Các giao dịch thuộc danh mục này sẽ hiển thị là \"Chưa phân loại\". Ngân sách của danh mục cũng bị xóa.",
            onConfirm = {
                vm.deleteCategory(category.id)
                onDismiss()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
