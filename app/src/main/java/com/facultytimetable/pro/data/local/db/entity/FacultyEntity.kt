package com.facultytimetable.pro.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "faculty",
    foreignKeys = [
        ForeignKey(
            entity = DepartmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["departmentId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["departmentId"]),
        Index(value = ["email"], unique = true),
        Index(value = ["employeeId"], unique = true),
        Index(value = ["facultyCode"])
    ]
)
data class FacultyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String,
    val phone: String = "",
    val designation: String,
    val departmentId: Long,
    val qualification: String = "",
    val experience: Int = 0,
    val maxWeeklyHours: Int = 24,
    val photoUri: String = "",
    val employeeId: String = "",
    val facultyCode: String = "",
    val gender: String = "",
    val officeRoom: String = "",
    val preferredDays: String = "",
    val unavailableDays: String = "",
    val preferredTimeSlots: String = "",
    val labEligible: Boolean = false,
    val status: String = "Active",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
