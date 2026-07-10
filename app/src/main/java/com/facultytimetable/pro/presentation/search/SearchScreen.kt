package com.facultytimetable.pro.presentation.search

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ProfessionalEmptyState
import com.facultytimetable.pro.presentation.common.components.SearchBar
import com.facultytimetable.pro.presentation.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Search", onBackClick = { navController.popBackStack() })

        SearchBar(
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
            placeholder = "Search faculty, subject, room, department...",
            modifier = Modifier.padding(16.dp)
        )

        if (state.query.isBlank()) {
            Text(
                text = "Search across faculty, subjects, departments, rooms, labs, sections, holidays, semesters & more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (state.isSearching) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
            )
        } else if (state.results.isEmpty() && state.query.isNotBlank()) {
            ProfessionalEmptyState(
                icon = Icons.Default.SearchOff,
                title = "No Results Found",
                description = "No matches found for \"${state.query}\". Try different keywords."
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            state.groupedResults.forEach { (type, items) ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            typeIcon(type),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = type,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${items.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(items) { suggestion ->
                    ResultRow(suggestion = suggestion, navController = navController)
                }
            }
        }
    }
}

@Composable
private fun ResultRow(suggestion: SearchSuggestion, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when (suggestion.type) {
                    "Faculty" -> navController.navigate(Routes.facultyDetail(suggestion.id))
                    "Subject" -> navController.navigate(Routes.subjectForm(suggestion.id))
                    "Room" -> navController.navigate(Routes.roomForm(suggestion.id))
                    "Department" -> navController.navigate(Routes.departmentForm(suggestion.id))
                    "Lab" -> navController.navigate(Routes.labForm(suggestion.id))
                    "Section" -> navController.navigate(Routes.sectionForm(suggestion.id))
                    "Holiday" -> navController.navigate(Routes.holidayForm(suggestion.id))
                    "Semester" -> navController.navigate(Routes.semesterForm(suggestion.id))
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            typeIcon(suggestion.type),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                suggestion.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Row {
                Text(
                    suggestion.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    suggestion.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

private fun typeIcon(type: String): ImageVector = when (type) {
    "Faculty" -> Icons.Default.People
    "Subject" -> Icons.Default.Book
    "Room" -> Icons.Default.MeetingRoom
    "Department" -> Icons.Default.School
    "Lab" -> Icons.Default.Science
    "Section" -> Icons.Default.ViewModule
    "Holiday" -> Icons.Default.Event
    "Semester" -> Icons.Default.Schedule
    "Academic Year" -> Icons.Default.DateRange
    else -> Icons.Default.School
}
