package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.facultytimetable.pro.data.local.db.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE entityType = :entityType AND entityId = :entityId ORDER BY timestamp DESC")
    fun getLogsForEntity(entityType: String, entityId: Long): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getLogsBetween(startTime: Long, endTime: Long): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs WHERE timestamp < :beforeTime")
    suspend fun deleteOldLogs(beforeTime: Long)
}
