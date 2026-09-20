package com.retroweather.art

import com.retroweather.data.WeatherKind

/** Original hand-authored pixel masks. Every '#' is one occupied cell, '.' is transparent.
 * No downloaded icons, vector rasterization, or generative imagery is used. */
object PixelArt {
    private val sun = """
        .......#.......
        .......#.......
        ..#.........#..
        ...#.......#...
        .....#####.....
        ....#.....#....
        ....#.....#....
        ##..#.....#..##
        ....#.....#....
        ....#.....#....
        .....#####.....
        ...#.......#...
        ..#.........#..
        .......#.......
        .......#.......
    """.trimIndent()
    private val moon = """
        ......#####...
        ....##..#.....
        ...#...#......
        ..#....#......
        .#.....#......
        .#.....#......
        #.......#.....
        #........##...
        #..........###
        .#...........#
        .#..........#.
        ..#........#..
        ...##....##...
        .....####.....
    """.trimIndent()
    private val cloud = """
        ........######........
        ......##......##......
        .....#..........#.....
        ....#............#....
        .###.............###..
        #...................#.
        #....................#
        #....................#
        .#..................#.
        ..##################..
    """.trimIndent()
    private val bolt = """
        ....###.
        ...###..
        ..###...
        .######.
        ....##..
        ...##...
        ..##....
        .#......
    """.trimIndent()
    private val flake = """
        ..#..
        #.#.#
        .###.
        #.#.#
        ..#..
    """.trimIndent()
    private val unknown = """
        .#####.
        #.....#
        ......#
        ....##.
        ...#...
        ...#...
        .......
        ...#...
    """.trimIndent()

    fun sprite(kind: WeatherKind, day: Boolean): Array<BooleanArray> {
        val pixels = Array(32) { BooleanArray(32) }
        fun stamp(mask: String, x: Int, y: Int) {
            mask.lines().forEachIndexed { dy, row -> row.forEachIndexed { dx, c ->
                if (c == '#' && y + dy in 0..31 && x + dx in 0..31) pixels[y + dy][x + dx] = true
            } }
        }
        fun drop(x: Int, y: Int) { stamp(".#\n.#\n#.", x, y) }
        when (kind) {
            WeatherKind.CLEAR -> stamp(if (day) sun else moon, 8, 8)
            WeatherKind.PARTLY_CLOUDY -> {
                stamp(if (day) sun else moon, 2, 2)
                // Clear the foreground cloud interior so celestial pixels don't bleed through.
                for (y in 13..24) for (x in 5..29) pixels[y][x] = false
                stamp(cloud, 6, 14)
            }
            WeatherKind.UNKNOWN -> stamp(unknown, 12, 12)
            WeatherKind.FOG -> {
                stamp(cloud, 5, 5)
                stamp("####################\n....................\n..###############...\n....................\n##################..\n....................\n....############....", 6, 18)
            }
            else -> {
                if (kind == WeatherKind.CLOUDY) stamp(cloud, 2, 6)
                if (kind == WeatherKind.SHOWERS || kind == WeatherKind.SNOW_SHOWERS) stamp(if (day) sun else moon, 1, 1)
                stamp(cloud, 5, 11)
                when (kind) {
                    WeatherKind.DRIZZLE -> { stamp("#...#...#", 10, 24); stamp("#...#", 12, 27) }
                    WeatherKind.RAIN, WeatherKind.SHOWERS -> { drop(9, 23); drop(16, 24); drop(23, 23) }
                    WeatherKind.HEAVY_RAIN -> { for (x in listOf(7, 12, 17, 22)) { drop(x, 22); drop(x - 2, 27) } }
                    WeatherKind.FREEZING -> { drop(8, 23); stamp(flake, 15, 23); drop(24, 24) }
                    WeatherKind.SNOW, WeatherKind.SNOW_SHOWERS -> { stamp(flake, 8, 23); stamp(flake, 20, 24) }
                    WeatherKind.HEAVY_SNOW -> { stamp(flake, 5, 23); stamp(flake, 14, 26); stamp(flake, 23, 23) }
                    WeatherKind.THUNDER, WeatherKind.HAIL -> {
                        stamp(bolt, 13, 21)
                        if (kind == WeatherKind.HAIL) { stamp("##\n##", 7, 25); stamp("##\n##", 24, 26) }
                    }
                    else -> Unit
                }
            }
        }
        return pixels
    }

    // Original 5x7 bitmap type. Slash separates rows; each '1' is a pixel.
    val glyphs: Map<Char, List<String>> = mapOf(
        '0' to "01110/11011/10001/10001/10001/11011/01110",
        '1' to "00100/01100/00100/00100/00100/00100/01110",
        '2' to "11110/00001/00001/01110/10000/10000/11111",
        '3' to "11110/00001/00001/01110/00001/00001/11110",
        '4' to "10010/10010/10010/11111/00010/00010/00010",
        '5' to "11111/10000/10000/11110/00001/00001/11110",
        '6' to "01110/10000/10000/11110/10001/10001/01110",
        '7' to "11111/00001/00010/00100/01000/01000/01000",
        '8' to "01110/10001/10001/01110/10001/10001/01110",
        '9' to "01110/10001/10001/01111/00001/00001/01110",
        '-' to "00000/00000/00000/11111/00000/00000/00000",
        '°' to "01100/10010/10010/01100/00000/00000/00000",
        'F' to "11111/10000/10000/11110/10000/10000/10000",
        'C' to "01111/10000/10000/10000/10000/10000/01111",
        '?' to "01110/10001/00001/00110/00100/00000/00100"
    ).mapValues { it.value.split('/') }
}
