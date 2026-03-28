package com.example.dailysnapshot

/**
 * TEMPORARY — replaced by AppNavGraph in DAI-15.
 * Provides a minimal navigation scaffold for manual testing.
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.dailysnapshot.ui.camera.CameraScreen
import com.example.dailysnapshot.ui.detail.DetailScreen
import com.example.dailysnapshot.ui.edit.EditScreen
import com.example.dailysnapshot.ui.gallery.GalleryScreen

@Composable
fun DebugNavHost() {
    var currentScreen by remember { mutableStateOf<DebugScreen>(DebugScreen.Gallery) }

    when (val screen = currentScreen) {
        DebugScreen.Gallery -> GalleryScreen(
            onOpenCamera = { currentScreen = DebugScreen.Camera },
            onOpenDetail = { id -> currentScreen = DebugScreen.Detail(id) },
            onOpenSettings = { /* DAI-20 */ }
        )
        DebugScreen.Camera -> CameraScreen(
            onPhotoCaptured = { path -> currentScreen = DebugScreen.Edit(path) },
            onClose = { currentScreen = DebugScreen.Gallery }
        )
        is DebugScreen.Detail -> DetailScreen(
            snapshotId = screen.snapshotId,
            onNavigateBack = { currentScreen = DebugScreen.Gallery },
            onNavigateToEdit = { id, path ->
                currentScreen = DebugScreen.Edit(rawFilePath = path, snapshotId = id)
            },
            onNavigateToGallery = { currentScreen = DebugScreen.Gallery }
        )
        is DebugScreen.Edit -> EditScreen(
            rawFilePath = screen.rawFilePath,
            snapshotId = screen.snapshotId,
            onSaved = { currentScreen = DebugScreen.Gallery },
            onNavigateBack = { currentScreen = DebugScreen.Gallery }
        )
    }
}

private sealed class DebugScreen {
    object Gallery : DebugScreen()
    object Camera : DebugScreen()
    data class Detail(val snapshotId: Long) : DebugScreen()
    data class Edit(val rawFilePath: String, val snapshotId: Long? = null) : DebugScreen()
}
