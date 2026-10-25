package com.facultytimetable.pro.presentation.subject

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.domain.repository.DepartmentRepository
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
    val hoursPerWeek: String = "4",
    val labHoursPerWeek: String = "0",
    val isLabRequired: Boolean = false,
    val departments: List<DepartmentEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SubjectFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val subjectRepository: SubjectRepository,
    private val departmentRepository: DepartmentRepository
) : ViewModel() {

    private val subjectId: Long? = savedStateHandle.get<Long>("subjectId")?.takeIf { it > 0 }
    private val existing = MutableStateFlow<SubjectEntity?>(null)
    private val _state = MutableStateFlow(SubjectFormState())
    val state: StateFlow<SubjectFormState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val departments = departmentRepository.getActiveDepartments().first()
                if (subjectId != null) {
                    val subj = subjectRepository.getSubjectById(subjectId)
                    if (subj != null) {
                        existing.value = subj
                        _state.value = _state.value.copy(
                            name = subj.name,
                            code = subj.code,
                            type = subj.type,
                            departmentId = subj.departmentId,
                            hoursPerWeek = subj.hoursPerWeek.toString(),
                            labHoursPerWeek = subj.labHoursPerWeek.toString(),
                            isLabRequired = subj.isLabRequired,
                            departments = departments,
                            isLoading = false
                        )
                        return@launch
                    }
                }
                _state.value = _state.value.copy(departments = departments, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Could not load form data"
                )
            }
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onCodeChange(v: String) { _state.value = _state.value.copy(code = v, error = null) }
    fun onTypeChange(t: SubjectType) {
        _state.value = _state.value.copy(
            type = t,
            isLabRequired = t == SubjectType.LAB,
            labHoursPerWeek = if (t == SubjectType.LAB && (_state.value.labHoursPerWeek.toIntOrNull() ?: 0) == 0) {
                _state.value.hoursPerWeek
            } else _state.value.labHoursPerWeek
        )
    }
    fun onDepartmentSelected(id: Long) { _state.value = _state.value.copy(departmentId = id, error = null) }
    fun onHoursChange(v: String) { _state.value = _state.value.copy(hoursPerWeek = v.filter { it.isDigit() }) }
    fun onLabHoursChange(v: String) { _state.value = _state.value.copy(labHoursPerWeek = v.filter { it.isDigit() }) }
    fun onLabRequiredChange(required: Boolean) {
        _state.value = _state.value.copy(isLabRequired = required)
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Name is required"); return }
        if (s.code.isBlank()) { _state.value = s.copy(error = "Code is required"); return }
        if (s.departmentId == null) { _state.value = s.copy(error = "Department is required"); return }
        val hours = s.hoursPerWeek.toIntOrNull()
        if (hours == null || hours !in 1..20) {
            _state.value = s.copy(error = "Hours per week must be between 1 and 20")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val base = existing.value
                val normalizedCode = s.code.trim().uppercase()
                val duplicate = subjectRepository.getSubjectByCode(normalizedCode)
                if (duplicate != null && duplicate.id != subjectId) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = "Subject code $normalizedCode is already in use"
                    )
                    return@launch
                }
                val labHours = s.labHoursPerWeek.toIntOrNull() ?: 0
                val entity = if (base == null) {
                    SubjectEntity(
                        name = s.name.trim(),
                        code = normalizedCode,
                        type = s.type,
                        departmentId = s.departmentId,
                        hoursPerWeek = hours,
                        labHoursPerWeek = if (s.isLabRequired) labHours else 0,
                        isLabRequired = s.isLabRequired
                    )
                } else {
                    base.copy(
                        name = s.name.trim(),
                        code = normalizedCode,
                        type = s.type,
                        departmentId = s.departmentId,
                        hoursPerWeek = hours,
                        labHoursPerWeek = if (s.isLabRequired) labHours else 0,
                        isLabRequired = s.isLabRequired,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                if (base == null) subjectRepository.insert(entity)
                else subjectRepository.update(entity)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }
}
