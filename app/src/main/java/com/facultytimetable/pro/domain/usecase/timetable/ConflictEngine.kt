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

            if (existing.dayOfWeek == entry.dayOfWeek && existing.timeSlotId == entry.timeSlotId) {
                if (existing.facultyId == entry.facultyId) {
                    conflicts.add(
                        ConflictReport(
                            type = ConflictType.FACULTY_CLASH,
                            message = "Faculty already occupied at this time slot",
                            suggestion = "Change time slot or use a different faculty member",
                            facultyName = "",
                            dayOfWeek = entry.dayOfWeek,
                            periodNumber = entry.timeSlotId.toInt()
                        )
                    )
                    suggestions.add("Try scheduling during a free period")
                }

                if (existing.roomId == entry.roomId) {
                    conflicts.add(
                        ConflictReport(
                            type = ConflictType.ROOM_CLASH,
                            message = "Room already occupied at this time slot",
                            suggestion = "Use a different room",
                            roomName = "",
                            dayOfWeek = entry.dayOfWeek
                        )
                    )
                    suggestions.add("Try an alternative room")
                }

                if (existing.sectionId == entry.sectionId) {
                    conflicts.add(
                        ConflictReport(
                            type = ConflictType.SECTION_CLASH,
                            message = "Section already has a class at this time",
                            suggestion = "Schedule during a free period for this section",
                            sectionName = "",
                            dayOfWeek = entry.dayOfWeek
                        )
                    )
                    suggestions.add("Check section availability")
                }
            }
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
}
