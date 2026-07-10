package com.facultytimetable.pro.presentation.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectListState(
    val subjects: List<SubjectEntity> = emptyList(),
    val departmentNames: Map<Long, String> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class SubjectListViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val departmentRepository: DepartmentRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val state: StateFlow<SubjectListState> = combine(
        subjectRepository.getAllSubjects(),
        departmentRepository.getAllDepartments(),
        searchQuery
    ) { subjects, departments, query ->
        val deptMap = departments.associate { it.id to it.name }
        val filtered = if (query.isBlank()) subjects
        else subjects.filter { s ->
            val deptName = deptMap[s.departmentId] ?: ""
            s.name.contains(query, ignoreCase = true) ||
            s.code.contains(query, ignoreCase = true) ||
            deptName.contains(query, ignoreCase = true)
        }
        SubjectListState(
            subjects = filtered,
            departmentNames = deptMap,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubjectListState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            subjectRepository.delete(subject)
        }
    }
}
