package com.facultytimetable.pro.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.datastore.AppPreferences
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import com.facultytimetable.pro.domain.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardState(
    val departmentCount: Int = 0,
    val facultyCount: Int = 0,
    val subjectCount: Int = 0,
    val roomCount: Int = 0,
    val timetableCount: Int = 0,
    val firstLaunch: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    departmentRepository: DepartmentRepository,
    facultyRepository: FacultyRepository,
    subjectRepository: SubjectRepository,
    roomRepository: RoomRepository,
    timetableRepository: TimetableRepository
) : ViewModel() {

    private val deptCount = departmentRepository.getCountFlow()
    private val facCount = facultyRepository.getCountFlow()
    private val subjCount = subjectRepository.getCountFlow()
    private val roomCount = roomRepository.getCountFlow()
    private val ttCount = timetableRepository.getCountFlow()

    val state: StateFlow<DashboardState> = combine(
        deptCount, facCount, subjCount, roomCount, ttCount, appPreferences.isFirstLaunch
    ) {
        DashboardState(
            departmentCount = it[0] as Int,
            facultyCount = it[1] as Int,
            subjectCount = it[2] as Int,
            roomCount = it[3] as Int,
            timetableCount = it[4] as Int,
            firstLaunch = it[5] as Boolean,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())
}
