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
import kotlin.random.Random

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

/**
 * Constraint aware weekly timetable generator.
 *
 * Hard constraints (a violation is reported and never produced):
 *  - faculty, room and section can only be in one place per period,
 *  - lunch and break periods are never used,
 *  - faculty weekly maximum is respected,
 *  - faculty/section daily load is capped at the number of teaching periods,
 *  - laboratory sessions are placed in laboratory rooms, in consecutive blocks.
 *
 * Soft preferences:
 *  - theory periods are spread over the week (max two per subject per day),
 *  - each section gets a stable home classroom,
 *  - laboratories and rooms matching the section's department are preferred.
 *
 * The algorithm is deterministic for a given seed. When an attempt cannot schedule
 * everything, [generateBest] retries with a different seed and keeps the best result.
 */
@Singleton
class TimetableGenerator @Inject constructor() {

    private data class SectionSlot(val sectionId: Long, val day: Int, val slotId: Long)
    private data class FacultySlot(val facultyId: Long, val day: Int, val slotId: Long)
    private data class RoomSlot(val roomId: Long, val day: Int, val slotId: Long)

    fun generate(
        assignments: List<TeachingAssignment>,
        timeSlots: List<TimeSlotEntity>,
        rooms: List<RoomEntity>,
        facultyList: List<FacultyEntity>,
        onProgress: (GenerationProgress) -> Unit = {},
        seed: Long = DEFAULT_SEED
    ): GenerationResult {
        if (assignments.isEmpty()) {
            return GenerationResult(
                success = false,
                conflicts = listOf(
                    ConflictReport(
                        ConflictType.SECTION_CLASH,
                        "No teaching assignments to schedule",
                        "Add sections, subjects and faculty first"
                    )
                )
            )
        }

        val activeRooms = rooms.filter { it.isActive }
        val regularSlots = timeSlots
            .filter { it.isActive && it.type == SlotType.REGULAR }
            .sortedWith(compareBy({ it.dayOfWeek }, { it.periodNumber }))

        if (activeRooms.isEmpty()) {
            return GenerationResult(
                success = false,
                conflicts = listOf(
                    ConflictReport(
                        ConflictType.ROOM_CLASH,
                        "No active rooms are available",
                        "Add at least one classroom and one laboratory"
                    )
                )
            )
        }
        if (regularSlots.isEmpty()) {
            return GenerationResult(
                success = false,
                conflicts = listOf(
                    ConflictReport(
                        ConflictType.SECTION_CLASH,
                        "No active teaching periods are configured",
                        "Configure time slots before generating"
                    )
                )
            )
        }

        val random = Random(seed)
        val regularByDay = regularSlots.groupBy { it.dayOfWeek }.toSortedMap()
        val days = regularByDay.keys.toList()
        val maxDaily = minOf(MAX_DAILY_PERIODS, regularByDay.values.maxOf { it.size })

        val sections = assignments.map { it.section }.distinctBy { it.id }
        val homeRooms = assignHomeRooms(sections, activeRooms)

        val entries = mutableListOf<TimetableEntryEntity>()
        val sectionUsed = mutableSetOf<SectionSlot>()
        val facultyUsed = mutableSetOf<FacultySlot>()
        val roomUsed = mutableSetOf<RoomSlot>()
        val facultyWeekly = mutableMapOf<Long, Int>()
        val facultyDaily = mutableMapOf<Pair<Long, Int>, Int>()
        val sectionDaily = mutableMapOf<Pair<Long, Int>, Int>()
        val subjectDaily = mutableMapOf<Triple<Long, Long, Int>, Int>()

        // Laboratories first: they are the hardest to place because they need a block of
        // consecutive periods in a specialised room.
        val ordered = assignments.sortedWith(
            compareByDescending<TeachingAssignment> { it.subject.type == SubjectType.LAB }
                .thenByDescending { it.subject.requiredPeriods }
                .thenBy { it.section.name }
                .thenBy { it.subject.code }
        )

        val totalSteps = ordered.size
        onProgress(GenerationProgress(0, totalSteps, "Planning ${ordered.size} subject allocations…"))

        ordered.forEachIndexed { index, assignment ->
            val required = assignment.subject.requiredPeriods
            if (required > 0) {
                if (assignment.subject.type == SubjectType.LAB) {
                    scheduleLab(
                        assignment = assignment,
                        required = required,
                        days = days,
                        regularByDay = regularByDay,
                        rooms = activeRooms,
                        maxDaily = maxDaily,
                        random = random,
                        entries = entries,
                        sectionUsed = sectionUsed,
                        facultyUsed = facultyUsed,
                        roomUsed = roomUsed,
                        facultyWeekly = facultyWeekly,
                        facultyDaily = facultyDaily,
                        sectionDaily = sectionDaily,
                        subjectDaily = subjectDaily
                    )
                } else {
                    scheduleTheory(
                        assignment = assignment,
                        required = required,
                        days = days,
                        regularByDay = regularByDay,
                        rooms = activeRooms,
                        homeRoom = homeRooms[assignment.section.id],
                        maxDaily = maxDaily,
                        random = random,
                        entries = entries,
                        sectionUsed = sectionUsed,
                        facultyUsed = facultyUsed,
                        roomUsed = roomUsed,
                        facultyWeekly = facultyWeekly,
                        facultyDaily = facultyDaily,
                        sectionDaily = sectionDaily,
                        subjectDaily = subjectDaily
                    )
                }
            }

            onProgress(
                GenerationProgress(
                    currentStep = index + 1,
                    totalSteps = totalSteps,
                    message = "Scheduled ${assignment.subject.code} for ${assignment.section.name}"
                )
            )
        }

        val conflicts = validate(assignments, entries, timeSlots, activeRooms)
        val sortedEntries = entries.sortedWith(
            compareBy({ it.dayOfWeek }, { it.timeSlotId }, { it.sectionId })
        )

        onProgress(
            GenerationProgress(
                currentStep = totalSteps,
                totalSteps = totalSteps,
                message = if (conflicts.isEmpty()) "Timetable generated successfully" else "Completed with ${conflicts.size} warning(s)"
            )
        )

        return GenerationResult(
            success = conflicts.isEmpty(),
            entries = sortedEntries,
            conflicts = conflicts,
            progress = GenerationProgress(totalSteps, totalSteps)
        )
    }

