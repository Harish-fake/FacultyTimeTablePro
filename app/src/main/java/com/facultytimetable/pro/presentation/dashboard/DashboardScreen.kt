package com.facultytimetable.pro.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppFAB
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.StatsCard
import com.facultytimetable.pro.presentation.navigation.Routes

data class DashboardQuickAction(
    val title: String,
    val icon: @Composable () -> Unit,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val quickActions = listOf(
        DashboardQuickAction("Search", { Icon(Icons.Default.Search, contentDescription = null) }, Routes.SEARCH),
        DashboardQuickAction("Faculty", { Icon(Icons.Default.People, contentDescription = null) }, Routes.FACULTY_LIST),
        DashboardQuickAction("Departments", { Icon(Icons.Default.School, contentDescription = null) }, Routes.DEPARTMENT_LIST),
        DashboardQuickAction("Subjects", { Icon(Icons.Default.Book, contentDescription = null) }, Routes.SUBJECT_LIST),
        DashboardQuickAction("Rooms", { Icon(Icons.Default.MeetingRoom, contentDescription = null) }, Routes.ROOM_LIST),
        DashboardQuickAction("Timetable", { Icon(Icons.Default.CalendarMonth, contentDescription = null) }, Routes.TIMETABLE_GRID)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Faculty TimeTable Pro",
            actions = {
                Text(
                    text = "College Edition",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        )

        if (state.isLoading) {
            LoadingState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        StatsCard(
                            title = "Departments",
                            value = state.departmentCount.toString(),
                            icon = { Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        StatsCard(
                            title = "Faculty",
                            value = state.facultyCount.toString(),
                            icon = { Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    item {
                        StatsCard(
                            title = "Subjects",
                            value = state.subjectCount.toString(),
                            icon = { Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    item {
                        StatsCard(
                            title = "Timetable Entries",
                            value = state.timetableCount.toString(),
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(quickActions) { action ->
                        com.facultytimetable.pro.presentation.common.components.AppCard(
                            onClick = { navController.navigate(action.route) },
                            modifier = Modifier.height(96.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                action.icon()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = action.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        AppFAB(onClick = { navController.navigate(Routes.TIMETABLE_GENERATOR) })
    }
}
