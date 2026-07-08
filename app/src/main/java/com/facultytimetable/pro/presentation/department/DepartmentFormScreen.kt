package com.facultytimetable.pro.presentation.department

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.presentation.common.components.ActionButton
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DepartmentFormState(
    val name: String = "",
    val code: String = "",
    val headName: String = "",
    val description: String = "",
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class DepartmentFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val departmentRepository: DepartmentRepository
) : ViewModel() {

    private val departmentId: Long? = savedStateHandle.get<Long>("departmentId")?.takeIf { it > 0 }
    private val _state = MutableStateFlow(DepartmentFormState())
    val state: StateFlow<DepartmentFormState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            if (departmentId != null) {
                val dept = departmentRepository.getDepartmentById(departmentId)
                if (dept != null) {
                    _state.value = _state.value.copy(
                        name = dept.name, code = dept.code,
                        headName = dept.headName, description = dept.description,
                        isEditing = true, isLoading = false
                    )
                    return@launch
                }
            }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onCodeChange(v: String) { _state.value = _state.value.copy(code = v) }
    fun onHeadNameChange(v: String) { _state.value = _state.value.copy(headName = v) }
    fun onDescriptionChange(v: String) { _state.value = _state.value.copy(description = v) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Name is required"); return }
        if (s.code.isBlank()) { _state.value = s.copy(error = "Code is required"); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val entity = DepartmentEntity(
                    id = departmentId ?: 0, name = s.name, code = s.code.uppercase(),
                    headName = s.headName, description = s.description
                )
                if (departmentId != null) departmentRepository.update(entity)
                else departmentRepository.insert(entity)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentFormScreen(
    departmentId: Long?,
    navController: NavController,
    viewModel: DepartmentFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) navController.popBackStack() }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = if (departmentId != null) "Edit Department" else "Add Department",
            onBackClick = { navController.popBackStack() }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.name, onValueChange = viewModel::onNameChange,
                label = { Text("Department Name *") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.code, onValueChange = viewModel::onCodeChange,
                label = { Text("Code *") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.headName, onValueChange = viewModel::onHeadNameChange,
                label = { Text("Head Name") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.description, onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description") }, modifier = Modifier.fillMaxWidth(),
                minLines = 3, shape = MaterialTheme.shapes.medium
            )
            if (state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(24.dp))
            ActionButton(
                text = if (departmentId != null) "Update Department" else "Add Department",
                onClick = viewModel::save, enabled = !state.isSaving
            )
        }
    }
}
