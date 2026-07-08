package com.facultytimetable.pro.data.repository

import com.facultytimetable.pro.data.local.db.dao.SubjectDao
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectRepositoryImpl @Inject constructor(
    private val subjectDao: SubjectDao
) : SubjectRepository {

    override fun getAllSubjects(): Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()

    override fun getActiveSubjects(): Flow<List<SubjectEntity>> = subjectDao.getActiveSubjects()

    override suspend fun getSubjectById(id: Long): SubjectEntity? = subjectDao.getSubjectById(id)

    override fun getSubjectByIdFlow(id: Long): Flow<SubjectEntity?> = subjectDao.getSubjectByIdFlow(id)

    override fun getSubjectsByDepartment(departmentId: Long): Flow<List<SubjectEntity>> = subjectDao.getSubjectsByDepartment(departmentId)

    override suspend fun getSubjectByCode(code: String): SubjectEntity? = subjectDao.getSubjectByCode(code)

    override suspend fun insert(subject: SubjectEntity): Long = subjectDao.insert(subject)

    override suspend fun update(subject: SubjectEntity) = subjectDao.update(subject)

    override suspend fun delete(subject: SubjectEntity) = subjectDao.delete(subject)

    override suspend fun deleteById(id: Long) = subjectDao.deleteById(id)

    override suspend fun getCount(): Int = subjectDao.getCount()

    override fun getCountFlow(): Flow<Int> = subjectDao.getCountFlow()

    override fun searchSubjects(query: String): Flow<List<SubjectEntity>> = subjectDao.searchSubjects(query)
}
