package com.facultytimetable.pro.data.repository

import com.facultytimetable.pro.data.local.db.dao.TimetableEntryDao
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimetableRepositoryImpl @Inject constructor(
    private val timetableEntryDao: TimetableEntryDao
) : TimetableRepository {

    override fun getAllEntries(): Flow<List<TimetableEntryEntity>> = timetableEntryDao.getAllEntries()

    override suspend fun getEntryById(id: Long): TimetableEntryEntity? = timetableEntryDao.getEntryById(id)

    override fun getEntriesBySection(sectionId: Long): Flow<List<TimetableEntryEntity>> = timetableEntryDao.getEntriesBySection(sectionId)

    override suspend fun getEntriesBySectionList(sectionId: Long): List<TimetableEntryEntity> = timetableEntryDao.getEntriesBySectionList(sectionId)

    override fun getEntriesByFaculty(facultyId: Long): Flow<List<TimetableEntryEntity>> = timetableEntryDao.getEntriesByFaculty(facultyId)

    override fun getEntriesByRoom(roomId: Long): Flow<List<TimetableEntryEntity>> = timetableEntryDao.getEntriesByRoom(roomId)

    override fun getEntriesByDay(day: Int): Flow<List<TimetableEntryEntity>> = timetableEntryDao.getEntriesByDay(day)

    override suspend fun getEntriesByFacultyAndDay(facultyId: Long, day: Int): List<TimetableEntryEntity> = timetableEntryDao.getEntriesByFacultyAndDay(facultyId, day)

    override suspend fun getEntryByRoomDaySlot(roomId: Long, day: Int, timeSlotId: Long): TimetableEntryEntity? = timetableEntryDao.getEntryByRoomDaySlot(roomId, day, timeSlotId)

    override suspend fun getEntryByFacultyDaySlot(facultyId: Long, day: Int, timeSlotId: Long): TimetableEntryEntity? = timetableEntryDao.getEntryByFacultyDaySlot(facultyId, day, timeSlotId)

    override suspend fun getEntryBySectionDaySlot(sectionId: Long, day: Int, timeSlotId: Long): TimetableEntryEntity? = timetableEntryDao.getEntryBySectionDaySlot(sectionId, day, timeSlotId)

    override suspend fun getEntryCountByFaculty(facultyId: Long): Int = timetableEntryDao.getEntryCountByFaculty(facultyId)

    override fun getEntryCountByRoom(roomId: Long): Flow<Int> = timetableEntryDao.getEntryCountByRoom(roomId)

    override suspend fun insert(entry: TimetableEntryEntity): Long = timetableEntryDao.insert(entry)

    override suspend fun insertAll(entries: List<TimetableEntryEntity>) = timetableEntryDao.insertAll(entries)

    override suspend fun update(entry: TimetableEntryEntity) = timetableEntryDao.update(entry)

    override suspend fun delete(entry: TimetableEntryEntity) = timetableEntryDao.delete(entry)

    override suspend fun deleteById(id: Long) = timetableEntryDao.deleteById(id)

    override suspend fun deleteBySection(sectionId: Long) = timetableEntryDao.deleteBySection(sectionId)

    override suspend fun deleteAll() = timetableEntryDao.deleteAll()

    override suspend fun getCount(): Int = timetableEntryDao.getCount()

    override fun getCountFlow(): Flow<Int> = timetableEntryDao.getCountFlow()
}
