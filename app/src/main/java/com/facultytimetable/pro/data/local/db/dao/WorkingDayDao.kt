package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.WorkingDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkingDayDao {
    @Query("SELECT * FROM working_days ORDER BY dayOfWeek ASC")
    fun getAllWorkingDays(): Flow<List<WorkingDayEntity>>

    @Query("SELECT * FROM working_days WHERE isWorking = 1 ORDER BY dayOfWeek ASC")
    fun getWorkingDays(): Flow<List<WorkingDayEntity>>

    @Query("SELECT * FROM working_days WHERE id = :id")
    suspend fun getWorkingDayById(id: Long): WorkingDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(day: WorkingDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<WorkingDayEntity>)

    @Update
    suspend fun update(day: WorkingDayEntity)

    @Query("UPDATE working_days SET isWorking = :isWorking WHERE dayOfWeek = :dayOfWeek")
    suspend fun setWorking(dayOfWeek: Int, isWorking: Boolean)

    @Query("DELETE FROM working_days")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM working_days WHERE isWorking = 1")
    suspend fun getWorkingDayCount(): Int
}
