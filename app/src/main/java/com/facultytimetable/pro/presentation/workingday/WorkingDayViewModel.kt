package com.facultytimetable.pro.presentation.workingday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.WorkingDayDao
import com.facultytimetable.pro.data.local.db.entity.WorkingDayEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkingDayState(
    val days: List<WorkingDayUi> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

data class WorkingDayUi(
    val id: Long = 0,
    val dayOfWeek: Int,
    val dayName: String,
    val isWorking: Boolean
)

@HiltViewModel
class WorkingDayViewModel @Inject constructor(
    private val workingDayDao: WorkingDayDao
) : ViewModel() {

    private val _state = MutableStateFlow(WorkingDayState())
    val state: StateFlow<WorkingDayState> = _state

    init { loadDays() }

    private fun loadDays() {
        viewModelScope.launch {
            workingDayDao.getAllWorkingDays().collect { entities ->
                if (entities.isEmpty()) {
                    initializeDefaults()
                } else {
                    _state.value = _state.value.copy(
                        days = entities.map { it.toUi() },
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun initializeDefaults() {
        viewModelScope.launch {
            val defaults = listOf(
                WorkingDayEntity(dayOfWeek = 1, dayName = "Monday", isWorking = true),
                WorkingDayEntity(dayOfWeek = 2, dayName = "Tuesday", isWorking = true),
                WorkingDayEntity(dayOfWeek = 3, dayName = "Wednesday", isWorking = true),
                WorkingDayEntity(dayOfWeek = 4, dayName = "Thursday", isWorking = true),
                WorkingDayEntity(dayOfWeek = 5, dayName = "Friday", isWorking = true),
                WorkingDayEntity(dayOfWeek = 6, dayName = "Saturday", isWorking = false),
                WorkingDayEntity(dayOfWeek = 7, dayName = "Sunday", isWorking = false)
            )
            workingDayDao.insertAll(defaults)
        }
    }

    fun toggleDay(dayOfWeek: Int) {
        val days = _state.value.days.map { day ->
            if (day.dayOfWeek == dayOfWeek) day.copy(isWorking = !day.isWorking)
            else day
        }
        _state.value = _state.value.copy(days = days)
    }

    fun save() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                _state.value.days.forEach { day ->
                    workingDayDao.setWorking(day.dayOfWeek, day.isWorking)
                }
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }

    fun clearSuccess() { _state.value = _state.value.copy(saveSuccess = false) }

    private fun WorkingDayEntity.toUi() = WorkingDayUi(
        id = id,
        dayOfWeek = dayOfWeek,
        dayName = dayName,
        isWorking = isWorking
    )
}
