package com.example.dailysnapshot.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraViewModel: ViewModel() {

    val _isCameraPermissionsGranted = MutableStateFlow(false)
    val isCameraPermissionsGranted = _isCameraPermissionsGranted.asStateFlow()

    fun onCameraPermissionsGranted() {
        _isCameraPermissionsGranted.value = true
    }

    fun onCameraPermissionsDenied() {
        _isCameraPermissionsGranted.value = false
    }

}