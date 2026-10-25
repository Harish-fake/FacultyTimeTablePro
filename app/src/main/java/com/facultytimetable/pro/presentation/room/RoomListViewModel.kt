package com.facultytimetable.pro.presentation.room

import androidx.lifecycle.ViewModel
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.domain.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.viewModelScope

data class RoomListState(
    val rooms: List<RoomEntity> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class RoomListViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<RoomListState> = combine(
        roomRepository.getAllRooms(),
        message
    ) { rooms, msg ->
        RoomListState(rooms = rooms, isLoading = false, message = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoomListState())

    fun consumeMessage() { message.value = null }

    fun deleteRoom(room: RoomEntity) {
        viewModelScope.launch {
            runCatching { roomRepository.delete(room) }
                .onSuccess { message.value = "${room.name} deleted" }
                .onFailure {
                    message.value = "Cannot delete ${room.name}: it is used by the timetable."
                }
        }
    }
}
