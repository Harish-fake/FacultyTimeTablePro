package com.facultytimetable.pro.presentation.recyclebin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.entity.RecycleBinEntity
import com.facultytimetable.pro.domain.repository.RecycleBinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecycleBinState(
    val items: List<RecycleBinEntity> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = ""
)

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val recycleBinRepository: RecycleBinRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecycleBinState())
    val state: StateFlow<RecycleBinState> = _state

    init { loadItems() }

    fun loadItems() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            recycleBinRepository.getAllItems().collect { items ->
                _state.value = _state.value.copy(items = items, isLoading = false)
            }
        }
    }

    fun restoreItem(item: RecycleBinEntity) {
        viewModelScope.launch {
            recycleBinRepository.restore(item)
            loadItems()
        }
    }

    fun permanentlyDeleteItem(item: RecycleBinEntity) {
        viewModelScope.launch {
            recycleBinRepository.permanentlyDelete(item)
            loadItems()
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            recycleBinRepository.emptyAll()
            loadItems()
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }
}
