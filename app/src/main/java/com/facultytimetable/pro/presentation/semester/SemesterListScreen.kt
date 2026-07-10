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
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppFAB
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ColorChip
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.SearchBar
import com.facultytimetable.pro.presentation.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterListScreen(
    navController: NavController,
    viewModel: SemesterListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<SemesterEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Semesters",
            onBackClick = { navController.popBackStack() },
            actions = {
                Text(
                    text = "${state.semesters.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        )

        SearchBar(
            query = state.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            placeholder = "Search by name, year, or number...",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (state.isLoading) {
            LoadingState()
        } else if (state.semesters.isEmpty()) {
            EmptyState(
                title = if (state.searchQuery.isNotBlank()) "No Results Found" else "No Semesters",
                message = if (state.searchQuery.isNotBlank()) "Try adjusting your search terms"
                else "Add semesters for your academic years"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(state.semesters, key = { it.semester.id }) { item ->
                    AppCard(
                        modifier = Modifier.animateContentSize(),
                        onClick = { navController.navigate(Routes.semesterForm(item.semester.id)) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Book, contentDescription = null,
                                modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.semester.name, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Sem ${item.semester.semesterNumber} | ${item.yearName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val typeColor = when (item.semester.semesterType.uppercase()) {
                                "ODD" -> MaterialTheme.colorScheme.tertiary
                                "EVEN" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            }
                            ColorChip(
                                label = item.semester.semesterType.replaceFirstChar { it.uppercase() },
                                color = typeColor
                            )
                            if (item.semester.isActive) {
                                Spacer(modifier = Modifier.width(4.dp))
                                ColorChip(label = "Active", color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { navController.navigate(Routes.semesterForm(item.semester.id)) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { showDeleteDialog = item.semester }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        AppFAB(onClick = { navController.navigate(Routes.semesterForm()) })
    }

    showDeleteDialog?.let { semester ->
        ConfirmDialog(
            title = "Delete Semester",
            message = "Delete ${semester.name}? All sections in this semester will also be deleted.",
            confirmText = "Delete",
            onConfirm = { viewModel.deleteSemester(semester); showDeleteDialog = null },
            onDismiss = { showDeleteDialog = null },
            isDestructive = true
        )
    }
}
