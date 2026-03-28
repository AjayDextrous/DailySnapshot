package com.example.dailysnapshot.ui.camera

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.Executor
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    sealed class CameraState {
        object Active : CameraState()
        data class PostCapture(val rawFilePath: String) : CameraState()
    }

    data class UiState(
        val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        val cameraState: CameraState = CameraState.Active
    )

    sealed class UiEvent {
        data class PhotoCaptured(val rawFilePath: String) : UiEvent()
        data class Error(val message: String) : UiEvent()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    /** Resets to Active state on screen entry, clearing any stale PostCapture state. */
    fun resetToActive() {
        _uiState.update { it.copy(cameraState = CameraState.Active) }
    }

    fun toggleCamera() {
        _uiState.update {
            it.copy(
                lensFacing = if (it.lensFacing == CameraSelector.LENS_FACING_BACK)
                    CameraSelector.LENS_FACING_FRONT
                else
                    CameraSelector.LENS_FACING_BACK
            )
        }
    }

    fun capturePhoto(controller: LifecycleCameraController, executor: Executor) {
        val outputFile = File(context.filesDir, "snapshots/raw/${UUID.randomUUID()}.jpg")
            .also { it.parentFile?.mkdirs() }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        controller.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    _uiState.update { it.copy(cameraState = CameraState.PostCapture(outputFile.absolutePath)) }
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModelScope.launch {
                        _uiEvents.send(UiEvent.Error(exception.message ?: "Capture failed"))
                    }
                }
            }
        )
    }

    /** User confirmed the captured photo — navigate to Edit screen. */
    fun confirmCapture() {
        val state = _uiState.value.cameraState as? CameraState.PostCapture ?: return
        viewModelScope.launch {
            _uiEvents.send(UiEvent.PhotoCaptured(state.rawFilePath))
        }
    }

    /** Test-only: drives the ViewModel into PostCapture state without a real camera controller. */
    @VisibleForTesting
    internal fun setPostCaptureState(path: String) {
        _uiState.update { it.copy(cameraState = CameraState.PostCapture(path)) }
    }

    /** User chose to retake — discard the captured file and return to live preview. */
    fun retakePhoto() {
        val state = _uiState.value.cameraState as? CameraState.PostCapture ?: return
        File(state.rawFilePath).delete()
        _uiState.update { it.copy(cameraState = CameraState.Active) }
    }
}
