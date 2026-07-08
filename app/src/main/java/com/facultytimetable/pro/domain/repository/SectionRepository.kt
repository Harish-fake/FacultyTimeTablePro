package com.facultytimetable.pro.domain.repository

import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import kotlinx.coroutines.flow.Flow

interface SectionRepository {
    fun getAllSections(): Flow<List<SectionEntity>>
    fun getActiveSections(): Flow<List<SectionEntity>>
    suspend fun getSectionById(id: Long): SectionEntity?
    fun getSectionByIdFlow(id: Long): Flow<SectionEntity?>
    fun getSectionsBySemester(semesterId: Long): Flow<List<SectionEntity>>
    suspend fun insert(section: SectionEntity): Long
    suspend fun update(section: SectionEntity)
    suspend fun delete(section: SectionEntity)
    suspend fun deleteById(id: Long)
    suspend fun getCount(): Int
}
