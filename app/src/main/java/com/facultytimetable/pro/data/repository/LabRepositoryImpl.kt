package com.facultytimetable.pro.data.repository

import com.facultytimetable.pro.data.local.db.dao.LabDao
import com.facultytimetable.pro.data.local.db.entity.LabEntity
import com.facultytimetable.pro.domain.repository.LabRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LabRepositoryImpl @Inject constructor(
    private val labDao: LabDao
) : LabRepository {

    override fun getAllLabs(): Flow<List<LabEntity>> = labDao.getAllLabs()
    override fun getActiveLabs(): Flow<List<LabEntity>> = labDao.getActiveLabs()
    override suspend fun getLabById(id: Long): LabEntity? = labDao.getLabById(id)
    override fun getLabsByDepartment(departmentId: Long): Flow<List<LabEntity>> = labDao.getLabsByDepartment(departmentId)
    override fun searchLabs(query: String): Flow<List<LabEntity>> = labDao.searchLabs(query)
    override suspend fun insert(lab: LabEntity): Long = labDao.insert(lab)
    override suspend fun update(lab: LabEntity) = labDao.update(lab)
    override suspend fun delete(lab: LabEntity) = labDao.delete(lab)
    override suspend fun deleteById(id: Long) = labDao.deleteById(id)
    override suspend fun getCount(): Int = labDao.getCount()
    override fun getCountFlow(): Flow<Int> = labDao.getCountFlow()
}
