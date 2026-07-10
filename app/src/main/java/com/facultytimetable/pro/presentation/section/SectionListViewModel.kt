package com.facultytimetable.pro.presentation.section

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.RoomDao
import com.facultytimetable.pro.data.local.db.dao.SectionDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectionListState(
    val sections: List<SectionWithDetails> = emptyList(),
    val isLoading: Boolean = true
)

data class SectionWithDetails(
    val section: SectionEntity,
    val semesterName: String,
    val roomName: String = ""
)

@HiltViewModel
class SectionListViewModel @Inject constructor(
    private val sectionDao: SectionDao,
    private val semesterDao: SemesterDao,
    private val roomDao: RoomDao
) : ViewModel() {

    val state: StateFlow<SectionListState> = combine(
        sectionDao.getAllSections(),
        semesterDao.getAllSemesters(),
        roomDao.getAllRooms()
    ) { sections, semesters, rooms ->
        val semMap = semesters.associateBy { it.id }
        val roomMap = rooms.associateBy { it.id }
        SectionListState(
            sections = sections.map {
                SectionWithDetails(
                    section = it,
                    semesterName = semMap[it.semesterId]?.name ?: "Unknown",
                    roomName = if (it.roomId > 0) roomMap[it.roomId]?.name ?: "" else ""
                )
            },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionListState())

    fun deleteSection(section: SectionEntity) {
        viewModelScope.launch { sectionDao.delete(section) }
    }
}
