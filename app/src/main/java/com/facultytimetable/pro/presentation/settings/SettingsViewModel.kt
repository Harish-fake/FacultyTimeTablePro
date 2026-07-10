package com.facultytimetable.pro.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.datastore.AppPreferences
import com.facultytimetable.pro.data.local.db.AppDatabase
import com.facultytimetable.pro.domain.repository.RecycleBinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    private val _state = MutableStateFlow(SettingsState())

    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                appPreferences.themeMode,
                appPreferences.isAppLockEnabled,
                appPreferences.isBiometricEnabled,
                appPreferences.backupReminderInterval,
                recycleBinRepository.getCountFlow()
            ) { theme, lock, bio, interval, binCount ->
                _state.value = _state.value.copy(
                    themeMode = theme, isAppLockEnabled = lock, isBiometricEnabled = bio,
                    backupReminderInterval = interval, recycleBinCount = binCount
                )
            }.collect { }
        }
    }

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
