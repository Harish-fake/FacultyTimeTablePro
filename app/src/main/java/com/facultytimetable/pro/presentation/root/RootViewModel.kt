package com.facultytimetable.pro.presentation.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.datastore.AppPreferences
import com.facultytimetable.pro.data.local.security.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class RootState(
    val themeMode: String = "system",
    val isAppLockEnabled: Boolean = false,
    val hasPin: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class RootViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    val state: StateFlow<RootState> = combine(
        appPreferences.themeMode,
        appPreferences.isAppLockEnabled,
        appPreferences.pinHash,
        appPreferences.pinSalt
    ) { theme, lock, hash, salt ->
        RootState(
            themeMode = theme,
            isAppLockEnabled = lock,
            hasPin = hash.isNotBlank() && salt.isNotBlank(),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootState())

    suspend fun verifyPin(pin: String): Boolean {
        val hash = appPreferences.pinHash.first()
        val salt = appPreferences.pinSalt.first()
        return PinHasher.verify(pin, salt, hash)
    }
}
