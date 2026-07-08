package com.facultytimetable.pro.presentation.section

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.dao.SectionDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import com.facultytimetable.pro.presentation.common.components.ActionButton
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.DropdownSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectionFormState(
    val name: String = "",
    val strength: String = "",
    val semesters: List<SemesterEntity> = emptyList(),
    val selectedSemesterId: Long? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SectionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sectionDao: SectionDao,
    private val semesterDao: SemesterDao
) : ViewModel() {

    private val sectionId: Long? = savedStateHandle.get<Long>("sectionId")?.takeIf { it > 0 }
    private val _state = MutableStateFlow(SectionFormState())
    val state: StateFlow<SectionFormState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val semesters = semesterDao.getActiveSemesters().first()
            if (sectionId != null) {
                val sec = sectionDao.getSectionById(sectionId)
                if (sec != null) {
                    _state.value = _state.value.copy(
                        name = sec.name, strength = sec.strength.toString(),
                        selectedSemesterId = sec.semesterId, semesters = semesters,
                        isEditing = true, isLoading = false
                    )
                    return@launch
                }
            }
            _state.value = _state.value.copy(semesters = semesters, isLoading = false)
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onStrengthChange(v: String) { _state.value = _state.value.copy(strength = v.filter { it.isDigit() }) }
    fun onSemesterSelected(semester: SemesterEntity) { _state.value = _state.value.copy(selectedSemesterId = semester.id) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Name is required"); return }
        if (s.selectedSemesterId == null) { _state.value = s.copy(error = "Select a semester"); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                sectionDao.insert(
                    SectionEntity(
                        id = sectionId ?: 0, name = s.name,
                        semesterId = s.selectedSemesterId,
                        strength = s.strength.toIntOrNull() ?: 0
                    )
                )
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }
}

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.name, onValueChange = viewModel::onNameChange,
                label = { Text("Section Name *") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.strength, onValueChange = viewModel::onStrengthChange,
                label = { Text("Strength") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(12.dp))
            DropdownSelector(
                label = "Semester *",
                items = state.semesters,
                selectedItem = state.semesters.find { it.id == state.selectedSemesterId },
                itemLabel = { "${it.name} (Sem ${it.semesterNumber})" },
                onItemSelected = viewModel::onSemesterSelected,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(24.dp))
            ActionButton(
                text = if (sectionId != null) "Update Section" else "Add Section",
                onClick = viewModel::save, enabled = !state.isSaving
            )
        }
    }
}
