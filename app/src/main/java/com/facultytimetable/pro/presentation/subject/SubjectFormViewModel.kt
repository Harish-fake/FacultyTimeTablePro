package com.facultytimetable.pro.presentation.subject

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectFormState(
    val name: String = "",
    val code: String = "",
    val type: SubjectType = SubjectType.THEORY,
    val departmentId: Long? = null,
    val semesterId: Long? = null,
    val credits: String = "4",
    val hoursPerWeek: String = "4",
    val labHoursPerWeek: String = "0",
    val isLabRequired: Boolean = false,
    val isProject: Boolean = false,
    val isElective: Boolean = false,
    val colorHex: String = "#1976D2",
    val facultyId: Long? = null,
    val continuousHours: String = "1",
    val roomRequirement: String = "",
    val departments: List<DepartmentEntity> = emptyList(),
    val semesters: List<SemesterEntity> = emptyList(),
    val facultyList: List<FacultyEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SubjectFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val subjectRepository: SubjectRepository,
    private val departmentRepository: DepartmentRepository,
    private val semesterDao: SemesterDao,
    private val facultyRepository: FacultyRepository
) : ViewModel() {

    private val subjectId: Long? = savedStateHandle.get<Long>("subjectId")?.takeIf { it > 0 }

    private val _state = MutableStateFlow(SubjectFormState())
    val state: StateFlow<SubjectFormState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val departments = departmentRepository.getActiveDepartments().first()
            val semesters = semesterDao.getActiveSemesters().first()
            val facultyList = facultyRepository.getActiveFaculty().first()

            if (subjectId != null) {
                val subj = subjectRepository.getSubjectById(subjectId)
                if (subj != null) {
                    _state.value = _state.value.copy(
                        name = subj.name,
                        code = subj.code,
                        type = subj.type,
                        departmentId = subj.departmentId,
                        semesterId = subj.semesterId.takeIf { it > 0 },
                        credits = subj.credits.toString(),
                        hoursPerWeek = subj.hoursPerWeek.toString(),
                        labHoursPerWeek = subj.labHoursPerWeek.toString(),
                        isLabRequired = subj.isLabRequired,
                        isProject = subj.isProject,
                        isElective = subj.isElective,
                        colorHex = subj.colorHex.ifBlank { "#1976D2" },
                        facultyId = subj.facultyId.takeIf { it > 0 },
                        continuousHours = subj.continuousHours.toString(),
                        roomRequirement = subj.roomRequirement,
                        departments = departments,
                        semesters = semesters,
                        facultyList = facultyList,
                        isEditing = true,
                        isLoading = false
                    )
                    return@launch
                }
            }
            _state.value = _state.value.copy(
                departments = departments,
                semesters = semesters,
                facultyList = facultyList,
                isLoading = false
            )
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value, error = null) }
    fun onCodeChange(value: String) { _state.value = _state.value.copy(code = value, error = null) }
    fun onTypeChange(type: SubjectType) { _state.value = _state.value.copy(type = type, error = null) }
    fun onDepartmentSelected(id: Long) { _state.value = _state.value.copy(departmentId = id, error = null) }
    fun onSemesterSelected(semester: SemesterEntity) { _state.value = _state.value.copy(semesterId = semester.id) }
    fun onCreditsChange(value: String) { _state.value = _state.value.copy(credits = value.filter { it.isDigit() || it == '.' }) }
    fun onHoursPerWeekChange(value: String) { _state.value = _state.value.copy(hoursPerWeek = value.filter { it.isDigit() }) }
    fun onLabHoursPerWeekChange(value: String) { _state.value = _state.value.copy(labHoursPerWeek = value.filter { it.isDigit() }) }
    fun onLabRequiredChange(value: Boolean) { _state.value = _state.value.copy(isLabRequired = value) }
    fun onProjectChange(value: Boolean) { _state.value = _state.value.copy(isProject = value) }
    fun onElectiveChange(value: Boolean) { _state.value = _state.value.copy(isElective = value) }
    fun onColorHexChange(value: String) { _state.value = _state.value.copy(colorHex = value) }
    fun onFacultySelected(faculty: FacultyEntity) { _state.value = _state.value.copy(facultyId = faculty.id) }
    fun onContinuousHoursChange(value: String) { _state.value = _state.value.copy(continuousHours = value.filter { it.isDigit() }) }
    fun onRoomRequirementChange(value: String) { _state.value = _state.value.copy(roomRequirement = value) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Subject name is required"); return }
        if (s.code.isBlank()) { _state.value = s.copy(error = "Subject code is required"); return }
        if (s.departmentId == null) { _state.value = s.copy(error = "Department is required"); return }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val entity = SubjectEntity(
                    id = subjectId ?: 0,
                    name = s.name.trim(),
                    code = s.code.uppercase().trim(),
                    type = s.type,
                    departmentId = s.departmentId,
                    semesterId = s.semesterId ?: 0,
                    credits = s.credits.toIntOrNull() ?: 4,
                    hoursPerWeek = s.hoursPerWeek.toIntOrNull() ?: 4,
                    labHoursPerWeek = s.labHoursPerWeek.toIntOrNull() ?: 0,
                    isLabRequired = s.isLabRequired,
                    isProject = s.isProject,
                    isElective = s.isElective,
                    colorHex = s.colorHex,
                    facultyId = s.facultyId ?: 0,
                    continuousHours = s.continuousHours.toIntOrNull() ?: 1,
                    roomRequirement = s.roomRequirement.trim()
                )
                if (subjectId != null) subjectRepository.update(entity)
                else subjectRepository.insert(entity)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Failed to save subject")
            }
        }
    }
}
