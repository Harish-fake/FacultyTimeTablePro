package com.facultytimetable.pro.presentation.semester

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SemesterListState(
    val semesters: List<SemesterWithYear> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

data class SemesterWithYear(
    val semester: SemesterEntity,
    val yearName: String
)

@HiltViewModel
class SemesterListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val semesterDao: SemesterDao,
    private val academicYearDao: AcademicYearDao
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val state: StateFlow<SemesterListState> = combine(
        semesterDao.getAllSemesters(),
        academicYearDao.getAllAcademicYears(),
        searchQuery
    ) { semesters, years, query ->
        val yearMap = years.associateBy { it.id }
        val mapped = semesters.map { SemesterWithYear(it, yearMap[it.academicYearId]?.name ?: "Unknown") }
        val filtered = if (query.isBlank()) mapped
        else mapped.filter { item ->
            item.semester.name.contains(query, ignoreCase = true) ||
            item.yearName.contains(query, ignoreCase = true) ||
            item.semester.semesterNumber.toString().contains(query)
        }
        SemesterListState(semesters = filtered, searchQuery = query, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SemesterListState())

    fun onSearchQueryChange(query: String) { searchQuery.value = query }

    fun deleteSemester(semester: SemesterEntity) {
        viewModelScope.launch { semesterDao.delete(semester) }
    }

    fun restoreSemester(semester: SemesterEntity) {
        viewModelScope.launch { semesterDao.insert(semester) }
    }
}
