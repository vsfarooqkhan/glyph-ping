package com.glyphpings.ping

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("glyph_pings", Context.MODE_PRIVATE)

    /** Only show who pinged you, never the message text. On by default for privacy. */
    var namesOnly: Boolean
        get() = sp.getBoolean("names_only", true)
        set(v) = sp.edit().putBoolean("names_only", v).apply()

    /** Include group chats (shown as "Sender in Group"). */
    var showGroups: Boolean
        get() = sp.getBoolean("show_groups", true)
        set(v) = sp.edit().putBoolean("show_groups", v).apply()

    /** Try to light the matrix even when the Pings toy isn't the active toy (uses the app-display API). */
    var lightWithoutToy: Boolean
        get() = sp.getBoolean("light_without_toy", true)
        set(v) = sp.edit().putBoolean("light_without_toy", v).apply()

    /** 1 = normal, 2 = fast scrolling. */
    var scrollSpeed: Int
        get() = sp.getInt("scroll_speed", 1)
        set(v) = sp.edit().putInt("scroll_speed", v).apply()

    var brightness: Float
        get() = sp.getFloat("brightness", 1f)
        set(v) = sp.edit().putFloat("brightness", v).apply()

    fun isEnabled(s: Source) = sp.getBoolean("src_${s.name}", true)
    fun setEnabled(s: Source, on: Boolean) = sp.edit().putBoolean("src_${s.name}", on).apply()
}
