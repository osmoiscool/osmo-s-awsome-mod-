package com.oam.features

import com.oam.config.OAMConfig
import com.oam.utils.Dungeon

object LeapTarget {

    private val config get() = OAMConfig.settings
    private val profile get() = config.profile

    fun resolve(target: String): String = Dungeon.getNameByClass(target) ?: target.trim()

    @Volatile var doorOpener: String? = null

    @Volatile private var p2Step = 0
    @Volatile private var p5Step = 0
    @Volatile private var pendingAdvance: (() -> Unit)? = null

    private val teleportedRegex = Regex("""^You have teleported to .+!$""")

    private fun queued(targets: List<String>, step: Int, advance: () -> Unit): String? {
        val queue = targets.filter { it.isNotBlank() }
        if (queue.isEmpty()) return null
        pendingAdvance = advance
        return resolve(queue[step.coerceAtMost(queue.size - 1)])
    }

    fun onChat(message: String) {
        if (teleportedRegex.matches(message)) {
            pendingAdvance?.invoke()
            pendingAdvance = null
        } else if (message.startsWith("This ability is on cooldown for")) {
            pendingAdvance = null
        }
    }

    fun onWorldChange() {
        doorOpener = null
        p2Step = 0
        p5Step = 0
        pendingAdvance = null
    }

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
        if (config.selectedClass != "Mage") return resolve(profile.p2StormTarget)
        val targets = mutableListOf(profile.p2InitialTarget, profile.p2LightningTarget, profile.p2CrushTarget)
        if (profile.p3Leap) targets += profile.ssTarget
        return queued(targets, p2Step) { p2Step++ }
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
            Dungeon.inRegion("mid") -> resolve(profile.midTarget)
            Dungeon.phase >= 4 -> resolve(profile.dropTarget)
            else -> null
        }
    }

    private fun phaseFiveLeap(): String? {
        if (!profile.p5Leap) return null
        if (!Dungeon.inP5()) return null
        if (config.selectedClass != "Mage" && config.selectedClass != "Tank") return resolve(profile.p5Target)
        return queued(listOf(profile.p5Target, profile.p5GeneralTarget), p5Step) { p5Step++ }
    }

    fun getLeap(): String? {
        pendingAdvance = null
        return phaseLeap()?.takeIf { it.isNotBlank() }
    }

    private fun phaseLeap(): String? = if (Dungeon.inP5()) phaseFiveLeap() else if (Dungeon.phase >= 4 || Dungeon.inP4()) phaseFourLeap() else when (Dungeon.phase) {
        0 -> phaseZeroLeap()
        1 -> phaseOneLeap()
        2 -> phaseTwoLeap()
        3 -> phaseThreeLeap()
        4 -> phaseFourLeap()
        5 -> phaseFiveLeap()
        else -> null
    }
}
