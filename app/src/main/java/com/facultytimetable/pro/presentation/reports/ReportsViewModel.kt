package com.facultytimetable.pro.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.RoomType
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.data.model.ConflictReport
import com.facultytimetable.pro.data.model.ConflictType
import com.facultytimetable.pro.data.model.FacultyWorkload
import com.facultytimetable.pro.data.model.RoomUtilization
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.RoomRepository
import com.facultytimetable.pro.domain.repository.SectionRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import com.facultytimetable.pro.domain.repository.TimeSlotRepository
import com.facultytimetable.pro.domain.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsState(
    val isLoading: Boolean = true,
    val selectedReport: ReportType? = null,
    val facultyWorkload: List<FacultyWorkload> = emptyList(),
    val departmentSummaries: List<DepartmentSummary> = emptyList(),
    val subjectAllocations: List<SubjectAllocation> = emptyList(),
    val roomUtilizations: List<RoomUtilization> = emptyList(),
    val labUtilizations: List<RoomUtilization> = emptyList(),
    val freePeriods: List<FreePeriod> = emptyList(),
    val missingHours: List<MissingHour> = emptyList(),
    val conflicts: List<ConflictReport> = emptyList(),
    val error: String? = null
)

data class DepartmentSummary(
    val departmentName: String,
    val departmentCode: String,
    val headName: String,
    val facultyCount: Int,
    val subjectCount: Int
)

data class SubjectAllocation(
    val departmentName: String,
    val departmentCode: String,
    val totalSubjects: Int,
    val theoryCount: Int,
    val labCount: Int,
    val projectCount: Int,
    val seminarCount: Int,
    val subjectNames: List<String>
)

data class FreePeriod(
    val dayOfWeek: Int,
    val periodNumber: Int,
    val startTime: String,
    val endTime: String,
    val dayName: String
)

