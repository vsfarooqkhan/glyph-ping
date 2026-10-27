package com.glyphpings.matrix

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable

/**
 * Turns an app's own notification icon into a 25x25 LED frame.
 * Android notification small icons are single-colour silhouettes by design, so we only need
 * their alpha channel. For full-colour launcher icons we use luminance instead.
 */
object IconRasterizer {
    private const val HI = 200

    fun fromDrawable(d: Drawable, size: Int = 19, useLuminance: Boolean = false): IntArray {
        val big = Bitmap.createBitmap(HI, HI, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, HI, HI)
        d.draw(Canvas(big))
        val src = IntArray(HI * HI)
        big.getPixels(src, 0, HI, 0, 0, HI, HI)
        big.recycle()

        val out = Matrix.blank()
        val off = (Matrix.SIZE - size) / 2
        val cell = HI / size.toFloat()
        for (cy in 0 until size) for (cx in 0 until size) {
            val xs = (cx * cell).toInt(); val xe = maxOf(xs + 1, ((cx + 1) * cell).toInt())
            val ys = (cy * cell).toInt(); val ye = maxOf(ys + 1, ((cy + 1) * cell).toInt())
            var sum = 0f; var n = 0
            for (y in ys until minOf(ye, HI)) for (x in xs until minOf(xe, HI)) {
                val p = src[y * HI + x]
                val a = Color.alpha(p) / 255f
                sum += if (useLuminance) a * (0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)) / 255f else a
                n++
            }
            val v = if (n > 0) sum / n else 0f
            if (v > 0.42f) Matrix.set(out, cx + off, cy + off, 255)
        }
        return out
    }

    /** Generic speech-bubble icon, used if an app's icon can't be loaded. */
    fun bubble(): IntArray {
        val px = Matrix.blank()
        for (y in 5..15) for (x in 4..20) {
            val corner = (y == 5 || y == 15) && (x == 4 || x == 20)
            if (!corner) Matrix.set(px, x, y, 255)
        }
        for (k in 0..3) for (x in 7..(10 - k)) Matrix.set(px, x, 16 + k, 255)
        for (x in listOf(8, 12, 16)) for (y in 9..11) for (dx in 0..1) Matrix.set(px, x + dx, y, 0)
        return px
    }
}
