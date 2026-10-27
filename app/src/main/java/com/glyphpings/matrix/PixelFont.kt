package com.glyphpings.matrix

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.text.Normalizer

/**
 * Hand-made 8-row pixel font (rows 0-6 = capitals, row 7 = descenders), proportional width.
 * Characters outside the font (other scripts, emoji) are drawn with the system font at tiny size,
 * so Tamil / Hindi / Arabic names still show up as something readable-ish.
 */
object PixelFont {
    const val HEIGHT = 8
    private const val SPACING = 1

    /** A glyph: [width] columns, [rows] = 8 bitmasks (bit (width-1) = leftmost column). */
    class Glyph(val width: Int, val rows: IntArray) {
        fun lit(x: Int, y: Int) = (rows[y] shr (width - 1 - x)) and 1 == 1
    }

    private fun g(w: Int, vararg r: Int) = Glyph(w, r)

    private val glyphs: Map<Char, Glyph> = mapOf(
        ' ' to g(3, 0, 0, 0, 0, 0, 0, 0, 0),
        'A' to g(5, 14, 17, 17, 31, 17, 17, 17, 0),
        'B' to g(5, 30, 17, 17, 30, 17, 17, 30, 0),
        'C' to g(5, 14, 17, 16, 16, 16, 17, 14, 0),
        'D' to g(5, 30, 17, 17, 17, 17, 17, 30, 0),
        'E' to g(5, 31, 16, 16, 30, 16, 16, 31, 0),
        'F' to g(5, 31, 16, 16, 30, 16, 16, 16, 0),
        'G' to g(5, 14, 17, 16, 23, 17, 17, 14, 0),
        'H' to g(5, 17, 17, 17, 31, 17, 17, 17, 0),
        'I' to g(3, 7, 2, 2, 2, 2, 2, 7, 0),
        'J' to g(5, 7, 2, 2, 2, 2, 18, 12, 0),
        'K' to g(5, 17, 18, 20, 24, 20, 18, 17, 0),
        'L' to g(5, 16, 16, 16, 16, 16, 16, 31, 0),
        'M' to g(5, 17, 27, 21, 21, 17, 17, 17, 0),
        'N' to g(5, 17, 25, 21, 19, 17, 17, 17, 0),
        'O' to g(5, 14, 17, 17, 17, 17, 17, 14, 0),
        'P' to g(5, 30, 17, 17, 30, 16, 16, 16, 0),
        'Q' to g(5, 14, 17, 17, 17, 21, 18, 13, 0),
        'R' to g(5, 30, 17, 17, 30, 20, 18, 17, 0),
        'S' to g(5, 15, 16, 16, 14, 1, 1, 30, 0),
        'T' to g(5, 31, 4, 4, 4, 4, 4, 4, 0),
        'U' to g(5, 17, 17, 17, 17, 17, 17, 14, 0),
        'V' to g(5, 17, 17, 17, 17, 17, 10, 4, 0),
        'W' to g(5, 17, 17, 17, 21, 21, 21, 10, 0),
        'X' to g(5, 17, 17, 10, 4, 10, 17, 17, 0),
        'Y' to g(5, 17, 17, 10, 4, 4, 4, 4, 0),
        'Z' to g(5, 31, 1, 2, 4, 8, 16, 31, 0),
        'a' to g(4, 0, 0, 7, 1, 7, 9, 7, 0),
        'b' to g(4, 8, 8, 14, 9, 9, 9, 14, 0),
        'c' to g(4, 0, 0, 6, 8, 8, 8, 6, 0),
        'd' to g(4, 1, 1, 7, 9, 9, 9, 7, 0),
        'e' to g(4, 0, 0, 6, 9, 15, 8, 7, 0),
        'f' to g(3, 1, 2, 2, 7, 2, 2, 2, 0),
        'g' to g(4, 0, 0, 7, 9, 9, 7, 1, 6),
        'h' to g(4, 8, 8, 14, 9, 9, 9, 9, 0),
        'i' to g(1, 1, 0, 1, 1, 1, 1, 1, 0),
        'j' to g(3, 1, 0, 3, 1, 1, 1, 1, 6),
        'k' to g(4, 8, 8, 9, 10, 12, 10, 9, 0),
        'l' to g(2, 2, 2, 2, 2, 2, 2, 1, 0),
        'm' to g(5, 0, 0, 26, 21, 21, 21, 21, 0),
        'n' to g(4, 0, 0, 14, 9, 9, 9, 9, 0),
        'o' to g(4, 0, 0, 6, 9, 9, 9, 6, 0),
        'p' to g(4, 0, 0, 14, 9, 9, 14, 8, 8),
        'q' to g(4, 0, 0, 7, 9, 9, 7, 1, 1),
        'r' to g(3, 0, 0, 5, 6, 4, 4, 4, 0),
        's' to g(4, 0, 0, 7, 8, 6, 1, 14, 0),
        't' to g(3, 2, 2, 7, 2, 2, 2, 1, 0),
        'u' to g(4, 0, 0, 9, 9, 9, 9, 7, 0),
        'v' to g(5, 0, 0, 17, 17, 17, 10, 4, 0),
        'w' to g(5, 0, 0, 17, 17, 21, 21, 10, 0),
        'x' to g(4, 0, 0, 9, 9, 6, 9, 9, 0),
        'y' to g(4, 0, 0, 9, 9, 9, 7, 1, 6),
        'z' to g(4, 0, 0, 15, 1, 6, 8, 15, 0),
        '0' to g(5, 14, 17, 19, 21, 25, 17, 14, 0),
        '1' to g(3, 2, 6, 2, 2, 2, 2, 7, 0),
        '2' to g(5, 14, 17, 1, 2, 4, 8, 31, 0),
        '3' to g(5, 30, 1, 1, 14, 1, 1, 30, 0),
        '4' to g(5, 2, 6, 10, 18, 31, 2, 2, 0),
        '5' to g(5, 31, 16, 30, 1, 1, 17, 14, 0),
        '6' to g(5, 14, 16, 16, 30, 17, 17, 14, 0),
        '7' to g(5, 31, 1, 2, 4, 8, 8, 8, 0),
        '8' to g(5, 14, 17, 17, 14, 17, 17, 14, 0),
        '9' to g(5, 14, 17, 17, 15, 1, 1, 14, 0),
        '.' to g(1, 0, 0, 0, 0, 0, 0, 1, 0),
        ',' to g(2, 0, 0, 0, 0, 0, 1, 1, 2),
        '!' to g(1, 1, 1, 1, 1, 1, 0, 1, 0),
        '?' to g(5, 14, 17, 1, 2, 4, 0, 4, 0),
        ':' to g(1, 0, 0, 1, 0, 0, 1, 0, 0),
        ';' to g(2, 0, 0, 1, 0, 0, 1, 1, 2),
        '\'' to g(1, 1, 1, 0, 0, 0, 0, 0, 0),
        '"' to g(3, 5, 5, 0, 0, 0, 0, 0, 0),
        '-' to g(4, 0, 0, 0, 15, 0, 0, 0, 0),
        '_' to g(4, 0, 0, 0, 0, 0, 0, 15, 0),
        '+' to g(5, 0, 4, 4, 31, 4, 4, 0, 0),
        '=' to g(4, 0, 0, 15, 0, 15, 0, 0, 0),
        '/' to g(5, 1, 2, 2, 4, 8, 8, 16, 0),
        '(' to g(2, 1, 2, 2, 2, 2, 2, 1, 0),
        ')' to g(2, 2, 1, 1, 1, 1, 1, 2, 0),
        '@' to g(5, 14, 17, 23, 21, 22, 16, 14, 0),
        '#' to g(5, 10, 10, 31, 10, 31, 10, 10, 0),
        '&' to g(5, 12, 18, 20, 8, 21, 18, 13, 0),
        '*' to g(5, 0, 21, 14, 31, 14, 21, 0, 0),
        '%' to g(5, 25, 25, 2, 4, 8, 19, 19, 0),
        '$' to g(5, 4, 15, 20, 14, 5, 30, 4, 0),
        '<' to g(4, 1, 2, 4, 8, 4, 2, 1, 0),
        '>' to g(4, 8, 4, 2, 1, 2, 4, 8, 0),
        '[' to g(2, 3, 2, 2, 2, 2, 2, 3, 0),
        ']' to g(2, 3, 1, 1, 1, 1, 1, 3, 0),
        '~' to g(5, 0, 0, 8, 21, 2, 0, 0, 0),
        '|' to g(1, 1, 1, 1, 1, 1, 1, 1, 0),
        '^' to g(5, 4, 10, 17, 0, 0, 0, 0, 0),

    )

