package com.facultytimetable.pro.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.datastore.AppPreferences
import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.dao.AuditLogDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.AuditLogEntity
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyAssignmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.LabRepository
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.domain.repository.SectionRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import com.facultytimetable.pro.domain.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val departmentCount: Int = 0,
    val facultyCount: Int = 0,
    val subjectCount: Int = 0,
    val roomCount: Int = 0,
    val labCount: Int = 0,
    val sectionCount: Int = 0,
    val academicYearCount: Int = 0,
    val semesterCount: Int = 0,
    val timetableCount: Int = 0,
    val assignmentCount: Int = 0,
    val firstLaunch: Boolean = false,
    val isLoading: Boolean = true,
    val recentActivities: List<AuditLogEntity> = emptyList(),
    val completionPercent: Float = 0f
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val departmentRepository: DepartmentRepository,
    private val facultyRepository: FacultyRepository,
    private val subjectRepository: SubjectRepository,
    private val roomRepository: RoomRepository,
    private val sectionRepository: SectionRepository,
    private val timetableRepository: TimetableRepository,
    private val labRepository: LabRepository,
    private val academicYearDao: AcademicYearDao,
    private val semesterDao: SemesterDao,
    private val facultyAssignmentRepository: FacultyAssignmentRepository,
    private val auditLogDao: AuditLogDao
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init { loadDashboard() }

    fun refresh() { loadDashboard() }

    private fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val dept = departmentRepository.getCount()
                val fac = facultyRepository.getCount()
                val subj = subjectRepository.getCount()
                val room = roomRepository.getCount()
                val lab = labRepository.getCount()
                val sec = sectionRepository.getCount()
                val year = academicYearDao.getCount()
                val sem = semesterDao.getCount()
                val tt = timetableRepository.getCount()
                val assign = facultyAssignmentRepository.getCount()
                val firstLaunch = appPreferences.isFirstLaunch.first()
                val recent = auditLogDao.getRecentLogs(10).first()

                val totalSteps = 12
                val completed = listOf(dept > 0, fac > 0, subj > 0, room > 0, lab > 0, sec > 0, year > 0, sem > 0, tt > 0, assign > 0).count { it }
                val completion = completed.toFloat() / totalSteps

                _state.value = DashboardState(
                    departmentCount = dept, facultyCount = fac, subjectCount = subj,
                    roomCount = room, labCount = lab, sectionCount = sec,
                    academicYearCount = year, semesterCount = sem,
                    timetableCount = tt, assignmentCount = assign,
                    firstLaunch = firstLaunch, isLoading = false,
                    recentActivities = recent, completionPercent = completion
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}
