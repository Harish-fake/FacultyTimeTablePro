package com.facultytimetable.pro.presentation.academic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.dao.HolidayDao
import com.facultytimetable.pro.data.local.db.dao.SectionDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
import com.facultytimetable.pro.data.local.db.entity.HolidayEntity
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AcademicCalendarState(
    val years: List<AcademicYearEntity> = emptyList(),
    val semesters: List<SemesterEntity> = emptyList(),
    val holidays: List<HolidayEntity> = emptyList(),
    val selectedYearId: Long? = null,
    val isLoading: Boolean = true,
    val message: String? = null,
    val error: String? = null
) {
    val currentYear: AcademicYearEntity? get() = years.firstOrNull { it.isCurrent } ?: years.firstOrNull()
    val activeSemester: SemesterEntity? get() = semesters.firstOrNull { it.isActive }
    val yearsForSelection: List<AcademicYearEntity>
        get() = if (years.isEmpty()) emptyList() else years
    val semestersForSelectedYear: List<SemesterEntity>
        get() {
            val yearId = selectedYearId ?: currentYear?.id ?: return emptyList()
            return semesters.filter { it.academicYearId == yearId }.sortedBy { it.semesterNumber }
        }
    val upcomingHolidays: List<HolidayEntity>
        get() {
            val today = localDateToMillis(LocalDate.now())
            return holidays.filter { it.date >= today }.sortedBy { it.date }
        }
}

private fun localDateToMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun millisToLocalDate(millis: Long): LocalDate =
    java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

fun parseDate(text: String): Long? = runCatching {
    localDateToMillis(LocalDate.parse(text.trim()))
}.getOrNull()

fun formatDate(millis: Long): String = runCatching {
    millisToLocalDate(millis).toString()
}.getOrDefault("—")

fun formatReadableDate(millis: Long): String = runCatching {
    millisToLocalDate(millis).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
}.getOrDefault("—")

