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

val DESIGNATIONS = listOf(
    "Professor",
    "Associate Professor",
    "Assistant Professor",
    "Lecturer",
    "Adjunct Faculty",
    "Visiting Faculty",
    "Lab Assistant",
    "HOD"
)

data class FacultyFormState(
    val photoUri: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val designation: String = "",
    val departmentId: Long? = null,
    val qualification: String = "",
    val employeeId: String = "",
    val experience: String = "",
    val maxWeeklyHours: String = "24",
    val canHandleLab: Boolean = false,
    val isActive: Boolean = true,
    val notes: String = "",
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

    private val _state = MutableStateFlow(FacultyFormState())
    val state: StateFlow<FacultyFormState> = _state

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val departments = departmentRepository.getActiveDepartments().first()

            if (facultyId != null) {
                val faculty = facultyRepository.getFacultyById(facultyId)
                if (faculty != null) {
                    _state.value = _state.value.copy(
                        photoUri = faculty.photoUri,
                        name = faculty.name,
                        email = faculty.email,
                        phone = faculty.phone,
                        designation = faculty.designation,
                        departmentId = faculty.departmentId,
                        qualification = faculty.qualification,
                        experience = faculty.experience.toString(),
                        maxWeeklyHours = faculty.maxWeeklyHours.toString(),
                        isActive = faculty.isActive,
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
        }
    }

    fun onPhotoUriChange(value: String) { _state.value = _state.value.copy(photoUri = value) }
    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value, error = null) }
    fun onEmailChange(value: String) { _state.value = _state.value.copy(email = value, error = null) }
    fun onPhoneChange(value: String) { _state.value = _state.value.copy(phone = value) }
    fun onDesignationChange(value: String) { _state.value = _state.value.copy(designation = value, error = null) }
    fun onDepartmentSelected(id: Long) { _state.value = _state.value.copy(departmentId = id, error = null) }
    fun onQualificationChange(value: String) { _state.value = _state.value.copy(qualification = value) }
    fun onEmployeeIdChange(value: String) { _state.value = _state.value.copy(employeeId = value) }
    fun onExperienceChange(value: String) { _state.value = _state.value.copy(experience = value) }
    fun onMaxHoursChange(value: String) { _state.value = _state.value.copy(maxWeeklyHours = value) }
    fun onCanHandleLabChange(value: Boolean) { _state.value = _state.value.copy(canHandleLab = value) }
    fun onIsActiveChange(value: Boolean) { _state.value = _state.value.copy(isActive = value) }
    fun onNotesChange(value: String) { _state.value = _state.value.copy(notes = value) }

    fun save() {
        val s = _state.value

        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Full name is required")
            return
        }
        if (s.email.isBlank()) {
            _state.value = s.copy(error = "Email address is required")
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

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val entity = FacultyEntity(
                    id = facultyId ?: 0,
                    name = s.name.trim(),
                    email = s.email.trim(),
                    phone = s.phone.trim(),
                    designation = s.designation,
                    departmentId = s.departmentId,
                    qualification = s.qualification.trim(),
                    experience = s.experience.toIntOrNull() ?: 0,
                    maxWeeklyHours = s.maxWeeklyHours.toIntOrNull() ?: 24,
                    photoUri = s.photoUri,
                    isActive = s.isActive
                )
                if (facultyId != null) {
                    facultyRepository.update(entity)
                } else {
                    facultyRepository.insert(entity)
                }
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Failed to save faculty")
            }
        }
    }
}
