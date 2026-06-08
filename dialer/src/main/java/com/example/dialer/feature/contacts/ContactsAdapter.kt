package com.example.dialer.feature.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dialer.databinding.ItemContactBinding

class ContactsAdapter : RecyclerView.Adapter<ContactsAdapter.VH>() {
    private val items = mutableListOf<ContactRow>()

    fun submit(list: List<ContactRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    class VH(private val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContactRow) {
            binding.name.text = item.name
            binding.number.text = item.number
        }
    }
}
