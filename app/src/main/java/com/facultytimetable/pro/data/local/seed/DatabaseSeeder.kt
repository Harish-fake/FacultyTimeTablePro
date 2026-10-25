package com.facultytimetable.pro.data.local.seed

import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.dao.BackupHistoryDao
import com.facultytimetable.pro.data.local.db.dao.DepartmentDao
import com.facultytimetable.pro.data.local.db.dao.FacultyDao
import com.facultytimetable.pro.data.local.db.dao.FacultyLeaveDao
import com.facultytimetable.pro.data.local.db.dao.HolidayDao
import com.facultytimetable.pro.data.local.db.dao.RoomDao
import com.facultytimetable.pro.data.local.db.dao.SectionDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.dao.SubjectDao
import com.facultytimetable.pro.data.local.db.dao.SubstituteFacultyDao
import com.facultytimetable.pro.data.local.db.dao.TimeSlotDao
import com.facultytimetable.pro.data.local.db.dao.TimetableEntryDao
import com.facultytimetable.pro.domain.usecase.timetable.AssignmentPlanner
import com.facultytimetable.pro.domain.usecase.timetable.TimetableGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the built-in demo data and produces a first, fully validated timetable.
 *
 * Each table is seeded independently and only when it is empty. This means a partially
 * populated database is repaired on the next launch instead of being left broken (the old
 * implementation returned early as soon as one department existed).
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val academicYearDao: AcademicYearDao,
    private val departmentDao: DepartmentDao,
    private val facultyDao: FacultyDao,
    private val subjectDao: SubjectDao,
    private val roomDao: RoomDao,
    private val semesterDao: SemesterDao,
    private val sectionDao: SectionDao,
    private val timeSlotDao: TimeSlotDao,
    private val timetableEntryDao: TimetableEntryDao,
    private val facultyLeaveDao: FacultyLeaveDao,
    private val substituteFacultyDao: SubstituteFacultyDao,
    private val holidayDao: HolidayDao,
    private val backupHistoryDao: BackupHistoryDao,
    private val planner: AssignmentPlanner,
    private val generator: TimetableGenerator
) {
    suspend fun seed() = withContext(Dispatchers.IO) {
        val deptIds = ensureDepartments()
        ensureFaculty(deptIds)
        ensureSubjects(deptIds)
        ensureRooms()
        val yearId = ensureAcademicYear()
        val semesterId = ensureSemesters(yearId)
        ensureSections(semesterId, deptIds)
        ensureTimeSlots()
        ensureDemoTimetable()
    }

    /**
     * Restores the shipped demo data set. Used by “Restore demo data” on the settings screen.
     * Preferences (theme, PIN, …) are intentionally preserved.
     */
    suspend fun resetToSampleData() = withContext(Dispatchers.IO) {
        // Delete children first so foreign keys are always satisfied.
        timetableEntryDao.deleteAll()
        substituteFacultyDao.deleteAll()
        facultyLeaveDao.deleteAll()
        holidayDao.deleteAll()
        sectionDao.deleteAll()
        semesterDao.deleteAll()
        subjectDao.deleteAll()
        facultyDao.deleteAll()
        academicYearDao.deleteAll()
        departmentDao.deleteAll()
        roomDao.deleteAll()
        timeSlotDao.deleteAll()
        backupHistoryDao.deleteAll()
        seed()
    }

    /**
     * Generates the demo timetable once, so a fresh install opens with a complete,
     * conflict-free weekly schedule instead of an empty grid. Failures are non-fatal:
     * the user can still open the generator and see the exact reason.
     */
    private suspend fun ensureDemoTimetable() {
        if (timetableEntryDao.getCount() > 0) return

        runCatching {
            val departments = departmentDao.getActiveDepartments().first()
            val sections = sectionDao.getActiveSections().first()
            val subjects = subjectDao.getActiveSubjects().first()
            val faculty = facultyDao.getActiveFaculty().first()
            val rooms = roomDao.getActiveRooms().first()
            val timeSlots = timeSlotDao.getActiveTimeSlots().first()

            val plan = planner.plan(sections, subjects, faculty, departments)
            if (plan.assignments.isEmpty()) return@runCatching

            val result = generator.generateBest(
                assignments = plan.assignments,
                timeSlots = timeSlots,
                rooms = rooms,
                facultyList = faculty,
                attempts = 5
            )
            if (result.entries.isNotEmpty()) {
                timetableEntryDao.replaceAll(result.entries)
            }
        }
    }

    private suspend fun ensureDepartments(): Map<String, Long> {
        val ids = mutableMapOf<String, Long>()
        SampleDataProvider.departments().forEach { department ->
            val existing = departmentDao.getDepartmentByCode(department.code)
            ids[department.code] = existing?.id ?: departmentDao.insert(department)
        }
        return ids
    }

    private suspend fun ensureFaculty(deptIds: Map<String, Long>) {
        if (facultyDao.getCount() > 0) return
        SampleDataProvider.faculty(deptIds).forEach { facultyDao.insert(it) }
    }

    private suspend fun ensureSubjects(deptIds: Map<String, Long>) {
        if (subjectDao.getCount() > 0) return
        SampleDataProvider.subjects(deptIds).forEach { subjectDao.insert(it) }
    }

    private suspend fun ensureRooms() {
        if (roomDao.getCount() > 0) return
        SampleDataProvider.rooms().forEach { roomDao.insert(it) }
    }

    private suspend fun ensureAcademicYear(): Long {
        academicYearDao.getCurrentAcademicYear()?.let { return it.id }
        academicYearDao.getAllAcademicYears().first().firstOrNull()?.let { return it.id }
        return academicYearDao.insert(SampleDataProvider.academicYear())
    }

    private suspend fun ensureSemesters(yearId: Long): Long {
        semesterDao.getActiveSemesters().first().firstOrNull()?.let { return it.id }
        semesterDao.getAllSemesters().first().firstOrNull()?.let { return it.id }
        val ids = SampleDataProvider.semesters(yearId).map { semesterDao.insert(it) }
        return ids.first()
    }

    private suspend fun ensureSections(semesterId: Long, deptIds: Map<String, Long>) {
        if (sectionDao.getCount() > 0) return
        SampleDataProvider.sections(semesterId, deptIds).forEach { sectionDao.insert(it) }
    }

    private suspend fun ensureTimeSlots() {
        if (timeSlotDao.getCount() > 0) return
        timeSlotDao.insertAll(SampleDataProvider.timeSlots())
    }
}
