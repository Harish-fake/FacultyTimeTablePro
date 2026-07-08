package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun getSubjectByIdFlow(id: Long): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE departmentId = :departmentId ORDER BY name ASC")
    fun getSubjectsByDepartment(departmentId: Long): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE code = :code LIMIT 1")
    suspend fun getSubjectByCode(code: String): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(subject: SubjectEntity): Long

    @Update
    suspend fun update(subject: SubjectEntity)

    @Delete
    suspend fun delete(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM subjects")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT * FROM subjects WHERE name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%'")
    fun searchSubjects(query: String): Flow<List<SubjectEntity>>
}
