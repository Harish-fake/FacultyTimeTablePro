package com.facultytimetable.pro.presentation.semester

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
import androidx.compose.material.icons.filled.Book
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import com.facultytimetable.pro.domain.repository.AcademicYearRepository
import com.facultytimetable.pro.domain.repository.SemesterRepository
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppFAB
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ColorChip
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SemesterListState(
    val semesters: List<SemesterEntity> = emptyList(),
    val academicYear: AcademicYearEntity? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class SemesterListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val semesterRepository: SemesterRepository,
    private val academicYearRepository: AcademicYearRepository
) : ViewModel() {

    private val academicYearId: Long = savedStateHandle.get<Long>("academicYearId") ?: 0L

    val state: StateFlow<SemesterListState> = combine(
        semesterRepository.getByAcademicYear(academicYearId),
        MutableStateFlow(Unit)
    ) { semesters, _ ->
        SemesterListState(
            semesters = semesters,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SemesterListState())

    init {
        viewModelScope.launch {
            val year = academicYearRepository.getById(academicYearId)
            _academicYear.value = year
        }
    }

    private val _academicYear = MutableStateFlow<AcademicYearEntity?>(null)
    val academicYear: StateFlow<AcademicYearEntity?> = _academicYear

    fun deleteSemester(semester: SemesterEntity) {
        viewModelScope.launch { semesterRepository.delete(semester) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterListScreen(
    navController: NavController,
    viewModel: SemesterListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val academicYear by viewModel.academicYear.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<SemesterEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = academicYear?.name ?: "Semesters",
            onBackClick = { navController.popBackStack() }
        )

        if (state.isLoading) {
            LoadingState()
        } else if (state.semesters.isEmpty()) {
            EmptyState(
                title = "No Semesters",
                message = "Add semesters to this academic year to get started"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(state.semesters, key = { it.id }) { semester ->
                    SemesterCard(
                        semester = semester,
                        onEdit = { navController.navigate(Routes.semesterForm(semester.id)) },
                        onDelete = { showDeleteDialog = semester }
                    )
                }
            }
        }

        AppFAB(onClick = { navController.navigate(Routes.semesterForm()) })
    }

    showDeleteDialog?.let { semester ->
        ConfirmDialog(
            title = "Delete Semester",
            message = "Delete ${semester.name}? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteSemester(semester)
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null },
            isDestructive = true
        )
    }
}

@Composable
private fun SemesterCard(
    semester: SemesterEntity,
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
                Icons.Default.Book,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Semester ${semester.semesterNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (semester.isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ColorChip(label = "Active", color = MaterialTheme.colorScheme.primary)
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                        ColorChip(label = "Inactive", color = MaterialTheme.colorScheme.error)
                    }
                }
                Text(
                    text = semester.name,
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
