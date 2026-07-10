package com.facultytimetable.pro.presentation.assignment

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.FacultyAssignmentEntity
import com.facultytimetable.pro.presentation.common.components.ActionButton
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.DropdownSelector
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyAssignmentScreen(
    navController: NavController,
    viewModel: FacultyAssignmentViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) viewModel.clearSuccess() }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Faculty Assignment",
            onBackClick = { navController.popBackStack() }
        )

        if (state.isLoading) {
            LoadingState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                SectionHeader("Create Assignment")

                DropdownSelector(
                    label = "Department *",
                    selectedItem = state.departments.find { it.id == state.selectedDepartmentId },
                    items = state.departments,
                    itemLabel = { it.name },
                    onItemSelected = { viewModel.onDepartmentSelected(it.id) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (state.selectedDepartmentId != null) {
                    DropdownSelector(
                        label = "Semester *",
                        selectedItem = state.semesters.find { it.id == state.selectedSemesterId },
                        items = state.semesters,
                        itemLabel = { it.name },
                        onItemSelected = { viewModel.onSemesterSelected(it.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (state.selectedDepartmentId != null && state.selectedSemesterId != null) {
                    DropdownSelector(
                        label = "Subject *",
                        selectedItem = state.subjects.find { it.id == state.selectedSubjectId },
                        items = state.subjects,
                        itemLabel = { "${it.name} (${it.code})" },
                        onItemSelected = { viewModel.onSubjectSelected(it.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (state.selectedDepartmentId != null) {
                    DropdownSelector(
                        label = "Faculty *",
                        selectedItem = state.faculty.find { it.id == state.selectedFacultyId },
                        items = state.faculty,
                        itemLabel = { it.name },
                        onItemSelected = { viewModel.onFacultySelected(it.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = state.hoursPerWeek,
                    onValueChange = viewModel::onHoursChange,
                    label = { Text("Hours Per Week *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                state.workload?.let { wl ->
                    Spacer(modifier = Modifier.height(8.dp))
                    val isOverloaded = wl.totalHours > wl.maxHours
                    val color = if (isOverloaded) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (isOverloaded) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Workload: ${wl.totalHours} / ${wl.maxHours} hrs",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = color
                        )
                    }
                }

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                ActionButton(
                    text = "Save Assignment",
                    onClick = viewModel::save,
                    enabled = !state.isSaving
                )

                if (state.existingAssignments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader("Existing Assignments")
                    Spacer(modifier = Modifier.height(8.dp))

                    state.existingAssignments.forEach { item ->
                        AssignmentCard(
                            assignment = item,
                            onDelete = { viewModel.deleteAssignment(item.assignment) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AssignmentCard(
    assignment: AssignmentWithDetails,
    onDelete: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    assignment.subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    assignment.facultyName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${assignment.assignment.hoursPerWeek} hrs/week",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
