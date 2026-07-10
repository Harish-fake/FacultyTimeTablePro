package com.facultytimetable.pro.presentation.department

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DepartmentListState(
    val departments: List<DepartmentEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val departmentFacultyCounts: Map<Long, Int> = emptyMap(),
    val departmentSubjectCounts: Map<Long, Int> = emptyMap()
)

@HiltViewModel
class DepartmentListViewModel @Inject constructor(
    private val departmentRepository: DepartmentRepository,
    private val facultyRepository: FacultyRepository,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)

    val state: StateFlow<DepartmentListState> = combine(
        departmentRepository.getAllDepartments(),
        _searchQuery.debounce(300),
        facultyRepository.getAllFaculty(),
        subjectRepository.getAllSubjects(),
        _isRefreshing
    ) { depts, query, faculty, subjects, refreshing ->
        val filtered = if (query.isBlank()) depts
        else depts.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.code.contains(query, ignoreCase = true)
        }
        val facultyCounts = faculty.groupBy { it.departmentId }.mapValues { it.value.size }
        val subjectCounts = subjects.groupBy { it.departmentId }.mapValues { it.value.size }
        DepartmentListState(
            departments = filtered,
            searchQuery = query,
            isLoading = false,
            isRefreshing = refreshing,
            departmentFacultyCounts = facultyCounts,
            departmentSubjectCounts = subjectCounts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DepartmentListState())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(300)
            _isRefreshing.value = false
        }
    }

    fun deleteDepartment(department: DepartmentEntity) {
        viewModelScope.launch { departmentRepository.delete(department) }
    }

    fun restoreDepartment(department: DepartmentEntity) {
        viewModelScope.launch { departmentRepository.insert(department) }
    }
}
