package com.facultytimetable.pro.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timetable_entries",
    foreignKeys = [
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FacultyEntity::class,
            parentColumns = ["id"],
            childColumns = ["facultyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
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
        Index(value = ["sectionId"]),
        Index(value = ["subjectId"]),
        Index(value = ["facultyId"]),
        Index(value = ["roomId"]),
        Index(value = ["timeSlotId"]),
        Index(value = ["facultyId", "timeSlotId", "dayOfWeek"]),
        Index(value = ["roomId", "timeSlotId", "dayOfWeek"]),
        Index(value = ["sectionId", "dayOfWeek", "timeSlotId"])
    ]
)
data class TimetableEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sectionId: Long,
    val subjectId: Long,
    val facultyId: Long,
    val roomId: Long,
    val timeSlotId: Long,
    val dayOfWeek: Int,
    val weekType: WeekType = WeekType.ALL,
    val color: Int = 0,
    val notes: String = "",
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class WeekType {
    ALL, ODD, EVEN
}
