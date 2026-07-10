package com.facultytimetable.pro.presentation.leave

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.FacultyDao
import com.facultytimetable.pro.data.local.db.dao.FacultyLeaveDao
import com.facultytimetable.pro.data.local.db.entity.FacultyLeaveEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaveWithFaculty(
    val leave: FacultyLeaveEntity,
    val facultyName: String
)

data class FacultyLeaveState(
    val leaves: List<LeaveWithFaculty> = emptyList(),
    val faculties: List<com.facultytimetable.pro.data.local.db.entity.FacultyEntity> = emptyList(),
    val selectedFacultyId: Long? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class FacultyLeaveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val facultyLeaveDao: FacultyLeaveDao,
    private val facultyDao: FacultyDao
) : ViewModel() {

    private val facultyId: Long = savedStateHandle["facultyId"] ?: -1L
    private val selectedFacultyId = MutableStateFlow<Long?>(if (facultyId > 0) facultyId else null)

    val state: StateFlow<FacultyLeaveState> = combine(
        facultyLeaveDao.getAllLeaves(),
        facultyDao.getActiveFaculty(),
        selectedFacultyId
    ) { allLeaves, faculties, selectedId ->
        val facultyMap = faculties.associate { it.id to it.name }
        val filtered = if (selectedId != null) {
            allLeaves.filter { it.facultyId == selectedId }
        } else {
            allLeaves
        }
        FacultyLeaveState(
            leaves = filtered.map { leave ->
                LeaveWithFaculty(leave = leave, facultyName = facultyMap[leave.facultyId] ?: "Unknown")
            },
            faculties = faculties,
            selectedFacultyId = selectedId,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FacultyLeaveState())

    fun onFacultyFilterChanged(facultyId: Long?) {
        selectedFacultyId.value = facultyId
    }

    fun insertLeave(leave: FacultyLeaveEntity) {
        viewModelScope.launch { facultyLeaveDao.insert(leave) }
    }

    fun updateLeave(leave: FacultyLeaveEntity) {
        viewModelScope.launch { facultyLeaveDao.update(leave) }
    }

    fun deleteLeave(leave: FacultyLeaveEntity) {
        viewModelScope.launch { facultyLeaveDao.delete(leave) }
    }
}
