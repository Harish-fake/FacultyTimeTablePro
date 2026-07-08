package com.facultytimetable.pro.domain.repository

import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import kotlinx.coroutines.flow.Flow

interface FacultyRepository {
    fun getAllFaculty(): Flow<List<FacultyEntity>>
    fun getActiveFaculty(): Flow<List<FacultyEntity>>
    suspend fun getFacultyById(id: Long): FacultyEntity?
    fun getFacultyByIdFlow(id: Long): Flow<FacultyEntity?>
    fun getFacultyByDepartment(departmentId: Long): Flow<List<FacultyEntity>>
    fun getActiveFacultyByDepartment(departmentId: Long): Flow<List<FacultyEntity>>
    suspend fun insert(faculty: FacultyEntity): Long
    suspend fun update(faculty: FacultyEntity)
    suspend fun delete(faculty: FacultyEntity)
    suspend fun deleteById(id: Long)
    suspend fun getCount(): Int
    fun getCountFlow(): Flow<Int>
    fun getFacultyCountByDepartment(departmentId: Long): Flow<Int>
    fun searchFaculty(query: String): Flow<List<FacultyEntity>>
}
