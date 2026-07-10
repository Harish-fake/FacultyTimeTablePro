package com.facultytimetable.pro.presentation.room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.RoomType
import com.facultytimetable.pro.domain.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomFormState(
    val name: String = "",
    val capacity: String = "60",
    val type: RoomType = RoomType.CLASSROOM,
    val building: String = "",
    val floor: String = "",
    val roomNumber: String = "",
    val hasProjector: Boolean = true,
    val hasAC: Boolean = false,
    val hasSmartBoard: Boolean = false,
    val equipment: String = "",
    val availability: String = "",
    val isLab: Boolean = false,
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class RoomFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val roomId: Long? = savedStateHandle.get<Long>("roomId")?.takeIf { it > 0 }

    private val _state = MutableStateFlow(RoomFormState())
    val state: StateFlow<RoomFormState> = _state

    init { loadData() }

    private fun loadData() {
        if (roomId != null) {
            viewModelScope.launch {
                val room = roomRepository.getRoomById(roomId)
                if (room != null) {
                    _state.value = _state.value.copy(
                        name = room.name,
                        capacity = room.capacity.toString(),
                        type = room.type,
                        building = room.building,
                        floor = room.floor,
                        roomNumber = room.roomNumber,
                        hasProjector = room.hasProjector,
                        hasAC = room.hasAC,
                        hasSmartBoard = room.hasSmartBoard,
                        equipment = room.equipment,
                        availability = room.availability,
                        isLab = room.isLab,
                        isEditing = true,
                        isLoading = false
                    )
                    return@launch
                }
            }
        } else {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value, error = null) }
    fun onCapacityChange(value: String) { _state.value = _state.value.copy(capacity = value.filter { it.isDigit() }) }
    fun onTypeChange(type: RoomType) { _state.value = _state.value.copy(type = type) }
    fun onBuildingChange(value: String) { _state.value = _state.value.copy(building = value, error = null) }
    fun onFloorChange(value: String) { _state.value = _state.value.copy(floor = value) }
    fun onRoomNumberChange(value: String) { _state.value = _state.value.copy(roomNumber = value) }
    fun onProjectorChange(value: Boolean) { _state.value = _state.value.copy(hasProjector = value) }
    fun onACChange(value: Boolean) { _state.value = _state.value.copy(hasAC = value) }
    fun onSmartBoardChange(value: Boolean) { _state.value = _state.value.copy(hasSmartBoard = value) }
    fun onEquipmentChange(value: String) { _state.value = _state.value.copy(equipment = value) }
    fun onAvailabilityChange(value: String) { _state.value = _state.value.copy(availability = value) }
    fun onLabChange(value: Boolean) { _state.value = _state.value.copy(isLab = value) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Room name is required"); return }
        if (s.building.isBlank()) { _state.value = s.copy(error = "Building is required"); return }
        if (s.capacity.toIntOrNull() == null || (s.capacity.toIntOrNull() ?: 0) <= 0) {
            _state.value = s.copy(error = "Valid capacity is required"); return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val entity = RoomEntity(
                    id = roomId ?: 0,
                    name = s.name.trim(),
                    capacity = s.capacity.toIntOrNull() ?: 60,
                    type = s.type,
                    building = s.building.trim(),
                    floor = s.floor.trim(),
                    roomNumber = s.roomNumber.trim(),
                    hasProjector = s.hasProjector,
                    hasAC = s.hasAC,
                    hasSmartBoard = s.hasSmartBoard,
                    equipment = s.equipment.trim(),
                    availability = s.availability.trim(),
                    isLab = s.isLab
                )
                if (roomId != null) roomRepository.update(entity)
                else roomRepository.insert(entity)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Failed to save room")
            }
        }
    }
}
