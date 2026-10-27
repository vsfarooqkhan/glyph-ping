package com.glyphpings.matrix

import android.graphics.Bitmap
import kotlin.math.roundToInt

/** Phone (3) Glyph Matrix: 25x25 grid, circular, 489 physical LEDs. Frames are IntArray(625) of 0..255. */
object Matrix {
    const val SIZE = 25
    const val COUNT = SIZE * SIZE

    val mask: BooleanArray = BooleanArray(COUNT) { i ->
        val x = i % SIZE - 12
        val y = i / SIZE - 12
        (x * x + y * y) <= 12.5f * 12.5f
    }

    fun idx(x: Int, y: Int) = y * SIZE + x
    fun blank() = IntArray(COUNT)

    fun set(px: IntArray, x: Int, y: Int, v: Int) {
        if (x in 0 until SIZE && y in 0 until SIZE) {
            val i = idx(x, y)
            if (mask[i]) px[i] = v
        }
    }

    fun scaled(px: IntArray, k: Float) = IntArray(COUNT) { (px[it] * k).roundToInt().coerceIn(0, 255) }

    fun toBitmap(px: IntArray, brightness: Float = 1f): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val argb = IntArray(COUNT) { i ->
            val v = (px[i] * brightness).roundToInt().coerceIn(0, 255)
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        bmp.setPixels(argb, 0, SIZE, 0, 0, SIZE, SIZE)
        return bmp
    }
}
