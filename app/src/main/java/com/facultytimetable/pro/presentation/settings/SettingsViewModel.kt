package com.facultytimetable.pro.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.datastore.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val themeMode: String = "system",
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val backupReminderInterval: Int = 7
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    val state: StateFlow<SettingsState> = combine(
        appPreferences.themeMode,
        appPreferences.isAppLockEnabled,
        appPreferences.isBiometricEnabled,
        appPreferences.backupReminderInterval
    ) { theme, lock, bio, interval ->
        SettingsState(themeMode = theme, isAppLockEnabled = lock, isBiometricEnabled = bio, backupReminderInterval = interval)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun setThemeMode(mode: String) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setAppLockEnabled(enabled)
            if (!enabled) appPreferences.setBiometricEnabled(false)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setBiometricEnabled(enabled) }
    }

    fun setBackupReminderInterval(days: Int) {
        viewModelScope.launch { appPreferences.setBackupReminderInterval(days) }
    }
}
