package com.facultytimetable.pro.data.local.seed

import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.dao.DepartmentDao
import com.facultytimetable.pro.data.local.db.dao.FacultyDao
import com.facultytimetable.pro.data.local.db.dao.RoomDao
import com.facultytimetable.pro.data.local.db.dao.SectionDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.dao.SubjectDao
import com.facultytimetable.pro.data.local.db.dao.TimeSlotDao
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val academicYearDao: AcademicYearDao,
    private val departmentDao: DepartmentDao,
    private val facultyDao: FacultyDao,
    private val subjectDao: SubjectDao,
    private val roomDao: RoomDao,
    private val semesterDao: SemesterDao,
    private val sectionDao: SectionDao,
    private val timeSlotDao: TimeSlotDao
) {
    suspend fun seed() = withContext(Dispatchers.IO) {
        if (departmentDao.getCount() > 0) return@withContext

        val deptIds = insertDepartments()
        val facultyIds = insertFaculty(deptIds)
        val subjectIds = insertSubjects(deptIds)
        val roomIds = insertRooms()

        val yearId = insertAcademicYear()
        val semIds = insertSemesters(yearId)
        val sectionIds = insertSections(semIds)

        insertTimeSlots()
    }

    private suspend fun insertDepartments(): Map<String, Long> {
        val depts = listOf(
            DepartmentEntity(name = "Computer Science & Engineering", code = "CSE", headName = "Dr. Rajesh Kumar", description = "Department of Computer Science and Engineering offering B.Tech and M.Tech programs."),
            DepartmentEntity(name = "Electronics & Communication", code = "ECE", headName = "Dr. Priya Sharma", description = "Department of Electronics and Communication Engineering."),
            DepartmentEntity(name = "Mechanical Engineering", code = "ME", headName = "Prof. Suresh Patel", description = "Department of Mechanical Engineering.")
        )
        val ids = mutableMapOf<String, Long>()
        depts.forEach { ids[it.code] = departmentDao.insert(it) }
        return ids
    }

    private suspend fun insertFaculty(deptIds: Map<String, Long>): Map<String, Long> {
        val faculty = listOf(
            FacultyEntity(name = "Dr. Arun Kumar", email = "arun@college.edu", phone = "9876543210", designation = "Professor", departmentId = deptIds["CSE"]!!, qualification = "Ph.D., CSE", experience = 15, maxWeeklyHours = 24),
            FacultyEntity(name = "Prof. Neha Gupta", email = "neha@college.edu", phone = "9876543211", designation = "Associate Professor", departmentId = deptIds["CSE"]!!, qualification = "M.Tech, CSE", experience = 10, maxWeeklyHours = 22),
            FacultyEntity(name = "Dr. Amit Singh", email = "amit@college.edu", phone = "9876543212", designation = "Professor", departmentId = deptIds["ECE"]!!, qualification = "Ph.D., ECE", experience = 12, maxWeeklyHours = 24),
            FacultyEntity(name = "Prof. Sunita Reddy", email = "sunita@college.edu", phone = "9876543213", designation = "Assistant Professor", departmentId = deptIds["ECE"]!!, qualification = "M.Tech, ECE", experience = 8, maxWeeklyHours = 20),
            FacultyEntity(name = "Dr. Vikram Joshi", email = "vikram@college.edu", phone = "9876543214", designation = "Professor", departmentId = deptIds["ME"]!!, qualification = "Ph.D., ME", experience = 18, maxWeeklyHours = 22),
            FacultyEntity(name = "Prof. Anjali Deshmukh", email = "anjali@college.edu", phone = "9876543215", designation = "Associate Professor", departmentId = deptIds["ME"]!!, qualification = "M.Tech, ME", experience = 9, maxWeeklyHours = 24)
        )
        val ids = mutableMapOf<String, Long>()
        faculty.forEach { ids[it.email.substringBefore("@")] = facultyDao.insert(it) }
        return ids
    }

    private suspend fun insertSubjects(deptIds: Map<String, Long>): Map<String, Long> {
        val subjects = listOf(
            SubjectEntity(name = "Data Structures & Algorithms", code = "CS201", type = SubjectType.THEORY, departmentId = deptIds["CSE"]!!, hoursPerWeek = 4),
            SubjectEntity(name = "Database Management Systems", code = "CS202", type = SubjectType.THEORY, departmentId = deptIds["CSE"]!!, hoursPerWeek = 4),
            SubjectEntity(name = "Data Structures Lab", code = "CS251", type = SubjectType.LAB, departmentId = deptIds["CSE"]!!, hoursPerWeek = 0, labHoursPerWeek = 3, isLabRequired = true),
            SubjectEntity(name = "Digital Logic Design", code = "EC201", type = SubjectType.THEORY, departmentId = deptIds["ECE"]!!, hoursPerWeek = 4),
            SubjectEntity(name = "Analog Electronics", code = "EC202", type = SubjectType.THEORY, departmentId = deptIds["ECE"]!!, hoursPerWeek = 4),
            SubjectEntity(name = "Analog Lab", code = "EC251", type = SubjectType.LAB, departmentId = deptIds["ECE"]!!, hoursPerWeek = 0, labHoursPerWeek = 3, isLabRequired = true),
            SubjectEntity(name = "Engineering Mechanics", code = "ME201", type = SubjectType.THEORY, departmentId = deptIds["ME"]!!, hoursPerWeek = 4),
            SubjectEntity(name = "Thermodynamics", code = "ME202", type = SubjectType.THEORY, departmentId = deptIds["ME"]!!, hoursPerWeek = 4),
            SubjectEntity(name = "Mini Project", code = "CS290", type = SubjectType.PROJECT, departmentId = deptIds["CSE"]!!, hoursPerWeek = 2)
        )
        val ids = mutableMapOf<String, Long>()
        subjects.forEach { ids[it.code] = subjectDao.insert(it) }
        return ids
    }

    private suspend fun insertRooms(): Map<String, Long> {
        val rooms = listOf(
            RoomEntity(name = "CSE-101", capacity = 60, type = RoomType.CLASSROOM, building = "CS Block", floor = "1st", hasProjector = true, hasAC = true),
            RoomEntity(name = "ECE-102", capacity = 50, type = RoomType.CLASSROOM, building = "ECE Block", floor = "1st", hasProjector = true, hasAC = false),
            RoomEntity(name = "CS Lab-1", capacity = 30, type = RoomType.LAB, building = "CS Block", floor = "2nd", hasProjector = false, hasAC = true),
            RoomEntity(name = "Analog Lab", capacity = 25, type = RoomType.LAB, building = "ECE Block", floor = "2nd", hasProjector = false, hasAC = true),
            RoomEntity(name = "Seminar Hall", capacity = 100, type = RoomType.SEMINAR_HALL, building = "Main Block", floor = "Ground", hasProjector = true, hasAC = true)
        )
        val ids = mutableMapOf<String, Long>()
        rooms.forEach { ids[it.name] = roomDao.insert(it) }
        return ids
    }

    private suspend fun insertAcademicYear(): Long {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        cal.set(year, Calendar.JULY, 1, 0, 0, 0)
        val start = cal.timeInMillis
        cal.set(year + 1, Calendar.JUNE, 30, 23, 59, 59)
        val end = cal.timeInMillis
        return academicYearDao.insert(
            AcademicYearEntity(name = "$year-${year + 1}", startDate = start, endDate = end, isCurrent = true)
        )
    }

    private suspend fun insertSemesters(yearId: Long): Map<Int, Long> {
        val oddId = semesterDao.insert(
            SemesterEntity(name = "Odd Semester", academicYearId = yearId, semesterNumber = 1, isActive = true)
        )
        val evenId = semesterDao.insert(
            SemesterEntity(name = "Even Semester", academicYearId = yearId, semesterNumber = 2, isActive = false)
        )
        return mapOf(1 to oddId, 2 to evenId)
    }

    private suspend fun insertSections(semIds: Map<Int, Long>): Map<String, Long> {
        val sections = listOf(
            SectionEntity(name = "CSE 3A", semesterId = semIds[1]!!, strength = 55, isActive = true),
            SectionEntity(name = "CSE 3B", semesterId = semIds[1]!!, strength = 52, isActive = true),
            SectionEntity(name = "ECE 3A", semesterId = semIds[1]!!, strength = 48, isActive = true)
        )
        val ids = mutableMapOf<String, Long>()
        sections.forEach { ids[it.name] = sectionDao.insert(it) }
        return ids
    }

    private suspend fun insertTimeSlots() {
        val timeSlots = mutableListOf<TimeSlotEntity>()
        for (day in 1..5) {
            val periods = listOf(
                "08:30" to "09:30",
                "09:30" to "10:30",
                "10:30" to "11:30",
                "11:30" to "12:30",
                "12:30" to "13:30",
                "13:30" to "14:30",
                "14:30" to "15:30",
                "15:30" to "16:30"
            )
            periods.forEachIndexed { idx, (start, end) ->
                val periodNum = idx + 1
                val type = when (periodNum) {
                    5 -> SlotType.LUNCH
                    7 -> SlotType.BREAK
                    else -> SlotType.REGULAR
                }
                timeSlots.add(TimeSlotEntity(dayOfWeek = day, periodNumber = periodNum, startTime = start, endTime = end, type = type, isActive = true))
            }
        }
        timeSlotDao.insertAll(timeSlots)
    }
}