    /**
     * Runs the generator a few times and keeps the best attempt. Generation is still
     * deterministic: the seeds are derived from [DEFAULT_SEED].
     */
    fun generateBest(
        assignments: List<TeachingAssignment>,
        timeSlots: List<TimeSlotEntity>,
        rooms: List<RoomEntity>,
        facultyList: List<FacultyEntity>,
        attempts: Int = 5,
        onProgress: (GenerationProgress) -> Unit = {}
    ): GenerationResult {
        var best: GenerationResult? = null
        repeat(attempts) { attempt ->
            val result = generate(
                assignments = assignments,
                timeSlots = timeSlots,
                rooms = rooms,
                facultyList = facultyList,
                onProgress = { progress ->
                    onProgress(
                        progress.copy(
                            message = if (attempt == 0) progress.message
                            else "Improving result (attempt ${attempt + 1}/$attempts)…"
                        )
                    )
                },
                seed = DEFAULT_SEED + attempt
            )
            if (best == null ||
                result.conflicts.size < best!!.conflicts.size ||
                (result.conflicts.size == best!!.conflicts.size && result.entries.size > best!!.entries.size)
            ) {
                best = result
            }
            if (result.success) return result
        }
        return best ?: GenerationResult(success = false)
    }

    private fun scheduleTheory(
        assignment: TeachingAssignment,
        required: Int,
        days: List<Int>,
        regularByDay: Map<Int, List<TimeSlotEntity>>,
        rooms: List<RoomEntity>,
        homeRoom: RoomEntity?,
        maxDaily: Int,
        random: Random,
        entries: MutableList<TimetableEntryEntity>,
        sectionUsed: MutableSet<SectionSlot>,
        facultyUsed: MutableSet<FacultySlot>,
        roomUsed: MutableSet<RoomSlot>,
        facultyWeekly: MutableMap<Long, Int>,
        facultyDaily: MutableMap<Pair<Long, Int>, Int>,
        sectionDaily: MutableMap<Pair<Long, Int>, Int>,
        subjectDaily: MutableMap<Triple<Long, Long, Int>, Int>
    ) {
        var scheduled = 0
        val dayOrder = days.shuffled(random)

        // Progressively relax the “spread across the week” preference, while never relaxing
        // the hard constraints.
        for (perDayLimit in intArrayOf(2, 3, maxDaily)) {
            if (scheduled >= required) break
            for (day in dayOrder) {
                if (scheduled >= required) break
                val slots = regularByDay[day].orEmpty().shuffled(random)
                for (slot in slots) {
                    if (scheduled >= required) break

                    val sectionSlot = SectionSlot(assignment.section.id, day, slot.id)
                    val facultySlot = FacultySlot(assignment.faculty.id, day, slot.id)
                    if (sectionSlot in sectionUsed || facultySlot in facultyUsed) continue

                    val subjectKey = Triple(assignment.section.id, assignment.subject.id, day)
                    if ((subjectDaily[subjectKey] ?: 0) >= perDayLimit) continue

                    val facultyDayKey = assignment.faculty.id to day
                    if ((facultyDaily[facultyDayKey] ?: 0) >= maxDaily) continue
                    val sectionDayKey = assignment.section.id to day
                    if ((sectionDaily[sectionDayKey] ?: 0) >= maxDaily) continue

                    val weeklyLoad = facultyWeekly[assignment.faculty.id] ?: 0
                    if (weeklyLoad >= assignment.faculty.maxWeeklyHours) continue

                    val room = findTheoryRoom(
                        section = assignment.section,
                        preferred = homeRoom,
                        rooms = rooms,
                        day = day,
                        slotId = slot.id,
                        roomUsed = roomUsed
                    ) ?: continue

                    roomUsed += RoomSlot(room.id, day, slot.id)
                    sectionUsed += sectionSlot
                    facultyUsed += facultySlot
                    facultyWeekly[assignment.faculty.id] = weeklyLoad + 1
                    facultyDaily[facultyDayKey] = (facultyDaily[facultyDayKey] ?: 0) + 1
                    sectionDaily[sectionDayKey] = (sectionDaily[sectionDayKey] ?: 0) + 1
                    subjectDaily[subjectKey] = (subjectDaily[subjectKey] ?: 0) + 1

                    entries += entry(assignment, slot, room)
                    scheduled++
                }
            }
        }
    }

