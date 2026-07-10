package com.facultytimetable.pro.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DepartmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["departmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AcademicYearEntity::class,
            parentColumns = ["id"],
            childColumns = ["academicYearId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["semesterId"]),
        Index(value = ["departmentId"]),
        Index(value = ["academicYearId"])
    ]
)
data class SectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val semesterId: Long,
    val departmentId: Long = 0,
    val academicYearId: Long = 0,
    val strength: Int = 0,
    val classAdvisor: String = "",
    val roomId: Long = -1,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
