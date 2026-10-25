package com.facultytimetable.pro.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.RoomType
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.domain.repository.SectionRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import com.facultytimetable.pro.domain.repository.TimeSlotRepository
import com.facultytimetable.pro.domain.repository.TimetableRepository
import com.facultytimetable.pro.domain.usecase.timetable.AssignmentPlanner
import com.facultytimetable.pro.domain.usecase.timetable.requiredPeriods
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class DashboardClass(
    val entry: TimetableEntryEntity,
    val subjectName: String,
    val subjectCode: String,
    val subjectType: String,
    val facultyName: String,
    val roomName: String,
    val sectionName: String,
    val startTime: String,
    val endTime: String,
    val periodNumber: Int
)

data class DashboardState(
    val departmentCount: Int = 0,
    val facultyCount: Int = 0,
    val subjectCount: Int = 0,
    val roomCount: Int = 0,
    val sectionCount: Int = 0,
    val timetableCount: Int = 0,
    val requiredPeriods: Int = 0,
    val labRoomCount: Int = 0,
    val classroomCount: Int = 0,
    val regularPeriodCount: Int = 0,
    val todayClasses: List<DashboardClass> = emptyList(),
    val todayLabel: String = "",
    val todayIsTeachingDay: Boolean = true,
    val academicYear: String? = null,
    val semesterName: String? = null,
    val dataIssues: List<String> = emptyList(),
    val isLoading: Boolean = true
) {
    val coveragePercent: Float
        get() = if (requiredPeriods <= 0) 0f
        else (timetableCount.toFloat() / requiredPeriods).coerceIn(0f, 1f)

    val unscheduledPeriods: Int get() = (requiredPeriods - timetableCount).coerceAtLeast(0)
    val isReady: Boolean get() = dataIssues.isEmpty()
    val hasTimetable: Boolean get() = timetableCount > 0
}

private data class Counts(
    val departments: Int,
    val faculty: Int,
    val subjects: Int,
    val rooms: Int,
    val sections: Int
)

private data class ScheduleData(
    val entries: List<TimetableEntryEntity>,
    val slots: List<TimeSlotEntity>,
    val subjects: List<SubjectEntity>
)

private data class PeopleData(
    val faculty: List<FacultyEntity>,
    val rooms: List<RoomEntity>,
    val departments: List<DepartmentEntity>
)

private data class AcademicInfo(
    val year: String?,
    val semester: String?
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    departmentRepository: DepartmentRepository,
    facultyRepository: FacultyRepository,
    subjectRepository: SubjectRepository,
    roomRepository: RoomRepository,
    sectionRepository: SectionRepository,
    timetableRepository: TimetableRepository,
    timeSlotRepository: TimeSlotRepository,
    academicYearDao: AcademicYearDao,
    semesterDao: SemesterDao,
    private val planner: AssignmentPlanner
) : ViewModel() {

    private val counts = combine(
        departmentRepository.getCountFlow(),
        facultyRepository.getCountFlow(),
        subjectRepository.getCountFlow(),
        roomRepository.getCountFlow(),
        sectionRepository.getCountFlow()
    ) { departments, faculty, subjects, rooms, sections ->
        Counts(departments, faculty, subjects, rooms, sections)
    }

    private val schedule = combine(
        timetableRepository.getAllEntries(),
        timeSlotRepository.getActiveTimeSlots(),
        subjectRepository.getActiveSubjects()
    ) { entries, slots, subjects ->
        ScheduleData(entries, slots, subjects)
    }

    private val people = combine(
        facultyRepository.getActiveFaculty(),
        roomRepository.getActiveRooms(),
        departmentRepository.getActiveDepartments()
    ) { faculty, rooms, departments ->
        PeopleData(faculty, rooms, departments)
    }

    private val academic = combine(
        academicYearDao.getCurrentAcademicYearFlow(),
        semesterDao.getActiveSemesters()
    ) { year, semesters ->
        AcademicInfo(year?.name, semesters.firstOrNull()?.name)
    }

    val state: StateFlow<DashboardState> = combine(
        counts,
        schedule,
        people,
        academic,
        sectionRepository.getActiveSections()
    ) { counts, schedule, people, academic, sections ->
        buildState(counts, schedule, people, academic, sections)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    private fun buildState(
        counts: Counts,
        schedule: ScheduleData,
        people: PeopleData,
        academic: AcademicInfo,
        sections: List<SectionEntity>
    ): DashboardState {
        val plan = planner.plan(sections, schedule.subjects, people.faculty, people.departments)
        val required = plan.assignments.sumOf { it.subject.requiredPeriods }

        val subjectMap = schedule.subjects.associateBy { it.id }
        val facultyMap = people.faculty.associateBy { it.id }
        val roomMap = people.rooms.associateBy { it.id }
        val sectionMap = sections.associateBy { it.id }
        val slotMap = schedule.slots.associateBy { it.id }

        val today = isoDayOfWeek()
        val todayClasses = schedule.entries
            .filter { it.dayOfWeek == today }
            .mapNotNull { entry ->
                val slot = slotMap[entry.timeSlotId] ?: return@mapNotNull null
                DashboardClass(
                    entry = entry,
                    subjectName = subjectMap[entry.subjectId]?.name ?: "Unknown subject",
                    subjectCode = subjectMap[entry.subjectId]?.code ?: "",
                    subjectType = subjectMap[entry.subjectId]?.type?.name ?: "THEORY",
                    facultyName = facultyMap[entry.facultyId]?.name ?: "Unassigned",
                    roomName = roomMap[entry.roomId]?.name ?: "Room",
                    sectionName = sectionMap[entry.sectionId]?.name ?: "Section",
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    periodNumber = slot.periodNumber
                )
            }
            .sortedBy { it.periodNumber }

        val issues = mutableListOf<String>()
        if (sections.isEmpty()) issues += "No active sections"
        if (schedule.subjects.isEmpty()) issues += "No active subjects"
        if (people.faculty.isEmpty()) issues += "No active faculty"
        if (people.rooms.none { it.type == RoomType.CLASSROOM }) issues += "No classroom"
        if (schedule.slots.none { it.type == SlotType.REGULAR }) issues += "No teaching periods"
        issues += plan.warnings

        return DashboardState(
            departmentCount = counts.departments,
            facultyCount = counts.faculty,
            subjectCount = counts.subjects,
            roomCount = counts.rooms,
            sectionCount = counts.sections,
            timetableCount = schedule.entries.size,
            requiredPeriods = required,
            labRoomCount = people.rooms.count { it.type == RoomType.LAB },
            classroomCount = people.rooms.count { it.type == RoomType.CLASSROOM },
            regularPeriodCount = schedule.slots.count { it.type == SlotType.REGULAR },
            todayClasses = todayClasses,
            todayLabel = dayName(today),
            todayIsTeachingDay = today in 1..5,
            academicYear = academic.year,
            semesterName = academic.semester,
            dataIssues = issues.distinct(),
            isLoading = false
        )
    }

    private fun isoDayOfWeek(): Int {
        val value = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return if (value == Calendar.SUNDAY) 7 else value - 1
    }

    companion object {
        fun dayName(day: Int): String = when (day) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Day $day"
        }
    }
}