    private fun scheduleLab(
        assignment: TeachingAssignment,
        required: Int,
        days: List<Int>,
        regularByDay: Map<Int, List<TimeSlotEntity>>,
        rooms: List<RoomEntity>,
        maxDaily: Int,
        random: Random,
        entries: MutableList<TimetableEntryEntity>,
        sectionUsed: MutableSet<SectionSlot>,
        facultyUsed: MutableSet<FacultySlot>,
        roomUsed: MutableSet<RoomSlot>,
        facultyWeekly: MutableMap<Long, Int>,
        facultyDaily: MutableMap<Pair<Long, Int>, Int>,
        sectionDaily: MutableMap<Pair<Long, Int>, Int>,
        subjectDaily: MutableMap<Triple<Long, Long, Int>, Int>
    ) {
        val dayOrder = days.shuffled(random)
        var scheduled = 0

        // Preferred: one consecutive block of the full laboratory duration.
        for (day in dayOrder) {
            if ((subjectDaily[Triple(assignment.section.id, assignment.subject.id, day)] ?: 0) > 0) continue
            val slots = regularByDay[day].orEmpty()
            val blocks = consecutiveBlocks(slots, required).shuffled(random)
            for (block in blocks) {
                if (canPlace(
                        assignment = assignment,
                        slots = block,
                        day = day,
                        maxDaily = maxDaily,
                        sectionUsed = sectionUsed,
                        facultyUsed = facultyUsed,
                        facultyWeekly = facultyWeekly,
                        facultyDaily = facultyDaily,
                        sectionDaily = sectionDaily
                    )
                ) {
                    val room = findLabRoom(
                        section = assignment.section,
                        rooms = rooms,
                        day = day,
                        slotIds = block.map { it.id },
                        roomUsed = roomUsed
                    ) ?: continue

                    block.forEach { slot ->
                        entries += entry(assignment, slot, room)
                        roomUsed += RoomSlot(room.id, day, slot.id)
                        sectionUsed += SectionSlot(assignment.section.id, day, slot.id)
                        facultyUsed += FacultySlot(assignment.faculty.id, day, slot.id)
                    }
                    facultyWeekly[assignment.faculty.id] =
                        (facultyWeekly[assignment.faculty.id] ?: 0) + block.size
                    facultyDaily[assignment.faculty.id to day] =
                        (facultyDaily[assignment.faculty.id to day] ?: 0) + block.size
                    sectionDaily[assignment.section.id to day] =
                        (sectionDaily[assignment.section.id to day] ?: 0) + block.size
                    subjectDaily[Triple(assignment.section.id, assignment.subject.id, day)] = block.size
                    scheduled += block.size
                    return
                }
            }
        }

        // Fallback: schedule the remaining laboratory periods individually in a lab room.
        // Validation reports the broken continuity so the user can make room in the week.
        for (day in dayOrder) {
            if (scheduled >= required) break
            if ((subjectDaily[Triple(assignment.section.id, assignment.subject.id, day)] ?: 0) > 0) continue
            val slots = regularByDay[day].orEmpty().shuffled(random)
            for (slot in slots) {
                if (scheduled >= required) break
                val sectionSlot = SectionSlot(assignment.section.id, day, slot.id)
                val facultySlot = FacultySlot(assignment.faculty.id, day, slot.id)
                if (sectionSlot in sectionUsed || facultySlot in facultyUsed) continue
                if ((facultyDaily[assignment.faculty.id to day] ?: 0) >= maxDaily) continue
                if ((facultyWeekly[assignment.faculty.id] ?: 0) >= assignment.faculty.maxWeeklyHours) continue

                val room = findLabRoom(
                    section = assignment.section,
                    rooms = rooms,
                    day = day,
                    slotIds = listOf(slot.id),
                    roomUsed = roomUsed
                ) ?: continue

                entries += entry(assignment, slot, room)
                roomUsed += RoomSlot(room.id, day, slot.id)
                sectionUsed += sectionSlot
                facultyUsed += facultySlot
                facultyWeekly[assignment.faculty.id] = (facultyWeekly[assignment.faculty.id] ?: 0) + 1
                facultyDaily[assignment.faculty.id to day] = (facultyDaily[assignment.faculty.id to day] ?: 0) + 1
                sectionDaily[assignment.section.id to day] = (sectionDaily[assignment.section.id to day] ?: 0) + 1
                subjectDaily[Triple(assignment.section.id, assignment.subject.id, day)] =
                    (subjectDaily[Triple(assignment.section.id, assignment.subject.id, day)] ?: 0) + 1
                scheduled++
            }
        }
    }

