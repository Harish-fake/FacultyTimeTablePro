package com.facultytimetable.pro.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.datastore.AppPreferences
import com.facultytimetable.pro.data.local.security.PinHasher
import com.facultytimetable.pro.data.local.seed.DatabaseSeeder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val themeMode: String = "system",
    val isAppLockEnabled: Boolean = false,
    val hasPin: Boolean = false,
    val backupReminderInterval: Int = 7,
    val lastBackupAt: Long = 0L,
    val isResetRunning: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val databaseSeeder: DatabaseSeeder
) : ViewModel() {

    private val resetRunning = MutableStateFlow(false)

    val state: StateFlow<SettingsState> = combine(
        appPreferences.themeMode,
        appPreferences.isAppLockEnabled,
        appPreferences.pinHash,
        appPreferences.backupReminderInterval,
        appPreferences.lastBackupAt,
        resetRunning
    ) { values ->
        SettingsState(
            themeMode = values[0] as String,
            isAppLockEnabled = values[1] as Boolean,
            hasPin = (values[2] as String).isNotBlank(),
            backupReminderInterval = values[3] as Int,
            lastBackupAt = values[4] as Long,
            isResetRunning = values[5] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setThemeMode(mode: String) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    /**
     * Stores a new PIN and enables the lock. Returns null on success or a user-facing
     * validation message.
     */
    suspend fun setPin(pin: String, confirmation: String): String? {
        if (!PinHasher.isValidPin(pin)) return "The PIN must be 4 to 8 digits."
        if (pin != confirmation) return "The two PINs do not match."
        val salt = PinHasher.newSalt()
        appPreferences.setPin(PinHasher.hash(pin, salt), salt)
        appPreferences.setAppLockEnabled(true)
        return null
    }

    suspend fun verifyPin(pin: String): Boolean {
        val hash = appPreferences.pinHash.first()
        val salt = appPreferences.pinSalt.first()
        return PinHasher.verify(pin, salt, hash)
    }

    fun disableLock() {
        viewModelScope.launch { appPreferences.clearPin() }
    }

    fun setBackupReminderInterval(days: Int) {
        viewModelScope.launch { appPreferences.setBackupReminderInterval(days) }
    }

    fun resetDemoData(onDone: (String) -> Unit) {
        if (resetRunning.value) return
        viewModelScope.launch {
            resetRunning.value = true
            try {
                databaseSeeder.resetToSampleData()
                onDone("Demo data restored")
            } catch (e: Exception) {
                onDone("Could not restore demo data: ${e.message ?: "unknown error"}")
            } finally {
                resetRunning.value = false
            }
        }
    }
}
