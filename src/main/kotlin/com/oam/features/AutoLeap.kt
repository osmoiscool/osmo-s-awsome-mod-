package com.oam.features

import com.oam.config.OAMConfig
import com.oam.utils.Dungeon
import com.oam.utils.Scheduler
import com.oam.utils.isPlayerInBox
import com.oam.utils.mc
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import java.util.Optional
import kotlin.math.hypot

object AutoLeap {

    private val config get() = OAMConfig.settings

    private fun leapTo(target: String) {
        if (target.isBlank()) return
        LeapHelper.autoLeap(LeapTarget.resolve(target))
    }

    fun onWorldChange() {
        LeapTarget.doorOpener = null
        resetSections()
        stormCrushed = false
        bersDone = false
    }

    fun onChat(message: String) {
        onBrChat(message)
        onCrystalChat(message)
        onSectionChat(message)
        onRelicChat(message)
        onStormChat(message)
        onBersChat(message)
    }

    fun onPacketReceive(packet: Packet<*>) {
        if (packet is ClientboundSetEntityDataPacket) onBersEntityData(packet)
    }

    private val witherDoorRegex = Regex("""^(?:\[[^\]]+] )?(\w+) opened a WITHER door!$""")

    private fun onBrChat(message: String) {
        if (message.startsWith("You have teleported to ")) {
            LeapTarget.doorOpener = null
            return
        }

        if (Dungeon.phase != 0) return
        val opener = witherDoorRegex.find(message)?.groupValues?.get(1) ?: return
        if (opener == mc.player?.name?.string) return

        LeapTarget.doorOpener = opener

        if (config.brAuto) LeapHelper.autoLeap(opener, useSpiritLeapWhenRequired = config.brUseSpiritLeap)
    }

    private fun onCrystalChat(message: String) {
        if (!config.crystalAuto) return
        val me = mc.player?.name?.string ?: return
        if (message != "$me picked up an Energy Crystal!") return
        val target = LeapTarget.getLeap() ?: return
        LeapHelper.autoLeap(target)
    }

    private val progressRegex = Regex("""(\w+) (activated|completed) (a terminal|a device|a lever)! \((\d)/(\d)\)""")

    @Volatile private var progress = 0
    @Volatile private var total = 7
    @Volatile private var gateBlown = false
    @Volatile private var section = 1

    private fun resetSections() {
        progress = 0
        total = 7
        gateBlown = false
        section = 1
    }

    private fun onSectionChat(message: String) {
        if (message == "[BOSS] Goldor: Who dares trespass into my domain?") {
            resetSections()
            return
        }
        if (Dungeon.phase != 3) return

        progressRegex.find(message)?.let { match ->
            val completed = match.groupValues[4].toInt()
            val newTotal = match.groupValues[5].toInt()

            when {
                completed < progress -> {
                    newSection()
                    progress = completed
                    total = newTotal
                }
                completed == newTotal && gateBlown -> newSection()
                else -> {
                    progress = completed
                    total = newTotal
                }
            }
            return
        }

        when (message) {
            "The gate has been destroyed!" ->
                if (progress == total) newSection() else gateBlown = true
            "The Core entrance is opening!" ->
                if (section == 4) newSection()
        }
    }

    private fun newSection() {
        val finished = section
        progress = 0
        total = 7
        gateBlown = false
        section = if (section >= 4) 1 else section + 1

        if (finished == 1 && config.ssCompleteAuto && config.ssCompleteTarget.isNotBlank() &&
            isPlayerInBox(90.5, 110.5, 113.0, 144.0, 52.5, 122.5)) {
            leapTo(config.ssCompleteTarget)
            return
        }

        if (!config.p3Auto) return

        val target = when (finished) {
            1 -> if (Dungeon.inRegion("s1")) config.profile.s1Target else null
            2 -> if (Dungeon.inRegion("s2")) config.profile.s2Target else null
            3 -> if (Dungeon.inRegion("s3")) config.profile.s3Target else null
            4 -> if (Dungeon.inRegion("s4")) config.profile.s4Target else null
            else -> null
        } ?: return

        leapTo(target)
    }

    private val relicRegex = Regex("""(\w+) picked the Corrupted \w+ Relic!""")

    private fun onRelicChat(message: String) {
        if (!config.p5Auto) return
        val name = relicRegex.find(message)?.groupValues?.get(1) ?: return
        if (name != mc.player?.name?.string) return

        val target = config.profile.p5Target
        Scheduler.afterMs(config.p5AutoDelay.coerceIn(0, 500).toLong()) { leapTo(target) }
    }

    @Volatile private var stormCrushed = false

    private fun onStormChat(message: String) {
        if (!config.stormCrushAuto || stormCrushed) return
        if (message.contains("[BOSS] Storm: Oof") || message.contains("[BOSS] Storm: Ouch, that hurt!")) {
            stormCrushed = true
            leapTo(config.stormCrushTarget)
        }
    }

    private val deviceCompleteRegex = Regex("""^(.{1,16}) completed a device! \((\d)/(\d)\)$""")

    @Volatile private var bersDone = false

    private fun onFourthDevice(): Boolean {
        val p = mc.player ?: return false
        return p.y == 127.0 && hypot(p.x - 63.5, p.z - 35.5) < 0.5
    }

    private fun holdingTerminator(): Boolean =
        mc.player?.mainHandItem?.hoverName?.string?.lowercase()?.contains("terminator") == true

    private fun bersReady(): Boolean =
        config.bersLeapoutAuto && !bersDone && Dungeon.phase == 3 && onFourthDevice() && holdingTerminator()

    private fun triggerBers() {
        bersDone = true
        val target = config.bersLeapoutTarget
        Scheduler.afterTicks(1) { leapTo(target) }
    }

    private fun onBersChat(message: String) {
        if (!bersReady()) return
        val name = deviceCompleteRegex.find(message)?.groupValues?.get(1) ?: return
        if (name == mc.player?.name?.string) triggerBers()
    }

    private fun onBersEntityData(packet: ClientboundSetEntityDataPacket) {
        if (!bersReady()) return
        val becameActive = packet.packedItems().any { value ->
            val v = value.value()
            val component = (v as? Optional<*>)?.orElse(null) ?: v
            (component as? Component)?.string == "Active"
        }
        if (!becameActive) return

        val entity = mc.level?.getEntity(packet.id) ?: return
        if (hypot(entity.x - 63.5, entity.z - 35.5) > 8.0) return
        triggerBers()
    }
}
