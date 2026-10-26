package com.facultytimetable.pro.presentation.faculty.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.TimetableEntryDao
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
    val isLoading: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class FacultyListViewModel @Inject constructor(
    private val facultyRepository: FacultyRepository,
    private val timetableEntryDao: TimetableEntryDao
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<FacultyListState> = combine(
        facultyRepository.getAllFaculty(),
        searchQuery,
        message
    ) { faculty, query, msg ->
        val filtered = if (query.isBlank()) faculty
        else faculty.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.email.contains(query, ignoreCase = true) ||
            it.designation.contains(query, ignoreCase = true)
        }
        FacultyListState(faculty = filtered, searchQuery = query, isLoading = false, message = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FacultyListState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun consumeMessage() { message.value = null }

    fun deleteFaculty(faculty: FacultyEntity) {
        viewModelScope.launch {
            val scheduled = runCatching {
                timetableEntryDao.getEntryCountByFaculty(faculty.id)
            }.getOrDefault(0)
            if (scheduled > 0) {
                message.value = "Cannot delete ${faculty.name}: $scheduled timetable period(s) are assigned to them."
                return@launch
            }
            runCatching { facultyRepository.delete(faculty) }
                .onSuccess { message.value = "${faculty.name} deleted" }
                .onFailure {
                    message.value = "Cannot delete ${faculty.name}: they are still in use."
                }
        }
    }
}
