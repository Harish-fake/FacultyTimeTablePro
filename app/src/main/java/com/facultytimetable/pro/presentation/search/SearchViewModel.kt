package com.facultytimetable.pro.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.LabRepository
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.domain.repository.SectionRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchSuggestion(
    val type: String,
    val id: Long,
    val title: String,
    val subtitle: String
)

data class SearchState(
    val query: String = "",
    val results: List<SearchSuggestion> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val facultyRepository: FacultyRepository,
    private val subjectRepository: SubjectRepository,
    private val roomRepository: RoomRepository,
    private val departmentRepository: DepartmentRepository,
    private val sectionRepository: SectionRepository,
    private val labRepository: LabRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _state.update { it.copy(isSearching = true) }
            performSearch(query)
            _state.update { it.copy(isSearching = false) }
        }
    }

    private suspend fun performSearch(query: String) {
        val results = mutableListOf<SearchSuggestion>()
        val q = query.trim()

        val departments = departmentRepository.getAllDepartments().first()
        val faculty = facultyRepository.getAllFaculty().first()
        val subjects = subjectRepository.getAllSubjects().first()
        val rooms = roomRepository.getAllRooms().first()
        val sections = sectionRepository.getAllSections().first()
        val labs = labRepository.getAllLabs().first()
        val deptMap = departments.associateBy { it.id }

        for (f in faculty) {
            if (f.name.contains(q, true) || f.email.contains(q, true) ||
                f.employeeId.contains(q, true) || f.facultyCode.contains(q, true) ||
                f.designation.contains(q, true) || f.phone.contains(q, true)
            ) {
                val deptName = deptMap[f.departmentId]?.name ?: ""
                results.add(SearchSuggestion("Faculty", f.id, f.name, "$deptName | ${f.designation}"))
            }
        }

        for (s in subjects) {
            if (s.name.contains(q, true) || s.code.contains(q, true)) {
                val deptName = deptMap[s.departmentId]?.name ?: ""
                results.add(SearchSuggestion("Subject", s.id, "${s.name} (${s.code})", deptName))
            }
        }

        for (d in departments) {
            if (d.name.contains(q, true) || d.code.contains(q, true)) {
                results.add(SearchSuggestion("Department", d.id, d.name, "${d.code} | ${d.headName}"))
            }
        }

        for (r in rooms) {
            if (r.name.contains(q, true) || r.building.contains(q, true) || r.roomNumber.contains(q, true)) {
                results.add(SearchSuggestion("Room", r.id, r.name, "${r.type.name} | ${r.building}"))
            }
        }

        for (l in labs) {
            if (l.name.contains(q, true) || l.roomNumber.contains(q, true) || l.building.contains(q, true)) {
                val deptName = deptMap[l.departmentId]?.name ?: ""
                results.add(SearchSuggestion("Lab", l.id, l.name, "$deptName | ${l.roomNumber}"))
            }
        }

        for (s in sections) {
            if (s.name.contains(q, true)) {
                val deptName = deptMap[s.departmentId]?.name ?: ""
                results.add(SearchSuggestion("Section", s.id, s.name, deptName))
            }
        }

        _state.update { it.copy(results = results.take(50)) }
    }
}
