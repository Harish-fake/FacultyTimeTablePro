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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppFAB
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.StatsCard
import com.facultytimetable.pro.presentation.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.firstLaunch) {
        if (state.firstLaunch && !state.isLoading) {
            navController.navigate(Routes.ONBOARDING)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Faculty TimeTable Pro")

        if (state.isLoading) {
            LoadingState()
        } else if (state.departmentCount == 0) {
            EmptyDashboardContent(navController = navController)
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatsCard("Dept", "${state.departmentCount}", icon = { Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary) }, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f), onClick = { navController.navigate(Routes.DEPARTMENT_LIST) })
                    StatsCard("Faculty", "${state.facultyCount}", icon = { Icon(Icons.Default.People, null, tint = MaterialTheme.colorScheme.secondary) }, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f), onClick = { navController.navigate(Routes.FACULTY_LIST) })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatsCard("Subjects", "${state.subjectCount}", icon = { Icon(Icons.Default.Book, null, tint = MaterialTheme.colorScheme.tertiary) }, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f), onClick = { navController.navigate(Routes.SUBJECT_LIST) })
                    StatsCard("Rooms", "${state.roomCount}", icon = { Icon(Icons.Default.MeetingRoom, null, tint = MaterialTheme.colorScheme.primary) }, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f), onClick = { navController.navigate(Routes.ROOM_LIST) })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatsCard("Labs", "${state.labCount}", icon = { Icon(Icons.Default.Science, null, tint = MaterialTheme.colorScheme.secondary) }, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f), onClick = { navController.navigate(Routes.LAB_LIST) })
                    StatsCard("Sections", "${state.sectionCount}", icon = { Icon(Icons.Default.ViewModule, null, tint = MaterialTheme.colorScheme.tertiary) }, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f), onClick = { navController.navigate(Routes.SECTION_LIST) })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatsCard("Ac.Years", "${state.academicYearCount}", icon = { Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary) }, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f), onClick = { navController.navigate(Routes.ACADEMIC_YEAR_LIST) })
                    StatsCard("Assign.", "${state.assignmentCount}", icon = { Icon(Icons.Default.Assignment, null, tint = MaterialTheme.colorScheme.secondary) }, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f), onClick = { navController.navigate(Routes.FACULTY_ASSIGNMENT) })
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Setup Progress", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Completion", style = MaterialTheme.typography.bodyMedium)
                            Text("${(state.completionPercent * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { state.completionPercent }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(onClick = { navController.navigate(Routes.SETUP_WIZARD) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.SmartToy, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Setup Wizard")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionChip("Search", Icons.Default.Search, { navController.navigate(Routes.SEARCH) }, Modifier.weight(1f))
                    QuickActionChip("Generate", Icons.Default.AutoAwesome, { navController.navigate(Routes.TIMETABLE_GENERATOR) }, Modifier.weight(1f))
                    QuickActionChip("Assign", Icons.Default.Assignment, { navController.navigate(Routes.FACULTY_ASSIGNMENT) }, Modifier.weight(1f))
                }

                if (state.recentActivities.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Recent Activity", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    state.recentActivities.take(5).forEach { log ->
                        RecentActivityItem(log)
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        AppFAB(onClick = { navController.navigate(Routes.SEARCH) })
    }
}

@Composable
private fun QuickActionChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RecentActivityItem(log: com.facultytimetable.pro.data.local.db.entity.AuditLogEntity) {
    val dateFormat = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(log.action, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${log.entityType} | ${dateFormat.format(Date(log.timestamp))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyDashboardContent(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Get Started", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Set up your timetable system by completing the setup wizard.\nFollow the guided steps to add your college data.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        FilledTonalButton(onClick = { navController.navigate(Routes.SETUP_WIZARD) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Text("Open Setup Wizard", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(onClick = { navController.navigate(Routes.DEPARTMENT_FORM) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Text("Add First Department", style = MaterialTheme.typography.titleSmall)
        }
    }
}
