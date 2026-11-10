package com.oam.utils

import com.google.common.primitives.Shorts
import com.google.common.primitives.SignedBytes
import com.oam.features.LeapHelper
import com.oam.mixin.accessors.IConnectionAccessor
import com.oam.mixin.accessors.IMultiPlayerGameModeAccessor
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.network.HashedStack
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType

fun sendPacketDirect(packet: Packet<*>) {
    val connection = mc.connection?.connection ?: return
    (connection as IConnectionAccessor).channel.eventLoop().execute { connection.send(packet) }
}

fun sendWindowClick(windowId: Int, stateId: Int, slot: Int) {
    val listener = mc.connection ?: return
    val hashGenerator = listener.decoratedHashOpsGenenerator()

    val packet = ServerboundContainerClickPacket(
        windowId,
        stateId,
        Shorts.checkedCast(slot.toLong()),
        SignedBytes.checkedCast(0L),
        ContainerInput.CLONE,
        Int2ObjectOpenHashMap<HashedStack>(),
        HashedStack.create(ItemStack.EMPTY, hashGenerator)
    )
    sendPacketDirect(packet)
}

fun swapSlot(slot: Int): Boolean {
    val player = mc.player ?: return false
    if (slot !in 0..8) return false
    if (player.inventory.selectedSlot == slot) return true
    player.inventory.selectedSlot = slot
    (mc.gameMode as IMultiPlayerGameModeAccessor).callEnsureHasSentCarriedItem()
    return true
}

fun doPacketInteract(hand: InteractionHand = InteractionHand.MAIN_HAND): InteractionResult {
    val gameMode = mc.gameMode ?: return InteractionResult.PASS
    val player = mc.player ?: return InteractionResult.PASS
    val level = mc.level ?: return InteractionResult.PASS
    if (gameMode.playerMode == GameType.SPECTATOR) return InteractionResult.PASS

    val accessor = gameMode as IMultiPlayerGameModeAccessor
    accessor.callEnsureHasSentCarriedItem()

    var result: InteractionResult = InteractionResult.PASS
    accessor.callStartPrediction(level) { sequence ->
        val packet = ServerboundUseItemPacket(hand, sequence, player.yRot, player.xRot)
        val stack = player.getItemInHand(hand)
        LeapHelper.onItemUsed(stack)
        if (!player.cooldowns.isOnCooldown(stack)) {
            result = stack.use(level, player, hand)
        }
        packet
    }
    return result
}
