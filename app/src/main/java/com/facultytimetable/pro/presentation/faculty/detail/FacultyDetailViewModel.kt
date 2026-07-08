package com.facultytimetable.pro.presentation.faculty.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.domain.repository.FacultyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FacultyDetailState(
    val faculty: FacultyEntity? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class FacultyDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val facultyRepository: FacultyRepository
) : ViewModel() {

    private val facultyId: Long = savedStateHandle["facultyId"] ?: -1L

    private val _state = MutableStateFlow(FacultyDetailState())
    val state: StateFlow<FacultyDetailState> = _state

    init {
        loadFaculty()
    }

    private fun loadFaculty() {
        viewModelScope.launch {
            val faculty = facultyRepository.getFacultyById(facultyId)
            _state.value = FacultyDetailState(faculty = faculty, isLoading = false)
        }
    }
}
