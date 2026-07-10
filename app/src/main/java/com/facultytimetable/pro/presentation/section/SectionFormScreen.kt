package com.facultytimetable.pro.presentation.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.ActionButton
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.DropdownSelector
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionFormScreen(
    sectionId: Long?,
    navController: NavController,
    viewModel: SectionFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) navController.popBackStack() }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = if (sectionId != null) "Edit Section" else "Add Section",
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
                SectionHeader("Basic Information")

                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Section Name *") },
                    placeholder = { Text("e.g. A, B, CSE-A") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.strength,
                    onValueChange = viewModel::onStrengthChange,
                    label = { Text("Student Strength") },
                    placeholder = { Text("60") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("Associations")

                DropdownSelector(
                    label = "Department",
                    selectedItem = state.departments.find { it.id == state.departmentId },
                    items = state.departments,
                    itemLabel = { it.name },
                    onItemSelected = { viewModel.onDepartmentSelected(it.id) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                DropdownSelector(
                    label = "Academic Year",
                    selectedItem = state.academicYears.find { it.id == state.academicYearId },
                    items = state.academicYears,
                    itemLabel = { it.name },
                    onItemSelected = viewModel::onAcademicYearSelected,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                DropdownSelector(
                    label = "Semester *",
                    selectedItem = state.semesters.find { it.id == state.semesterId },
                    items = state.semesters,
                    itemLabel = { "${it.name} (Sem ${it.semesterNumber})" },
                    onItemSelected = viewModel::onSemesterSelected,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                DropdownSelector(
                    label = "Room",
                    selectedItem = state.rooms.find { it.id == state.roomId },
                    items = state.rooms,
                    itemLabel = { it.name },
                    onItemSelected = viewModel::onRoomSelected,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.classAdvisor,
                    onValueChange = viewModel::onClassAdvisorChange,
                    label = { Text("Class Advisor") },
                    placeholder = { Text("Faculty name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(24.dp))
                ActionButton(
                    text = if (sectionId != null) "Update Section" else "Add Section",
                    onClick = viewModel::save,
                    enabled = !state.isSaving
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
