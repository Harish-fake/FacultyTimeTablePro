package com.facultytimetable.pro.presentation.timetable.grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.RoomType
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.data.model.ConflictReport
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.domain.repository.SectionRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import com.facultytimetable.pro.domain.repository.TimetableRepository
import com.facultytimetable.pro.domain.repository.TimeSlotRepository
import com.facultytimetable.pro.domain.usecase.timetable.ConflictEngine
import com.facultytimetable.pro.presentation.theme.SubjectBreak
import com.facultytimetable.pro.presentation.theme.SubjectLab
import com.facultytimetable.pro.presentation.theme.SubjectLibrary
import com.facultytimetable.pro.presentation.theme.SubjectLunch
import com.facultytimetable.pro.presentation.theme.SubjectProject
import com.facultytimetable.pro.presentation.theme.SubjectSeminar
import com.facultytimetable.pro.presentation.theme.SubjectSports
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
    val availableSubjects: List<SubjectEntity> = emptyList(),
    val availableFaculty: List<FacultyEntity> = emptyList(),
    val availableRooms: List<RoomEntity> = emptyList(),
    val showEditSheet: Boolean = false,
    val editingEntry: TimetableEntryEntity? = null,
    val selectedDay: Int = 1,
    val selectedTimeSlot: TimeSlotEntity? = null,
    val validationErrors: List<ConflictReport> = emptyList(),
    val snackbarMessage: String? = null,
    val isLoading: Boolean = true
)

