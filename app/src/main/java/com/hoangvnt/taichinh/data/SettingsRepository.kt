package com.hoangvnt.taichinh.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Cài đặt riêng trên thiết bị (không đồng bộ). */
class SettingsRepository(private val context: Context) {
    private val lockKey = booleanPreferencesKey("app_lock")
    private val remindersKey = booleanPreferencesKey("reminders")

    val lockEnabled: Flow<Boolean> = context.dataStore.data.map { it[lockKey] ?: false }
    val remindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[remindersKey] ?: true }

    suspend fun setLockEnabled(value: Boolean) {
        context.dataStore.edit { it[lockKey] = value }
    }

    suspend fun setRemindersEnabled(value: Boolean) {
        context.dataStore.edit { it[remindersKey] = value }
    }
}
