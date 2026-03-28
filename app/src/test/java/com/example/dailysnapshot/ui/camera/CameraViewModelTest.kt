package com.example.dailysnapshot.ui.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import app.cash.turbine.test
import com.example.dailysnapshot.util.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = mockk(relaxed = true)
    private lateinit var viewModel: CameraViewModel

    @Before
    fun setUp() {
        viewModel = CameraViewModel(context)
    }

    // ── toggleCamera ──────────────────────────────────────────────────────────

    @Test
    fun toggleCamera_initialState_isLensFacingBack() {
        assertEquals(CameraSelector.LENS_FACING_BACK, viewModel.uiState.value.lensFacing)
    }

    @Test
    fun toggleCamera_flipsToFront_onFirstCall() {
        viewModel.toggleCamera()
        assertEquals(CameraSelector.LENS_FACING_FRONT, viewModel.uiState.value.lensFacing)
    }

    @Test
    fun toggleCamera_flipsBackToBack_onSecondCall() {
        viewModel.toggleCamera()
        viewModel.toggleCamera()
        assertEquals(CameraSelector.LENS_FACING_BACK, viewModel.uiState.value.lensFacing)
    }

    @Test
    fun toggleCamera_doesNotChangeCameraStateField() {
        viewModel.toggleCamera()
        assertTrue(viewModel.uiState.value.cameraState is CameraViewModel.CameraState.Active)
    }

    // ── confirmCapture ────────────────────────────────────────────────────────

    @Test
    fun confirmCapture_emitsPhotoCaptured_whenInPostCaptureState() = runTest {
        viewModel.setPostCaptureState("/tmp/test.jpg")

        viewModel.uiEvents.test {
            viewModel.confirmCapture()
            val event = awaitItem()
            assertTrue(event is CameraViewModel.UiEvent.PhotoCaptured)
            assertEquals("/tmp/test.jpg", (event as CameraViewModel.UiEvent.PhotoCaptured).rawFilePath)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmCapture_doesNotEmitEvent_whenInActiveState() = runTest {
        viewModel.uiEvents.test {
            viewModel.confirmCapture() // Active state — should be a no-op
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── retakePhoto ───────────────────────────────────────────────────────────

    @Test
    fun retakePhoto_resetsStateToActive() {
        viewModel.setPostCaptureState("/tmp/test.jpg")
        viewModel.retakePhoto()
        assertTrue(viewModel.uiState.value.cameraState is CameraViewModel.CameraState.Active)
    }

    @Test
    fun retakePhoto_deletesRawFile() {
        val file = File.createTempFile("raw", ".jpg")
        assertTrue(file.exists())

        viewModel.setPostCaptureState(file.absolutePath)
        viewModel.retakePhoto()

        assertFalse(file.exists())
    }

    @Test
    fun retakePhoto_doesNothing_whenInActiveState() {
        viewModel.retakePhoto() // no-op — no exception, state stays Active
        assertTrue(viewModel.uiState.value.cameraState is CameraViewModel.CameraState.Active)
    }

    // ── resetToActive ─────────────────────────────────────────────────────────

    @Test
    fun resetToActive_setsStateToActive() {
        viewModel.setPostCaptureState("/tmp/test.jpg")
        viewModel.resetToActive()
        assertTrue(viewModel.uiState.value.cameraState is CameraViewModel.CameraState.Active)
    }
}
