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
        val LANGUAGE = stringPreferencesKey("language")
        val FONT_SCALE = intPreferencesKey("font_scale")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_PIN = stringPreferencesKey("app_pin")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val CURRENT_ACADEMIC_YEAR_ID = longPreferencesKey("current_academic_year_id")
        val CURRENT_SEMESTER_ID = longPreferencesKey("current_semester_id")
        val WORKING_DAYS = stringPreferencesKey("working_days")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val LAST_BACKUP_REMINDER = longPreferencesKey("last_backup_reminder")
        val BACKUP_REMINDER_INTERVAL = intPreferencesKey("backup_reminder_interval")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: "system"
    }

    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LANGUAGE] ?: "en"
    }

    val fontScale: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.FONT_SCALE] ?: 100
    }

    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_LOCK_ENABLED] ?: false
    }

    val appPin: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_PIN] ?: ""
    }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BIOMETRIC_ENABLED] ?: false
    }

    val currentAcademicYearId: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_ACADEMIC_YEAR_ID] ?: -1L
    }

    val currentSemesterId: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_SEMESTER_ID] ?: -1L
    }

    val workingDays: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.WORKING_DAYS] ?: "1,2,3,4,5"
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.FIRST_LAUNCH] ?: true
    }

    val lastBackupReminder: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_BACKUP_REMINDER] ?: 0L
    }

    val backupReminderInterval: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.BACKUP_REMINDER_INTERVAL] ?: 7
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    suspend fun setFontScale(scale: Int) {
        context.dataStore.edit { it[Keys.FONT_SCALE] = scale }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setAppPin(pin: String) {
        context.dataStore.edit { it[Keys.APP_PIN] = pin }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setCurrentAcademicYearId(id: Long) {
        context.dataStore.edit { it[Keys.CURRENT_ACADEMIC_YEAR_ID] = id }
    }

    suspend fun setCurrentSemesterId(id: Long) {
        context.dataStore.edit { it[Keys.CURRENT_SEMESTER_ID] = id }
    }

    suspend fun setWorkingDays(days: String) {
        context.dataStore.edit { it[Keys.WORKING_DAYS] = days }
    }

    suspend fun setFirstLaunch(value: Boolean) {
        context.dataStore.edit { it[Keys.FIRST_LAUNCH] = value }
    }

    suspend fun setLastBackupReminder(time: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_REMINDER] = time }
    }

    suspend fun setBackupReminderInterval(days: Int) {
        context.dataStore.edit { it[Keys.BACKUP_REMINDER_INTERVAL] = days }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
