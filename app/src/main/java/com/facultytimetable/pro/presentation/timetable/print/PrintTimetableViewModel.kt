package com.facultytimetable.pro.presentation.timetable.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.domain.repository.DepartmentRepository
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

data class PrintTimetableState(
    val sections: List<SectionEntity> = emptyList(),
    val selectedSection: SectionEntity? = null,
    val entries: List<TimetableEntryEntity> = emptyList(),
    val timeSlots: List<TimeSlotEntity> = emptyList(),
    val subjects: Map<Long, SubjectEntity> = emptyMap(),
    val faculty: Map<Long, FacultyEntity> = emptyMap(),
    val rooms: Map<Long, RoomEntity> = emptyMap(),
    val departmentName: String = "",
    val semester: SemesterEntity? = null,
    val academicYear: AcademicYearEntity? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class PrintTimetableViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val sectionRepository: SectionRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val subjectRepository: SubjectRepository,
    private val facultyRepository: FacultyRepository,
    private val roomRepository: RoomRepository,
    private val departmentRepository: DepartmentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PrintTimetableState())
    val state: StateFlow<PrintTimetableState> = _state

    init { loadSections() }

    private fun loadSections() {
        viewModelScope.launch {
            val sections = sectionRepository.getActiveSections().first()
            _state.value = _state.value.copy(sections = sections, isLoading = false)
            sections.firstOrNull()?.let { onSectionSelected(it) }
        }
    }

    fun onSectionSelected(section: SectionEntity) {
        viewModelScope.launch {
            _state.value = _state.value.copy(selectedSection = section, isLoading = true)
            val timeSlots = timeSlotRepository.getActiveTimeSlots().first()
                .sortedWith(compareBy({ it.dayOfWeek }, { it.periodNumber }))
            val subjects = subjectRepository.getActiveSubjects().first().associateBy { it.id }
            val faculty = facultyRepository.getActiveFaculty().first().associateBy { it.id }
            val rooms = roomRepository.getActiveRooms().first().associateBy { it.id }
            val dept = departmentRepository.getDepartmentByIdFlow(section.departmentId).first()
            _state.value = _state.value.copy(
                timeSlots = timeSlots,
                subjects = subjects,
                faculty = faculty,
                rooms = rooms,
                departmentName = dept?.name ?: "",
                isLoading = false
            )
            loadEntries(section.id)
        }
    }

    private fun loadEntries(sectionId: Long) {
        viewModelScope.launch {
            timetableRepository.getEntriesBySection(sectionId).collect { entries ->
                _state.value = _state.value.copy(entries = entries)
            }
        }
    }
}
