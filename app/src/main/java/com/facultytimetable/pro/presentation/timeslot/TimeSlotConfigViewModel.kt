package com.facultytimetable.pro.presentation.timeslot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.domain.repository.TimeSlotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimeSlotConfigState(
    val dayConfigs: List<DayConfig> = (1..7).map { DayConfig(dayOfWeek = it) },
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

data class DayConfig(
    val dayOfWeek: Int,
    val isWorkingDay: Boolean = false,
    val periods: List<PeriodModel> = emptyList()
)

data class PeriodModel(
    val id: Long = 0,
    val periodNumber: Int = 1,
    val startTime: String = "",
    val endTime: String = "",
    val type: SlotType = SlotType.REGULAR
)

@HiltViewModel
class TimeSlotConfigViewModel @Inject constructor(
    private val timeSlotRepository: TimeSlotRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TimeSlotConfigState())
    val state: StateFlow<TimeSlotConfigState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val slots = timeSlotRepository.getAllTimeSlots().first()
                val grouped = slots.groupBy { it.dayOfWeek }
                val dayConfigs = (1..7).map { day ->
                    val daySlots = grouped[day] ?: emptyList()
                    DayConfig(
                        dayOfWeek = day,
                        isWorkingDay = daySlots.any { it.isActive },
                        periods = daySlots.sortedBy { it.periodNumber }.map { slot ->
                            PeriodModel(
                                id = slot.id,
                                periodNumber = slot.periodNumber,
                                startTime = slot.startTime,
                                endTime = slot.endTime,
                                type = slot.type
                            )
                        }
                    )
                }
                _state.value = _state.value.copy(dayConfigs = dayConfigs, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load data")
            }
        }
    }

    fun toggleDay(dayOfWeek: Int) {
        val updated = _state.value.dayConfigs.map { day ->
            if (day.dayOfWeek == dayOfWeek) day.copy(isWorkingDay = !day.isWorkingDay)
            else day
        }
        _state.value = _state.value.copy(dayConfigs = updated, error = null)
    }

    fun addPeriod(dayOfWeek: Int) {
        val updated = _state.value.dayConfigs.map { day ->
            if (day.dayOfWeek == dayOfWeek) {
                val maxPeriod = day.periods.maxOfOrNull { it.periodNumber } ?: 0
                day.copy(
                    isWorkingDay = true,
                    periods = day.periods + PeriodModel(
                        periodNumber = maxPeriod + 1,
                        startTime = "09:00",
                        endTime = "10:00"
                    )
                )
            } else day
        }
        _state.value = _state.value.copy(dayConfigs = updated, error = null)
    }

    fun removePeriod(dayOfWeek: Int, index: Int) {
        val updated = _state.value.dayConfigs.map { day ->
            if (day.dayOfWeek == dayOfWeek) {
                val newPeriods = day.periods.toMutableList().apply { removeAt(index) }
                    .mapIndexed { i, p -> p.copy(periodNumber = i + 1) }
                day.copy(periods = newPeriods)
            } else day
        }
        _state.value = _state.value.copy(dayConfigs = updated, error = null)
    }

    fun updatePeriodStartTime(dayOfWeek: Int, index: Int, value: String) {
        updatePeriodField(dayOfWeek, index) { it.copy(startTime = value) }
    }

    fun updatePeriodEndTime(dayOfWeek: Int, index: Int, value: String) {
        updatePeriodField(dayOfWeek, index) { it.copy(endTime = value) }
    }

    fun updatePeriodType(dayOfWeek: Int, index: Int, type: SlotType) {
        updatePeriodField(dayOfWeek, index) { it.copy(type = type) }
    }

    private fun updatePeriodField(dayOfWeek: Int, index: Int, transform: (PeriodModel) -> PeriodModel) {
        val updated = _state.value.dayConfigs.map { day ->
            if (day.dayOfWeek == dayOfWeek) {
                val newPeriods = day.periods.toMutableList().apply {
                    if (index in indices) set(index, transform(this[index]))
                }
                day.copy(periods = newPeriods)
            } else day
        }
        _state.value = _state.value.copy(dayConfigs = updated, error = null)
    }

    fun save() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            try {
                val existingSlots = timeSlotRepository.getAllTimeSlots().first()
                val existingMap = existingSlots.groupBy { it.dayOfWeek }
                val toDelete = mutableListOf<TimeSlotEntity>()
                val toUpdate = mutableListOf<TimeSlotEntity>()
                val toInsert = mutableListOf<TimeSlotEntity>()

                for (dayConfig in _state.value.dayConfigs) {
                    val existingDaySlots = existingMap[dayConfig.dayOfWeek] ?: emptyList()
                    val uiPeriods = dayConfig.periods
                    val uiIds = uiPeriods.map { it.id }.filter { it > 0 }.toSet()

                    existingDaySlots.filter { it.id !in uiIds }.forEach { toDelete.add(it) }

                    for (period in uiPeriods) {
                        val entity = TimeSlotEntity(
                            id = period.id,
                            dayOfWeek = dayConfig.dayOfWeek,
                            periodNumber = period.periodNumber,
                            startTime = period.startTime,
                            endTime = period.endTime,
                            type = period.type,
                            isActive = dayConfig.isWorkingDay
                        )
                        if (period.id == 0L) {
                            toInsert.add(entity)
                        } else {
                            toUpdate.add(entity)
                        }
                    }
                }

                toDelete.forEach { timeSlotRepository.delete(it) }
                toUpdate.forEach { timeSlotRepository.update(it) }
                toInsert.forEach { timeSlotRepository.insert(it) }

                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Failed to save")
            }
        }
    }
}
