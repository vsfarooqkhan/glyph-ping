package com.glyphpings.ping

import android.content.Context
import android.util.Log
import com.glyphpings.glyph.GlyphController
import com.glyphpings.matrix.IconRasterizer
import com.glyphpings.matrix.Matrix
import com.glyphpings.matrix.PingRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The brain: keeps unread pings, plays them one after another on the matrix, and decides
 * where frames go:
 *  1. the Pings Glyph Toy, if it is currently active (reliable, works with the screen off)
 *  2. otherwise, if enabled, the app-display API (experimental: NothingOS may not allow it from the background)
 *  3. always: the in-app preview
 */
object PingCenter {
    private const val TAG = "PingCenter"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var appContext: Context
    private lateinit var prefs: Prefs
    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        prefs = Prefs(appContext)
    }

    // ---- state for the UI
    private val _frame = MutableStateFlow(Matrix.blank())
    val frame: StateFlow<IntArray> = _frame.asStateFlow()
    private val _recent = MutableStateFlow<List<Ping>>(emptyList())
    val recent: StateFlow<List<Ping>> = _recent.asStateFlow()
    private val _unread = MutableStateFlow<List<Ping>>(emptyList())
    val unread: StateFlow<List<Ping>> = _unread.asStateFlow()

    // ---- outputs
    private var toy: GlyphController? = null
    private var app: GlyphController? = null
    private var appCloseJob: Job? = null

    private val _toyActive = MutableStateFlow(false)
    val toyActive: StateFlow<Boolean> = _toyActive.asStateFlow()

    fun attachToy(controller: GlyphController) {
        // the toy takes over: release the app-display path without unbinding the shared Glyph service
        appCloseJob?.cancel(); app?.stop(unbind = false); app = null
        toy = controller; _toyActive.value = true
        showIdle()
    }
    fun detachToy() { toy = null; _toyActive.value = false }

    // ---- queue & playback
    private val queue = ArrayDeque<Ping>()
    private var player: Job? = null

    fun onPing(p: Ping) {
        _recent.value = (listOf(p) + _recent.value).take(30)
        _unread.value = (_unread.value.filterNot { it.key == p.key } + p).takeLast(20)
        // keep the queue short: if many arrive at once, show the latest few
        queue.removeAll { it.key == p.key }
        queue.addLast(p)
        while (queue.size > 4) queue.removeFirst()
        if (player?.isActive != true) player = scope.launch { playQueue() }
    }

    fun onRemoved(key: String) {
        PingParser.forget(key)
        if (_unread.value.any { it.key == key }) {
            _unread.value = _unread.value.filterNot { it.key == key }
            if (player?.isActive != true) showIdle()
        }
    }

    /** Long-press on the Glyph Button: replay the newest unread ping (or the newest ping). */
    fun replay() {
        val p = _unread.value.lastOrNull() ?: _recent.value.firstOrNull() ?: return
        if (player?.isActive == true) return
        queue.addLast(p)
        player = scope.launch { playQueue() }
    }

    /** Call after changing brightness in the app. */
    fun applyBrightness(b: Float) { toy?.brightness = b; app?.brightness = b; showIdle() }

    fun clearUnread() { _unread.value = emptyList(); showIdle() }

    fun test() {
        onPing(Ping("test-${System.currentTimeMillis()}", Source.WHATSAPP, "test", "Rahul", "Weekend plans",
            "Are you coming tonight? We're meeting at 8 near the beach", System.currentTimeMillis(),
            IconRasterizer.bubble()))
    }

    private suspend fun playQueue() {
        while (queue.isNotEmpty()) {
            val p = queue.removeFirst()
            for (f in framesFor(p)) { emit(f); delay(PingRenderer.FRAME_MS) }
            delay(250)
        }
        showIdle()
        scheduleAppClose()
    }

    private fun framesFor(p: Ping): List<IntArray> {
        val speed = prefs.scrollSpeed
        val unread = _unread.value.size
        val mini: IntArray? = null   // the logo pops first, so the text gets the whole matrix
        val who = if (p.group != null) "${p.sender} in ${p.group}" else p.sender
        val frames = PingRenderer.logoFrames(p.icon).toMutableList()
        frames += PingRenderer.scrollFrames(who, 255, mini, unread, speed)
        if (!prefs.namesOnly && p.text.isNotBlank()) {
            frames += PingRenderer.scrollFrames(p.text.take(160), 170, mini, unread, speed)
        }
        return frames
    }

    private fun emit(px: IntArray) {
        _frame.value = px
        val t = toy
        if (t != null) { t.show(px); return }
        if (!prefs.lightWithoutToy) return
        appCloseJob?.cancel()
        val a = app ?: GlyphController(appContext, GlyphController.Mode.APP).also { app = it; it.start() }
        a.brightness = prefs.brightness
        a.show(px)
    }

    private fun showIdle() {
        val last = _unread.value.lastOrNull()
        val px = PingRenderer.idleFrame(last?.icon, _unread.value.size)
        _frame.value = px
        toy?.show(px)
    }

    /** Hand the matrix back to NothingOS a few seconds after a ping finishes (app-display path only). */
    private fun scheduleAppClose() {
        val a = app ?: return
        appCloseJob?.cancel()
        appCloseJob = scope.launch {
            delay(4000)
            Log.d(TAG, "releasing app matrix")
            a.stop(unbind = toy == null); app = null
        }
    }
}
