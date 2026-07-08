package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.HolidayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {

    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun getAllHolidays(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays WHERE id = :id")
    suspend fun getHolidayById(id: Long): HolidayEntity?

    @Query("SELECT * FROM holidays WHERE date BETWEEN :startDate AND :endDate ORDER BY date")
    fun getHolidaysBetween(startDate: Long, endDate: Long): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays WHERE date = :date LIMIT 1")
    suspend fun getHolidayByDate(date: Long): HolidayEntity?

    @Query("SELECT * FROM holidays WHERE isRecurring = 1")
    fun getRecurringHolidays(): Flow<List<HolidayEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(holiday: HolidayEntity): Long

    @Update
    suspend fun update(holiday: HolidayEntity)

    @Delete
    suspend fun delete(holiday: HolidayEntity)

    @Query("DELETE FROM holidays WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM holidays")
    suspend fun getCount(): Int
}
