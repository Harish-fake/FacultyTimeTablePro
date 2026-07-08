package com.facultytimetable.pro.domain.repository

import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import kotlinx.coroutines.flow.Flow

interface TimetableRepository {
    fun getAllEntries(): Flow<List<TimetableEntryEntity>>
    suspend fun getEntryById(id: Long): TimetableEntryEntity?
    fun getEntriesBySection(sectionId: Long): Flow<List<TimetableEntryEntity>>
    suspend fun getEntriesBySectionList(sectionId: Long): List<TimetableEntryEntity>
    fun getEntriesByFaculty(facultyId: Long): Flow<List<TimetableEntryEntity>>
    fun getEntriesByRoom(roomId: Long): Flow<List<TimetableEntryEntity>>
    fun getEntriesByDay(day: Int): Flow<List<TimetableEntryEntity>>
    suspend fun getEntriesByFacultyAndDay(facultyId: Long, day: Int): List<TimetableEntryEntity>
    suspend fun getEntryByRoomDaySlot(roomId: Long, day: Int, timeSlotId: Long): TimetableEntryEntity?
    suspend fun getEntryByFacultyDaySlot(facultyId: Long, day: Int, timeSlotId: Long): TimetableEntryEntity?
    suspend fun getEntryBySectionDaySlot(sectionId: Long, day: Int, timeSlotId: Long): TimetableEntryEntity?
    suspend fun getEntryCountByFaculty(facultyId: Long): Int
    fun getEntryCountByRoom(roomId: Long): Flow<Int>
    suspend fun insert(entry: TimetableEntryEntity): Long
    suspend fun insertAll(entries: List<TimetableEntryEntity>)
    suspend fun update(entry: TimetableEntryEntity)
    suspend fun delete(entry: TimetableEntryEntity)
    suspend fun deleteById(id: Long)
    suspend fun deleteBySection(sectionId: Long)
    suspend fun deleteAll()
    suspend fun getCount(): Int
    fun getCountFlow(): Flow<Int>
}
