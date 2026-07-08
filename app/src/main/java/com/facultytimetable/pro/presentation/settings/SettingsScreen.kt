package com.facultytimetable.pro.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TextFields
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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                subtitle = when (state.themeMode) {
                    "dark" -> "Dark"
                    "light" -> "Light"
                    else -> "System default"
                },
                onClick = {
                    val next = when (state.themeMode) {
                        "system" -> "light"
                        "light" -> "dark"
                        else -> "system"
                    }
                    viewModel.setThemeMode(next)
                }
            )

            SettingsCard(
                icon = Icons.Default.TextFields,
                title = "Font Size",
                subtitle = "Default",
                onClick = { /* TODO */ }
            )

            SettingsCard(
                icon = Icons.Default.Language,
                title = "Language",
                subtitle = "English",
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Security",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSwitch(
                icon = Icons.Default.Lock,
                title = "App Lock",
                subtitle = "Require PIN to open app",
                checked = state.isAppLockEnabled,
                onCheckedChange = viewModel::setAppLockEnabled
            )

            if (state.isAppLockEnabled) {
                SettingsSwitch(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric",
                    subtitle = "Use fingerprint to unlock",
                    checked = state.isBiometricEnabled,
                    onCheckedChange = viewModel::setBiometricEnabled
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Data Management",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard(
                icon = Icons.Default.School,
                title = "Departments",
                subtitle = "Manage departments & HODs",
                onClick = { navController.navigate(com.facultytimetable.pro.presentation.navigation.Routes.DEPARTMENT_LIST) }
            )

            SettingsCard(
                icon = Icons.Default.People,
                title = "Faculty",
                subtitle = "Manage faculty & designations",
                onClick = { navController.navigate(com.facultytimetable.pro.presentation.navigation.Routes.FACULTY_LIST) }
            )

            SettingsCard(
                icon = Icons.Default.Book,
                title = "Subjects",
                subtitle = "Manage subjects & codes",
                onClick = { navController.navigate(com.facultytimetable.pro.presentation.navigation.Routes.SUBJECT_LIST) }
            )

            SettingsCard(
                icon = Icons.Default.MeetingRoom,
                title = "Rooms & Labs",
                subtitle = "Manage rooms & facilities",
                onClick = { navController.navigate(com.facultytimetable.pro.presentation.navigation.Routes.ROOM_LIST) }
            )

            SettingsCard(
                icon = Icons.Default.Group,
                title = "Sections",
                subtitle = "Manage sections & strength",
                onClick = { navController.navigate(com.facultytimetable.pro.presentation.navigation.Routes.SECTION_LIST) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Data",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard(
                icon = Icons.Default.Backup,
                title = "Backup & Restore",
                subtitle = "Manage backups",
                onClick = { navController.navigate(com.facultytimetable.pro.presentation.navigation.Routes.BACKUP) }
            )

            SettingsCard(
                icon = Icons.Default.Notifications,
                title = "Backup Reminder",
                subtitle = "Every ${state.backupReminderInterval} days",
                onClick = {
                    val next = when (state.backupReminderInterval) {
                        7 -> 14
                        14 -> 30
                        else -> 7
                    }
                    viewModel.setBackupReminderInterval(next)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "About",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCardSimple(title = "Version", subtitle = "1.0.0")
            SettingsCardSimple(title = "Developer", subtitle = "Faculty TimeTable Pro Team")

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun SettingsCardSimple(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(40.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
