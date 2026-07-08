package com.facultytimetable.pro.presentation.department

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DepartmentListState(
    val departments: List<DepartmentEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class DepartmentListViewModel @Inject constructor(
    private val departmentRepository: DepartmentRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val state: StateFlow<DepartmentListState> = combine(
        departmentRepository.getAllDepartments(),
        searchQuery
    ) { depts, query ->
        val filtered = if (query.isBlank()) depts
        else depts.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.code.contains(query, ignoreCase = true)
        }
        DepartmentListState(departments = filtered, searchQuery = query, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DepartmentListState())

    fun onSearchQueryChange(query: String) { searchQuery.value = query }

    fun deleteDepartment(department: DepartmentEntity) {
        viewModelScope.launch { departmentRepository.delete(department) }
    }
}
