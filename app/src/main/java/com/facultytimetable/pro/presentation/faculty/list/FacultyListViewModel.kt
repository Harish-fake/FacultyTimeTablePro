package com.facultytimetable.pro.presentation.faculty.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.domain.repository.FacultyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FacultyListState(
    val faculty: List<FacultyEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class FacultyListViewModel @Inject constructor(
    private val facultyRepository: FacultyRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val state: StateFlow<FacultyListState> = combine(
        facultyRepository.getAllFaculty(),
        searchQuery
    ) { faculty, query ->
        val filtered = if (query.isBlank()) faculty
        else faculty.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.email.contains(query, ignoreCase = true) ||
            it.designation.contains(query, ignoreCase = true)
        }
        FacultyListState(faculty = filtered, searchQuery = query, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FacultyListState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun deleteFaculty(faculty: FacultyEntity) {
        viewModelScope.launch {
            facultyRepository.delete(faculty)
        }
    }
}
