package com.example.helper_application.dialer.feature.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.helper_application.R

class MoreFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_more, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.cardHelper).setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_helperHostFragment)
        }
        view.findViewById<View>(R.id.cardSettings).setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_settingsFragment)
        }
    }
}
