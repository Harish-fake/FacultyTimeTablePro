package com.facultytimetable.pro.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "faculty_leave",
    foreignKeys = [
        ForeignKey(
            entity = FacultyEntity::class,
            parentColumns = ["id"],
            childColumns = ["facultyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["facultyId"])]
)
data class FacultyLeaveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val facultyId: Long,
    val leaveDate: Long,
    val startTime: String = "",
    val endTime: String = "",
    val reason: String = "",
    val isApproved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
