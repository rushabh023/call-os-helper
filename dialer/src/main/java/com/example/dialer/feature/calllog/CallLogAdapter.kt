package com.example.dialer.feature.calllog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dialer.databinding.ItemCallLogBinding

class CallLogAdapter : RecyclerView.Adapter<CallLogAdapter.VH>() {
    private val items = mutableListOf<CallLogEntry>()

    fun submit(list: List<CallLogEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCallLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    class VH(private val binding: ItemCallLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CallLogEntry) {
            binding.name.text = item.name
            binding.meta.text = "${item.typeLabel} · ${item.whenText} · ${item.durationSec}s"
            binding.number.text = item.number
        }
    }
}
