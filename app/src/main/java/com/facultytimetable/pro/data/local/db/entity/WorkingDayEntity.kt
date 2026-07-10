package com.facultytimetable.pro.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "working_days")
data class WorkingDayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayOfWeek: Int,
    val dayName: String,
    val isWorking: Boolean = true,
    val isActive: Boolean = true
)
