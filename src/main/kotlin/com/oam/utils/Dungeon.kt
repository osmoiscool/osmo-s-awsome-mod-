package com.oam.utils

import java.util.Collections

object Dungeon {

    @Volatile
    var phase = 0
        private set

    class Box(val x1: Double, val x2: Double, val y1: Double, val y2: Double, val z1: Double, val z2: Double) {
        fun containsPlayer() = isPlayerInBox(x1, x2, y1, y2, z1, z2)
    }

    private fun box(vararg v: Int) = Box(v[0].toDouble(), v[1].toDouble(), v[2].toDouble(), v[3].toDouble(), v[4].toDouble(), v[5].toDouble())

    val regions: Map<String, Box> = linkedMapOf(
        "p1" to box(42, 105, 220, 230, 12, 84),
        "p2" to box(20, 85, 163, 213, 0, 107),
        "gpad" to box(20, 42, 170, 172, 0, 22),
        "pillars" to box(33, 60, 165, 195, 31, 76),
        "ppillar" to box(87, 114, 163, 172, 31, 76),
        "ppad" to box(87, 127, 163, 172, 85, 107),
        "ypad" to box(21, 52, 165, 177, 85, 107),
        "s1" to box(89, 113, 106, 146, 30, 122),
        "s2" to box(19, 111, 106, 146, 121, 145),
        "s3" to box(-6, 20, 106, 146, 50, 143),
        "s4" to box(-2, 89, 106, 152, 30, 51),
        "mid" to box(46, 63, 58, 96, 68, 85),
        "p5" to box(14, 99, 5, 8, 52, 134),
    )

    fun inRegion(name: String): Boolean = regions[name]?.containsPlayer() == true

    fun inP4(): Boolean {
        if (phase == 0) return false
        val y = mc.player?.y ?: return false
        return y > 45.0 && y <= 100.0
    }

    fun inP5(): Boolean {
        if (phase == 0) return false
        val y = mc.player?.y ?: return false
        return y <= 45.0
    }

    fun currentRegions(): List<String> = regions.filterValues { it.containsPlayer() }.keys.toList()

    fun onChat(message: String) {
        if (message.startsWith("[BOSS] Goldor: You have done it, you destroyed the factory")) {
            phase = 4
            return
        }
        phase = when (message) {
            "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!" -> 1
            "[BOSS] Storm: Pathetic Maxor, just like expected." -> 2
            "[BOSS] Storm: I should have known that I stood no chance." -> 3
            "[BOSS] Necron: All this, for nothing..." -> 5
            else -> return
        }
    }

    fun onWorldChange() {
        phase = 0
        partyCache.clear()
    }

    private val tablistRegex = Regex("""^\[(\d+)] (?:\[[^\]]+] )*(\w+) .*?\((\w+)(?: (\w+))*\)$""")

    private val partyCache: MutableMap<String, String> = Collections.synchronizedMap(LinkedHashMap())

    private fun parseInto(line: String, into: MutableMap<String, String>) {
        val m = tablistRegex.find(line.removeFormatting().trim()) ?: return
        into[m.groupValues[2]] = m.groupValues[3]
    }

    fun onTabUpdate(lines: List<String>) {
        for (line in lines) parseInto(line, partyCache)
    }

    fun onTick() {
        for (line in tabLines()) parseInto(line, partyCache)
    }

    fun party(): Map<String, String> = synchronized(partyCache) { LinkedHashMap(partyCache) }

    fun getNameByClass(query: String): String? {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return null
        val me = mc.player?.name?.string
        val party = party()

        party.entries.firstOrNull { (name, clazz) -> name != me && clazz != "DEAD" && clazz.lowercase().startsWith(q) }
            ?.let { return it.key }

        return party.keys.firstOrNull { it.equals(q, ignoreCase = true) }
    }

    fun getClass(): String? {
        val me = mc.player?.name?.string ?: return null
        return party()[me]
    }
}
