package com.facultytimetable.pro.data.repository

import com.facultytimetable.pro.data.local.db.dao.RoomDao
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.RoomType
import com.facultytimetable.pro.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepositoryImpl @Inject constructor(
    private val roomDao: RoomDao
) : RoomRepository {

    override fun getAllRooms(): Flow<List<RoomEntity>> = roomDao.getAllRooms()

    override fun getActiveRooms(): Flow<List<RoomEntity>> = roomDao.getActiveRooms()

    override suspend fun getRoomById(id: Long): RoomEntity? = roomDao.getRoomById(id)

    override fun getRoomByIdFlow(id: Long): Flow<RoomEntity?> = roomDao.getRoomByIdFlow(id)

    override fun getRoomsByType(type: RoomType): Flow<List<RoomEntity>> = roomDao.getRoomsByType(type)

    override suspend fun getRoomByName(name: String): RoomEntity? = roomDao.getRoomByName(name)

    override suspend fun insert(room: RoomEntity): Long = roomDao.insert(room)

    override suspend fun update(room: RoomEntity) = roomDao.update(room)

    override suspend fun delete(room: RoomEntity) = roomDao.delete(room)

    override suspend fun deleteById(id: Long) = roomDao.deleteById(id)

    override suspend fun getCount(): Int = roomDao.getCount()

    override fun getRoomCountByType(type: RoomType): Flow<Int> = roomDao.getRoomCountByType(type)

    override fun searchRooms(query: String): Flow<List<RoomEntity>> = roomDao.searchRooms(query)
}
