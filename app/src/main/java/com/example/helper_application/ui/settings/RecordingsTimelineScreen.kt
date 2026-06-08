package com.example.helper_application.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.helper_application.R
import com.example.helper_application.recording.RecordingStorage
import com.example.helper_application.ui.playback.RecordingPlayerController
import com.example.helper_application.ui.theme.CubeDashboardBackground
import com.example.helper_application.ui.theme.CubePinkAccent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingsTimelineScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var playerTick by remember { mutableIntStateOf(0) }
    val player = remember {
        RecordingPlayerController(context).apply {
            onStateChanged = { playerTick++ }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    LaunchedEffect(refreshTick) {
        RecordingStorage.ensureFolderExists(context)
        files = RecordingStorage.listSavedRecordings()
    }

    @Suppress("UNUSED_VARIABLE")
    val playingState = playerTick

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CubeDashboardBackground)
    ) {
        if (files.isEmpty()) {
            Text(
                text = stringResource(R.string.timeline_empty),
                modifier = Modifier.padding(24.dp),
                color = Color.Gray,
                fontSize = 15.sp
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(files, key = { it.absolutePath }) { file ->
                    TimelineItem(
                        file = file,
                        isPlaying = player.isPlaying(file.absolutePath),
                        onPlayClick = { player.toggle(file) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(
    file: File,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    val date = SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault())
        .format(Date(file.lastModified()))
    val sizeKb = (file.length() / 1024).coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPlayClick) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying) R.string.stop_playback else R.string.play
                ),
                tint = CubePinkAccent
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = file.nameWithoutExtension, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = date, fontSize = 13.sp, color = Color.Gray)
            Text(text = "${file.extension.uppercase()} · ${sizeKb} KB", fontSize = 12.sp, color = Color.Gray)
        }
        if (isPlaying) {
            Text(
                text = stringResource(R.string.timeline_now_playing),
                fontSize = 12.sp,
                color = CubePinkAccent,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}
