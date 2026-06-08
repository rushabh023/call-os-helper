package com.example.helper_application.dialer.core.utils

import android.content.Context
import com.example.helper_application.R
import java.util.concurrent.TimeUnit

object RelativeTimeFormatter {

    fun format(context: Context, timestampMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestampMs
        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return context.getString(R.string.time_just_now)
        }
        if (diff < TimeUnit.HOURS.toMillis(1)) {
            val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
            return context.getString(R.string.time_minutes_ago, mins)
        }
        if (diff < TimeUnit.DAYS.toMillis(1)) {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            return context.getString(R.string.time_hours_ago, hours)
        }
        if (diff < TimeUnit.DAYS.toMillis(7)) {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            return context.getString(R.string.time_days_ago, days)
        }
        return android.text.format.DateFormat.getDateFormat(context).format(timestampMs)
    }
}
