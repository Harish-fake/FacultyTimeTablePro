package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicYearDao {

    @Query("SELECT * FROM academic_years ORDER BY startDate DESC")
    fun getAllAcademicYears(): Flow<List<AcademicYearEntity>>

    @Query("SELECT * FROM academic_years WHERE id = :id")
    suspend fun getAcademicYearById(id: Long): AcademicYearEntity?

    @Query("SELECT * FROM academic_years WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentAcademicYear(): AcademicYearEntity?

    @Query("SELECT * FROM academic_years WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentAcademicYearFlow(): Flow<AcademicYearEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(academicYear: AcademicYearEntity): Long

    @Update
    suspend fun update(academicYear: AcademicYearEntity)

    @Delete
    suspend fun delete(academicYear: AcademicYearEntity)

    @Query("UPDATE academic_years SET isCurrent = 0")
    suspend fun clearCurrentYear()

    @Query("UPDATE academic_years SET isCurrent = 1 WHERE id = :id")
    suspend fun setCurrentYear(id: Long)

    @Query("SELECT COUNT(*) FROM academic_years")
    suspend fun getCount(): Int
}
