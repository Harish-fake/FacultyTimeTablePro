package com.facultytimetable.pro.di

import com.facultytimetable.pro.data.repository.DepartmentRepositoryImpl
import com.facultytimetable.pro.data.repository.FacultyAssignmentRepositoryImpl
import com.facultytimetable.pro.data.repository.FacultyRepositoryImpl
import com.facultytimetable.pro.data.repository.LabRepositoryImpl
import com.facultytimetable.pro.data.repository.RecycleBinRepositoryImpl
import com.facultytimetable.pro.data.repository.RoomRepositoryImpl
import com.facultytimetable.pro.data.repository.SectionRepositoryImpl
import com.facultytimetable.pro.data.repository.SubjectRepositoryImpl
import com.facultytimetable.pro.data.repository.TimeSlotRepositoryImpl
import com.facultytimetable.pro.data.repository.TimetableRepositoryImpl
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyAssignmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.LabRepository
import com.facultytimetable.pro.domain.repository.RecycleBinRepository
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.domain.repository.SectionRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import com.facultytimetable.pro.domain.repository.TimeSlotRepository
import com.facultytimetable.pro.domain.repository.TimetableRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDepartmentRepository(impl: DepartmentRepositoryImpl): DepartmentRepository

    @Binds
    @Singleton
    abstract fun bindFacultyRepository(impl: FacultyRepositoryImpl): FacultyRepository

    @Binds
    @Singleton
    abstract fun bindSubjectRepository(impl: SubjectRepositoryImpl): SubjectRepository

    @Binds
    @Singleton
    abstract fun bindRoomRepository(impl: RoomRepositoryImpl): RoomRepository

    @Binds
    @Singleton
    abstract fun bindTimetableRepository(impl: TimetableRepositoryImpl): TimetableRepository

    @Binds
    @Singleton
    abstract fun bindSectionRepository(impl: SectionRepositoryImpl): SectionRepository

    @Binds
    @Singleton
    abstract fun bindTimeSlotRepository(impl: TimeSlotRepositoryImpl): TimeSlotRepository

    @Binds
    @Singleton
    abstract fun bindLabRepository(impl: LabRepositoryImpl): LabRepository

    @Binds
    @Singleton
    abstract fun bindFacultyAssignmentRepository(impl: FacultyAssignmentRepositoryImpl): FacultyAssignmentRepository

    @Binds
    @Singleton
    abstract fun bindRecycleBinRepository(impl: RecycleBinRepositoryImpl): RecycleBinRepository
}
