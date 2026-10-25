package com.facultytimetable.pro.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.facultytimetable.pro.data.local.db.converter.Converters
import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.dao.AuditLogDao
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
import com.facultytimetable.pro.data.local.db.dao.UserPreferenceDao
import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
import com.facultytimetable.pro.data.local.db.entity.AuditLogEntity
import com.facultytimetable.pro.data.local.db.entity.BackupHistoryEntity
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyLeaveEntity
import com.facultytimetable.pro.data.local.db.entity.HolidayEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.SubstituteFacultyEntity
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.data.local.db.entity.UserPreferenceEntity

@Database(
    entities = [
        DepartmentEntity::class,
        FacultyEntity::class,
        SubjectEntity::class,
        SectionEntity::class,
        RoomEntity::class,
        TimeSlotEntity::class,
        AcademicYearEntity::class,
        SemesterEntity::class,
        TimetableEntryEntity::class,
        FacultyLeaveEntity::class,
        SubstituteFacultyEntity::class,
        HolidayEntity::class,
        UserPreferenceEntity::class,
        BackupHistoryEntity::class,
        AuditLogEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun departmentDao(): DepartmentDao
    abstract fun facultyDao(): FacultyDao
    abstract fun subjectDao(): SubjectDao
    abstract fun sectionDao(): SectionDao
    abstract fun roomDao(): RoomDao
    abstract fun timeSlotDao(): TimeSlotDao
    abstract fun academicYearDao(): AcademicYearDao
    abstract fun semesterDao(): SemesterDao
    abstract fun timetableEntryDao(): TimetableEntryDao
    abstract fun facultyLeaveDao(): FacultyLeaveDao
    abstract fun substituteFacultyDao(): SubstituteFacultyDao
    abstract fun holidayDao(): HolidayDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun backupHistoryDao(): BackupHistoryDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        const val DATABASE_NAME = "faculty_timetable_pro.db"

        /**
         * v2 adds the section's owning department and a database-level guarantee that a
         * section can never be double-booked in the same period.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sections ADD COLUMN departmentId INTEGER NOT NULL DEFAULT 0")

                // Clean up any historical duplicate rows before adding the unique index.
                db.execSQL(
                    "DELETE FROM timetable_entries WHERE id NOT IN (" +
                        "SELECT MIN(id) FROM timetable_entries " +
                        "GROUP BY sectionId, dayOfWeek, timeSlotId)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_timetable_entries_sectionId_timeSlotId_dayOfWeek " +
                        "ON timetable_entries (sectionId, timeSlotId, dayOfWeek)"
                )
            }
        }
    }
}
