package com.example.helper_application.dialer.feature.dialpad

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.helper_application.R
import com.example.helper_application.dialer.core.domain.model.ContactMatch
import com.example.helper_application.dialer.core.utils.AvatarHelper

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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dialpad_match, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.name)
        private val number: TextView = view.findViewById(R.id.number)
        private val avatar: TextView = view.findViewById(R.id.avatar)

        fun bind(item: ContactMatch) {
            name.text = item.displayName
            number.text = item.phoneNumber
            AvatarHelper.bindInitials(avatar, item.displayName)
            view.setOnClickListener { onClick(item) }
        }
    }
}
