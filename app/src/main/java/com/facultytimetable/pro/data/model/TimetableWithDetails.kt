package com.facultytimetable.pro.data.model

import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity

data class TimetableWithDetails(
    val entry: TimetableEntryEntity,
    val subjectName: String,
    val subjectCode: String,
    val subjectType: String,
    val facultyName: String,
    val roomName: String,
    val sectionName: String,
    val semesterName: String,
    val startTime: String,
    val endTime: String,
    val periodNumber: Int
)

data class FacultyWithDepartment(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val designation: String,
    val departmentId: Long,
    val departmentName: String,
    val departmentCode: String,
    val qualification: String,
    val experience: Int,
    val maxWeeklyHours: Int,
    val photoUri: String,
    val isActive: Boolean,
    val currentWorkload: Int = 0
)

data class SubjectWithDepartment(
    val id: Long,
    val name: String,
    val code: String,
    val type: String,
    val departmentId: Long,
    val departmentName: String,
    val hoursPerWeek: Int,
    val labHoursPerWeek: Int,
    val isLabRequired: Boolean
)

data class RoomUtilization(
    val roomId: Long,
    val roomName: String,
    val totalSlots: Int,
    val usedSlots: Int,
    val utilizationPercent: Float
)

data class FacultyWorkload(
    val facultyId: Long,
    val facultyName: String,
    val departmentName: String,
    val maxHours: Int,
    val assignedHours: Int,
    val utilizationPercent: Float
)

data class ConflictReport(
    val type: ConflictType,
    val message: String,
    val suggestion: String,
    val facultyName: String = "",
    val roomName: String = "",
    val sectionName: String = "",
    val dayOfWeek: Int = 0,
    val periodNumber: Int = 0
)

enum class ConflictType {
    FACULTY_CLASH,
    ROOM_CLASH,
    LAB_CLASH,
    SECTION_CLASH,
    WORKLOAD_EXCEEDED,
    INVALID_LAB_CONTINUITY,
    ROOM_CAPACITY_EXCEEDED
}
