package com.facultytimetable.pro.presentation.more

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.GradientBanner
import com.facultytimetable.pro.presentation.navigation.Routes

@Composable
fun MoreScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "More", subtitle = "Manage your institution")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            GradientBanner(
                title = "Workspace",
                subtitle = "Everything you need to run the timetable",
                icon = Icons.Default.Tune,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))
            GroupCard(
                title = "Timetable tools",
                items = listOf(
                    MoreItem("Auto generator", "Build and validate a new timetable", Icons.Default.AutoAwesome) {
                        navController.navigate(Routes.TIMETABLE_GENERATOR)
                    },
                    MoreItem("Academic calendar", "Years, semesters and holidays", Icons.Default.CalendarMonth) {
                        navController.navigate(Routes.ACADEMIC_CALENDAR)
                    },
                    MoreItem("Time slots", "Periods, lunch and breaks", Icons.Default.Schedule) {
                        navController.navigate(Routes.TIME_SLOT_CONFIG)
                    },
                    MoreItem("Search", "Find faculty, subjects and rooms", Icons.Default.Search) {
                        navController.navigate(Routes.SEARCH)
                    }
                )
            )

            Spacer(modifier = Modifier.height(14.dp))
            GroupCard(
                title = "Institution data",
                items = listOf(
                    MoreItem("Departments", "Programmes and heads", Icons.Default.School) {
                        navController.navigate(Routes.DEPARTMENT_LIST)
                    },
                    MoreItem("Faculty", "Staff, workload and leave", Icons.Default.People) {
                        navController.navigate(Routes.FACULTY_LIST)
                    },
                    MoreItem("Subjects", "Theory, labs and projects", Icons.Default.Book) {
                        navController.navigate(Routes.SUBJECT_LIST)
                    },
                    MoreItem("Rooms & labs", "Capacity and facilities", Icons.Default.MeetingRoom) {
                        navController.navigate(Routes.ROOM_LIST)
                    },
                    MoreItem("Sections", "Classes and strength", Icons.Default.Group) {
                        navController.navigate(Routes.SECTION_LIST)
                    }
                )
            )

            Spacer(modifier = Modifier.height(14.dp))
            GroupCard(
                title = "Insights & data",
                items = listOf(
                    MoreItem("Reports", "Workload, room use and coverage", Icons.Default.Analytics) {
                        navController.navigate(Routes.REPORTS)
                    },
                    MoreItem("Backup & restore", "Create or import a snapshot", Icons.Default.Backup) {
                        navController.navigate(Routes.BACKUP)
                    },
                    MoreItem("Settings", "Theme, PIN and demo data", Icons.Default.Settings) {
                        navController.navigate(Routes.SETTINGS)
                    }
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Faculty TimeTable Pro · v1.1.0",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                "Offline-first timetable management for colleges.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

private data class MoreItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun GroupCard(title: String, items: List<MoreItem>) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp)
    )
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (index != items.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(start = 50.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
