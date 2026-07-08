package com.facultytimetable.pro.di

import android.content.Context
import androidx.room.Room
import com.facultytimetable.pro.data.local.db.AppDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideDepartmentDao(db: AppDatabase): DepartmentDao = db.departmentDao()
    @Provides fun provideFacultyDao(db: AppDatabase): FacultyDao = db.facultyDao()
    @Provides fun provideSubjectDao(db: AppDatabase): SubjectDao = db.subjectDao()
    @Provides fun provideSectionDao(db: AppDatabase): SectionDao = db.sectionDao()
    @Provides fun provideRoomDao(db: AppDatabase): RoomDao = db.roomDao()
    @Provides fun provideTimeSlotDao(db: AppDatabase): TimeSlotDao = db.timeSlotDao()
    @Provides fun provideAcademicYearDao(db: AppDatabase): AcademicYearDao = db.academicYearDao()
    @Provides fun provideSemesterDao(db: AppDatabase): SemesterDao = db.semesterDao()
    @Provides fun provideTimetableEntryDao(db: AppDatabase): TimetableEntryDao = db.timetableEntryDao()
    @Provides fun provideFacultyLeaveDao(db: AppDatabase): FacultyLeaveDao = db.facultyLeaveDao()
    @Provides fun provideSubstituteFacultyDao(db: AppDatabase): SubstituteFacultyDao = db.substituteFacultyDao()
    @Provides fun provideHolidayDao(db: AppDatabase): HolidayDao = db.holidayDao()
    @Provides fun provideUserPreferenceDao(db: AppDatabase): UserPreferenceDao = db.userPreferenceDao()
    @Provides fun provideBackupHistoryDao(db: AppDatabase): BackupHistoryDao = db.backupHistoryDao()
    @Provides fun provideAuditLogDao(db: AppDatabase): AuditLogDao = db.auditLogDao()
}
