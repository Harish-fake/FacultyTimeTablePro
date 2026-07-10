package com.facultytimetable.pro.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.datastore.AppPreferences
import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.dao.AuditLogDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.dao.TimeSlotDao
import com.facultytimetable.pro.data.local.db.dao.WorkingDayDao
import com.facultytimetable.pro.data.local.db.entity.AuditLogEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.data.model.TimetableWithDetails
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyAssignmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.LabRepository
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.domain.repository.SectionRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import com.facultytimetable.pro.domain.repository.TimetableRepository
import com.facultytimetable.pro.domain.usecase.timetable.ConflictEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
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
    val completionPercent: Float = 0f,
    val todaysClasses: List<TimetableWithDetails> = emptyList(),
    val todaysClassCount: Int = 0,
    val conflictCount: Int = 0,
    val nextClass: TimetableWithDetails? = null,
    val facultyUtilization: Float = 0f,
    val roomUtilization: Float = 0f,
    val semesterName: String = "",
    val welcomeMessage: String = "",
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
    private val auditLogDao: AuditLogDao,
    private val timeSlotDao: TimeSlotDao,
    private val workingDayDao: WorkingDayDao,
    private val conflictEngine: ConflictEngine
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
                val wdCount = workingDayDao.getWorkingDayCount()
                val slotCount = timeSlotDao.getCount()
                val completed = listOf(dept > 0, year > 0, sem > 0, sec > 0, wdCount > 0, slotCount > 0, room > 0, lab > 0, fac > 0, subj > 0, assign > 0, tt > 0).count { it }
                val completion = completed.toFloat() / totalSteps

                val todayDay = Calendar.getInstance().let { cal ->
                    (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY).let { if (it == 0) 0 else it - 1 }
                }
                val dayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                val todayName = dayNames.getOrElse(todayDay) { "Monday" }

                val allEntries = timetableRepository.getAllEntries().first()
                val todayEntries = allEntries.filter { it.dayOfWeek == todayDay }

                val allFaculties = facultyRepository.getAllFaculty().first()
                val allSubjects = subjectRepository.getAllSubjects().first()
                val allRooms = roomRepository.getAllRooms().first()
                val allSections = sectionRepository.getAllSections().first()
                val allTimeSlots = timeSlotDao.getAllTimeSlots().first()
                val allDepartments = departmentRepository.getAllDepartments().first()
                val deptMap = allDepartments.associate { it.id to it.name }
                val timeSlotMap = allTimeSlots.associate { it.id to it }
                val sectionNames = allSections.associate { it.id to it.name }

                val todaysClasses = todayEntries.map { entry ->
                    val slot = timeSlotMap[entry.timeSlotId]
                    val subject = allSubjects.find { it.id == entry.subjectId }
                    val faculty = allFaculties.find { it.id == entry.facultyId }
                    val roomEnt = allRooms.find { it.id == entry.roomId }
                    val section = allSections.find { it.id == entry.sectionId }
                    TimetableWithDetails(
                        entry = entry,
                        subjectName = subject?.name ?: "Unknown",
                        subjectCode = subject?.code ?: "",
                        subjectType = subject?.type?.name ?: "",
                        facultyName = faculty?.name ?: "Unknown",
                        roomName = roomEnt?.name ?: "Unknown",
                        sectionName = section?.name ?: "Unknown",
                        semesterName = "",
                        startTime = slot?.startTime ?: "",
                        endTime = slot?.endTime ?: "",
                        periodNumber = slot?.periodNumber ?: 0
                    )
                }.sortedBy { it.periodNumber }

                val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                val nextClass = todaysClasses.firstOrNull { tc ->
                    val endTime = tc.endTime
                    try {
                        val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        val nowTime = fmt.parse(now) ?: return@firstOrNull false
                        val classEnd = fmt.parse(endTime) ?: return@firstOrNull false
                        nowTime.before(classEnd)
                    } catch (_: Exception) { false }
                }

                val allConflicts = conflictEngine.detectConflicts(allEntries)
                val conflictCount = allConflicts.size

                val facultyAssignedCount = allFaculties.count { f ->
                    allEntries.any { it.facultyId == f.id }
                }
                val facUtil = if (fac > 0) facultyAssignedCount.toFloat() / fac else 0f
                val roomUsedCount = allRooms.count { r ->
                    allEntries.any { it.roomId == r.id }
                }
                val romUtil = if (room > 0) roomUsedCount.toFloat() / room else 0f
                val todayClassCount = todaysClasses.size

                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val greeting = when (hour) {
                    in 5..11 -> "Good Morning"
                    in 12..16 -> "Good Afternoon"
                    in 17..21 -> "Good Evening"
                    else -> "Good Night"
                }

                _state.value = DashboardState(
                    departmentCount = dept, facultyCount = fac, subjectCount = subj,
                    roomCount = room, labCount = lab, sectionCount = sec,
                    academicYearCount = year, semesterCount = sem,
                    timetableCount = tt, assignmentCount = assign,
                    firstLaunch = firstLaunch, isLoading = false,
                    recentActivities = recent, completionPercent = completion,
                    todaysClasses = todaysClasses, todaysClassCount = todaysClasses.size,
                    nextClass = nextClass, conflictCount = conflictCount,
                    facultyUtilization = facUtil, roomUtilization = romUtil,
                    semesterName = "Current Semester",
                    welcomeMessage = "$greeting! ${todayName}, $todayClassCount classes today"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}
