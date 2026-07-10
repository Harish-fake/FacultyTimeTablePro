package com.facultytimetable.pro.presentation.faculty.form

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
fun FacultyFormScreen(
    facultyId: Long?,
    navController: NavController,
    viewModel: FacultyFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) navController.popBackStack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = if (facultyId != null) "Edit Faculty" else "Add Faculty",
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
                PhotoSection(
                    initial = state.name.firstOrNull()?.uppercase() ?: "?",
                    onClick = { }
                )

                Spacer(modifier = Modifier.height(20.dp))

                SectionHeader("Basic Information")
                OutlinedTextField(value = state.name, onValueChange = viewModel::onNameChange, label = { Text("Full Name *") }, placeholder = { Text("Enter faculty name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = state.employeeId, onValueChange = viewModel::onEmployeeIdChange, label = { Text("Employee ID") }, placeholder = { Text("EMP-001") }, modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    OutlinedTextField(value = state.facultyCode, onValueChange = viewModel::onFacultyCodeChange, label = { Text("Faculty Code") }, placeholder = { Text("FAC-001") }, modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(value = state.email, onValueChange = viewModel::onEmailChange, label = { Text("Email *") }, placeholder = { Text("email@example.com") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(value = state.phone, onValueChange = viewModel::onPhoneChange, label = { Text("Phone") }, placeholder = { Text("+1 234 567 8900") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next))
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownSelector(label = "Gender", selectedItem = state.gender.ifBlank { null }, items = GENDERS, itemLabel = { it }, onItemSelected = viewModel::onGenderChange, modifier = Modifier.weight(1f))
                    DropdownSelector(label = "Designation *", selectedItem = state.designation.ifBlank { null }, items = DESIGNATIONS, itemLabel = { it }, onItemSelected = viewModel::onDesignationChange, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))

                DropdownSelector(label = "Department *", selectedItem = state.departments.find { it.id == state.departmentId }, items = state.departments, itemLabel = { it.name }, onItemSelected = { viewModel.onDepartmentSelected(it.id) }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = state.qualification, onValueChange = viewModel::onQualificationChange, label = { Text("Qualification") }, placeholder = { Text("Ph.D., M.Tech") }, modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    DropdownSelector(label = "Status", selectedItem = state.status, items = STATUS_OPTIONS, itemLabel = { it }, onItemSelected = viewModel::onStatusChange, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(value = state.officeRoom, onValueChange = viewModel::onOfficeRoomChange, label = { Text("Office Room") }, placeholder = { Text("Room 201, Block A") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = state.experience, onValueChange = viewModel::onExperienceChange, label = { Text("Experience (years)") }, placeholder = { Text("5") }, modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next))
                    OutlinedTextField(value = state.maxWeeklyHours, onValueChange = viewModel::onMaxHoursChange, label = { Text("Max Weekly Hours") }, placeholder = { Text("24") }, modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next))
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                SectionHeader("Preferences & Availability")
                OutlinedTextField(value = state.preferredDays, onValueChange = viewModel::onPreferredDaysChange, label = { Text("Preferred Days") }, placeholder = { Text("Mon, Tue, Wed, Thu, Fri") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(value = state.unavailableDays, onValueChange = viewModel::onUnavailableDaysChange, label = { Text("Unavailable Days") }, placeholder = { Text("Sat, Sun") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(value = state.preferredTimeSlots, onValueChange = viewModel::onPreferredTimeSlotsChange, label = { Text("Preferred Time Slots") }, placeholder = { Text("Morning, Afternoon") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                SectionHeader("Settings")
                SettingsToggle(label = "Lab Eligible", checked = state.labEligible, onCheckedChange = viewModel::onLabEligibleChange, description = "Can be assigned to lab sessions")
                SettingsToggle(label = "Active", checked = state.isActive, onCheckedChange = viewModel::onIsActiveChange, description = "Inactive faculty won't appear in selections")

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                SectionHeader("Additional Information")
                OutlinedTextField(value = state.notes, onValueChange = viewModel::onNotesChange, label = { Text("Notes") }, placeholder = { Text("Additional information...") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), colors = OutlinedTextFieldDefaults.colors())

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(24.dp))
                ActionButton(text = if (facultyId != null) "Update Faculty" else "Add Faculty", onClick = viewModel::save, enabled = !state.isSaving)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PhotoSection(initial: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(88.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = initial, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.height(2.dp))
                Icon(Icons.Default.Edit, contentDescription = "Add Photo", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, description: String? = null, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
