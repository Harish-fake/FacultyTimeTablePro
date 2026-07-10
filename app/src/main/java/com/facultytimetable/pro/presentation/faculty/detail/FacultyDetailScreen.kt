package com.facultytimetable.pro.presentation.faculty.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.InfoRow
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.SectionHeader
import com.facultytimetable.pro.presentation.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyDetailScreen(
    facultyId: Long,
    navController: NavController,
    viewModel: FacultyDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = state.faculty?.name ?: "Faculty Details",
            onBackClick = { navController.popBackStack() },
            actions = {
                IconButton(onClick = { navController.navigate(Routes.facultyForm(facultyId)) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        )

        if (state.isLoading) {
            LoadingState()
        } else {
            state.faculty?.let { faculty ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    AppCard {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = faculty.name.first().uppercase().toString(),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = faculty.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = faculty.designation,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (faculty.employeeId.isNotBlank()) {
                                Text(
                                    text = "ID: ${faculty.employeeId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("Contact Information")
                    AppCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow("Email", faculty.email)
                            InfoRow("Phone", faculty.phone.ifBlank { "—" })
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SectionHeader("Professional Details")
                    AppCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow("Designation", faculty.designation)
                            InfoRow("Qualification", faculty.qualification.ifBlank { "—" })
                            InfoRow("Experience", "${faculty.experience} years")
                            InfoRow("Max Weekly Hours", "${faculty.maxWeeklyHours} hours")
                            InfoRow("Faculty Code", faculty.facultyCode.ifBlank { "—" })
                            InfoRow("Gender", faculty.gender.ifBlank { "—" })
                            InfoRow("Office Room", faculty.officeRoom.ifBlank { "—" })
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SectionHeader("Preferences & Availability")
                    AppCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow("Preferred Days", faculty.preferredDays.ifBlank { "—" })
                            InfoRow("Unavailable Days", faculty.unavailableDays.ifBlank { "—" })
                            InfoRow("Preferred Time Slots", faculty.preferredTimeSlots.ifBlank { "—" })
                            InfoRow("Lab Eligible", if (faculty.labEligible) "Yes" else "No")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SectionHeader("Settings")
                    AppCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow("Status", faculty.status)
                            InfoRow("Active", if (faculty.isActive) "Yes" else "No")
                            if (faculty.notes.isNotBlank()) InfoRow("Notes", faculty.notes)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        com.facultytimetable.pro.presentation.common.components.ActionChip(
                            text = "Edit",
                            onClick = { navController.navigate(Routes.facultyForm(facultyId)) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        com.facultytimetable.pro.presentation.common.components.ActionChip(
                            text = "Timetable",
                            onClick = { navController.navigate(Routes.timetableFaculty(facultyId)) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        com.facultytimetable.pro.presentation.common.components.ActionChip(
                            text = "Leave",
                            onClick = { navController.navigate(Routes.facultyLeave(facultyId)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete Faculty",
            message = "Are you sure you want to delete ${state.faculty?.name ?: "this faculty member"}?",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteFaculty()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
            isDestructive = true
        )
    }
}