    private fun canPlace(
        assignment: TeachingAssignment,
        slots: List<TimeSlotEntity>,
        day: Int,
        maxDaily: Int,
        sectionUsed: Set<SectionSlot>,
        facultyUsed: Set<FacultySlot>,
        facultyWeekly: Map<Long, Int>,
        facultyDaily: Map<Pair<Long, Int>, Int>,
        sectionDaily: Map<Pair<Long, Int>, Int>
    ): Boolean {
        if (slots.isEmpty()) return false
        if (slots.any { sectionUsed.contains(SectionSlot(assignment.section.id, day, it.id)) }) return false
        if (slots.any { facultyUsed.contains(FacultySlot(assignment.faculty.id, day, it.id)) }) return false
        if ((facultyWeekly[assignment.faculty.id] ?: 0) + slots.size > assignment.faculty.maxWeeklyHours) return false
        if ((facultyDaily[assignment.faculty.id to day] ?: 0) + slots.size > maxDaily) return false
        if ((sectionDaily[assignment.section.id to day] ?: 0) + slots.size > maxDaily) return false
        return true
    }

    private fun consecutiveBlocks(
        slots: List<TimeSlotEntity>,
        length: Int
    ): List<List<TimeSlotEntity>> {
        if (length <= 0 || slots.size < length) return emptyList()
        return (0..slots.size - length).mapNotNull { start ->
            val window = slots.subList(start, start + length)
            val consecutive = window.zipWithNext().all { (a, b) -> b.periodNumber == a.periodNumber + 1 }
            if (window.first().dayOfWeek == window.last().dayOfWeek && consecutive) window.toList() else null
        }
    }