data class MissingHour(
    val subjectName: String,
    val subjectCode: String,
    val departmentName: String,
    val requiredHours: Int,
    val allocatedHours: Int,
    val missingHours: Int
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val departmentRepository: DepartmentRepository,
    private val facultyRepository: FacultyRepository,
    private val subjectRepository: SubjectRepository,
    private val roomRepository: RoomRepository,
    private val timetableRepository: TimetableRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val sectionRepository: SectionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    init {
        loadAllData()
    }

    fun selectReport(type: ReportType) {
        _state.update { it.copy(selectedReport = if (it.selectedReport == type) null else type) }
    }

    fun refresh() {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val departments = departmentRepository.getAllDepartments().first()
                val facultyList = facultyRepository.getAllFaculty().first()
                val subjects = subjectRepository.getAllSubjects().first()
                val rooms = roomRepository.getAllRooms().first()
                val entries = timetableRepository.getAllEntries().first()
                val timeSlots = timeSlotRepository.getAllTimeSlots().first()
                val sections = sectionRepository.getAllSections().first()

                val deptMap = departments.associateBy { it.id }
                val facultyMap = facultyList.associateBy { it.id }
                val roomMap = rooms.associateBy { it.id }
                val sectionMap = sections.associateBy { it.id }
                val timeSlotMap = timeSlots.associateBy { it.id }

                val regularSlots = timeSlots.filter { it.type == SlotType.REGULAR }
                val totalTimeSlots = regularSlots.size

                val entriesByFaculty = entries.groupBy { it.facultyId }
                val entriesByRoom = entries.groupBy { it.roomId }
                val entriesBySubject = entries.groupBy { it.subjectId }

                val occupiedSlotSet = entries.map { it.dayOfWeek to it.timeSlotId }.toSet()

                val facultyWorkload = facultyList.map { f ->
                    val assigned = entriesByFaculty[f.id]?.size ?: 0
                    FacultyWorkload(
                        facultyId = f.id,
                        facultyName = f.name,
                        departmentName = deptMap[f.departmentId]?.name ?: "",
                        maxHours = f.maxWeeklyHours,
                        assignedHours = assigned,
                        utilizationPercent = if (f.maxWeeklyHours > 0)
                            (assigned.toFloat() / f.maxWeeklyHours * 100).coerceAtMost(100f) else 0f
                    )
                }

                val departmentSummaries = departments.map { dept ->
                    DepartmentSummary(
                        departmentName = dept.name,
                        departmentCode = dept.code,
                        headName = dept.headName,
                        facultyCount = facultyList.count { it.departmentId == dept.id },
                        subjectCount = subjects.count { it.departmentId == dept.id }
                    )
                }

                val subjectAllocations = departments.map { dept ->
                    val deptSubjects = subjects.filter { it.departmentId == dept.id }
                    SubjectAllocation(
                        departmentName = dept.name,
                        departmentCode = dept.code,
                        totalSubjects = deptSubjects.size,
                        theoryCount = deptSubjects.count { it.type == SubjectType.THEORY },
                        labCount = deptSubjects.count { it.type == SubjectType.LAB },
                        projectCount = deptSubjects.count { it.type == SubjectType.PROJECT },
                        seminarCount = deptSubjects.count { it.type == SubjectType.SEMINAR },
                        subjectNames = deptSubjects.map { "${it.name} (${it.code})" }
                    )
                }

                val roomUtilizations = rooms.map { r ->
                    val used = entriesByRoom[r.id]?.distinctBy { it.dayOfWeek to it.timeSlotId }?.size ?: 0
                    RoomUtilization(
                        roomId = r.id,
                        roomName = r.name,
                        totalSlots = totalTimeSlots,
                        usedSlots = used,
                        utilizationPercent = if (totalTimeSlots > 0)
                            (used.toFloat() / totalTimeSlots * 100).coerceAtMost(100f) else 0f
                    )
                }

                val labUtilizations = rooms.filter { it.type == RoomType.LAB }.map { r ->
                    val used = entriesByRoom[r.id]?.distinctBy { it.dayOfWeek to it.timeSlotId }?.size ?: 0
                    RoomUtilization(
                        roomId = r.id,
                        roomName = r.name,
                        totalSlots = totalTimeSlots,
                        usedSlots = used,
                        utilizationPercent = if (totalTimeSlots > 0)
                            (used.toFloat() / totalTimeSlots * 100).coerceAtMost(100f) else 0f
                    )
                }

                val freePeriods = regularSlots
                    .filter { (it.dayOfWeek to it.id) !in occupiedSlotSet }
                    .sortedBy { it.dayOfWeek * 100 + it.periodNumber }
                    .map { ts ->
                        FreePeriod(
                            dayOfWeek = ts.dayOfWeek,
                            periodNumber = ts.periodNumber,
                            startTime = ts.startTime,
                            endTime = ts.endTime,
                            dayName = getDayName(ts.dayOfWeek)
                        )
                    }

                val missingHours = subjects.mapNotNull { s ->
                    val allocated = entriesBySubject[s.id]?.size ?: 0
                    val required = s.hoursPerWeek + s.labHoursPerWeek
                    if (allocated < required) {
                        MissingHour(
                            subjectName = s.name,
                            subjectCode = s.code,
                            departmentName = deptMap[s.departmentId]?.name ?: "",
                            requiredHours = required,
                            allocatedHours = allocated,
                            missingHours = required - allocated
                        )
                    } else null
                }

                val conflicts = detectConflicts(
                    entries, facultyMap, roomMap, sectionMap, timeSlotMap, facultyWorkload
                )

                _state.update {
                    it.copy(
                        isLoading = false,
                        facultyWorkload = facultyWorkload.sortedByDescending { w -> w.utilizationPercent },
                        departmentSummaries = departmentSummaries.sortedBy { d -> d.departmentName },
                        subjectAllocations = subjectAllocations.sortedBy { s -> s.departmentName },
                        roomUtilizations = roomUtilizations.sortedByDescending { r -> r.utilizationPercent },
                        labUtilizations = labUtilizations.sortedByDescending { r -> r.utilizationPercent },
                        freePeriods = freePeriods,
                        missingHours = missingHours.sortedByDescending { it.missingHours },
                        conflicts = conflicts
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load reports") }
            }
        }
    }

    private fun detectConflicts(
        entries: List<TimetableEntryEntity>,
        facultyMap: Map<Long, FacultyEntity>,
        roomMap: Map<Long, RoomEntity>,
        sectionMap: Map<Long, SectionEntity>,
        timeSlotMap: Map<Long, TimeSlotEntity>,
        workloads: List<FacultyWorkload>
    ): List<ConflictReport> {
        val conflicts = mutableListOf<ConflictReport>()

        entries.groupBy { Triple(it.facultyId, it.dayOfWeek, it.timeSlotId) }
            .filter { it.value.size > 1 }
            .forEach { (key, group) ->
                conflicts.add(
                    ConflictReport(
                        type = ConflictType.FACULTY_CLASH,
                        message = "Faculty scheduled in ${group.size} rooms simultaneously",
                        suggestion = "Reassign one entry to a different time slot",
                        facultyName = facultyMap[key.first]?.name ?: "",
                        dayOfWeek = key.second,
                        periodNumber = timeSlotMap[key.third]?.periodNumber ?: 0
                    )
                )
            }

        entries.groupBy { Triple(it.roomId, it.dayOfWeek, it.timeSlotId) }
            .filter { it.value.size > 1 }
            .forEach { (key, group) ->
                conflicts.add(
                    ConflictReport(
                        type = ConflictType.ROOM_CLASH,
                        message = "Room is double-booked for ${group.size} classes",
                        suggestion = "Move one class to a different room or time slot",
                        roomName = roomMap[key.first]?.name ?: "",
                        dayOfWeek = key.second,
                        periodNumber = timeSlotMap[key.third]?.periodNumber ?: 0
                    )
                )
            }

        entries.groupBy { Triple(it.sectionId, it.dayOfWeek, it.timeSlotId) }
            .filter { it.value.size > 1 }
            .forEach { (key, group) ->
                conflicts.add(
                    ConflictReport(
                        type = ConflictType.SECTION_CLASH,
                        message = "Section has ${group.size} classes at the same time",
                        suggestion = "Reschedule one class for this section",
                        sectionName = sectionMap[key.first]?.name ?: "",
                        dayOfWeek = key.second,
                        periodNumber = timeSlotMap[key.third]?.periodNumber ?: 0
                    )
                )
            }

        workloads.filter { it.utilizationPercent > 100f }.forEach { w ->
            conflicts.add(
                ConflictReport(
                    type = ConflictType.WORKLOAD_EXCEEDED,
                    message = "${w.facultyName} exceeds max hours (${w.assignedHours}/${w.maxHours})",
                    suggestion = "Reduce teaching load or increase max weekly hours",
                    facultyName = w.facultyName
                )
            )
        }

        return conflicts
    }

    companion object {
        fun getDayName(day: Int): String = when (day) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Day $day"
        }
    }
}
