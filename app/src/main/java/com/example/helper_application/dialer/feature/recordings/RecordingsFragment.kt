package com.example.helper_application.dialer.feature.recordings

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.helper_application.R
import com.example.helper_application.dialer.core.utils.attachVerticalList
import com.example.helper_application.util.AppLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class RecordingsFragment : Fragment() {

    private val viewModel: RecordingsViewModel by viewModels()
    private var player: MediaPlayer? = null
    private var playingPath: String? = null
    private lateinit var adapter: RecordingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_recordings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = RecordingsAdapter(
            playingPath = { playingPath },
            onPlay = { path -> togglePlayback(path) },
            onDelete = { item ->
                stopPlayback()
                viewModel.delete(item)
            },
            onShare = { path -> shareRecording(path) }
        )
        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        val emptyView = view.findViewById<View>(R.id.emptyView)
        val folderPath = view.findViewById<android.widget.TextView>(R.id.folderPath)
        recycler.attachVerticalList()
        recycler.adapter = adapter
        folderPath.text = viewModel.folderPath

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recordings.collect { list ->
                    adapter.submit(list)
                    emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    recycler.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshLegacyScan()
    }

    private fun togglePlayback(path: String) {
        if (playingPath == path && player?.isPlaying == true) {
            stopPlayback()
            adapter.notifyPlaybackChanged()
            return
        }
        play(path)
    }

    private fun play(path: String) {
        stopPlayback()
        val file = File(path)
        if (!file.exists() || file.length() <= 0L) {
            AppLog.w("Playback failed: missing or empty file at $path")
            Toast.makeText(requireContext(), R.string.playback_failed, Toast.LENGTH_SHORT).show()
            viewModel.refreshLegacyScan()
            return
        }
        try {
            player = MediaPlayer().apply {
                setOnErrorListener { _, what, extra ->
                    AppLog.w("MediaPlayer error what=$what extra=$extra path=$path")
                    Toast.makeText(requireContext(), R.string.playback_failed, Toast.LENGTH_SHORT).show()
                    stopPlayback()
                    adapter.notifyPlaybackChanged()
                    true
                }
                setOnCompletionListener {
                    stopPlayback()
                    adapter.notifyPlaybackChanged()
                }
                setDataSource(path)
                setOnPreparedListener {
                    start()
                    playingPath = path
                    adapter.notifyPlaybackChanged()
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            AppLog.e("Playback prepare failed for $path", e)
            Toast.makeText(requireContext(), R.string.playback_failed, Toast.LENGTH_SHORT).show()
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        player?.runCatching {
            if (isPlaying) stop()
            release()
        }
        player = null
        playingPath = null
    }

    private fun shareRecording(path: String) {
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(requireContext(), R.string.playback_failed, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        val ext = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "audio/*"
        startActivity(
            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mime
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }

    override fun onPause() {
        stopPlayback()
        adapter.notifyPlaybackChanged()
        super.onPause()
    }

    override fun onDestroyView() {
        stopPlayback()
        super.onDestroyView()
    }
}
