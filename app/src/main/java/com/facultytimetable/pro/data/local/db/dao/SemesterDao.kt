package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {

    @Query("SELECT * FROM semesters ORDER BY academicYearId DESC, semesterNumber ASC")
    fun getAllSemesters(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE id = :id")
    suspend fun getSemesterById(id: Long): SemesterEntity?

    @Query("SELECT * FROM semesters WHERE id = :id")
    fun getSemesterByIdFlow(id: Long): Flow<SemesterEntity?>

    @Query("SELECT * FROM semesters WHERE academicYearId = :yearId ORDER BY semesterNumber ASC")
    fun getSemestersByAcademicYear(yearId: Long): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE isActive = 1 ORDER BY semesterNumber ASC")
    fun getActiveSemesters(): Flow<List<SemesterEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(semester: SemesterEntity): Long

    @Update
    suspend fun update(semester: SemesterEntity)

    @Delete
    suspend fun delete(semester: SemesterEntity)

    @Query("DELETE FROM semesters WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM semesters")
    suspend fun getCount(): Int
}
