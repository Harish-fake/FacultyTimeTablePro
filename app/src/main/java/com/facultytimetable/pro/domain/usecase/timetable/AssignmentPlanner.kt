package com.facultytimetable.pro.domain.usecase.timetable

import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import javax.inject.Inject
import javax.inject.Singleton

/** Number of weekly periods a subject needs on the timetable. */
val SubjectEntity.requiredPeriods: Int
    get() = when (type) {
        SubjectType.LAB -> maxOf(labHoursPerWeek, hoursPerWeek)
        else -> hoursPerWeek
    }.coerceAtLeast(0)

/**
 * Turns sections, subjects and faculty into concrete teaching assignments.
 *
 * A section belongs to exactly one department. The assignment is resolved in this order:
 *  1. [SectionEntity.departmentId] when it is set,
 *  2. the department whose code appears in the section name (e.g. “CSE 3A” → CSE).
 *
 * Faculty are balanced by their planned weekly load, never exceeding their weekly maximum
 * when a suitable alternative exists. Every decision is deterministic so repeated
 * generation runs produce the same result.
 */
@Singleton
class AssignmentPlanner @Inject constructor() {

    data class Plan(
        val assignments: List<TeachingAssignment>,
        val warnings: List<String> = emptyList()
    )

    fun plan(
        sections: List<SectionEntity>,
        subjects: List<SubjectEntity>,
        faculty: List<FacultyEntity>,
        departments: List<DepartmentEntity>
    ): Plan {
        val warnings = mutableListOf<String>()
        val assignments = mutableListOf<TeachingAssignment>()
        val load = faculty.associate { it.id to 0 }.toMutableMap()

        val activeSubjects = subjects.filter { it.isActive }
        val activeFaculty = faculty.filter { it.isActive }

        sections.filter { it.isActive }
            .sortedWith(compareBy({ it.name.substringBefore(' ') }, { it.name }))
            .forEach { section ->
                val departmentId = resolveDepartmentId(section, departments)
                if (departmentId == null) {
                    warnings += "Could not determine the department of ${section.name}. " +
                        "Open the section and select a department."
                    return@forEach
                }

                val sectionSubjects = activeSubjects
                    .filter { it.departmentId == departmentId }
                    .sortedBy { it.code }

                if (sectionSubjects.isEmpty()) {
                    warnings += "No active subjects found for ${section.name}."
                     return@forEach
                }

                sectionSubjects.forEach { subject ->
                    val candidates = activeFaculty.filter { it.departmentId == departmentId }
                    if (candidates.isEmpty()) {
                        warnings += "No active faculty found for ${section.name} / ${subject.name}."
                        return@forEach
                    }

                    val required = subject.requiredPeriods
                    val selected = candidates.minWithOrNull(
                        compareBy<FacultyEntity>(
                            { load[it.id] ?: 0 },
                            { if ((load[it.id] ?: 0) + required > it.maxWeeklyHours) 1 else 0 },
                            { it.id }
                        )
                    ) ?: return@forEach

                    assignments += TeachingAssignment(subject = subject, section = section, faculty = selected)
                    load[selected.id] = (load[selected.id] ?: 0) + required
                }
            }

        return Plan(assignments = assignments, warnings = warnings.distinct())
    }

    private fun resolveDepartmentId(
        section: SectionEntity,
        departments: List<DepartmentEntity>
    ): Long? {
        if (section.departmentId > 0) return section.departmentId
        val name = section.name.uppercase()
        return departments.firstOrNull { department ->
            val code = department.code.uppercase()
            name == code || name.startsWith("$code ") || name.startsWith("$code-")
        }?.id
    }
}
