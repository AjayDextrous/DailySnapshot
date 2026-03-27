package com.example.dailysnapshot.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import java.io.File

private enum class CameraPermission { ShowRationale, Denied, Granted }

@Composable
fun CameraScreen(
    onPhotoCaptured: (rawFilePath: String) -> Unit,
    onClose: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    var permissionState by remember {
        mutableStateOf(
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) CameraPermission.Granted else CameraPermission.ShowRationale
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionState = if (granted) CameraPermission.Granted else CameraPermission.Denied
    }

    when (permissionState) {
        CameraPermission.ShowRationale -> PermissionRationaleScreen(
            onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onClose = onClose
        )
        CameraPermission.Denied -> PermissionDeniedScreen(
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                )
            },
            onClose = onClose
        )
        CameraPermission.Granted -> ActiveCameraContent(
            viewModel = viewModel,
            onPhotoCaptured = onPhotoCaptured,
            onClose = onClose
        )
    }
}

@Composable
private fun ActiveCameraContent(
    viewModel: CameraViewModel,
    onPhotoCaptured: (rawFilePath: String) -> Unit,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            bindToLifecycle(lifecycleOwner)
        }
    }

    // Sync lens facing
    LaunchedEffect(uiState.lensFacing) {
        controller.cameraSelector = if (uiState.lensFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.DEFAULT_FRONT_CAMERA
        else
            CameraSelector.DEFAULT_BACK_CAMERA
    }

    // Collect navigation events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is CameraViewModel.UiEvent.PhotoCaptured -> onPhotoCaptured(event.rawFilePath)
                is CameraViewModel.UiEvent.Error -> { /* TODO: show snackbar */ }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Live camera preview — always rendered so camera keeps running during post-capture
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    this.controller = controller
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Bottom controls — only shown in Active state
        if (uiState.cameraState is CameraViewModel.CameraState.Active) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Capture button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            viewModel.capturePhoto(
                                controller = controller,
                                executor = ContextCompat.getMainExecutor(context)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(64.dp).background(Color.White, CircleShape))
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconButton(onClick = viewModel::toggleCamera) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Switch camera",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // Post-capture overlay — shown on top of live preview
        if (uiState.cameraState is CameraViewModel.CameraState.PostCapture) {
            PostCaptureOverlay(
                rawFilePath = (uiState.cameraState as CameraViewModel.CameraState.PostCapture).rawFilePath,
                onUsePhoto = viewModel::confirmCapture,
                onRetake = viewModel::retakePhoto
            )
        }
    }
}

@Composable
private fun PostCaptureOverlay(
    rawFilePath: String,
    onUsePhoto: () -> Unit,
    onRetake: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Full-screen captured image
        AsyncImage(
            model = File(rawFilePath),
            contentDescription = "Captured photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Action buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = onRetake) {
                Text("Retake", color = Color.White)
            }
            Button(onClick = onUsePhoto) {
                Text("Use this photo")
            }
        }
    }
}

@Composable
private fun PermissionRationaleScreen(onGrant: () -> Unit, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Camera Access Needed", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Daily Snapshot uses the camera to capture your daily photo. " +
                        "Your photos are stored privately on your device.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) {
                Text("Grant Camera Access")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Not Now")
            }
        }
    }
}

@Composable
private fun PermissionDeniedScreen(onOpenSettings: () -> Unit, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Camera Access Denied", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "To capture snapshots, please enable camera access for Daily Snapshot in your device settings.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Open Settings")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Go Back")
            }
        }
    }
}
