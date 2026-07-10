package com.facultytimetable.pro.presentation.holiday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.HolidayDao
import com.facultytimetable.pro.data.local.db.entity.HolidayEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HolidayListState(
    val holidays: List<HolidayEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class HolidayListViewModel @Inject constructor(
    private val holidayDao: HolidayDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())

    val state: StateFlow<HolidayListState> = combine(
        holidayDao.getAllHolidays(),
        _searchQuery
    ) { holidays, query ->
        val filtered = if (query.isBlank()) holidays
        else holidays.filter { it.name.contains(query, ignoreCase = true) }
        HolidayListState(
            holidays = filtered,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HolidayListState())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteHoliday(holiday: HolidayEntity) {
        viewModelScope.launch {
            holidayDao.deleteById(holiday.id)
        }
    }

    fun restoreHoliday(holiday: HolidayEntity) {
        viewModelScope.launch {
            holidayDao.insert(holiday)
        }
    }

    fun formatDate(epoch: Long): String = dateFormat.format(Date(epoch))
}
