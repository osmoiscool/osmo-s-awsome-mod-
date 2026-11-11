package com.oam.utils

import com.google.common.primitives.Shorts
import com.google.common.primitives.SignedBytes
import com.oam.mixin.accessors.IConnectionAccessor
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.network.HashedStack
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import java.util.concurrent.ConcurrentHashMap

object ContainerData {

    @Volatile var title: String = ""
    @Volatile var windowId: Int = -1
    @Volatile var stateId: Int = 0
    val slots: MutableMap<Int, ItemStack> = ConcurrentHashMap()
    @Volatile var isFullyLoaded: Boolean = false
    @Volatile var expectedSlotCount: Int = 0

    fun windowClick(slot: Int, clickType: ContainerInput) {
        val clientPacketListener = mc.connection ?: return
        val connection = clientPacketListener.connection

        if (windowId == -1) return
        if (slot !in 0..expectedSlotCount + 35) return
        if (!isFullyLoaded) return

        val button = when (clickType) {
            ContainerInput.CLONE -> 0
            ContainerInput.PICKUP -> 1
            ContainerInput.THROW -> 0
            else -> return
        }

        val hashGenerator = clientPacketListener.decoratedHashOpsGenenerator()
        val int2ObjectMap = Int2ObjectOpenHashMap<HashedStack>()

        val beforeSnapshot = slots.toMap()

        val carriedItem = when (clickType) {
            ContainerInput.CLONE -> HashedStack.create(ItemStack.EMPTY, hashGenerator)

            ContainerInput.PICKUP -> {
                val item = slots[slot] ?: return

                val takeCount = (item.count + 1) / 2  // right click so half the stacksize
                val newCarried = item.copyWithCount(takeCount)
                val remaining = item.copyWithCount(item.count - takeCount)

                slots[slot] = if (remaining.count <= 0) ItemStack.EMPTY else remaining

                for ((index, before) in beforeSnapshot) {
                    val after = slots[index] ?: ItemStack.EMPTY
                    if (!ItemStack.matches(before, after)) {
                        int2ObjectMap[index] = HashedStack.create(after, hashGenerator)
                    }
                }

                HashedStack.create(newCarried, hashGenerator)
            }

            ContainerInput.THROW -> {
                slots[slot] = ItemStack.EMPTY

                for ((index, before) in beforeSnapshot) {
                    val after = slots[index] ?: ItemStack.EMPTY
                    if (!ItemStack.matches(before, after)) {
                        int2ObjectMap[index] = HashedStack.create(after, hashGenerator)
                    }
                }

                HashedStack.create(ItemStack.EMPTY, hashGenerator)
            }

            else -> return
        }

        val packet = ServerboundContainerClickPacket(
            windowId,
            stateId,
            Shorts.checkedCast(slot.toLong()),
            SignedBytes.checkedCast(button.toLong()),
            clickType,
            int2ObjectMap,
            carriedItem
        )

        (connection as IConnectionAccessor).channel.eventLoop().execute {
            connection.send(packet)
        }
    }

    fun onPacketReceive(packet: Packet<*>): Boolean {
        when (packet) {
            is ClientboundOpenScreenPacket -> {
                reset()
                windowId = packet.containerId
                title = packet.title.string
                expectedSlotCount = when (packet.type) {
                    MenuType.GENERIC_9x1 -> 9
                    MenuType.GENERIC_9x2 -> 18
                    MenuType.GENERIC_9x3 -> 27
                    MenuType.GENERIC_9x4 -> 36
                    MenuType.GENERIC_9x5 -> 45
                    MenuType.GENERIC_9x6 -> 54
                    MenuType.GENERIC_3x3 -> 9
                    MenuType.HOPPER -> 5
                    MenuType.ANVIL -> 3
                    MenuType.BEACON -> 1
                    MenuType.BLAST_FURNACE -> 3
                    MenuType.BREWING_STAND -> 5
                    MenuType.CRAFTING -> 10
                    MenuType.ENCHANTMENT -> 2
                    MenuType.FURNACE -> 3
                    MenuType.GRINDSTONE -> 3
                    MenuType.LECTERN -> 1
                    MenuType.LOOM -> 4
                    MenuType.MERCHANT -> 3
                    MenuType.SHULKER_BOX -> 27
                    MenuType.SMITHING -> 4
                    MenuType.SMOKER -> 3
                    MenuType.CARTOGRAPHY_TABLE -> 3
                    MenuType.STONECUTTER -> 2
                    else -> 0
                }
            }

            is ClientboundContainerSetSlotPacket -> {
                if (packet.containerId == windowId) return setSlot(packet.slot, packet.item, packet.stateId)
            }

            is ClientboundContainerSetContentPacket -> {
                if (packet.containerId == windowId) stateId = packet.stateId
            }

            is ClientboundContainerClosePacket -> {
                if (packet.containerId == windowId) reset()
            }
        }
        return false
    }

    fun onPacketSend(packet: Packet<*>) {
        if (packet is ServerboundContainerClosePacket) reset()
    }

    fun reset() {
        windowId = -1
        stateId = 0
        title = ""
        slots.clear()
        isFullyLoaded = false
        expectedSlotCount = 0
    }

    private fun setSlot(slotIndex: Int, item: ItemStack, newStateId: Int): Boolean {
        slots[slotIndex] = item
        stateId = newStateId
        if (!isFullyLoaded && expectedSlotCount > 0 && slots.size >= expectedSlotCount) {
            isFullyLoaded = true
            return true
        }
        return false
    }
}
