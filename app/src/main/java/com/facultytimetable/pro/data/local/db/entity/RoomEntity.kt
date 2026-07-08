package com.facultytimetable.pro.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rooms",
    indices = [Index(value = ["name"], unique = true)]
)
data class RoomEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val capacity: Int = 60,
    val type: RoomType = RoomType.CLASSROOM,
    val building: String = "",
    val floor: String = "",
    val hasProjector: Boolean = true,
    val hasAC: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class RoomType {
    CLASSROOM, LAB, SEMINAR_HALL, AUDITORIUM, LIBRARY
}
