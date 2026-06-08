package com.example.helper_application.dialer.feature.contacts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.helper_application.R
import com.example.helper_application.dialer.core.utils.AvatarHelper

class ContactsAdapter(
    private val onCall: (ContactRow) -> Unit
) : ListAdapter<ContactRow, ContactsAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.name)
        private val number: TextView = view.findViewById(R.id.number)
        private val avatar: TextView = view.findViewById(R.id.avatar)
        private val btnCall: ImageButton = view.findViewById(R.id.btnCall)

        fun bind(item: ContactRow) {
            name.text = item.name
            number.text = item.displayNumber
            AvatarHelper.bindInitials(avatar, item.name)
            itemView.setOnClickListener { onCall(item) }
            btnCall.setOnClickListener { onCall(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ContactRow>() {
        override fun areItemsTheSame(a: ContactRow, b: ContactRow): Boolean =
            a.id == b.id && a.number == b.number

        override fun areContentsTheSame(a: ContactRow, b: ContactRow): Boolean = a == b
    }
}
