package com.example.helper_application.dialer.feature.calllog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.helper_application.R
import com.example.helper_application.dialer.core.utils.DialerCallPlacer
import com.example.helper_application.dialer.core.utils.DialerTabPermissions
import com.example.helper_application.dialer.core.utils.addTextChangedListenerSimple
import com.example.helper_application.dialer.core.utils.attachVerticalList
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CallLogFragment : Fragment() {

    private val viewModel: CallLogViewModel by viewModels()
    private lateinit var adapter: CallLogAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var emptyView: View
    private lateinit var permissionBanner: View
    private lateinit var chipGroup: ChipGroup

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refresh() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_call_log, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recycler = view.findViewById(R.id.recycler)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        emptyView = view.findViewById(R.id.emptyView)
        permissionBanner = view.findViewById(R.id.permissionBanner)
        chipGroup = view.findViewById(R.id.chipGroup)
        val searchInput: EditText = view.findViewById(R.id.searchInput)
        val emptyTitle: android.widget.TextView = view.findViewById(R.id.emptyTitle)
        val emptySubtitle: android.widget.TextView = view.findViewById(R.id.emptySubtitle)

        adapter = CallLogAdapter { entry ->
            DialerCallPlacer.placeCall(requireContext(), entry.number)
        }
        recycler.attachVerticalList()
        recycler.adapter = adapter

        swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipMissed -> CallLogFilter.MISSED
                R.id.chipIncoming -> CallLogFilter.INCOMING
                R.id.chipOutgoing -> CallLogFilter.OUTGOING
                else -> CallLogFilter.ALL
            }
            viewModel.setFilter(filter)
        }
        searchInput.addTextChangedListenerSimple { viewModel.search(it) }

        view.findViewById<MaterialButton>(R.id.btnGrantCallLog).setOnClickListener {
            permissionLauncher.launch(DialerTabPermissions.callLogPermissions)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submitList(state.entries)
                    swipeRefresh.isRefreshing = state.loading
                    permissionBanner.visibility = if (state.permissionGranted) View.GONE else View.VISIBLE
                    val showEmpty = state.permissionGranted && state.entries.isEmpty() && !state.loading
                    emptyView.visibility = if (showEmpty) View.VISIBLE else View.GONE
                    recycler.visibility = if (state.permissionGranted && state.entries.isNotEmpty()) {
                        View.VISIBLE
                    } else if (!state.permissionGranted) {
                        View.GONE
                    } else {
                        View.GONE
                    }
                    if (state.permissionGranted && state.entries.isNotEmpty()) {
                        recycler.visibility = View.VISIBLE
                    }
                    emptyTitle.text = if (state.searchQuery.isNotBlank()) {
                        getString(R.string.no_search_results)
                    } else {
                        getString(R.string.no_calls)
                    }
                    emptySubtitle.text = getString(R.string.no_calls_hint)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
