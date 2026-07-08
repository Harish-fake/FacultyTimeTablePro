package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeSlotDao {

    @Query("SELECT * FROM time_slots ORDER BY dayOfWeek ASC, periodNumber ASC")
    fun getAllTimeSlots(): Flow<List<TimeSlotEntity>>

    @Query("SELECT * FROM time_slots WHERE id = :id")
    suspend fun getTimeSlotById(id: Long): TimeSlotEntity?

    @Query("SELECT * FROM time_slots WHERE dayOfWeek = :day ORDER BY periodNumber ASC")
    fun getTimeSlotsByDay(day: Int): Flow<List<TimeSlotEntity>>

    @Query("SELECT * FROM time_slots WHERE type = :type ORDER BY dayOfWeek, periodNumber")
    fun getTimeSlotsByType(type: SlotType): Flow<List<TimeSlotEntity>>

    @Query("SELECT * FROM time_slots WHERE isActive = 1 ORDER BY dayOfWeek, periodNumber")
    fun getActiveTimeSlots(): Flow<List<TimeSlotEntity>>

    @Query("SELECT * FROM time_slots WHERE isActive = 1 AND type = 'REGULAR' ORDER BY dayOfWeek, periodNumber")
    fun getRegularTimeSlots(): Flow<List<TimeSlotEntity>>

    @Query("SELECT * FROM time_slots WHERE dayOfWeek = :day AND isActive = 1 AND type = 'REGULAR' ORDER BY periodNumber")
    suspend fun getRegularTimeSlotsByDay(day: Int): List<TimeSlotEntity>

    @Query("SELECT * FROM time_slots WHERE type = 'LUNCH' AND dayOfWeek = :day LIMIT 1")
    suspend fun getLunchSlotByDay(day: Int): TimeSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timeSlot: TimeSlotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(timeSlots: List<TimeSlotEntity>)

    @Update
    suspend fun update(timeSlot: TimeSlotEntity)

    @Delete
    suspend fun delete(timeSlot: TimeSlotEntity)

    @Query("DELETE FROM time_slots")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM time_slots")
    suspend fun getCount(): Int
}
