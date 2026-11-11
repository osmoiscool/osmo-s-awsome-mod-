package com.oam.features

import com.oam.config.OAMConfig
import com.oam.utils.Dungeon

object LeapTarget {

    private val config get() = OAMConfig.settings
    private val profile get() = config.profile

    fun resolve(target: String): String = Dungeon.getNameByClass(target) ?: target.trim()

    @Volatile var doorOpener: String? = null

    private fun phaseZeroLeap(): String? {
        if (!profile.brLeap) return null
        return doorOpener
            ?: Dungeon.getNameByClass("heal")
            ?: Dungeon.getNameByClass("tank")
    }

    private fun phaseOneLeap(): String? {
        if (!profile.p1Leap) return null
        if (Dungeon.inRegion("p1")) return resolve(profile.p1Target)
        return null
    }

    private fun phaseTwoLeap(): String? {
        if (!profile.p2Leap) return null
        val target = when {
            config.selectedClass != "Mage" -> profile.p2StormTarget
            Dungeon.stormCrushed -> profile.p2CrushTarget
            else -> profile.p2LightningTarget
        }
        return resolve(target)
    }

    private fun phaseThreeLeap(): String? {
        if (!profile.p3Leap) return null
        return when {
            Dungeon.inRegion("p2") -> resolve(profile.ssTarget)
            Dungeon.inRegion("s1") -> resolve(profile.s1Target)
            Dungeon.inRegion("s2") -> resolve(profile.s2Target)
            Dungeon.inRegion("s3") -> resolve(profile.s3Target)
            Dungeon.inRegion("s4") -> resolve(profile.s4Target)
            else -> null
        }
    }

    private fun phaseFourLeap(): String? {
        if (!profile.p4Leap) return null
        return when {
            Dungeon.inRegion("drop") -> resolve(profile.dropTarget)
            Dungeon.inRegion("mid") -> resolve(profile.midTarget)
            else -> null
        }
    }

    private fun phaseFiveLeap(): String? {
        if (!profile.p5Leap) return null
        if (Dungeon.inRegion("p5")) return resolve(profile.p5Target)
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
