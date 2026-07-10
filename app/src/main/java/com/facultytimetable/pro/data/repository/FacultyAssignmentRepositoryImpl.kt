package com.facultytimetable.pro.data.repository

import com.facultytimetable.pro.data.local.db.dao.FacultyAssignmentDao
import com.facultytimetable.pro.data.local.db.entity.FacultyAssignmentEntity
import com.facultytimetable.pro.domain.repository.FacultyAssignmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacultyAssignmentRepositoryImpl @Inject constructor(
    private val dao: FacultyAssignmentDao
) : FacultyAssignmentRepository {

    override fun getAllAssignments(): Flow<List<FacultyAssignmentEntity>> = dao.getAllAssignments()
    override suspend fun getAssignmentById(id: Long): FacultyAssignmentEntity? = dao.getAssignmentById(id)
    override fun getAssignmentsByDepartment(departmentId: Long): Flow<List<FacultyAssignmentEntity>> = dao.getAssignmentsByDepartment(departmentId)
    override fun getAssignmentsBySemester(semesterId: Long): Flow<List<FacultyAssignmentEntity>> = dao.getAssignmentsBySemester(semesterId)
    override fun getAssignmentsByFaculty(facultyId: Long): Flow<List<FacultyAssignmentEntity>> = dao.getAssignmentsByFaculty(facultyId)
    override fun getAssignmentsBySubject(subjectId: Long): Flow<List<FacultyAssignmentEntity>> = dao.getAssignmentsBySubject(subjectId)
    override fun getAssignmentsByDeptAndSemester(deptId: Long, semId: Long): Flow<List<FacultyAssignmentEntity>> = dao.getAssignmentsByDeptAndSemester(deptId, semId)
    override suspend fun getTotalHoursByFacultyAndSemester(facultyId: Long, semesterId: Long): Int =
        dao.getTotalHoursByFacultyAndSemester(facultyId, semesterId) ?: 0
    override suspend fun insert(assignment: FacultyAssignmentEntity): Long = dao.insert(assignment)
    override suspend fun update(assignment: FacultyAssignmentEntity) = dao.update(assignment)
    override suspend fun delete(assignment: FacultyAssignmentEntity) = dao.delete(assignment)
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override suspend fun getCount(): Int = dao.getCount()
    override fun getCountFlow(): Flow<Int> = dao.getCountFlow()
}
