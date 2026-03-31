package com.example.dailysnapshot

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.example.dailysnapshot.ui.camera.CameraScreen
import com.example.dailysnapshot.ui.detail.DetailScreen
import com.example.dailysnapshot.ui.edit.EditScreen
import com.example.dailysnapshot.ui.gallery.GalleryScreen
import com.example.dailysnapshot.ui.licenses.LicensesScreen
import com.example.dailysnapshot.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable data object Gallery : Screen
    @Serializable data object Camera : Screen
    /** [snapshotId] defaults to -1 for new entries; treat -1 as null at the call site. */
    @Serializable data class Edit(val rawFilePath: String, val snapshotId: Long = -1L) : Screen
    @Serializable data class Detail(val snapshotId: Long) : Screen
    @Serializable data object Settings : Screen
    @Serializable data object Licenses : Screen
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Gallery) {

        composable<Screen.Gallery> {
            GalleryScreen(
                onOpenCamera = { navController.navigate(Screen.Camera) },
                onOpenDetail = { id -> navController.navigate(Screen.Detail(id)) },
                onOpenSettings = { navController.navigate(Screen.Settings) }
            )
        }

        composable<Screen.Camera>(
            deepLinks = listOf(navDeepLink<Screen.Camera>(basePath = "dailysnapshot://camera"))
        ) {
            CameraScreen(
                onPhotoCaptured = { path -> navController.navigate(Screen.Edit(rawFilePath = path)) },
                onClose = { navController.popBackStack() }
            )
        }

        composable<Screen.Edit> { backStackEntry ->
            val dest = backStackEntry.toRoute<Screen.Edit>()
            val snapshotId = dest.snapshotId.takeIf { it != -1L }
            EditScreen(
                rawFilePath = dest.rawFilePath,
                snapshotId = snapshotId,
                onSaved = {
                    // New entry saved — pop Camera + Edit, land back on Gallery.
                    navController.popBackStack<Screen.Gallery>(inclusive = false)
                },
                onNavigateBack = {
                    // Re-edit saved or discard — pop to previous screen (Detail or Gallery).
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.Detail>(
            deepLinks = listOf(navDeepLink<Screen.Detail>(basePath = "dailysnapshot://detail"))
        ) { backStackEntry ->
            val dest = backStackEntry.toRoute<Screen.Detail>()
            DetailScreen(
                snapshotId = dest.snapshotId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id, path ->
                    navController.navigate(Screen.Edit(rawFilePath = path, snapshotId = id))
                },
                onNavigateToGallery = {
                    navController.popBackStack<Screen.Gallery>(inclusive = false)
                }
            )
        }

        composable<Screen.Settings> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenLicenses = { navController.navigate(Screen.Licenses) }
            )
        }

        composable<Screen.Licenses> {
            LicensesScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
