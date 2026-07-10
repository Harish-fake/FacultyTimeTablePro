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
        existingEntries: List<TimetableEntryEntity> = emptyList(),
        onProgress: (GenerationProgress) -> Unit = {}
    ): GenerationResult {
        val regularSlots = timeSlots.filter { it.type == SlotType.REGULAR }.sortedBy { it.periodNumber }
        val lunchSlots = timeSlots.filter { it.type == SlotType.LUNCH }
        val workingDays = timeSlots.map { it.dayOfWeek }.distinct().sorted()

        if (assignments.isEmpty()) {
            return GenerationResult(false, conflicts = listOf(ConflictReport(ConflictType.FACULTY_CLASH, "No teaching assignments to schedule", "Add faculty and subjects first")))
        }

        val total = assignments.size
        onProgress(GenerationProgress(0, total, "Starting generation..."))

        val lockedEntries = existingEntries.filter { it.isLocked }
        val lockedKeys = lockedEntries.map { "${it.facultyId}_${it.dayOfWeek}_${it.timeSlotId}" }.toSet()
        val lockedRoomKeys = lockedEntries.map { "${it.roomId}_${it.dayOfWeek}_${it.timeSlotId}" }.toSet()
        val lockedSectionKeys = lockedEntries.map { "${it.sectionId}_${it.dayOfWeek}_${it.timeSlotId}" }.toSet()

        val result = mutableListOf<TimetableEntryEntity>()
        val conflicts = mutableListOf<ConflictReport>()

        val facultySlotUsed = mutableSetOf<String>()
        val roomSlotUsed = mutableSetOf<String>()
        val sectionSlotUsed = mutableSetOf<String>()
        val facultyDailyCount = mutableMapOf<Long, MutableMap<Int, Int>>()

        facultySlotUsed.addAll(lockedKeys)
        roomSlotUsed.addAll(lockedRoomKeys)
        sectionSlotUsed.addAll(lockedSectionKeys)

        val sortedAssignments = assignments.sortedByDescending { it.subject.hoursPerWeek }

        for ((index, assignment) in sortedAssignments.withIndex()) {
            onProgress(GenerationProgress(index + 1, total, "Scheduling ${assignment.subject.name} for ${assignment.section.name}..."))

            val hoursNeeded = assignment.subject.hoursPerWeek
            var scheduled = 0
            val daysShuffled = workingDays.shuffled()

            val facultyUnavailableDays = assignment.faculty.unavailableDays
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .toSet()

            for (day in daysShuffled) {
                if (scheduled >= hoursNeeded) break
                if (day in facultyUnavailableDays) continue

                val daySlots = regularSlots.filter { it.dayOfWeek == day }
                if (daySlots.isEmpty()) continue

                val lunchSlot = lunchSlots.find { it.dayOfWeek == day }

                for (slot in daySlots) {
                    if (scheduled >= hoursNeeded) break
                    if (lunchSlot?.periodNumber == slot.periodNumber) continue

                    val facKey = "${assignment.faculty.id}_${day}_${slot.id}"
                    val secKey = "${assignment.section.id}_${day}_${slot.id}"

                    if (facKey in facultySlotUsed) continue
                    if (secKey in sectionSlotUsed) continue

                    val availableRoom = findAvailableRoom(rooms, assignment.subject, day, slot.id, roomSlotUsed)
                    if (availableRoom == null) continue

                    val dailyCount = facultyDailyCount.getOrPut(assignment.faculty.id) { mutableMapOf() }
                    if (dailyCount.getOrDefault(day, 0) >= 7) continue

                    val totalFacultySlots = facultySlotUsed.count { it.startsWith("${assignment.faculty.id}_") }
                    if (totalFacultySlots >= assignment.faculty.maxWeeklyHours) continue

                    val roomKey = "${availableRoom.id}_${day}_${slot.id}"
                    facultySlotUsed.add(facKey)
                    roomSlotUsed.add(roomKey)
                    sectionSlotUsed.add(secKey)
                    dailyCount[day] = dailyCount.getOrDefault(day, 0) + 1

                    val entry = TimetableEntryEntity(
                        sectionId = assignment.section.id,
                        subjectId = assignment.subject.id,
                        facultyId = assignment.faculty.id,
                        roomId = availableRoom.id,
                        timeSlotId = slot.id,
                        dayOfWeek = day,
                        weekType = WeekType.ALL,
                        isLocked = false
                    )
                    result.add(entry)
                    scheduled++
                }
            }

            if (scheduled < hoursNeeded) {
                conflicts.add(ConflictReport(ConflictType.FACULTY_CLASH, "Could only schedule $scheduled/$hoursNeeded hours for ${assignment.subject.name} (${assignment.section.name})", "Check faculty availability or add more time slots", facultyName = assignment.faculty.name, sectionName = assignment.section.name))
            }
        }

        onProgress(GenerationProgress(total, total, if (conflicts.isEmpty()) "Generation complete!" else "Completed with conflicts"))

        return GenerationResult(success = conflicts.isEmpty(), entries = result, conflicts = conflicts)
    }

    private fun findAvailableRoom(
        rooms: List<RoomEntity>,
        subject: SubjectEntity,
        day: Int,
        slotId: Long,
        usedRooms: MutableSet<String>
    ): RoomEntity? {
        val preferredType = if (subject.type == SubjectType.LAB) RoomType.LAB else RoomType.CLASSROOM
        val candidates = rooms.filter { it.type == preferredType || it.type == RoomType.CLASSROOM || it.type == RoomType.LECTURE_HALL || it.type == RoomType.SMART_CLASSROOM }
        for (room in candidates.shuffled()) {
            if ("${room.id}_${day}_${slotId}" !in usedRooms) return room
        }
        for (room in rooms.shuffled()) {
            if ("${room.id}_${day}_${slotId}" !in usedRooms) return room
        }
        return null
    }
}
