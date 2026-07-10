package com.facultytimetable.pro.presentation.navigation

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
import androidx.compose.runtime.collectAsState
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
import com.facultytimetable.pro.presentation.academicyear.AcademicYearFormScreen
import com.facultytimetable.pro.presentation.academicyear.AcademicYearListScreen
import com.facultytimetable.pro.presentation.assignment.FacultyAssignmentScreen
import com.facultytimetable.pro.presentation.backup.BackupScreen
import com.facultytimetable.pro.presentation.dashboard.DashboardScreen
import com.facultytimetable.pro.presentation.department.DepartmentFormScreen
import com.facultytimetable.pro.presentation.department.DepartmentListScreen
import com.facultytimetable.pro.presentation.faculty.detail.FacultyDetailScreen
import com.facultytimetable.pro.presentation.recyclebin.RecycleBinScreen
import com.facultytimetable.pro.presentation.timetable.daily.DailyTimetableScreen
import com.facultytimetable.pro.presentation.timetable.department.DepartmentTimetableScreen
import com.facultytimetable.pro.presentation.timetable.faculty.FacultyTimetableScreen
import com.facultytimetable.pro.presentation.timetable.room.RoomTimetableScreen
import com.facultytimetable.pro.presentation.timetable.section.SectionTimetableScreen
import com.facultytimetable.pro.presentation.faculty.form.FacultyFormScreen
import com.facultytimetable.pro.presentation.faculty.list.FacultyListScreen
import com.facultytimetable.pro.presentation.holiday.HolidayListScreen
import com.facultytimetable.pro.presentation.lab.LabFormScreen
import com.facultytimetable.pro.presentation.lab.LabListScreen
import com.facultytimetable.pro.presentation.leave.FacultyLeaveScreen
import com.facultytimetable.pro.presentation.onboarding.OnboardingScreen
import com.facultytimetable.pro.presentation.reports.ReportsScreen
import com.facultytimetable.pro.presentation.room.RoomFormScreen
import com.facultytimetable.pro.presentation.room.RoomListScreen
import com.facultytimetable.pro.presentation.search.SearchScreen
import com.facultytimetable.pro.presentation.section.SectionFormScreen
import com.facultytimetable.pro.presentation.section.SectionListScreen
import com.facultytimetable.pro.presentation.semester.SemesterFormScreen
import com.facultytimetable.pro.presentation.semester.SemesterListScreen
import com.facultytimetable.pro.presentation.settings.SettingsScreen
import com.facultytimetable.pro.presentation.setup.SetupWizardScreen
import com.facultytimetable.pro.presentation.subject.SubjectFormScreen
import com.facultytimetable.pro.presentation.subject.SubjectListScreen
import com.facultytimetable.pro.presentation.timeslot.TimeSlotConfigScreen
import com.facultytimetable.pro.presentation.timetable.generator.GeneratorScreen
import com.facultytimetable.pro.presentation.timetable.grid.TimetableGridScreen
import com.facultytimetable.pro.presentation.workingday.WorkingDayScreen

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

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
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            startDestination = Routes.ONBOARDING,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = { navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.ONBOARDING) { inclusive = true } } }
                )
            }

            composable(Routes.SETUP_WIZARD) { SetupWizardScreen(navController = navController) }
            composable(Routes.DASHBOARD) { DashboardScreen(navController = navController) }
            composable(Routes.FACULTY_LIST) { FacultyListScreen(navController = navController) }
            composable(Routes.TIMETABLE_GRID) { TimetableGridScreen(navController = navController) }
            composable(Routes.SETTINGS) { SettingsScreen(navController = navController) }

            composable(Routes.FACULTY_DETAIL, arguments = listOf(navArgument("facultyId") { type = NavType.LongType })) {
                FacultyDetailScreen(facultyId = it.arguments?.getLong("facultyId") ?: return@composable, navController = navController)
            }

            composable(Routes.FACULTY_FORM, arguments = listOf(navArgument("facultyId") { type = NavType.LongType; defaultValue = -1L })) {
                val id = it.arguments?.getLong("facultyId") ?: -1L
                FacultyFormScreen(facultyId = if (id == -1L) null else id, navController = navController)
            }

            composable(Routes.DEPARTMENT_LIST) { DepartmentListScreen(navController = navController) }
            composable(Routes.DEPARTMENT_FORM, arguments = listOf(navArgument("departmentId") { type = NavType.LongType; defaultValue = -1L })) {
                val id = it.arguments?.getLong("departmentId") ?: -1L
                DepartmentFormScreen(departmentId = if (id == -1L) null else id, navController = navController)
            }

            composable(Routes.SUBJECT_LIST) { SubjectListScreen(navController = navController) }
            composable(Routes.SUBJECT_FORM, arguments = listOf(navArgument("subjectId") { type = NavType.LongType; defaultValue = -1L })) {
                val id = it.arguments?.getLong("subjectId") ?: -1L
                SubjectFormScreen(subjectId = if (id == -1L) null else id, navController = navController)
            }

            composable(Routes.ROOM_LIST) { RoomListScreen(navController = navController) }
            composable(Routes.ROOM_FORM, arguments = listOf(navArgument("roomId") { type = NavType.LongType; defaultValue = -1L })) {
                val id = it.arguments?.getLong("roomId") ?: -1L
                RoomFormScreen(roomId = if (id == -1L) null else id, navController = navController)
            }

            composable(Routes.SECTION_LIST) { SectionListScreen(navController = navController) }
            composable(Routes.SECTION_FORM, arguments = listOf(navArgument("sectionId") { type = NavType.LongType; defaultValue = -1L })) {
                val id = it.arguments?.getLong("sectionId") ?: -1L
                SectionFormScreen(sectionId = if (id == -1L) null else id, navController = navController)
            }

            composable(Routes.ACADEMIC_YEAR_LIST) { AcademicYearListScreen(navController = navController) }
            composable(Routes.ACADEMIC_YEAR_FORM, arguments = listOf(navArgument("yearId") { type = NavType.LongType; defaultValue = -1L })) {
                val id = it.arguments?.getLong("yearId") ?: -1L
                AcademicYearFormScreen(yearId = if (id == -1L) null else id, navController = navController)
            }

            composable(Routes.SEMESTER_LIST) { SemesterListScreen(navController = navController) }
            composable(Routes.SEMESTER_FORM, arguments = listOf(navArgument("semesterId") { type = NavType.LongType; defaultValue = -1L })) {
                val id = it.arguments?.getLong("semesterId") ?: -1L
                SemesterFormScreen(semesterId = if (id == -1L) null else id, navController = navController)
            }

            composable(Routes.TIME_SLOT_CONFIG) { TimeSlotConfigScreen(navController = navController) }
            composable(Routes.HOLIDAY_LIST) { HolidayListScreen(navController = navController) }
            composable(Routes.FACULTY_LEAVE) { FacultyLeaveScreen(navController = navController) }
            composable(Routes.TIMETABLE_GENERATOR) { GeneratorScreen(navController = navController) }
            composable(Routes.SEARCH) { SearchScreen(navController = navController) }
            composable(Routes.REPORTS) { ReportsScreen(navController = navController) }
            composable(Routes.BACKUP) { BackupScreen(navController = navController) }

            composable(Routes.LAB_LIST) { LabListScreen(navController = navController) }
            composable(Routes.LAB_FORM, arguments = listOf(navArgument("labId") { type = NavType.LongType; defaultValue = -1L })) {
                val id = it.arguments?.getLong("labId") ?: -1L
                LabFormScreen(labId = if (id == -1L) null else id, navController = navController)
            }
            composable(Routes.FACULTY_ASSIGNMENT) { FacultyAssignmentScreen(navController = navController) }
            composable(Routes.WORKING_DAY) { WorkingDayScreen(navController = navController) }

            composable(Routes.RECYCLE_BIN) { RecycleBinScreen(navController = navController) }

            composable(Routes.TIMETABLE_SECTION, arguments = listOf(navArgument("sectionId") { type = NavType.LongType })) {
                SectionTimetableScreen(
                    sectionId = it.arguments?.getLong("sectionId") ?: return@composable,
                    navController = navController
                )
            }

            composable(Routes.TIMETABLE_DAILY, arguments = listOf(navArgument("day") { type = NavType.IntType })) {
                DailyTimetableScreen(
                    day = it.arguments?.getInt("day") ?: return@composable,
                    navController = navController
                )
            }

            composable(Routes.TIMETABLE_FACULTY, arguments = listOf(navArgument("facultyId") { type = NavType.LongType })) {
                FacultyTimetableScreen(
                    facultyId = it.arguments?.getLong("facultyId") ?: return@composable,
                    navController = navController
                )
            }

            composable(Routes.TIMETABLE_DEPARTMENT, arguments = listOf(navArgument("departmentId") { type = NavType.LongType })) {
                DepartmentTimetableScreen(
                    departmentId = it.arguments?.getLong("departmentId") ?: return@composable,
                    navController = navController
                )
            }

            composable(Routes.TIMETABLE_ROOM, arguments = listOf(navArgument("roomId") { type = NavType.LongType })) {
                RoomTimetableScreen(
                    roomId = it.arguments?.getLong("roomId") ?: return@composable,
                    navController = navController
                )
            }
        }
    }
}
