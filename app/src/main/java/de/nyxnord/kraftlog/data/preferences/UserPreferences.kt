package de.nyxnord.kraftlog.data.preferences

import android.content.Context

class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    var displayName: String
        get() = prefs.getString("display_name", "") ?: ""
        set(value) { prefs.edit().putString("display_name", value).apply() }
}
