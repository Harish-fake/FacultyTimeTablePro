package com.facultytimetable.pro.data.repository

import com.facultytimetable.pro.data.local.db.dao.FacultyDao
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.domain.repository.FacultyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacultyRepositoryImpl @Inject constructor(
    private val facultyDao: FacultyDao
) : FacultyRepository {

    override fun getAllFaculty(): Flow<List<FacultyEntity>> = facultyDao.getAllFaculty()

    override fun getActiveFaculty(): Flow<List<FacultyEntity>> = facultyDao.getActiveFaculty()

    override suspend fun getFacultyById(id: Long): FacultyEntity? = facultyDao.getFacultyById(id)

    override fun getFacultyByIdFlow(id: Long): Flow<FacultyEntity?> = facultyDao.getFacultyByIdFlow(id)

    override fun getFacultyByDepartment(departmentId: Long): Flow<List<FacultyEntity>> = facultyDao.getFacultyByDepartment(departmentId)

    override fun getActiveFacultyByDepartment(departmentId: Long): Flow<List<FacultyEntity>> = facultyDao.getActiveFacultyByDepartment(departmentId)

    override suspend fun insert(faculty: FacultyEntity): Long = facultyDao.insert(faculty)

    override suspend fun update(faculty: FacultyEntity) = facultyDao.update(faculty)

    override suspend fun delete(faculty: FacultyEntity) = facultyDao.delete(faculty)

    override suspend fun deleteById(id: Long) = facultyDao.deleteById(id)

    override suspend fun getCount(): Int = facultyDao.getCount()

    override fun getCountFlow(): Flow<Int> = facultyDao.getCountFlow()

    override fun getFacultyCountByDepartment(departmentId: Long): Flow<Int> = facultyDao.getFacultyCountByDepartment(departmentId)

    override fun searchFaculty(query: String): Flow<List<FacultyEntity>> = facultyDao.searchFaculty(query)
}
