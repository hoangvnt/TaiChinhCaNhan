package com.hoangvnt.taichinh.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val IncomeColor = Color(0xFF2E7D32)
val ExpenseColor = Color(0xFFC62828)
val IncomeColorDark = Color(0xFF81C784)
val ExpenseColorDark = Color(0xFFEF9A9A)
val WarningColor = Color(0xFFF9A825)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B8A5A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA6F2C9),
    onPrimaryContainer = Color(0xFF002112),
    secondary = Color(0xFF4E6356),
    tertiary = Color(0xFF3B6470),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD6AE),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF005234),
    onPrimaryContainer = Color(0xFFA6F2C9),
    secondary = Color(0xFFB4CCBC),
    tertiary = Color(0xFFA3CDDB),
)

@Composable
fun TaiChinhTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun incomeColor() = if (isSystemInDarkTheme()) IncomeColorDark else IncomeColor

@Composable
fun expenseColor() = if (isSystemInDarkTheme()) ExpenseColorDark else ExpenseColor
