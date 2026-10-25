package com.facultytimetable.pro.data.local.seed

import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.RoomType
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import java.util.Calendar

/**
 * Single source of truth for the built-in demo data set.
 *
 * The same data is used by [DatabaseSeeder], by the “Reset demo data” action and by the
 * generator unit tests. Keeping it in one place guarantees that the dataset shown in the
 * app is exactly the dataset that is verified by tests.
 */
object SampleDataProvider {

    data class Bundle(
        val departments: List<DepartmentEntity>,
        val faculty: List<FacultyEntity>,
        val subjects: List<SubjectEntity>,
        val rooms: List<RoomEntity>,
        val academicYear: AcademicYearEntity,
        val semesters: List<SemesterEntity>,
        val sections: List<SectionEntity>,
        val timeSlots: List<TimeSlotEntity>
    )

    fun departments(): List<DepartmentEntity> = listOf(
        DepartmentEntity(
            name = "Computer Science & Engineering",
            code = "CSE",
            headName = "Dr. Rajesh Kumar",
            description = "Department of Computer Science and Engineering offering B.Tech and M.Tech programmes."
        ),
        DepartmentEntity(
            name = "Electronics & Communication",
            code = "ECE",
            headName = "Dr. Priya Sharma",
            description = "Department of Electronics and Communication Engineering."
        ),
        DepartmentEntity(
            name = "Mechanical Engineering",
            code = "ME",
            headName = "Prof. Suresh Patel",
            description = "Department of Mechanical Engineering."
        )
    )

    fun faculty(deptIds: Map<String, Long>): List<FacultyEntity> = listOf(
        FacultyEntity(name = "Dr. Arun Kumar", email = "arun.kumar@college.edu", phone = "9876543210",
            designation = "Professor", departmentId = deptIds.getValue("CSE"),
            qualification = "Ph.D., Computer Science", experience = 15, maxWeeklyHours = 24),
        FacultyEntity(name = "Prof. Neha Gupta", email = "neha.gupta@college.edu", phone = "9876543211",
            designation = "Associate Professor", departmentId = deptIds.getValue("CSE"),
            qualification = "M.Tech, Computer Science", experience = 10, maxWeeklyHours = 24),
        FacultyEntity(name = "Dr. Amit Singh", email = "amit.singh@college.edu", phone = "9876543212",
            designation = "Professor", departmentId = deptIds.getValue("ECE"),
            qualification = "Ph.D., Electronics", experience = 12, maxWeeklyHours = 24),
        FacultyEntity(name = "Prof. Sunita Reddy", email = "sunita.reddy@college.edu", phone = "9876543213",
            designation = "Assistant Professor", departmentId = deptIds.getValue("ECE"),
            qualification = "M.Tech, VLSI Design", experience = 8, maxWeeklyHours = 22),
        FacultyEntity(name = "Dr. Vikram Joshi", email = "vikram.joshi@college.edu", phone = "9876543214",
            designation = "Professor", departmentId = deptIds.getValue("ME"),
            qualification = "Ph.D., Thermal Engineering", experience = 18, maxWeeklyHours = 24),
        FacultyEntity(name = "Prof. Anjali Deshmukh", email = "anjali.deshmukh@college.edu", phone = "9876543215",
            designation = "Associate Professor", departmentId = deptIds.getValue("ME"),
            qualification = "M.Tech, Machine Design", experience = 9, maxWeeklyHours = 24)
    )

    fun subjects(deptIds: Map<String, Long>): List<SubjectEntity> = listOf(
        SubjectEntity(name = "Data Structures & Algorithms", code = "CS201", type = SubjectType.THEORY,
            departmentId = deptIds.getValue("CSE"), hoursPerWeek = 4),
        SubjectEntity(name = "Database Management Systems", code = "CS202", type = SubjectType.THEORY,
            departmentId = deptIds.getValue("CSE"), hoursPerWeek = 4),
        SubjectEntity(name = "Data Structures Lab", code = "CS251", type = SubjectType.LAB,
            departmentId = deptIds.getValue("CSE"), hoursPerWeek = 3, labHoursPerWeek = 3, isLabRequired = true),
        SubjectEntity(name = "Mini Project", code = "CS290", type = SubjectType.PROJECT,
            departmentId = deptIds.getValue("CSE"), hoursPerWeek = 2),

        SubjectEntity(name = "Digital Logic Design", code = "EC201", type = SubjectType.THEORY,
            departmentId = deptIds.getValue("ECE"), hoursPerWeek = 4),
        SubjectEntity(name = "Analog Electronics", code = "EC202", type = SubjectType.THEORY,
            departmentId = deptIds.getValue("ECE"), hoursPerWeek = 4),
        SubjectEntity(name = "Analog Electronics Lab", code = "EC251", type = SubjectType.LAB,
            departmentId = deptIds.getValue("ECE"), hoursPerWeek = 3, labHoursPerWeek = 3, isLabRequired = true),

        SubjectEntity(name = "Engineering Mechanics", code = "ME201", type = SubjectType.THEORY,
            departmentId = deptIds.getValue("ME"), hoursPerWeek = 4),
        SubjectEntity(name = "Thermodynamics", code = "ME202", type = SubjectType.THEORY,
            departmentId = deptIds.getValue("ME"), hoursPerWeek = 4),
        SubjectEntity(name = "Mechanics Lab", code = "ME251", type = SubjectType.LAB,
            departmentId = deptIds.getValue("ME"), hoursPerWeek = 3, labHoursPerWeek = 3, isLabRequired = true)
    )

