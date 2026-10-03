package com.retroweather.art

import java.text.Normalizer
import java.util.Locale

/** Hand-placed Latin capitals share the original 5x7 temperature grid. */
object PixelText {
    private val alphabet = mapOf(
        'A' to "01110/10001/10001/11111/10001/10001/10001",
        'B' to "11110/10001/10001/11110/10001/10001/11110",
        'D' to "11110/10001/10001/10001/10001/10001/11110",
        'E' to "11111/10000/10000/11110/10000/10000/11111",
        'G' to "01110/10001/10000/10111/10001/10001/01110",
        'H' to "10001/10001/10001/11111/10001/10001/10001",
        'I' to "01110/00100/00100/00100/00100/00100/01110",
        'J' to "00111/00010/00010/00010/00010/10010/01100",
        'K' to "10001/10010/10100/11000/10100/10010/10001",
        'L' to "10000/10000/10000/10000/10000/10000/11111",
        'M' to "10001/11011/10101/10101/10001/10001/10001",
        'N' to "10001/11001/11001/10101/10011/10011/10001",
        'O' to "01110/10001/10001/10001/10001/10001/01110",
        'P' to "11110/10001/10001/11110/10000/10000/10000",
        'Q' to "01110/10001/10001/10001/10101/10010/01101",
        'R' to "11110/10001/10001/11110/10100/10010/10001",
        'S' to "01111/10000/10000/01110/00001/00001/11110",
        'T' to "11111/00100/00100/00100/00100/00100/00100",
        'U' to "10001/10001/10001/10001/10001/10001/01110",
        'V' to "10001/10001/10001/10001/10001/01010/00100",
        'W' to "10001/10001/10001/10101/10101/11011/10001",
        'X' to "10001/10001/01010/00100/01010/10001/10001",
        'Y' to "10001/10001/01010/00100/00100/00100/00100",
        'Z' to "11111/00001/00010/00100/01000/10000/11111",
        ' ' to "00000/00000/00000/00000/00000/00000/00000",
        '.' to "00000/00000/00000/00000/00000/00110/00110",
        ',' to "00000/00000/00000/00000/00110/00110/00100",
        ':' to "00000/00110/00110/00000/00110/00110/00000",
        '/' to "00001/00001/00010/00100/01000/10000/10000",
        '!' to "00100/00100/00100/00100/00100/00000/00100",
        '\'' to "00100/00100/00000/00000/00000/00000/00000",
        '+' to "00000/00100/00100/11111/00100/00100/00000",
        '(' to "00010/00100/01000/01000/01000/00100/00010",
        ')' to "01000/00100/00010/00010/00010/00100/01000",
        '%' to "11001/11010/00010/00100/01000/01011/10011"
    ).mapValues { it.value.split('/') } + PixelArt.glyphs
    fun glyph(char: Char) = alphabet[char] ?: alphabet.getValue('?')
    fun normalize(text: String): String = Normalizer.normalize(text.uppercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").map { if (it == '\n' || it in alphabet) it else '?' }.joinToString("")

    data class Block(val lines: List<String>) {
        val width get() = ((lines.maxOfOrNull { it.length } ?: 0) * 6 - 1).coerceAtLeast(0)
        val height get() = (lines.size * 9 - 2).coerceAtLeast(0)
    }
    fun layout(text: String, columns: Int, maxLines: Int): Block {
        if (columns < 1 || maxLines < 1) return Block(emptyList())
        val lines = mutableListOf<String>()
        for (paragraph in normalize(text).split('\n')) {
            var remaining = paragraph.trim()
            while (remaining.length > columns) {
                val space = remaining.lastIndexOf(' ', columns).takeIf { it > 0 } ?: columns
                lines.add(remaining.take(space)); remaining = remaining.drop(space).trimStart()
            }
            lines.add(remaining)
        }
        if (lines.size > maxLines) {
            val dots = ".".repeat(minOf(3, columns))
            return Block(lines.take(maxLines - 1) + (lines[maxLines - 1].take(columns - dots.length).trimEnd() + dots))
        }
        return Block(lines)
    }
}
