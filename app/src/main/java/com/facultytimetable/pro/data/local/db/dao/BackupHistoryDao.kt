package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.facultytimetable.pro.data.local.db.entity.BackupHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupHistoryDao {

    @Query("SELECT * FROM backup_history ORDER BY createdAt DESC")
    fun getAllBackups(): Flow<List<BackupHistoryEntity>>

    @Query("SELECT * FROM backup_history WHERE id = :id")
    suspend fun getBackupById(id: Long): BackupHistoryEntity?

    @Query("SELECT * FROM backup_history WHERE isRestorePoint = 1 ORDER BY createdAt DESC")
    fun getRestorePoints(): Flow<List<BackupHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(backup: BackupHistoryEntity): Long

    @Query("DELETE FROM backup_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM backup_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM backup_history")
    suspend fun getCount(): Int
}
