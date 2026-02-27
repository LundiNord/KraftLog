package de.nyxnord.kraftlog.data.preferences

import android.content.Context

class ReminderPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }

    var intervalDays: Int
        get() = prefs.getInt("interval_days", 4)
        set(value) { prefs.edit().putInt("interval_days", value).apply() }
}
