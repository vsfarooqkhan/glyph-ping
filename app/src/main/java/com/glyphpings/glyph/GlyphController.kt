package com.glyphpings.glyph

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.glyphpings.matrix.Matrix
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject

/**
 * Wraps the Nothing GlyphMatrixManager. All calls are guarded so the app runs anywhere.
 * TOY mode -> setMatrixFrame (inside the Glyph Toy service)
 * APP mode -> setAppMatrixFrame (experimental "light up without the toy" path)
 */
class GlyphController(private val context: Context, private val mode: Mode) {
    enum class Mode { APP, TOY }

    private var gm: GlyphMatrixManager? = null
    @Volatile var connected = false
        private set
    var brightness = 1f
    private var pending: IntArray? = null

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            try {
                gm?.register(Glyph.DEVICE_23112)
                connected = true
                pending?.let { show(it) }
            } catch (t: Throwable) {
                Log.w(TAG, "register failed", t)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) { connected = false }
    }

    fun start() {
        if (gm != null) return
        try {
            gm = GlyphMatrixManager.getInstance(context.applicationContext)
            gm?.init(callback)
        } catch (t: Throwable) {
            Log.i(TAG, "Glyph Matrix not available: ${t.message}")
            gm = null
        }
    }

    fun show(pixels: IntArray) {
        pending = pixels
        val m = gm ?: return
        if (!connected) return
        try {
            val obj = GlyphMatrixObject.Builder()
                .setImageSource(Matrix.toBitmap(pixels, brightness))
                .setScale(100).setOrientation(0).setPosition(0, 0).setBrightness(255)
                .build()
            val frame = GlyphMatrixFrame.Builder().addTop(obj).build(context)
            if (mode == Mode.APP) m.setAppMatrixFrame(frame.render()) else m.setMatrixFrame(frame.render())
        } catch (t: Throwable) {
            Log.w(TAG, "show failed", t)
        }
    }

    /** [unbind] = false keeps the shared Glyph service binding alive (used when a toy is also connected). */
    fun stop(unbind: Boolean = true) {
        val m = gm ?: return
        try { if (mode == Mode.APP) m.closeAppMatrix() else m.turnOff() } catch (t: Throwable) { Log.w(TAG, "close failed", t) }
        if (unbind) try { m.unInit() } catch (_: Throwable) {}
        gm = null
        connected = false
    }

    companion object { private const val TAG = "GlyphController" }
}
