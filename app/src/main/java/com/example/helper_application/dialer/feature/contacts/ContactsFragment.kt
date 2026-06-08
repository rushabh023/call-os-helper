package com.example.helper_application.dialer.feature.contacts

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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ContactsFragment : Fragment() {

    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var adapter: ContactsAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var emptyView: View
    private lateinit var permissionBanner: View

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refresh() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_contacts, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recycler = view.findViewById(R.id.recycler)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        emptyView = view.findViewById(R.id.emptyView)
        permissionBanner = view.findViewById(R.id.permissionBanner)
        val searchInput: EditText = view.findViewById(R.id.searchInput)
        val emptyTitle: android.widget.TextView = view.findViewById(R.id.emptyTitle)
        val emptySubtitle: android.widget.TextView = view.findViewById(R.id.emptySubtitle)

        adapter = ContactsAdapter { row ->
            DialerCallPlacer.placeCall(requireContext(), row.number)
        }
        recycler.attachVerticalList()
        recycler.adapter = adapter

        swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        searchInput.addTextChangedListenerSimple { viewModel.search(it) }

        view.findViewById<MaterialButton>(R.id.btnGrantContacts).setOnClickListener {
            permissionLauncher.launch(DialerTabPermissions.contactsPermissions)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submitList(state.contacts)
                    swipeRefresh.isRefreshing = state.loading
                    permissionBanner.visibility = if (state.permissionGranted) View.GONE else View.VISIBLE
                    val showEmpty = state.permissionGranted && state.contacts.isEmpty() && !state.loading
                    emptyView.visibility = if (showEmpty) View.VISIBLE else View.GONE
                    recycler.visibility = if (state.permissionGranted && state.contacts.isNotEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                    emptyTitle.text = if (state.searchQuery.isNotBlank()) {
                        getString(R.string.no_search_results)
                    } else {
                        getString(R.string.no_contacts)
                    }
                    emptySubtitle.text = getString(R.string.no_contacts_hint)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
