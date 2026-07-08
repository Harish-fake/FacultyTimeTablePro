package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.FacultyLeaveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FacultyLeaveDao {

    @Query("SELECT * FROM faculty_leave ORDER BY leaveDate DESC")
    fun getAllLeaves(): Flow<List<FacultyLeaveEntity>>

    @Query("SELECT * FROM faculty_leave WHERE id = :id")
    suspend fun getLeaveById(id: Long): FacultyLeaveEntity?

    @Query("SELECT * FROM faculty_leave WHERE facultyId = :facultyId ORDER BY leaveDate DESC")
    fun getLeavesByFaculty(facultyId: Long): Flow<List<FacultyLeaveEntity>>

    @Query("SELECT * FROM faculty_leave WHERE leaveDate BETWEEN :startDate AND :endDate ORDER BY leaveDate")
    fun getLeavesBetween(startDate: Long, endDate: Long): Flow<List<FacultyLeaveEntity>>

    @Query("SELECT * FROM faculty_leave WHERE leaveDate = :date")
    suspend fun getLeavesByDate(date: Long): List<FacultyLeaveEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(leave: FacultyLeaveEntity): Long

    @Update
    suspend fun update(leave: FacultyLeaveEntity)

    @Delete
    suspend fun delete(leave: FacultyLeaveEntity)

    @Query("DELETE FROM faculty_leave WHERE id = :id")
    suspend fun deleteById(id: Long)
}
