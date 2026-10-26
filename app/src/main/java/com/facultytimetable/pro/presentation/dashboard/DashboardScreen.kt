package com.facultytimetable.pro.presentation.dashboard

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.GradientBanner
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.SectionHeader
import com.facultytimetable.pro.presentation.common.components.StatsCard
import com.facultytimetable.pro.presentation.common.components.StatusRow
import com.facultytimetable.pro.presentation.common.subjectColor
import com.facultytimetable.pro.presentation.navigation.Routes
import java.util.Calendar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Dashboard",
            subtitle = "Faculty TimeTable Pro",
            actions = {
                IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        )

        if (state.isLoading) {
            LoadingState()
        } else {
            val greeting = remember {
                when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                    in 0..11 -> "Good morning"
                    in 12..16 -> "Good afternoon"
                    else -> "Good evening"
                }
            }
            val academicLabel = listOfNotNull(state.academicYear, state.semesterName)
                .joinToString(" · ")
                .ifBlank { "Academic session not configured" }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    GradientBanner(
                        title = greeting,
                        subtitle = academicLabel,
                        icon = Icons.Default.TrendingUp,
                        actionLabel = if (state.hasTimetable) "Open generator" else "Generate timetable",
                        onAction = { navController.navigate(Routes.TIMETABLE_GENERATOR) }
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${state.timetableCount} of ${state.requiredPeriods} periods scheduled",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    if (state.unscheduledPeriods == 0 && state.requiredPeriods > 0)
                                        "The weekly timetable is complete"
                                    else "${state.unscheduledPeriods} period(s) still need a slot",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                            Text(
                                "${(state.coveragePercent * 100).roundToInt()}%",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { state.coveragePercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.24f)
                        )
                    }
                }

                item {
                    AppCard {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            SectionHeader("Timetable health")
                            StatusRow(
                                title = "Timetable generated",
                                subtitle = "${state.timetableCount} periods saved",
                                isOk = state.timetableCount > 0
                            )
                            StatusRow(
                                title = "All required periods scheduled",
                                subtitle = if (state.unscheduledPeriods == 0 && state.requiredPeriods > 0)
                                    "No missing periods" else "${state.unscheduledPeriods} missing",
                                isOk = state.requiredPeriods > 0 && state.unscheduledPeriods == 0
                            )
                            StatusRow(
                                title = "Teaching periods configured",
                                subtitle = "${state.regularPeriodCount} regular periods per week",
                                isOk = state.regularPeriodCount > 0
                            )
                            StatusRow(
                                title = "Classrooms & labs",
                                subtitle = "${state.classroomCount} classrooms · ${state.labRoomCount} labs",
                                isOk = state.classroomCount > 0
                            )
                            if (state.dataIssues.isNotEmpty()) {
                                Text(
                                    state.dataIssues.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatsCard(
                            title = "Sections",
                            value = state.sectionCount.toString(),
                            icon = { Icon(Icons.Default.Group, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Routes.SECTION_LIST) }
                        )
                        StatsCard(
                            title = "Faculty",
                            value = state.facultyCount.toString(),
                            icon = { Icon(Icons.Default.People, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp)) },
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Routes.FACULTY_LIST) }
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatsCard(
                            title = "Subjects",
                            value = state.subjectCount.toString(),
                            icon = { Icon(Icons.Default.Book, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp)) },
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Routes.SUBJECT_LIST) }
                        )
                        StatsCard(
                            title = "Rooms",
                            value = state.roomCount.toString(),
                            icon = { Icon(Icons.Default.MeetingRoom, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Routes.ROOM_LIST) }
                        )
                    }
                }

                item {
                    SectionHeader("Today · ${state.todayLabel}")
                }

                if (!state.todayIsTeachingDay) {
                    item {
                        InfoCard(
                            title = "Weekend",
                            message = "There are no scheduled classes today. Enjoy the break."
                        )
                    }
                } else if (state.todayClasses.isEmpty()) {
                    item {
                        InfoCard(
                            title = "No classes today",
                            message = if (state.hasTimetable)
                                "This day has no scheduled periods in the current timetable."
                            else "Generate a timetable to see today's classes here."
                        )
                    }
                } else {
                    items(state.todayClasses, key = { it.entry.id }) { item ->
                        TodayClassCard(item)
                    }
                }

                item {
                    SectionHeader("Quick actions")
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction("Generator", Icons.Default.AutoAwesome, Modifier.weight(1f)) {
                            navController.navigate(Routes.TIMETABLE_GENERATOR)
                        }
                        QuickAction("Reports", Icons.Default.TrendingUp, Modifier.weight(1f)) {
                            navController.navigate(Routes.REPORTS)
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction("Calendar", Icons.Default.CalendarMonth, Modifier.weight(1f)) {
                            navController.navigate(Routes.ACADEMIC_CALENDAR)
                        }
                        QuickAction("Backup", Icons.Default.Backup, Modifier.weight(1f)) {
                            navController.navigate(Routes.BACKUP)
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction("Sample data", Icons.Default.AutoAwesome, Modifier.weight(1f)) {
                            navController.navigate(Routes.SAMPLE_DATA)
                        }
                        QuickAction("Search", Icons.Default.Search, Modifier.weight(1f)) {
                            navController.navigate(Routes.SEARCH)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, message: String) {
    AppCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TodayClassCard(item: DashboardClass) {
    val color = subjectColor(item.subjectType)
    AppCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(58.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    item.startTime,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    item.endTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${item.subjectCode} · ${item.sectionName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${item.roomName} · ${item.facultyName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "P${item.periodNumber}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}
