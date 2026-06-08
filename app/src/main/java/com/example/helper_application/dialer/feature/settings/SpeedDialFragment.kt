package com.example.helper_application.dialer.feature.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.helper_application.R
import com.example.helper_application.dialer.core.utils.attachVerticalList
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SpeedDialFragment : Fragment() {

    private val viewModel: SpeedDialViewModel by viewModels()
    private lateinit var adapter: SpeedDialAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_speed_dial, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = SpeedDialAdapter(
            onEdit = { slot -> showEditDialog(slot) },
            onClear = { slot -> viewModel.clear(slot) }
        )
        view.findViewById<RecyclerView>(R.id.recycler).apply {
            attachVerticalList()
            adapter = this@SpeedDialFragment.adapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.slots.collect { adapter.submit(it) }
            }
        }
    }

    private fun showEditDialog(slot: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_speed_dial_edit, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.inputName)
        val numberInput = dialogView.findViewById<EditText>(R.id.inputNumber)
        val current = adapter.currentFor(slot)
        nameInput.setText(current?.displayName.orEmpty())
        numberInput.setText(current?.phoneNumber.orEmpty())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.speed_dial_edit_title, slot))
            .setView(dialogView)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                viewModel.save(slot, nameInput.text.toString(), numberInput.text.toString())
            }
            .show()
    }
}

private class SpeedDialAdapter(
    private val onEdit: (Int) -> Unit,
    private val onClear: (Int) -> Unit
) : RecyclerView.Adapter<SpeedDialAdapter.VH>() {

    private val items = mutableListOf<SpeedDialSlotUi>()

    fun submit(list: List<SpeedDialSlotUi>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun currentFor(slot: Int): SpeedDialSlotUi? = items.find { it.slot == slot }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_speed_dial, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val slotLabel: TextView = itemView.findViewById(R.id.slotLabel)
        private val assignment: TextView = itemView.findViewById(R.id.assignment)
        private val btnClear: View = itemView.findViewById(R.id.btnClear)

        fun bind(item: SpeedDialSlotUi) {
            slotLabel.text = item.slot.toString()
            assignment.text = item.phoneNumber?.let { number ->
                val name = item.displayName?.takeIf { it.isNotBlank() && it != number }
                if (name != null) "$name · $number" else number
            } ?: itemView.context.getString(R.string.speed_dial_unassigned)
            itemView.setOnClickListener { onEdit(item.slot) }
            btnClear.setOnClickListener { onClear(item.slot) }
        }
    }
}
