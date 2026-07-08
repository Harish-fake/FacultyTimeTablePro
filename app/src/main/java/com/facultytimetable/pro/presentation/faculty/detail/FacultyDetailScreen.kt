package com.facultytimetable.pro.presentation.faculty.detail

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton
import com.facultytimetable.pro.presentation.common.components.InfoRow
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyDetailScreen(
    facultyId: Long,
    navController: NavController,
    viewModel: FacultyDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = state.faculty?.name ?: "Faculty Details",
            onBackClick = { navController.popBackStack() }
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            Column {
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
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("Contact Information")
                    AppCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow("Email", faculty.email)
                            InfoRow("Phone", faculty.phone)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SectionHeader("Professional Details")
                    AppCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow("Designation", faculty.designation)
                            InfoRow("Qualification", faculty.qualification)
                            InfoRow("Experience", "${faculty.experience} years")
                            InfoRow("Max Weekly Hours", "${faculty.maxWeeklyHours} hours")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("Actions")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        com.facultytimetable.pro.presentation.common.components.ActionChip(
                            text = "Edit",
                            onClick = { navController.navigate(com.facultytimetable.pro.presentation.navigation.Routes.facultyForm(facultyId)) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        com.facultytimetable.pro.presentation.common.components.ActionChip(
                            text = "View Timetable",
                            onClick = { navController.navigate(com.facultytimetable.pro.presentation.navigation.Routes.timetableFaculty(facultyId)) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        com.facultytimetable.pro.presentation.common.components.ActionChip(
                            text = "Leave Records",
                            onClick = { navController.navigate(com.facultytimetable.pro.presentation.navigation.Routes.facultyLeave(facultyId)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
