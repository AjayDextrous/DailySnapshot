package com.example.dailysnapshot.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailysnapshot.data.model.Snapshot
import com.example.dailysnapshot.data.repository.SnapshotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: SnapshotRepository
) : ViewModel() {

    data class GalleryUiState(
        val snapshots: List<Snapshot> = emptyList(),
        val isLoading: Boolean = true
    )

    /** Debug only — removes all snapshots and their image files. */
    fun deleteAll() {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteAllSnapshots() }
    }

    val uiState: StateFlow<GalleryUiState> = repository.getAllSnapshots()
        .map { GalleryUiState(snapshots = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GalleryUiState()
        )
}
