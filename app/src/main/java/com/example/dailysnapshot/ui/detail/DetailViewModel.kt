package com.example.dailysnapshot.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailysnapshot.data.model.Snapshot
import com.example.dailysnapshot.data.repository.SnapshotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: SnapshotRepository
) : ViewModel() {

    data class UiState(
        val snapshot: Snapshot? = null,
        val sameDayCount: Int = 1,
        val sameDayPosition: Int = 1,
        val isLoading: Boolean = true
    )

    sealed class UiEvent {
        object NavigateToGallery : UiEvent()
        data class NavigateToEdit(val snapshotId: Long, val rawFilePath: String) : UiEvent()
        object ShowDeleteSnackbar : UiEvent()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    private var loadedSnapshotId: Long? = null
    private var deletedSnapshot: Snapshot? = null

    /**
     * Idempotent for the same [snapshotId]; reloads when called with a different id
     * (e.g. after an undo re-insert or navigating to a different detail).
     */
    fun initialize(snapshotId: Long) {
        if (loadedSnapshotId == snapshotId) return
        loadedSnapshotId = snapshotId
        viewModelScope.launch { loadSnapshot(snapshotId) }
    }

    private suspend fun loadSnapshot(id: Long) {
        val snapshot = repository.getSnapshotById(id) ?: run {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        val sameDay = repository.getSnapshotsByDate(snapshot.date).first()
        val position = sameDay.indexOfFirst { it.id == id }.coerceAtLeast(0) + 1
        _uiState.update {
            it.copy(
                snapshot = snapshot,
                sameDayCount = sameDay.size,
                sameDayPosition = position,
                isLoading = false
            )
        }
    }

    fun onEditClicked() {
        val snapshot = _uiState.value.snapshot ?: return
        val rawPath = snapshot.rawImagePath ?: return
        viewModelScope.launch {
            _uiEvents.send(UiEvent.NavigateToEdit(snapshot.id, rawPath))
        }
    }

    /** Stub — implemented in DAI-29/DAI-30. */
    fun onShareClicked() = Unit

    fun onDeleteClicked() {
        val snapshot = _uiState.value.snapshot ?: return
        viewModelScope.launch(Dispatchers.IO) {
            deletedSnapshot = snapshot
            repository.softDeleteSnapshot(snapshot)
            _uiEvents.send(UiEvent.ShowDeleteSnackbar)
        }
    }

    /** Called when the user taps Undo on the delete Snackbar. */
    fun undoDelete() {
        val snapshot = deletedSnapshot ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.undoDeleteSnapshot(snapshot)
            deletedSnapshot = null
            // Restore state from in-memory copy (no DB round-trip needed).
            _uiState.update { it.copy(snapshot = snapshot) }
        }
    }

    /** Called when the delete Snackbar times out or is dismissed without Undo. */
    fun confirmDelete() {
        val snapshot = deletedSnapshot ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSnapshotFiles(snapshot)
            deletedSnapshot = null
            _uiEvents.send(UiEvent.NavigateToGallery)
        }
    }
}
