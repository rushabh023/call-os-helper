package com.example.dialer.feature.calllog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dialer.core.utils.addTextChangedListenerSimple
import com.example.dialer.databinding.FragmentCallLogBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CallLogFragment : Fragment() {

    private var _binding: FragmentCallLogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CallLogViewModel by viewModels()
    private lateinit var adapter: CallLogAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCallLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = CallLogAdapter()
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.chipAll.setOnClickListener { viewModel.setFilter(CallLogFilter.ALL) }
        binding.chipMissed.setOnClickListener { viewModel.setFilter(CallLogFilter.MISSED) }
        binding.chipIncoming.setOnClickListener { viewModel.setFilter(CallLogFilter.INCOMING) }
        binding.chipOutgoing.setOnClickListener { viewModel.setFilter(CallLogFilter.OUTGOING) }
        binding.searchInput.addTextChangedListenerSimple { viewModel.search(it) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submit(state.entries)
                    binding.swipeRefresh.isRefreshing = state.loading
                    binding.emptyView.visibility = if (state.entries.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        viewModel.refresh()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
