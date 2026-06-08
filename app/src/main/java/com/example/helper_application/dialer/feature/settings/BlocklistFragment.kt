package com.example.helper_application.dialer.feature.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.helper_application.R
import com.example.helper_application.dialer.core.utils.attachVerticalList
import com.example.helper_application.dialer.core.data.local.entity.BlockedNumberEntity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BlocklistFragment : Fragment() {

    private val viewModel: BlocklistViewModel by viewModels()
    private lateinit var adapter: BlocklistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_blocklist, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = BlocklistAdapter { viewModel.remove(it) }
        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        val emptyView = view.findViewById<View>(R.id.emptyView)
        recycler.attachVerticalList()
        recycler.adapter = adapter

        view.findViewById<MaterialButton>(R.id.btnAdd).setOnClickListener { showAddDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.blocked.collect { list ->
                    adapter.submit(list)
                    emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    recycler.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_blocklist_add, null)
        val numberInput = dialogView.findViewById<EditText>(R.id.inputNumber)
        val labelInput = dialogView.findViewById<EditText>(R.id.inputLabel)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.blocklist_add_title)
            .setView(dialogView)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                viewModel.add(numberInput.text.toString(), labelInput.text.toString())
            }
            .show()
    }
}

private class BlocklistAdapter(
    private val onRemove: (BlockedNumberEntity) -> Unit
) : RecyclerView.Adapter<BlocklistAdapter.VH>() {

    private val items = mutableListOf<BlockedNumberEntity>()

    fun submit(list: List<BlockedNumberEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blocked_number, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val number: android.widget.TextView = itemView.findViewById(R.id.number)
        private val label: android.widget.TextView = itemView.findViewById(R.id.label)
        private val btnRemove: View = itemView.findViewById(R.id.btnRemove)

        fun bind(item: BlockedNumberEntity) {
            number.text = item.number
            label.text = item.label?.takeIf { it.isNotBlank() } ?: item.pattern.name
            btnRemove.setOnClickListener { onRemove(item) }
        }
    }
}
