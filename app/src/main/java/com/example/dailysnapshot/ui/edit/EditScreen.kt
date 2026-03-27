package com.example.dailysnapshot.ui.edit

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    rawFilePath: String,
    snapshotId: Long? = null,
    onSaved: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EditViewModel = hiltViewModel()
) {
    LaunchedEffect(rawFilePath) {
        viewModel.initialize(rawFilePath, snapshotId)
    }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is EditViewModel.UiEvent.NavigateToGallery -> onSaved()
                is EditViewModel.UiEvent.NavigateBack -> onNavigateBack()
                is EditViewModel.UiEvent.Error -> { /* TODO DAI-15: show snackbar */ }
            }
        }
    }

    if (uiState.showDiscardDialog) {
        DiscardConfirmationDialog(
            onConfirm = viewModel::onDiscardConfirmed,
            onDismiss = viewModel::onDiscardDialogDismissed
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (uiState.isExistingEntry) uiState.existingDate ?: "Edit Snapshot"
                        else "New Snapshot"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::onDiscardClicked) {
                        Icon(Icons.Default.Close, contentDescription = "Discard")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::onSaveClicked,
                        enabled = !uiState.isSaving && uiState.previewBitmap != null
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PolaroidPreview(
                bitmap = uiState.previewBitmap,
                caption = uiState.caption,
                filter = uiState.selectedFilter,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(0.82f)
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = uiState.caption,
                onValueChange = viewModel::onCaptionChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Caption") },
                placeholder = { Text("Add a caption…") },
                singleLine = false,
                maxLines = 3,
                supportingText = { Text("${uiState.caption.length}/100") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { })
            )

            Spacer(Modifier.height(20.dp))

            FilterStrip(
                thumbnails = uiState.filterThumbnails,
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = viewModel::onFilterSelected
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Live Polaroid frame preview.
 *
 * The white frame is split into:
 *   - a photo area (top ~78%, equal side/top padding)
 *   - a caption margin (bottom ~22%, wider — classic Polaroid proportions)
 *
 * The caption text is a Compose Text composable layered over the bottom margin,
 * updating in real-time as the user types. A [ColorFilter] is applied to the
 * image for filter preview.
 */
@Composable
private fun PolaroidPreview(
    bitmap: Bitmap?,
    caption: String,
    filter: EditViewModel.FilterId,
    modifier: Modifier = Modifier
) {
    val composeColorFilter = remember(filter) { filter.toComposeColorFilter() }

    Box(
        modifier = modifier
            .shadow(elevation = 8.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

            Column(modifier = Modifier.fillMaxSize()) {
                // Photo (occupies ~78% of frame height)
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Photo preview",
                    contentScale = ContentScale.Crop,
                    colorFilter = composeColorFilter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)
                )

                // Caption margin (~22% of frame height — wider bottom of Polaroid)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.28f),
                    contentAlignment = Alignment.Center
                ) {
                    if (caption.isNotEmpty()) {
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun FilterStrip(
    thumbnails: Map<String, Bitmap>,
    selectedFilter: EditViewModel.FilterId,
    onFilterSelected: (EditViewModel.FilterId) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Filter",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(EditViewModel.FilterId.entries) { filter ->
                FilterChipItem(
                    filter = filter,
                    thumbnail = thumbnails[filter.id],
                    isSelected = filter == selectedFilter,
                    onClick = { onFilterSelected(filter) }
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    filter: EditViewModel.FilterId,
    thumbnail: Bitmap?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isSelected) 2.5.dp else 1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = filter.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = filter.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DiscardConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard photo?") },
        text = { Text("Your caption will be lost and the photo discarded.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Discard") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep editing") }
        }
    )
}

/** Maps [EditViewModel.FilterId] to a Compose [ColorFilter] for live preview rendering. */
private fun EditViewModel.FilterId.toComposeColorFilter(): ColorFilter? {
    val m = matrixValues() ?: return null
    return ColorFilter.colorMatrix(ColorMatrix(m))
}
