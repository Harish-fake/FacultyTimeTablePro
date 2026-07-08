package com.facultytimetable.pro.presentation.room

import androidx.lifecycle.ViewModel
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.domain.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.viewModelScope

data class RoomListState(
    val rooms: List<RoomEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class RoomListViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel() {

    val state: StateFlow<RoomListState> = roomRepository.getAllRooms()
        .map { rooms -> RoomListState(rooms = rooms, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoomListState())

    fun deleteRoom(room: RoomEntity) {
        viewModelScope.launch { roomRepository.delete(room) }
    }
}
