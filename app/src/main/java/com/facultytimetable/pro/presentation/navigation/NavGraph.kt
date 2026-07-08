package com.facultytimetable.pro.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.facultytimetable.pro.presentation.dashboard.DashboardScreen
import com.facultytimetable.pro.presentation.faculty.list.FacultyListScreen
import com.facultytimetable.pro.presentation.timetable.grid.TimetableGridScreen
import com.facultytimetable.pro.presentation.settings.SettingsScreen

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, "Dashboard", Icons.Default.Home),
    BottomNavItem(Routes.TIMETABLE_GRID, "Timetable", Icons.Default.CalendarMonth),
    BottomNavItem(Routes.FACULTY_LIST, "Faculty", Icons.Default.People),
    BottomNavItem(Routes.SETTINGS, "Settings", Icons.Default.Settings)
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
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
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(navController = navController)
            }

            composable(Routes.FACULTY_LIST) {
                FacultyListScreen(navController = navController)
            }

            composable(Routes.TIMETABLE_GRID) {
                TimetableGridScreen(navController = navController)
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(navController = navController)
            }

            composable(
                route = Routes.FACULTY_DETAIL,
                arguments = listOf(navArgument("facultyId") { type = NavType.LongType })
            ) { backStackEntry ->
                val facultyId = backStackEntry.arguments?.getLong("facultyId") ?: return@composable
                com.facultytimetable.pro.presentation.faculty.detail.FacultyDetailScreen(
                    facultyId = facultyId,
                    navController = navController
                )
            }

            composable(
                route = Routes.FACULTY_FORM,
                arguments = listOf(navArgument("facultyId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val facultyId = backStackEntry.arguments?.getLong("facultyId") ?: -1L
                com.facultytimetable.pro.presentation.faculty.form.FacultyFormScreen(
                    facultyId = if (facultyId == -1L) null else facultyId,
                    navController = navController
                )
            }

            composable(Routes.DEPARTMENT_LIST) {
                com.facultytimetable.pro.presentation.department.DepartmentListScreen(navController = navController)
            }

            composable(
                route = Routes.DEPARTMENT_FORM,
                arguments = listOf(navArgument("departmentId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val deptId = backStackEntry.arguments?.getLong("departmentId") ?: -1L
                com.facultytimetable.pro.presentation.department.DepartmentFormScreen(
                    departmentId = if (deptId == -1L) null else deptId,
                    navController = navController
                )
            }

            composable(Routes.SUBJECT_LIST) {
                com.facultytimetable.pro.presentation.subject.SubjectListScreen(navController = navController)
            }

            composable(
                route = Routes.SUBJECT_FORM,
                arguments = listOf(navArgument("subjectId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val subjId = backStackEntry.arguments?.getLong("subjectId") ?: -1L
                com.facultytimetable.pro.presentation.subject.SubjectFormScreen(
                    subjectId = if (subjId == -1L) null else subjId,
                    navController = navController
                )
            }

            composable(Routes.ROOM_LIST) {
                com.facultytimetable.pro.presentation.room.RoomListScreen(navController = navController)
            }

            composable(
                route = Routes.ROOM_FORM,
                arguments = listOf(navArgument("roomId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getLong("roomId") ?: -1L
                com.facultytimetable.pro.presentation.room.RoomFormScreen(
                    roomId = if (roomId == -1L) null else roomId,
                    navController = navController
                )
            }

            composable(
                route = Routes.TIMETABLE_SECTION,
                arguments = listOf(navArgument("sectionId") { type = NavType.LongType })
            ) {
                com.facultytimetable.pro.presentation.timetable.section.SectionTimetableScreen(
                    navController = navController
                )
            }

            composable(Routes.TIMETABLE_GENERATOR) {
                com.facultytimetable.pro.presentation.timetable.generator.GeneratorScreen(
                    navController = navController
                )
            }

            composable(Routes.SEARCH) {
                com.facultytimetable.pro.presentation.search.SearchScreen(navController = navController)
            }

            composable(Routes.REPORTS) {
                com.facultytimetable.pro.presentation.reports.ReportsScreen(navController = navController)
            }

            composable(Routes.SECTION_LIST) {
                com.facultytimetable.pro.presentation.section.SectionListScreen(navController = navController)
            }

            composable(
                route = Routes.SECTION_FORM,
                arguments = listOf(navArgument("sectionId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val sectionId = backStackEntry.arguments?.getLong("sectionId") ?: -1L
                com.facultytimetable.pro.presentation.section.SectionFormScreen(
                    sectionId = if (sectionId == -1L) null else sectionId,
                    navController = navController
                )
            }

            composable(Routes.BACKUP) {
                com.facultytimetable.pro.presentation.backup.BackupScreen(navController = navController)
            }
        }
    }
}
