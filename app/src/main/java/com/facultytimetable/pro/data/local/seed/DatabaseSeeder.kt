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
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.domain.usecase.timetable.AssignmentPlanner
import com.facultytimetable.pro.domain.usecase.timetable.TimetableGenerator
import com.facultytimetable.pro.domain.usecase.timetable.requiredPeriods
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome of a seed/reset run. The sample-data screen uses it to prove that the shipped
 * dataset produced the exact expected number of periods without conflicts.
 */
data class SectionCoverage(
    val name: String,
    val expectedPeriods: Int,
    val scheduledPeriods: Int
) {
    val isComplete: Boolean get() = expectedPeriods > 0 && scheduledPeriods == expectedPeriods
}

data class SeedResult(
    val departments: Int = 0,
    val faculty: Int = 0,
    val subjects: Int = 0,
    val rooms: Int = 0,
    val sections: Int = 0,
    val timeSlots: Int = 0,
    val expectedPeriods: Int = 0,
    val scheduledPeriods: Int = 0,
    val warnings: List<String> = emptyList(),
    val generated: Boolean = false,
    val coverage: List<SectionCoverage> = emptyList()
) {
    val isComplete: Boolean
        get() = generated && warnings.isEmpty() && scheduledPeriods > 0 && scheduledPeriods == expectedPeriods
}

private data class DemoTimetableResult(
    val expectedPeriods: Int = 0,
    val scheduledPeriods: Int = 0,
    val warnings: List<String> = emptyList(),
    val generated: Boolean = false,
    val coverage: List<SectionCoverage> = emptyList()
)

/**
 * Seeds the built-in demo data and produces a first, fully validated timetable.
 *
 * Each table is seeded independently and only when it is empty. Departments are matched by
 * code, so startup is idempotent and never duplicates the shipped data. The sample-data
 * workflow performs a full replacement under the same mutex.
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
    /** Serialises startup seeding and sample-data resets so they can never interleave. */
    private val seedMutex = Mutex()

    suspend fun seed(): SeedResult = withContext(Dispatchers.IO) {
        seedMutex.withLock { seedLocked() }
    }

    private suspend fun seedLocked(): SeedResult {
        val deptIds = ensureDepartments()
        ensureFaculty(deptIds)
        ensureSubjects(deptIds)
        ensureRooms()
        val yearId = ensureAcademicYear()
        val semesterId = ensureSemesters(yearId)
        ensureSections(semesterId, deptIds)
        ensureTimeSlots()
        val timetable = ensureDemoTimetable()

        return SeedResult(
            departments = departmentDao.getCount(),
            faculty = facultyDao.getCount(),
            subjects = subjectDao.getCount(),
            rooms = roomDao.getCount(),
            sections = sectionDao.getCount(),
            timeSlots = timeSlotDao.getCount(),
            expectedPeriods = timetable.expectedPeriods,
            scheduledPeriods = timetable.scheduledPeriods,
            warnings = timetable.warnings,
            generated = timetable.generated,
            coverage = timetable.coverage
        )
    }

    /**
     * Restores the shipped demo data set. Used by “Restore demo data” on the settings screen
     * and by the sample-data workflow. Preferences (theme, PIN, …) are intentionally preserved.
     */
    suspend fun resetToSampleData(): SeedResult = withContext(Dispatchers.IO) {
        seedMutex.withLock {
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
            seedLocked()
        }
    }

    /**
     * Generates the demo timetable once, so a fresh install opens with a complete,
     * conflict-free weekly schedule instead of an empty grid. Failures are non-fatal:
     * the user can still open the generator and see the exact reason.
     */
    private suspend fun ensureDemoTimetable(): DemoTimetableResult {
        val departments = departmentDao.getActiveDepartments().first()
        val sections = sectionDao.getActiveSections().first()
        val subjects = subjectDao.getActiveSubjects().first()
        val faculty = facultyDao.getActiveFaculty().first()
        val rooms = roomDao.getActiveRooms().first()
        val timeSlots = timeSlotDao.getActiveTimeSlots().first()

        val plan = planner.plan(sections, subjects, faculty, departments)
        val expected = plan.assignments.sumOf { it.subject.requiredPeriods }
        if (plan.assignments.isEmpty()) {
            return DemoTimetableResult(
                expectedPeriods = expected,
                warnings = plan.warnings.ifEmpty { listOf("The sample data has no teaching assignments") }
            )
        }

        fun coverageFor(entries: List<TimetableEntryEntity>): List<SectionCoverage> =
            sections.sortedBy { it.name }.map { section ->
                SectionCoverage(
                    name = section.name,
                    expectedPeriods = plan.assignments
                        .filter { it.section.id == section.id }
                        .sumOf { it.subject.requiredPeriods },
                    scheduledPeriods = entries.count { it.sectionId == section.id }
                )
            }

        if (timetableEntryDao.getCount() > 0) {
            val existing = timetableEntryDao.getAllEntriesList()
            val conflicts = generator.validate(plan.assignments, existing, timeSlots, rooms)
            return DemoTimetableResult(
                expectedPeriods = expected,
                scheduledPeriods = existing.size,
                warnings = (plan.warnings + conflicts.map { it.message }).distinct(),
                generated = existing.isNotEmpty(),
                coverage = coverageFor(existing)
            )
        }

        return runCatching {
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
            DemoTimetableResult(
                expectedPeriods = expected,
                scheduledPeriods = result.entries.size,
                warnings = (plan.warnings + result.conflicts.map { it.message }).distinct(),
                generated = result.entries.isNotEmpty(),
                coverage = coverageFor(result.entries)
            )
        }.getOrElse { error ->
            DemoTimetableResult(
                expectedPeriods = expected,
                warnings = listOf(error.message ?: "The sample timetable could not be generated")
            )
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
