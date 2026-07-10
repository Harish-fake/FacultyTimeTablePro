package com.facultytimetable.pro.presentation.timetable.faculty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.data.model.ConflictReport
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.domain.repository.SectionRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import com.facultytimetable.pro.domain.repository.TimetableRepository
import com.facultytimetable.pro.domain.repository.TimeSlotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FacultyTimetableState(
    val faculty: FacultyEntity? = null,
    val entries: List<TimetableEntryEntity> = emptyList(),
    val timeSlots: List<TimeSlotEntity> = emptyList(),
    val subjects: Map<Long, SubjectEntity> = emptyMap(),
    val sections: Map<Long, SectionEntity> = emptyMap(),
    val rooms: Map<Long, RoomEntity> = emptyMap(),
    val viewMode: FacultyViewMode = FacultyViewMode.WEEK,
    val isLoading: Boolean = true
)

enum class FacultyViewMode { TODAY, WEEK, SEMESTER, ACADEMIC_YEAR }

@HiltViewModel
class FacultyTimetableViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val subjectRepository: SubjectRepository,
    private val facultyRepository: FacultyRepository,
    private val sectionRepository: SectionRepository,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FacultyTimetableState())
    val state: StateFlow<FacultyTimetableState> = _state

    fun loadFaculty(facultyId: Long) {
        viewModelScope.launch {
            val faculty = facultyRepository.getFacultyByIdFlow(facultyId).first()
            val timeSlots = timeSlotRepository.getActiveTimeSlots().first()
                .sortedWith(compareBy({ it.dayOfWeek }, { it.periodNumber }))
            val subjects = subjectRepository.getActiveSubjects().first().associateBy { it.id }
            val sections = sectionRepository.getActiveSections().first().associateBy { it.id }
            val rooms = roomRepository.getActiveRooms().first().associateBy { it.id }

            _state.value = _state.value.copy(
                faculty = faculty,
                timeSlots = timeSlots,
                subjects = subjects,
                sections = sections,
                rooms = rooms,
                isLoading = false
            )

            faculty?.let { loadEntries(it.id) }
        }
    }

    private fun loadEntries(facultyId: Long) {
        viewModelScope.launch {
            timetableRepository.getEntriesByFaculty(facultyId).collect { entries ->
                _state.value = _state.value.copy(entries = entries)
            }
        }
    }

    fun setViewMode(mode: FacultyViewMode) {
        _state.value = _state.value.copy(viewMode = mode)
    }
}
