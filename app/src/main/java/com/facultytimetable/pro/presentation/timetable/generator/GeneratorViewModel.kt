package com.facultytimetable.pro.presentation.timetable.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.RoomType
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.model.ConflictReport
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.domain.repository.SectionRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import com.facultytimetable.pro.domain.repository.TimeSlotRepository
import com.facultytimetable.pro.domain.repository.TimetableRepository
import com.facultytimetable.pro.domain.usecase.timetable.AssignmentPlanner
import com.facultytimetable.pro.domain.usecase.timetable.GenerationProgress
import com.facultytimetable.pro.domain.usecase.timetable.TeachingAssignment
import com.facultytimetable.pro.domain.usecase.timetable.TimetableGenerator
import com.facultytimetable.pro.domain.usecase.timetable.requiredPeriods
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class GeneratorState(
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val progress: GenerationProgress = GenerationProgress(),
    val result: String? = null,
    val conflicts: List<ConflictReport> = emptyList(),
    val isSuccess: Boolean = false,
    val error: String? = null,
    val sections: List<SectionEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val faculty: List<FacultyEntity> = emptyList(),
    val rooms: List<RoomEntity> = emptyList(),
    val timeSlots: List<TimeSlotEntity> = emptyList(),
    val departments: List<DepartmentEntity> = emptyList(),
    val previewAssignments: List<TeachingAssignment> = emptyList(),
    val dataIssues: List<String> = emptyList(),
    val savedPeriods: Int = 0,
    val savedSections: Int = 0,
    val savedLabs: Int = 0,
    val saved: Boolean = false
) {
    val regularPeriodsPerWeek: Int get() = timeSlots.count { it.type.name == "REGULAR" }
    val requiredPeriods: Int get() = previewAssignments.sumOf { it.subject.requiredPeriods }
    val labRooms: Int get() = rooms.count { it.type == RoomType.LAB }
    val canGenerate: Boolean
        get() = !isGenerating && sections.isNotEmpty() && subjects.isNotEmpty() &&
            faculty.isNotEmpty() && rooms.isNotEmpty() && regularPeriodsPerWeek > 0
}

@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val sectionRepository: SectionRepository,
    private val subjectRepository: SubjectRepository,
    private val facultyRepository: FacultyRepository,
    private val roomRepository: RoomRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val departmentRepository: DepartmentRepository,
    private val timetableRepository: TimetableRepository,
    private val planner: AssignmentPlanner,
    private val generator: TimetableGenerator
) : ViewModel() {

    private val _state = MutableStateFlow(GeneratorState())
    val state: StateFlow<GeneratorState> = _state

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val sections = sectionRepository.getActiveSections().first()
                val subjects = subjectRepository.getActiveSubjects().first()
                val faculty = facultyRepository.getActiveFaculty().first()
                val rooms = roomRepository.getActiveRooms().first()
                val timeSlots = timeSlotRepository.getActiveTimeSlots().first()
                val departments = departmentRepository.getActiveDepartments().first()

                val plan = planner.plan(sections, subjects, faculty, departments)
                val issues = mutableListOf<String>()
                if (sections.isEmpty()) issues += "No active sections. Add a section first."
                if (subjects.isEmpty()) issues += "No active subjects. Add subjects first."
                if (faculty.isEmpty()) issues += "No active faculty. Add faculty first."
                if (rooms.none { it.type == RoomType.CLASSROOM }) issues += "No classroom available."
                if (timeSlots.none { it.type.name == "REGULAR" }) issues += "No teaching periods configured."
                issues += plan.warnings

                _state.value = _state.value.copy(
                    sections = sections,
                    subjects = subjects,
                    faculty = faculty,
                    rooms = rooms,
                    timeSlots = timeSlots,
                    departments = departments,
                    previewAssignments = plan.assignments,
                    dataIssues = issues.distinct(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Could not load timetable data"
                )
            }
        }
    }

    fun generate() {
        val snapshot = _state.value
        if (!snapshot.canGenerate) {
            _state.value = snapshot.copy(
                error = "Add sections, subjects, faculty, rooms and teaching periods before generating."
            )
            return
        }

        viewModelScope.launch {
            _state.value = snapshot.copy(
                isGenerating = true,
                result = null,
                conflicts = emptyList(),
                isSuccess = false,
                error = null,
                saved = false,
                savedPeriods = 0,
                savedSections = 0,
                savedLabs = 0,
                progress = GenerationProgress(0, snapshot.previewAssignments.size, "Preparing…")
            )

            try {
                val generated = withContext(Dispatchers.Default) {
                    generator.generateBest(
                        assignments = snapshot.previewAssignments,
                        timeSlots = snapshot.timeSlots,
                        rooms = snapshot.rooms,
                        facultyList = snapshot.faculty,
                        attempts = 5
                    ) { progress ->
                        _state.value = _state.value.copy(progress = progress)
                    }
                }

                if (generated.entries.isEmpty()) {
                    _state.value = _state.value.copy(
                        isGenerating = false,
                        isSuccess = false,
                        result = "Nothing could be scheduled",
                        conflicts = generated.conflicts,
                        error = generated.conflicts.firstOrNull()?.message
                    )
                    return@launch
                }

                // The previous timetable is only replaced once a complete result has been
                // generated and saved atomically. A failure leaves the old timetable intact.
                timetableRepository.replaceAll(generated.entries)

                val sectionCount = generated.entries.map { it.sectionId }.distinct().size
                val labCount = generated.entries.count { entry ->
                    snapshot.subjects.firstOrNull { it.id == entry.subjectId }?.type?.name == "LAB"
                }
                val message = if (generated.conflicts.isEmpty()) {
                    "Timetable generated: ${generated.entries.size} periods across $sectionCount sections"
                } else {
                    "Timetable saved with ${generated.conflicts.size} warning(s): " +
                        "${generated.entries.size} periods across $sectionCount sections"
                }

                _state.value = _state.value.copy(
                    isGenerating = false,
                    isSuccess = generated.conflicts.isEmpty(),
                    result = message,
                    conflicts = generated.conflicts,
                    savedPeriods = generated.entries.size,
                    savedSections = sectionCount,
                    savedLabs = labCount,
                    saved = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isGenerating = false,
                    isSuccess = false,
                    error = "Could not save the timetable: ${e.message ?: "unknown error"}"
                )
            }
        }
    }

    fun dismissResult() {
        _state.value = _state.value.copy(
            result = null,
            conflicts = emptyList(),
            isSuccess = false,
            error = null,
            saved = false
        )
    }
}
