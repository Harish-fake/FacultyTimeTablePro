package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.LabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabDao {
    @Query("SELECT * FROM labs ORDER BY name ASC")
    fun getAllLabs(): Flow<List<LabEntity>>

    @Query("SELECT * FROM labs WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveLabs(): Flow<List<LabEntity>>

    @Query("SELECT * FROM labs WHERE id = :id")
    suspend fun getLabById(id: Long): LabEntity?

    @Query("SELECT * FROM labs WHERE departmentId = :departmentId ORDER BY name ASC")
    fun getLabsByDepartment(departmentId: Long): Flow<List<LabEntity>>

    @Query("SELECT * FROM labs WHERE name LIKE '%' || :query || '%' OR building LIKE '%' || :query || '%'")
    fun searchLabs(query: String): Flow<List<LabEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(lab: LabEntity): Long

    @Update
    suspend fun update(lab: LabEntity)

    @Delete
    suspend fun delete(lab: LabEntity)

    @Query("DELETE FROM labs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM labs")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM labs")
    fun getCountFlow(): Flow<Int>
}
