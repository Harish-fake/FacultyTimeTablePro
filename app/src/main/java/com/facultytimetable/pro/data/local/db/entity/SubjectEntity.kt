package com.facultytimetable.pro.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = DepartmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["departmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["departmentId"])
    ]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String,
    val type: SubjectType = SubjectType.THEORY,
    val departmentId: Long,
    val hoursPerWeek: Int = 4,
    val labHoursPerWeek: Int = 0,
    val isLabRequired: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class SubjectType {
    THEORY, LAB, PROJECT, SEMINAR, LIBRARY, SPORTS
}
