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
    val hasProjector: Boolean = true,
    val hasAC: Boolean = false,
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
    private val existing = MutableStateFlow<RoomEntity?>(null)
    private val _state = MutableStateFlow(RoomFormState())
    val state: StateFlow<RoomFormState> = _state

    init { loadData() }

    private fun loadData() {
        if (roomId != null) {
            viewModelScope.launch {
                try {
                    val room = roomRepository.getRoomById(roomId)
                    if (room != null) {
                        existing.value = room
                        _state.value = _state.value.copy(
                            name = room.name,
                            capacity = room.capacity.toString(),
                            type = room.type,
                            building = room.building,
                            floor = room.floor,
                            hasProjector = room.hasProjector,
                            hasAC = room.hasAC,
                            isLoading = false
                        )
                    } else {
                        _state.value = _state.value.copy(isLoading = false)
                    }
                } catch (e: Exception) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Could not load room"
                    )
                }
            }
        } else {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onCapacityChange(v: String) { _state.value = _state.value.copy(capacity = v.filter { it.isDigit() }) }
    fun onTypeChange(t: RoomType) { _state.value = _state.value.copy(type = t) }
    fun onBuildingChange(v: String) { _state.value = _state.value.copy(building = v) }
    fun onFloorChange(v: String) { _state.value = _state.value.copy(floor = v) }
    fun onProjectorChange(v: Boolean) { _state.value = _state.value.copy(hasProjector = v) }
    fun onACChange(v: Boolean) { _state.value = _state.value.copy(hasAC = v) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Name is required"); return }
        val capacity = s.capacity.toIntOrNull()
        if (capacity == null || capacity !in 1..1000) {
            _state.value = s.copy(error = "Capacity must be between 1 and 1000")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val base = existing.value
                val normalizedName = s.name.trim()
                val duplicate = roomRepository.getRoomByName(normalizedName)
                if (duplicate != null && duplicate.id != roomId) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = "A room named $normalizedName already exists"
                    )
                    return@launch
                }
                val entity = if (base == null) {
                    RoomEntity(
                        name = normalizedName,
                        capacity = capacity,
                        type = s.type,
                        building = s.building.trim(),
                        floor = s.floor.trim(),
                        hasProjector = s.hasProjector,
                        hasAC = s.hasAC
                    )
                } else {
                    base.copy(
                        name = normalizedName,
                        capacity = capacity,
                        type = s.type,
                        building = s.building.trim(),
                        floor = s.floor.trim(),
                        hasProjector = s.hasProjector,
                        hasAC = s.hasAC,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                if (base == null) roomRepository.insert(entity)
                else roomRepository.update(entity)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }
}
