package com.facultytimetable.pro.presentation.department

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.facultytimetable.pro.presentation.common.components.AppCard
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.LoadingState
import com.facultytimetable.pro.presentation.common.components.SectionHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val saveSuccess: Boolean = false,
    val nameError: String? = null,
    val codeError: String? = null
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

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, nameError = null, error = null) }
    fun onCodeChange(v: String) { _state.value = _state.value.copy(code = v.uppercase(), codeError = null, error = null) }
    fun onHeadNameChange(v: String) { _state.value = _state.value.copy(headName = v) }
    fun onDescriptionChange(v: String) {
        if (v.length <= 500) _state.value = _state.value.copy(description = v)
    }

    fun save() {
        val s = _state.value
        var hasError = false
        if (s.name.isBlank()) {
            _state.value = _state.value.copy(nameError = "Department name is required")
            hasError = true
        }
        if (s.code.isBlank()) {
            _state.value = _state.value.copy(codeError = "Department code is required")
            hasError = true
        }
        if (hasError) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val entity = DepartmentEntity(
                    id = departmentId ?: 0, name = s.name, code = s.code,
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

        if (state.isLoading) {
            LoadingState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(title = "Basic Information", modifier = Modifier.padding(0.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = viewModel::onNameChange,
                            label = { Text("Department Name *") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            isError = state.nameError != null,
                            supportingText = {
                                AnimatedVisibility(
                                    visible = state.nameError != null,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Text(
                                        state.nameError ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.code,
                            onValueChange = viewModel::onCodeChange,
                            label = { Text("Code *") },
                            leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            isError = state.codeError != null,
                            supportingText = {
                                AnimatedVisibility(
                                    visible = state.codeError != null,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Text(
                                        state.codeError ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(title = "Contact Details", modifier = Modifier.padding(0.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.headName,
                            onValueChange = viewModel::onHeadNameChange,
                            label = { Text("Head of Department") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            placeholder = { Text("e.g. Dr. John Smith") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(title = "Description", modifier = Modifier.padding(0.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.description,
                            onValueChange = viewModel::onDescriptionChange,
                            label = { Text("Description") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 6,
                            shape = MaterialTheme.shapes.medium,
                            placeholder = { Text("Brief description of the department...") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${state.description.length}/500",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.description.length > 450)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = state.error != null,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    state.error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                ActionButton(
                    text = if (departmentId != null) "Update Department" else "Add Department",
                    onClick = viewModel::save,
                    enabled = !state.isSaving
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
