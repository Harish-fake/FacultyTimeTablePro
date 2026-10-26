package com.facultytimetable.pro.presentation.sampledata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.seed.DatabaseSeeder
import com.facultytimetable.pro.data.local.seed.SampleDataProvider
import com.facultytimetable.pro.data.local.seed.SeedResult
import com.facultytimetable.pro.domain.usecase.timetable.AssignmentPlanner
import com.facultytimetable.pro.domain.usecase.timetable.requiredPeriods
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SampleSectionPreview(
    val name: String,
    val departmentCode: String,
    val strength: Int,
    val subjectCount: Int,
    val expectedPeriods: Int
)

data class SampleDataState(
    val departments: Int = 0,
    val faculty: Int = 0,
    val subjects: Int = 0,
    val rooms: Int = 0,
    val sections: Int = 0,
    val timeSlots: Int = 0,
    val expectedPeriods: Int = 0,
    val sectionPreviews: List<SampleSectionPreview> = emptyList(),
    val isRunning: Boolean = false,
    val result: SeedResult? = null,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class SampleDataViewModel @Inject constructor(
    private val databaseSeeder: DatabaseSeeder
) : ViewModel() {

    private val _state = MutableStateFlow(previewState())
    val state: StateFlow<SampleDataState> = _state

    /**
     * Replaces all timetable data with the shipped sample college and immediately generates
     * a timetable. The generated result is validated by [DatabaseSeeder] before it is saved.
     */
    fun loadSampleData() {
        if (_state.value.isRunning) return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isRunning = true,
                result = null,
                message = null,
                error = null
            )
            runCatching { databaseSeeder.resetToSampleData() }
                .onSuccess { result ->
                    _state.value = _state.value.copy(
                        isRunning = false,
                        result = result,
                        message = if (result.isComplete) {
                            "Sample timetable generated: ${result.scheduledPeriods} of ${result.expectedPeriods} periods, no conflicts."
                        } else {
                            "Sample data loaded, but the timetable needs attention."
                        }
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isRunning = false,
                        error = error.message ?: "Could not load the sample data"
                    )
                }
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun consumeError() {
        _state.value = _state.value.copy(error = null)
    }

    private companion object {
        fun previewState(): SampleDataState {
            val data = SampleDataProvider.bundle()
            val plan = AssignmentPlanner().plan(data.sections, data.subjects, data.faculty, data.departments)
            val previews = data.sections.sortedBy { it.name }.map { section ->
                SampleSectionPreview(
                    name = section.name,
                    departmentCode = data.departments.firstOrNull { it.id == section.departmentId }?.code ?: "",
                    strength = section.strength,
                    subjectCount = plan.assignments.count { it.section.id == section.id },
                    expectedPeriods = plan.assignments
                        .filter { it.section.id == section.id }
                        .sumOf { it.subject.requiredPeriods }
                )
            }
            return SampleDataState(
                departments = data.departments.size,
                faculty = data.faculty.size,
                subjects = data.subjects.size,
                rooms = data.rooms.size,
                sections = data.sections.size,
                timeSlots = data.timeSlots.size,
                expectedPeriods = previews.sumOf { it.expectedPeriods },
                sectionPreviews = previews
            )
        }
    }
}
