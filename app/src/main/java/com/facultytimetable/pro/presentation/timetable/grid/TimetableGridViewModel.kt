package com.facultytimetable.pro.presentation.timetable.grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.domain.repository.SectionRepository
import com.facultytimetable.pro.domain.repository.TimeSlotRepository
import com.facultytimetable.pro.domain.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimetableGridState(
    val sections: List<SectionEntity> = emptyList(),
    val selectedSection: SectionEntity? = null,
    val entries: List<TimetableEntryEntity> = emptyList(),
    val timeSlots: List<TimeSlotEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TimetableGridViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val sectionRepository: SectionRepository,
    private val timeSlotRepository: TimeSlotRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TimetableGridState())
    val state: StateFlow<TimetableGridState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val sections = sectionRepository.getActiveSections().first()
            val timeSlots = timeSlotRepository.getActiveTimeSlots().first()

            _state.value = _state.value.copy(
                sections = sections,
                selectedSection = sections.firstOrNull(),
                timeSlots = timeSlots,
                isLoading = false
            )

            sections.firstOrNull()?.let { loadEntries(it.id) }
        }
    }

    fun selectSection(section: SectionEntity) {
        _state.value = _state.value.copy(selectedSection = section)
        loadEntries(section.id)
    }

    private fun loadEntries(sectionId: Long) {
        viewModelScope.launch {
            timetableRepository.getEntriesBySection(sectionId).collect { entries ->
                _state.value = _state.value.copy(entries = entries)
            }
        }
    }

    fun deleteEntry(entry: TimetableEntryEntity) {
        viewModelScope.launch { timetableRepository.delete(entry) }
    }
}
