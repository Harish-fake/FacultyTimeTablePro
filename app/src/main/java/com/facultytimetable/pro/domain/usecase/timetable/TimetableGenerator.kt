package com.facultytimetable.pro.domain.usecase.timetable

import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.RoomType
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.data.local.db.entity.WeekType
import com.facultytimetable.pro.data.model.ConflictReport
import com.facultytimetable.pro.data.model.ConflictType
import javax.inject.Inject
import javax.inject.Singleton

data class GenerationProgress(
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val message: String = ""
)

data class GenerationResult(
    val success: Boolean,
    val entries: List<TimetableEntryEntity> = emptyList(),
    val conflicts: List<ConflictReport> = emptyList(),
    val progress: GenerationProgress = GenerationProgress()
)

data class TeachingAssignment(
    val subject: SubjectEntity,
    val section: SectionEntity,
    val faculty: FacultyEntity
)

data class SlotAssignment(
    val dayOfWeek: Int,
    val timeSlot: TimeSlotEntity,
    val room: RoomEntity
)

@Singleton
class TimetableGenerator @Inject constructor() {

    fun generate(
        assignments: List<TeachingAssignment>,
        timeSlots: List<TimeSlotEntity>,
        rooms: List<RoomEntity>,
        facultyList: List<FacultyEntity>,
        onProgress: (GenerationProgress) -> Unit = {}
    ): GenerationResult {
        val regularSlots = timeSlots.filter { it.type == SlotType.REGULAR }.sortedBy { it.periodNumber }
        val lunchSlots = timeSlots.filter { it.type == SlotType.LUNCH }
        val breakSlots = timeSlots.filter { it.type == SlotType.BREAK }

        val workingDays = timeSlots.map { it.dayOfWeek }.distinct().sorted()

        if (assignments.isEmpty()) {
            return GenerationResult(
                success = false,
                conflicts = listOf(
                    ConflictReport(ConflictType.FACULTY_CLASH, "No teaching assignments to schedule", "Add faculty and subjects first")
                )
            )
        }

        val total = assignments.size
        onProgress(GenerationProgress(0, total, "Starting generation..."))

        val result = mutableListOf<TimetableEntryEntity>()
        val conflicts = mutableListOf<ConflictReport>()

        // Track allocations to avoid clashes
        val facultySlotUsed = mutableSetOf<String>()  // "facultyId_day_slotId"
        val roomSlotUsed = mutableSetOf<String>()     // "roomId_day_slotId"
        val sectionSlotUsed = mutableSetOf<String>()  // "sectionId_day_slotId"
        val facultyDailyCount = mutableMapOf<Long, MutableMap<Int, Int>>()

        // Sort assignments: theory first (which may need to precede labs), then by hours
        val sortedAssignments = assignments.sortedByDescending { it.subject.hoursPerWeek }

        for ((index, assignment) in sortedAssignments.withIndex()) {
            onProgress(GenerationProgress(index + 1, total, "Scheduling ${assignment.subject.name} for ${assignment.section.name}..."))

            val hoursNeeded = assignment.subject.hoursPerWeek
            var scheduled = 0
            var attempts = 0
            val maxAttempts = 200

            // Try each day
            val daysShuffled = workingDays.shuffled()

            for (day in daysShuffled) {
                if (scheduled >= hoursNeeded) break

                val daySlots = regularSlots.filter { it.dayOfWeek == day }
                if (daySlots.isEmpty()) continue

                val lunchSlot = lunchSlots.find { it.dayOfWeek == day }

                for (slot in daySlots) {
                    if (scheduled >= hoursNeeded) break
                    attempts++
                    if (attempts > maxAttempts) break

                    // Skip lunch slot
                    if (lunchSlot?.periodNumber == slot.periodNumber) continue

                    val facKey = "${assignment.faculty.id}_${day}_${slot.id}"
                    val roomKey = "_${day}_${slot.id}"
                    val secKey = "${assignment.section.id}_${day}_${slot.id}"

                    // Find available room
                    val availableRoom = findAvailableRoom(rooms, assignment.subject, day, slot.id, roomSlotUsed)

                    if (facKey in facultySlotUsed) {
                        continue
                    }
                    if (secKey in sectionSlotUsed) {
                        continue
                    }
                    if (availableRoom == null) {
                        continue
                    }

                    // Check faculty daily limit (max 7 periods)
                    val dailyCount = facultyDailyCount.getOrPut(assignment.faculty.id) { mutableMapOf() }
                    val currentDayCount = dailyCount.getOrDefault(day, 0)
                    if (currentDayCount >= 7) continue

                    // Check faculty weekly max
                    val totalFacultySlots = facultySlotUsed.count { it.startsWith("${assignment.faculty.id}_") }
                    if (totalFacultySlots >= assignment.faculty.maxWeeklyHours) {
                        conflicts.add(
                            ConflictReport(
                                ConflictType.WORKLOAD_EXCEEDED,
                                "${assignment.faculty.name} would exceed ${assignment.faculty.maxWeeklyHours} hrs/week",
                                "Reduce workload or increase max hours",
                                facultyName = assignment.faculty.name
                            )
                        )
                        continue
                    }

                    // Allocate
                    val roomKey_specific = "${availableRoom.id}_${day}_${slot.id}"
                    facultySlotUsed.add(facKey)
                    roomSlotUsed.add(roomKey_specific)
                    sectionSlotUsed.add(secKey)
                    dailyCount[day] = currentDayCount + 1

                    val entry = TimetableEntryEntity(
                        sectionId = assignment.section.id,
                        subjectId = assignment.subject.id,
                        facultyId = assignment.faculty.id,
                        roomId = availableRoom.id,
                        timeSlotId = slot.id,
                        dayOfWeek = day,
                        weekType = WeekType.ALL
                    )
                    result.add(entry)
                    scheduled++
                }
            }

            if (scheduled < hoursNeeded) {
                conflicts.add(
                    ConflictReport(
                        ConflictType.FACULTY_CLASH,
                        "Could only schedule $scheduled/$hoursNeeded hours for ${assignment.subject.name} (${assignment.section.name})",
                        "Check faculty availability or add more time slots",
                        facultyName = assignment.faculty.name,
                        sectionName = assignment.section.name
                    )
                )
            }
        }

        onProgress(GenerationProgress(total, total, if (conflicts.isEmpty()) "Generation complete!" else "Completed with conflicts"))

        return GenerationResult(
            success = conflicts.isEmpty(),
            entries = result,
            conflicts = conflicts
        )
    }

    private fun findAvailableRoom(
        rooms: List<RoomEntity>,
        subject: SubjectEntity,
        day: Int,
        slotId: Long,
        usedRooms: MutableSet<String>
    ): RoomEntity? {
        val preferredType = if (subject.type == SubjectType.LAB) RoomType.LAB else RoomType.CLASSROOM
        val candidates = rooms.filter { it.type == preferredType || it.type == RoomType.CLASSROOM }
        val shuffled = candidates.shuffled()

        for (room in shuffled) {
            val key = "${room.id}_${day}_${slotId}"
            if (key !in usedRooms) {
                return room
            }
        }

        // Fallback: try any room
        for (room in rooms.shuffled()) {
            val key = "${room.id}_${day}_${slotId}"
            if (key !in usedRooms) {
                return room
            }
        }

        return null
    }
}
