package com.facultytimetable.pro.presentation.lab

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.LabEntity
import com.facultytimetable.pro.presentation.common.components.AppFAB
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.ProfessionalEmptyState
import com.facultytimetable.pro.presentation.common.components.SearchBar
import com.facultytimetable.pro.presentation.common.components.UndoSnackbarHost
import com.facultytimetable.pro.presentation.common.components.showUndoSnackbar
import com.facultytimetable.pro.presentation.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabListScreen(
    navController: NavController,
    viewModel: LabListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<LabEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val hasItems = state.labs.isNotEmpty()

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (state.labs.size == 1) "1 Lab" else "${state.labs.size} Labs",
                actions = {}
            )
        },
        floatingActionButton = {
            AppFAB(
                onClick = { navController.navigate(Routes.labForm()) },
                extended = hasItems,
                scrollState = listState
            )
        },
        snackbarHost = { UndoSnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Search by name, code, or department...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.isLoading) {
                LoadingState()
            } else if (state.labs.isEmpty()) {
                ProfessionalEmptyState(
                    icon = Icons.Default.Science,
                    title = if (state.searchQuery.isNotBlank()) "No Results Found" else "No Labs",
                    description = if (state.searchQuery.isNotBlank()) "Try adjusting your search terms"
                    else "Add your first lab to get started"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(state.labs, key = { it.id }) { lab ->
                        LabCard(
                            lab = lab,
                            departmentName = state.departmentNames[lab.departmentId] ?: "",
                            onEdit = { navController.navigate(Routes.labForm(lab.id)) },
                            onDelete = { showDeleteDialog = lab }
                        )
                    }
                }
            }
        }
    }

    showDeleteDialog?.let { lab ->
        ConfirmDialog(
            title = "Delete Lab",
            message = "Are you sure you want to delete ${lab.name}?",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteLab(lab)
                showDeleteDialog = null
                scope.launch {
                    snackbarHostState.showUndoSnackbar(
                        message = "${lab.name} deleted",
                        onUndo = { viewModel.restoreLab(lab) }
                    )
                }
            },
            onDismiss = { showDeleteDialog = null },
            isDestructive = true
        )
    }
}

@Composable
private fun LabCard(
    lab: LabEntity,
    departmentName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        lab.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(4.dp))
                    if (lab.roomNumber.isNotBlank()) {
                        Text(
                            lab.roomNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Capacity: ${lab.capacity}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (lab.equipment.isNotBlank()) {
                        Spacer(Modifier.size(8.dp))
                        Text(
                            lab.equipment,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
                if (departmentName.isNotBlank()) {
                    Text(
                        departmentName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
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
