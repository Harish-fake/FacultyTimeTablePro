package com.facultytimetable.pro.domain.repository

import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import kotlinx.coroutines.flow.Flow

interface TimeSlotRepository {
    fun getAllTimeSlots(): Flow<List<TimeSlotEntity>>
    suspend fun getTimeSlotById(id: Long): TimeSlotEntity?
    fun getTimeSlotsByDay(day: Int): Flow<List<TimeSlotEntity>>
    fun getActiveTimeSlots(): Flow<List<TimeSlotEntity>>
    suspend fun getRegularTimeSlotsByDay(day: Int): List<TimeSlotEntity>
    suspend fun getLunchSlotByDay(day: Int): TimeSlotEntity?
    suspend fun insert(timeSlot: TimeSlotEntity): Long
    suspend fun insertAll(timeSlots: List<TimeSlotEntity>)
    suspend fun update(timeSlot: TimeSlotEntity)
    suspend fun delete(timeSlot: TimeSlotEntity)
    suspend fun deleteAll()
    suspend fun getCount(): Int
}
