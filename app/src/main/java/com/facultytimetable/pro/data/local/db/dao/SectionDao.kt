package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {

    @Query("SELECT * FROM sections ORDER BY name ASC")
    fun getAllSections(): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveSections(): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE id = :id")
    suspend fun getSectionById(id: Long): SectionEntity?

    @Query("SELECT * FROM sections WHERE id = :id")
    fun getSectionByIdFlow(id: Long): Flow<SectionEntity?>

    @Query("SELECT * FROM sections WHERE semesterId = :semesterId ORDER BY name ASC")
    fun getSectionsBySemester(semesterId: Long): Flow<List<SectionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(section: SectionEntity): Long

    @Update
    suspend fun update(section: SectionEntity)

    @Delete
    suspend fun delete(section: SectionEntity)

    @Query("DELETE FROM sections WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM sections")
    suspend fun getCount(): Int
}
