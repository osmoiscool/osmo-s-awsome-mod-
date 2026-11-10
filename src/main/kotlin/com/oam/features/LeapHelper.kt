package com.oam.features

import com.oam.utils.Scheduler
import com.oam.utils.doPacketInteract
import com.oam.utils.mc
import com.oam.utils.modMessage
import com.oam.utils.removeFormatting
import com.oam.utils.sendPacketDirect
import com.oam.utils.sendWindowClick
import com.oam.utils.skyblockId
import com.oam.utils.swapSlot
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.item.ItemStack

object LeapHelper {

    @Volatile var target: String? = null
    @Volatile private var clickedLeap = false
    @Volatile private var clickedAt = 0L
    @Volatile private var menuOpened = false
    @Volatile private var windowId = -1
    @Volatile private var stateId = 0

    @Volatile private var autoPending = false
    @Volatile private var lastAutoLeap = 0L

    private const val CLICK_TIMEOUT_MS = 1500L

    private val cooldownRegex = Regex("""^This ability is on cooldown for (\d+)s\.$""")
    private val teleportedRegex = Regex("""^You have teleported to .+!$""")

    private fun busy(): Boolean =
        autoPending || menuOpened || (clickedLeap && System.currentTimeMillis() - clickedAt < CLICK_TIMEOUT_MS)

    fun leap(name: String) {
        if (autoPending) return
        target = name
        openLeap()
    }

    private const val INFINILEAP_COOLDOWN_MS = 2000L
    private const val SPIRIT_LEAP_COOLDOWN_MS = 5000L

    @Volatile private var lastInfinileapUse = 0L
    @Volatile private var lastSpiritLeapUse = 0L

    private fun ItemStack.isInfinileap() =
        skyblockId() == "INFINITE_SPIRIT_LEAP" || hoverName.string.removeFormatting() == "Infinileap"

    private fun ItemStack.isSpiritLeap() =
        skyblockId() == "SPIRIT_LEAP" || hoverName.string.removeFormatting() == "Spirit Leap"

    fun infinileapOnCooldown() = System.currentTimeMillis() - lastInfinileapUse < INFINILEAP_COOLDOWN_MS
    fun spiritLeapOnCooldown() = System.currentTimeMillis() - lastSpiritLeapUse < SPIRIT_LEAP_COOLDOWN_MS

    @JvmStatic
    fun onItemUsed(stack: ItemStack) {
        if (stack.isEmpty) return
        val now = System.currentTimeMillis()
        if (stack.isInfinileap()) {
            if (!infinileapOnCooldown()) lastInfinileapUse = now
        } else if (stack.isSpiritLeap()) {
            if (!spiritLeapOnCooldown()) lastSpiritLeapUse = now
        }
    }

    private fun findSpiritLeap(): Int {
        val inv = mc.player?.inventory ?: return -1
        for (i in 0..8) {
            val stack = inv.getItem(i)
            if (!stack.isEmpty && stack.isSpiritLeap()) return i
        }
        return -1
    }

    fun autoLeap(name: String, useSpiritLeapWhenRequired: Boolean = false) {
        if (name.isBlank()) return
        if (System.currentTimeMillis() - lastAutoLeap < 500L) return
        if (busy()) return

        var leapSlot = findLeap()
        if (useSpiritLeapWhenRequired && infinileapOnCooldown() && !spiritLeapOnCooldown()) {
            val spiritSlot = findSpiritLeap()
            if (spiritSlot >= 0) leapSlot = spiritSlot
        }
        if (leapSlot < 0) return

        target = name
        autoPending = true
        lastAutoLeap = System.currentTimeMillis()

        Scheduler.afterTicks(0) {
            val scheduled = SilentSwap.useSlot(leapSlot, "Leaping") {
                autoPending = false
                clickedLeap = true
                clickedAt = System.currentTimeMillis()
                doPacketInteract()
            }
            if (!scheduled) {
                autoPending = false
                target = null
            }
        }
    }

    fun reset() {
        menuOpened = false
        target = null
        clickedLeap = false
        autoPending = false
        windowId = -1
        stateId = 0
    }

    fun onTick() {
        if (clickedLeap && !menuOpened && System.currentTimeMillis() - clickedAt > CLICK_TIMEOUT_MS) reset()
    }

    private fun findLeap(): Int {
        val inv = mc.player?.inventory ?: return -1
        var fallback = -1
        for (i in 0..8) {
            val stack = inv.getItem(i)
            if (stack.isEmpty) continue
            if (stack.isInfinileap()) return i
            if (fallback < 0 && stack.hoverName.string.removeFormatting().lowercase().contains("leap")) fallback = i
        }
        return fallback
    }

    private fun openLeap(): Boolean {
        if (menuOpened) return false
        if (clickedLeap && System.currentTimeMillis() - clickedAt < CLICK_TIMEOUT_MS) return false

        val leapSlot = findLeap()
        if (leapSlot < 0) return false
        if (!swapSlot(leapSlot)) return false

        clickedLeap = true
        clickedAt = System.currentTimeMillis()
        doPacketInteract()
        return true
    }

    fun onPacketReceive(packet: Packet<*>): Boolean = when (packet) {
        is ClientboundOpenScreenPacket -> onOpenScreen(packet)
        is ClientboundContainerSetSlotPacket -> {
            if (packet.containerId == windowId) stateId = packet.stateId
            onSlot(packet.containerId, packet.slot, packet.item)
        }
        is ClientboundContainerSetContentPacket -> onContent(packet)
        is ClientboundContainerClosePacket -> {
            menuOpened = false
            false
        }
        else -> false
    }

    fun onPacketSend(packet: Packet<*>) {
        if (packet is ServerboundContainerClosePacket) menuOpened = false
    }

    fun onChat(message: String) {
        if (cooldownRegex.matches(message) ||
            teleportedRegex.matches(message) ||
            message == "You can only use this item inside dungeons!"
        ) mc.execute { reset() }
    }

    private fun onOpenScreen(packet: ClientboundOpenScreenPacket): Boolean {
        val title = packet.title.string.removeFormatting()
        if (title != "Spirit Leap") {
            menuOpened = false
            return false
        }

        clickedLeap = false
        menuOpened = true
        windowId = packet.containerId
        stateId = 0

        return target != null
    }

    private fun onContent(packet: ClientboundContainerSetContentPacket): Boolean {
        if (target == null || !menuOpened || packet.containerId != windowId) return false
        stateId = packet.stateId
        for ((slot, item) in packet.items.withIndex()) {
            if (target == null) break
            onSlot(packet.containerId, slot, item)
        }
        return true
    }

    private fun onSlot(containerId: Int, slot: Int, item: ItemStack): Boolean {
        val target = target ?: return false
        if (!menuOpened || containerId != windowId || windowId <= 0) return false
        if (item.isEmpty || slot == 0) return false

        if (slot > 35) {
            modMessage("&7Could not find &b$target")
            sendPacketDirect(ServerboundContainerClosePacket(windowId))
            mc.execute { reset() }
            this.target = null
            return true
        }

        val name = item.hoverName.string.removeFormatting().lowercase()
        if (name != target.lowercase()) return true

        LeapRotationFix.onHiddenClick(slot, item)
        sendWindowClick(windowId, stateId, slot)
        modMessage("&7Leaping to &b$target")
        this.target = null
        mc.execute { reset() }
        return true
    }
}
