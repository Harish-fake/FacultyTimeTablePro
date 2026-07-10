package com.facultytimetable.pro.presentation.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.facultytimetable.pro.presentation.common.components.AppTopBar
import com.facultytimetable.pro.presentation.common.components.ConfirmDialog
import com.facultytimetable.pro.presentation.common.components.SectionHeader
import com.facultytimetable.pro.presentation.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Settings")

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            SectionHeader("Setup")
            SettingsCard(Icons.Default.RocketLaunch, "Setup Wizard", "Guided step-by-step configuration", onClick = { navController.navigate(Routes.SETUP_WIZARD) })

            Spacer(Modifier.height(24.dp))
            SectionHeader("Appearance")
            SettingsCard(Icons.Default.DarkMode, "Dark Mode", when (state.themeMode) { "dark" -> "Dark"; "light" -> "Light"; else -> "System default" }, onClick = { viewModel.setThemeMode(when (state.themeMode) { "system" -> "light"; "light" -> "dark"; else -> "system" }) })
            SettingsCard(Icons.Default.TextFields, "Font Size", "Default", onClick = { })
            SettingsCard(Icons.Default.Language, "Language", "English", onClick = { })

            Spacer(Modifier.height(24.dp))
            SectionHeader("Security")
            SettingsSwitch(Icons.Default.Lock, "App Lock", "Require PIN to open app", checked = state.isAppLockEnabled, onCheckedChange = viewModel::setAppLockEnabled)
            if (state.isAppLockEnabled) SettingsSwitch(Icons.Default.Fingerprint, "Biometric", "Use fingerprint to unlock", checked = state.isBiometricEnabled, onCheckedChange = viewModel::setBiometricEnabled)

            Spacer(Modifier.height(24.dp))
            SectionHeader("College Setup")
            SettingsCard(Icons.Default.School, "Departments", "Manage departments & HODs", onClick = { navController.navigate(Routes.DEPARTMENT_LIST) })
            SettingsCard(Icons.Default.DateRange, "Academic Years", "Manage academic years", onClick = { navController.navigate(Routes.ACADEMIC_YEAR_LIST) })
            SettingsCard(Icons.Default.CalendarViewWeek, "Semesters", "Manage semesters", onClick = { navController.navigate(Routes.SEMESTER_LIST) })
            SettingsCard(Icons.Default.CalendarMonth, "Working Days & Time Slots", "Configure periods, breaks, lunch", onClick = { navController.navigate(Routes.WORKING_DAY) })
            SettingsCard(Icons.Default.Celebration, "Holidays", "Manage college holidays", onClick = { navController.navigate(Routes.HOLIDAY_LIST) })

            Spacer(Modifier.height(24.dp))
            SectionHeader("Resources")
            SettingsCard(Icons.Default.AccessTime, "Rooms", "Manage rooms & facilities", onClick = { navController.navigate(Routes.ROOM_LIST) })
            SettingsCard(Icons.Default.Science, "Labs", "Manage laboratories & equipment", onClick = { navController.navigate(Routes.LAB_LIST) })
            SettingsCard(Icons.Default.ViewModule, "Sections", "Manage sections & student strength", onClick = { navController.navigate(Routes.SECTION_LIST) })
            SettingsCard(Icons.Default.Assignment, "Faculty Assignments", "Assign faculty to subjects", onClick = { navController.navigate(Routes.FACULTY_ASSIGNMENT) })

            Spacer(Modifier.height(24.dp))
            SectionHeader("Tools")
            SettingsCard(Icons.Default.Search, "Global Search", "Search across all entities", onClick = { navController.navigate(Routes.SEARCH) })
            SettingsCard(Icons.Default.Assessment, "Reports", "View workload, utilization, conflicts", onClick = { navController.navigate(Routes.REPORTS) })
            SettingsCard(Icons.Default.Restore, "Recycle Bin", "${state.recycleBinCount} items" + if (state.recycleBinCount > 0) " - Undo available" else "", onClick = {
                // TODO: navigate to recycle bin when route added
            })

            Spacer(Modifier.height(24.dp))
            SectionHeader("Data")
            SettingsCard(Icons.Default.Backup, "Backup & Restore", "Create and restore database backups", onClick = { navController.navigate(Routes.BACKUP) })
            SettingsCard(Icons.Default.Notifications, "Backup Reminder", "Every ${state.backupReminderInterval} days", onClick = { viewModel.setBackupReminderInterval(when (state.backupReminderInterval) { 7 -> 14; 14 -> 30; else -> 7 }) })
            SettingsCard(Icons.Default.DeleteSweep, "Reset All Data", "Clear all entries and start fresh", onClick = { viewModel.showResetDialog() })

            Spacer(Modifier.height(24.dp))
            SectionHeader("About")
            SettingsCard(Icons.Default.Info, "Version", "1.0.0", onClick = { })
            SettingsCard(Icons.Default.Info, "Developer", "Faculty TimeTable Pro Team", onClick = { })
            SettingsCard(Icons.Default.CheckCircle, "Setup Progress", "${(state.recycleBinCount)}", onClick = { navController.navigate(Routes.SETUP_WIZARD) })

            Spacer(Modifier.height(32.dp))
        }
    }

    if (state.showResetDialog) {
        ConfirmDialog(
            title = "Reset All Data",
            message = "This will permanently delete ALL data including faculty, subjects, timetable entries, and settings. This cannot be undone.",
            confirmText = "Reset Everything",
            onConfirm = viewModel::resetDatabase,
            onDismiss = viewModel::hideResetDialog,
            isDestructive = true
        )
    }
}

@Composable
private fun SettingsCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun SettingsSwitch(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