    private fun findTheoryRoom(
        section: SectionEntity,
        preferred: RoomEntity?,
        rooms: List<RoomEntity>,
        day: Int,
        slotId: Long,
        roomUsed: Set<RoomSlot>
    ): RoomEntity? {
        val theoryTypes = setOf(RoomType.CLASSROOM, RoomType.SEMINAR_HALL, RoomType.AUDITORIUM)
        val ordered = buildList {
            preferred?.let { add(it) }
            addAll(rooms.filter { it.type in theoryTypes }.sortedBy { it.capacity })
        }.distinctBy { it.id }

        ordered.firstOrNull {
            it.capacity >= section.strength && RoomSlot(it.id, day, slotId) !in roomUsed
        }?.let { return it }

        // Capacity shortfall: fall back to the largest available theory room. Validation
        // raises a capacity warning for it.
        return rooms.filter { it.type in theoryTypes }
            .sortedByDescending { it.capacity }
            .firstOrNull { RoomSlot(it.id, day, slotId) !in roomUsed }
    }

    private fun findLabRoom(
        section: SectionEntity,
        rooms: List<RoomEntity>,
        day: Int,
        slotIds: List<Long>,
        roomUsed: Set<RoomSlot>
    ): RoomEntity? {
        val departmentCode = section.name.substringBefore(' ').uppercase()
        val ranked = rooms.filter { it.type == RoomType.LAB }
            .sortedWith(
                compareBy<RoomEntity>(
                    { if (it.name.uppercase().startsWith(departmentCode)) 0 else 1 },
                    { if (it.capacity >= section.strength) 0 else 1 },
                    { it.capacity }
                )
            )

        ranked.firstOrNull { room ->
            slotIds.all { RoomSlot(room.id, day, it) !in roomUsed }
        }?.let { return it }

        return null
    }

    private fun assignHomeRooms(
        sections: List<SectionEntity>,
        rooms: List<RoomEntity>
    ): Map<Long, RoomEntity> {
        val theoryTypes = setOf(RoomType.CLASSROOM, RoomType.SEMINAR_HALL, RoomType.AUDITORIUM)
        val candidates = rooms.filter { it.type in theoryTypes }
        if (candidates.isEmpty()) return emptyMap()

        val result = mutableMapOf<Long, RoomEntity>()
        val used = mutableSetOf<Long>()

        sections.sortedByDescending { it.strength }.forEach { section ->
            val departmentCode = section.name.substringBefore(' ').uppercase()
            val ranked = candidates.sortedWith(
                compareBy<RoomEntity>(
                    { if (it.name.uppercase().startsWith(departmentCode)) 0 else 1 },
                    { if (it.capacity >= section.strength) 0 else 1 },
                    { it.capacity }
                )
            )
            val room = ranked.firstOrNull { it.id !in used } ?: ranked.firstOrNull()
            if (room != null) {
                result[section.id] = room
                used += room.id
            }
        }
        return result
    }

