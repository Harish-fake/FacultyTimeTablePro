package com.facultytimetable.pro.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.facultytimetable.pro.presentation.academic.AcademicCalendarScreen
import com.facultytimetable.pro.presentation.academic.TimeSlotConfigScreen
import com.facultytimetable.pro.presentation.backup.BackupScreen
import com.facultytimetable.pro.presentation.dashboard.DashboardScreen
import com.facultytimetable.pro.presentation.department.DepartmentFormScreen
import com.facultytimetable.pro.presentation.department.DepartmentListScreen
import com.facultytimetable.pro.presentation.faculty.detail.FacultyDetailScreen
import com.facultytimetable.pro.presentation.faculty.form.FacultyFormScreen
import com.facultytimetable.pro.presentation.faculty.leave.FacultyLeaveScreen
import com.facultytimetable.pro.presentation.faculty.list.FacultyListScreen
import com.facultytimetable.pro.presentation.more.MoreScreen
import com.facultytimetable.pro.presentation.reports.ReportsScreen
import com.facultytimetable.pro.presentation.room.RoomFormScreen
import com.facultytimetable.pro.presentation.sampledata.SampleDataScreen
import com.facultytimetable.pro.presentation.room.RoomListScreen
import com.facultytimetable.pro.presentation.search.SearchScreen
import com.facultytimetable.pro.presentation.section.SectionFormScreen
import com.facultytimetable.pro.presentation.section.SectionListScreen
import com.facultytimetable.pro.presentation.settings.SettingsScreen
import com.facultytimetable.pro.presentation.subject.SubjectFormScreen
import com.facultytimetable.pro.presentation.subject.SubjectListScreen
import com.facultytimetable.pro.presentation.timetable.faculty.FacultyTimetableScreen
import com.facultytimetable.pro.presentation.timetable.generator.GeneratorScreen
import com.facultytimetable.pro.presentation.timetable.grid.TimetableGridScreen
import com.facultytimetable.pro.presentation.timetable.section.SectionTimetableScreen

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, "Home", Icons.Default.Home),
    BottomNavItem(Routes.TIMETABLE_GRID, "Timetable", Icons.Default.CalendarMonth),
    BottomNavItem(Routes.FACULTY_LIST, "Faculty", Icons.Default.People),
    BottomNavItem(Routes.REPORTS, "Reports", Icons.Default.Analytics),
    BottomNavItem(Routes.MORE, "More", Icons.Default.MoreHoriz)
)

private val bottomBarRoutes = bottomNavItems.map { it.route }.toSet()

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.route == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(220)) },
            exitTransition = { fadeOut(animationSpec = tween(180)) }
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(navController = navController)
            }

            composable(Routes.TIMETABLE_GRID) {
                TimetableGridScreen(navController = navController)
            }

            composable(Routes.FACULTY_LIST) {
                FacultyListScreen(navController = navController)
            }

            composable(Routes.REPORTS) {
                ReportsScreen(navController = navController, showBack = false)
            }

            composable(Routes.MORE) {
                MoreScreen(navController = navController)
            }

            composable(
                route = Routes.FACULTY_DETAIL,
                arguments = listOf(navArgument("facultyId") { type = NavType.LongType })
            ) { backStackEntry ->
                val facultyId = backStackEntry.arguments?.getLong("facultyId") ?: return@composable
                FacultyDetailScreen(facultyId = facultyId, navController = navController)
            }

            composable(
                route = Routes.FACULTY_FORM,
                arguments = listOf(navArgument("facultyId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val facultyId = backStackEntry.arguments?.getLong("facultyId") ?: -1L
                FacultyFormScreen(
                    facultyId = if (facultyId == -1L) null else facultyId,
                    navController = navController
                )
            }

            composable(Routes.DEPARTMENT_LIST) {
                DepartmentListScreen(navController = navController)
            }

            composable(
                route = Routes.DEPARTMENT_FORM,
                arguments = listOf(navArgument("departmentId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val deptId = backStackEntry.arguments?.getLong("departmentId") ?: -1L
                DepartmentFormScreen(
                    departmentId = if (deptId == -1L) null else deptId,
                    navController = navController
                )
            }

            composable(Routes.SUBJECT_LIST) {
                SubjectListScreen(navController = navController)
            }

            composable(
                route = Routes.SUBJECT_FORM,
                arguments = listOf(navArgument("subjectId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val subjId = backStackEntry.arguments?.getLong("subjectId") ?: -1L
                SubjectFormScreen(
                    subjectId = if (subjId == -1L) null else subjId,
                    navController = navController
                )
            }

            composable(Routes.ROOM_LIST) {
                RoomListScreen(navController = navController)
            }

            composable(
                route = Routes.ROOM_FORM,
                arguments = listOf(navArgument("roomId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getLong("roomId") ?: -1L
                RoomFormScreen(
                    roomId = if (roomId == -1L) null else roomId,
                    navController = navController
                )
            }

            composable(Routes.SECTION_LIST) {
                SectionListScreen(navController = navController)
            }

            composable(
                route = Routes.SECTION_FORM,
                arguments = listOf(navArgument("sectionId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val sectionId = backStackEntry.arguments?.getLong("sectionId") ?: -1L
                SectionFormScreen(
                    sectionId = if (sectionId == -1L) null else sectionId,
                    navController = navController
                )
            }

            composable(
                route = Routes.TIMETABLE_SECTION,
                arguments = listOf(navArgument("sectionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val sectionId = backStackEntry.arguments?.getLong("sectionId") ?: return@composable
                SectionTimetableScreen(sectionId = sectionId, navController = navController)
            }

            composable(
                route = Routes.TIMETABLE_FACULTY,
                arguments = listOf(navArgument("facultyId") { type = NavType.LongType })
            ) {
                FacultyTimetableScreen(navController = navController)
            }

            composable(
                route = Routes.FACULTY_LEAVE,
                arguments = listOf(navArgument("facultyId") { type = NavType.LongType })
            ) {
                FacultyLeaveScreen(navController = navController)
            }

            composable(Routes.TIMETABLE_GENERATOR) {
                GeneratorScreen(navController = navController)
            }

            composable(Routes.SAMPLE_DATA) {
                SampleDataScreen(navController = navController)
            }

            composable(Routes.ACADEMIC_CALENDAR) {
                AcademicCalendarScreen(navController = navController)
            }

            composable(Routes.TIME_SLOT_CONFIG) {
                TimeSlotConfigScreen(navController = navController)
            }

            composable(Routes.SEARCH) {
                SearchScreen(navController = navController)
            }

            composable(Routes.BACKUP) {
                BackupScreen(navController = navController)
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(navController = navController)
            }
        }
    }
}
