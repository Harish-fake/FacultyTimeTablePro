package com.facultytimetable.pro.presentation.reports

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.DepartmentDao
import com.facultytimetable.pro.data.local.db.dao.FacultyDao
import com.facultytimetable.pro.data.local.db.dao.RoomDao
import com.facultytimetable.pro.data.local.db.dao.SectionDao
import com.facultytimetable.pro.data.local.db.dao.SubjectDao
import com.facultytimetable.pro.data.local.db.dao.TimeSlotDao
import com.facultytimetable.pro.data.local.db.dao.TimetableEntryDao
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class WorkloadRow(
    val facultyId: Long,
    val name: String,
    val department: String,
    val assigned: Int,
    val maxHours: Int
) {
    val percent: Float get() = if (maxHours <= 0) 0f else (assigned.toFloat() / maxHours).coerceIn(0f, 1f)
    val overloaded: Boolean get() = maxHours > 0 && assigned > maxHours
}

data class RoomUsageRow(
    val roomId: Long,
    val name: String,
    val type: String,
    val used: Int,
    val available: Int
) {
    val percent: Float get() = if (available <= 0) 0f else (used.toFloat() / available).coerceIn(0f, 1f)
}

data class SectionCoverageRow(
    val sectionId: Long,
    val name: String,
    val covered: Int,
    val available: Int
) {
    val percent: Float get() = if (available <= 0) 0f else (covered.toFloat() / available).coerceIn(0f, 1f)
}

data class DepartmentSummaryRow(
    val code: String,
    val name: String,
    val sections: Int,
    val faculty: Int,
    val periods: Int
)

