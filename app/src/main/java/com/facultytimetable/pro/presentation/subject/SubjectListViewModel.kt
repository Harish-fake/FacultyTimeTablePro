package com.facultytimetable.pro.presentation.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
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
    val isLoading: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class SubjectListViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<SubjectListState> = combine(
        subjectRepository.getAllSubjects(),
        message
    ) { subjects, msg ->
        SubjectListState(subjects = subjects, isLoading = false, message = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubjectListState())

    fun consumeMessage() { message.value = null }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            runCatching { subjectRepository.delete(subject) }
                .onSuccess { message.value = "${subject.name} deleted" }
                .onFailure {
                    message.value = "Cannot delete ${subject.code}: it is used by the timetable."
                }
        }
    }
}
