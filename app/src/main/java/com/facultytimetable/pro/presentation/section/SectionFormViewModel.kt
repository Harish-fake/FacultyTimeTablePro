package com.facultytimetable.pro.presentation.section

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.dao.RoomDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.SectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectionFormState(
    val name: String = "",
    val strength: String = "",
    val classAdvisor: String = "",
    val roomId: Long = -1L,
    val departmentId: Long? = null,
    val semesterId: Long? = null,
    val academicYearId: Long? = null,
    val departments: List<DepartmentEntity> = emptyList(),
    val semesters: List<SemesterEntity> = emptyList(),
    val academicYears: List<AcademicYearEntity> = emptyList(),
    val rooms: List<RoomEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SectionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sectionRepository: SectionRepository,
    private val departmentRepository: DepartmentRepository,
    private val semesterDao: SemesterDao,
    private val academicYearDao: AcademicYearDao,
    private val roomDao: RoomDao
) : ViewModel() {

    private val sectionId: Long? = savedStateHandle.get<Long>("sectionId")?.takeIf { it > 0 }

    private val _state = MutableStateFlow(SectionFormState())
    val state: StateFlow<SectionFormState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val departments = departmentRepository.getActiveDepartments().first()
            val semesters = semesterDao.getActiveSemesters().first()
            val academicYears = academicYearDao.getAllAcademicYears().first()
            val rooms = roomDao.getActiveRooms().first()

            if (sectionId != null) {
                val sec = sectionRepository.getSectionById(sectionId)
                if (sec != null) {
                    _state.value = _state.value.copy(
                        name = sec.name,
                        strength = sec.strength.toString(),
                        classAdvisor = sec.classAdvisor,
                        roomId = sec.roomId,
                        departmentId = sec.departmentId,
                        semesterId = sec.semesterId,
                        academicYearId = sec.academicYearId,
                        departments = departments,
                        semesters = semesters,
                        academicYears = academicYears,
                        rooms = rooms,
                        isEditing = true,
                        isLoading = false
                    )
                    return@launch
                }
            }
            _state.value = _state.value.copy(
                departments = departments,
                semesters = semesters,
                academicYears = academicYears,
                rooms = rooms,
                isLoading = false
            )
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value, error = null) }
    fun onStrengthChange(value: String) { _state.value = _state.value.copy(strength = value.filter { it.isDigit() }) }
    fun onClassAdvisorChange(value: String) { _state.value = _state.value.copy(classAdvisor = value) }
    fun onDepartmentSelected(id: Long) { _state.value = _state.value.copy(departmentId = id) }
    fun onSemesterSelected(semester: SemesterEntity) { _state.value = _state.value.copy(semesterId = semester.id) }
    fun onAcademicYearSelected(year: AcademicYearEntity) { _state.value = _state.value.copy(academicYearId = year.id) }
    fun onRoomSelected(room: RoomEntity) { _state.value = _state.value.copy(roomId = room.id) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Section name is required"); return }
        if (s.semesterId == null) { _state.value = s.copy(error = "Semester is required"); return }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val entity = SectionEntity(
                    id = sectionId ?: 0,
                    name = s.name.trim(),
                    semesterId = s.semesterId,
                    departmentId = s.departmentId ?: 0,
                    academicYearId = s.academicYearId ?: 0,
                    strength = s.strength.toIntOrNull() ?: 0,
                    classAdvisor = s.classAdvisor.trim(),
                    roomId = s.roomId
                )
                if (sectionId != null) sectionRepository.update(entity)
                else sectionRepository.insert(entity)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Failed to save section")
            }
        }
    }
}
