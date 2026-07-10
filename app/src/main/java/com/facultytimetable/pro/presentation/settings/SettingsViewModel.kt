package com.facultytimetable.pro.presentation.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.datastore.AppPreferences
import com.facultytimetable.pro.data.local.db.AppDatabase
import com.facultytimetable.pro.domain.repository.RecycleBinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context

data class SettingsState(
    val themeMode: String = "system",
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val backupReminderInterval: Int = 7,
    val recycleBinCount: Int = 0,
    val showResetDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val recycleBinRepository: RecycleBinRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val state: StateFlow<SettingsState> = combine(
        appPreferences.themeMode,
        appPreferences.isAppLockEnabled,
        appPreferences.isBiometricEnabled,
        appPreferences.backupReminderInterval,
        recycleBinRepository.getCountFlow()
    ) { theme, lock, bio, interval, binCount ->
        SettingsState(themeMode = theme, isAppLockEnabled = lock, isBiometricEnabled = bio, backupReminderInterval = interval, recycleBinCount = binCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun setThemeMode(mode: String) { viewModelScope.launch { appPreferences.setThemeMode(mode) } }
    fun setAppLockEnabled(enabled: Boolean) { viewModelScope.launch { appPreferences.setAppLockEnabled(enabled); if (!enabled) appPreferences.setBiometricEnabled(false) } }
    fun setBiometricEnabled(enabled: Boolean) { viewModelScope.launch { appPreferences.setBiometricEnabled(enabled) } }
    fun setBackupReminderInterval(days: Int) { viewModelScope.launch { appPreferences.setBackupReminderInterval(days) } }

    fun showResetDialog() { _state.value = _state.value.copy(showResetDialog = true) }
    fun hideResetDialog() { _state.value = _state.value.copy(showResetDialog = false) }

    fun resetDatabase() {
        viewModelScope.launch {
            context.deleteDatabase(AppDatabase.DATABASE_NAME)
            appPreferences.clearAll()
            hideResetDialog()
        }
    }
}
