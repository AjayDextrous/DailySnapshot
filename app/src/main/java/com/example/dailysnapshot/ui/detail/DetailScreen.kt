package com.example.dailysnapshot.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    snapshotId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (snapshotId: Long, rawFilePath: String) -> Unit,
    onNavigateToGallery: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    LaunchedEffect(snapshotId) { viewModel.initialize(snapshotId) }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is DetailViewModel.UiEvent.NavigateToGallery -> onNavigateToGallery()
                is DetailViewModel.UiEvent.NavigateToEdit ->
                    onNavigateToEdit(event.snapshotId, event.rawFilePath)
                is DetailViewModel.UiEvent.ShowDeleteSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Entry deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    when (result) {
                        SnackbarResult.ActionPerformed -> viewModel.undoDelete()
                        SnackbarResult.Dismissed -> viewModel.confirmDelete()
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Share — stub for DAI-30
                    IconButton(onClick = { }, enabled = false) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share (coming soon)",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    // Edit
                    IconButton(
                        onClick = viewModel::onEditClicked,
                        enabled = uiState.snapshot?.rawImagePath != null
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    // Overflow: Delete
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.onDeleteClicked()
                                },
                                enabled = uiState.snapshot != null
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> { /* Avoid flicker on first emission */ }
            uiState.snapshot == null -> { /* Entry not found or already deleted */ }
            else -> {
                val snapshot = uiState.snapshot!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AsyncImage(
                        model = File(snapshot.imagePath),
                        contentDescription = snapshot.caption.ifEmpty {
                            "Snapshot from ${snapshot.date}"
                        },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = formatDate(snapshot.date),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )

                    if (uiState.sameDayCount > 1) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${uiState.sameDayPosition} of ${uiState.sameDayCount} on this day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(isoDate: String): String = try {
    LocalDate.parse(isoDate)
        .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
} catch (_: Exception) {
    isoDate
}
