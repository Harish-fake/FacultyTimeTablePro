package com.facultytimetable.pro.domain.repository

import com.facultytimetable.pro.data.local.db.entity.LabEntity
import kotlinx.coroutines.flow.Flow

interface LabRepository {
    fun getAllLabs(): Flow<List<LabEntity>>
    fun getActiveLabs(): Flow<List<LabEntity>>
    suspend fun getLabById(id: Long): LabEntity?
    fun getLabsByDepartment(departmentId: Long): Flow<List<LabEntity>>
    fun searchLabs(query: String): Flow<List<LabEntity>>
    suspend fun insert(lab: LabEntity): Long
    suspend fun update(lab: LabEntity)
    suspend fun delete(lab: LabEntity)
    suspend fun deleteById(id: Long)
    suspend fun getCount(): Int
    fun getCountFlow(): Flow<Int>
}
