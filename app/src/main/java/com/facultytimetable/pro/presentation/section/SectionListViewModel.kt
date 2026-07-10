package com.facultytimetable.pro.presentation.section

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.dao.DepartmentDao
import com.facultytimetable.pro.data.local.db.dao.RoomDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.domain.repository.SectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectionListState(
    val sections: List<SectionWithDetails> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

data class SectionWithDetails(
    val section: SectionEntity,
    val semesterName: String = "",
    val departmentName: String = "",
    val academicYearName: String = "",
    val roomName: String = ""
)

@HiltViewModel
class SectionListViewModel @Inject constructor(
    private val sectionRepository: SectionRepository,
    private val semesterDao: SemesterDao,
    private val departmentDao: DepartmentDao,
    private val academicYearDao: AcademicYearDao,
    private val roomDao: RoomDao
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val state: StateFlow<SectionListState> = combine(
        sectionRepository.getAllSections(),
        semesterDao.getAllSemesters(),
        departmentDao.getAllDepartments(),
        academicYearDao.getAllAcademicYears(),
        roomDao.getAllRooms(),
        searchQuery
    ) { sections, semesters, departments, years, rooms, query ->
        val semMap = semesters.associateBy { it.id }
        val deptMap = departments.associateBy { it.id }
        val yearMap = years.associateBy { it.id }
        val roomMap = rooms.associateBy { it.id }

        val sectionDetails = sections.map {
            SectionWithDetails(
                section = it,
                semesterName = semMap[it.semesterId]?.name ?: "Unknown",
                departmentName = deptMap[it.departmentId]?.name ?: "",
                academicYearName = yearMap[it.academicYearId]?.name ?: "",
                roomName = if (it.roomId > 0) roomMap[it.roomId]?.name ?: "" else ""
            )
        }

        val filtered = if (query.isBlank()) sectionDetails
        else sectionDetails.filter { s ->
            s.section.name.contains(query, ignoreCase = true) ||
            s.semesterName.contains(query, ignoreCase = true) ||
            s.departmentName.contains(query, ignoreCase = true) ||
            s.roomName.contains(query, ignoreCase = true)
        }

        SectionListState(
            sections = filtered,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionListState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun deleteSection(section: SectionEntity) {
        viewModelScope.launch { sectionRepository.delete(section) }
    }
}
