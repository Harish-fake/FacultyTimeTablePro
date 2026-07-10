package com.facultytimetable.pro.presentation.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.facultytimetable.pro.data.local.db.dao.SemesterDao
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectType
import com.facultytimetable.pro.domain.repository.DepartmentRepository
import com.facultytimetable.pro.domain.repository.FacultyRepository
import com.facultytimetable.pro.domain.repository.SubjectRepository
import com.facultytimetable.pro.presentation.common.components.ActionButton
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.DropdownSelector
import com.facultytimetable.pro.presentation.common.components.LoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectFormState(
    val name: String = "",
    val code: String = "",
    val type: SubjectType = SubjectType.THEORY,
    val credits: String = "",
    val hoursPerWeek: String = "4",
    val labHoursPerWeek: String = "0",
    val departmentId: Long? = null,
    val semesterId: Long? = null,
    val facultyId: Long? = null,
    val isContinuousHours: Boolean = false,
    val isLabRequired: Boolean = false,
    val roomTypeRequired: String = "",
    val color: Long = 0xFF1976D2,
    val isActive: Boolean = true,
    val departments: List<DepartmentEntity> = emptyList(),
    val semesters: List<SemesterEntity> = emptyList(),
    val facultyList: List<FacultyEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
) {
    val isLabType: Boolean get() = type == SubjectType.LAB
    val isTheoryType: Boolean get() = type == SubjectType.THEORY
}

