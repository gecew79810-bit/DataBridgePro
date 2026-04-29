package com.databridgepro.filemanager.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.databridgepro.filemanager.presentation.backup.BackupScreen
import com.databridgepro.filemanager.presentation.files.FilesScreen
import com.databridgepro.filemanager.presentation.home.HomeScreen
import com.databridgepro.filemanager.presentation.permission.PermissionScreen
import com.databridgepro.filemanager.presentation.settings.SettingsScreen
import com.databridgepro.filemanager.presentation.viewer.FileViewerScreen
import com.databridgepro.filemanager.presentation.viewer.CategoryScreen

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Files : Screen("files", "Files", Icons.Filled.Folder, Icons.Outlined.Folder)
    data object Backup : Screen("backup", "Backup", Icons.Filled.Backup, Icons.Outlined.Backup)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    data object Permission : Screen("permission", "Permission", Icons.Filled.Settings, Icons.Outlined.Settings)
}

private val bottomNavItems = listOf(Screen.Home, Screen.Files, Screen.Backup, Screen.Settings)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route ?: ""
    val showBottomBar = currentRoute != Screen.Permission.route &&
        !currentRoute.startsWith("viewer/") &&
        !currentRoute.startsWith("category/")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    screen.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Permission.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(tween(300)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start, tween(300)
                )
            },
            exitTransition = {
                fadeOut(tween(200)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start, tween(300)
                )
            },
            popEnterTransition = {
                fadeIn(tween(300)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End, tween(300)
                )
            },
            popExitTransition = {
                fadeOut(tween(200)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End, tween(300)
                )
            }
        ) {
            composable(Screen.Permission.route) {
                PermissionScreen(
                    onAllPermissionsGranted = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Permission.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToFiles = { navController.navigate(Screen.Files.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToFolder = { path, name ->
                        navController.navigate("files_folder/${android.net.Uri.encode(path)}/${android.net.Uri.encode(name)}")
                    },
                    onNavigateToCategory = { type ->
                        navController.navigate("category/$type")
                    },
                    onOpenFile = { file ->
                        val type = when {
                            file.isImage -> "image"
                            file.isVideo -> "video"
                            file.isAudio -> "audio"
                            file.isPdf -> "pdf"
                            file.isText -> "text"
                            file.isApk -> "apk"
                            else -> "other"
                        }
                        navController.navigate("viewer/${android.net.Uri.encode(file.path)}/${android.net.Uri.encode(file.name)}/$type")
                    }
                )
            }
            composable(Screen.Files.route) {
                FilesScreen(
                    onOpenViewer = { file ->
                        val type = when {
                            file.isImage -> "image"
                            file.isVideo -> "video"
                            file.isAudio -> "audio"
                            file.isPdf -> "pdf"
                            file.isText -> "text"
                            file.isApk -> "apk"
                            else -> "other"
                        }
                        navController.navigate("viewer/${android.net.Uri.encode(file.path)}/${android.net.Uri.encode(file.name)}/$type")
                    }
                )
            }
            composable(
                "files_folder/{path}/{name}",
                arguments = listOf(
                    navArgument("path") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { backStack ->
                val path = backStack.arguments?.getString("path") ?: ""
                val name = backStack.arguments?.getString("name") ?: ""
                FolderFilesScreen(
                    initialPath = path,
                    folderName = name,
                    onBack = { navController.popBackStack() },
                    onOpenViewer = { file ->
                        val type = when {
                            file.isImage -> "image"
                            file.isVideo -> "video"
                            file.isAudio -> "audio"
                            file.isPdf -> "pdf"
                            file.isText -> "text"
                            file.isApk -> "apk"
                            else -> "other"
                        }
                        navController.navigate("viewer/${android.net.Uri.encode(file.path)}/${android.net.Uri.encode(file.name)}/$type")
                    }
                )
            }
            composable(
                "viewer/{path}/{name}/{type}",
                arguments = listOf(
                    navArgument("path") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                    navArgument("type") { type = NavType.StringType }
                )
            ) { backStack ->
                FileViewerScreen(
                    filePath = backStack.arguments?.getString("path") ?: "",
                    fileName = backStack.arguments?.getString("name") ?: "",
                    fileType = backStack.arguments?.getString("type") ?: "other",
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                "category/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { backStack ->
                CategoryScreen(
                    type = backStack.arguments?.getString("type") ?: "image",
                    onBack = { navController.popBackStack() },
                    onOpenViewer = { file ->
                        val fileType = when {
                            file.isImage -> "image"
                            file.isVideo -> "video"
                            file.isAudio -> "audio"
                            file.isPdf -> "pdf"
                            file.isText -> "text"
                            file.isApk -> "apk"
                            else -> "other"
                        }
                        navController.navigate("viewer/${android.net.Uri.encode(file.path)}/${android.net.Uri.encode(file.name)}/$fileType")
                    }
                )
            }
            composable(Screen.Backup.route) {
                BackupScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun FolderFilesScreen(
    initialPath: String,
    folderName: String,
    onBack: () -> Unit,
    onOpenViewer: (com.databridgepro.filemanager.data.model.FileItem) -> Unit
) {
    val viewModel: com.databridgepro.filemanager.presentation.files.FilesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    androidx.compose.runtime.LaunchedEffect(initialPath) {
        viewModel.initializeAtPath(initialPath, folderName)
    }
    FilesScreen(viewModel = viewModel, onOpenViewer = onOpenViewer, onExitScreen = onBack)
}

@Preview(showBackground = true)
@Composable
private fun AppNavHostPreview() {
    AppNavHost()
}
