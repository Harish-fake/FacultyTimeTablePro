package com.facultytimetable.pro.domain.repository

import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

interface SubjectRepository {
    fun getAllSubjects(): Flow<List<SubjectEntity>>
    fun getActiveSubjects(): Flow<List<SubjectEntity>>
    suspend fun getSubjectById(id: Long): SubjectEntity?
    fun getSubjectByIdFlow(id: Long): Flow<SubjectEntity?>
    fun getSubjectsByDepartment(departmentId: Long): Flow<List<SubjectEntity>>
    suspend fun getSubjectByCode(code: String): SubjectEntity?
    suspend fun insert(subject: SubjectEntity): Long
    suspend fun update(subject: SubjectEntity)
    suspend fun delete(subject: SubjectEntity)
    suspend fun deleteById(id: Long)
    suspend fun getCount(): Int
    fun getCountFlow(): Flow<Int>
    fun searchSubjects(query: String): Flow<List<SubjectEntity>>
}
