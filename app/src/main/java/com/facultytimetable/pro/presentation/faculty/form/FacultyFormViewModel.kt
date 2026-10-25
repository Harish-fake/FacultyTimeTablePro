package com.facultytimetable.pro.presentation.faculty.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FacultyFormState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val designation: String = "",
    val departmentId: Long? = null,
    val qualification: String = "",
    val experience: String = "",
    val maxWeeklyHours: String = "24",
    val departments: List<DepartmentEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class FacultyFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val facultyRepository: FacultyRepository,
    private val departmentRepository: DepartmentRepository
) : ViewModel() {

    private val facultyId: Long? = savedStateHandle.get<Long>("facultyId")?.takeIf { it > 0 }

    private val existing = MutableStateFlow<FacultyEntity?>(null)
    private val _state = MutableStateFlow(FacultyFormState())
    val state: StateFlow<FacultyFormState> = _state

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val departments = departmentRepository.getActiveDepartments().first()

                if (facultyId != null) {
                    val faculty = facultyRepository.getFacultyById(facultyId)
                    if (faculty != null) {
                        existing.value = faculty
                        _state.value = _state.value.copy(
                            name = faculty.name,
                            email = faculty.email,
                            phone = faculty.phone,
                            designation = faculty.designation,
                            departmentId = faculty.departmentId,
                            qualification = faculty.qualification,
                            experience = faculty.experience.toString(),
                            maxWeeklyHours = faculty.maxWeeklyHours.toString(),
                            isEditing = true,
                            isLoading = false,
                            departments = departments
                        )
                        return@launch
                    }
                }

                _state.value = _state.value.copy(
                    isLoading = false,
                    departments = departments
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Could not load form data"
                )
            }
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value, error = null) }
    fun onEmailChange(value: String) { _state.value = _state.value.copy(email = value, error = null) }
    fun onPhoneChange(value: String) { _state.value = _state.value.copy(phone = value) }
    fun onDesignationChange(value: String) { _state.value = _state.value.copy(designation = value, error = null) }
    fun onDepartmentSelected(id: Long) { _state.value = _state.value.copy(departmentId = id, error = null) }
    fun onQualificationChange(value: String) { _state.value = _state.value.copy(qualification = value) }
    fun onExperienceChange(value: String) { _state.value = _state.value.copy(experience = value.filter { it.isDigit() }) }
    fun onMaxHoursChange(value: String) { _state.value = _state.value.copy(maxWeeklyHours = value.filter { it.isDigit() }) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Name is required")
            return
        }
        if (!Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(s.email.trim())) {
            _state.value = s.copy(error = "Enter a valid email address")
            return
        }
        if (s.designation.isBlank()) {
            _state.value = s.copy(error = "Designation is required")
            return
        }
        if (s.departmentId == null) {
            _state.value = s.copy(error = "Department is required")
            return
        }
        val maxHours = s.maxWeeklyHours.toIntOrNull()
        if (maxHours == null || maxHours !in 1..60) {
            _state.value = s.copy(error = "Max weekly hours must be between 1 and 60")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val normalizedEmail = s.email.trim().lowercase()
                val duplicate = facultyRepository.getAllFaculty().first()
                    .firstOrNull { it.email.equals(normalizedEmail, ignoreCase = true) && it.id != facultyId }
                if (duplicate != null) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = "That email is already used by ${duplicate.name}"
                    )
                    return@launch
                }

                val base = existing.value
                val entity = if (base == null) {
                    FacultyEntity(
                        name = s.name.trim(),
                        email = normalizedEmail,
                        phone = s.phone.trim(),
                        designation = s.designation.trim(),
                        departmentId = s.departmentId,
                        qualification = s.qualification.trim(),
                        experience = s.experience.toIntOrNull() ?: 0,
                        maxWeeklyHours = maxHours
                    )
                } else {
                    base.copy(
                        name = s.name.trim(),
                        email = normalizedEmail,
                        phone = s.phone.trim(),
                        designation = s.designation.trim(),
                        departmentId = s.departmentId,
                        qualification = s.qualification.trim(),
                        experience = s.experience.toIntOrNull() ?: 0,
                        maxWeeklyHours = maxHours,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                if (base == null) {
                    facultyRepository.insert(entity)
                } else {
                    facultyRepository.update(entity)
                }
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }
}