@HiltViewModel
class SubjectFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val subjectRepository: SubjectRepository,
    private val departmentRepository: DepartmentRepository,
    private val semesterDao: SemesterDao,
    private val facultyRepository: FacultyRepository
) : ViewModel() {

    private val subjectId: Long? = savedStateHandle.get<Long>("subjectId")?.takeIf { it > 0 }
    private val _state = MutableStateFlow(SubjectFormState())
    val state: StateFlow<SubjectFormState> = _state

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val departments = departmentRepository.getActiveDepartments().first()
            val semesters = semesterDao.getActiveSemesters().first()
            val facultyList = facultyRepository.getActiveFaculty().first()

            if (subjectId != null) {
                val subj = subjectRepository.getSubjectById(subjectId)
                if (subj != null) {
                    _state.value = _state.value.copy(
                        name = subj.name,
                        code = subj.code,
                        type = subj.type,
                        hoursPerWeek = subj.hoursPerWeek.toString(),
                        labHoursPerWeek = subj.labHoursPerWeek.toString(),
                        departmentId = subj.departmentId,
                        isLabRequired = subj.isLabRequired,
                        isActive = subj.isActive,
                        departments = departments,
                        semesters = semesters,
                        facultyList = facultyList,
                        isLoading = false
                    )
                    return@launch
                }
            }
            _state.value = _state.value.copy(
                departments = departments,
                semesters = semesters,
                facultyList = facultyList,
                isLoading = false
            )
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onCodeChange(v: String) { _state.value = _state.value.copy(code = v, error = null) }
    fun onTypeChange(t: SubjectType) { _state.value = _state.value.copy(type = t, error = null) }
    fun onCreditsChange(v: String) { _state.value = _state.value.copy(credits = v.filter { it.isDigit() || it == '.' }) }
    fun onHoursChange(v: String) { _state.value = _state.value.copy(hoursPerWeek = v.filter { it.isDigit() }) }
    fun onLabHoursChange(v: String) { _state.value = _state.value.copy(labHoursPerWeek = v.filter { it.isDigit() }) }
    fun onDepartmentSelected(id: Long) { _state.value = _state.value.copy(departmentId = id) }
    fun onSemesterSelected(semester: SemesterEntity) { _state.value = _state.value.copy(semesterId = semester.id) }
    fun onFacultySelected(faculty: FacultyEntity) { _state.value = _state.value.copy(facultyId = faculty.id) }
    fun onContinuousHoursChange(v: Boolean) { _state.value = _state.value.copy(isContinuousHours = v) }
    fun onLabRequiredChange(v: Boolean) { _state.value = _state.value.copy(isLabRequired = v) }
    fun onRoomTypeChange(v: String) { _state.value = _state.value.copy(roomTypeRequired = v) }
    fun onColorChange(c: Long) { _state.value = _state.value.copy(color = c) }
    fun onActiveChange(v: Boolean) { _state.value = _state.value.copy(isActive = v) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Course name is required"); return }
        if (s.code.isBlank()) { _state.value = s.copy(error = "Course code is required"); return }
        if (s.departmentId == null) { _state.value = s.copy(error = "Department is required"); return }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val entity = SubjectEntity(
                    id = subjectId ?: 0,
                    name = s.name.trim(),
                    code = s.code.uppercase().trim(),
                    type = s.type,
                    departmentId = s.departmentId,
                    hoursPerWeek = s.hoursPerWeek.toIntOrNull() ?: 4,
                    labHoursPerWeek = if (s.isLabType) (s.labHoursPerWeek.toIntOrNull() ?: 0) else 0,
                    isLabRequired = s.isLabRequired,
                    isActive = s.isActive
                )
                if (subjectId != null) subjectRepository.update(entity)
                else subjectRepository.insert(entity)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubjectFormScreen(
    subjectId: Long?,
    navController: NavController,
    viewModel: SubjectFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) navController.popBackStack() }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = if (subjectId != null) "Edit Subject" else "Add Subject",
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
                SectionLabel("Course Code *")
                OutlinedTextField(
                    value = state.code, onValueChange = viewModel::onCodeChange,
                    label = { Text("e.g. CS501") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(12.dp))

                SectionLabel("Course Name *")
                OutlinedTextField(
                    value = state.name, onValueChange = viewModel::onNameChange,
                    label = { Text("e.g. Data Structures") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(16.dp))

                DropdownSelector(
                    label = "Subject Type *",
                    selectedItem = state.type,
                    items = SubjectType.entries,
                    itemLabel = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onItemSelected = { viewModel.onTypeChange(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                DropdownSelector(
                    label = "Department *",
                    selectedItem = state.departments.find { it.id == state.departmentId },
                    items = state.departments,
                    itemLabel = { it.name },
                    onItemSelected = { viewModel.onDepartmentSelected(it.id) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                SectionLabel("Credits")
                OutlinedTextField(
                    value = state.credits, onValueChange = viewModel::onCreditsChange,
                    label = { Text("e.g. 4") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                SectionLabel("Hours per Week *")
                OutlinedTextField(
                    value = state.hoursPerWeek, onValueChange = viewModel::onHoursChange,
                    label = { Text("e.g. 3") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (state.isLabType) {
                    SectionLabel("Lab Hours per Week")
                    OutlinedTextField(
                        value = state.labHoursPerWeek, onValueChange = viewModel::onLabHoursChange,
                        label = { Text("e.g. 2") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                DropdownSelector(
                    label = "Semester",
                    selectedItem = state.semesters.find { it.id == state.semesterId },
                    items = state.semesters,
                    itemLabel = { "${it.name} (Sem ${it.semesterNumber})" },
                    onItemSelected = viewModel::onSemesterSelected,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                DropdownSelector(
                    label = "Faculty Assigned",
                    selectedItem = state.facultyList.find { it.id == state.facultyId },
                    items = state.facultyList,
                    itemLabel = { it.name },
                    onItemSelected = viewModel::onFacultySelected,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                ToggleRow(
                    label = "Continuous Hours Required",
                    checked = state.isContinuousHours,
                    onCheckedChange = viewModel::onContinuousHoursChange
                )

                if (state.isTheoryType) {
                    ToggleRow(
                        label = "Lab Required",
                        checked = state.isLabRequired,
                        onCheckedChange = viewModel::onLabRequiredChange
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SectionLabel("Room Type Required")
                DropdownSelector(
                    label = "Room Type",
                    selectedItem = listOf("Classroom", "Lab", "Seminar Hall")
                        .firstOrNull { it == state.roomTypeRequired },
                    items = listOf("", "Classroom", "Lab", "Seminar Hall"),
                    itemLabel = { if (it.isBlank()) "Not specified" else it },
                    onItemSelected = viewModel::onRoomTypeChange,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                SectionLabel("Color")
                Spacer(modifier = Modifier.height(8.dp))
                ColorPicker(
                    selectedColor = state.color,
                    onColorSelected = viewModel::onColorChange
                )
                Spacer(modifier = Modifier.height(16.dp))

                ToggleRow(
                    label = "Active",
                    checked = state.isActive,
                    onCheckedChange = viewModel::onActiveChange
                )

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
                    text = if (subjectId != null) "Update Subject" else "Add Subject",
                    onClick = viewModel::save,
                    enabled = !state.isSaving
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private val predefinedColors = listOf(
    0xFF1976D2L, 0xFF1565C0L, 0xFF0D47A1L,
    0xFF388E3CL, 0xFF2E7D32L, 0xFF1B5E20L,
    0xFF7B1FA2L, 0xFF6A1B9AL, 0xFF4A148CL,
    0xFFE64A19L, 0xFFD84315L, 0xFFBF360CL,
    0xFF00838FL, 0xFF00695CL, 0xFF004D40L,
    0xFFF57C00L, 0xFFEF6C00L, 0xFFE65100L,
    0xFFD32F2FL, 0xFFC62828L, 0xFFB71C1CL,
    0xFF455A64L, 0xFF37474FL, 0xFF263238L
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        predefinedColors.forEach { colorLong ->
            val color = Color(colorLong)
            val isSelected = colorLong == selectedColor
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier.border(1.dp, color.copy(alpha = 0.3f), CircleShape)
                    )
                    .clickable { onColorSelected(colorLong) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
