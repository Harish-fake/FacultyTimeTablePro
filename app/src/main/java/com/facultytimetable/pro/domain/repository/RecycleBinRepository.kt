package com.facultytimetable.pro.domain.repository

import com.facultytimetable.pro.data.local.db.entity.RecycleBinEntity
import kotlinx.coroutines.flow.Flow

interface RecycleBinRepository {
    fun getAllItems(): Flow<List<RecycleBinEntity>>
    fun getItemsByType(entityType: String): Flow<List<RecycleBinEntity>>
    suspend fun getItemById(id: Long): RecycleBinEntity?
    suspend fun insert(item: RecycleBinEntity): Long
    suspend fun deleteById(id: Long)
    suspend fun deleteExpired()
    suspend fun deleteAll()
    fun getCountFlow(): Flow<Int>
}
