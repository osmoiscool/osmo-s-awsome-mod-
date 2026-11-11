//u are sexy leo MWAHH
package com.oam.features

import com.oam.mixin.accessors.IInventoryAccessor
import com.oam.utils.mc
import com.oam.utils.modMessage
import net.minecraft.world.item.ItemStack

object SilentSwap {

    @JvmStatic
    var currentSlot: Int? = null
        private set

    private var scheduledSwap: Int? = -1
    private var callback: (() -> Unit)? = null

    private var scheduledCaller: String = ""
    private var scheduledReason: String = ""

    fun useSlot(slot: Int?, reason: String, callback: (() -> Unit)? = null): Boolean {
        slot ?: return false
        return scheduleSwap(slot, "OAM", reason, callback)
    }

    @JvmStatic
    fun isSilent(): Boolean = currentSlot != null

    @JvmStatic
    fun originalSlot(): Int {
        val inventory = mc.player!!.inventory
        return (inventory as IInventoryAccessor).selected
    }

    @JvmStatic
    fun originalStack(): ItemStack {
        val inventory = mc.player!!.inventory
        return inventory.getItem((inventory as IInventoryAccessor).selected)
    }

    fun scheduleSwap(slot: Int, caller: String, reason: String, callback: (() -> Unit)? = null): Boolean {
        if (scheduledSwap != -1) {
            modMessage("[$scheduledCaller] Currently doing $scheduledReason while $caller is trying to $reason")
            return false
        }

        if (slot !in 0..8) return false

        scheduledSwap = slot
        scheduledCaller = caller
        scheduledReason = reason
        this.callback = callback
        return true
    }

    fun swapNow(slot: Int, reason: String): Boolean {
        if (scheduledSwap != -1) {
            modMessage("[$scheduledCaller] Currently doing $scheduledReason while OAM is trying to $reason")
            return false
        }
        if (slot !in 0..8) return false
        currentSlot = slot
        return true
    }

    private fun afterSlotSwapped() {
        if (scheduledSwap == -1) return

        currentSlot = scheduledSwap
        scheduledSwap = -1

        this.callback?.let { callback ->
            this.callback = null
            callback.invoke()
        }
    }

    fun onTickStart() {
        if (scheduledSwap == -1 && currentSlot != null) {
            scheduledSwap = null
        }

        afterSlotSwapped()
    }

    fun reset() {
        currentSlot = null
        scheduledSwap = -1
        callback = null
    }
}
