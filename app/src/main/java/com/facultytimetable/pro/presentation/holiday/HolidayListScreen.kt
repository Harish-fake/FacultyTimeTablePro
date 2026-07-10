package com.facultytimetable.pro.presentation.holiday

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppFAB
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ColorChip
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
fun HolidayListScreen(
    navController: NavController,
    viewModel: HolidayListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<HolidayUi?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (state.holidays.size == 1) "1 Holiday" else "${state.holidays.size} Holidays",
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            AppFAB(
                onClick = { navController.navigate(Routes.holidayForm()) },
                extended = state.holidays.isNotEmpty(),
                scrollState = listState
            )
        },
        snackbarHost = { UndoSnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Search holidays...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.isLoading) {
                LoadingState()
            } else if (state.holidays.isEmpty()) {
                ProfessionalEmptyState(
                    icon = Icons.Default.CalendarMonth,
                    title = if (state.searchQuery.isNotBlank()) "No Results Found"
                    else "No Holidays Yet",
                    description = if (state.searchQuery.isNotBlank())
                        "No holidays match \"${state.searchQuery}\""
                    else "Add holidays to mark off-days in your timetable",
                    primaryButtonText = if (state.searchQuery.isBlank()) "Add Holiday" else null,
                    onPrimaryButtonClick = if (state.searchQuery.isBlank()) {{ navController.navigate(Routes.holidayForm()) }} else null
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(state.holidays, key = { it.id }) { holiday ->
                        AppCard(
                            onClick = { navController.navigate(Routes.holidayForm(holiday.id)) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = holiday.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        ColorChip(
                                            label = holiday.type.name,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                        if (holiday.isRecurring) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            ColorChip(
                                                label = "Repeats",
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = viewModel.formatDate(holiday.date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (holiday.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = holiday.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { navController.navigate(Routes.holidayForm(holiday.id)) }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { showDeleteDialog = holiday }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    showDeleteDialog?.let { holiday ->
        ConfirmDialog(
            title = "Delete Holiday",
            message = "Delete ${holiday.name} (${viewModel.formatDate(holiday.date)})?",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteHoliday(holiday)
                showDeleteDialog = null
                scope.launch {
                    snackbarHostState.showUndoSnackbar(
                        message = "${holiday.name} deleted",
                        onUndo = { viewModel.restoreHoliday(holiday) }
                    )
                }
            },
            onDismiss = { showDeleteDialog = null },
            isDestructive = true
        )
    }
}
