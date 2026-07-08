package com.facultytimetable.pro.domain.repository

import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import kotlinx.coroutines.flow.Flow

interface DepartmentRepository {
    fun getAllDepartments(): Flow<List<DepartmentEntity>>
    fun getActiveDepartments(): Flow<List<DepartmentEntity>>
    suspend fun getDepartmentById(id: Long): DepartmentEntity?
    fun getDepartmentByIdFlow(id: Long): Flow<DepartmentEntity?>
    suspend fun getDepartmentByCode(code: String): DepartmentEntity?
    suspend fun insert(department: DepartmentEntity): Long
    suspend fun update(department: DepartmentEntity)
    suspend fun delete(department: DepartmentEntity)
    suspend fun getCount(): Int
    fun getCountFlow(): Flow<Int>
    fun searchDepartments(query: String): Flow<List<DepartmentEntity>>
}
