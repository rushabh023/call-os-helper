package com.example.helper_application.dialer.core.utils

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.helper_application.R

object AvatarHelper {

    private val avatarColors = intArrayOf(
        R.color.avatar_1,
        R.color.avatar_2,
        R.color.avatar_3,
        R.color.avatar_4,
        R.color.avatar_5,
        R.color.avatar_6
    )

    fun bindInitials(view: TextView, label: String) {
        val initials = initialsFor(label)
        view.text = initials
        val colorRes = avatarColors[kotlin.math.abs(label.hashCode()) % avatarColors.size]
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(view.context, colorRes))
        }
        view.background = bg
    }

    fun initialsFor(label: String): String {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return "?"
        val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            trimmed.first().isDigit() -> "#"
            else -> trimmed.take(2).uppercase()
        }
    }
}
