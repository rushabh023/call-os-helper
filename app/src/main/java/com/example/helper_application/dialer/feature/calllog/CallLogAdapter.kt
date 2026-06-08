package com.example.helper_application.dialer.feature.calllog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.helper_application.R
import com.example.helper_application.dialer.core.utils.AvatarHelper

class CallLogAdapter(
    private val onCall: (CallLogEntry) -> Unit
) : ListAdapter<CallLogEntry, CallLogAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call_log, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.name)
        private val meta: TextView = view.findViewById(R.id.meta)
        private val number: TextView = view.findViewById(R.id.number)
        private val avatar: TextView = view.findViewById(R.id.avatar)
        private val callTypeIcon: ImageView = view.findViewById(R.id.callTypeIcon)
        private val btnCall: ImageButton = view.findViewById(R.id.btnCall)

        fun bind(item: CallLogEntry) {
            name.text = item.name
            meta.text = item.metaText
            number.text = item.number
            AvatarHelper.bindInitials(avatar, item.name)
            val ctx = itemView.context
            when (item.typeLabel) {
                "Missed" -> {
                    callTypeIcon.setImageResource(R.drawable.ic_call_missed)
                    callTypeIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.dialer_missed))
                    name.setTextColor(ContextCompat.getColor(ctx, R.color.dialer_missed))
                }
                "Incoming" -> {
                    callTypeIcon.scaleX = 1f
                    callTypeIcon.setImageResource(R.drawable.ic_call_incoming)
                    callTypeIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.dialer_incoming))
                    name.setTextColor(ContextCompat.getColor(ctx, R.color.dialer_on_surface))
                }
                "Outgoing" -> {
                    callTypeIcon.setImageResource(R.drawable.ic_call_incoming)
                    callTypeIcon.scaleX = -1f
                    callTypeIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.dialer_outgoing))
                    name.setTextColor(ContextCompat.getColor(ctx, R.color.dialer_on_surface))
                }
                else -> {
                    callTypeIcon.setImageResource(R.drawable.ic_call_filled)
                    callTypeIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.dialer_on_surface_variant))
                    name.setTextColor(ContextCompat.getColor(ctx, R.color.dialer_on_surface))
                }
            }
            itemView.setOnClickListener { onCall(item) }
            btnCall.setOnClickListener { onCall(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<CallLogEntry>() {
        override fun areItemsTheSame(a: CallLogEntry, b: CallLogEntry): Boolean = a.id == b.id
        override fun areContentsTheSame(a: CallLogEntry, b: CallLogEntry): Boolean = a == b
    }
}
