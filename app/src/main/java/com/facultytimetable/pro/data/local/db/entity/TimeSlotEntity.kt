package com.facultytimetable.pro.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "time_slots",
    indices = [Index(value = ["dayOfWeek", "periodNumber"], unique = true)]
)
data class TimeSlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayOfWeek: Int,
    val periodNumber: Int,
    val startTime: String,
    val endTime: String,
    val type: SlotType = SlotType.REGULAR,
    val isActive: Boolean = true
)

enum class SlotType {
    REGULAR, LUNCH, BREAK
}
