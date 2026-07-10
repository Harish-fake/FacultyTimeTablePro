package com.facultytimetable.pro.domain.usecase.timetable

import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.facultytimetable.pro.data.model.ConflictReport
import com.facultytimetable.pro.data.model.ConflictType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConflictEngine @Inject constructor() {

    data class ValidationResult(
        val hasConflicts: Boolean,
        val conflicts: List<ConflictReport>,
        val suggestions: List<String>
    )

    fun validateEntry(
        entry: TimetableEntryEntity,
        existingEntries: List<TimetableEntryEntity>
    ): ValidationResult {
        val conflicts = mutableListOf<ConflictReport>()
        val suggestions = mutableListOf<String>()

        for (existing in existingEntries) {
            if (existing.id == entry.id) continue
            if (existing.dayOfWeek != entry.dayOfWeek || existing.timeSlotId != entry.timeSlotId) continue

            if (existing.facultyId == entry.facultyId && entry.facultyId != 0L) {
                conflicts.add(
                    ConflictReport(
                        type = ConflictType.FACULTY_CLASH,
                        message = "Faculty is already assigned to another class at this time",
                        suggestion = "Choose a different time slot or faculty member",
                        facultyName = "",
                        dayOfWeek = entry.dayOfWeek,
                        periodNumber = entry.timeSlotId.toInt()
                    )
                )
                suggestions.add("Try a different time slot")
            }

            if (existing.roomId == entry.roomId && entry.roomId != 0L) {
                conflicts.add(
                    ConflictReport(
                        type = ConflictType.ROOM_CLASH,
                        message = "Room is already booked at this time slot",
                        suggestion = "Select a different room or time slot",
                        roomName = "",
                        dayOfWeek = entry.dayOfWeek
                    )
                )
                suggestions.add("Try a different room")
            }

            if (existing.sectionId == entry.sectionId) {
                conflicts.add(
                    ConflictReport(
                        type = ConflictType.SECTION_CLASH,
                        message = "This section already has a class scheduled here",
                        suggestion = "Select a free period for this section",
                        sectionName = "",
                        dayOfWeek = entry.dayOfWeek
                    )
                )
                suggestions.add("Check section free periods")
            }
        }

        if (entry.subjectId == 0L) {
            conflicts.add(
                ConflictReport(
                    type = ConflictType.FACULTY_CLASH,
                    message = "No subject selected",
                    suggestion = "Please select a subject",
                    dayOfWeek = entry.dayOfWeek
                )
            )
        }
        if (entry.facultyId == 0L) {
            conflicts.add(
                ConflictReport(
                    type = ConflictType.FACULTY_CLASH,
                    message = "No faculty assigned",
                    suggestion = "Please select a faculty member",
                    dayOfWeek = entry.dayOfWeek
                )
            )
        }
        if (entry.roomId == 0L) {
            conflicts.add(
                ConflictReport(
                    type = ConflictType.ROOM_CLASH,
                    message = "No room assigned",
                    suggestion = "Please select a room",
                    dayOfWeek = entry.dayOfWeek
                )
            )
        }

        return ValidationResult(
            hasConflicts = conflicts.isNotEmpty(),
            conflicts = conflicts,
            suggestions = suggestions.distinct()
        )
    }

    fun findFreeSlots(
        dayOfWeek: Int,
        allSlots: List<Long>,
        existingEntries: List<TimetableEntryEntity>,
        facultyId: Long? = null,
        roomId: Long? = null
    ): List<Long> {
        val usedSlots = existingEntries
            .filter { it.dayOfWeek == dayOfWeek }
            .map { it.timeSlotId }
            .toSet()
        return allSlots.filter { it !in usedSlots }
    }

    fun detectConflicts(
        entries: List<TimetableEntryEntity>
    ): List<ConflictReport> {
        val conflicts = mutableListOf<ConflictReport>()

        entries.groupBy { Triple(it.facultyId, it.dayOfWeek, it.timeSlotId) }
            .filter { it.key.first != 0L && it.value.size > 1 }
            .forEach { conflicts.add(ConflictReport(ConflictType.FACULTY_CLASH, "Faculty double-booked", "Reschedule one entry")) }

        entries.groupBy { Triple(it.roomId, it.dayOfWeek, it.timeSlotId) }
            .filter { it.key.first != 0L && it.value.size > 1 }
            .forEach { conflicts.add(ConflictReport(ConflictType.ROOM_CLASH, "Room double-booked", "Move to different room")) }

        entries.groupBy { Triple(it.sectionId, it.dayOfWeek, it.timeSlotId) }
            .filter { it.value.size > 1 }
            .forEach { conflicts.add(ConflictReport(ConflictType.SECTION_CLASH, "Section double-booked", "Reschedule one class")) }

        return conflicts
    }
}
