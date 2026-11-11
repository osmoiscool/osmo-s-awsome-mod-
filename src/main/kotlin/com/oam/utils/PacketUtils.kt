package com.oam.utils

import com.oam.features.LeapHelper
import com.oam.mixin.accessors.IConnectionAccessor
import com.oam.mixin.accessors.IMultiPlayerGameModeAccessor
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.GameType

fun sendPacketDirect(packet: Packet<*>) {
    val connection = mc.connection?.connection ?: return
    (connection as IConnectionAccessor).channel.eventLoop().execute { connection.send(packet) }
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
