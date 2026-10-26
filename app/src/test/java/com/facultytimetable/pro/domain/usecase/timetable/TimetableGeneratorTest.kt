package com.facultytimetable.pro.domain.usecase.timetable

import com.facultytimetable.pro.data.local.db.entity.SlotType
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.data.local.seed.SampleDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableGeneratorTest {

    private val sampleNow = 1_790_000_000_000L // 2026-09-21, safely inside an academic year

    @Test
    fun `sample data produces a complete conflict free timetable`() {
        val data = SampleDataProvider.bundle(sampleNow)
        val plan = AssignmentPlanner().plan(data.sections, data.subjects, data.faculty, data.departments)

        assertTrue("Planner should not report warnings: ${plan.warnings}", plan.warnings.isEmpty())
        assertTrue("Every section should receive assignments", plan.assignments.isNotEmpty())

        val result = TimetableGenerator().generateBest(
            assignments = plan.assignments,
            timeSlots = data.timeSlots,
            rooms = data.rooms,
            facultyList = data.faculty,
            attempts = 5
        )

        assertEquals(
            "Expected no conflicts: " + result.conflicts.joinToString { it.message },
            0,
            result.conflicts.size
        )
        assertTrue("Timetable should be marked successful", result.success)

        val expectedPeriods = plan.assignments.sumOf { it.subject.requiredPeriods }
        assertEquals("Every required period should be scheduled", expectedPeriods, result.entries.size)
        assertEquals("The sample dataset must require 48 periods", 48, expectedPeriods)

        val sectionNames = data.sections.associate { it.id to it.name }
        val periodsBySection = result.entries
            .groupingBy { sectionNames.getValue(it.sectionId) }
            .eachCount()
        assertEquals(
            "Each section must receive its exact documented quota",
            mapOf(
                "CSE 3A" to 13,
                "CSE 3B" to 13,
                "ECE 3A" to 11,
                "ME 3A" to 11
            ),
            periodsBySection
        )

        // No section may be double-booked.
        val sectionSlots = result.entries.map { Triple(it.sectionId, it.dayOfWeek, it.timeSlotId) }
        assertEquals("Duplicate section slots", sectionSlots.size, sectionSlots.distinct().size)

        // Faculty and rooms are unique too.
        val facultySlots = result.entries.map { Triple(it.facultyId, it.dayOfWeek, it.timeSlotId) }
        assertEquals("Duplicate faculty slots", facultySlots.size, facultySlots.distinct().size)
        val roomSlots = result.entries.map { Triple(it.roomId, it.dayOfWeek, it.timeSlotId) }
        assertEquals("Duplicate room slots", roomSlots.size, roomSlots.distinct().size)

        // No teaching in lunch or break periods.
        val slotById = data.timeSlots.associateBy { it.id }
        result.entries.forEach { entry ->
            assertEquals(
                "Only regular periods may be used",
                SlotType.REGULAR,
                slotById.getValue(entry.timeSlotId).type
            )
        }

        // Labs are consecutive, single-day blocks in actual lab rooms.
        val subjectById = data.subjects.associateBy { it.id }
        val roomById = data.rooms.associateBy { it.id }
        result.entries
            .filter { subjectById.getValue(it.subjectId).type == SubjectType.LAB }
            .groupBy { it.sectionId to it.subjectId }
            .forEach { (_, entries) ->
                assertEquals("Laboratory must be one day", 1, entries.map { it.dayOfWeek }.distinct().size)
                assertEquals("Laboratory must use one room", 1, entries.map { it.roomId }.distinct().size)
                val room = roomById.getValue(entries.first().roomId)
                assertEquals("Laboratory must be scheduled in a lab", "LAB", room.type.name)
                val periods = entries.map { slotById.getValue(it.timeSlotId).periodNumber }.sorted()
                periods.zipWithNext().forEach { (a, b) ->
                    assertEquals("Laboratory periods must be consecutive", a + 1, b)
                }
            }

        // Faculty weekly limits are respected.
        val facultyById = data.faculty.associateBy { it.id }
        result.entries.groupBy { it.facultyId }.forEach { (facultyId, load) ->
            assertTrue(
                "${facultyById.getValue(facultyId).name} exceeds weekly maximum",
                load.size <= facultyById.getValue(facultyId).maxWeeklyHours
            )
        }

        // Every entry is inside the section's capacity-adequate room.
        val sectionById = data.sections.associateBy { it.id }
        result.entries.forEach { entry ->
            val room = roomById.getValue(entry.roomId)
            val section = sectionById.getValue(entry.sectionId)
            assertTrue(
                "Room ${room.name} is too small for ${section.name}",
                room.capacity >= section.strength
            )
        }
    }

    @Test
    fun `sample dataset matches the documented size and quotas`() {
        val data = SampleDataProvider.bundle(sampleNow)
        val plan = AssignmentPlanner().plan(data.sections, data.subjects, data.faculty, data.departments)

        assertEquals(3, data.departments.size)
        assertEquals(6, data.faculty.size)
        assertEquals(10, data.subjects.size)
        assertEquals(8, data.rooms.size)
        assertEquals(4, data.sections.size)
        assertEquals(40, data.timeSlots.size)
        assertEquals(30, data.timeSlots.count { it.type == SlotType.REGULAR })
        assertEquals(48, plan.assignments.sumOf { it.subject.requiredPeriods })

        val quotas = plan.assignments
            .groupBy { it.section.name }
            .mapValues { (_, assignments) -> assignments.sumOf { it.subject.requiredPeriods } }
        assertEquals(mapOf("CSE 3A" to 13, "CSE 3B" to 13, "ECE 3A" to 11, "ME 3A" to 11), quotas)
        assertTrue("Planner should not report warnings: ${plan.warnings}", plan.warnings.isEmpty())
    }

    @Test
    fun `generation is deterministic for the same seed`() {
        val data = SampleDataProvider.bundle(sampleNow)
        val plan = AssignmentPlanner().plan(data.sections, data.subjects, data.faculty, data.departments)
        val generator = TimetableGenerator()

        val first = generator.generate(plan.assignments, data.timeSlots, data.rooms, data.faculty, seed = 7L)
        val second = generator.generate(plan.assignments, data.timeSlots, data.rooms, data.faculty, seed = 7L)

        assertEquals(first.entries.size, second.entries.size)
        assertEquals(
            first.entries.map { Triple(it.sectionId, it.subjectId, it.timeSlotId) },
            second.entries.map { Triple(it.sectionId, it.subjectId, it.timeSlotId) }
        )
    }

    @Test
    fun `lunch and break periods are never used`() {
        val data = SampleDataProvider.bundle(sampleNow)
        val plan = AssignmentPlanner().plan(data.sections, data.subjects, data.faculty, data.departments)
        val result = TimetableGenerator().generateBest(
            plan.assignments, data.timeSlots, data.rooms, data.faculty, attempts = 3
        )
        val nonTeaching = data.timeSlots
            .filter { it.type != SlotType.REGULAR }
            .map { it.id }
            .toSet()
        assertTrue(result.entries.none { it.timeSlotId in nonTeaching })
    }
}
