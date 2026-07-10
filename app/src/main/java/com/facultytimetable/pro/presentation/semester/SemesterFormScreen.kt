package com.facultytimetable.pro.presentation.semester

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import com.facultytimetable.pro.domain.repository.AcademicYearRepository
import com.facultytimetable.pro.domain.repository.SemesterRepository
import com.facultytimetable.pro.presentation.common.components.ActionButton
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.DropdownSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SemesterFormState(
    val name: String = "",
    val semesterNumber: String = "",
    val selectedAcademicYear: AcademicYearEntity? = null,
    val academicYears: List<AcademicYearEntity> = emptyList(),
    val isActive: Boolean = true,
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SemesterFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val semesterRepository: SemesterRepository,
    private val academicYearRepository: AcademicYearRepository
) : ViewModel() {

    private val semesterId: Long? = savedStateHandle.get<Long>("semesterId")?.takeIf { it > 0 }
    private val _state = MutableStateFlow(SemesterFormState())
    val state: StateFlow<SemesterFormState> = _state

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val years = academicYearRepository.getAllAcademicYears().first()

            if (semesterId != null) {
                val semester = semesterRepository.getById(semesterId)
                if (semester != null) {
                    val year = years.find { it.id == semester.academicYearId }
                    _state.value = _state.value.copy(
                        name = semester.name,
                        semesterNumber = semester.semesterNumber.toString(),
                        selectedAcademicYear = year,
                        isActive = semester.isActive,
                        academicYears = years,
                        isEditing = true,
                        isLoading = false
                    )
                    return@launch
                }
            }
            _state.value = _state.value.copy(
                academicYears = years,
                isLoading = false
            )
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value, error = null) }
    fun onSemesterNumberChange(value: String) {
        if (value.all { it.isDigit() } && value.length <= 2) {
            _state.value = _state.value.copy(semesterNumber = value, error = null)
        }
    }
    fun onAcademicYearChange(year: AcademicYearEntity) { _state.value = _state.value.copy(selectedAcademicYear = year) }
    fun onIsActiveChange(value: Boolean) { _state.value = _state.value.copy(isActive = value) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Name is required"); return }
        if (s.semesterNumber.isBlank()) { _state.value = s.copy(error = "Semester number is required"); return }
        if (s.selectedAcademicYear == null) { _state.value = s.copy(error = "Academic year is required"); return }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val entity = SemesterEntity(
                    id = semesterId ?: 0,
                    name = s.name,
                    academicYearId = s.selectedAcademicYear.id,
                    semesterNumber = s.semesterNumber.toInt(),
                    isActive = s.isActive,
                    createdAt = if (semesterId != null) {
                        semesterRepository.getById(semesterId)?.createdAt ?: System.currentTimeMillis()
                    } else System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                if (semesterId != null) semesterRepository.update(entity)
                else semesterRepository.insert(entity)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterFormScreen(
    navController: NavController,
    viewModel: SemesterFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) navController.popBackStack() }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = if (state.isEditing) "Edit Semester" else "Add Semester",
            onBackClick = { navController.popBackStack() }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Semester Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.semesterNumber,
                onValueChange = viewModel::onSemesterNumberChange,
                label = { Text("Semester Number *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            DropdownSelector(
                label = "Academic Year *",
                selectedItem = state.selectedAcademicYear,
                items = state.academicYears,
                itemLabel = { it.name },
                onItemSelected = viewModel::onAcademicYearChange,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Semester",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = state.isActive, onCheckedChange = viewModel::onIsActiveChange)
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            ActionButton(
                text = if (state.isEditing) "Update Semester" else "Add Semester",
                onClick = viewModel::save,
                enabled = !state.isSaving
            )
        }
    }
}
