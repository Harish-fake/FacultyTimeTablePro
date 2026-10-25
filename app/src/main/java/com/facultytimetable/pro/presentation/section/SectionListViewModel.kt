package com.facultytimetable.pro.presentation.section

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.SectionDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectionListState(
    val sections: List<SectionWithSemester> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
)

data class SectionWithSemester(
    val section: SectionEntity,
    val semesterName: String
)

@HiltViewModel
class SectionListViewModel @Inject constructor(
    private val sectionDao: SectionDao,
    private val semesterDao: SemesterDao
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<SectionListState> = combine(
        sectionDao.getAllSections(),
        semesterDao.getAllSemesters(),
        message
    ) { sections, semesters, msg ->
        val semMap = semesters.associateBy { it.id }
        SectionListState(
            sections = sections.map { SectionWithSemester(it, semMap[it.semesterId]?.name ?: "Unknown") },
            isLoading = false,
            message = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionListState())

    fun consumeMessage() { message.value = null }

    fun deleteSection(section: SectionEntity) {
        viewModelScope.launch {
            runCatching { sectionDao.delete(section) }
                .onSuccess { message.value = "${section.name} deleted" }
                .onFailure { message.value = "Could not delete ${section.name}" }
        }
    }
}
