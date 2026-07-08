package com.facultytimetable.pro.data.repository

import com.facultytimetable.pro.data.local.db.dao.SectionDao
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.domain.repository.SectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SectionRepositoryImpl @Inject constructor(
    private val sectionDao: SectionDao
) : SectionRepository {

    override fun getAllSections(): Flow<List<SectionEntity>> = sectionDao.getAllSections()
    override fun getActiveSections(): Flow<List<SectionEntity>> = sectionDao.getActiveSections()
    override suspend fun getSectionById(id: Long): SectionEntity? = sectionDao.getSectionById(id)
    override fun getSectionByIdFlow(id: Long): Flow<SectionEntity?> = sectionDao.getSectionByIdFlow(id)
    override fun getSectionsBySemester(semesterId: Long): Flow<List<SectionEntity>> = sectionDao.getSectionsBySemester(semesterId)
    override suspend fun insert(section: SectionEntity): Long = sectionDao.insert(section)
    override suspend fun update(section: SectionEntity) = sectionDao.update(section)
    override suspend fun delete(section: SectionEntity) = sectionDao.delete(section)
    override suspend fun deleteById(id: Long) = sectionDao.deleteById(id)
    override suspend fun getCount(): Int = sectionDao.getCount()
}
