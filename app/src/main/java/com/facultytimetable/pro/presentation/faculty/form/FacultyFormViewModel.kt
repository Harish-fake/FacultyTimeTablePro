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
    "Professor", "Associate Professor", "Assistant Professor",
    "Lecturer", "Adjunct Faculty", "Visiting Faculty",
    "Lab Assistant", "HOD"
)

val GENDERS = listOf("Male", "Female", "Other")
val STATUS_OPTIONS = listOf("Active", "On Leave", "Part-Time", "Contract", "Retired")

data class FacultyFormState(
    val photoUri: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val designation: String = "",
    val departmentId: Long? = null,
    val qualification: String = "",
    val employeeId: String = "",
    val facultyCode: String = "",
    val gender: String = "",
    val officeRoom: String = "",
    val preferredDays: String = "",
    val unavailableDays: String = "",
    val preferredTimeSlots: String = "",
    val labEligible: Boolean = false,
    val status: String = "Active",
    val experience: String = "",
    val maxWeeklyHours: String = "24",
    val notes: String = "",
    val isActive: Boolean = true,
    val departments: List<DepartmentEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val designationError: String? = null,
    val departmentError: String? = null,
    val employeeIdError: String? = null,
    val showSaveAnimation: Boolean = false
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

    init { loadData() }

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
                        employeeId = faculty.employeeId,
                        facultyCode = faculty.facultyCode,
                        gender = faculty.gender,
                        officeRoom = faculty.officeRoom,
                        preferredDays = faculty.preferredDays,
                        unavailableDays = faculty.unavailableDays,
                        preferredTimeSlots = faculty.preferredTimeSlots,
                        labEligible = faculty.labEligible,
                        status = faculty.status,
                        experience = faculty.experience.toString(),
                        maxWeeklyHours = faculty.maxWeeklyHours.toString(),
                        notes = faculty.notes,
                        isActive = faculty.isActive,
                        isEditing = true,
                        isLoading = false,
                        departments = departments
                    )
                    return@launch
                }
            }
            _state.value = _state.value.copy(isLoading = false, departments = departments)
        }
    }

    fun onPhotoUriChange(value: String) { _state.value = _state.value.copy(photoUri = value) }
    fun onNameChange(value: String) {
        if (value.length <= 100) _state.value = _state.value.copy(name = value, nameError = null)
    }
    fun onEmailChange(value: String) {
        if (value.length <= 100) _state.value = _state.value.copy(email = value, emailError = null)
    }
    fun onPhoneChange(value: String) {
        if (value.length <= 20) _state.value = _state.value.copy(phone = value, phoneError = null)
    }
    fun onDesignationChange(value: String) { _state.value = _state.value.copy(designation = value, designationError = null) }
    fun onDepartmentSelected(id: Long) { _state.value = _state.value.copy(departmentId = id, departmentError = null) }
    fun onQualificationChange(value: String) {
        if (value.length <= 100) _state.value = _state.value.copy(qualification = value)
    }
    fun onEmployeeIdChange(value: String) {
        if (value.length <= 20) _state.value = _state.value.copy(employeeId = value, employeeIdError = null)
    }
    fun onFacultyCodeChange(value: String) {
        if (value.length <= 20) _state.value = _state.value.copy(facultyCode = value)
    }
    fun onGenderChange(value: String) { _state.value = _state.value.copy(gender = value) }
    fun onOfficeRoomChange(value: String) {
        if (value.length <= 100) _state.value = _state.value.copy(officeRoom = value)
    }
    fun onPreferredDaysChange(value: String) {
        if (value.length <= 200) _state.value = _state.value.copy(preferredDays = value)
    }
    fun onUnavailableDaysChange(value: String) {
        if (value.length <= 200) _state.value = _state.value.copy(unavailableDays = value)
    }
    fun onPreferredTimeSlotsChange(value: String) {
        if (value.length <= 200) _state.value = _state.value.copy(preferredTimeSlots = value)
    }
    fun onLabEligibleChange(value: Boolean) { _state.value = _state.value.copy(labEligible = value) }
    fun onStatusChange(value: String) { _state.value = _state.value.copy(status = value) }
    fun onExperienceChange(value: String) {
        if (value.length <= 3) _state.value = _state.value.copy(experience = value)
    }
    fun onMaxHoursChange(value: String) {
        if (value.length <= 3) _state.value = _state.value.copy(maxWeeklyHours = value)
    }
    fun onNotesChange(value: String) {
        if (value.length <= 500) _state.value = _state.value.copy(notes = value)
    }
    fun onIsActiveChange(value: Boolean) { _state.value = _state.value.copy(isActive = value) }

    fun save() {
        val s = _state.value

        val nameError = if (s.name.isBlank()) "Full name is required" else null
        val emailError = when {
            s.email.isBlank() -> "Email address is required"
            !s.email.contains("@") || !s.email.contains(".") -> "Enter a valid email address"
            else -> null
        }
        val phoneError = if (s.phone.isNotBlank() && !s.phone.matches(Regex("^\\+?[0-9]{10,15}$"))) "Enter a valid phone number (10-15 digits)" else null
        val designationError = if (s.designation.isBlank()) "Designation is required" else null
        val departmentError = if (s.departmentId == null) "Department is required" else null

        _state.value = s.copy(
            nameError = nameError,
            emailError = emailError,
            phoneError = phoneError,
            designationError = designationError,
            departmentError = departmentError,
            employeeIdError = null
        )

        if (nameError != null || emailError != null || phoneError != null || designationError != null || departmentError != null) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)

            try {
                val allFaculty = facultyRepository.getAllFaculty().first()
                val currentId = facultyId ?: 0

                val emailExists = allFaculty.any {
                    it.email.equals(s.email.trim(), ignoreCase = true) && it.id != currentId
                }
                if (emailExists) {
                    _state.value = _state.value.copy(isSaving = false, emailError = "This email is already in use")
                    return@launch
                }

                val empIdExists = s.employeeId.isNotBlank() && allFaculty.any {
                    it.employeeId.equals(s.employeeId.trim(), ignoreCase = true) && it.id != currentId
                }
                if (empIdExists) {
                    _state.value = _state.value.copy(isSaving = false, employeeIdError = "This Employee ID is already in use")
                    return@launch
                }

                val entity = FacultyEntity(
                    id = currentId,
                    name = s.name.trim(),
                    email = s.email.trim(),
                    phone = s.phone.trim(),
                    designation = s.designation,
                    departmentId = s.departmentId,
                    qualification = s.qualification.trim(),
                    employeeId = s.employeeId.trim(),
                    facultyCode = s.facultyCode.trim(),
                    gender = s.gender,
                    officeRoom = s.officeRoom.trim(),
                    preferredDays = s.preferredDays,
                    unavailableDays = s.unavailableDays,
                    preferredTimeSlots = s.preferredTimeSlots,
                    labEligible = s.labEligible,
                    status = s.status,
                    experience = s.experience.toIntOrNull() ?: 0,
                    maxWeeklyHours = s.maxWeeklyHours.toIntOrNull() ?: 24,
                    notes = s.notes.trim(),
                    photoUri = s.photoUri,
                    isActive = s.isActive
                )

                if (facultyId != null) facultyRepository.update(entity)
                else facultyRepository.insert(entity)

                _state.value = _state.value.copy(isSaving = false, showSaveAnimation = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Failed to save faculty")
            }
        }
    }
}
