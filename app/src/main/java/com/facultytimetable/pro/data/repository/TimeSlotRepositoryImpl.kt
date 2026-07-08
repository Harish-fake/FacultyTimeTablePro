package com.facultytimetable.pro.data.repository

import com.facultytimetable.pro.data.local.db.dao.TimeSlotDao
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.domain.repository.TimeSlotRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeSlotRepositoryImpl @Inject constructor(
    private val timeSlotDao: TimeSlotDao
) : TimeSlotRepository {

    override fun getAllTimeSlots(): Flow<List<TimeSlotEntity>> = timeSlotDao.getAllTimeSlots()
    override suspend fun getTimeSlotById(id: Long): TimeSlotEntity? = timeSlotDao.getTimeSlotById(id)
    override fun getTimeSlotsByDay(day: Int): Flow<List<TimeSlotEntity>> = timeSlotDao.getTimeSlotsByDay(day)
    override fun getActiveTimeSlots(): Flow<List<TimeSlotEntity>> = timeSlotDao.getActiveTimeSlots()
    override suspend fun getRegularTimeSlotsByDay(day: Int): List<TimeSlotEntity> = timeSlotDao.getRegularTimeSlotsByDay(day)
    override suspend fun getLunchSlotByDay(day: Int): TimeSlotEntity? = timeSlotDao.getLunchSlotByDay(day)
    override suspend fun insert(timeSlot: TimeSlotEntity): Long = timeSlotDao.insert(timeSlot)
    override suspend fun insertAll(timeSlots: List<TimeSlotEntity>) = timeSlotDao.insertAll(timeSlots)
    override suspend fun update(timeSlot: TimeSlotEntity) = timeSlotDao.update(timeSlot)
    override suspend fun delete(timeSlot: TimeSlotEntity) = timeSlotDao.delete(timeSlot)
    override suspend fun deleteAll() = timeSlotDao.deleteAll()
    override suspend fun getCount(): Int = timeSlotDao.getCount()
}
