package com.neomods.libdumper.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neomods.libdumper.ui.screens.about.AboutScreen
import com.neomods.libdumper.ui.screens.main.MainDumperScreen
import com.neomods.libdumper.ui.screens.permissions.PermissionScreen
import com.neomods.libdumper.ui.screens.settings.SettingsScreen
import com.neomods.libdumper.ui.screens.splash.SplashScreen
import com.neomods.libdumper.utils.PermissionUtils

@Composable
fun LibDumperNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    val destination = if (PermissionUtils.hasStoragePermission(context)) {
                        Screen.Main.route
                    } else {
                        Screen.Permissions.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Permissions.route) {
            PermissionScreen(
                onPermissionsGranted = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Permissions.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainDumperScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Permissions : Screen("permissions")
    object Main : Screen("main")
    object Settings : Screen("settings")
    object About : Screen("about")
}
