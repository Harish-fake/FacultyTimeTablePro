package com.facultytimetable.pro.presentation.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.TimetableEntryDao
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
    private val subjectRepository: SubjectRepository,
    private val timetableEntryDao: TimetableEntryDao
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
            val scheduled = runCatching {
                timetableEntryDao.getEntryCountBySubject(subject.id)
            }.getOrDefault(0)
            if (scheduled > 0) {
                message.value = "Cannot delete ${subject.code}: $scheduled timetable period(s) still use it. Regenerate the timetable first."
                return@launch
            }
            runCatching { subjectRepository.delete(subject) }
                .onSuccess { message.value = "${subject.name} deleted" }
                .onFailure {
                    message.value = "Cannot delete ${subject.code}: it is still in use."
                }
        }
    }
}
