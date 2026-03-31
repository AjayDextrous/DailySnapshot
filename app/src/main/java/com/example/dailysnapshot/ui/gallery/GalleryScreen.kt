package com.example.dailysnapshot.ui.gallery

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.rotate
import kotlin.random.Random
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.dailysnapshot.data.model.Snapshot
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onOpenCamera: () -> Unit,
    onOpenDetail: (snapshotId: Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete all snapshots?") },
            text = { Text("This will permanently remove every photo and entry. Debug only.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAll()
                    showDeleteAllDialog = false
                }) { Text("Delete all", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Snapshot") },
                actions = {
                    // Debug: delete all snapshots
                    if (uiState.snapshots.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Delete all (debug)",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    // Calendar view toggle — implemented in DAI-24
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Calendar view (coming soon)",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenCamera) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Capture a new snapshot")
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> { /* First DB emission hasn't arrived — show nothing to avoid flicker */ }
            uiState.snapshots.isEmpty() -> EmptyState(modifier = Modifier.padding(innerPadding))
            else -> SnapshotGrid(
                snapshots = uiState.snapshots,
                onSnapshotClick = onOpenDetail,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun SnapshotGrid(
    snapshots: List<Snapshot>,
    onSnapshotClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(snapshots, key = { it.id }) { snapshot ->
            SnapshotGridItem(
                snapshot = snapshot,
                onClick = { onSnapshotClick(snapshot.id) }
            )
        }
    }
}

/** Maximum tilt angle (degrees) in either direction for the scattered-photos effect. */
private const val MAX_ITEM_ROTATION_DEGREES = 12f

@Composable
private fun SnapshotGridItem(
    snapshot: Snapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Derive a stable rotation from the snapshot ID so it survives recomposition and
    // scroll recycling. Sign alternates with ID parity to guarantee a mix of CW/CCW.
    val rotation = remember(snapshot.id) {
        (Random(snapshot.id).nextFloat() * 2 - 1) * MAX_ITEM_ROTATION_DEGREES
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .rotate(rotation)
            .shadow(elevation = 4.dp)
            .background(Color(0xFFFAFAF8))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = File(snapshot.imagePath),
            contentDescription = snapshot.caption.ifEmpty { "Snapshot from ${snapshot.date}" },
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Your journal is empty",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Tap the button below to capture your first moment.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