data class ReportsState(
    val workload: List<WorkloadRow> = emptyList(),
    val rooms: List<RoomUsageRow> = emptyList(),
    val sections: List<SectionCoverageRow> = emptyList(),
    val departments: List<DepartmentSummaryRow> = emptyList(),
    val totalPeriods: Int = 0,
    val facultyCount: Int = 0,
    val roomCount: Int = 0,
    val sectionCount: Int = 0,
    val isLoading: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

private data class RawReportData(
    val entries: List<TimetableEntryEntity>,
    val faculty: List<FacultyEntity>,
    val rooms: List<RoomEntity>,
    val sections: List<SectionEntity>,
    val subjects: List<SubjectEntity>
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    timetableEntryDao: TimetableEntryDao,
    facultyDao: FacultyDao,
    roomDao: RoomDao,
    sectionDao: SectionDao,
    subjectDao: SubjectDao,
    departmentDao: DepartmentDao,
    timeSlotDao: TimeSlotDao
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    private val rawData = combine(
        timetableEntryDao.getAllEntries(),
        facultyDao.getAllFaculty(),
        roomDao.getAllRooms(),
        sectionDao.getAllSections(),
        subjectDao.getAllSubjects()
    ) { entries, faculty, rooms, sections, subjects ->
        RawReportData(entries, faculty, rooms, sections, subjects)
    }

    private val calendarData = combine(
        departmentDao.getAllDepartments(),
        timeSlotDao.getAllTimeSlots()
    ) { departments, slots -> departments to slots }

    val state: StateFlow<ReportsState> = combine(
        rawData,
        calendarData,
        message,
        error
    ) { raw, calendar, msg, err ->
        buildState(raw, calendar.first, calendar.second, msg, err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsState())

    fun consumeMessage() { message.value = null }
    fun consumeError() { error.value = null }

    fun exportCsv(uri: Uri) {
        val current = state.value
        viewModelScope.launch {
            try {
                val csv = withContext(Dispatchers.Default) { buildCsv(current) }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                        stream.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Could not open the selected file")
                }
                message.value = "Report exported"
            } catch (e: Exception) {
                error.value = e.message ?: "Export failed"
            }
        }
    }

    companion object {
        fun suggestedFileName(): String {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
            return "timetable-report-$stamp.csv"
        }
    }

    private fun buildState(
        raw: RawReportData,
        departments: List<DepartmentEntity>,
        slots: List<TimeSlotEntity>,
        message: String?,
        error: String?
    ): ReportsState {
        val deptById = departments.associateBy { it.id }
        val facultyById = raw.faculty.associateBy { it.id }
        val roomById = raw.rooms.associateBy { it.id }
        val sectionById = raw.sections.associateBy { it.id }
        val subjectById = raw.subjects.associateBy { it.id }

        val activeDays = slots.filter { it.isActive }.map { it.dayOfWeek }.distinct().size.coerceAtLeast(1)
        val regularPerDay = slots.count { it.isActive && it.type.name == "REGULAR" } / activeDays
        val weeklySlots = regularPerDay * activeDays

        val workload = raw.faculty.map { member ->
            WorkloadRow(
                facultyId = member.id,
                name = member.name,
                department = deptById[member.departmentId]?.code ?: "—",
                assigned = raw.entries.count { it.facultyId == member.id },
                maxHours = member.maxWeeklyHours
            )
        }.sortedWith(compareByDescending<WorkloadRow> { it.overloaded }.thenByDescending { it.assigned })

        val rooms = raw.rooms.map { room ->
            RoomUsageRow(
                roomId = room.id,
                name = room.name,
                type = room.type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                used = raw.entries.count { it.roomId == room.id },
                available = weeklySlots
            )
        }.sortedByDescending { it.used }

        val sections = raw.sections.map { section ->
            SectionCoverageRow(
                sectionId = section.id,
                name = section.name,
                covered = raw.entries.count { it.sectionId == section.id },
                available = weeklySlots
            )
        }.sortedBy { it.name }

        val departmentRows = departments.map { dept ->
            DepartmentSummaryRow(
                code = dept.code,
                name = dept.name,
                sections = raw.sections.count { section ->
                    section.departmentId == dept.id ||
                        (section.departmentId == 0L && section.name.startsWith(dept.code))
                },
                faculty = raw.faculty.count { it.departmentId == dept.id },
                periods = raw.entries.count { entry ->
                    val subject = subjectById[entry.subjectId]
                    subject?.departmentId == dept.id
                }
            )
        }.sortedBy { it.code }

        // Touch the maps so lookups stay consistent even when references are missing.
        raw.entries.forEach { entry ->
            facultyById[entry.facultyId]
            roomById[entry.roomId]
            sectionById[entry.sectionId]
        }

        return ReportsState(
            workload = workload,
            rooms = rooms,
            sections = sections,
            departments = departmentRows,
            totalPeriods = raw.entries.size,
            facultyCount = raw.faculty.size,
            roomCount = raw.rooms.size,
            sectionCount = raw.sections.size,
            isLoading = false,
            message = message,
            error = error
        )
    }

    private fun buildCsv(state: ReportsState): String = buildString {
        appendLine("Faculty TimeTable Pro - Report")
        appendLine("Generated,${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}")
        appendLine("Total periods,${state.totalPeriods}")
        appendLine("Faculty,${state.facultyCount}")
        appendLine("Rooms,${state.roomCount}")
        appendLine("Sections,${state.sectionCount}")
        appendLine()
        appendLine("FACULTY WORKLOAD")
        appendLine("Faculty,Department,Assigned Periods,Max Weekly Hours,Utilization")
        state.workload.forEach {
            appendLine(
                "${csvEscape(it.name)},${csvEscape(it.department)},${it.assigned},${it.maxHours},${(it.percent * 100).toInt()}%"
            )
        }
        appendLine()
        appendLine("ROOM UTILIZATION")
        appendLine("Room,Type,Assigned Periods,Available Periods,Utilization")
        state.rooms.forEach {
            appendLine(
                "${csvEscape(it.name)},${csvEscape(it.type)},${it.used},${it.available},${(it.percent * 100).toInt()}%"
            )
        }
        appendLine()
        appendLine("SECTION COVERAGE")
        appendLine("Section,Scheduled Periods,Available Periods,Coverage")
        state.sections.forEach {
            appendLine(
                "${csvEscape(it.name)},${it.covered},${it.available},${(it.percent * 100).toInt()}%"
            )
        }
        appendLine()
        appendLine("DEPARTMENT SUMMARY")
        appendLine("Code,Department,Sections,Faculty,Periods")
        state.departments.forEach {
            appendLine("${csvEscape(it.code)},${csvEscape(it.name)},${it.sections},${it.faculty},${it.periods}")
        }
    }

    private fun csvEscape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
}
