package com.facultytimetable.pro.presentation.settings

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.ActionButton
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.navigation.Routes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPinSetup by remember { mutableStateOf(false) }
    var showChangePin by remember { mutableStateOf(false) }
    var showDisableLock by remember { mutableStateOf(false) }
    var showResetDemo by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Settings",
            subtitle = "Preferences, security and data",
            onBackClick = { navController.popBackStack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SectionLabel("Appearance")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeChip("System", Icons.Default.Devices, state.themeMode == "system", Modifier.weight(1f)) {
                    viewModel.setThemeMode("system")
                }
                ThemeChip("Light", Icons.Default.LightMode, state.themeMode == "light", Modifier.weight(1f)) {
                    viewModel.setThemeMode("light")
                }
                ThemeChip("Dark", Icons.Default.DarkMode, state.themeMode == "dark", Modifier.weight(1f)) {
                    viewModel.setThemeMode("dark")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel("Security")
            SettingsSwitch(
                icon = Icons.Default.Lock,
                title = "App lock",
                subtitle = if (state.hasPin) "Ask for your PIN when the app opens"
                else "Create a PIN to protect this device",
                checked = state.isAppLockEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) showPinSetup = true else showDisableLock = true
                }
            )
            if (state.isAppLockEnabled && state.hasPin) {
                SettingsRow(
                    icon = Icons.Default.LockOpen,
                    title = "Change PIN",
                    subtitle = "Replace the current 4–8 digit PIN",
                    onClick = { showChangePin = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel("Academic data")
            SettingsRow(Icons.Default.School, "Departments", "Programmes and heads",
                onClick = { navController.navigate(Routes.DEPARTMENT_LIST) })
            SettingsRow(Icons.Default.People, "Faculty", "Teaching staff and workload",
                onClick = { navController.navigate(Routes.FACULTY_LIST) })
            SettingsRow(Icons.Default.Book, "Subjects", "Theory, labs and projects",
                onClick = { navController.navigate(Routes.SUBJECT_LIST) })
            SettingsRow(Icons.Default.MeetingRoom, "Rooms & labs", "Capacity and facilities",
                onClick = { navController.navigate(Routes.ROOM_LIST) })
            SettingsRow(Icons.Default.Group, "Sections", "Classes and strength",
                onClick = { navController.navigate(Routes.SECTION_LIST) })
            SettingsRow(Icons.Default.CalendarMonth, "Academic calendar", "Year, semesters and holidays",
                onClick = { navController.navigate(Routes.ACADEMIC_CALENDAR) })
            SettingsRow(Icons.Default.Schedule, "Time slots", "Periods, lunch and breaks",
                onClick = { navController.navigate(Routes.TIME_SLOT_CONFIG) })

            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel("Data")
            SettingsRow(Icons.Default.Backup, "Backup & restore", "Export or import a JSON snapshot",
                onClick = { navController.navigate(Routes.BACKUP) })
            SettingsRow(
                Icons.Default.Notifications,
                "Backup reminder",
                "Every ${state.backupReminderInterval} days" +
                    if (state.lastBackupAt > 0) " · last ${formatDate(state.lastBackupAt)}" else "",
                onClick = {
                    val next = when (state.backupReminderInterval) {
                        7 -> 14
                        14 -> 30
                        else -> 7
                    }
                    viewModel.setBackupReminderInterval(next)
                }
            )
            SettingsRow(
                Icons.Default.DeleteSweep,
                "Restore demo data",
                "Replace all data with the verified sample college",
                onClick = { showResetDemo = true }
            )

            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel("About")
            SettingsRow(Icons.Default.School, "Faculty TimeTable Pro", "Version 1.1.0 · Offline first", onClick = null)
            Text(
                "Built for department timetabling: deterministic generation, conflict validation and a complete sample data set.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showPinSetup) {
        PinSetupDialog(
            title = "Create app PIN",
            onDismiss = { showPinSetup = false },
            onConfirm = { pin, confirmation ->
                val error = viewModel.setPin(pin, confirmation)
                if (error == null) {
                    showPinSetup = false
                    scope.launch { snackbarHostState.showSnackbar("App lock enabled") }
                }
                error
            }
        )
    }

    if (showChangePin) {
        PinSetupDialog(
            title = "Change app PIN",
            requireCurrentPin = true,
            verifyCurrentPin = { viewModel.verifyPin(it) },
            onDismiss = { showChangePin = false },
            onConfirm = { pin, confirmation ->
                val error = viewModel.setPin(pin, confirmation)
                if (error == null) showChangePin = false
                error
            }
        )
    }

    if (showDisableLock) {
        ConfirmDialog(
            title = "Disable app lock?",
            message = "The current PIN will be removed. Anyone with this device will be able to open the app.",
            confirmText = "Disable",
            onConfirm = {
                viewModel.disableLock()
                showDisableLock = false
            },
            onDismiss = { showDisableLock = false },
            isDestructive = true
        )
    }

    if (showResetDemo) {
        ConfirmDialog(
            title = "Restore demo data?",
            message = "All departments, faculty, subjects, rooms, sections and timetable entries will be replaced with the verified sample data. Your PIN and theme are kept.",
            confirmText = "Restore",
            onConfirm = {
                showResetDemo = false
                viewModel.resetDemoData { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            },
            onDismiss = { showResetDemo = false },
            isDestructive = true
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = { Text(label, maxLines = 1) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
    )
}

@Composable
private fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.padding(vertical = 2.dp) else Modifier
            )
            .then(
                if (onClick != null) Modifier.clickableRow(onClick) else Modifier
            )
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

private fun formatDate(time: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(time))

@Composable
private fun PinSetupDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: suspend (String, String) -> String?,
    requireCurrentPin: Boolean = false,
    verifyCurrentPin: (suspend (String) -> Boolean)? = null
) {
    var currentPin by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (requireCurrentPin && verifyCurrentPin != null) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { currentPin = it.filter(Char::isDigit).take(8); error = null },
                        label = { Text("Current PIN") },
                        singleLine = true,
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                        trailingIcon = {
                            IconButton(onClick = { visible = !visible }) {
                                Icon(
                                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (visible) "Hide PIN" else "Show PIN"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8); error = null },
                    label = { Text("New PIN (4–8 digits)") },
                    singleLine = true,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it.filter(Char::isDigit).take(8); error = null },
                    label = { Text("Confirm new PIN") },
                    singleLine = true,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !checking,
                onClick = {
                    scope.launch {
                        checking = true
                        if (requireCurrentPin && verifyCurrentPin != null && !verifyCurrentPin(currentPin)) {
                            error = "The current PIN is incorrect."
                            checking = false
                            return@launch
                        }
                        error = onConfirm(pin, confirmation)
                        checking = false
                    }
                }
            ) { Text(if (checking) "Checking…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
