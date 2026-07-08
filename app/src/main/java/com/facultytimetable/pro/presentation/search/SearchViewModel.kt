package com.facultytimetable.pro.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val results: List<SearchSuggestion> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val facultyRepository: FacultyRepository,
    private val subjectRepository: SubjectRepository,
    private val roomRepository: RoomRepository,
    private val departmentRepository: DepartmentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.value = _state.value.copy(results = emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
        val results = mutableListOf<SearchSuggestion>()

        facultyRepository.searchFaculty(query).first().forEach { f ->
            results.add(SearchSuggestion("Faculty", f.id, f.name, f.designation))
        }
        subjectRepository.searchSubjects(query).first().forEach { s ->
            results.add(SearchSuggestion("Subject", s.id, s.name, s.code))
        }
        roomRepository.searchRooms(query).first().forEach { r ->
            results.add(SearchSuggestion("Room", r.id, r.name, r.building))
        }
        departmentRepository.searchDepartments(query).first().forEach { d ->
            results.add(SearchSuggestion("Department", d.id, d.name, d.code))
        }

        _state.value = _state.value.copy(results = results)
    }
}
