package com.facultytimetable.pro.presentation.timetable.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.model.ConflictReport
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.domain.repository.SectionRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import com.facultytimetable.pro.domain.repository.TimetableRepository
import com.facultytimetable.pro.domain.repository.TimeSlotRepository
import com.facultytimetable.pro.domain.usecase.timetable.GenerationProgress
import com.facultytimetable.pro.domain.usecase.timetable.TeachingAssignment
import com.facultytimetable.pro.domain.usecase.timetable.TimetableGenerator
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
    val timeSlots: List<TimeSlotEntity> = emptyList()
)

@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val sectionRepository: SectionRepository,
    private val subjectRepository: SubjectRepository,
    private val facultyRepository: FacultyRepository,
    private val roomRepository: RoomRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val timetableRepository: TimetableRepository,
    private val generator: TimetableGenerator
) : ViewModel() {

    private val _state = MutableStateFlow(GeneratorState())
    val state: StateFlow<GeneratorState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val sections = sectionRepository.getActiveSections().first()
            val subjects = subjectRepository.getActiveSubjects().first()
            val faculty = facultyRepository.getActiveFaculty().first()
            val rooms = roomRepository.getActiveRooms().first()
            val timeSlots = timeSlotRepository.getActiveTimeSlots().first()

            _state.value = _state.value.copy(
                sections = sections,
                subjects = subjects,
                faculty = faculty,
                rooms = rooms,
                timeSlots = timeSlots,
                isLoading = false
            )
        }
    }

    fun generate() {
        val s = _state.value
        if (s.subjects.isEmpty() || s.faculty.isEmpty() || s.rooms.isEmpty() || s.sections.isEmpty()) {
            _state.value = s.copy(error = "Missing data: add faculty, subjects, rooms, and sections first")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isGenerating = true, result = null, conflicts = emptyList(), error = null)

            // Clear existing entries before generating
            timetableRepository.deleteAll()

            // Build teaching assignments with round-robin faculty distribution
            val assignments = mutableListOf<TeachingAssignment>()
            val facultyByDept = s.faculty.groupBy { it.departmentId }

            // Match sections to subjects by department prefix in section name
            // e.g. "CSE 3A" gets CSE subjects, "ECE 3A" gets ECE subjects
            val deptCodes = s.subjects.map { it.code.take(2) }.distinct()

            for (section in s.sections) {
                val sectionDept = deptCodes.firstOrNull { section.name.startsWith(it, ignoreCase = true) }
                for (subject in s.subjects) {
                    // Skip subject if section doesn't belong to its department (by code prefix match)
                    if (sectionDept != null && !subject.code.startsWith(sectionDept, ignoreCase = true)) continue

                    val deptFaculty = facultyByDept[subject.departmentId] ?: s.faculty
                    if (deptFaculty.isEmpty()) continue

                    val deptFacultyIds = deptFaculty.map { it.id }.toSet()
                    val existingForDept = assignments.count { it.faculty.id in deptFacultyIds }
                    val facultyForSubject = deptFaculty[existingForDept % deptFaculty.size]

                    assignments.add(
                        TeachingAssignment(
                            subject = subject,
                            section = section,
                            faculty = facultyForSubject
                        )
                    )
                }
            }

            val result = withContext(Dispatchers.Default) {
                generator.generate(
                    assignments = assignments,
                    timeSlots = s.timeSlots,
                    rooms = s.rooms,
                    facultyList = s.faculty,
                    onProgress = { progress ->
                        _state.value = _state.value.copy(progress = progress)
                    }
                )
            }

            if (result.success && result.entries.isNotEmpty()) {
                timetableRepository.insertAll(result.entries)

                _state.value = _state.value.copy(
                    isGenerating = false,
                    isSuccess = true,
                    result = "Successfully generated ${result.entries.size} timetable entries"
                )
            } else {
                _state.value = _state.value.copy(
                    isGenerating = false,
                    isSuccess = false,
                    result = "Generation completed with ${result.conflicts.size} conflict(s)",
                    conflicts = result.conflicts
                )
            }
        }
    }

    fun dismissResult() {
        _state.value = _state.value.copy(result = null, conflicts = emptyList(), isSuccess = false)
    }
}
