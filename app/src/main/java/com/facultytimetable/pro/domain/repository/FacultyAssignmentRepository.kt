package com.facultytimetable.pro.domain.repository

import com.facultytimetable.pro.data.local.db.entity.FacultyAssignmentEntity
import kotlinx.coroutines.flow.Flow

interface FacultyAssignmentRepository {
    fun getAllAssignments(): Flow<List<FacultyAssignmentEntity>>
    suspend fun getAssignmentById(id: Long): FacultyAssignmentEntity?
    fun getAssignmentsByDepartment(departmentId: Long): Flow<List<FacultyAssignmentEntity>>
    fun getAssignmentsBySemester(semesterId: Long): Flow<List<FacultyAssignmentEntity>>
    fun getAssignmentsByFaculty(facultyId: Long): Flow<List<FacultyAssignmentEntity>>
    fun getAssignmentsBySubject(subjectId: Long): Flow<List<FacultyAssignmentEntity>>
    fun getAssignmentsByDeptAndSemester(deptId: Long, semId: Long): Flow<List<FacultyAssignmentEntity>>
    suspend fun getTotalHoursByFacultyAndSemester(facultyId: Long, semesterId: Long): Int
    suspend fun insert(assignment: FacultyAssignmentEntity): Long
    suspend fun update(assignment: FacultyAssignmentEntity)
    suspend fun delete(assignment: FacultyAssignmentEntity)
    suspend fun deleteById(id: Long)
    suspend fun getCount(): Int
    fun getCountFlow(): Flow<Int>
}
