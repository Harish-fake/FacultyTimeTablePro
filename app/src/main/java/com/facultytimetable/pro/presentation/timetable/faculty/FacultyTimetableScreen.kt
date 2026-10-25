package com.facultytimetable.pro.presentation.timetable.faculty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.EmptyState
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.timetable.common.DaySelector
import com.facultytimetable.pro.presentation.timetable.common.PeriodRow
import com.facultytimetable.pro.presentation.timetable.common.dayShortNames
import java.util.Calendar

@Composable
fun FacultyTimetableScreen(
    navController: NavController,
    viewModel: FacultyTimetableViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedDay by rememberSaveable { mutableStateOf(todayIsoDay()) }

    LaunchedEffect(state.days) {
        if (state.days.isNotEmpty() && selectedDay !in state.days) selectedDay = state.days.first()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = state.faculty?.name ?: "Faculty timetable",
            subtitle = state.faculty?.designation,
            onBackClick = { navController.popBackStack() }
        )

        if (state.isLoading) {
            LoadingState()
        } else if (state.faculty == null) {
            EmptyState(title = "Faculty not found", message = "This faculty member may have been deleted.")
        } else {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "${state.weeklyPeriods}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("periods / week", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "${state.faculty?.maxWeeklyHours ?: 0}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("max hours", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                DaySelector(
                    selectedDay = selectedDay,
                    onDaySelected = { selectedDay = it },
                    days = state.days.ifEmpty { listOf(1, 2, 3, 4, 5) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val slots = state.timeSlots
                .filter { it.dayOfWeek == selectedDay }
                .sortedBy { it.periodNumber }

            if (slots.isEmpty()) {
                EmptyState(
                    title = "No periods",
                    message = "No time slots are configured for ${dayShortNames.getOrElse(selectedDay - 1) { "this day" }}."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)
                ) {
                    items(slots, key = { it.id }) { slot ->
                        val entry = state.entries.firstOrNull { it.timeSlotId == slot.id }
                        PeriodRow(
                            slot = slot,
                            entry = entry,
                            subject = entry?.let { state.subjects[it.subjectId] },
                            faculty = null,
                            room = entry?.let { state.rooms[it.roomId] },
                            secondaryLabel = entry?.let { state.sections[it.sectionId]?.name }
                        )
                    }
                }
            }
        }
    }
}

private fun todayIsoDay(): Int {
    val value = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return if (value == Calendar.SUNDAY) 7 else value - 1
}
