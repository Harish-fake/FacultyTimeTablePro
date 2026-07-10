package com.facultytimetable.pro.presentation.academicyear

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.dao.AcademicYearDao
import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
import com.facultytimetable.pro.presentation.common.components.ActionButton
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AcademicYearFormState(
    val name: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isCurrent: Boolean = false,
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class AcademicYearFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val academicYearDao: AcademicYearDao
) : ViewModel() {

    private val yearId: Long? = savedStateHandle.get<Long>("yearId")?.takeIf { it > 0 }
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val _state = MutableStateFlow(AcademicYearFormState())
    val state: StateFlow<AcademicYearFormState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            if (yearId != null) {
                val year = academicYearDao.getAcademicYearById(yearId)
                if (year != null) {
                    _state.value = _state.value.copy(
                        name = year.name, startDate = year.startDate, endDate = year.endDate,
                        isCurrent = year.isCurrent, isEditing = true, isLoading = false
                    )
                    return@launch
                }
            }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onStartDateSelected(d: Long) { _state.value = _state.value.copy(startDate = d) }
    fun onEndDateSelected(d: Long) { _state.value = _state.value.copy(endDate = d) }
    fun onIsCurrentChange(v: Boolean) { _state.value = _state.value.copy(isCurrent = v) }

    fun formatDate(epoch: Long): String = dateFormat.format(Date(epoch))

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Name is required"); return }
        if (s.startDate == null) { _state.value = s.copy(error = "Start date is required"); return }
        if (s.endDate == null) { _state.value = s.copy(error = "End date is required"); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                if (s.isCurrent) {
                    academicYearDao.clearCurrentYear()
                }
                val entity = AcademicYearEntity(
                    id = yearId ?: 0, name = s.name,
                    startDate = s.startDate, endDate = s.endDate,
                    isCurrent = s.isCurrent
                )
                if (yearId != null) academicYearDao.update(entity)
                else academicYearDao.insert(entity)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicYearFormScreen(
    yearId: Long?,
    navController: NavController,
    viewModel: AcademicYearFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) navController.popBackStack() }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = if (yearId != null) "Edit Academic Year" else "Add Academic Year",
            onBackClick = { navController.popBackStack() }
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.name, onValueChange = viewModel::onNameChange,
                label = { Text("Year Name *") }, placeholder = { Text("e.g. 2024-2025") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showStartPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Text(if (state.startDate != null) "Start: ${viewModel.formatDate(state.startDate!!)}" else "Select Start Date")
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showEndPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Text(if (state.endDate != null) "End: ${viewModel.formatDate(state.endDate!!)}" else "Select End Date")
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Set as Current Year", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.isCurrent, onCheckedChange = viewModel::onIsCurrentChange)
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(24.dp))
            ActionButton(
                text = if (yearId != null) "Update Year" else "Add Year",
                onClick = viewModel::save, enabled = !state.isSaving
            )
        }
    }

    if (showStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.startDate)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onStartDateSelected(it) }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.endDate)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onEndDateSelected(it) }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
