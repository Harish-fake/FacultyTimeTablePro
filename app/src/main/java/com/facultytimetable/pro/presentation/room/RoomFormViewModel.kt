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
                        name = room.name, capacity = room.capacity.toString(),
                        type = room.type, building = room.building, isLoading = false
                    )
                }
            }
        } else {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onCapacityChange(v: String) { _state.value = _state.value.copy(capacity = v) }
    fun onTypeChange(t: RoomType) { _state.value = _state.value.copy(type = t) }
    fun onBuildingChange(v: String) { _state.value = _state.value.copy(building = v) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Name is required"); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val entity = RoomEntity(
                    id = roomId ?: 0, name = s.name,
                    capacity = s.capacity.toIntOrNull() ?: 60,
                    type = s.type, building = s.building
                )
                if (roomId != null) roomRepository.update(entity)
                else roomRepository.insert(entity)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }
}
