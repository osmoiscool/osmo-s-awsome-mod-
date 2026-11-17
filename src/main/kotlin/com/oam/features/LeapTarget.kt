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

    private const val NECRON_LEAP_DELAY_MS = 2000L
    @Volatile private var coreOpenedAt = 0L

    private fun necronLeapReady(): Boolean =
        coreOpenedAt != 0L && System.currentTimeMillis() - coreOpenedAt >= NECRON_LEAP_DELAY_MS

    private const val SS_LEAP_DELAY_MS = 3000L
    @Volatile private var stormCrushedAt = 0L

    private fun ssLeapReady(): Boolean =
        stormCrushedAt != 0L && System.currentTimeMillis() - stormCrushedAt >= SS_LEAP_DELAY_MS

    fun onChat(message: String) {
        if (stormCrushedAt == 0L &&
            (message.contains("[BOSS] Storm: Oof") || message.contains("[BOSS] Storm: Ouch, that hurt!"))) {
            stormCrushedAt = System.currentTimeMillis()
            return
        }
        if (message == "The Core entrance is opening!") {
            coreOpenedAt = System.currentTimeMillis()
            return
        }
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
        coreOpenedAt = 0L
        stormCrushedAt = 0L
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
        if (profile.p3Leap && ssLeapReady()) return resolve(profile.ssTarget)
        if (!profile.p2Leap) return null
        if (config.selectedClass == "Healer" && Dungeon.inRegion("s3")) return resolve(profile.pdTarget)
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
            necronLeapReady() || Dungeon.phase >= 4 -> resolve(profile.dropTarget)
            else -> null
        }
    }

    private fun phaseFiveLeap(): String? {
        if (!profile.p5Leap) return null
        if (!Dungeon.inP5()) return null
        if (config.selectedClass != "Mage" && config.selectedClass != "Tank") return resolve(profile.p5Target)
        return queued(listOf(profile.p5Target, profile.p5GeneralTarget), p5Step) { p5Step++ }
    }

    fun getSlLeap(): String? {
        if (!profile.slFastLeap) return null
        if (Dungeon.phase != 3 || Dungeon.inP5() || Dungeon.inP4() || necronLeapReady()) return null
        val target = when {
            Dungeon.inRegion("s1") -> profile.slS1Target
            Dungeon.inRegion("s2") -> profile.slS2Target
            Dungeon.inRegion("s3") -> profile.slS3Target
            Dungeon.inRegion("s4") -> profile.slS4Target
            else -> return null
        }
        if (target.isBlank()) return null
        pendingAdvance = null
        return resolve(target).takeIf { it.isNotBlank() }
    }

    fun getLeap(): String? {
        pendingAdvance = null
        return phaseLeap()?.takeIf { it.isNotBlank() }
    }

    private fun phaseLeap(): String? = if (Dungeon.phase <= 1 && Dungeon.inRegion("p1")) phaseOneLeap() else if (Dungeon.inP5()) phaseFiveLeap() else if (necronLeapReady() || Dungeon.phase >= 4 || Dungeon.inP4()) phaseFourLeap() else when (Dungeon.phase) {
        0 -> phaseZeroLeap()
        1 -> phaseOneLeap()
        2 -> phaseTwoLeap()
        3 -> phaseThreeLeap()
        4 -> phaseFourLeap()
        5 -> phaseFiveLeap()
        else -> null
    }
}
