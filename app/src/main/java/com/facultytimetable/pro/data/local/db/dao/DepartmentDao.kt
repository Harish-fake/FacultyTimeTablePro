package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DepartmentDao {

    @Query("SELECT * FROM departments ORDER BY name ASC")
    fun getAllDepartments(): Flow<List<DepartmentEntity>>

    @Query("SELECT * FROM departments WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveDepartments(): Flow<List<DepartmentEntity>>

    @Query("SELECT * FROM departments WHERE id = :id")
    suspend fun getDepartmentById(id: Long): DepartmentEntity?

    @Query("SELECT * FROM departments WHERE id = :id")
    fun getDepartmentByIdFlow(id: Long): Flow<DepartmentEntity?>

    @Query("SELECT * FROM departments WHERE code = :code LIMIT 1")
    suspend fun getDepartmentByCode(code: String): DepartmentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(department: DepartmentEntity): Long

    @Update
    suspend fun update(department: DepartmentEntity)

    @Delete
    suspend fun delete(department: DepartmentEntity)

    @Query("DELETE FROM departments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM departments")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM departments")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT * FROM departments WHERE name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%'")
    fun searchDepartments(query: String): Flow<List<DepartmentEntity>>
}
