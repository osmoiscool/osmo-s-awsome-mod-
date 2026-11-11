package com.oam.features

import com.oam.utils.ContainerData
import com.oam.utils.doPacketInteract
import com.oam.utils.mc
import com.oam.utils.modMessage
import com.oam.utils.removeFormatting
import com.oam.utils.sendPacketDirect
import com.oam.utils.skyblockId
import com.oam.utils.swapSlot
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack

object LeapHelper {

    @Volatile var target: String? = null
    @Volatile private var clickedLeap = false
    @Volatile private var clickedAt = 0L
    @Volatile private var menuOpened = false
    @Volatile private var windowId = -1

    private class PendingLeap(val name: String, val useSpiritLeapWhenRequired: Boolean)

    @Volatile private var pendingLeap: PendingLeap? = null
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

        pendingLeap = PendingLeap(name, useSpiritLeapWhenRequired)
        autoPending = true
        lastAutoLeap = System.currentTimeMillis()
    }

    fun onTickStart() {
        val pending = pendingLeap ?: return
        pendingLeap = null
        autoPending = false

        var leapSlot = findLeap()
        if (pending.useSpiritLeapWhenRequired && infinileapOnCooldown() && !spiritLeapOnCooldown()) {
            val spiritSlot = findSpiritLeap()
            if (spiritSlot >= 0) leapSlot = spiritSlot
        }
        if (leapSlot < 0) return
        if (!SilentSwap.swapNow(leapSlot, "Leaping")) return

        target = pending.name
        clickedLeap = true
        clickedAt = System.currentTimeMillis()
        doPacketInteract()
    }

    fun reset() {
        menuOpened = false
        target = null
        clickedLeap = false
        pendingLeap = null
        autoPending = false
        windowId = -1
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
        is ClientboundContainerSetSlotPacket -> hidingMenu(packet.containerId)
        is ClientboundContainerSetContentPacket -> hidingMenu(packet.containerId)
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

        return target != null
    }

    private fun hidingMenu(containerId: Int): Boolean =
        target != null && menuOpened && containerId == windowId && windowId > 0

    fun onContainerLoaded(containerId: Int) {
        val target = target ?: return
        if (!menuOpened || containerId != windowId || windowId <= 0) return

        for (slot in 1..35) {
            val item = ContainerData.slots[slot] ?: continue
            if (item.isEmpty) continue
            if (item.hoverName.string.removeFormatting().lowercase() != target.lowercase()) continue

            LeapRotationFix.onHiddenClick(slot, item)
            ContainerData.windowClick(slot, ContainerInput.CLONE)
            modMessage("&7Leaping to &b$target")
            this.target = null
            mc.execute { reset() }
            return
        }

        modMessage("&7Could not find &b$target")
        sendPacketDirect(ServerboundContainerClosePacket(windowId))
        this.target = null
        mc.execute { reset() }
    }
}
