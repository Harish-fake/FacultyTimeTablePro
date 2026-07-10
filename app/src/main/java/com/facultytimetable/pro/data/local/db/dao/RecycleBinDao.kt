package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.facultytimetable.pro.data.local.db.entity.RecycleBinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecycleBinDao {
    @Query("SELECT * FROM recycle_bin ORDER BY deletedAt DESC")
    fun getAllItems(): Flow<List<RecycleBinEntity>>

    @Query("SELECT * FROM recycle_bin WHERE entityType = :entityType ORDER BY deletedAt DESC")
    fun getItemsByType(entityType: String): Flow<List<RecycleBinEntity>>

    @Query("SELECT * FROM recycle_bin WHERE id = :id")
    suspend fun getItemById(id: Long): RecycleBinEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: RecycleBinEntity): Long

    @Query("DELETE FROM recycle_bin WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recycle_bin WHERE expiresAt < :currentTime")
    suspend fun deleteExpired(currentTime: Long)

    @Query("DELETE FROM recycle_bin")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM recycle_bin")
    fun getCountFlow(): Flow<Int>
}
