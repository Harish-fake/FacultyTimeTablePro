package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableEntryDao {

    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek ASC, timeSlotId ASC")
    fun getAllEntries(): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): TimetableEntryEntity?

    @Query("SELECT * FROM timetable_entries WHERE sectionId = :sectionId ORDER BY dayOfWeek ASC, timeSlotId ASC")
    fun getEntriesBySection(sectionId: Long): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE sectionId = :sectionId")
    suspend fun getEntriesBySectionList(sectionId: Long): List<TimetableEntryEntity>

    @Query("SELECT * FROM timetable_entries WHERE facultyId = :facultyId ORDER BY dayOfWeek ASC, timeSlotId ASC")
    fun getEntriesByFaculty(facultyId: Long): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE roomId = :roomId ORDER BY dayOfWeek ASC, timeSlotId ASC")
    fun getEntriesByRoom(roomId: Long): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :day ORDER BY timeSlotId ASC")
    fun getEntriesByDay(day: Int): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE facultyId = :facultyId AND dayOfWeek = :day")
    suspend fun getEntriesByFacultyAndDay(facultyId: Long, day: Int): List<TimetableEntryEntity>

    @Query("SELECT * FROM timetable_entries WHERE roomId = :roomId AND dayOfWeek = :day AND timeSlotId = :timeSlotId LIMIT 1")
    suspend fun getEntryByRoomDaySlot(roomId: Long, day: Int, timeSlotId: Long): TimetableEntryEntity?

    @Query("SELECT * FROM timetable_entries WHERE facultyId = :facultyId AND dayOfWeek = :day AND timeSlotId = :timeSlotId LIMIT 1")
    suspend fun getEntryByFacultyDaySlot(facultyId: Long, day: Int, timeSlotId: Long): TimetableEntryEntity?

    @Query("SELECT * FROM timetable_entries WHERE sectionId = :sectionId AND dayOfWeek = :day AND timeSlotId = :timeSlotId LIMIT 1")
    suspend fun getEntryBySectionDaySlot(sectionId: Long, day: Int, timeSlotId: Long): TimetableEntryEntity?

    @Query("SELECT COUNT(*) FROM timetable_entries WHERE facultyId = :facultyId")
    suspend fun getEntryCountByFaculty(facultyId: Long): Int

    @Query("SELECT COUNT(*) FROM timetable_entries WHERE roomId = :roomId")
    fun getEntryCountByRoom(roomId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: TimetableEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entries: List<TimetableEntryEntity>)

    @Update
    suspend fun update(entry: TimetableEntryEntity)

    @Delete
    suspend fun delete(entry: TimetableEntryEntity)

    @Query("DELETE FROM timetable_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM timetable_entries WHERE sectionId = :sectionId")
    suspend fun deleteBySection(sectionId: Long)

    @Query("DELETE FROM timetable_entries")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM timetable_entries")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM timetable_entries")
    fun getCountFlow(): Flow<Int>
}
