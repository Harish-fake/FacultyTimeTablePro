package com.facultytimetable.pro.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppCard
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
        DashboardQuickAction("Search", { Icon(Icons.Default.Search, null) }, Routes.SEARCH),
        DashboardQuickAction("Faculty", { Icon(Icons.Default.People, null) }, Routes.FACULTY_LIST),
        DashboardQuickAction("Dept", { Icon(Icons.Default.School, null) }, Routes.DEPARTMENT_LIST),
        DashboardQuickAction("Subjects", { Icon(Icons.Default.Book, null) }, Routes.SUBJECT_LIST),
        DashboardQuickAction("Rooms", { Icon(Icons.Default.MeetingRoom, null) }, Routes.ROOM_LIST),
        DashboardQuickAction("Timetable", { Icon(Icons.Default.CalendarMonth, null) }, Routes.TIMETABLE_GRID)
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Text("Overview", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatsCard("Departments", state.departmentCount.toString(),
                        icon = { Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary) },
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    StatsCard("Faculty", state.facultyCount.toString(),
                        icon = { Icon(Icons.Default.People, null, tint = MaterialTheme.colorScheme.secondary) },
                        color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatsCard("Subjects", state.subjectCount.toString(),
                        icon = { Icon(Icons.Default.Book, null, tint = MaterialTheme.colorScheme.tertiary) },
                        color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                    StatsCard("Rooms", state.roomCount.toString(),
                        icon = { Icon(Icons.Default.MeetingRoom, null, tint = MaterialTheme.colorScheme.primary) },
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatsCard("Timetable", state.timetableCount.toString(),
                        icon = { Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.error) },
                        color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    Card(
                        onClick = { navController.navigate(Routes.TIMETABLE_GENERATOR) },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.AutoAwesome, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.height(4.dp))
                            Text("Generate", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickActions.take(3).forEach { QuickActionCard(it, navController, Modifier.weight(1f)) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickActions.drop(3).forEach { QuickActionCard(it, navController, Modifier.weight(1f)) }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        AppFAB(onClick = { navController.navigate(Routes.TIMETABLE_GENERATOR) })
    }
}

@Composable
private fun QuickActionCard(action: DashboardQuickAction, navController: NavController, modifier: Modifier = Modifier) {
    AppCard(onClick = { navController.navigate(action.route) }, modifier = modifier.height(80.dp)) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            action.icon()
            Spacer(Modifier.height(6.dp))
            Text(action.title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
