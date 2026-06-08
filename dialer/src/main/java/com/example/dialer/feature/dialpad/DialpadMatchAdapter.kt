package com.example.dialer.feature.dialpad

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dialer.R
import com.example.dialer.core.domain.model.ContactMatch
import com.example.dialer.databinding.ItemDialpadMatchBinding

class DialpadMatchAdapter(
    private val onClick: (ContactMatch) -> Unit
) : RecyclerView.Adapter<DialpadMatchAdapter.VH>() {

    private val items = mutableListOf<ContactMatch>()

    fun submit(list: List<ContactMatch>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDialpadMatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val binding: ItemDialpadMatchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContactMatch) {
            binding.name.text = item.displayName
            binding.number.text = item.phoneNumber
            Glide.with(binding.avatar)
                .load(item.photoUri)
                .circleCrop()
                .placeholder(R.drawable.bg_avatar_placeholder)
                .into(binding.avatar)
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
