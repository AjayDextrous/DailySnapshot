package com.example.dailysnapshot.ui.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailysnapshot.data.repository.SnapshotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

@HiltViewModel
class EditViewModel @Inject constructor(
    private val repository: SnapshotRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    enum class FilterId(val id: String, val displayName: String) {
        NONE("none", "None"),
        SEPIA("sepia", "Sepia"),
        FADED("faded", "Faded"),
        NOIR("noir", "Noir"),
        WARM("warm", "Warm"),
        COOL("cool", "Cool");

        /** Returns the 4×5 ColorMatrix float array for this filter, or null for no-op. */
        fun matrixValues(): FloatArray? = when (this) {
            NONE -> null
            SEPIA -> floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            )
            FADED -> floatArrayOf(
                0.7f, 0.1f, 0.1f, 0f, 40f,
                0.1f, 0.7f, 0.1f, 0f, 40f,
                0.1f, 0.1f, 0.7f, 0f, 50f,
                0f,   0f,   0f,   1f, 0f
            )
            NOIR -> floatArrayOf(
                0.419f, 0.822f, 0.160f, 0f, -51f,
                0.419f, 0.822f, 0.160f, 0f, -51f,
                0.419f, 0.822f, 0.160f, 0f, -51f,
                0f,     0f,     0f,     1f, 0f
            )
            WARM -> floatArrayOf(
                1.2f, 0f,   0f,   0f, 10f,
                0f,   1.0f, 0f,   0f, 5f,
                0f,   0f,   0.8f, 0f, -10f,
                0f,   0f,   0f,   1f, 0f
            )
            COOL -> floatArrayOf(
                0.8f, 0f,   0f,   0f, -10f,
                0f,   1.0f, 0f,   0f, 5f,
                0f,   0f,   1.2f, 0f, 15f,
                0f,   0f,   0f,   1f, 0f
            )
        }

        companion object {
            fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: NONE
        }
    }

    data class UiState(
        val previewBitmap: Bitmap? = null,
        val filterThumbnails: Map<String, Bitmap> = emptyMap(),
        val caption: String = "",
        val selectedFilter: FilterId = FilterId.NONE,
        val isExistingEntry: Boolean = false,
        val existingDate: String? = null,
        val isSaving: Boolean = false,
        val showDiscardDialog: Boolean = false
    )

    sealed class UiEvent {
        object NavigateToGallery : UiEvent()
        object NavigateBack : UiEvent()
        data class Error(val message: String) : UiEvent()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    private var rawFilePath: String = ""
    private var snapshotId: Long? = null

    /**
     * Called from the Composable once nav args are known.
     * Idempotent for the same [rawFilePath] + [snapshotId]; performs a full state reset
     * and reload when called with different args (e.g. returning to EditScreen for a new photo).
     */
    fun initialize(rawFilePath: String, snapshotId: Long? = null) {
        if (this.rawFilePath == rawFilePath && this.snapshotId == snapshotId) return
        this.rawFilePath = rawFilePath
        this.snapshotId = snapshotId
        _uiState.update { UiState() }   // clear caption, filter, bitmaps, isSaving
        viewModelScope.launch {
            loadBitmapAndThumbnails()
            if (snapshotId != null) loadExistingSnapshot(snapshotId)
        }
    }

    private suspend fun loadBitmapAndThumbnails() = withContext(Dispatchers.IO) {
        // Decode at up to 2048px to limit memory while keeping preview quality
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(rawFilePath, opts)
        opts.inSampleSize = calculateSampleSize(opts, 2048, 2048)
        opts.inJustDecodeBounds = false

        val raw = BitmapFactory.decodeFile(rawFilePath, opts) ?: return@withContext
        val bitmap = correctExifOrientation(rawFilePath, raw)

        val thumbSize = 80
        val thumb = bitmap.scale(thumbSize, thumbSize)
        val thumbnails = FilterId.entries.associate { filter ->
            filter.id to applyAndroidFilter(thumb, filter)
        }

        _uiState.update { it.copy(previewBitmap = bitmap, filterThumbnails = thumbnails) }
    }

    private fun applyAndroidFilter(src: Bitmap, filter: FilterId): Bitmap {
        val result = createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        val paint = Paint().apply {
            val m = filter.matrixValues()
            if (m != null) colorFilter = ColorMatrixColorFilter(android.graphics.ColorMatrix(m))
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    private fun calculateSampleSize(opts: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
        var size = 1
        val h = opts.outHeight
        val w = opts.outWidth
        if (h > reqH || w > reqW) {
            while ((h / (size * 2)) >= reqH && (w / (size * 2)) >= reqW) size *= 2
        }
        return size
    }

    /**
     * Reads the EXIF orientation tag from [filePath] and rotates [bitmap] accordingly.
     * BitmapFactory.decodeFile ignores EXIF rotation, so this must be applied manually.
     * The original [bitmap] is recycled if a new rotated copy is created.
     */
    private fun correctExifOrientation(filePath: String, bitmap: Bitmap): Bitmap {
        val degrees = try {
            val exif = ExifInterface(filePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (_: Exception) { 0f }

        if (degrees == 0f) return bitmap
        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(degrees) }, true
        )
        bitmap.recycle()
        return rotated
    }

    private suspend fun loadExistingSnapshot(id: Long) {
        val snapshot = repository.getSnapshotById(id) ?: return
        _uiState.update {
            it.copy(
                caption = snapshot.caption,
                selectedFilter = FilterId.fromId(snapshot.filterApplied ?: "none"),
                isExistingEntry = true,
                existingDate = snapshot.date
            )
        }
    }

    fun onCaptionChanged(newCaption: String) {
        if (newCaption.length <= 100) {
            _uiState.update { it.copy(caption = newCaption) }
        }
    }

    fun onFilterSelected(filter: FilterId) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onSaveClicked() {
        val state = _uiState.value
        if (state.isSaving || state.previewBitmap == null) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                // Pass null for "no filter" (repository/ImageProcessor treat null and "none" identically,
                // but null is the canonical DB representation for filterApplied = none).
                val filterId = state.selectedFilter.id.takeUnless { it == FilterId.NONE.id }
                if (snapshotId != null) {
                    repository.updateSnapshot(
                        id      = snapshotId!!,
                        caption = state.caption,
                        filter  = filterId
                    )
                    _uiEvents.send(UiEvent.NavigateBack)
                } else {
                    // Reload full-res bitmap for saving; apply EXIF correction as on preview load
                    val raw = BitmapFactory.decodeFile(rawFilePath)
                    val saveBitmap = if (raw != null) correctExifOrientation(rawFilePath, raw)
                                     else state.previewBitmap
                    repository.saveSnapshot(
                        rawBitmap = saveBitmap,
                        caption   = state.caption,
                        filter    = filterId,
                        date      = today
                    )
                    _uiEvents.send(UiEvent.NavigateToGallery)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _uiEvents.send(UiEvent.Error(e.message ?: "Save failed"))
            }
        }
    }

    fun onDiscardClicked() {
        if (_uiState.value.caption.isNotEmpty()) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            discardAndNavigateBack()
        }
    }

    fun onDiscardDialogDismissed() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }

    fun onDiscardConfirmed() {
        _uiState.update { it.copy(showDiscardDialog = false) }
        discardAndNavigateBack()
    }

    private fun discardAndNavigateBack() {
        // Only delete raw file for new entries; re-edits preserve the original
        if (snapshotId == null) File(rawFilePath).delete()
        viewModelScope.launch { _uiEvents.send(UiEvent.NavigateBack) }
    }
}
