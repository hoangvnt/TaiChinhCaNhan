package com.hoangvnt.taichinh.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hoangvnt.taichinh.TaiChinhApp
import com.hoangvnt.taichinh.ui.screens.BackupScreen
import com.hoangvnt.taichinh.ui.screens.CategoriesScreen
import com.hoangvnt.taichinh.ui.screens.DebtDetailScreen
import com.hoangvnt.taichinh.ui.screens.DebtEditScreen
import com.hoangvnt.taichinh.ui.screens.HomeScreen
import com.hoangvnt.taichinh.ui.screens.LoginScreen
import com.hoangvnt.taichinh.ui.screens.MoreScreen
import com.hoangvnt.taichinh.ui.screens.PlanScreen
import com.hoangvnt.taichinh.ui.screens.RecurringEditScreen
import com.hoangvnt.taichinh.ui.screens.RecurringScreen
import com.hoangvnt.taichinh.ui.screens.ReportsScreen
import com.hoangvnt.taichinh.ui.screens.TransactionEditScreen
import com.hoangvnt.taichinh.ui.screens.TransactionsScreen
import com.hoangvnt.taichinh.ui.screens.WalletsScreen

@Composable
fun AppRoot(locked: Boolean, onUnlock: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as TaiChinhApp).container
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            !container.firebaseReady -> FirebaseMissingScreen()
            locked -> LockScreen(onUnlock)
            else -> SignedInGate()
        }
    }
}

@Composable
private fun SignedInGate() {
    val vm: MainViewModel = viewModel()
    val user by vm.user.collectAsStateWithLifecycle()
    if (user == null) {
        LoginScreen()
    } else {
        MainNav(vm)
    }
}

@Composable
private fun MainNav(vm: MainViewModel) {
    val nav = rememberNavController()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.messages.collect { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show() }
    }
    val optionalString = { name: String ->
        navArgument(name) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }
    }
    NavHost(nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(vm, nav) }
        composable(Routes.TRANSACTIONS) { TransactionsScreen(vm, nav) }
        composable(Routes.REPORTS) { ReportsScreen(vm, nav) }
        composable(Routes.PLAN) { PlanScreen(vm, nav) }
        composable(Routes.MORE) { MoreScreen(vm, nav) }
        composable(Routes.TX_EDIT, arguments = listOf(optionalString("id"), optionalString("type"))) { e ->
            TransactionEditScreen(vm, nav, e.arguments?.getString("id").orEmpty(), e.arguments?.getString("type").orEmpty())
        }
        composable(Routes.WALLETS) { WalletsScreen(vm, nav) }
        composable(Routes.CATEGORIES) { CategoriesScreen(vm, nav) }
        composable(Routes.RECURRING) { RecurringScreen(vm, nav) }
        composable(Routes.RECURRING_EDIT, arguments = listOf(optionalString("id"))) { e ->
            RecurringEditScreen(vm, nav, e.arguments?.getString("id").orEmpty())
        }
        composable(Routes.DEBT_EDIT, arguments = listOf(optionalString("id"))) { e ->
            DebtEditScreen(vm, nav, e.arguments?.getString("id").orEmpty())
        }
        composable(Routes.DEBT_DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
            DebtDetailScreen(vm, nav, e.arguments?.getString("id").orEmpty())
        }
        composable(Routes.BACKUP) { BackupScreen(vm, nav) }
    }
}

/** Thanh điều hướng dưới cho các màn hình chính. */
@Composable
fun BottomBar(nav: NavController) {
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    NavigationBar {
        topTabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = {
                    if (current != tab.route) {
                        nav.navigate(tab.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
        Text("Ứng dụng đang khóa", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onUnlock) { Text("Mở khóa") }
    }
}

@Composable
private fun FirebaseMissingScreen() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Chưa cấu hình Firebase", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "Hãy thêm tệp app/google-services.json từ Firebase Console rồi build lại ứng dụng. Xem hướng dẫn trong README.",
            textAlign = TextAlign.Center,
        )
    }
}
