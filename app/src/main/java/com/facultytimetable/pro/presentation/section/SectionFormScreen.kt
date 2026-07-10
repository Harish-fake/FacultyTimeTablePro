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
import androidx.compose.material3.Switch
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
import com.facultytimetable.pro.data.local.db.dao.FacultyDao
import com.facultytimetable.pro.data.local.db.dao.RoomDao
import com.facultytimetable.pro.data.local.db.dao.SectionDao
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
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
    val classAdvisor: String = "",
    val roomId: Long = -1L,
    val semesters: List<SemesterEntity> = emptyList(),
    val faculties: List<FacultyEntity> = emptyList(),
    val rooms: List<RoomEntity> = emptyList(),
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
    private val semesterDao: SemesterDao,
    private val facultyDao: FacultyDao,
    private val roomDao: RoomDao
) : ViewModel() {

    private val sectionId: Long? = savedStateHandle.get<Long>("sectionId")?.takeIf { it > 0 }
    private val _state = MutableStateFlow(SectionFormState())
    val state: StateFlow<SectionFormState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val semesters = semesterDao.getActiveSemesters().first()
            val faculties = facultyDao.getActiveFaculty().first()
            val rooms = roomDao.getActiveRooms().first()
            if (sectionId != null) {
                val sec = sectionDao.getSectionById(sectionId)
                if (sec != null) {
                    _state.value = _state.value.copy(
                        name = sec.name, strength = sec.strength.toString(),
                        classAdvisor = sec.classAdvisor,
                        roomId = sec.roomId,
                        selectedSemesterId = sec.semesterId,
                        semesters = semesters, faculties = faculties, rooms = rooms,
                        isEditing = true, isLoading = false
                    )
                    return@launch
                }
            }
            _state.value = _state.value.copy(semesters = semesters, faculties = faculties, rooms = rooms, isLoading = false)
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onStrengthChange(v: String) { _state.value = _state.value.copy(strength = v.filter { it.isDigit() }) }
    fun onClassAdvisorChange(v: String) { _state.value = _state.value.copy(classAdvisor = v) }
    fun onSemesterSelected(semester: SemesterEntity) { _state.value = _state.value.copy(selectedSemesterId = semester.id) }
    fun onRoomSelected(room: RoomEntity) { _state.value = _state.value.copy(roomId = room.id) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Name is required"); return }
        if (s.selectedSemesterId == null) { _state.value = s.copy(error = "Select a semester"); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
                try {
                val entity = SectionEntity(
                    id = sectionId ?: 0, name = s.name,
                    semesterId = s.selectedSemesterId,
                    strength = s.strength.toIntOrNull() ?: 0,
                    classAdvisor = s.classAdvisor,
                    roomId = s.roomId
                )
                if (sectionId != null) {
                    sectionDao.update(entity)
                } else {
                    sectionDao.insert(entity)
                }
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
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
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
                label = { Text("Student Strength") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Number)
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
            Spacer(modifier = Modifier.height(12.dp))
            DropdownSelector(
                label = "Class Advisor",
                items = state.faculties,
                selectedItem = state.faculties.find { it.name == state.classAdvisor },
                itemLabel = { it.name },
                onItemSelected = { viewModel.onClassAdvisorChange(it.name) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            DropdownSelector(
                label = "Room",
                items = state.rooms,
                selectedItem = state.rooms.find { it.id == state.roomId },
                itemLabel = { it.name },
                onItemSelected = viewModel::onRoomSelected,
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