    fun rooms(): List<RoomEntity> = listOf(
        RoomEntity(name = "CSE-101", capacity = 60, type = RoomType.CLASSROOM,
            building = "Computer Block", floor = "1st", hasProjector = true, hasAC = true),
        RoomEntity(name = "CSE-102", capacity = 60, type = RoomType.CLASSROOM,
            building = "Computer Block", floor = "1st", hasProjector = true, hasAC = true),
        RoomEntity(name = "ECE-101", capacity = 55, type = RoomType.CLASSROOM,
            building = "Electronics Block", floor = "2nd", hasProjector = true, hasAC = false),
        RoomEntity(name = "ME-101", capacity = 60, type = RoomType.CLASSROOM,
            building = "Mechanical Block", floor = "1st", hasProjector = true, hasAC = false),
        RoomEntity(name = "CSE Lab", capacity = 60, type = RoomType.LAB,
            building = "Computer Block", floor = "2nd", hasProjector = true, hasAC = true),
        RoomEntity(name = "ECE Lab", capacity = 55, type = RoomType.LAB,
            building = "Electronics Block", floor = "2nd", hasProjector = false, hasAC = true),
        RoomEntity(name = "ME Lab", capacity = 60, type = RoomType.LAB,
            building = "Mechanical Block", floor = "Ground", hasProjector = false, hasAC = false),
        RoomEntity(name = "Seminar Hall", capacity = 120, type = RoomType.SEMINAR_HALL,
            building = "Main Block", floor = "Ground", hasProjector = true, hasAC = true)
    )

    fun academicYear(now: Long = System.currentTimeMillis()): AcademicYearEntity {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val calendarYear = cal.get(Calendar.YEAR)
        val startYear = if (cal.get(Calendar.MONTH) >= Calendar.JULY) calendarYear else calendarYear - 1

        val start = Calendar.getInstance().apply {
            clear()
            set(startYear, Calendar.JULY, 1, 0, 0, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
            clear()
            set(startYear + 1, Calendar.JUNE, 30, 23, 59, 59)
        }.timeInMillis

        return AcademicYearEntity(
            name = "$startYear-${startYear + 1}",
            startDate = start,
            endDate = end,
            isCurrent = true
        )
    }

    /** Semester rows are created with temporary ids; the seeder rewrites them after insert. */
    fun semesters(yearId: Long): List<SemesterEntity> = listOf(
        SemesterEntity(name = "Odd Semester", academicYearId = yearId, semesterNumber = 1, isActive = true),
        SemesterEntity(name = "Even Semester", academicYearId = yearId, semesterNumber = 2, isActive = false)
    )

    fun sections(semesterId: Long, deptIds: Map<String, Long>): List<SectionEntity> = listOf(
        SectionEntity(name = "CSE 3A", semesterId = semesterId, departmentId = deptIds.getValue("CSE"), strength = 55),
        SectionEntity(name = "CSE 3B", semesterId = semesterId, departmentId = deptIds.getValue("CSE"), strength = 55),
        SectionEntity(name = "ECE 3A", semesterId = semesterId, departmentId = deptIds.getValue("ECE"), strength = 50),
        SectionEntity(name = "ME 3A", semesterId = semesterId, departmentId = deptIds.getValue("ME"), strength = 55)
    )

    fun timeSlots(): List<TimeSlotEntity> {
        val periods = listOf(
            "08:30" to "09:25",
            "09:25" to "10:20",
            "10:20" to "11:15",
            "11:15" to "12:10",
            "12:10" to "13:00",
            "13:00" to "13:55",
            "13:55" to "14:50",
            "14:50" to "15:45"
        )
        return buildList {
            for (day in 1..5) {
                periods.forEachIndexed { index, (start, end) ->
                    val period = index + 1
                    val type = when (period) {
                        5 -> SlotType.LUNCH
                        7 -> SlotType.BREAK
                        else -> SlotType.REGULAR
                    }
                    add(
                        TimeSlotEntity(
                            dayOfWeek = day,
                            periodNumber = period,
                            startTime = start,
                            endTime = end,
                            type = type,
                            isActive = true
                        )
                    )
                }
            }
        }
    }

    /**
     * Builds a complete, internally consistent in-memory data set. Department ids are
     * generated from 1..n and referenced by the returned entities, which is enough for
     * generator tests and for previews.
     */
    fun bundle(now: Long = System.currentTimeMillis()): Bundle {
        val departments = departments().mapIndexed { index, dept -> dept.copy(id = index + 1L) }
        val deptIds = departments.associate { it.code to it.id }
        val year = academicYear(now).copy(id = 1L)
        val semesters = semesters(year.id).mapIndexed { index, semester ->
            semester.copy(id = index + 1L)
        }
        return Bundle(
            departments = departments,
            faculty = faculty(deptIds).mapIndexed { index, member -> member.copy(id = index + 1L) },
            subjects = subjects(deptIds).mapIndexed { index, subject -> subject.copy(id = index + 1L) },
            rooms = rooms().mapIndexed { index, room -> room.copy(id = index + 1L) },
            academicYear = year,
            semesters = semesters,
            sections = sections(semesters.first().id, deptIds).mapIndexed { index, section ->
                section.copy(id = index + 1L)
            },
            timeSlots = timeSlots().mapIndexed { index, slot -> slot.copy(id = index + 1L) }
        )
    }
}
