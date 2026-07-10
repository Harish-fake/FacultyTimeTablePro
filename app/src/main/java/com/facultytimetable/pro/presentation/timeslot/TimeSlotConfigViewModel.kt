package com.facultytimetable.pro.presentation.timeslot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.dao.TimeSlotDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimeSlotConfigState(
    val timeSlots: Map<Int, List<TimeSlotEntity>> = emptyMap(),
    val selectedDay: Int = 1,
    val showAddDialog: Boolean = false,
    val showGenerateDialog: Boolean = false,
    val editingSlot: TimeSlotEntity? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class TimeSlotConfigViewModel @Inject constructor(
    private val timeSlotDao: TimeSlotDao
) : ViewModel() {

    private val _state = MutableStateFlow(TimeSlotConfigState())
    val state: StateFlow<TimeSlotConfigState> = _state

    init { loadTimeSlots() }

    private fun loadTimeSlots() {
        viewModelScope.launch {
            try {
                timeSlotDao.getAllTimeSlots().collect { slots ->
                    _state.value = _state.value.copy(
                        timeSlots = slots.groupBy { it.dayOfWeek },
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load time slots"
                )
            }
        }
    }

    fun selectDay(day: Int) {
        _state.value = _state.value.copy(selectedDay = day)
    }

    fun showAddDialog() {
        _state.value = _state.value.copy(
            showAddDialog = true,
            editingSlot = null
        )
    }

    fun showEditDialog(slot: TimeSlotEntity) {
        _state.value = _state.value.copy(
            showAddDialog = true,
            editingSlot = slot
        )
    }

    fun showGenerateDialog() {
        _state.value = _state.value.copy(showGenerateDialog = true)
    }

    fun dismissDialog() {
        _state.value = _state.value.copy(
            showAddDialog = false,
            showGenerateDialog = false,
            editingSlot = null
        )
    }

    fun saveSlot(
        dayOfWeek: Int,
        periodNumber: Int,
        periodName: String,
        startTime: String,
        endTime: String,
        type: SlotType
    ) {
        viewModelScope.launch {
            try {
                val existing = _state.value.editingSlot
                if (existing != null) {
                    timeSlotDao.update(
                        existing.copy(
                            periodNumber = periodNumber,
                            periodName = periodName,
                            startTime = startTime,
                            endTime = endTime,
                            type = type
                        )
                    )
                } else {
                    timeSlotDao.insert(
                        TimeSlotEntity(
                            dayOfWeek = dayOfWeek,
                            periodNumber = periodNumber,
                            periodName = periodName,
                            startTime = startTime,
                            endTime = endTime,
                            type = type
                        )
                    )
                }
                _state.value = _state.value.copy(
                    showAddDialog = false,
                    editingSlot = null,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to save slot"
                )
            }
        }
    }

    fun deleteSlot(slot: TimeSlotEntity) {
        viewModelScope.launch {
            try {
                timeSlotDao.delete(slot)
                _state.value = _state.value.copy(error = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to delete slot"
                )
            }
        }
    }

    fun generateDefaults(dayOfWeek: Int, periodCount: Int) {
        viewModelScope.launch {
            try {
                val existingSlots = _state.value.timeSlots[dayOfWeek].orEmpty()
                existingSlots.forEach { timeSlotDao.delete(it) }

                val defaultPeriods = mutableListOf<TimeSlotEntity>()
                val startHour = 9
                val durationMinutes = 55
                val breakPeriod = when (periodCount) {
                    6 -> 3
                    7 -> 3
                    8 -> 4
                    else -> 3
                }
                val lunchPeriod = when (periodCount) {
                    6 -> null
                    7 -> 4
                    8 -> 5
                    else -> null
                }

                var currentHour = startHour
                var currentMinute = 0
                for (i in 1..periodCount) {
                    val isBreak = i == breakPeriod
                    val isLunch = lunchPeriod != null && i == lunchPeriod
                    val type = when {
                        isBreak -> SlotType.BREAK
                        isLunch -> SlotType.LUNCH
                        else -> SlotType.REGULAR
                    }
                    val endHour = currentHour + (currentMinute + durationMinutes) / 60
                    val endMinute = (currentMinute + durationMinutes) % 60
                    val name = when (type) {
                        SlotType.REGULAR -> "Period $i"
                        SlotType.LUNCH -> "Lunch Break"
                        SlotType.BREAK -> "Short Break"
                    }
                    defaultPeriods.add(
                        TimeSlotEntity(
                            dayOfWeek = dayOfWeek,
                            periodNumber = i,
                            periodName = name,
                            startTime = String.format("%02d:%02d", currentHour, currentMinute),
                            endTime = String.format("%02d:%02d", endHour, endMinute),
                            type = type
                        )
                    )
                    currentHour = endHour
                    currentMinute = endMinute
                }
                timeSlotDao.insertAll(defaultPeriods)
                _state.value = _state.value.copy(
                    showGenerateDialog = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to generate defaults"
                )
            }
        }
    }

    fun setLunchSlot(dayOfWeek: Int, slotId: Long) {
        viewModelScope.launch {
            try {
                val existingLunch = timeSlotDao.getLunchSlotByDay(dayOfWeek)
                existingLunch?.let {
                    timeSlotDao.update(it.copy(type = SlotType.REGULAR))
                }
                val slot = timeSlotDao.getTimeSlotById(slotId)
                slot?.let {
                    timeSlotDao.update(it.copy(type = SlotType.LUNCH))
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to set lunch slot"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
