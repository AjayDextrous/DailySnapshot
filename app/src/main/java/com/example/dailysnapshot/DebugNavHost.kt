package com.example.dailysnapshot

/**
 * TEMPORARY — replaced by AppNavGraph in DAI-15.
 * Provides a minimal navigation scaffold for manual testing.
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dailysnapshot.ui.camera.CameraScreen
import com.example.dailysnapshot.ui.edit.EditScreen

@Composable
fun DebugNavHost() {
    var currentScreen by remember { mutableStateOf<DebugScreen>(DebugScreen.Home) }

    when (val screen = currentScreen) {
        DebugScreen.Home -> DebugHomeScreen(
            onOpenCamera = { currentScreen = DebugScreen.Camera }
        )
        DebugScreen.Camera -> CameraScreen(
            onPhotoCaptured = { path -> currentScreen = DebugScreen.Edit(path) },
            onClose = { currentScreen = DebugScreen.Home }
        )
        is DebugScreen.Edit -> EditScreen(
            rawFilePath = screen.rawFilePath,
            onSaved = { currentScreen = DebugScreen.Home },
            onNavigateBack = { currentScreen = DebugScreen.Home }
        )
    }
}

@Composable
private fun DebugHomeScreen(onOpenCamera: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Daily Snapshot — Debug", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onOpenCamera) {
                Text("Open Camera (DAI-10)")
            }
        }
    }
}

private sealed class DebugScreen {
    object Home : DebugScreen()
    object Camera : DebugScreen()
    data class Edit(val rawFilePath: String) : DebugScreen()
}
