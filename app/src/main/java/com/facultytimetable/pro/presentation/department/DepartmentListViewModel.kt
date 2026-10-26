package com.facultytimetable.pro.presentation.department

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.SectionDao
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DepartmentListState(
    val departments: List<DepartmentEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class DepartmentListViewModel @Inject constructor(
    private val departmentRepository: DepartmentRepository,
    private val facultyRepository: FacultyRepository,
    private val subjectRepository: SubjectRepository,
    private val sectionDao: SectionDao
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<DepartmentListState> = combine(
        departmentRepository.getAllDepartments(),
        searchQuery,
        message
    ) { depts, query, msg ->
        val filtered = if (query.isBlank()) depts
        else depts.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.code.contains(query, ignoreCase = true)
        }
        DepartmentListState(departments = filtered, searchQuery = query, isLoading = false, message = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DepartmentListState())

    fun onSearchQueryChange(query: String) { searchQuery.value = query }

    fun consumeMessage() { message.value = null }

    fun deleteDepartment(department: DepartmentEntity) {
        viewModelScope.launch {
            val hasFaculty = runCatching {
                facultyRepository.getAllFaculty().first().any { it.departmentId == department.id }
            }.getOrDefault(false)
            val hasSubjects = runCatching {
                subjectRepository.getAllSubjects().first().any { it.departmentId == department.id }
            }.getOrDefault(false)
            val hasSections = runCatching {
                sectionDao.getAllSections().first().any {
                    it.departmentId == department.id ||
                        (it.departmentId == 0L && it.name.startsWith(department.code, ignoreCase = true))
                }
            }.getOrDefault(false)
            if (hasFaculty || hasSubjects || hasSections) {
                val blockers = buildList {
                    if (hasFaculty) add("faculty")
                    if (hasSubjects) add("subjects")
                    if (hasSections) add("sections")
                }
                message.value = "Cannot delete ${department.code}: it still has ${blockers.joinToString(", ")}."
                return@launch
            }
            runCatching { departmentRepository.delete(department) }
                .onSuccess { message.value = "${department.name} deleted" }
                .onFailure {
                    message.value = "Cannot delete ${department.code}: it is still in use."
                }
        }
    }
}
