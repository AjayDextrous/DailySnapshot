package com.example.dailysnapshot.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageSavedCallback
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.dailysnapshot.viewmodels.CameraViewModel
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material3.IconButton
import androidx.compose.runtime.rememberCoroutineScope
import com.example.dailysnapshot.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@Composable
fun CameraCaptureComponent(isPermissionsGranted: Boolean) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val executor = remember { Executors.newSingleThreadExecutor() }

    val cameraController: LifecycleCameraController = remember {
        LifecycleCameraController(context).apply {
            bindToLifecycle(lifecycleOwner)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isPermissionsGranted){
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PreviewView(context).apply {
                        scaleType = PreviewView.ScaleType.FILL_START
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        controller = cameraController
                    }
                },
                onRelease = {
                    // Release the camera controller when the composable is removed from the screen
                    cameraController.unbind()
                }

            )
        } else {
            Text("Camera Permissions are not granted.")
        }

        CameraHUDComponent(
            onCaptureClicked = {
                coroutineScope.launch {
                    val result = takePicture(
                        cameraController = cameraController,
                        context = context,
                        executor = executor
                    )
                    result.fold(
                        onSuccess = { uri ->
                            // Image capture successful
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Image saved at $uri", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFailure = { exception ->
                            // Image capture failed
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Image capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        )

    }

}


@Composable
fun CameraHUDComponent(onCaptureClicked: () -> Unit = {}) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        IconButton(
            onClick = onCaptureClicked,
            modifier = Modifier
                .padding(32.dp)
                .size(64.dp)
        ) {
            Icon(
                modifier = Modifier.size(64.dp),
                imageVector = Icons.Outlined.Camera,
                contentDescription = "Camera",
                tint = Color.White
            )
        }
    }
}

@Preview
@Composable
fun CameraCaptureComponentPreview() {
    CameraHUDComponent()
}

private fun createPhotoFile(context: Context): File {
    val mediaDir = context.getExternalFilesDir(null)?.let {
        File(it, context.getString(R.string.app_name)).apply { mkdirs() }
    }
    val outputDirectory = if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    return File(outputDirectory, photoFileName(context)).apply {
        parentFile?.mkdirs()
    }
}

private fun photoFileName(context: Context) =
    SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", context.resources.configuration.locales[0])
        .format(System.currentTimeMillis()) + ".jpg"

suspend fun takePicture(
    cameraController: CameraController,
    context: Context,
    executor: ExecutorService,
): Result<Uri> {
    val photoFile = createPhotoFile(context)
    val outputOptions = OutputFileOptions.Builder(photoFile).build()

    // Instruct the cameraController to take a picture
    return suspendCoroutine { continuation ->
        cameraController.takePicture(
            outputOptions,
            executor,
            object : OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // On successful capture, invoke callback with the Uri of the saved file
                    continuation.resume(Result.success(Uri.fromFile(photoFile)))
                }

                override fun onError(exception: ImageCaptureException) {
                    // On error, invoke the error callback with the encountered exception
                    continuation.resume(Result.failure(exception))
                }
            }
        )
    }
}