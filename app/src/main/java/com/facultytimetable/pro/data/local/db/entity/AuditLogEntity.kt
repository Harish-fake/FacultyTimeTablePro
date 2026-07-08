package com.facultytimetable.pro.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["timestamp"])]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val action: String,
    val entityType: String,
    val entityId: Long,
    val details: String = "",
    val performedBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
