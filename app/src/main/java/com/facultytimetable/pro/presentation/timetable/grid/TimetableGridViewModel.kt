package com.facultytimetable.pro.presentation.timetable.grid

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimetableGridState(
    val sections: List<SectionEntity> = emptyList(),
    val selectedSection: SectionEntity? = null,
    val entries: List<TimetableEntryEntity> = emptyList(),
    val timeSlots: List<TimeSlotEntity> = emptyList(),
    val subjects: Map<Long, SubjectEntity> = emptyMap(),
    val facultyMap: Map<Long, FacultyEntity> = emptyMap(),
    val rooms: Map<Long, RoomEntity> = emptyMap(),
    val isLoading: Boolean = true,
    val message: String? = null
) {
    val dailySlots: List<TimeSlotEntity>
        get() = timeSlots.distinctBy { it.periodNumber }.sortedBy { it.periodNumber }

    val weeklyPeriods: Int get() = entries.size
    val labPeriods: Int get() = entries.count { subjects[it.subjectId]?.type?.name == "LAB" }
    val days: List<Int> get() = timeSlots.map { it.dayOfWeek }.distinct().sorted()
}

@HiltViewModel
class TimetableGridViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val sectionRepository: SectionRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val subjectRepository: SubjectRepository,
    private val facultyRepository: FacultyRepository,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TimetableGridState())
    val state: StateFlow<TimetableGridState> = _state

    private var entriesJob: Job? = null
    private var observedSectionId: Long? = null

    init {
        observeSections()
        observeReferenceData()
    }

    private fun observeSections() {
        viewModelScope.launch {
            sectionRepository.getActiveSections().collect { sections ->
                val current = _state.value.selectedSection
                val selected = when {
                    current == null -> sections.firstOrNull()
                    sections.any { it.id == current.id } -> sections.first { it.id == current.id }
                    else -> sections.firstOrNull()
                }
                _state.update {
                    it.copy(
                        sections = sections,
                        selectedSection = selected,
                        isLoading = false
                    )
                }
                if (selected != null && observedSectionId != selected.id) {
                    observeEntries(selected.id)
                }
            }
        }
    }

    private fun observeReferenceData() {
        viewModelScope.launch {
            combine(
                subjectRepository.getActiveSubjects(),
                facultyRepository.getActiveFaculty(),
                roomRepository.getActiveRooms(),
                timeSlotRepository.getActiveTimeSlots()
            ) { subjects, faculty, rooms, slots ->
                ReferenceData(
                    subjects = subjects.associateBy { it.id },
                    faculty = faculty.associateBy { it.id },
                    rooms = rooms.associateBy { it.id },
                    slots = slots
                )
            }.collect { data ->
                _state.update {
                    it.copy(
                        subjects = data.subjects,
                        facultyMap = data.faculty,
                        rooms = data.rooms,
                        timeSlots = data.slots
                    )
                }
            }
        }
    }

    private fun observeEntries(sectionId: Long) {
        observedSectionId = sectionId
        entriesJob?.cancel()
        entriesJob = viewModelScope.launch {
            timetableRepository.getEntriesBySection(sectionId).collect { entries ->
                _state.update { it.copy(entries = entries) }
            }
        }
    }

    fun selectSection(section: SectionEntity) {
        if (_state.value.selectedSection?.id == section.id) return
        _state.update { it.copy(selectedSection = section, entries = emptyList()) }
        observeEntries(section.id)
    }

    fun selectSectionById(sectionId: Long) {
        _state.value.sections.firstOrNull { it.id == sectionId }?.let(::selectSection)
    }

    fun deleteEntry(entry: TimetableEntryEntity) {
        viewModelScope.launch {
            runCatching { timetableRepository.delete(entry) }
                .onSuccess {
                    _state.update { it.copy(message = "Period removed from the timetable") }
                }
                .onFailure { error ->
                    _state.update { it.copy(message = "Could not remove period: ${error.message}") }
                }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private data class ReferenceData(
        val subjects: Map<Long, SubjectEntity>,
        val faculty: Map<Long, FacultyEntity>,
        val rooms: Map<Long, RoomEntity>,
        val slots: List<TimeSlotEntity>
    )
}
