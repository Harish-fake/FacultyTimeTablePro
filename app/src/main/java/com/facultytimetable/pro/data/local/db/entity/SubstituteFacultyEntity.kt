package com.facultytimetable.pro.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "substitute_faculty",
    foreignKeys = [
        ForeignKey(
            entity = FacultyLeaveEntity::class,
            parentColumns = ["id"],
            childColumns = ["leaveId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FacultyEntity::class,
            parentColumns = ["id"],
            childColumns = ["substituteFacultyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TimeSlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["timeSlotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["leaveId"]),
        Index(value = ["substituteFacultyId"]),
        Index(value = ["timeSlotId"])
    ]
)
data class SubstituteFacultyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val leaveId: Long,
    val substituteFacultyId: Long,
    val timeSlotId: Long,
    val date: Long,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