data class ResolvedTimetableEntry(
    val entry: TimetableEntryEntity,
    val subjectName: String,
    val subjectCode: String,
    val subjectType: SubjectType,
    val facultyName: String,
    val facultyInitials: String,
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
    private val roomRepository: RoomRepository,
    private val departmentRepository: DepartmentRepository,
    private val conflictEngine: ConflictEngine
) : ViewModel() {

    private val _state = MutableStateFlow(TimetableGridState())
    val state: StateFlow<TimetableGridState> = _state

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val sections = sectionRepository.getActiveSections().first()
            val timeSlots = timeSlotRepository.getActiveTimeSlots().first()
                .sortedWith(compareBy({ it.dayOfWeek }, { it.periodNumber }))
            val subjects = subjectRepository.getActiveSubjects().first().associateBy { it.id }
            val facultyMap = facultyRepository.getActiveFaculty().first().associateBy { it.id }
            val rooms = roomRepository.getActiveRooms().first().associateBy { it.id }

            _state.value = _state.value.copy(
                sections = sections,
                timeSlots = timeSlots,
                subjects = subjects,
                facultyMap = facultyMap,
                rooms = rooms,
                isLoading = false
            )

            sections.firstOrNull()?.let { onSectionSelected(it) }
        }
    }

    fun onSectionSelected(section: SectionEntity) {
        _state.value = _state.value.copy(selectedSection = section)
        loadEntries(section.id)
        loadFilteredData(section)
    }

    private fun loadEntries(sectionId: Long) {
        viewModelScope.launch {
            timetableRepository.getEntriesBySection(sectionId).collect { entries ->
                _state.value = _state.value.copy(entries = entries)
            }
        }
    }

    private fun loadFilteredData(section: SectionEntity) {
        viewModelScope.launch {
            val allSubjects = subjectRepository.getActiveSubjects().first()
            val allFaculty = facultyRepository.getActiveFaculty().first()
            val allRooms = roomRepository.getActiveRooms().first()
            _state.value = _state.value.copy(
                availableSubjects = allSubjects,
                availableFaculty = allFaculty,
                availableRooms = allRooms
            )
        }
    }

    fun showAddSheet(day: Int, timeSlot: TimeSlotEntity) {
        val section = _state.value.selectedSection ?: return
        val newEntry = TimetableEntryEntity(
            sectionId = section.id,
            subjectId = 0,
            facultyId = 0,
            roomId = 0,
            timeSlotId = timeSlot.id,
            dayOfWeek = day
        )
        _state.value = _state.value.copy(
            showEditSheet = true,
            editingEntry = newEntry,
            selectedDay = day,
            selectedTimeSlot = timeSlot,
            validationErrors = emptyList()
        )
    }

    fun showEditSheet(entry: TimetableEntryEntity) {
        val slot = _state.value.timeSlots.find { it.id == entry.timeSlotId }
        _state.value = _state.value.copy(
            showEditSheet = true,
            editingEntry = entry,
            selectedDay = entry.dayOfWeek,
            selectedTimeSlot = slot,
            validationErrors = emptyList()
        )
    }

    fun dismissSheet() {
        _state.value = _state.value.copy(
            showEditSheet = false,
            editingEntry = null,
            selectedTimeSlot = null,
            validationErrors = emptyList()
        )
    }

    fun updateEditingSubject(subjectId: Long) {
        _state.value = _state.value.copy(
            editingEntry = _state.value.editingEntry?.copy(subjectId = subjectId)
        )
    }

    fun updateEditingFaculty(facultyId: Long) {
        _state.value = _state.value.copy(
            editingEntry = _state.value.editingEntry?.copy(facultyId = facultyId)
        )
    }

    fun updateEditingRoom(roomId: Long) {
        _state.value = _state.value.copy(
            editingEntry = _state.value.editingEntry?.copy(roomId = roomId)
        )
    }

    fun addEntry() {
        val entry = _state.value.editingEntry ?: return
        if (!validateEntry(entry, _state.value.entries)) return
        viewModelScope.launch {
            timetableRepository.insert(entry)
            _state.value = _state.value.copy(
                snackbarMessage = "Entry added",
                showEditSheet = false,
                editingEntry = null,
                validationErrors = emptyList()
            )
        }
    }

    fun updateEntry() {
        val entry = _state.value.editingEntry ?: return
        val existingEntries = _state.value.entries.filter { it.id != entry.id }
        if (!validateEntry(entry, existingEntries)) return
        viewModelScope.launch {
            timetableRepository.update(entry)
            _state.value = _state.value.copy(
                snackbarMessage = "Entry updated",
                showEditSheet = false,
                editingEntry = null,
                validationErrors = emptyList()
            )
        }
    }

    fun deleteEntry(entry: TimetableEntryEntity) {
        viewModelScope.launch {
            timetableRepository.delete(entry)
            _state.value = _state.value.copy(
                snackbarMessage = "Entry deleted",
                showEditSheet = false,
                editingEntry = null,
                validationErrors = emptyList()
            )
        }
    }

    fun validateEntry(
        entry: TimetableEntryEntity,
        existingEntries: List<TimetableEntryEntity>
    ): Boolean {
        val result = conflictEngine.validateEntry(entry, existingEntries)
        _state.value = _state.value.copy(validationErrors = result.conflicts)
        return !result.hasConflicts
    }

    fun clearSnackbar() {
        _state.value = _state.value.copy(snackbarMessage = null)
    }

    fun getDepartmentSubjects(departmentId: Long): List<SubjectEntity> {
        return _state.value.availableSubjects.filter { it.departmentId == departmentId }
    }

    fun getFacultyByDepartment(departmentId: Long): List<FacultyEntity> {
        return _state.value.availableFaculty.filter { it.departmentId == departmentId }
    }

    fun getRoomsByType(type: RoomType): List<RoomEntity> {
        return _state.value.availableRooms.filter { it.type == type }
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

        val initials = faculty?.let {
            it.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { w ->
                w.first().uppercase()
            }
        } ?: "?"

        return ResolvedTimetableEntry(
            entry = entry,
            subjectName = subject?.name ?: "Unknown",
            subjectCode = subject?.code ?: "",
            subjectType = subject?.type ?: SubjectType.THEORY,
            facultyName = faculty?.name ?: "Unknown",
            facultyInitials = initials,
            roomName = room?.name ?: "Unknown",
            color = color
        )
    }
}
