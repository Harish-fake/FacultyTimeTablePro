package com.facultytimetable.pro.presentation.faculty.leave

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.FacultyDao
import com.facultytimetable.pro.data.local.db.dao.FacultyLeaveDao
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyLeaveEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class FacultyLeaveState(
    val faculty: FacultyEntity? = null,
    val leaves: List<FacultyLeaveEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class FacultyLeaveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val facultyDao: FacultyDao,
    private val leaveDao: FacultyLeaveDao
) : ViewModel() {

    private val facultyId: Long = savedStateHandle["facultyId"] ?: -1L
    private val _state = MutableStateFlow(FacultyLeaveState())
    val state: StateFlow<FacultyLeaveState> = _state

    init {
        viewModelScope.launch {
            facultyDao.getFacultyByIdFlow(facultyId).collect { faculty ->
                _state.update { it.copy(faculty = faculty, isLoading = false) }
            }
        }
        viewModelScope.launch {
            leaveDao.getLeavesByFaculty(facultyId).collect { leaves ->
                _state.update { it.copy(leaves = leaves) }
            }
        }
    }

    fun addLeave(dateText: String, startTime: String, endTime: String, reason: String, approved: Boolean) {
        val date = parseDate(dateText)
        if (date == null) {
            _state.update { it.copy(message = "Enter the leave date as YYYY-MM-DD.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            runCatching {
                leaveDao.insert(
                    FacultyLeaveEntity(
                        facultyId = facultyId,
                        leaveDate = date,
                        startTime = startTime.trim(),
                        endTime = endTime.trim(),
                        reason = reason.trim(),
                        isApproved = approved
                    )
                )
            }.onSuccess {
                _state.update { it.copy(isSaving = false, message = "Leave added") }
            }.onFailure { error ->
                _state.update { it.copy(isSaving = false, message = "Could not add leave: ${error.message}") }
            }
        }
    }

    fun toggleApproval(leave: FacultyLeaveEntity) {
        viewModelScope.launch {
            runCatching { leaveDao.update(leave.copy(isApproved = !leave.isApproved)) }
                .onSuccess {
                    _state.update {
                        it.copy(message = if (!leave.isApproved) "Leave approved" else "Leave marked pending")
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(message = "Could not update leave: ${error.message}") }
                }
        }
    }

    fun deleteLeave(leave: FacultyLeaveEntity) {
        viewModelScope.launch {
            runCatching { leaveDao.delete(leave) }
                .onSuccess { _state.update { it.copy(message = "Leave deleted") } }
                .onFailure { error ->
                    _state.update { it.copy(message = "Could not delete leave: ${error.message}") }
                }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun parseDate(text: String): Long? {
        return runCatching {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val date = format.parse(text.trim()) ?: return null
            Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.getOrNull()
    }
}
