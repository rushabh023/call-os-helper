package com.example.helper_application.dialer.core.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.attachVerticalList() {
    layoutManager = layoutManager ?: LinearLayoutManager(context)
    if (itemAnimator == null) {
        itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
    }
}
