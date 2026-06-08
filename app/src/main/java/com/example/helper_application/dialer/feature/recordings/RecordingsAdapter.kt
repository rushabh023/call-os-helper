package com.example.helper_application.dialer.feature.recordings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.helper_application.R
import com.google.android.material.button.MaterialButton

class RecordingsAdapter(
    private val playingPath: () -> String?,
    private val onPlay: (String) -> Unit,
    private val onDelete: (RecordingListItem) -> Unit,
    private val onShare: (String) -> Unit
) : RecyclerView.Adapter<RecordingsAdapter.VH>() {

    private val items = mutableListOf<RecordingListItem>()

    fun submit(list: List<RecordingListItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun notifyPlaybackChanged() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.title)
        private val subtitle: TextView = view.findViewById(R.id.subtitle)

        fun bind(item: RecordingListItem) {
            title.text = item.title
            subtitle.text = item.subtitle
            val btnPlay = view.findViewById<MaterialButton>(R.id.btnPlay)
            val isPlaying = playingPath() == item.filePath
            btnPlay.text = view.context.getString(
                if (isPlaying) R.string.stop_playback else R.string.play
            )
            btnPlay.setOnClickListener { onPlay(item.filePath) }
            view.findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener { onDelete(item) }
            view.findViewById<MaterialButton>(R.id.btnShare).setOnClickListener { onShare(item.filePath) }
        }
    }
}
