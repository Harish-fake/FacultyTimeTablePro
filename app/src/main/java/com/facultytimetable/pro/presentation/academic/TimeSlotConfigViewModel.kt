package com.facultytimetable.pro.presentation.academic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.TimeSlotDao
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimeSlotConfigState(
    val allSlots: List<TimeSlotEntity> = emptyList(),
    val selectedDay: Int = 1,
    val isLoading: Boolean = true,
    val message: String? = null,
    val error: String? = null
) {
    val slotsForDay: List<TimeSlotEntity>
        get() = allSlots.filter { it.dayOfWeek == selectedDay }.sortedBy { it.periodNumber }
    val regularCount: Int get() = allSlots.count { it.type == SlotType.REGULAR && it.isActive }
    val activeDays: Int get() = allSlots.filter { it.isActive }.map { it.dayOfWeek }.distinct().size
}

@HiltViewModel
class TimeSlotConfigViewModel @Inject constructor(
    private val timeSlotDao: TimeSlotDao
) : ViewModel() {

    private val selectedDay = MutableStateFlow(1)
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<TimeSlotConfigState> = combine(
        timeSlotDao.getAllTimeSlots(),
        selectedDay,
        message,
        error
    ) { slots, day, msg, err ->
        TimeSlotConfigState(
            allSlots = slots,
            selectedDay = day,
            isLoading = false,
            message = msg,
            error = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimeSlotConfigState())

    fun selectDay(day: Int) { selectedDay.value = day }
    fun consumeMessage() { message.value = null }
    fun consumeError() { error.value = null }

    fun saveSlot(
        existing: TimeSlotEntity?,
        day: Int,
        period: String,
        start: String,
        end: String,
        type: SlotType,
        active: Boolean
    ) {
        val periodValue = period.toIntOrNull()
        if (periodValue == null || periodValue !in 1..20) {
            error.value = "Period number must be between 1 and 20"
            return
        }
        if (!isTime(start) || !isTime(end)) {
            error.value = "Use 24-hour times such as 08:30"
            return
        }
        if (end <= start) {
            error.value = "End time must be after the start time"
            return
        }
        val duplicate = state.value.allSlots.any {
            it.dayOfWeek == day && it.periodNumber == periodValue && it.id != existing?.id
        }
        if (duplicate) {
            error.value = "Period $periodValue already exists on this day"
            return
        }

        viewModelScope.launch {
            try {
                if (existing == null) {
                    timeSlotDao.insert(
                        TimeSlotEntity(
                            dayOfWeek = day,
                            periodNumber = periodValue,
                            startTime = start,
                            endTime = end,
                            type = type,
                            isActive = active
                        )
                    )
                } else {
                    timeSlotDao.update(
                        existing.copy(
                            dayOfWeek = day,
                            periodNumber = periodValue,
                            startTime = start,
                            endTime = end,
                            type = type,
                            isActive = active
                        )
                    )
                }
                message.value = if (existing == null) "Time slot added" else "Time slot updated"
            } catch (e: Exception) {
                error.value = e.message ?: "Could not save time slot"
            }
        }
    }

    fun toggleActive(slot: TimeSlotEntity, active: Boolean) {
        viewModelScope.launch {
            try {
                timeSlotDao.update(slot.copy(isActive = active))
                message.value = if (active) "Period enabled" else "Period disabled"
            } catch (e: Exception) {
                error.value = e.message ?: "Could not update period"
            }
        }
    }

    fun deleteSlot(slot: TimeSlotEntity) {
        viewModelScope.launch {
            try {
                timeSlotDao.delete(slot)
                message.value = "Time slot deleted"
            } catch (e: Exception) {
                error.value = "This period is used by the timetable. Remove those classes first."
            }
        }
    }

    private fun isTime(value: String): Boolean =
        Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(value.trim())
}

val dayLabels = mapOf(
    1 to "Monday",
    2 to "Tuesday",
    3 to "Wednesday",
    4 to "Thursday",
    5 to "Friday",
    6 to "Saturday",
    7 to "Sunday"
)

fun dayShortLabel(day: Int): String = when (day) {
    1 -> "Mon"
    2 -> "Tue"
    3 -> "Wed"
    4 -> "Thu"
    5 -> "Fri"
    6 -> "Sat"
    else -> "Sun"
}
