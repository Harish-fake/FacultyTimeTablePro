package com.facultytimetable.pro.data.repository

import com.facultytimetable.pro.data.local.db.dao.RecycleBinDao
import com.facultytimetable.pro.data.local.db.entity.RecycleBinEntity
import com.facultytimetable.pro.domain.repository.RecycleBinRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecycleBinRepositoryImpl @Inject constructor(
    private val dao: RecycleBinDao
) : RecycleBinRepository {

    override fun getAllItems(): Flow<List<RecycleBinEntity>> = dao.getAllItems()
    override fun getItemsByType(entityType: String): Flow<List<RecycleBinEntity>> = dao.getItemsByType(entityType)
    override suspend fun getItemById(id: Long): RecycleBinEntity? = dao.getItemById(id)
    override suspend fun insert(item: RecycleBinEntity): Long = dao.insert(item)
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override suspend fun deleteExpired() = dao.deleteExpired(System.currentTimeMillis())
    override suspend fun deleteAll() = dao.deleteAll()
    override fun getCountFlow(): Flow<Int> = dao.getCountFlow()
}
