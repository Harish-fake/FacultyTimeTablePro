package com.facultytimetable.pro.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val CURRENT_ACADEMIC_YEAR_ID = longPreferencesKey("current_academic_year_id")
        val CURRENT_SEMESTER_ID = longPreferencesKey("current_semester_id")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val BACKUP_REMINDER_INTERVAL = intPreferencesKey("backup_reminder_interval")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }
    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.APP_LOCK_ENABLED] ?: false }
    val pinHash: Flow<String> = context.dataStore.data.map { it[Keys.PIN_HASH] ?: "" }
    val pinSalt: Flow<String> = context.dataStore.data.map { it[Keys.PIN_SALT] ?: "" }
    val currentAcademicYearId: Flow<Long> = context.dataStore.data.map { it[Keys.CURRENT_ACADEMIC_YEAR_ID] ?: -1L }
    val currentSemesterId: Flow<Long> = context.dataStore.data.map { it[Keys.CURRENT_SEMESTER_ID] ?: -1L }
    val lastBackupAt: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_BACKUP_AT] ?: 0L }
    val backupReminderInterval: Flow<Int> = context.dataStore.data.map { it[Keys.BACKUP_REMINDER_INTERVAL] ?: 7 }

    suspend fun setThemeMode(mode: String) {
        require(mode in setOf("system", "light", "dark")) { "Unsupported theme mode: $mode" }
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setPin(hash: String, salt: String) {
        context.dataStore.edit {
            it[Keys.PIN_HASH] = hash
            it[Keys.PIN_SALT] = salt
        }
    }

    suspend fun clearPin() {
        context.dataStore.edit {
            it.remove(Keys.PIN_HASH)
            it.remove(Keys.PIN_SALT)
            it[Keys.APP_LOCK_ENABLED] = false
        }
    }

    suspend fun setCurrentAcademicYearId(id: Long) {
        context.dataStore.edit { it[Keys.CURRENT_ACADEMIC_YEAR_ID] = id }
    }

    suspend fun setCurrentSemesterId(id: Long) {
        context.dataStore.edit { it[Keys.CURRENT_SEMESTER_ID] = id }
    }

    suspend fun setLastBackupAt(time: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_AT] = time }
    }

    suspend fun setBackupReminderInterval(days: Int) {
        context.dataStore.edit { it[Keys.BACKUP_REMINDER_INTERVAL] = days.coerceAtLeast(1) }
    }
}
