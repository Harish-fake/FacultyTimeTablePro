package com.facultytimetable.pro.presentation.subject

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppFAB
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.SearchBar
import com.facultytimetable.pro.presentation.navigation.Routes
import com.facultytimetable.pro.presentation.theme.SubjectLab
import com.facultytimetable.pro.presentation.theme.SubjectLibrary
import com.facultytimetable.pro.presentation.theme.SubjectProject
import com.facultytimetable.pro.presentation.theme.SubjectSeminar
import com.facultytimetable.pro.presentation.theme.SubjectSports
import com.facultytimetable.pro.presentation.theme.SubjectTheory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectListState(
    val subjects: List<SubjectEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val departmentMap: Map<Long, String> = emptyMap()
)

@HiltViewModel
class SubjectListViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val departmentRepository: DepartmentRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val state: StateFlow<SubjectListState> = combine(
        subjectRepository.getAllSubjects(),
        departmentRepository.getAllDepartments(),
        _searchQuery
    ) { subjects, departments, query ->
        val deptMap = departments.associate { it.id to it.name }
        val filtered = if (query.isBlank()) subjects
        else subjects.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.code.contains(query, ignoreCase = true)
        }
        SubjectListState(
            subjects = filtered,
            searchQuery = query,
            isLoading = false,
            departmentMap = deptMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubjectListState())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch { subjectRepository.delete(subject) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectListScreen(
    navController: NavController,
    viewModel: SubjectListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<SubjectEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Subjects", onBackClick = { navController.popBackStack() })

        Column(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                LoadingState()
            } else {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = "Search by name or code...",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (state.subjects.isEmpty()) {
                    EmptyState(
                        title = "No Subjects",
                        message = if (state.searchQuery.isNotBlank()) "No subjects match your search"
                        else "Add subjects to get started with timetable creation"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(state.subjects, key = { it.id }) { subject ->
                            SubjectCard(
                                subject = subject,
                                departmentName = state.departmentMap[subject.departmentId] ?: "",
                                onEdit = { navController.navigate(Routes.subjectForm(subject.id)) },
                                onDelete = { showDeleteDialog = subject }
                            )
                        }
                    }
                }
            }
        }

        AppFAB(onClick = { navController.navigate(Routes.subjectForm()) })
    }

    showDeleteDialog?.let { subject ->
        ConfirmDialog(
            title = "Delete Subject",
            message = "Are you sure you want to delete ${subject.name} (${subject.code})?",
            confirmText = "Delete",
            onConfirm = { viewModel.deleteSubject(subject); showDeleteDialog = null },
            onDismiss = { showDeleteDialog = null },
            isDestructive = true
        )
    }
}

@Composable
private fun SubjectCard(
    subject: SubjectEntity,
    departmentName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = when (subject.type) {
        SubjectType.LAB -> SubjectLab
        SubjectType.PROJECT -> SubjectProject
        SubjectType.SEMINAR -> SubjectSeminar
        SubjectType.LIBRARY -> SubjectLibrary
        SubjectType.SPORTS -> SubjectSports
        else -> SubjectTheory
    }
    val typeLabel = when (subject.type) {
        SubjectType.THEORY -> "Theory"
        SubjectType.LAB -> "Lab"
        SubjectType.PROJECT -> "Project"
        SubjectType.SEMINAR -> "Seminar"
        SubjectType.LIBRARY -> "Library"
        SubjectType.SPORTS -> "Sports"
    }
    val hoursLabel = when (subject.type) {
        SubjectType.THEORY -> "Lecture"
        SubjectType.LAB -> "Lab"
        else -> typeLabel
    }

    AppCard(modifier = Modifier.animateContentSize(animationSpec = spring(stiffness = 300f))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(typeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (subject.type) {
                        SubjectType.LAB -> Icons.Default.Science
                        else -> Icons.Default.Book
                    },
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = typeColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subject.code,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (departmentName.isNotBlank()) {
                        Text(
                            text = " | $departmentName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(typeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(typeLabel, style = MaterialTheme.typography.labelSmall, color = typeColor)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${subject.hoursPerWeek}h $hoursLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
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
