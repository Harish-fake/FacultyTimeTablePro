package com.facultytimetable.pro.presentation.lab

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.LabEntity
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.LabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LabFormState(
    val name: String = "",
    val roomNumber: String = "",
    val capacity: String = "30",
    val departmentId: Long = 0,
    val building: String = "",
    val floor: String = "",
    val equipment: String = "",
    val availableSystems: String = "0",
    val hasProjector: Boolean = true,
    val hasAC: Boolean = false,
    val inCharge: String = "",
    val departments: List<DepartmentEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class LabFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val labRepository: LabRepository,
    private val departmentRepository: DepartmentRepository
) : ViewModel() {

    private val labId: Long? = savedStateHandle.get<Long>("labId")?.takeIf { it > 0 }

    private val _state = MutableStateFlow(LabFormState())

    val state: StateFlow<LabFormState> = combine(
        departmentRepository.getAllDepartments(),
        _state
    ) { departments, s ->
        s.copy(departments = departments)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LabFormState())

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            if (labId != null) {
                val lab = labRepository.getLabById(labId)
                if (lab != null) {
                    _state.value = _state.value.copy(
                        name = lab.name,
                        roomNumber = lab.roomNumber,
                        capacity = lab.capacity.toString(),
                        departmentId = lab.departmentId,
                        building = lab.building,
                        floor = lab.floor,
                        equipment = lab.equipment,
                        availableSystems = lab.availableSystems.toString(),
                        hasProjector = lab.hasProjector,
                        hasAC = lab.hasAC,
                        inCharge = lab.inCharge,
                        isEditing = true,
                        isLoading = false
                    )
                    return@launch
                }
            }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onRoomNumberChange(v: String) { _state.value = _state.value.copy(roomNumber = v) }
    fun onCapacityChange(v: String) { _state.value = _state.value.copy(capacity = v.filter { it.isDigit() }) }
    fun onDepartmentChange(id: Long) { _state.value = _state.value.copy(departmentId = id) }
    fun onBuildingChange(v: String) { _state.value = _state.value.copy(building = v) }
    fun onFloorChange(v: String) { _state.value = _state.value.copy(floor = v) }
    fun onEquipmentChange(v: String) { _state.value = _state.value.copy(equipment = v) }
    fun onAvailableSystemsChange(v: String) { _state.value = _state.value.copy(availableSystems = v.filter { it.isDigit() }) }
    fun onProjectorChange(v: Boolean) { _state.value = _state.value.copy(hasProjector = v) }
    fun onACChange(v: Boolean) { _state.value = _state.value.copy(hasAC = v) }
    fun onInChargeChange(v: String) { _state.value = _state.value.copy(inCharge = v) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Lab name is required"); return }
        if (s.departmentId == 0L) { _state.value = s.copy(error = "Department is required"); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val entity = LabEntity(
                    id = labId ?: 0,
                    name = s.name.trim(),
                    roomNumber = s.roomNumber.trim(),
                    capacity = s.capacity.toIntOrNull() ?: 30,
                    departmentId = s.departmentId,
                    building = s.building.trim(),
                    floor = s.floor.trim(),
                    equipment = s.equipment.trim(),
                    availableSystems = s.availableSystems.toIntOrNull() ?: 0,
                    hasProjector = s.hasProjector,
                    hasAC = s.hasAC,
                    inCharge = s.inCharge.trim()
                )
                if (labId != null) labRepository.update(entity)
                else labRepository.insert(entity)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Failed to save lab")
            }
        }
    }
}
