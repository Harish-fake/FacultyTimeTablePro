package com.facultytimetable.pro.presentation.academicyear

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
import com.facultytimetable.pro.domain.repository.AcademicYearRepository
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppFAB
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ColorChip
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.SearchBar
import com.facultytimetable.pro.presentation.navigation.Routes
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
    val academicYears: List<AcademicYearEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class AcademicYearListViewModel @Inject constructor(
    private val academicYearRepository: AcademicYearRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val state: StateFlow<AcademicYearListState> = combine(
        academicYearRepository.getAllAcademicYears(),
        searchQuery
    ) { years, query ->
        val filtered = if (query.isBlank()) years
        else years.filter { it.name.contains(query, ignoreCase = true) }
        AcademicYearListState(academicYears = filtered, searchQuery = query, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AcademicYearListState())

    fun onSearchQueryChange(query: String) { searchQuery.value = query }

    fun deleteAcademicYear(year: AcademicYearEntity) {
        viewModelScope.launch { academicYearRepository.delete(year) }
    }

    fun formatDate(epoch: Long): String = dateFormat.format(Date(epoch))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicYearListScreen(
    navController: NavController,
    viewModel: AcademicYearListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<AcademicYearEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Academic Years",
            onBackClick = { navController.popBackStack() }
        )

        SearchBar(
            query = state.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            placeholder = "Search academic years...",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (state.isLoading) {
            LoadingState()
        } else if (state.academicYears.isEmpty()) {
            EmptyState(
                title = "No Academic Years",
                message = if (state.searchQuery.isNotBlank()) "No academic years matching your search"
                else "Add an academic year to get started"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(state.academicYears, key = { it.id }) { year ->
                    AcademicYearCard(
                        year = year,
                        dateFormatter = viewModel::formatDate,
                        onEdit = { navController.navigate(Routes.academicYearForm(year.id)) },
                        onDelete = { showDeleteDialog = year }
                    )
                }
            }
        }

        AppFAB(onClick = { navController.navigate(Routes.academicYearForm()) })
    }

    showDeleteDialog?.let { year ->
        ConfirmDialog(
            title = "Delete Academic Year",
            message = "Delete ${year.name}? This will also delete all semesters in this year.",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteAcademicYear(year)
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null },
            isDestructive = true
        )
    }
}

@Composable
private fun AcademicYearCard(
    year: AcademicYearEntity,
    dateFormatter: (Long) -> String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = year.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (year.isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ColorChip(label = "Current", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    text = "${dateFormatter(year.startDate)} - ${dateFormatter(year.endDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