@HiltViewModel
class AcademicCalendarViewModel @Inject constructor(
    private val academicYearDao: AcademicYearDao,
    private val semesterDao: SemesterDao,
    private val holidayDao: HolidayDao,
    private val sectionDao: SectionDao
) : ViewModel() {

    private val selectedYearId = MutableStateFlow<Long?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    private val calendarData = combine(
        academicYearDao.getAllAcademicYears(),
        semesterDao.getAllSemesters(),
        holidayDao.getAllHolidays()
    ) { years, semesters, holidays -> Triple(years, semesters, holidays) }

    val state: StateFlow<AcademicCalendarState> = combine(
        calendarData,
        selectedYearId,
        message,
        error
    ) { data, selected, msg, err ->
        val years = data.first
        val semesters = data.second
        val holidays = data.third
        val effectiveSelected = when {
            selected != null && years.any { it.id == selected } -> selected
            else -> years.firstOrNull { it.isCurrent }?.id ?: years.firstOrNull()?.id
        }
        AcademicCalendarState(
            years = years,
            semesters = semesters,
            holidays = holidays,
            selectedYearId = effectiveSelected,
            isLoading = false,
            message = msg,
            error = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AcademicCalendarState())

    fun selectYear(id: Long) { selectedYearId.value = id }

    fun consumeMessage() { message.value = null }
    fun consumeError() { error.value = null }

    fun saveYear(
        existing: AcademicYearEntity?,
        name: String,
        startDate: String,
        endDate: String,
        isCurrent: Boolean
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) { error.value = "Academic year name is required"; return }
        val start = parseDate(startDate)
        val end = parseDate(endDate)
        if (start == null || end == null) {
            error.value = "Use a valid date in yyyy-MM-dd format"
            return
        }
        if (end < start) { error.value = "End date must be after the start date"; return }

        viewModelScope.launch {
            try {
                if (isCurrent) academicYearDao.clearCurrentYear()
                if (existing == null) {
                    academicYearDao.insert(
                        AcademicYearEntity(
                            name = trimmedName,
                            startDate = start,
                            endDate = end,
                            isCurrent = isCurrent
                        )
                    )
                } else {
                    academicYearDao.update(
                        existing.copy(
                            name = trimmedName,
                            startDate = start,
                            endDate = end,
                            isCurrent = isCurrent,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                message.value = if (existing == null) "Academic year added" else "Academic year updated"
            } catch (e: Exception) {
                error.value = e.message ?: "Could not save academic year"
            }
        }
    }

    fun setCurrentYear(year: AcademicYearEntity) {
        viewModelScope.launch {
            try {
                academicYearDao.clearCurrentYear()
                academicYearDao.setCurrentYear(year.id)
                selectedYearId.value = year.id
                message.value = "${year.name} marked as current"
            } catch (e: Exception) {
                error.value = e.message ?: "Could not update academic year"
            }
        }
    }

    fun deleteYear(year: AcademicYearEntity) {
        if (state.value.semesters.any { it.academicYearId == year.id }) {
            error.value = "This year still has semesters. Delete them first."
            return
        }
        viewModelScope.launch {
            try {
                academicYearDao.delete(year)
                message.value = "Academic year deleted"
            } catch (e: Exception) {
                error.value = "Could not delete this academic year."
            }
        }
    }

    fun saveSemester(
        existing: SemesterEntity?,
        name: String,
        yearId: Long,
        number: String,
        isActive: Boolean
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) { error.value = "Semester name is required"; return }
        val numberValue = number.toIntOrNull()
        if (numberValue == null || numberValue !in 1..12) {
            error.value = "Semester number must be between 1 and 12"
            return
        }
        val duplicate = state.value.semesters.any {
            it.academicYearId == yearId &&
                it.semesterNumber == numberValue &&
                it.id != existing?.id
        }
        if (duplicate) { error.value = "That semester number already exists for this year"; return }

        viewModelScope.launch {
            try {
                if (isActive) semesterDao.deactivateAll()
                if (existing == null) {
                    semesterDao.insert(
                        SemesterEntity(
                            name = trimmedName,
                            academicYearId = yearId,
                            semesterNumber = numberValue,
                            isActive = isActive
                        )
                    )
                } else {
                    semesterDao.update(
                        existing.copy(
                            name = trimmedName,
                            academicYearId = yearId,
                            semesterNumber = numberValue,
                            isActive = isActive,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                message.value = if (existing == null) "Semester added" else "Semester updated"
            } catch (e: Exception) {
                error.value = e.message ?: "Could not save semester"
            }
        }
    }

    fun setActiveSemester(semester: SemesterEntity) {
        viewModelScope.launch {
            try {
                semesterDao.deactivateAll()
                semesterDao.setActiveSemester(semester.id)
                message.value = "${semester.name} is now the active semester"
            } catch (e: Exception) {
                error.value = e.message ?: "Could not activate semester"
            }
        }
    }

    fun deleteSemester(semester: SemesterEntity) {
        viewModelScope.launch {
            val hasSections = runCatching {
                sectionDao.getAllSections().first().any { it.semesterId == semester.id }
            }.getOrDefault(false)
            if (hasSections) {
                error.value = "This semester still has sections. Delete or move them first."
                return@launch
            }
            try {
                semesterDao.delete(semester)
                message.value = "Semester deleted"
            } catch (e: Exception) {
                error.value = "Could not delete this semester."
            }
        }
    }

    fun saveHoliday(existing: HolidayEntity?, name: String, date: String, isRecurring: Boolean) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) { error.value = "Holiday name is required"; return }
        val millis = parseDate(date)
        if (millis == null) { error.value = "Use a valid date in yyyy-MM-dd format"; return }

        viewModelScope.launch {
            try {
                if (existing == null) {
                    holidayDao.insert(
                        HolidayEntity(name = trimmedName, date = millis, isRecurring = isRecurring)
                    )
                } else {
                    holidayDao.update(
                        existing.copy(name = trimmedName, date = millis, isRecurring = isRecurring)
                    )
                }
                message.value = if (existing == null) "Holiday added" else "Holiday updated"
            } catch (e: Exception) {
                error.value = "A holiday already exists on that date"
            }
        }
    }

    fun deleteHoliday(holiday: HolidayEntity) {
        viewModelScope.launch {
            try {
                holidayDao.delete(holiday)
                message.value = "Holiday removed"
            } catch (e: Exception) {
                error.value = e.message ?: "Could not remove holiday"
            }
        }
    }
}