    /** Renders [text] into columns: result[x] = 8-bit column mask (bit y = row y lit). */
    fun columns(text: String): List<Int> {
        val cols = ArrayList<Int>(text.length * 5)
        for (raw in clean(text)) {
            val glyph = glyphs[raw] ?: glyphs[fold(raw)] ?: fallback(raw)
            for (x in 0 until glyph.width) {
                var c = 0
                for (y in 0 until HEIGHT) if (glyph.lit(x, y)) c = c or (1 shl y)
                cols += c
            }
            if (glyph.width > 0) repeat(SPACING) { cols += 0 }
        }
        return cols
    }

    /** Collapse whitespace/newlines, drop invisible characters. */
    private fun clean(s: String): String =
        s.replace(Regex("[\\r\\n\\t]+"), "  ").replace(Regex("\\s{3,}"), "  ").trim()

    /** é -> e, ñ -> n, etc. */
    private fun fold(c: Char): Char =
        Normalizer.normalize(c.toString(), Normalizer.Form.NFD).firstOrNull { it.code < 128 } ?: c

    private val cache = HashMap<Char, Glyph>()

    /** Draw an unknown character with the system font into an 8px tall cell. */
    private fun fallback(c: Char): Glyph = cache.getOrPut(c) {
        if (Character.isSurrogate(c) || Character.getType(c) == Character.NON_SPACING_MARK.toInt()) {
            return@getOrPut Glyph(0, IntArray(HEIGHT))
        }
        val paint = Paint().apply {
            isAntiAlias = false; textSize = 9f; color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
        }
        val w = paint.measureText(c.toString()).toInt().coerceIn(1, 9)
        val bmp = Bitmap.createBitmap(w, HEIGHT, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawText(c.toString(), 0f, 7f, paint)
        val rows = IntArray(HEIGHT) { y ->
            var m = 0
            for (x in 0 until w) if (Color.alpha(bmp.getPixel(x, y)) > 110) m = m or (1 shl (w - 1 - x))
            m
        }
        bmp.recycle()
        Glyph(w, rows)
    }
}
