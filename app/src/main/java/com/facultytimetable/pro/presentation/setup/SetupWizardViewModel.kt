package com.facultytimetable.pro.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.dao.WorkingDayDao
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyAssignmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.LabRepository
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

data class StepState(
    val id: Int,
    val isCompleted: Boolean = false,
    val isUnlocked: Boolean = false,
    val count: Int = 0
)

data class SetupWizardState(
    val completedSteps: Int = 0,
    val totalSteps: Int = setupSteps.size,
    val currentStepId: Int = 1,
    val completionProgress: Float = 0f,
    val setupComplete: Boolean = false,
    val stepStates: List<StepState> = emptyList()
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
    private val semesterDao: SemesterDao,
    private val labRepository: LabRepository,
    private val facultyAssignmentRepository: FacultyAssignmentRepository,
    private val workingDayDao: WorkingDayDao
) : ViewModel() {

    private val _state = MutableStateFlow(SetupWizardState())
    val state: StateFlow<SetupWizardState> = _state

    init { checkProgress() }

    fun checkProgress() {
        viewModelScope.launch {
            val counts = mutableMapOf<Int, Int>()

            val deptCount = departmentRepository.getCount().also { counts[1] = it }
            val yearCount = academicYearDao.getCount().also { counts[2] = it }
            val semCount = semesterDao.getCount().also { counts[3] = it }
            val secCount = sectionRepository.getCount().also { counts[4] = it }
            val wdCount = workingDayDao.getWorkingDayCount().also { counts[5] = it }
            val slotCount = timeSlotRepository.getCount().also { counts[6] = it }
            val roomCount = roomRepository.getCount().also { counts[7] = it }
            val labCount = labRepository.getCount().also { counts[8] = it }
            val facCount = facultyRepository.getCount().also { counts[9] = it }
            val subjCount = subjectRepository.getCount().also { counts[10] = it }
            val assignCount = facultyAssignmentRepository.getCount().also { counts[11] = it }
            val ttCount = timetableRepository.getCount().also { counts[12] = it }

            val completed = mutableSetOf<Int>()
            if (deptCount > 0) completed.add(1)
            if (yearCount > 0) completed.add(2)
            if (semCount > 0) completed.add(3)
            if (secCount > 0) completed.add(4)
            if (wdCount > 0) completed.add(5)
            if (slotCount > 0) completed.add(6)
            if (roomCount > 0) completed.add(7)
            if (labCount > 0) completed.add(8)
            if (facCount > 0) completed.add(9)
            if (subjCount > 0) completed.add(10)
            if (assignCount > 0) completed.add(11)
            if (ttCount > 0) completed.add(12)

            val nextStep = (1..12).firstOrNull { it !in completed } ?: 12

            val stepStates = setupSteps.map { step ->
                val isCompleted = step.id in completed
                val previousCompleted = step.id == 1 || (step.id - 1) in completed
                StepState(
                    id = step.id,
                    isCompleted = isCompleted,
                    isUnlocked = previousCompleted || isCompleted,
                    count = counts[step.id] ?: 0
                )
            }

            _state.value = SetupWizardState(
                completedSteps = completed.size,
                currentStepId = nextStep,
                completionProgress = if (completed.isEmpty()) 0f else completed.size.toFloat() / 12f,
                setupComplete = completed.size >= 12,
                stepStates = stepStates
            )
        }
    }
}
