package com.facultytimetable.pro.presentation.lab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.LabEntity
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.LabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LabListState(
    val labs: List<LabEntity> = emptyList(),
    val departmentNames: Map<Long, String> = emptyMap(),
    val searchQuery: String = "",
    val selectedDepartment: Long? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class LabListViewModel @Inject constructor(
    private val labRepository: LabRepository,
    private val departmentRepository: DepartmentRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedDepartment = MutableStateFlow<Long?>(null)

    val state: StateFlow<LabListState> = combine(
        labRepository.getAllLabs(),
        departmentRepository.getAllDepartments(),
        searchQuery,
        selectedDepartment
    ) { labs, departments, query, deptId ->
        val deptNames = departments.associate { it.id to it.name }
        val filtered = labs.filter { lab ->
            val matchesQuery = query.isBlank() ||
                lab.name.contains(query, ignoreCase = true) ||
                lab.roomNumber.contains(query, ignoreCase = true) ||
                deptNames[lab.departmentId]?.contains(query, ignoreCase = true) == true
            val matchesDept = deptId == null || lab.departmentId == deptId
            matchesQuery && matchesDept
        }
        LabListState(
            labs = filtered,
            departmentNames = deptNames,
            searchQuery = query,
            selectedDepartment = deptId,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LabListState())

    fun onSearchQueryChange(query: String) { searchQuery.value = query }

    fun onDepartmentFilterChange(departmentId: Long?) { selectedDepartment.value = departmentId }

    fun deleteLab(lab: LabEntity) {
        viewModelScope.launch { labRepository.delete(lab) }
    }

    fun restoreLab(lab: LabEntity) {
        viewModelScope.launch { labRepository.insert(lab) }
    }
}
