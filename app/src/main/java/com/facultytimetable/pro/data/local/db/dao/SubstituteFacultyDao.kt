package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.SubstituteFacultyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubstituteFacultyDao {

    @Query("SELECT * FROM substitute_faculty ORDER BY date DESC")
    fun getAllSubstitutes(): Flow<List<SubstituteFacultyEntity>>

    @Query("SELECT * FROM substitute_faculty WHERE id = :id")
    suspend fun getSubstituteById(id: Long): SubstituteFacultyEntity?

    @Query("SELECT * FROM substitute_faculty WHERE leaveId = :leaveId")
    suspend fun getSubstitutesByLeave(leaveId: Long): List<SubstituteFacultyEntity>

    @Query("SELECT * FROM substitute_faculty WHERE substituteFacultyId = :facultyId ORDER BY date DESC")
    fun getSubstitutesByFaculty(facultyId: Long): Flow<List<SubstituteFacultyEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(substitute: SubstituteFacultyEntity): Long

    @Update
    suspend fun update(substitute: SubstituteFacultyEntity)

    @Delete
    suspend fun delete(substitute: SubstituteFacultyEntity)
}
