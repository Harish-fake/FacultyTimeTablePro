package com.facultytimetable.pro.presentation.assignment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyAssignmentEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyAssignmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssignmentWithDetails(
    val assignment: FacultyAssignmentEntity,
    val subjectName: String = "",
    val facultyName: String = ""
)

data class FacultyWorkload(
    val facultyId: Long,
    val facultyName: String,
    val totalHours: Int,
    val maxHours: Int
)

data class FacultyAssignmentState(
    val departments: List<DepartmentEntity> = emptyList(),
    val selectedDepartmentId: Long? = null,
    val semesters: List<SemesterEntity> = emptyList(),
    val selectedSemesterId: Long? = null,
    val subjects: List<SubjectEntity> = emptyList(),
    val selectedSubjectId: Long? = null,
    val faculty: List<FacultyEntity> = emptyList(),
    val selectedFacultyId: Long? = null,
    val hoursPerWeek: String = "",
    val existingAssignments: List<AssignmentWithDetails> = emptyList(),
    val workload: FacultyWorkload? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class FacultyAssignmentViewModel @Inject constructor(
    private val departmentRepository: DepartmentRepository,
    private val semesterDao: SemesterDao,
    private val subjectRepository: SubjectRepository,
    private val facultyRepository: FacultyRepository,
    private val assignmentRepository: FacultyAssignmentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FacultyAssignmentState())

    val state: StateFlow<FacultyAssignmentState> = combine(
        departmentRepository.getAllDepartments(),
        _state
    ) { departments, s ->
        s.copy(departments = departments, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FacultyAssignmentState())

    init { loadDepartments() }

    private fun loadDepartments() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            departmentRepository.getAllDepartments()
        }
    }

    fun onDepartmentSelected(departmentId: Long) {
        _state.value = _state.value.copy(
            selectedDepartmentId = departmentId,
            selectedSemesterId = null,
            selectedSubjectId = null,
            selectedFacultyId = null,
            hoursPerWeek = "",
            workload = null
        )
        viewModelScope.launch {
            semesterDao.getAllSemesters().collect { semesters ->
                val filtered = semesters.filter {
                    it.departmentIds.isBlank() || it.departmentIds.split(",").any { id ->
                        id.trim().toLongOrNull() == departmentId
                    }
                }
                _state.value = _state.value.copy(semesters = filtered)
            }
        }
        viewModelScope.launch {
            subjectRepository.getSubjectsByDepartment(departmentId).collect { subjects ->
                _state.value = _state.value.copy(subjects = subjects)
            }
        }
        viewModelScope.launch {
            facultyRepository.getActiveFacultyByDepartment(departmentId).collect { faculty ->
                _state.value = _state.value.copy(faculty = faculty)
            }
        }
    }

    fun onSemesterSelected(semesterId: Long) {
        _state.value = _state.value.copy(
            selectedSemesterId = semesterId,
            selectedSubjectId = null,
            selectedFacultyId = null,
            hoursPerWeek = ""
        )
        loadAssignments()
    }

    fun onSubjectSelected(subjectId: Long) {
        _state.value = _state.value.copy(selectedSubjectId = subjectId)
        val s = _state.value
        val subject = s.subjects.find { it.id == subjectId }
        if (subject != null) {
            _state.value = _state.value.copy(hoursPerWeek = subject.hoursPerWeek.toString())
        }
    }

    fun onFacultySelected(facultyId: Long) {
        _state.value = _state.value.copy(selectedFacultyId = facultyId)
        recalculateWorkload(facultyId)
    }

    fun onHoursChange(v: String) {
        _state.value = _state.value.copy(hoursPerWeek = v.filter { it.isDigit() })
    }

    private fun recalculateWorkload(facultyId: Long) {
        val s = _state.value
        if (s.selectedSemesterId == null) return
        viewModelScope.launch {
            val totalHours = assignmentRepository.getTotalHoursByFacultyAndSemester(facultyId, s.selectedSemesterId)
            val faculty = s.faculty.find { it.id == facultyId }
            _state.value = _state.value.copy(
                workload = FacultyWorkload(
                    facultyId = facultyId,
                    facultyName = faculty?.name ?: "",
                    totalHours = totalHours + (s.hoursPerWeek.toIntOrNull() ?: 0),
                    maxHours = faculty?.maxWeeklyHours ?: 24
                )
            )
        }
    }

    private fun loadAssignments() {
        val s = _state.value
        val deptId = s.selectedDepartmentId ?: return
        val semId = s.selectedSemesterId ?: return
        viewModelScope.launch {
            assignmentRepository.getAssignmentsByDeptAndSemester(deptId, semId).collect { assignments ->
                val depts = s.departments
                val subjects = s.subjects
                val faculty = s.faculty
                val details = assignments.map { assignment ->
                    AssignmentWithDetails(
                        assignment = assignment,
                        subjectName = subjects.find { it.id == assignment.subjectId }?.name ?: "",
                        facultyName = faculty.find { it.id == assignment.facultyId }?.name ?: ""
                    )
                }
                _state.value = _state.value.copy(existingAssignments = details)
            }
        }
    }

    fun save() {
        val s = _state.value
        if (s.selectedDepartmentId == null) { _state.value = s.copy(error = "Select a department"); return }
        if (s.selectedSemesterId == null) { _state.value = s.copy(error = "Select a semester"); return }
        if (s.selectedSubjectId == null) { _state.value = s.copy(error = "Select a subject"); return }
        if (s.selectedFacultyId == null) { _state.value = s.copy(error = "Select a faculty member"); return }
        val hours = s.hoursPerWeek.toIntOrNull()
        if (hours == null || hours <= 0) { _state.value = s.copy(error = "Enter valid hours"); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            try {
                val entity = FacultyAssignmentEntity(
                    departmentId = s.selectedDepartmentId,
                    semesterId = s.selectedSemesterId,
                    subjectId = s.selectedSubjectId,
                    facultyId = s.selectedFacultyId,
                    hoursPerWeek = hours
                )
                assignmentRepository.insert(entity)
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    selectedSubjectId = null,
                    selectedFacultyId = null,
                    hoursPerWeek = "",
                    workload = null,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }

    fun deleteAssignment(assignment: FacultyAssignmentEntity) {
        viewModelScope.launch {
            assignmentRepository.delete(assignment)
            val s = _state.value
            if (s.selectedFacultyId != null) recalculateWorkload(s.selectedFacultyId)
        }
    }

    fun clearSuccess() { _state.value = _state.value.copy(saveSuccess = false) }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
