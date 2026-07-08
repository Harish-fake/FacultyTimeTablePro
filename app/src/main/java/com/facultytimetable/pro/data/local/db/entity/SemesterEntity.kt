package com.facultytimetable.pro.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "semesters",
    foreignKeys = [
        ForeignKey(
            entity = AcademicYearEntity::class,
            parentColumns = ["id"],
            childColumns = ["academicYearId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["academicYearId"])]
)
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val academicYearId: Long,
    val semesterNumber: Int,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
