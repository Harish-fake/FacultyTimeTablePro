package com.facultytimetable.pro.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.RoomType
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupWizardState(
    val completedSteps: Int = 0,
    val totalSteps: Int = setupSteps.size,
    val currentStepId: Int = 1,
    val completedStepIds: Set<Int> = emptySet(),
    val completionProgress: Float = 0f
)

@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    private val departmentRepository: DepartmentRepository,
    private val facultyRepository: FacultyRepository,
    private val subjectRepository: SubjectRepository,
    private val roomRepository: RoomRepository,
    private val sectionRepository: SectionRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val timetableRepository: TimetableRepository,
    private val academicYearDao: AcademicYearDao,
    private val semesterDao: SemesterDao
) : ViewModel() {

    private val _state = MutableStateFlow(SetupWizardState())
    val state: StateFlow<SetupWizardState> = _state

    init { checkProgress() }

    fun checkProgress() {
        viewModelScope.launch {
            val completed = mutableSetOf<Int>()

            val deptCount = departmentRepository.getCount()
            if (deptCount > 0) { completed.add(1); completed.add(2) }

            val yearCount = academicYearDao.getCount()
            if (yearCount > 0) { completed.add(3) }

            val semCount = semesterDao.getCount()
            if (semCount > 0) { completed.add(4) }

            val secCount = sectionRepository.getCount()
            if (secCount > 0) { completed.add(5) }

            val slotCount = timeSlotRepository.getCount()
            if (slotCount > 0) { completed.add(6); completed.add(7) }

            val roomCount = roomRepository.getCount()
            if (roomCount > 0) { completed.add(8) }

            val labFlow = roomRepository.getRoomCountByType(RoomType.LAB)
            val labCount = labFlow.first()
            if (labCount > 0) { completed.add(9) }

            val facCount = facultyRepository.getCount()
            if (facCount > 0) { completed.add(10) }

            val subjCount = subjectRepository.getCount()
            if (subjCount > 0) { completed.add(11); completed.add(12) }

            val ttCount = timetableRepository.getCount()
            if (ttCount > 0) { completed.add(13) }

            val nextStep = (1..13).firstOrNull { it !in completed } ?: 13

            _state.value = SetupWizardState(
                completedSteps = completed.size,
                completedStepIds = completed,
                currentStepId = nextStep,
                completionProgress = if (completed.isEmpty()) 0f else completed.size.toFloat() / 13f
            )
        }
    }
}
