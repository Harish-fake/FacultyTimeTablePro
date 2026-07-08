package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FacultyDao {

    @Query("SELECT * FROM faculty ORDER BY name ASC")
    fun getAllFaculty(): Flow<List<FacultyEntity>>

    @Query("SELECT * FROM faculty WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveFaculty(): Flow<List<FacultyEntity>>

    @Query("SELECT * FROM faculty WHERE id = :id")
    suspend fun getFacultyById(id: Long): FacultyEntity?

    @Query("SELECT * FROM faculty WHERE id = :id")
    fun getFacultyByIdFlow(id: Long): Flow<FacultyEntity?>

    @Query("SELECT * FROM faculty WHERE departmentId = :departmentId ORDER BY name ASC")
    fun getFacultyByDepartment(departmentId: Long): Flow<List<FacultyEntity>>

    @Query("SELECT * FROM faculty WHERE departmentId = :departmentId AND isActive = 1 ORDER BY name ASC")
    fun getActiveFacultyByDepartment(departmentId: Long): Flow<List<FacultyEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(faculty: FacultyEntity): Long

    @Update
    suspend fun update(faculty: FacultyEntity)

    @Delete
    suspend fun delete(faculty: FacultyEntity)

    @Query("DELETE FROM faculty WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM faculty")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM faculty")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM faculty WHERE departmentId = :departmentId")
    fun getFacultyCountByDepartment(departmentId: Long): Flow<Int>

    @Query("SELECT * FROM faculty WHERE name LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR designation LIKE '%' || :query || '%'")
    fun searchFaculty(query: String): Flow<List<FacultyEntity>>
}
