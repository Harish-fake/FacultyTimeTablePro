package com.facultytimetable.pro.data.repository

import com.facultytimetable.pro.data.local.db.dao.DepartmentDao
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DepartmentRepositoryImpl @Inject constructor(
    private val departmentDao: DepartmentDao
) : DepartmentRepository {

    override fun getAllDepartments(): Flow<List<DepartmentEntity>> = departmentDao.getAllDepartments()

    override fun getActiveDepartments(): Flow<List<DepartmentEntity>> = departmentDao.getActiveDepartments()

    override suspend fun getDepartmentById(id: Long): DepartmentEntity? = departmentDao.getDepartmentById(id)

    override fun getDepartmentByIdFlow(id: Long): Flow<DepartmentEntity?> = departmentDao.getDepartmentByIdFlow(id)

    override suspend fun getDepartmentByCode(code: String): DepartmentEntity? = departmentDao.getDepartmentByCode(code)

    override suspend fun insert(department: DepartmentEntity): Long = departmentDao.insert(department)

    override suspend fun update(department: DepartmentEntity) = departmentDao.update(department)

    override suspend fun delete(department: DepartmentEntity) = departmentDao.delete(department)

    override suspend fun getCount(): Int = departmentDao.getCount()

    override fun getCountFlow(): Flow<Int> = departmentDao.getCountFlow()

    override fun searchDepartments(query: String): Flow<List<DepartmentEntity>> = departmentDao.searchDepartments(query)
}
