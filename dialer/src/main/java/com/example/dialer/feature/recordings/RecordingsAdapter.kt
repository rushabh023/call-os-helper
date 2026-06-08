package com.example.dialer.feature.recordings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dialer.core.data.local.entity.CallRecordingEntity
import com.example.dialer.databinding.ItemRecordingBinding
import java.text.DateFormat

class RecordingsAdapter(
    private val onPlay: (String) -> Unit,
    private val onDelete: (CallRecordingEntity) -> Unit,
    private val onShare: (String) -> Unit
) : RecyclerView.Adapter<RecordingsAdapter.VH>() {

    private val items = mutableListOf<CallRecordingEntity>()

    fun submit(list: List<CallRecordingEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecordingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val binding: ItemRecordingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CallRecordingEntity) {
            binding.title.text = item.contactName ?: item.displayName
            val whenText = DateFormat.getDateTimeInstance().format(item.createdAt)
            binding.subtitle.text = "$whenText · ${item.durationMs / 1000}s · ${item.audioSourceLabel}"
            binding.btnPlay.setOnClickListener { onPlay(item.filePath) }
            binding.btnDelete.setOnClickListener { onDelete(item) }
            binding.btnShare.setOnClickListener { onShare(item.filePath) }
        }
    }
}
