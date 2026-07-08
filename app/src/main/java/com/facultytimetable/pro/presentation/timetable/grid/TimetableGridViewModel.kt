package com.facultytimetable.pro.presentation.timetable.grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.domain.repository.SectionRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import com.facultytimetable.pro.domain.repository.TimetableRepository
import com.facultytimetable.pro.domain.repository.TimeSlotRepository
import com.facultytimetable.pro.presentation.theme.SubjectBreak
import com.facultytimetable.pro.presentation.theme.SubjectLab
import com.facultytimetable.pro.presentation.theme.SubjectLunch
import com.facultytimetable.pro.presentation.theme.SubjectProject
import com.facultytimetable.pro.presentation.theme.SubjectSeminar
import com.facultytimetable.pro.presentation.theme.SubjectSports
import com.facultytimetable.pro.presentation.theme.SubjectLibrary
import com.facultytimetable.pro.presentation.theme.SubjectTheory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.graphics.Color

data class TimetableGridState(
    val sections: List<SectionEntity> = emptyList(),
    val selectedSection: SectionEntity? = null,
    val entries: List<TimetableEntryEntity> = emptyList(),
    val timeSlots: List<TimeSlotEntity> = emptyList(),
    val subjects: Map<Long, SubjectEntity> = emptyMap(),
    val facultyMap: Map<Long, FacultyEntity> = emptyMap(),
    val rooms: Map<Long, RoomEntity> = emptyMap(),
    val isLoading: Boolean = true
)

data class ResolvedTimetableEntry(
    val entry: TimetableEntryEntity,
    val subjectName: String,
    val subjectCode: String,
    val subjectType: SubjectType,
    val facultyName: String,
    val roomName: String,
    val color: Color
)

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

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val sections = sectionRepository.getActiveSections().first()
            val timeSlots = timeSlotRepository.getActiveTimeSlots().first()
            val subjects = subjectRepository.getActiveSubjects().first().associateBy { it.id }
            val facultyMap = facultyRepository.getActiveFaculty().first().associateBy { it.id }
            val rooms = roomRepository.getActiveRooms().first().associateBy { it.id }

            _state.value = _state.value.copy(
                sections = sections,
                selectedSection = sections.firstOrNull(),
                timeSlots = timeSlots,
                subjects = subjects,
                facultyMap = facultyMap,
                rooms = rooms,
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
    fun resolveEntry(entry: TimetableEntryEntity): ResolvedTimetableEntry {
        val s = _state.value
        val subject = s.subjects[entry.subjectId]
        val faculty = s.facultyMap[entry.facultyId]
        val room = s.rooms[entry.roomId]

        val color = when (subject?.type) {
            SubjectType.LAB -> SubjectLab
            SubjectType.PROJECT -> SubjectProject
            SubjectType.SEMINAR -> SubjectSeminar
            SubjectType.LIBRARY -> SubjectLibrary
            SubjectType.SPORTS -> SubjectSports
            else -> SubjectTheory
        }

        return ResolvedTimetableEntry(
            entry = entry,
            subjectName = subject?.name ?: "Unknown",
            subjectCode = subject?.code ?: "",
            subjectType = subject?.type ?: SubjectType.THEORY,
            facultyName = faculty?.name ?: "Unknown",
            roomName = room?.name ?: "Unknown",
            color = color
        )
    }
}
