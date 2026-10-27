package com.glyphpings.glyph

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.glyphpings.ping.PingCenter
import com.glyphpings.ping.Prefs
import com.nothing.ketchum.GlyphToy

/**
 * The "Pings" Glyph Toy. Select it in the Glyph Toys carousel and/or as the AOD toy.
 * While it's active, new pings play on the matrix immediately (screen on or off).
 *  - Long-press the Glyph Button: replay the latest ping
 *  - Press & hold, then release: mark everything as read
 */
class PingsToyService : Service() {
    private var glyph: GlyphController? = null
    private var holdStart = 0L

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what != GlyphToy.MSG_GLYPH_TOY) return super.handleMessage(msg)
            when (msg.data?.getString(GlyphToy.MSG_GLYPH_TOY_DATA)) {
                GlyphToy.EVENT_CHANGE -> PingCenter.replay()
                "action_down" -> holdStart = System.currentTimeMillis()
                "action_up" -> if (holdStart > 0 && System.currentTimeMillis() - holdStart > 1500) PingCenter.clearUnread()
                GlyphToy.EVENT_AOD -> Unit   // idle frame is already showing; nothing to redraw each minute
            }
        }
    }
    private val messenger = Messenger(handler)

    override fun onBind(intent: Intent?): IBinder {
        PingCenter.init(this)
        val g = GlyphController(this, GlyphController.Mode.TOY).apply {
            brightness = Prefs(this@PingsToyService).brightness
            start()
        }
        glyph = g
        PingCenter.attachToy(g)
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        PingCenter.detachToy()
        glyph?.stop()
        glyph = null
        return false
    }
}
