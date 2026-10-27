package com.glyphpings.matrix

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** Builds the frames for one ping: logo pop -> scrolling sender -> scrolling message. */
object PingRenderer {
    const val FRAME_MS = 50L
    private const val TEXT_TOP = 9      // text occupies rows 9..16 (widest part of the circle)
    private const val DOTS_ROW = 20

    /** Logo "pops": fades in, two quick pulses, then holds. ~1.3 s */
    fun logoFrames(icon: IntArray): List<IntArray> {
        val n = 26
        return (0 until n).map { f ->
            val t = f / (n - 1f)
            val k = when {
                t < 0.25f -> t / 0.25f
                t < 0.75f -> 0.75f + 0.25f * sin((t - 0.25f) / 0.5f * 4 * PI).toFloat()
                else -> 1f
            }
            Matrix.scaled(icon, k.coerceIn(0f, 1f))
        }
    }

    /**
     * Text scrolling right-to-left through the middle rows, like NothingOS caller ID.
     * [mini] is a small app icon shown above the text so you know which app it is.
     * [stepsPerFrame] 1 = normal speed.
     */
    fun scrollFrames(text: String, brightness: Int, mini: IntArray?, unread: Int, stepsPerFrame: Int = 1): List<IntArray> {
        val cols = PixelFont.columns(text)
        val frames = ArrayList<IntArray>()
        var offset = -Matrix.SIZE + 3          // start just off the right edge
        while (offset <= cols.size) {
            val px = Matrix.blank()
            mini?.let { overlay(px, it) }
            for (x in 0 until Matrix.SIZE) {
                val c = cols.getOrNull(offset + x) ?: 0
                if (c == 0) continue
                for (y in 0 until PixelFont.HEIGHT) if ((c shr y) and 1 == 1) Matrix.set(px, x, TEXT_TOP + y, brightness)
            }
            dots(px, unread)
            frames += px
            offset += stepsPerFrame
        }
        return frames
    }

    /** Resting picture between pings / on AOD: the last app's logo, dimmed, with unread dots. */
    fun idleFrame(icon: IntArray?, unread: Int): IntArray {
        if (icon == null || unread == 0) return Matrix.blank().also { Matrix.set(it, 12, 12, 40) }
        val px = Matrix.scaled(icon, 0.35f)
        dots(px, unread, row = 23)
        return px
    }

    /** 7x7 version of a 25x25 icon, placed at the top (rows 1..7). */
    fun miniIcon(icon: IntArray): IntArray {
        val px = Matrix.blank()
        for (my in 0 until 7) for (mx in 0 until 7) {
            // icon content lives in the central 19x19 area: sample a 3x3 block per mini pixel
            var sum = 0
            for (dy in 0..2) for (dx in 0..2) {
                val sx = 3 + mx * 19 / 7 + dx; val sy = 3 + my * 19 / 7 + dy
                if (sx < Matrix.SIZE && sy < Matrix.SIZE) sum += icon[Matrix.idx(sx, sy)]
            }
            if (sum / 9 > 110) Matrix.set(px, 9 + mx, 1 + my, 170)
        }
        return px
    }

    private fun overlay(dst: IntArray, src: IntArray) {
        for (i in dst.indices) if (src[i] > dst[i]) dst[i] = src[i]
    }

    /** Up to 5 unread dots, centred. */
    private fun dots(px: IntArray, unread: Int, row: Int = DOTS_ROW) {
        val n = unread.coerceAtMost(5)
        if (n <= 0) return
        val start = 12 - (n - 1)
        for (k in 0 until n) Matrix.set(px, start + k * 2, row, if (unread > 5 && k == n - 1) 255 else 140)
    }

    @Suppress("unused")
    fun fade(px: IntArray, k: Float) = IntArray(px.size) { (px[it] * k).roundToInt() }
}
