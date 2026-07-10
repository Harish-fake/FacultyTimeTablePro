package com.facultytimetable.pro.presentation.academicyear

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AcademicYearFormState(
    val name: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isCurrent: Boolean = false,
    val nameError: String? = null,
    val startDateError: String? = null,
    val endDateError: String? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AcademicYearFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val academicYearDao: AcademicYearDao
) : ViewModel() {

    private val yearId: Long? = savedStateHandle.get<Long>("yearId")?.takeIf { it > 0 }
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val _state = MutableStateFlow(AcademicYearFormState())
    val state: StateFlow<AcademicYearFormState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            if (yearId != null) {
                val year = academicYearDao.getAcademicYearById(yearId)
                if (year != null) {
                    _state.value = _state.value.copy(
                        name = year.name,
                        startDate = year.startDate,
                        endDate = year.endDate,
                        isCurrent = year.isCurrent,
                        isEditing = true,
                        isLoading = false
                    )
                    return@launch
                }
            }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, nameError = null) }
    fun onStartDateSelected(d: Long) { _state.value = _state.value.copy(startDate = d, startDateError = null) }
    fun onEndDateSelected(d: Long) { _state.value = _state.value.copy(endDate = d, endDateError = null) }
    fun onIsCurrentChange(v: Boolean) { _state.value = _state.value.copy(isCurrent = v) }

    fun formatDate(epoch: Long?): String {
        return epoch?.let { dateFormat.format(Date(it)) } ?: ""
    }

    fun save() {
        val s = _state.value
        var hasError = false

        if (s.name.isBlank()) {
            _state.value = _state.value.copy(nameError = "Name is required")
            hasError = true
        }
        if (s.startDate == null) {
            _state.value = _state.value.copy(startDateError = "Start date is required")
            hasError = true
        }
        if (s.endDate == null) {
            _state.value = _state.value.copy(endDateError = "End date is required")
            hasError = true
        }
        if (s.startDate != null && s.endDate != null && s.startDate >= s.endDate) {
            _state.value = _state.value.copy(endDateError = "End date must be after start date")
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                if (s.isCurrent) {
                    academicYearDao.clearCurrentYear()
                }
                val entity = AcademicYearEntity(
                    id = yearId ?: 0,
                    name = s.name.trim(),
                    startDate = s.startDate!!,
                    endDate = s.endDate!!,
                    isCurrent = s.isCurrent
                )
                val id = if (yearId != null) {
                    academicYearDao.update(entity)
                    yearId
                } else {
                    academicYearDao.insert(entity)
                }
                if (s.isCurrent) {
                    academicYearDao.setCurrentYear(id)
                }
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }
}
