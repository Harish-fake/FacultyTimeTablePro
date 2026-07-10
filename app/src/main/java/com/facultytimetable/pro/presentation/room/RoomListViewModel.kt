package com.facultytimetable.pro.presentation.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.RoomType
import com.facultytimetable.pro.domain.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomListState(
    val rooms: List<RoomEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedType: RoomType? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class RoomListViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedType = MutableStateFlow<RoomType?>(null)

    val state: StateFlow<RoomListState> = combine(
        roomRepository.getAllRooms(),
        searchQuery,
        selectedType
    ) { rooms, query, type ->
        val filtered = rooms.filter { room ->
            val matchesQuery = query.isBlank() ||
                room.name.contains(query, ignoreCase = true) ||
                room.building.contains(query, ignoreCase = true) ||
                room.type.name.contains(query, ignoreCase = true)
            val matchesType = type == null || room.type == type
            matchesQuery && matchesType
        }
        RoomListState(
            rooms = filtered,
            searchQuery = query,
            selectedType = type,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoomListState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onTypeFilterChange(type: RoomType?) {
        selectedType.value = type
    }

    fun deleteRoom(room: RoomEntity) {
        viewModelScope.launch { roomRepository.delete(room) }
    }
}
