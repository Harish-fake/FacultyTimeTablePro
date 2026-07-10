package com.facultytimetable.pro.presentation.semester

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SemesterType(val displayName: String) {
    REGULAR("Regular"),
    ODD("Odd"),
    EVEN("Even")
}

data class SemesterFormState(
    val name: String = "",
    val academicYearId: Long? = null,
    val semesterNumber: String = "",
    val semesterType: SemesterType = SemesterType.REGULAR,
    val departmentIds: String = "",
    val isActive: Boolean = true,
    val academicYears: List<AcademicYearEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SemesterFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val semesterDao: SemesterDao,
    private val academicYearDao: AcademicYearDao
) : ViewModel() {

    private val semesterId: Long? = savedStateHandle.get<Long>("semesterId")?.takeIf { it > 0 }
    private val _state = MutableStateFlow(SemesterFormState())
    val state: StateFlow<SemesterFormState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val years = academicYearDao.getAllAcademicYears().first()
            if (semesterId != null) {
                val sem = semesterDao.getSemesterById(semesterId)
                if (sem != null) {
                    val type = SemesterType.entries.find {
                        it.name.equals(sem.semesterType, ignoreCase = true)
                    } ?: SemesterType.REGULAR
                    _state.value = _state.value.copy(
                        name = sem.name,
                        academicYearId = sem.academicYearId,
                        semesterNumber = sem.semesterNumber.toString(),
                        semesterType = type,
                        departmentIds = sem.departmentIds,
                        isActive = sem.isActive,
                        academicYears = years,
                        isEditing = true,
                        isLoading = false
                    )
                    return@launch
                }
            }
            _state.value = _state.value.copy(academicYears = years, isLoading = false)
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onSemesterNumberChange(v: String) { _state.value = _state.value.copy(semesterNumber = v.filter { it.isDigit() }, error = null) }
    fun onAcademicYearSelected(year: AcademicYearEntity) { _state.value = _state.value.copy(academicYearId = year.id, error = null) }
    fun onSemesterTypeChange(type: SemesterType) { _state.value = _state.value.copy(semesterType = type) }
    fun onActiveChange(v: Boolean) { _state.value = _state.value.copy(isActive = v) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Name is required"); return }
        if (s.semesterNumber.isBlank()) { _state.value = s.copy(error = "Semester number is required"); return }
        if (s.academicYearId == null) { _state.value = s.copy(error = "Select an academic year"); return }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val entity = SemesterEntity(
                    id = semesterId ?: 0,
                    name = s.name.trim(),
                    semesterNumber = s.semesterNumber.toIntOrNull() ?: 1,
                    academicYearId = s.academicYearId!!,
                    semesterType = s.semesterType.name,
                    departmentIds = s.departmentIds,
                    isActive = s.isActive
                )
                if (semesterId != null) semesterDao.update(entity)
                else semesterDao.insert(entity)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }
}
