package com.facultytimetable.pro.presentation.timetable.faculty

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.domain.repository.SectionRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import com.facultytimetable.pro.domain.repository.TimeSlotRepository
import com.facultytimetable.pro.domain.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FacultyTimetableState(
    val faculty: FacultyEntity? = null,
    val entries: List<TimetableEntryEntity> = emptyList(),
    val timeSlots: List<TimeSlotEntity> = emptyList(),
    val subjects: Map<Long, SubjectEntity> = emptyMap(),
    val rooms: Map<Long, RoomEntity> = emptyMap(),
    val sections: Map<Long, SectionEntity> = emptyMap(),
    val isLoading: Boolean = true
) {
    val days: List<Int> get() = timeSlots.map { it.dayOfWeek }.distinct().sorted()
    val weeklyPeriods: Int get() = entries.size
}

@HiltViewModel
class FacultyTimetableViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val facultyRepository: FacultyRepository,
    private val timetableRepository: TimetableRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val subjectRepository: SubjectRepository,
    private val roomRepository: RoomRepository,
    private val sectionRepository: SectionRepository
) : ViewModel() {

    private val facultyId: Long = savedStateHandle["facultyId"] ?: -1L
    private val _state = MutableStateFlow(FacultyTimetableState())
    val state: StateFlow<FacultyTimetableState> = _state

    init {
        viewModelScope.launch {
            facultyRepository.getFacultyByIdFlow(facultyId).collect { faculty ->
                _state.update { it.copy(faculty = faculty, isLoading = false) }
            }
        }

        viewModelScope.launch {
            timetableRepository.getEntriesByFaculty(facultyId).collect { entries ->
                _state.update { it.copy(entries = entries) }
            }
        }

        viewModelScope.launch {
            combine(
                timeSlotRepository.getActiveTimeSlots(),
                subjectRepository.getActiveSubjects(),
                roomRepository.getActiveRooms(),
                sectionRepository.getActiveSections()
            ) { slots, subjects, rooms, sections ->
                Maps(slots, subjects.associateBy { it.id }, rooms.associateBy { it.id }, sections.associateBy { it.id })
            }.collect { maps ->
                _state.update {
                    it.copy(
                        timeSlots = maps.slots,
                        subjects = maps.subjects,
                        rooms = maps.rooms,
                        sections = maps.sections
                    )
                }
            }
        }
    }

    private data class Maps(
        val slots: List<TimeSlotEntity>,
        val subjects: Map<Long, SubjectEntity>,
        val rooms: Map<Long, RoomEntity>,
        val sections: Map<Long, SectionEntity>
    )
}
