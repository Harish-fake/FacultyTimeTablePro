package com.facultytimetable.pro.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_history")
data class BackupHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileSize: Long = 0,
    val checksum: String = "",
    val isRestorePoint: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
