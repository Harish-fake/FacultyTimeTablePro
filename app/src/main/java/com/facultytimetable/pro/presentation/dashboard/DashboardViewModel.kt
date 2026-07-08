package com.facultytimetable.pro.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
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
        deptCount, facCount, subjCount, roomCount, ttCount
    ) { dept, fac, subj, room, tt ->
        DashboardState(
            departmentCount = dept,
            facultyCount = fac,
            subjectCount = subj,
            roomCount = room,
            timetableCount = tt,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())
}