    private fun entry(
        assignment: TeachingAssignment,
        slot: TimeSlotEntity,
        room: RoomEntity
    ) = TimetableEntryEntity(
        sectionId = assignment.section.id,
        subjectId = assignment.subject.id,
        facultyId = assignment.faculty.id,
        roomId = room.id,
        timeSlotId = slot.id,
        dayOfWeek = slot.dayOfWeek,
        weekType = WeekType.ALL
    )

    /**
     * Independent verification of a generated timetable. This is deliberately separate from
     * the scheduling code: it does not trust any bookkeeping and re-checks every hard
     * constraint against the final entries.
     */
    fun validate(
        assignments: List<TeachingAssignment>,
        entries: List<TimetableEntryEntity>,
        timeSlots: List<TimeSlotEntity>,
        rooms: List<RoomEntity>
    ): List<ConflictReport> {
        val conflicts = mutableListOf<ConflictReport>()
        val facultyById = assignments.associate { it.faculty.id to it.faculty }
        val sectionById = assignments.associate { it.section.id to it.section }
        val subjectById = assignments.associate { it.subject.id to it.subject }
        val roomById = rooms.associateBy { it.id }
        val slotById = timeSlots.associateBy { it.id }

        entries.groupBy { Triple(it.sectionId, it.dayOfWeek, it.timeSlotId) }
            .filter { it.value.size > 1 }
            .forEach { (_, duplicates) ->
                val entry = duplicates.first()
                conflicts += ConflictReport(
                    type = ConflictType.SECTION_CLASH,
                    message = "${sectionById[entry.sectionId]?.name ?: "A section"} is double-booked in ${dayName(entry.dayOfWeek)} period ${slotById[entry.timeSlotId]?.periodNumber ?: "?"}",
                    suggestion = "Regenerate the timetable or remove the duplicate period",
                    sectionName = sectionById[entry.sectionId]?.name.orEmpty(),
                    dayOfWeek = entry.dayOfWeek
                )
            }

        entries.groupBy { Triple(it.facultyId, it.dayOfWeek, it.timeSlotId) }
            .filter { it.value.size > 1 }
            .forEach { (_, duplicates) ->
                val entry = duplicates.first()
                conflicts += ConflictReport(
                    type = ConflictType.FACULTY_CLASH,
                    message = "${facultyById[entry.facultyId]?.name ?: "A faculty member"} is assigned to two classes at the same time",
                    suggestion = "Reduce the faculty workload or add another faculty member",
                    facultyName = facultyById[entry.facultyId]?.name.orEmpty(),
                    dayOfWeek = entry.dayOfWeek
                )
            }

        entries.groupBy { Triple(it.roomId, it.dayOfWeek, it.timeSlotId) }
            .filter { it.value.size > 1 }
            .forEach { (_, duplicates) ->
                val entry = duplicates.first()
                conflicts += ConflictReport(
                    type = ConflictType.ROOM_CLASH,
                    message = "${roomById[entry.roomId]?.name ?: "A room"} is used by more than one class at the same time",
                    suggestion = "Add another room or move one of the classes",
                    roomName = roomById[entry.roomId]?.name.orEmpty(),
                    dayOfWeek = entry.dayOfWeek
                )
            }

        // Lunch/break periods, capacity and lab-room suitability.
        entries.forEach { entry ->
            val slot = slotById[entry.timeSlotId]
            if (slot != null && slot.type != SlotType.REGULAR) {
                conflicts += ConflictReport(
                    type = ConflictType.SECTION_CLASH,
                    message = "A class was placed during a ${slot.type.name.lowercase()} period",
                    suggestion = "Remove the class from the non-teaching period",
                    sectionName = sectionById[entry.sectionId]?.name.orEmpty(),
                    dayOfWeek = entry.dayOfWeek
                )
            }

            val room = roomById[entry.roomId]
            val section = sectionById[entry.sectionId]
            if (room != null && section != null && room.capacity > 0 && room.capacity < section.strength) {
                conflicts += ConflictReport(
                    type = ConflictType.ROOM_CAPACITY_EXCEEDED,
                    message = "${room.name} seats ${room.capacity} but ${section.name} has ${section.strength} students",
                    suggestion = "Assign a larger room",
                    roomName = room.name,
                    sectionName = section.name
                )
            }

            val subject = subjectById[entry.subjectId]
            if (room != null && subject != null && subject.type == SubjectType.LAB && room.type != RoomType.LAB) {
                conflicts += ConflictReport(
                    type = ConflictType.LAB_CLASH,
                    message = "${subject.name} needs a laboratory but is scheduled in ${room.name}",
                    suggestion = "Add a laboratory with enough capacity",
                    roomName = room.name,
                    sectionName = section?.name.orEmpty()
                )
            }
        }

        // Weekly workload per faculty.
        entries.groupBy { it.facultyId }.forEach { (facultyId, load) ->
            val faculty = facultyById[facultyId] ?: return@forEach
            if (load.size > faculty.maxWeeklyHours) {
                conflicts += ConflictReport(
                    type = ConflictType.WORKLOAD_EXCEEDED,
                    message = "${faculty.name} has ${load.size} periods but a maximum of ${faculty.maxWeeklyHours}",
                    suggestion = "Increase the maximum or distribute subjects to another faculty member",
                    facultyName = faculty.name
                )
            }
        }

        // Laboratory continuity: a full lab must be one consecutive block in one lab.
        entries.filter { subjectById[it.subjectId]?.type == SubjectType.LAB }
            .groupBy { it.sectionId to it.subjectId }
            .forEach { (key, labEntries) ->
                val subject = subjectById[key.second] ?: return@forEach
                val expected = subject.requiredPeriods
                if (expected <= 1 || labEntries.size != expected) return@forEach

                val oneDay = labEntries.map { it.dayOfWeek }.distinct().size == 1
                val periods = labEntries.mapNotNull { slotById[it.timeSlotId]?.periodNumber }.sorted()
                val consecutive = periods.size == expected && periods.zipWithNext().all { (a, b) -> b == a + 1 }
                val oneRoom = labEntries.map { it.roomId }.distinct().size == 1
                if (!oneDay || !consecutive || !oneRoom) {
                    conflicts += ConflictReport(
                        type = ConflictType.INVALID_LAB_CONTINUITY,
                        message = "${subject.name} (${sectionById[key.first]?.name ?: "section"}) is not a continuous ${expected}-period laboratory block",
                        suggestion = "Free two consecutive periods for the laboratory",
                        sectionName = sectionById[key.first]?.name.orEmpty()
                    )
                }
            }

        // Under-scheduled subjects.
        assignments.forEach { assignment ->
            val required = assignment.subject.requiredPeriods
            if (required <= 0) return@forEach
            val scheduled = entries.count {
                it.sectionId == assignment.section.id && it.subjectId == assignment.subject.id
            }
            if (scheduled < required) {
                conflicts += ConflictReport(
                    type = ConflictType.UNSCHEDULED_PERIODS,
                    message = "${assignment.subject.name} (${assignment.section.name}) has $scheduled of $required weekly periods",
                    suggestion = "Add a free period, another faculty member or another room",
                    facultyName = assignment.faculty.name,
                    sectionName = assignment.section.name
                )
            }
        }

        return conflicts.distinctBy { it.type to it.message }
    }

    companion object {
        const val DEFAULT_SEED = 20241001L
        const val MAX_DAILY_PERIODS = 6

        fun dayName(dayOfWeek: Int): String = when (dayOfWeek) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Day $dayOfWeek"
        }
    }
}
