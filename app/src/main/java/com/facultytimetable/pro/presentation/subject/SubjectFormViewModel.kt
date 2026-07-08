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
    private val _state = MutableStateFlow(SubjectFormState())
    val state: StateFlow<SubjectFormState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val departments = departmentRepository.getActiveDepartments().first()
            if (subjectId != null) {
                val subj = subjectRepository.getSubjectById(subjectId)
                if (subj != null) {
                    _state.value = _state.value.copy(
                        name = subj.name, code = subj.code, type = subj.type,
                        departmentId = subj.departmentId, hoursPerWeek = subj.hoursPerWeek.toString(),
                        departments = departments, isLoading = false
                    )
                    return@launch
                }
            }
            _state.value = _state.value.copy(departments = departments, isLoading = false)
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onCodeChange(v: String) { _state.value = _state.value.copy(code = v) }
    fun onTypeChange(t: SubjectType) { _state.value = _state.value.copy(type = t) }
    fun onDepartmentSelected(id: Long) { _state.value = _state.value.copy(departmentId = id) }
    fun onHoursChange(v: String) { _state.value = _state.value.copy(hoursPerWeek = v) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Name is required"); return }
        if (s.code.isBlank()) { _state.value = s.copy(error = "Code is required"); return }
        if (s.departmentId == null) { _state.value = s.copy(error = "Department is required"); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val entity = SubjectEntity(
                    id = subjectId ?: 0, name = s.name, code = s.code.uppercase(),
                    type = s.type, departmentId = s.departmentId,
                    hoursPerWeek = s.hoursPerWeek.toIntOrNull() ?: 4
                )
                if (subjectId != null) subjectRepository.update(entity)
                else subjectRepository.insert(entity)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }
}
