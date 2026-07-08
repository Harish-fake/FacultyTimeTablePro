package com.facultytimetable.pro.domain.repository

import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.RoomType
import kotlinx.coroutines.flow.Flow

interface RoomRepository {
    fun getAllRooms(): Flow<List<RoomEntity>>
    fun getActiveRooms(): Flow<List<RoomEntity>>
    suspend fun getRoomById(id: Long): RoomEntity?
    fun getRoomByIdFlow(id: Long): Flow<RoomEntity?>
    fun getRoomsByType(type: RoomType): Flow<List<RoomEntity>>
    suspend fun getRoomByName(name: String): RoomEntity?
    suspend fun insert(room: RoomEntity): Long
    suspend fun update(room: RoomEntity)
    suspend fun delete(room: RoomEntity)
    suspend fun deleteById(id: Long)
    fun getCountFlow(): Flow<Int>
    suspend fun getCount(): Int
    fun getRoomCountByType(type: RoomType): Flow<Int>
    fun searchRooms(query: String): Flow<List<RoomEntity>>
}
