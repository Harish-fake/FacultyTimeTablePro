package com.facultytimetable.pro.presentation.academicyear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
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

data class AcademicYearListState(
    val years: List<AcademicYearEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class AcademicYearListViewModel @Inject constructor(
    private val academicYearDao: AcademicYearDao
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    val state: StateFlow<AcademicYearListState> = combine(
        academicYearDao.getAllAcademicYears(),
        searchQuery
    ) { years, query ->
        val filtered = if (query.isBlank()) years
        else years.filter { it.name.contains(query, ignoreCase = true) }
        AcademicYearListState(years = filtered, searchQuery = query, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AcademicYearListState())

    fun onSearchQueryChange(query: String) { searchQuery.value = query }

    fun deleteYear(year: AcademicYearEntity) {
        viewModelScope.launch { academicYearDao.delete(year) }
    }

    fun restoreAcademicYear(year: AcademicYearEntity) {
        viewModelScope.launch { academicYearDao.insert(year) }
    }

    fun formatDate(epoch: Long): String = dateFormat.format(Date(epoch))
}
