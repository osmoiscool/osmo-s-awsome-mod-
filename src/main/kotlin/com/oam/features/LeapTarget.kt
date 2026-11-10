package com.oam.features

import com.oam.config.OAMConfig
import com.oam.utils.Dungeon

object LeapTarget {

    private val config get() = OAMConfig.settings

    fun resolve(target: String): String = Dungeon.getNameByClass(target) ?: target.trim()

    @Volatile var doorOpener: String? = null

    private fun phaseZeroLeap(): String? {
        if (!config.brLeap) return null
        return doorOpener
            ?: Dungeon.getNameByClass("heal")
            ?: Dungeon.getNameByClass("tank")
    }

    private fun phaseOneLeap(): String? {
        if (!config.p1Leap) return null
        if (Dungeon.inRegion("p1")) return resolve(config.p1Target)
        return null
    }

    private fun phaseTwoLeap(): String? {
        if (!config.p2Leap) return null
        return when {
            Dungeon.inRegion("pillars") -> resolve(config.pillarTarget)
            Dungeon.inRegion("ypad") -> resolve(config.ypadTarget)
            Dungeon.inRegion("s3") -> resolve(config.pdTarget)
            Dungeon.inRegion("gpad") -> resolve(config.gpadTarget)
            Dungeon.inRegion("ppillar") -> resolve(config.ppillarTarget)
            Dungeon.inRegion("ppad") -> resolve(config.ppadTarget)
            else -> null
        }
    }

    private fun phaseThreeLeap(): String? {
        if (!config.p3Leap) return null
        return when {
            Dungeon.inRegion("p2") -> resolve(config.p2Target)
            Dungeon.inRegion("s1") -> resolve(config.s1Target)
            Dungeon.inRegion("s2") -> resolve(config.s2Target)
            Dungeon.inRegion("s3") -> resolve(config.s3Target)
            Dungeon.inRegion("s4") -> resolve(config.s4Target)
            else -> null
        }
    }

    private fun phaseFourLeap(): String? {
        if (!config.p4Leap) return null
        return when {
            Dungeon.inRegion("drop") -> resolve(config.dropTarget)
            Dungeon.inRegion("mid") -> resolve(config.midTarget)
            else -> null
        }
    }

    private fun phaseFiveLeap(): String? {
        if (!config.p5Leap) return null
        if (Dungeon.inRegion("p5")) return resolve(config.p5Target)
        return null
    }

    fun getLeap(): String? = when (Dungeon.phase) {
        0 -> phaseZeroLeap()
        1 -> phaseOneLeap()
        2 -> phaseTwoLeap()
        3 -> phaseThreeLeap()
        4 -> phaseFourLeap()
        5 -> phaseFiveLeap()
        else -> null
    }?.takeIf { it.isNotBlank() }
}
