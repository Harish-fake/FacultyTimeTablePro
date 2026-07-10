package com.facultytimetable.pro.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.datastore.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val isLastPage: Boolean = false,
    val shouldSkip: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state

    init {
        viewModelScope.launch {
            val firstLaunch = appPreferences.isFirstLaunch.first()
            if (!firstLaunch) {
                _state.value = _state.value.copy(shouldSkip = true)
            }
        }
    }

    fun onLastPageReached() {
        _state.value = _state.value.copy(isLastPage = true)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            appPreferences.setFirstLaunch(false)
        }
    }
}
