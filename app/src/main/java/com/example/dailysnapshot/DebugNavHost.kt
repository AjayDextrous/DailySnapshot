package com.example.dailysnapshot

/**
 * TEMPORARY — replaced by AppNavGraph in DAI-15.
 * Provides a minimal navigation scaffold for testing DAI-9 camera capture.
 */

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.dailysnapshot.ui.camera.CameraScreen

@Composable
fun DebugNavHost() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<DebugScreen>(DebugScreen.Home) }
    var lastCapturedPath by remember { mutableStateOf<String?>(null) }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraPermissionGranted = granted }

    when (currentScreen) {
        DebugScreen.Home -> DebugHomeScreen(
            lastCapturedPath = lastCapturedPath,
            onOpenCamera = {
                if (cameraPermissionGranted) {
                    currentScreen = DebugScreen.Camera
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        )
        DebugScreen.Camera -> CameraScreen(
            onPhotoCaptured = { path ->
                lastCapturedPath = path
                currentScreen = DebugScreen.Home
                Toast.makeText(context, "Captured: $path", Toast.LENGTH_SHORT).show()
            },
            onClose = { currentScreen = DebugScreen.Home }
        )
    }
}

@Composable
private fun DebugHomeScreen(
    lastCapturedPath: String?,
    onOpenCamera: () -> Unit
) {
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
                Text("Open Camera (DAI-9)")
            }
            if (lastCapturedPath != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Last capture:\n$lastCapturedPath",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private sealed class DebugScreen {
    object Home : DebugScreen()
    object Camera : DebugScreen()
}
